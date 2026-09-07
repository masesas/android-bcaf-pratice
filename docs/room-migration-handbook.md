# Room Migration Handbook

Panduan operasional migrasi database Room di proyek ini. Ditulis dari implementasi nyata
yang sudah diverifikasi jalan di perangkat (SM-A556E, Android 16, Room 2.8.4).

---

## 1. Tiga komponen yang membuat schema export bekerja

Ketiganya harus ada. Hilang satu, seluruh mekanisme migrasi mati diam-diam.

| Komponen | Lokasi | Peran |
|---|---|---|
| `exportSchema = true` | `@Database` di `BcafDatabase.kt` | Izin: boleh menulis schema |
| `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` | `app/build.gradle.kts` | Alamat: tulis ke `app/schemas` |
| `sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")` | `app/build.gradle.kts` | Agar `MigrationTestHelper` bisa membaca schema dari assets |

`$projectDir` adalah folder **modul** (`app/`), bukan root repo. Nilai di komponen 2 dan 3
harus sama persis.

Room tidak bisa dikonfigurasi langsung dari Gradle — opsinya dititipkan lewat annotation
processor. Kalau proyek pindah ke KAPT, bentuknya jadi `kapt { arguments { arg(...) } }`.

---

## 2. Anatomi file schema

`app/schemas/<nama.class.Database>/<versi>.json` berisi:

- `identityHash` — sidik jari skema, dipakai Room saat runtime untuk mendeteksi mismatch
- `createSql` tiap tabel, lengkap dengan tipe kolom dan `NOT NULL`
- daftar `fields` (`columnName`, `affinity`, `notNull`, `defaultValue`)
- `indices` dan `foreignKeys`

`${TABLE_NAME}` di dalam `createSql` adalah placeholder. Saat menyalinnya ke kode migrasi,
ganti dengan nama tabel asli.

**File JSON wajib di-commit.** Schema yang hanya ada di mesin lokal membuat dev lain dan CI
kehilangan baseline, dan `runMigrationsAndValidate` akan gagal di mesin mereka.

---

## 3. Workflow saat menambah / mengubah entity

```
1. Ubah entity (atau buat baru + daftarkan di @Database.entities)
2. Naikkan version di @Database        ← paling sering lupa
3. ./gradlew :app:assembleDebug        → N.json muncul otomatis
4. Diff (N-1).json vs N.json           → itulah spesifikasi migrasi
5. Tulis MIGRATION_(N-1)_N di BcafMigrations.kt
6. Daftarkan di array BCAF_MIGRATIONS
7. Tambah case di BcafDatabaseMigrationTest
8. ./gradlew :app:connectedDebugAndroidTest
9. Commit kode + N.json BERSAMAAN
```

Kalau `version` tidak dinaikkan, `N.json` tidak pernah dibuat dan aplikasi crash saat dibuka.

Cara cepat melihat diff dua schema:

```bash
cd app/schemas/com.masesas.exercise.bcaf_test_1.core.database.BcafDatabase
python3 -c "
import json
def cols(v):
    d=json.load(open(f'{v}.json'))['database']
    return {e['tableName']:{f['columnName']:(f.get('affinity'),f.get('notNull')) for f in e['fields']} for e in d['entities']}
a,b=cols(3),cols(4)
print('tabel baru:', set(b)-set(a))
for t in set(a)&set(b):
    diff={k:(a[t].get(k),b[t].get(k)) for k in set(a[t])|set(b[t]) if a[t].get(k)!=b[t].get(k)}
    if diff: print(t, diff)
"
```

---

## 4. Tiga pola migrasi

### a. Tambah tabel — paling mudah

Salin `createSql` dari JSON versi baru, ganti `${TABLE_NAME}`:

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loan_product` (" +
                "`id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `code` TEXT NOT NULL, " +
                "... , PRIMARY KEY(`id`))"
        )
    }
}
```

Bisa juga tanpa menulis SQL sama sekali — Room men-generate sendiri dari kedua JSON:

```kotlin
@Database(..., autoMigrations = [AutoMigration(from = 2, to = 3)])
```

### b. Tambah kolom nullable — mudah

```kotlin
db.execSQL("ALTER TABLE `loan_application` ADD COLUMN `rejectReason` TEXT")
```

Kolom `NOT NULL` wajib punya `DEFAULT`, karena baris lama harus diisi sesuatu:

```kotlin
db.execSQL("ALTER TABLE `loan_application` ADD COLUMN `retryCount` INTEGER NOT NULL DEFAULT 0")
```

Nilai `DEFAULT` di SQL harus cocok dengan `defaultValue` di JSON, kalau tidak validasi gagal.

### c. Ubah tipe kolom — wajib rekonstruksi tabel

SQLite **tidak punya** `ALTER COLUMN TYPE`. Satu-satunya cara adalah empat langkah:
buat tabel baru → pindahkan data → drop tabel lama → rename.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `loan_application_new` (... `submittedAmount` TEXT NOT NULL ...)")
        db.execSQL(
            "INSERT INTO `loan_application_new` (`id`, ..., `submittedAmount`, ...) " +
                "SELECT `id`, ..., CAST(`submittedAmount` AS TEXT), ... FROM `loan_application`"
        )
        db.execSQL("DROP TABLE `loan_application`")
        db.execSQL("ALTER TABLE `loan_application_new` RENAME TO `loan_application`")
    }
}
```

Selalu tulis daftar kolom eksplisit di `INSERT INTO ... (kolom...)`. Mengandalkan urutan
kolom implisit akan meledak diam-diam begitu ada kolom baru disisipkan di tengah.

Kasus ini muncul di proyek ini ketika `submittedAmount` pindah dari `Long` ke `BigDecimal`:
`BigDecimalConverter` menyimpannya sebagai TEXT, sehingga affinity kolom berubah
`INTEGER` → `TEXT`. `CAST(... AS TEXT)` menghasilkan `"50000000"` yang dapat diparse utuh
oleh `BigDecimal`.

---

## 5. Testing

### Setup

```kotlin
// app/build.gradle.kts
androidTestImplementation(libs.androidx.room.testing)
```

Konstruktor yang valid di Room 2.8.4 (sudah diverifikasi compile):

```kotlin
@get:Rule
val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    BcafDatabase::class.java,
)
```

Overload ini berubah beberapa kali antar versi Room — versi lama memakai nama class sebagai
`String` plus `FrameworkSQLiteOpenHelperFactory`. Kalau upgrade Room bikin merah, cek
overload yang tersedia sebelum menyalahkan test.

### Tiga lapis yang perlu diuji

| Lapis | Yang dibuktikan |
|---|---|
| Per-step (`1→2`, `2→3`) | Setiap migrasi berdiri sendiri dengan benar |
| Data preservation | Isi baris lama selamat, bukan cuma bentuk tabelnya |
| Rantai penuh + buka Room asli | DAO produksi bisa membaca hasil migrasi |

Lapis ketiga yang paling penting dan paling sering dilewat:

```kotlin
helper.runMigrationsAndValidate(TEST_DB, 3, true, *BCAF_MIGRATIONS)

val database = Room.databaseBuilder(context, BcafDatabase::class.java, TEST_DB)
    .addMigrations(*BCAF_MIGRATIONS).build().also(helper::closeWhenFinished)

assertEquals(BigDecimal("50000000"), database.loanApplicationDao().observeAll().first().first().submittedAmount)
```

`runMigrationsAndValidate` hanya memvalidasi *bentuk* skema. Membuka instance Room asli
membuktikan DAO, TypeConverter, dan query benar-benar jalan di atas hasil migrasi — persis
yang terjadi di HP user saat update aplikasi.

Verifikasi tipe kolom pakai `typeof()`, bukan sekadar membaca nilainya:

```kotlin
db.query("SELECT submittedAmount, typeof(submittedAmount) FROM loan_application")
// kolom kedua harus "text", bukan "integer"
```

### Wajib: buktikan test-nya bisa gagal

Test hijau tidak berarti apa-apa kalau ia juga hijau saat kode salah. Setelah menulis
migrasi baru, **rusak sengaja lalu jalankan ulang**:

```bash
# ubah satu tipe kolom di migrasi, misal TEXT jadi INTEGER
./gradlew :app:connectedDebugAndroidTest
# harus muncul: IllegalStateException: Migration didn't properly handle: <tabel>
# lalu pulihkan dan pastikan hijau lagi
```

Exception itu persis yang akan meledak di HP user. Kalau sabotase tidak membuat test merah,
test-nya tidak menguji apa pun.

Satu jebakan praktis: pastikan sabotasenya benar-benar teraplikasi (`git diff` atau `diff`
terhadap backup) sebelum menyimpulkan. Edit yang gagal match menghasilkan kesimpulan terbalik
— seolah kode rusak tapi test lulus.

---

## 6. Aturan main proyek ini

**`fallbackToDestructiveMigration` tidak dipakai.** Yang dipakai:

```kotlin
Room.databaseBuilder(context, BcafDatabase::class.java, BcafDatabase.NAME)
    .addMigrations(*BCAF_MIGRATIONS)
    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
    .build()
```

Alasannya: fallback penuh menelan error "migrasi hilang" secara diam-diam, sehingga baru
ketahuan setelah user produksi kehilangan data. Varian `OnDowngrade` hanya menghapus DB saat
menginstall build lebih lama (turun versi) — situasi yang hanya terjadi di mesin dev dan
memang selalu aman. Jalur maju tetap crash keras kalau ada migrasi yang lupa ditulis, dan
crash di emulator jauh lebih murah daripada data hilang di HP user.

Konsekuensinya: **setiap kenaikan versi wajib punya migrasi + test.** Tidak ada jalan pintas.

---

## 7. Troubleshooting

| Gejala | Penyebab |
|---|---|
| `Migration didn't properly handle: <tabel>` | SQL migrasi tidak menghasilkan skema identik dengan JSON versi tujuan. Bandingkan blok `Expected:` dan `Found:` di pesan error — biasanya beda tipe kolom, `NOT NULL`, atau `DEFAULT` |
| `Cannot find the schema file` saat test | `sourceSets ... assets.srcDir` belum diset, atau file JSON versi tersebut belum di-commit |
| `A migration from X to Y was required but not found` | Lupa mendaftarkan migrasi di `BCAF_MIGRATIONS` |
| Schema JSON tidak muncul setelah build | `version` belum dinaikkan, atau `exportSchema`/`room.schemaLocation` hilang |
| Build warning "Schema export directory is not provided" | `room.schemaLocation` tidak diset |

---

## Referensi file di proyek ini

- `core/database/BcafDatabase.kt` — deklarasi `@Database`, versi, entities
- `core/database/BcafMigrations.kt` — semua `Migration` + array `BCAF_MIGRATIONS`
- `core/database/BigDecimalConverter.kt` — TypeConverter yang menyebabkan kolom bertipe TEXT
- `di/DatabaseModule.kt` — pemasangan migrasi ke `databaseBuilder`
- `androidTest/.../database/BcafDatabaseMigrationTest.kt` — test migrasi
- `app/schemas/com.masesas.exercise.bcaf_test_1.core.database.BcafDatabase/` — riwayat schema
