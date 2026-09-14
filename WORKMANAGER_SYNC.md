# Outbox Sync (WorkManager)

POST otomatis ke server saat koneksi tersedia **dan** layak pakai, dengan Room sebagai antrian
yang diawasi. Detail API/entity/DAO sengaja masih TODO — alur, penjadwalan, dan kontraknya sudah final.

## Berkas

| Berkas | Peran |
|---|---|
| `core/sync/OutboxUploadWorker.kt` | `CoroutineWorker` yang menguras antrian lewat HTTP POST |
| `core/sync/NetworkQualityChecker.kt` | Menilai koneksi benar-benar tembus dan cukup cepat |
| `core/sync/NetworkQuality.kt` | Hasil penilaian: `Good` / `Degraded(reason)` |
| `core/sync/SyncScheduler.kt` | Satu-satunya pintu enqueue (berkala, sekali jalan, batal) |
| `core/sync/SyncDefaults.kt` | Semua ambang batas dan nama unique work |
| `domain/sync/repository/PendingUploadRepository.kt` | Kontrak outbox |
| `data/sync/repository/PendingUploadRepositoryImpl.kt` | Kerangka implementasi — berisi TODO Room/Retrofit |
| `di/SyncModule.kt` | Binding Hilt |

## Kapan worker jalan

Tiga pemicu, semuanya bermuara ke `OutboxUploadWorker` yang sama:

1. **Otomatis saat data baru** — `PendingUploadRepository.enqueue()` menulis payload ke Room lalu
   memanggil `syncScheduler.syncNow(REASON_NEW_DATA)`. Inilah bentuk "watch" antrian Room-nya:
   pemicu ditempel di jalur tulis, bukan lewat observer permanen (WorkManager tidak dirancang
   untuk hidup selamanya — worker bisa dihentikan sistem setelah ~10 menit).
2. **Otomatis berkala** — `BcafApplication.onCreate()` memanggil `schedulePeriodicSync()`
   (tiap 15 menit, policy `KEEP`). Jaring pengaman kalau proses mati sebelum enqueue sempat jalan
   atau constraint tak kunjung terpenuhi.
3. **Manual** — `syncScheduler.syncNow()` dari tombol *Retry* atau *Pull to refresh*.

Jadi: **tidak perlu trigger manual untuk operasi normal**; entry point manual hanya untuk UI retry.

## Skenario pemeriksaan koneksi

Dua lapis, karena constraint WorkManager saja tidak cukup:

**Lapis 1 — constraint (sebelum worker dijalankan).** `NetworkType.CONNECTED`; khusus periodic
ditambah `setRequiresBatteryNotLow(true)`. Selama belum terpenuhi worker tidak dibangunkan sama sekali.

**Lapis 2 — `NetworkQualityChecker` (baris pertama di dalam worker).** Membaca `NetworkCapabilities`
dari jaringan aktif:

| Kondisi | Hasil |
|---|---|
| Tidak ada jaringan aktif | `Degraded(OFFLINE)` |
| Tanpa `NET_CAPABILITY_INTERNET` | `Degraded(NO_INTERNET_CAPABILITY)` |
| Tanpa `NET_CAPABILITY_VALIDATED` | `Degraded(NOT_VALIDATED)` — WiFi nyambung tapi internet mati / captive portal |
| `NET_CAPABILITY_NOT_SUSPENDED` hilang | `Degraded(SUSPENDED)` — mis. data terputus saat panggilan suara |
| `linkDownstreamBandwidthKbps` 1..319 | `Degraded(TOO_SLOW)` |
| Selain itu (termasuk bandwidth `0` = tidak diketahui) | `Good` |

`Degraded` → `Result.retry()` dengan backoff eksponensial 30 detik, menyerah (`Result.failure`)
setelah 5 percobaan.

## Alur satu kali jalan

```
constraint terpenuhi
  └─ cek NetworkQualityChecker
       ├─ Degraded  → log alasan → retry (backoff) / failure setelah 5x
       └─ Good      → ambil batch 20 item dari Room
                        ├─ kosong                → success
                        ├─ POST sukses           → tandai terkirim, lanjut item berikutnya
                        ├─ POST gagal (jaringan) → hentikan batch → retry (backoff)
                        └─ POST gagal (4xx/lain) → tandai gagal permanen, lanjut item berikutnya
                      ulangi maksimal 5 batch per run, sisanya → retry
```

Kegagalan jaringan dibedakan dari kegagalan bisnis: yang pertama dicoba ulang, yang kedua tidak
supaya antrian tidak macet oleh satu payload rusak.

## Cara pakai

Menaruh data ke antrian (satu-satunya yang dipanggil kode fitur):

```kotlin
class SubmitLoanViewModel @Inject constructor(
    private val pendingUploadRepository: PendingUploadRepository,
    private val json: Json,
) : ViewModel() {

    fun submit(form: LoanForm) = viewModelScope.launch {
        pendingUploadRepository.enqueue(
            endpoint = "v1/loan-applications",
            payload = json.encodeToString(form),
        )
        // Worker menyusul sendiri begitu koneksi layak — UI tidak perlu menunggu.
    }
}
```

Tombol retry manual dan indikator status:

```kotlin
class SyncStatusViewModel @Inject constructor(
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    val isSyncing = syncScheduler.observeSyncState()
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun retry() = syncScheduler.syncNow()
}
```

## Konfigurasi

Ambang batas di `SyncDefaults.kt`:

```kotlin
MIN_DOWNSTREAM_KBPS = 320        // batas bawah "koneksi bagus"
BATCH_SIZE = 20                  // item per query Room
MAX_BATCH_PER_RUN = 5            // batas kerja per run sebelum dijadwalkan ulang
MAX_RUN_ATTEMPTS = 5             // percobaan sebelum menyerah
BACKOFF_DELAY_SECONDS = 30       // backoff eksponensial
PERIODIC_INTERVAL_MINUTES = 15   // minimum yang diizinkan WorkManager
```

## Log

Filter `adb logcat -s OutboxUploadWorker SyncScheduler PendingUploadRepo`:

```
SyncScheduler: sync berkala terjadwal setiap 15 menit
PendingUploadRepo: payload masuk antrian untuk v1/loan-applications
SyncScheduler: sync sekali jalan di-enqueue, alasan=data-baru
OutboxUploadWorker: mulai, percobaan ke-1 dari 5
OutboxUploadWorker: koneksi belum layak: NOT_VALIDATED (12000 kbps)
OutboxUploadWorker: menjadwalkan percobaan ulang: koneksi NOT_VALIDATED
OutboxUploadWorker: koneksi layak (12000 kbps), mulai menguras outbox
OutboxUploadWorker: batch 1 berisi 3 item
OutboxUploadWorker: POST sukses id=41 endpoint=v1/loan-applications
OutboxUploadWorker: outbox kosong, selesai. total terkirim=3
```

## Yang tersisa

1. Buat `PendingUploadEntity` + `PendingUploadDao` (kolom minimal: `id`, `endpoint`, `payload`,
   `attemptCount`, `createdAt`, `lastError`), daftarkan di `BcafDatabase` beserta migrasinya.
2. Buat `PendingUploadApi` (`@POST` dinamis via `@Url`) dan sambungkan di `upload()` memakai
   `runApiCatching` + `requirePayload` seperti repository lain.
3. Ganti seluruh TODO di `PendingUploadRepositoryImpl` dengan pemanggilan DAO/API tersebut.
4. Opsional: `setExpedited` pada `syncNow()` bila payload butuh terkirim segera — di minSdk 29
   ini mewajibkan `getForegroundInfo()` beserta notification channel-nya.
