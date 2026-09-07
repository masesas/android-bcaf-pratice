# Dokumentasi

## Arsitektur

- [Arsitektur Aplikasi](architecture.md) — peta layer dan aturan dependency, alur data offline-first, keputusan desain beserta alternatif yang ditolak, perbandingan RecyclerView vs LazyColumn, strategi test, dan batasan yang diketahui.

## Jetpack Compose

- [Jetpack Compose UI Slicing Handbook](jetpack-compose-ui-handbook/README.md) — handbook lengkap UI Components API, workflow Figma ke Compose, decision table, pola slicing, quality checklist, dan studi kasus.
- [Panduan Belajar Jetpack Compose](jetpack-compose-learning-guide.md) — roadmap belajar dan referensi API utama yang lebih ringkas.

## Kotlin Coroutines

- [Kotlin Coroutines Handbook (Android Modern)](kotlin-coroutines-handbook/README.md) — referensi lengkap coroutine per layer (Repository, UseCase, ViewModel, Activity/Fragment, Compose), dispatcher, scope, Flow, error handling, cancellation, testing, anti-pattern, dan decision guide.

## Database

- [Room Migration Handbook](room-migration-handbook.md) — cara kerja schema export, workflow saat menambah/mengubah entity, tiga pola migrasi (tambah tabel, tambah kolom, ubah tipe kolom), strategi testing termasuk pembuktian test bisa gagal, dan troubleshooting.

## Kontrak API

- [`api-contract/`](api-contract/) — JSON Schema response endpoint (`get-list-loan.json`, `pageable-request.json`).
