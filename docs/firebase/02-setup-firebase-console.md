# 02 — Setup Firebase Console & Service Account

Target akhir bagian ini: file `google-services.json` ada di `app/`, dan backend punya kredensial
service account untuk mengirim pesan.

## 2.1 Membuat project Firebase

1. Buka <https://console.firebase.google.com> → **Add project**.
2. Nama project, mis. `bcaf-test-1`. Nama ini menentukan **Project ID** (mis. `bcaf-test-1-4f2a9`) —
   catat, nanti dipakai di URL endpoint HTTP v1.
3. Google Analytics boleh dimatikan; FCM tidak membutuhkannya. (Aktifkan hanya bila ingin memakai
   fitur audiens/campaign di Console.)

> **Satu project atau dua?** Praktik yang disarankan: satu Firebase project untuk `debug`/staging,
> satu lagi untuk `release`/production. Pesan uji coba tidak akan pernah nyasar ke device user asli.
> Cara memasang dua `google-services.json` ada di [03 §3.4](03-setup-gradle-dan-manifest.md#34-google-servicesjson-per-build-type).

## 2.2 Mendaftarkan app Android

1. Di halaman Project Overview → ikon Android.
2. **Android package name** — wajib sama persis dengan `applicationId` di `app/build.gradle.kts`:

   ```
   com.masesas.exercise.bcaf_test_1
   ```

   Bukan `namespace`. Kalau nanti dipasang `applicationIdSuffix = ".debug"` untuk build debug, itu
   adalah **package berbeda** dan butuh entri app tersendiri di Firebase.
3. **App nickname** — bebas, hanya label di Console.
4. **SHA-1** — **opsional untuk FCM.** Wajib hanya kalau memakai Firebase Authentication (Google
   Sign-In / phone auth) atau Dynamic Links. Kalau tetap ingin mengisi:

   ```bash
   ./gradlew signingReport
   ```

   Ambil nilai SHA-1 dari varian yang relevan (`debug` memakai `~/.android/debug.keystore`).
5. Download **`google-services.json`** → simpan ke `app/google-services.json`.

## 2.3 Isi google-services.json — apa yang boleh bocor

File ini **bukan rahasia**. Isinya API key client, project number, dan app id — semuanya juga
tertanam di APK yang bisa dibongkar siapa pun. Aman untuk di-commit ke repo.

Yang **rahasia** adalah service account JSON di §2.4. File itu **tidak boleh** masuk ke repo app
Android dan tidak boleh dibundel ke APK. Tambahkan ke `.gitignore` backend:

```gitignore
**/firebase-service-account*.json
```

## 2.4 Kredensial pengiriman untuk backend (HTTP v1)

FCM legacy API (`https://fcm.googleapis.com/fcm/send` dengan header `Authorization: key=<SERVER_KEY>`)
sudah **dimatikan**. Satu-satunya jalur yang berlaku sekarang adalah **HTTP v1 + OAuth2**.

Langkah:

1. Console → ⚙️ **Project settings** → tab **Service accounts**.
2. **Generate new private key** → unduh JSON. Isinya kira-kira:

   ```json
   {
     "type": "service_account",
     "project_id": "bcaf-test-1-4f2a9",
     "private_key_id": "...",
     "private_key": "-----BEGIN PRIVATE KEY-----\n...",
     "client_email": "firebase-adminsdk-xxxxx@bcaf-test-1-4f2a9.iam.gserviceaccount.com"
   }
   ```

3. Simpan di secret manager backend (Vault / AWS Secrets Manager / env var berisi JSON base64).
   **Jangan** taruh di repo, jangan taruh di image Docker sebagai file plaintext.
4. Bila kunci pernah bocor: Console → Service accounts → **Manage service account permissions** →
   IAM → hapus key lama, terbitkan baru. Rotasi ini instan dan tidak memutus app yang sudah ter-install.

## 2.5 Checklist bagian ini

- [ ] Project Firebase dibuat, **Project ID** dicatat
- [ ] App Android terdaftar dengan package name persis `com.masesas.exercise.bcaf_test_1`
- [ ] `app/google-services.json` ada di working tree
- [ ] Service account JSON tersimpan di secret manager backend, tidak di repo
- [ ] (Bila dipakai) Firebase project terpisah untuk debug dan release

---

Sebelumnya: [01 — Konsep & Arsitektur](01-konsep-dan-arsitektur.md) ·
Lanjut: [03 — Gradle & Manifest](03-setup-gradle-dan-manifest.md)
