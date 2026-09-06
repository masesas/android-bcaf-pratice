# 03 — Actions, Form, dan Selection

[← Layout, Text, dan Image](02-layout-text-image.md) · [Index](README.md) · [Container, Navigation, dan Feedback →](04-container-navigation-feedback.md)

## 1. Button components

### Decision table

| Kondisi UI | Button yang direkomendasikan | Catatan |
|---|---|---|
| Primary CTA | `Button` | Umumnya satu CTA dengan emphasis tertinggi dalam satu area. |
| Primary/significant tetapi lebih lembut | `FilledTonalButton` | Cocok untuk “Tambahkan ke keranjang” atau CTA sekunder kuat. |
| Perlu separation dari background berpola | `ElevatedButton` | Jangan memakai elevation hanya sebagai dekorasi. |
| Secondary CTA berdampingan dengan primary | `OutlinedButton` | Contoh: “Nanti” di samping “Lanjutkan”. |
| Tertiary/low emphasis | `TextButton` | Cocok untuk dialog action, “Lihat detail”, atau link-like action. |
| Destructive action | Button sesuai hierarchy + warna `error` | “Hapus” tidak otomatis selalu filled; sesuaikan tingkat risiko dan konfirmasi. |
| Icon-only action | `IconButton` | Wajib memiliki `contentDescription`; tambahkan tooltip bila makna tidak jelas. |
| Icon-only dengan emphasis | Filled/tonal/outlined icon button | Gunakan selected/emphasis secara konsisten. |
| Satu floating primary action | FAB | Hindari beberapa FAB yang bersaing. |
| FAB butuh label | `ExtendedFloatingActionButton` | Label membantu discoverability. |

### Button gallery

Contoh berikut memperlihatkan implementasi setiap varian button dalam satu tempat:

```kotlin
@Composable
fun ButtonGallery(
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onPrimary) { Text("Lanjutkan") }

        FilledTonalButton(onClick = onPrimary) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Tambah ke keranjang")
        }

        ElevatedButton(onClick = onPrimary) { Text("Buka penawaran") }
        OutlinedButton(onClick = onSecondary) { Text("Bandingkan") }
        TextButton(onClick = onSecondary) { Text("Lihat detail") }

        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { Text("Hapus akun") }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onFavorite) {
                Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Tambahkan ke favorit")
            }
            FilledIconButton(onClick = onFavorite) {
                Icon(Icons.Default.Favorite, contentDescription = "Favorit")
            }
            FilledTonalIconButton(onClick = onFavorite) {
                Icon(Icons.Outlined.Bookmark, contentDescription = "Simpan")
            }
            OutlinedIconButton(onClick = onFavorite) {
                Icon(Icons.Outlined.Share, contentDescription = "Bagikan")
            }
        }
    }
}
```

### FAB variants

```kotlin
@Composable
fun FabGallery(onCreate: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallFloatingActionButton(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = "Tambah")
        }
        FloatingActionButton(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = "Tambah")
        }
        LargeFloatingActionButton(onClick = onCreate) {
            Icon(Icons.Default.Add, contentDescription = "Tambah")
        }
        ExtendedFloatingActionButton(
            onClick = onCreate,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Buat baru") },
        )
    }
}
```

### Pedoman button

- Gunakan kata kerja yang spesifik: “Ajukan pinjaman”, bukan “OK”.
- State loading harus mencegah double submit dan mempertahankan ukuran button.
- Jangan mengandalkan warna saja untuk membedakan aksi destructive.
- Jangan membuat semua aksi sebagai filled button; hierarchy visual harus merefleksikan hierarchy keputusan.
- Untuk icon + label dalam `Button`, icon dekoratif memakai `contentDescription = null` karena label sudah menjelaskan aksi.

```kotlin
@Composable
fun SubmitButton(isLoading: Boolean, onSubmit: () -> Unit) {
    Button(
        onClick = onSubmit,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(if (isLoading) "Memproses" else "Ajukan sekarang")
    }
}
```

Referensi: [Material 3 buttons](https://developer.android.com/develop/ui/compose/components/button).

## 2. Input dan form

### Memilih text field

| Requirement | Gunakan | Alasan |
|---|---|---|
| Form Material filled | `TextField` | Emphasis filled dan slot lengkap. |
| Form Material outlined | `OutlinedTextField` | Boundary jelas pada form di surface netral. |
| Input custom total | `BasicTextField` state-based | Decoration dan layout sepenuhnya milik design system. |
| Password modern | `SecureTextField` / `OutlinedSecureTextField` | Obfuscation dan security defaults. |
| Compatibility dependency lama | Value-based `TextField(value, onValueChange)` | Tetap valid; rencanakan migrasi tanpa memaksakan arsitektur. |

### Catatan API modern

- **Preferred untuk kode baru:** overload state-based dengan `TextFieldState` dan `rememberTextFieldState()`.
- `InputTransformation` memfilter/mengubah input sebelum tersimpan.
- `OutputTransformation` mengubah tampilan tanpa mengubah data asli.
- `TextFieldLineLimits` menggantikan kombinasi ambigu `singleLine/minLines/maxLines` pada state-based API.
- `BasicTextField2` telah berganti nama menjadi state-based `BasicTextField`.
- Jika versi Material 3 proyek belum menyediakan API tersebut, gunakan overload value-based yang stabil pada versi itu. Jangan mencampur signature dari dokumentasi dengan dependency lama.

### Login form modern

```kotlin
@Composable
fun LoginForm(
    emailState: TextFieldState,
    passwordState: TextFieldState,
    emailError: String?,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            state = emailState,
            label = { Text("Email") },
            placeholder = { Text("nama@perusahaan.com") },
            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
            isError = emailError != null,
            supportingText = emailError?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedSecureTextField(
            state = passwordState,
            label = { Text("Kata sandi") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = {
                focusManager.clearFocus()
                onSubmit()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        SubmitButton(isLoading = isSubmitting, onSubmit = onSubmit)
    }
}
```

Signature `onKeyboardAction` dapat mengikuti versi Foundation/Material 3 proyek. Pada value-based API, gunakan `KeyboardActions(onDone = { ... })`.

### Compatibility: value-based form

```kotlin
@Composable
fun LoginFormValueBased(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        singleLine = true,
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Kata sandi") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        singleLine = true,
    )
}
```

### Search, amount, dan multiline

```kotlin
@Composable
fun SearchField(
    state: TextFieldState,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        state = state,
        placeholder = { Text("Cari produk") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (state.text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Hapus pencarian")
                }
            }
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun AmountField(state: TextFieldState) {
    OutlinedTextField(
        state = state,
        label = { Text("Jumlah pinjaman") },
        prefix = { Text("Rp") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        inputTransformation = InputTransformation.maxLength(12).then {
            if (!asCharSequence().all(Char::isDigit)) revertAllChanges()
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun NotesField(state: TextFieldState) {
    OutlinedTextField(
        state = state,
        label = { Text("Catatan") },
        supportingText = { Text("Maksimal 500 karakter") },
        inputTransformation = InputTransformation.maxLength(500),
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3, maxHeightInLines = 6),
        modifier = Modifier.fillMaxWidth(),
    )
}
```

### Custom input dengan `BasicTextField`

```kotlin
@Composable
fun SearchPill(state: TextFieldState, modifier: Modifier = Modifier) {
    BasicTextField(
        state = state,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        decorator = { innerTextField ->
            Row(
                modifier = modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null)
                Box(Modifier.weight(1f)) {
                    if (state.text.isEmpty()) Text("Cari", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    innerTextField()
                }
            }
        },
    )
}
```

### Struktur form yang baik

1. Satu label yang jelas untuk setiap field.
2. Supporting text menjelaskan format sebelum error terjadi; error text menjelaskan cara memperbaiki.
3. Keyboard type dan IME action sesuai urutan input.
4. Validasi lokal cepat dapat berlangsung saat input; validasi server tetap di state holder/domain.
5. Jangan menampilkan error merah sebelum pengguna sempat berinteraksi, kecuali submit sudah dicoba.
6. Pertahankan input saat configuration/process recreation sesuai kebutuhan produk.
7. Fokus otomatis hanya ketika membantu; jangan selalu membuka keyboard saat screen muncul.

Referensi: [Configure text fields](https://developer.android.com/develop/ui/compose/text/user-input) dan [Migrate to state-based text fields](https://developer.android.com/develop/ui/compose/text/migrate-state-based).

## 3. Selection controls

### Perbandingan

| Komponen | Model state | Use case | Hindari jika |
|---|---|---|---|
| `Checkbox` | Boolean per opsi; bisa multiple | Agreement, checklist, multi-select | Tepat satu opsi wajib |
| `RadioButton` | Satu selected value | Metode pembayaran, pilihan tenor tunggal | On/off setting |
| `Switch` | Boolean yang langsung berlaku | Setting notification/dark mode | Perubahan baru berlaku setelah submit |
| `Slider` | Satu nilai range | Jumlah/volume/rating | Butuh angka presisi tinggi |
| `RangeSlider` | Closed range | Filter harga min–max | Hanya satu batas |
| `FilterChip` | Boolean per filter | Category/status filters | Banyak filter kompleks |
| `InputChip` | Entity selected | Tag, penerima, attachment | Sekadar filter |
| `AssistChip` | Event | Shortcut/saran aksi | Primary action |
| `SuggestionChip` | Event dari recommendation | Suggested query/reply | Opsi wajib |
| `SegmentedButton` | Single/multi 2–5 opsi | View mode, period, sorting | Opsi banyak atau label panjang |

### Checkbox, radio, dan switch

Gunakan parent row sebagai target klik agar label ikut interaktif; berikan semantics selection/toggle yang benar.

```kotlin
@Composable
fun AgreementRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(Modifier.width(8.dp))
        Text("Saya menyetujui syarat dan ketentuan")
    }
}

@Composable
fun TenorOptions(options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        options.forEach { months ->
            Row(
                Modifier.fillMaxWidth().selectable(
                    selected = months == selected,
                    role = Role.RadioButton,
                    onClick = { onSelect(months) },
                ).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = months == selected, onClick = null)
                Text("$months bulan", Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun NotificationSetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Notifikasi transaksi", style = MaterialTheme.typography.titleMedium)
            Text("Terima pemberitahuan setiap transaksi", style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}
```

### Slider dan range

```kotlin
@Composable
fun AmountSlider(value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text("Plafond: Rp${value.toInt()} juta")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 5f..100f,
            steps = 18,
        )
    }
}

@Composable
fun PriceRange(value: ClosedFloatingPointRange<Float>, onChange: (ClosedFloatingPointRange<Float>) -> Unit) {
    RangeSlider(
        value = value,
        onValueChange = onChange,
        valueRange = 0f..50_000_000f,
    )
}
```

### Chip dan segmented button

```kotlin
@Composable
fun ChipExamples(selected: Boolean, onSelectedChange: (Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(onClick = {}, label = { Text("Gunakan lokasi") })
        FilterChip(selected, { onSelectedChange(!selected) }, label = { Text("Aktif") })
        InputChip(selected = true, onClick = {}, label = { Text("Jakarta") }, trailingIcon = {
            Icon(Icons.Default.Close, contentDescription = "Hapus Jakarta")
        })
        SuggestionChip(onClick = {}, label = { Text("Cicilan ringan") })
    }
}

@Composable
fun PeriodSelector(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label) },
            )
        }
    }
}

@Composable
fun MultiFormatSelector(
    options: List<String>,
    checked: Set<Int>,
    onCheckedChange: (Int, Boolean) -> Unit,
) {
    MultiChoiceSegmentedButtonRow {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                checked = index in checked,
                onCheckedChange = { onCheckedChange(index, it) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label) },
            )
        }
    }
}
```

Referensi: [Chips](https://developer.android.com/develop/ui/compose/components/chip) dan [Segmented buttons](https://developer.android.com/develop/ui/compose/components/segmented-button).
