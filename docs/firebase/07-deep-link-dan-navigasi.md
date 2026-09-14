# 07 — Deep Link: Dari Notifikasi ke Layar yang Benar

Notifikasi yang membuka halaman utama lalu membiarkan user mencari sendiri sama saja dengan tidak
punya deep link. Bagian ini menghubungkan payload push ke route Compose yang tepat.

## 7.1 Modal yang sudah ada di project

`TransactionRoutes.kt` sudah menyediakan pondasinya:

```kotlin
const val TRANSACTION_DETAIL_DEEP_LINK = "bcaf://transaction"
```

dan `transactionDetailDestinations()` sudah mendaftarkan:

```kotlin
deepLinks = listOf(navDeepLink<TransactionDetailRoute>(basePath = TRANSACTION_DETAIL_DEEP_LINK))
```

`navDeepLink<T>` versi type-safe menyusun pola URI dari parameter route. Karena
`TransactionDetailRoute(val transactionId: String)` punya satu argumen wajib, pola yang dihasilkan:

```
bcaf://transaction/{transactionId}
```

Jadi URI konkret untuk pengajuan `A-10293` adalah `bcaf://transaction/A-10293`.

Artinya deep link dari notifikasi **tidak perlu jalur baru** — cukup memicu `Intent.ACTION_VIEW`
dengan URI itu.

## 7.2 Mendaftarkan skema di manifest

```xml
<activity
    android:name=".presentation.compose.HomeActivityCompose"
    android:exported="true"
    android:launchMode="singleTop">

    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="bcaf" android:host="transaction" />
    </intent-filter>
</activity>
```

`launchMode="singleTop"` penting: tanpa itu, mengetuk notifikasi saat app sedang terbuka membuat
instance Activity kedua di atas yang lama.

> **Catatan keamanan.** Custom scheme bisa dipanggil aplikasi lain dan halaman web mana pun. Anggap
> nilai dari URI sebagai input tidak tepercaya: validasi `transactionId` sebelum dipakai, dan pastikan
> `TransactionDetailScreen` mengambil data lewat endpoint ber-otorisasi — bukan menampilkan apa pun
> yang dititipkan URI. Untuk jaminan lebih kuat, naikkan ke **App Links** (`https://` + verifikasi
> `assetlinks.json`), yang tidak bisa dibajak app lain.

## 7.3 Membangun PendingIntent deep link

Ganti `contentIntent()` di `PushNotifier` ([05 §5.3](05-messaging-service.md#53-pushnotifierkt)):

```kotlin
private fun contentIntent(payload: PushPayload): PendingIntent {
    val intent = when {
        payload.type == PushType.LOAN_APPLICATION && payload.referenceId != null ->
            Intent(
                Intent.ACTION_VIEW,
                "$TRANSACTION_DETAIL_DEEP_LINK/${Uri.encode(payload.referenceId)}".toUri(),
                context,
                HomeActivityCompose::class.java,
            )

        else -> Intent(context, HomeActivityCompose::class.java)
    }.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    return PendingIntent.getActivity(
        context,
        notificationId(payload),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
```

Dua hal:

- Konstruktor `Intent(action, uri, context, class)` membuat **explicit intent** — hanya Activity kita
  yang bisa menanganinya, tidak ada app chooser yang muncul.
- `Uri.encode()` wajib. `referenceId` yang mengandung `/` atau spasi akan merusak pencocokan pola
  path dan deep link gagal senyap.

## 7.4 Menyalurkan intent ke NavController

`NavHost` dari navigation-compose membaca intent Activity saat pertama kali dibuat, jadi kasus
**app tertutup** sudah tertangani otomatis. Yang belum: **app sudah berjalan** — di situ intent
datang lewat `onNewIntent()`.

Di `HomeActivityCompose`:

```kotlin
@AndroidEntryPoint
class HomeActivityCompose : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()
    private val pendingDeepLink = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyBcafTest1Theme {
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val deepLink by pendingDeepLink.collectAsStateWithLifecycle()

                if (authState.isRestoringSession) {
                    SessionRestoringIndicator()
                } else {
                    AppRoot(
                        startDestination = remember {
                            if (authState.isLoggedIn) MainRoute else AuthGraph
                        },
                        isLoggedIn = authState.isLoggedIn,
                        isLoggedOut = authState.isLoggedOut,
                        onLogout = authViewModel::logout,
                        pendingDeepLink = deepLink,
                        onDeepLinkHandled = { pendingDeepLink.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = intent
    }
}
```

Di `AppRoot`:

```kotlin
@Composable
fun AppRoot(
    startDestination: AppRoute,
    isLoggedIn: Boolean,
    isLoggedOut: Boolean,
    onLogout: () -> Unit,
    pendingDeepLink: Intent?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(isLoggedIn, isLoggedOut) { /* seperti sekarang */ }

    LaunchedEffect(pendingDeepLink, isLoggedIn) {
        val intent = pendingDeepLink ?: return@LaunchedEffect
        if (!isLoggedIn) return@LaunchedEffect

        navController.handleDeepLink(intent)
        onDeepLinkHandled()
    }

    AppNavHost(/* ... */)
}
```

`handleDeepLink()` mencocokkan URI intent dengan seluruh `navDeepLink` yang terdaftar di graph dan
melakukan navigasi berikut back stack-nya. Tidak perlu `when` manual untuk memetakan tipe ke route —
pemetaan itu sudah hidup di deklarasi masing-masing destination.

## 7.5 Deep link saat user belum login

Ini kasus yang paling sering terlewat: notifikasi diketuk, session sudah kedaluwarsa, app membuka
layar Login, lalu setelah login user mendarat di Home — bukan di detail yang dia klik.

Guard `if (!isLoggedIn) return@LaunchedEffect` di §7.4 sudah menahan navigasi, dan karena
`pendingDeepLink` **tidak** dibersihkan pada cabang itu, `LaunchedEffect` berjalan ulang begitu
`isLoggedIn` berubah `true`. Intent tersimpan, deep link dijalankan setelah login. Itu sebabnya
`isLoggedIn` ikut menjadi key `LaunchedEffect`.

Batas yang diterima: intent tertunda hanya bertahan selama proses hidup. Kalau user menutup app di
layar login, deep link hilang. Untuk v1 ini dapat diterima — mempertahankannya butuh penyimpanan
persisten dan aturan kedaluwarsa tersendiri.

## 7.6 Menguji deep link tanpa mengirim push

```bash
adb shell am start -a android.intent.action.VIEW \
  -d "bcaf://transaction/A-10293" \
  com.masesas.exercise.bcaf_test_1
```

Kalau perintah ini membuka layar detail yang benar, sisi navigasi sudah beres dan masalah apa pun
setelahnya ada di payload atau di `PushNotifier` — bukan di navigasi. Lakukan tes ini **sebelum**
mulai menebak-nebak di sisi FCM.

## 7.7 Matriks perilaku yang harus diuji

| Kondisi app | Yang diketuk | Hasil yang benar |
|---|---|---|
| Tertutup, sudah login | Notifikasi transaksi | Buka langsung detail, back → Main |
| Background, sudah login | Notifikasi transaksi | Detail tampil, tidak ada Activity kedua |
| Foreground di tab Home | Notifikasi transaksi | Pindah ke detail |
| Tertutup, session kedaluwarsa | Notifikasi transaksi | Login dulu → setelah login mendarat di detail |
| Apa pun | Notifikasi `GENERAL` | Buka Main, tidak crash |
| Payload `referenceId` kosong | Notifikasi transaksi | Buka Main, tidak crash |

## 7.8 Checklist bagian ini

- [ ] `intent-filter` skema `bcaf` terdaftar di `HomeActivityCompose`
- [ ] `launchMode="singleTop"`
- [ ] `referenceId` di-`Uri.encode()`
- [ ] `onNewIntent()` memanggil `setIntent()` **dan** meneruskan ke NavController
- [ ] Deep link tertunda dijalankan setelah login, bukan dibuang
- [ ] `adb am start` dengan URI membuka layar yang benar
- [ ] Enam baris matriks §7.7 lolos di device

---

Sebelumnya: [06 — Registrasi Token](06-registrasi-token-ke-backend.md) ·
Lanjut: [08 — Mengirim dari Backend](08-kirim-dari-backend.md)
