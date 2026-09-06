# Jetpack Compose UI Slicing Handbook

> Referensi praktis untuk menerjemahkan Figma dan design system menjadi UI Android dengan Jetpack Compose dan Material 3.

## Cara memakai handbook

Gunakan handbook ini dengan tiga mode:

1. **Mencari API:** buka [01 — API Overview](01-api-overview.md) atau [09 — Cheat Sheet](09-cheat-sheet-priority.md).
2. **Melakukan slicing:** mulai dari [06 — Workflow Figma ke Compose](06-slicing-workflow.md), lalu buka bab komponen yang relevan.
3. **Mereview implementasi:** gunakan checklist pada [08 — Quality, Accessibility, dan Preview](08-quality-accessibility-preview.md).

## Daftar isi

| Bab | Isi utama |
|---|---|
| [01 — API Overview](01-api-overview.md) | Tabel besar komponen, frequency, kapan digunakan, kapan dihindari, alternatif, dan tier prioritas. |
| [02 — Layout, Text, dan Image](02-layout-text-image.md) | `Row`, `Column`, lazy layout, grid, pager, text styling, link modern, image lokal dan Coil. |
| [03 — Actions, Form, dan Selection](03-actions-form-selection.md) | Semua button, FAB, text field modern, password, keyboard, checkbox, radio, switch, slider, chip, segmented button. |
| [04 — Container, Navigation, dan Feedback](04-container-navigation-feedback.md) | Card, Surface, app bar, Scaffold, navigation visual, dialog, sheet, snackbar, progress, menu, badge, divider, tooltip, pull-to-refresh. |
| [05 — Modifier dan Design System](05-modifier-design-system.md) | Handbook Modifier, modifier order, arrangement, alignment, shape, color, typography, dan spacing token. |
| [06 — Workflow Figma ke Compose](06-slicing-workflow.md) | Workflow slicing, mapping Figma, component decomposition, state hoisting, dan decision tree. |
| [07 — Common Slicing Patterns](07-common-slicing-patterns.md) | 15 pola layar yang paling sering ditemukan saat slicing. |
| [08 — Quality, Accessibility, dan Preview](08-quality-accessibility-preview.md) | Anti-pattern, performance, semantics, touch target, preview matrix, testing, dan struktur proyek. |
| [09 — Cheat Sheet dan Priority](09-cheat-sheet-priority.md) | Referensi satu layar dan Tier 1–4. |
| [10 — Loan Product Detail](10-loan-product-detail-case-study.md) | Studi kasus lengkap dari analisis design sampai implementasi Compose. |

## Prinsip kerja utama

```text
Design tokens -> Primitive components -> Reusable components -> Screen
                                                           ↑
State holder -> immutable UI state ------------------------+
UI event -------------------------------------------------> state holder
```

- UI adalah fungsi dari state; hindari mengubah widget secara imperatif.
- Composable content sebaiknya stateless. Hoist state ke pemilik terendah yang membutuhkan state tersebut.
- Screen-level business state biasanya berada di `ViewModel`; state elemen UI sederhana dapat tetap lokal.
- Setiap reusable composable menerima `modifier: Modifier = Modifier` dan meneruskannya ke root UI node.
- Gunakan design token, bukan warna, typography, radius, dan spacing hardcoded yang tersebar.
- Gunakan komponen Material 3 ketika semantiknya cocok; gunakan Foundation/custom UI jika design memang berbeda.
- Buat keputusan adaptive berdasarkan ukuran **window**, bukan nama perangkat.
- Accessibility dan state seperti loading, error, empty, selected, focused, serta disabled adalah bagian dari slicing.

## Status API dan baseline

Handbook ini ditinjau pada **4 September 2026**. Contoh memprioritaskan API stabil Material 3 dan Foundation yang sesuai untuk proyek baru.

| Label | Arti |
|---|---|
| **Preferred** | Pilihan awal untuk proyek baru jika requirement cocok. |
| **Alternative** | Valid, tetapi dipakai karena design atau arsitektur tertentu. |
| **Experimental** | Periksa annotation dan release notes versi dependency yang dipakai; isolasi pemakaiannya. |
| **Legacy/deprecated** | Jangan dipilih untuk kode baru; hanya relevan saat migrasi. |

Catatan penting:

- Material 3 `1.4.0` merupakan rilis stabil saat tinjauan handbook. Gunakan Compose BOM proyek untuk menyelaraskan dependency, bukan menyalin versi secara terpisah tanpa pemeriksaan. Lihat [Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3).
- State-based `TextField`, `TextFieldState`, `InputTransformation`, `OutputTransformation`, `SecureTextField`, dan `OutlinedSecureTextField` adalah pendekatan modern. Untuk dependency yang belum mendukung overload ini, value-based `value/onValueChange` masih valid. Lihat [Configure text fields](https://developer.android.com/develop/ui/compose/text/user-input).
- `BasicTextField2` adalah nama pengembangan lama; API state-based modern bernama `BasicTextField`. Lihat [Compose Foundation release notes](https://developer.android.com/jetpack/androidx/releases/compose-foundation).
- `ClickableText` telah deprecated. Gunakan `Text` dengan `AnnotatedString` dan `LinkAnnotation`. Lihat [Text interactions](https://developer.android.com/develop/ui/compose/text/user-interactions).
- `Divider` versi lama digantikan oleh `HorizontalDivider` dan `VerticalDivider` pada Material 3.
- `ConstraintLayout` tetap tersedia, tetapi dokumentasi Android sekarang merekomendasikan layout Compose lain untuk mayoritas kasus. Hierarki `Row`/`Column` tidak memiliki penalti seperti nested View lama. Lihat [ConstraintLayout in Compose](https://developer.android.com/develop/ui/compose/layouts/constraintlayout).
- `AsyncImage` bukan bagian dari AndroidX Compose; contoh handbook memakai Coil 3. Pilih versi lewat katalog dependency proyek. Lihat [Coil Compose](https://coil-kt.github.io/coil/compose/).
- Untuk proyek Compose-only baru, Navigation 3 adalah opsi Compose-first yang stabil. Untuk aplikasi existing, evaluasi biaya migrasi dan dukungan requirement sebelum mengganti Navigation Compose 2. Lihat [Navigation 3](https://developer.android.com/guide/navigation/navigation-3).
- `currentWindowAdaptiveInfoV2()` mulai menggantikan `currentWindowAdaptiveInfo()` untuk dukungan breakpoint Large dan Extra Large secara default. Karena evolusi library adaptive cepat, periksa API pada versi yang benar-benar dipakai. Lihat [API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/adaptive/currentWindowAdaptiveInfoV2.composable).

## Konvensi contoh

- Contoh berfokus pada struktur API sehingga import sering dihilangkan.
- Semua teks yang tampil ke pengguna seharusnya dipindahkan ke string resources pada production.
- Warna dan dimensi literal di contoh kecil hanya untuk menunjukkan mekanisme. Studi kasus lengkap menggunakan token.
- Callback diberi nama berdasarkan niat, misalnya `onApplyFilter`, bukan implementasi seperti `onButtonClick`.
- Composable yang tidak memiliki state internal tetap dapat di-preview dan diuji tanpa `ViewModel`.

## Sumber primer

- [Material components in Compose](https://developer.android.com/develop/ui/compose/components)
- [Compose layouts](https://developer.android.com/develop/ui/compose/layouts)
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
- [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers)
- [Adaptive apps](https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps)
- [Accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics)
- [Compose testing](https://developer.android.com/develop/ui/compose/testing)
