# BLUEPRINT LENGKAP APLIKASI ANDROID NATIVE — GEMPALA IURAN

> Dokumen implementasi untuk AI coding agent. Ikuti keputusan teknis, struktur data, alur, dan acceptance criteria di bawah ini. Jangan mengganti teknologi inti tanpa instruksi baru.

---

## 0. Instruksi Utama untuk AI Agent

Bangun aplikasi Android native bernama **Gempala Iuran** untuk mencatat penarikan iuran kegiatan warga yang bersifat tidak rutin, seperti HUT 17 Agustus, Maulid Nabi, kerja bakti, pembangunan fasilitas, santunan, dan kegiatan sosial lainnya.

Keputusan teknis yang **wajib dipertahankan**:

1. Gunakan **Kotlin** dan **Jetpack Compose Material 3**.
2. Gunakan **Cloud Firestore saja sebagai backend/database utama**.
3. Jangan menggunakan Firebase Authentication.
4. Jangan membuat backend tambahan, Cloud Functions, REST API, Supabase, MySQL, atau server lain.
5. Jangan menggunakan Room sebagai sumber data bisnis.
6. Gunakan DataStore hanya untuk sesi lokal, device ID, nomor urut lokal, preferensi, dan waktu sinkronisasi terakhir.
7. Aplikasi harus **offline-first untuk proses penarikan oleh petugas**.
8. Firestore persistence menjadi mekanisme cache dan antrean write offline.
9. Transaksi pembayaran wajib memakai pola **ledger append-only**.
10. Jangan menyimpan `totalPaid` sebagai sumber kebenaran utama.
11. Jangan menggunakan `.add()` ketika membuat transaksi.
12. Gunakan `.document(transactionId).set(...)` dengan ID transaksi yang dibuat sebelum write.
13. Retry dan sinkronisasi manual tidak boleh membangkitkan transaksi baru.
14. Transaksi yang salah tidak boleh dihapus atau diedit; koreksi memakai transaksi `REVERSAL`.
15. Login admin menggunakan kredensial hardcoded yang sudah ditentukan.
16. Akun petugas dibuat oleh admin dan disimpan di Firestore.
17. Semua nominal uang menggunakan `Long`, bukan `Double` atau `Float`.
18. Zona waktu operasional aplikasi adalah `Asia/Jakarta`.
19. Target minimum Android: **API 24** atau lebih tinggi.
20. UI harus modern, ramah, ringan, jelas, dan nyaman dipakai petugas di lapangan.

---

## 1. Identitas Produk

| Atribut | Nilai |
|---|---|
| Nama aplikasi | Gempala Iuran |
| Platform | Android Native |
| Bahasa | Kotlin |
| UI toolkit | Jetpack Compose + Material 3 |
| Arsitektur | MVVM + Repository + Use Case |
| Backend/database | Cloud Firestore |
| Mode data | Offline-first dengan sinkronisasi otomatis dan manual |
| Pengguna | Admin dan Petugas |
| Lingkup | Internal RT, RW, GEMPALA, atau organisasi warga |
| Zona waktu | Asia/Jakarta |
| Mata uang | Rupiah/IDR |

### 1.1 Deskripsi Singkat

Gempala Iuran adalah aplikasi pencatatan iuran berbasis kegiatan. Setiap kegiatan mempunyai tanggal mulai, tenggat, target nominal, daftar warga peserta, dan petugas yang ditugaskan. Warga dapat membayar sekaligus atau mencicil beberapa kali. Setiap cicilan dicatat sebagai transaksi tersendiri dan dapat dilihat melalui riwayat serta kalender pembayaran.

### 1.2 Masalah yang Diselesaikan

- Pencatatan manual mudah hilang, ganda, atau salah jumlah.
- Petugas sering melakukan penarikan di lokasi dengan internet tidak stabil.
- Pembayaran warga dapat dilakukan bertahap.
- Admin membutuhkan rekap kegiatan, warga, petugas, dan transaksi.
- Beberapa petugas dapat mencatat pembayaran secara bersamaan.
- Sinkronisasi ulang tidak boleh menambah transaksi duplikat.

---

## 2. Tujuan Produk

1. Memudahkan petugas mencatat pembayaran saat mendatangi warga.
2. Membuat pembayaran tetap dapat dicatat ketika offline.
3. Menyinkronkan data otomatis setelah internet tersedia.
4. Menyediakan tombol sinkronisasi manual yang aman ditekan berulang kali.
5. Mencegah duplikasi akibat double tap, retry, restart aplikasi, atau reconnect.
6. Menghindari overwrite ketika dua petugas mencatat warga yang sama.
7. Menampilkan progres dana per warga dan per kegiatan.
8. Menampilkan progres partisipasi warga secara terpisah dari progres dana.
9. Menyediakan riwayat pembayaran harian dan kalender.
10. Mempertahankan jejak audit ketika transaksi dibatalkan.

---

## 3. Batasan dan Risiko yang Diterima

Aplikasi tidak menggunakan Firebase Authentication. Oleh karena itu:

- Firestore Security Rules tidak dapat membedakan admin dan petugas berdasarkan identitas server.
- Pembatasan role admin dan petugas hanya dijaga oleh aplikasi/UI.
- Seseorang yang membongkar APK atau memanggil Firestore secara langsung berpotensi melewati pembatasan UI.
- Username dan hash password petugas dapat dibaca oleh klien apabila Firestore read dibuka.
- Kredensial admin hardcoded dapat ditemukan melalui reverse engineering walaupun kode di-obfuscate.

Aplikasi ini dikategorikan sebagai **trusted internal app** untuk perangkat pengurus yang dipercaya. Jangan mengklaim sistem ini memiliki keamanan setara aplikasi publik atau aplikasi keuangan produksi.

---

## 4. Ruang Lingkup MVP

### 4.1 Termasuk dalam MVP

- Splash screen dan session routing.
- Login admin hardcoded.
- Login petugas dari Firestore.
- CRUD data warga.
- CRUD petugas.
- CRUD kegiatan.
- Penentuan warga peserta kegiatan.
- Penugasan petugas ke kegiatan.
- Target default dan target khusus per warga.
- Daftar dan pencarian warga.
- Filter status pembayaran.
- Input pembayaran sebagian atau lunas.
- Metode pembayaran.
- Riwayat transaksi.
- Kalender pembayaran.
- Progress per warga.
- Progress dana kegiatan.
- Progress partisipasi kegiatan.
- Dashboard admin.
- Dashboard petugas.
- Sinkronisasi otomatis.
- Sinkronisasi manual.
- Status pending/synced.
- Reversal transaksi.
- Laporan dalam aplikasi.

### 4.2 Tidak Wajib pada MVP

- Firebase Authentication.
- Push notification.
- QRIS payment gateway.
- WhatsApp API.
- Cloud Functions.
- Web admin terpisah.
- Multi-organisasi/multi-tenant.
- Ekspor Excel/PDF.
- Cetak kuitansi Bluetooth.
- Tanda tangan digital.
- Upload foto bukti transfer.

Fitur di luar MVP hanya boleh dibuat setelah fitur inti stabil.

---

## 5. Peran Pengguna

## 5.1 Admin

Admin dapat:

- Login dengan kredensial hardcoded.
- Melihat dashboard keseluruhan.
- Membuat, mengedit, mengaktifkan, menyelesaikan, dan mengarsipkan kegiatan.
- Menentukan tanggal mulai dan tenggat.
- Menentukan target default per warga.
- Menentukan warga peserta.
- Menentukan target khusus warga tertentu.
- Membuat dan mengelola akun petugas.
- Menentukan petugas pada suatu kegiatan.
- Mengelola data warga.
- Melihat seluruh transaksi.
- Melihat laporan per kegiatan, warga, petugas, metode pembayaran, dan tanggal.
- Membuat reversal untuk transaksi yang salah.
- Melihat status sinkronisasi perangkat.

Admin tidak boleh menghapus permanen transaksi pembayaran.

## 5.2 Petugas

Petugas dapat:

- Login dengan akun yang dibuat admin.
- Melihat kegiatan aktif yang ditugaskan.
- Melihat daftar warga peserta kegiatan.
- Mencari warga berdasarkan nama, nomor rumah, blok, atau alamat.
- Memfilter warga berdasarkan status pembayaran.
- Membuka detail warga.
- Menambahkan pembayaran sebagian atau penuh.
- Memilih metode pembayaran.
- Menambahkan catatan.
- Melihat riwayat pembayaran warga.
- Melihat riwayat transaksi yang dibuat sendiri.
- Melihat transaksi pending.
- Menjalankan sinkronisasi manual.

Petugas tidak dapat melalui UI:

- Membuat atau mengubah kegiatan.
- Mengubah target warga.
- Mengelola petugas.
- Menghapus transaksi.
- Membuat reversal.
- Mengubah data master admin.

---

## 6. Login dan Sesi

## 6.1 Kredensial Admin

```text
Username: admin@gempala.com
Password: gempala2026
```

Validasi dilakukan di aplikasi:

```kotlin
private const val ADMIN_USERNAME = "admin@gempala.com"
private const val ADMIN_PASSWORD = "gempala2026"

fun validateAdminLogin(username: String, password: String): Boolean {
    return username.trim().lowercase() == ADMIN_USERNAME &&
        password == ADMIN_PASSWORD
}
```

Larangan:

- Jangan menampilkan kredensial admin pada UI.
- Jangan menulis password pada log.
- Jangan mengirim password ke Firestore.
- Jangan menyimpan password admin di DataStore.
- Aktifkan R8/ProGuard pada build release, tetapi jangan menganggap obfuscation sebagai keamanan absolut.

## 6.2 Akun Petugas

Collection:

```text
officers/{officerId}
```

Contoh:

```json
{
  "name": "Ahmad Fauzi",
  "username": "ahmad",
  "usernameNormalized": "ahmad",
  "passwordHash": "base64-hash",
  "passwordSalt": "base64-salt",
  "passwordIterations": 120000,
  "phone": "081234567890",
  "isActive": true,
  "assignedActivityIds": ["activity_17_agustus_2026"],
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp"
}
```

Ketentuan:

- Username wajib unik.
- Username dinormalisasi dengan `trim().lowercase()`.
- Username minimal 4 karakter dan tanpa spasi.
- Password minimal 6 karakter.
- Jangan menyimpan password plaintext.
- Gunakan `PBKDF2WithHmacSHA256` dengan salt acak per akun.
- Verifikasi password dilakukan di aplikasi.

## 6.3 Login Pertama dan Login Offline

- Login pertama petugas membutuhkan internet agar dokumen akun dapat diambil.
- Setelah login berhasil, simpan sesi petugas di DataStore.
- Jika sesi masih aktif, petugas dapat membuka aplikasi saat offline.
- Jangan mencoba login ulang ke Firestore setiap kali aplikasi dibuka apabila sesi lokal valid.
- Admin dapat menonaktifkan akun petugas. Perubahan tersebut berlaku setelah perangkat menerima data terbaru.

## 6.4 DataStore

DataStore hanya menyimpan:

```text
isLoggedIn
role
userId
userName
lastLoginAtEpochMs
deviceId
lastLocalSequence
lastSyncAtEpochMs
selectedActivityId
themePreference
```

Jangan menyimpan daftar warga, kegiatan, atau transaksi sebagai database kedua di DataStore.

---

## 7. Alur Utama Pengguna

## 7.1 Alur Admin Membuat Kegiatan

```text
Login Admin
  → Dashboard Admin
  → Tambah Kegiatan
  → Isi nama, deskripsi, tanggal mulai, tenggat, target default
  → Pilih warga peserta
  → Ubah target khusus jika diperlukan
  → Pilih petugas
  → Simpan sebagai Draft atau Aktifkan
  → Kegiatan tampil pada dashboard petugas yang ditugaskan
```

## 7.2 Alur Petugas Mencatat Pembayaran

```text
Login/Sesi Petugas
  → Dashboard Petugas
  → Pilih Kegiatan Aktif
  → Buka Daftar Warga
  → Cari/Pilih Warga
  → Detail Warga
  → Tambah Pembayaran
  → Isi nominal, metode, catatan
  → Simpan
  → Transaksi langsung muncul dari cache lokal
  → Jika online: Firestore mengonfirmasi
  → Jika offline: write tetap antre dan tersinkron saat online
```

## 7.3 Alur Koreksi Admin

```text
Admin membuka detail transaksi
  → Pilih Batalkan/Koreksi
  → Isi alasan
  → Sistem membuat transaksi REVERSAL bernilai negatif
  → Transaksi asli tetap ada
  → Total dihitung ulang dari seluruh ledger
```

---

## 8. Navigasi Aplikasi

## 8.1 Splash Screen

Fungsi:

- Menampilkan logo singkat.
- Memuat DataStore.
- Menentukan apakah sesi tersedia.
- Mengarahkan ke Login, Dashboard Admin, atau Dashboard Petugas.
- Tidak perlu menunggu internet.

## 8.2 Bottom Navigation Admin

```text
Dashboard | Kegiatan | Transaksi | Data | Akun
```

Menu `Data` berisi:

- Warga.
- Petugas.
- Laporan.

## 8.3 Bottom Navigation Petugas

```text
Beranda | Warga | Riwayat | Akun
```

## 8.4 Route Utama

```text
splash
login
admin/dashboard
admin/activities
admin/activity/create
admin/activity/{activityId}
admin/residents
admin/resident/{residentId}
admin/officers
admin/officer/{officerId}
admin/transactions
admin/transaction/{transactionId}
admin/reports
admin/profile
officer/dashboard
officer/activity/{activityId}/residents
officer/activity/{activityId}/resident/{residentId}
officer/payment/{activityId}/{residentId}
officer/history
officer/profile
```

---

## 9. Spesifikasi Layar

## 9.1 Login

Komponen:

- Logo Gempala Iuran.
- Judul aplikasi.
- Subtitle singkat.
- Segmented button `Admin` dan `Petugas`.
- Field username/email.
- Field password.
- Ikon tampilkan/sembunyikan password.
- Tombol `Masuk`.
- Indikator online/offline kecil.
- Teks: `Login pertama petugas membutuhkan internet.`

State:

- Idle.
- Loading.
- Invalid credentials.
- Officer inactive.
- Network required.
- Success.

## 9.2 Dashboard Admin

Tampilan modern dengan header gradient biru dan kartu putih.

### Header

- Sapaan: `Selamat datang, Admin`.
- Tanggal hari ini.
- Ikon notifikasi/status.

### Ringkasan Hari Ini

Gunakan grid 2 × 2:

- Total kegiatan aktif.
- Total target kegiatan aktif.
- Total terkumpul.
- Rata-rata progress dana.

### Kegiatan Aktif

Setiap card menampilkan:

- Ikon kegiatan.
- Nama kegiatan.
- Tenggat.
- Terkumpul / target.
- Progress bar.
- Persentase.
- Jumlah lunas, mencicil, dan belum bayar.
- Tombol/detail affordance.

### Menu Admin

- Kegiatan.
- Warga.
- Petugas.
- Laporan.

### Aktivitas Terbaru

- Nama warga.
- Nominal.
- Nama petugas.
- Metode.
- Waktu.
- Status sinkronisasi.

### Empty State

Jika belum ada kegiatan:

- Ilustrasi sederhana.
- Teks `Belum ada kegiatan iuran`.
- Tombol `Buat Kegiatan`.

## 9.3 Dashboard Petugas

Gunakan warna utama hijau atau biru-hijau agar terasa ramah dan berbeda dari admin.

### Header

- Sapaan: `Halo, {nama petugas}`.
- Role: `Petugas`.
- Ikon status/notification.

### Banner Sinkronisasi

Kondisi synced:

```text
✓ Semua data tersinkron
Terakhir: 12 Juli 2026, 10.45
```

Kondisi offline:

```text
Mode offline
3 transaksi tersimpan di perangkat
[Sinkronkan Sekarang]
```

Kondisi syncing:

```text
Menyinkronkan data…
```

### Card Kegiatan Aktif

- Nama kegiatan.
- Tenggat.
- Terkumpul / target.
- Progress dana.
- Jumlah warga sudah membayar.
- Jumlah warga mencicil.
- Jumlah warga belum bayar.
- Tombol `Buka Penarikan`.

### Ringkasan Pribadi

- Transaksi hari ini.
- Total nominal hari ini.
- Transaksi belum tersinkron.

### Menu Cepat

- Daftar Warga.
- Input Pembayaran.
- Riwayat Pembayaran.
- Sinkronisasi.

## 9.4 Daftar Kegiatan Admin

Filter:

```text
Semua | Draft | Aktif | Selesai | Arsip
```

Card kegiatan:

- Nama.
- Tanggal mulai.
- Tenggat.
- Status.
- Target.
- Terkumpul.
- Progress dana.
- Progress partisipasi.
- Jumlah petugas.

FAB: `Tambah Kegiatan`.

## 9.5 Form Kegiatan

Field:

- Nama kegiatan.
- Deskripsi.
- Tanggal mulai.
- Tenggat.
- Target default per warga.
- Izinkan pembayaran setelah tenggat.
- Pilih warga peserta.
- Pilih petugas.
- Status awal `DRAFT` atau `ACTIVE`.

Validasi:

- Nama wajib diisi.
- Target lebih besar dari nol.
- Tenggat tidak sebelum tanggal mulai.
- Minimal satu warga peserta.
- Minimal satu petugas ketika status `ACTIVE`.

Perilaku offline:

- Pembuatan/edit kegiatan adalah **online-preferred**.
- Jika offline, tampilkan peringatan dan nonaktifkan publish/activate untuk mengurangi bentrok master data.
- Draft lokal tidak perlu dibuat sebagai database terpisah.

## 9.6 Data Warga Admin

Fitur:

- Daftar warga.
- Search.
- Filter aktif/nonaktif.
- Tambah warga.
- Edit warga.
- Soft deactivate warga.

Field warga:

- Nama.
- Nomor rumah.
- Blok/dusun.
- Alamat.
- Nomor telepon opsional.
- Catatan opsional.

Jangan menghapus permanen warga yang sudah mempunyai histori transaksi.

## 9.7 Data Petugas Admin

Fitur:

- Daftar petugas.
- Tambah akun.
- Edit profil.
- Reset password.
- Aktif/nonaktif akun.
- Lihat kegiatan yang ditugaskan.
- Lihat total transaksi petugas.

## 9.8 Daftar Warga Petugas

Komponen:

- Header nama kegiatan.
- Search bar sticky.
- Filter status.
- Sort.
- Daftar card warga.

Filter:

```text
Semua | Belum Bayar | Mencicil | Lunas | Lebih Bayar
```

Sort:

```text
Nama A-Z | Nomor Rumah | Progress Terendah | Terakhir Bayar
```

Card warga:

- Nama.
- Nomor rumah/blok.
- Target.
- Terbayar.
- Sisa.
- Progress bar.
- Badge status.

## 9.9 Detail Warga dalam Kegiatan

Header:

- Nama warga.
- Nomor rumah.
- Alamat singkat.
- Nomor telepon opsional.

Summary card:

- Target.
- Total terbayar.
- Kekurangan atau kelebihan.
- Progress bar.
- Status.

Action:

- `Tambah Pembayaran`.
- `Lihat Riwayat`.

Riwayat singkat:

- Tiga transaksi terbaru.
- Status sync pada setiap transaksi.

## 9.10 Tambah Pembayaran

Gunakan modal bottom sheet atau full screen compact.

Field:

- Nama warga, read-only.
- Nama kegiatan, read-only.
- Sisa target, read-only.
- Input nominal.
- Nominal cepat: `10.000`, `20.000`, `50.000`, `Lunasi`.
- Metode: `Tunai`, `Transfer`, `QRIS`, `Lainnya`.
- Catatan opsional.
- Waktu perangkat.

Validasi:

- Nominal > 0.
- Tidak boleh kosong.
- Beri konfirmasi jika melebihi sisa target.
- Setelah submit, tombol disabled sampai local write berhasil.
- ID transaksi dibuat tepat satu kali sebelum write.

Pesan sukses online:

```text
Pembayaran Rp10.000 berhasil disimpan dan tersinkron.
```

Pesan sukses offline:

```text
Pembayaran Rp10.000 tersimpan di perangkat dan akan disinkronkan otomatis.
```

## 9.11 Riwayat Kalender

- Kalender bulanan.
- Dot hijau: pembayaran sudah tersinkron.
- Dot abu-abu: pembayaran masih pending lokal.
- Badge angka: lebih dari satu transaksi pada tanggal yang sama.
- Dot merah: reversal.

Saat tanggal dipilih, tampilkan:

- Nominal.
- Jam.
- Petugas.
- Metode.
- Catatan.
- Status sync.
- Jenis transaksi.

Di bawah kalender tampilkan timeline transaksi bulanan.

## 9.12 Transaksi Admin

- Search nama warga.
- Filter kegiatan.
- Filter petugas.
- Filter metode.
- Filter tipe `PAYMENT/REVERSAL`.
- Filter tanggal.
- Detail transaksi.
- Aksi reversal.

## 9.13 Laporan Admin

Tab:

```text
Ringkasan | Warga | Petugas | Harian | Metode
```

Metrik:

- Total target.
- Total terkumpul bersih.
- Kekurangan.
- Warga lunas.
- Warga mencicil.
- Warga belum membayar.
- Warga lebih bayar.
- Progress dana.
- Progress partisipasi.
- Total transaksi payment.
- Total reversal.
- Tunai.
- Transfer.
- QRIS.
- Lainnya.

---

## 10. Design System

## 10.1 Karakter Visual

- Modern.
- Bersih.
- Ramah warga.
- Tidak terlalu formal.
- Informasi utama mudah dipindai.
- Tombol besar dan nyaman di lapangan.
- Kontras cukup pada kondisi luar ruangan.

## 10.2 Palet Warna

```text
Admin Primary      #2563EB
Admin Dark         #1D4ED8
Officer Primary    #16A34A
Officer Dark       #15803D
Accent Purple      #7C3AED
Warning            #F59E0B
Danger             #DC2626
Info               #0891B2
Background         #F6F8FC
Surface            #FFFFFF
Text Primary       #172033
Text Secondary     #64748B
Border              #E2E8F0
Disabled           #CBD5E1
```

## 10.3 Warna Status

```text
UNPAID             Abu-abu
PARTIAL            Oranye
PAID               Hijau
OVERPAID           Biru/Ungu
LATE               Merah
LOCAL_PENDING      Abu kebiruan
SYNCING            Biru
SYNCED             Hijau
FAILED             Merah
```

## 10.4 Komponen

- Radius card: 20–24 dp.
- Radius input: 14–16 dp.
- Spacing halaman: 16 dp.
- Tinggi tombol utama: minimal 52 dp.
- Elevation ringan.
- Progress bar tinggi 8–10 dp.
- Gunakan badge status berbentuk pill.
- Gunakan skeleton loading untuk dashboard/list.
- Gunakan snackbar untuk hasil aksi singkat.
- Gunakan alert dialog untuk aksi reversal dan overpayment.

## 10.5 Typography

Gunakan salah satu:

- Plus Jakarta Sans.
- Inter.
- Roboto bawaan Android.

Aturan:

- Heading: SemiBold/Bold.
- Body: Regular/Medium.
- Nominal uang: Bold.
- Hindari teks terlalu kecil; minimum body 14sp.

---

## 11. Status Domain

## 11.1 Role

```kotlin
enum class UserRole {
    ADMIN,
    OFFICER
}
```

## 11.2 Status Kegiatan

```kotlin
enum class ActivityStatus {
    DRAFT,
    ACTIVE,
    EXPIRED,
    COMPLETED,
    ARCHIVED
}
```

Aturan:

- `DRAFT`: hanya admin.
- `ACTIVE`: tampil pada petugas terkait.
- `EXPIRED`: tenggat lewat, tetapi pembayaran dapat tetap diterima jika `allowLatePayment = true`.
- `COMPLETED`: kegiatan ditutup secara operasional.
- `ARCHIVED`: hanya untuk histori.

## 11.3 Status Pembayaran Warga

Dihitung, tidak disimpan sebagai sumber kebenaran:

```kotlin
enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    PAID,
    OVERPAID
}
```

UI dapat menambahkan label:

```text
LATE_UNPAID
LATE_PARTIAL
```

## 11.4 Tipe Transaksi

```kotlin
enum class TransactionType {
    PAYMENT,
    REVERSAL
}
```

## 11.5 Metode Pembayaran

```kotlin
enum class PaymentMethod {
    CASH,
    TRANSFER,
    QRIS,
    OTHER,
    REVERSAL
}
```

## 11.6 Status Sinkronisasi UI

```kotlin
enum class SyncStatus {
    LOCAL_PENDING,
    SYNCING,
    SYNCED,
    FAILED
}
```

`SyncStatus` terutama berasal dari metadata Firestore dan state aplikasi, bukan field bisnis permanen.

---

## 12. Struktur Firestore

```text
app_settings/
  general

officers/
  {officerId}

residents/
  {residentId}

activities/
  {activityId}

activity_participants/
  {activityId}_{residentId}

transactions/
  {transactionId}

devices/
  {deviceId}
```

Gunakan nama field konsisten dalam bahasa Inggris agar model kode lebih sederhana.

## 12.1 app_settings/general

```json
{
  "appName": "Gempala Iuran",
  "organizationName": "GEMPALA",
  "defaultCurrency": "IDR",
  "timezoneId": "Asia/Jakarta",
  "updatedAt": "serverTimestamp"
}
```

## 12.2 officers/{officerId}

```json
{
  "name": "Ahmad Fauzi",
  "username": "ahmad",
  "usernameNormalized": "ahmad",
  "passwordHash": "base64-hash",
  "passwordSalt": "base64-salt",
  "passwordIterations": 120000,
  "phone": "081234567890",
  "isActive": true,
  "assignedActivityIds": ["activity_17_agustus_2026"],
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp"
}
```

## 12.3 residents/{residentId}

```json
{
  "name": "Sukirman",
  "nameNormalized": "sukirman",
  "houseNumber": "14",
  "block": "Blok Utara",
  "address": "RT 02 RW 01",
  "phone": "",
  "notes": "",
  "isActive": true,
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp"
}
```

## 12.4 activities/{activityId}

```json
{
  "name": "Iuran HUT RI ke-81",
  "nameNormalized": "iuran hut ri ke-81",
  "description": "Kegiatan peringatan 17 Agustus",
  "startAt": "Timestamp",
  "deadlineAt": "Timestamp",
  "defaultTargetAmount": 100000,
  "allowLatePayment": true,
  "status": "ACTIVE",
  "assignedOfficerIds": ["officer_ahmad"],
  "createdBy": "ADMIN",
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp"
}
```

## 12.5 activity_participants/{activityId}_{residentId}

```json
{
  "activityId": "activity_17_agustus_2026",
  "residentId": "resident_014",
  "targetAmount": 100000,
  "isIncluded": true,
  "notes": "",
  "createdAt": "serverTimestamp",
  "updatedAt": "serverTimestamp"
}
```

ID gabungan mencegah warga terdaftar dua kali pada kegiatan yang sama.

## 12.6 transactions/{transactionId}

```json
{
  "transactionId": "device_a83f_00000142",
  "type": "PAYMENT",
  "activityId": "activity_17_agustus_2026",
  "residentId": "resident_014",
  "officerId": "officer_ahmad",
  "amount": 10000,
  "paymentMethod": "CASH",
  "note": "Cicilan pertama",
  "deviceId": "device_a83f",
  "localSequence": 142,
  "paidAtDeviceEpochMs": 1783827900000,
  "paidDateKey": "2026-07-12",
  "timezoneId": "Asia/Jakarta",
  "createdAtServer": "serverTimestamp",
  "relatedTransactionId": null,
  "createdByRole": "OFFICER",
  "schemaVersion": 1
}
```

Catatan:

- `paidAtDeviceEpochMs` dipakai untuk urutan waktu yang dicatat perangkat.
- `paidDateKey` memudahkan grouping kalender.
- `createdAtServer` dapat belum tersedia ketika write masih pending.
- Jangan menjadikan jam perangkat sebagai bukti waktu yang sepenuhnya tepercaya.

## 12.7 Reversal

```json
{
  "transactionId": "reversal_device_a83f_00000142",
  "type": "REVERSAL",
  "activityId": "activity_17_agustus_2026",
  "residentId": "resident_014",
  "officerId": "ADMIN",
  "amount": -10000,
  "paymentMethod": "REVERSAL",
  "note": "Salah input nominal",
  "deviceId": "admin_device_x1",
  "localSequence": 55,
  "paidAtDeviceEpochMs": 1783831200000,
  "paidDateKey": "2026-07-12",
  "timezoneId": "Asia/Jakarta",
  "createdAtServer": "serverTimestamp",
  "relatedTransactionId": "device_a83f_00000142",
  "createdByRole": "ADMIN",
  "schemaVersion": 1
}
```

## 12.8 devices/{deviceId}

```json
{
  "deviceId": "device_a83f",
  "deviceName": "Samsung A15 Ahmad",
  "officerId": "officer_ahmad",
  "appVersion": "1.0.0",
  "lastSeenAt": "serverTimestamp",
  "lastSyncAt": "serverTimestamp",
  "createdAt": "serverTimestamp"
}
```

Dokumen device hanya untuk observasi admin, bukan sumber utama nomor sequence. Sequence tetap disimpan lokal agar dapat bertambah saat offline.

---

## 13. Prinsip Ledger Append-Only

Sumber kebenaran pembayaran adalah seluruh transaksi pada kegiatan dan warga terkait.

```text
Net paid amount = SUM(amount seluruh PAYMENT dan REVERSAL)
```

Contoh:

```text
PAYMENT   +Rp50.000
PAYMENT   +Rp20.000
REVERSAL  -Rp20.000
PAYMENT   +Rp10.000
-------------------
NET        Rp60.000
```

Dilarang menjadikan field berikut sebagai sumber utama:

```text
resident.totalPaid
participant.totalPaid
activity.totalCollected
activity.progress
```

Field summary boleh ditambahkan di masa depan hanya sebagai cache yang dapat dibangun ulang, tetapi bukan pada MVP Firestore-only karena update summary antarperangkat berisiko konflik.

---

## 14. Anti-Duplikasi dan Anti-Bentrok

## 14.1 ID Transaksi

Format:

```text
{deviceId}_{localSequencePadded}
```

Contoh:

```text
device_a83f_00000142
```

Ketentuan:

- `deviceId` dibuat satu kali saat instalasi pertama.
- Gunakan UUID acak dan prefix singkat yang mudah dibaca.
- Simpan `deviceId` di DataStore.
- Jika aplikasi dihapus/reinstall, device ID baru dibuat.
- `localSequence` disimpan di DataStore.
- Sequence dinaikkan secara atomik menggunakan `DataStore.updateData`.
- Gap sequence diperbolehkan.
- Sequence tidak boleh dikurangi atau dipakai ulang.

## 14.2 Reservasi Sequence

Pseudocode:

```kotlin
suspend fun reserveNextSequence(): Long {
    var reserved = 0L
    dataStore.updateData { current ->
        reserved = current.lastLocalSequence + 1L
        current.copy(lastLocalSequence = reserved)
    }
    return reserved
}
```

## 14.3 Pembuatan Transaksi

```kotlin
suspend fun createPayment(input: PaymentInput): Result<String> = runCatching {
    val sequence = sequenceManager.reserveNextSequence()
    val transactionId = transactionIdGenerator.create(deviceId, sequence)

    val transaction = PaymentTransactionDto(
        transactionId = transactionId,
        type = "PAYMENT",
        activityId = input.activityId,
        residentId = input.residentId,
        officerId = input.officerId,
        amount = input.amount,
        paymentMethod = input.method.name,
        note = input.note,
        deviceId = deviceId,
        localSequence = sequence,
        paidAtDeviceEpochMs = clock.currentTimeMillis(),
        paidDateKey = dateKeyProvider.todayAsiaJakarta(),
        timezoneId = "Asia/Jakarta",
        createdAtServer = FieldValue.serverTimestamp(),
        relatedTransactionId = null,
        createdByRole = "OFFICER",
        schemaVersion = 1
    )

    firestore.collection("transactions")
        .document(transactionId)
        .set(transaction)
        .await()

    transactionId
}
```

Penting:

- Generate ID hanya sekali untuk satu aksi submit.
- Jangan membuat ID baru pada retry yang sama.
- Manual sync tidak memanggil `createPayment()` ulang.
- Gunakan `document(transactionId).set(...)`, bukan `.add()`.

## 14.4 Double Tap

- ViewModel memiliki `isSaving`.
- Abaikan event submit jika `isSaving == true`.
- Tombol dinonaktifkan segera setelah tap pertama.
- Jangan mengaktifkan tombol sebelum local write success/error selesai.

## 14.5 Dua Petugas Mencatat Warga yang Sama

Tidak terjadi overwrite karena setiap perangkat membuat dokumen transaksi berbeda. Setelah sinkronisasi, total dihitung dari seluruh transaksi.

Kemungkinan hasil:

- Total tetap di bawah target: normal.
- Total tepat target: status lunas.
- Total melebihi target: status `OVERPAID` dan admin mendapat indikator.

## 14.6 Reversal Ganda

ID reversal:

```text
reversal_{originalTransactionId}
```

Karena deterministic, reversal yang sama tidak menghasilkan dokumen kedua.

Sebelum membuat reversal:

- Pastikan transaksi asal bertipe `PAYMENT`.
- Pastikan belum terdapat reversal terkait.
- Amount reversal = `-original.amount`.
- Activity dan resident harus sama dengan transaksi asal.

---

## 15. Sinkronisasi Offline

Cloud Firestore Android menggunakan persistence offline dan menyinkronkan local writes ketika jaringan kembali tersedia.

## 15.1 Sinkronisasi Otomatis

```text
Petugas menekan Simpan
  → document(transactionId).set(...)
  → Firestore menyimpan write pada cache lokal
  → UI menerima data lokal
  → transaksi diberi indikator pending
  → saat online Firestore mengirim write
  → server mengonfirmasi
  → metadata hasPendingWrites menjadi false
  → UI menampilkan synced
```

## 15.2 Sinkronisasi Manual

Tombol `Sinkronkan Sekarang` hanya:

1. Memeriksa koneksi.
2. Memanggil `enableNetwork()`.
3. Menunggu `waitForPendingWrites()`.
4. Memuat ulang listener/query utama jika diperlukan.
5. Memperbarui `lastSyncAt` lokal.

```kotlin
suspend fun syncNow(): Result<Unit> = runCatching {
    firestore.enableNetwork().await()
    firestore.waitForPendingWrites().await()
    sessionStore.updateLastSyncAt(clock.currentTimeMillis())
}
```

Sinkronisasi manual **tidak**:

- Membaca daftar pending lalu membuat transaksi baru.
- Memanggil ulang use case create payment.
- Mengubah transaction ID.
- Menyalin transaksi ke collection lain.

## 15.3 Deteksi Pending Write

Gunakan listener dengan metadata:

```kotlin
query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
    val pendingCount = snapshot
        ?.documents
        ?.count { it.metadata.hasPendingWrites() }
        ?: 0
}
```

Untuk indikator perangkat, query transaksi berdasarkan `deviceId` dan periode yang relevan. Jangan membaca seluruh histori tanpa batas pada setiap pembukaan dashboard.

## 15.4 Status UI

- `LOCAL_PENDING`: dokumen berasal dari cache dan memiliki pending writes.
- `SYNCING`: user menekan manual sync atau koneksi sedang mengirim pending writes.
- `SYNCED`: tidak ada pending writes pada data yang dipantau.
- `FAILED`: operasi manual sync melempar error.

Pesan gagal harus menegaskan bahwa data lokal tetap aman selama write sudah masuk cache Firestore.

## 15.5 Admin Master Data

Transaksi petugas adalah offline-first. Perubahan master data admin bersifat online-preferred karena Firestore menggunakan last-write-wins pada perubahan dokumen yang sama.

Saat admin offline:

- Tetap izinkan membaca cache.
- Nonaktifkan publish/activate kegiatan.
- Nonaktifkan reset password dan perubahan petugas.
- Tampilkan pesan `Perubahan data utama memerlukan koneksi internet.`

---

## 16. Perhitungan

## 16.1 Total Warga

```text
paidAmount = SUM(transaction.amount)
```

Semua tipe transaksi ikut dijumlahkan karena reversal bernilai negatif.

## 16.2 Status Warga

```text
paidAmount <= 0                  → UNPAID
0 < paidAmount < targetAmount   → PARTIAL
paidAmount == targetAmount      → PAID
paidAmount > targetAmount       → OVERPAID
```

## 16.3 Progress Warga

```text
progressPercent = paidAmount / targetAmount × 100
```

- Progress bar visual dibatasi 0–100%.
- Teks dapat menampilkan lebih dari 100%.
- Jika target 0 karena data rusak, progress 0 dan tampilkan error data.

## 16.4 Total Target Kegiatan

```text
totalTarget = SUM(targetAmount participant dengan isIncluded = true)
```

## 16.5 Total Terkumpul Kegiatan

```text
totalCollected = SUM(amount seluruh transaksi kegiatan)
```

## 16.6 Progress Dana

```text
fundProgress = totalCollected / totalTarget × 100
```

## 16.7 Progress Partisipasi

Warga berpartisipasi jika `paidAmount > 0`.

```text
participationProgress = participatingResidents / totalParticipants × 100
```

Progress dana dan partisipasi harus ditampilkan sebagai dua metrik berbeda.

---

## 17. Query Firestore

## 17.1 Login Petugas

```kotlin
firestore.collection("officers")
    .whereEqualTo("usernameNormalized", normalizedUsername)
    .limit(1)
```

## 17.2 Kegiatan Aktif Petugas

```kotlin
firestore.collection("activities")
    .whereEqualTo("status", "ACTIVE")
    .whereArrayContains("assignedOfficerIds", officerId)
```

## 17.3 Participant Kegiatan

```kotlin
firestore.collection("activity_participants")
    .whereEqualTo("activityId", activityId)
    .whereEqualTo("isIncluded", true)
```

## 17.4 Transaksi Warga

```kotlin
firestore.collection("transactions")
    .whereEqualTo("activityId", activityId)
    .whereEqualTo("residentId", residentId)
    .orderBy("paidAtDeviceEpochMs", Query.Direction.DESCENDING)
```

## 17.5 Transaksi Petugas Hari Ini

```kotlin
firestore.collection("transactions")
    .whereEqualTo("officerId", officerId)
    .whereGreaterThanOrEqualTo("paidAtDeviceEpochMs", startOfDayEpochMs)
    .whereLessThan("paidAtDeviceEpochMs", startOfNextDayEpochMs)
```

## 17.6 Kalender Warga

Ambil transaksi berdasarkan activity dan resident, lalu grouping client-side menggunakan `paidDateKey`.

## 17.7 Pagination

- Gunakan `limit()` dan cursor `startAfter()` untuk histori panjang.
- Jangan menggunakan offset.
- Dashboard cukup memuat transaksi terbaru, misalnya 10–20 item.

---

## 18. Firestore Indexes

Siapkan composite index sesuai kebutuhan:

```json
{
  "indexes": [
    {
      "collectionGroup": "activities",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "status", "order": "ASCENDING" },
        { "fieldPath": "assignedOfficerIds", "arrayConfig": "CONTAINS" }
      ]
    },
    {
      "collectionGroup": "activity_participants",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "activityId", "order": "ASCENDING" },
        { "fieldPath": "isIncluded", "order": "ASCENDING" }
      ]
    },
    {
      "collectionGroup": "transactions",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "activityId", "order": "ASCENDING" },
        { "fieldPath": "residentId", "order": "ASCENDING" },
        { "fieldPath": "paidAtDeviceEpochMs", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "transactions",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "officerId", "order": "ASCENDING" },
        { "fieldPath": "paidAtDeviceEpochMs", "order": "DESCENDING" }
      ]
    },
    {
      "collectionGroup": "transactions",
      "queryScope": "COLLECTION",
      "fields": [
        { "fieldPath": "deviceId", "order": "ASCENDING" },
        { "fieldPath": "paidAtDeviceEpochMs", "order": "DESCENDING" }
      ]
    }
  ],
  "fieldOverrides": []
}
```

Jika Firebase Console meminta index tambahan, tambahkan ke file index dan dokumentasikan alasannya.

---

## 19. Firestore Security Rules

Karena tidak ada Firebase Auth, rules hanya dapat melakukan validasi bentuk data, bukan role-based authorization yang sebenarnya.

Jangan membuat catch-all rule `allow read, write: if true` setelah rules transaksi, karena rules yang overlap dievaluasi secara OR dan dapat membuka kembali update/delete transaksi.

Contoh baseline internal:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isNonEmptyString(value) {
      return value is string && value.size() > 0;
    }

    function validAmount(value) {
      return value is int && value != 0;
    }

    function validTransaction(id) {
      return request.resource.data.keys().hasAll([
          'transactionId',
          'type',
          'activityId',
          'residentId',
          'officerId',
          'amount',
          'paymentMethod',
          'deviceId',
          'localSequence',
          'paidAtDeviceEpochMs',
          'paidDateKey',
          'timezoneId',
          'createdByRole',
          'schemaVersion'
        ])
        && request.resource.data.transactionId == id
        && isNonEmptyString(request.resource.data.activityId)
        && isNonEmptyString(request.resource.data.residentId)
        && isNonEmptyString(request.resource.data.officerId)
        && isNonEmptyString(request.resource.data.deviceId)
        && request.resource.data.localSequence is int
        && request.resource.data.localSequence > 0
        && request.resource.data.paidAtDeviceEpochMs is int
        && request.resource.data.schemaVersion == 1
        && validAmount(request.resource.data.amount)
        && request.resource.data.type in ['PAYMENT', 'REVERSAL']
        && request.resource.data.paymentMethod in [
          'CASH', 'TRANSFER', 'QRIS', 'OTHER', 'REVERSAL'
        ];
    }

    match /transactions/{transactionId} {
      allow read: if true;
      allow create: if validTransaction(transactionId);
      allow update, delete: if false;
    }

    match /residents/{residentId} {
      allow read: if true;
      allow create, update: if
        isNonEmptyString(request.resource.data.name)
        && request.resource.data.isActive is bool;
      allow delete: if false;
    }

    match /officers/{officerId} {
      allow read: if true;
      allow create, update: if
        isNonEmptyString(request.resource.data.name)
        && isNonEmptyString(request.resource.data.usernameNormalized)
        && request.resource.data.isActive is bool;
      allow delete: if false;
    }

    match /activities/{activityId} {
      allow read: if true;
      allow create, update: if
        isNonEmptyString(request.resource.data.name)
        && request.resource.data.defaultTargetAmount is int
        && request.resource.data.defaultTargetAmount > 0
        && request.resource.data.status in [
          'DRAFT', 'ACTIVE', 'EXPIRED', 'COMPLETED', 'ARCHIVED'
        ];
      allow delete: if false;
    }

    match /activity_participants/{participantId} {
      allow read: if true;
      allow create, update: if
        isNonEmptyString(request.resource.data.activityId)
        && isNonEmptyString(request.resource.data.residentId)
        && request.resource.data.targetAmount is int
        && request.resource.data.targetAmount > 0
        && request.resource.data.isIncluded is bool;
      allow delete: if false;
    }

    match /devices/{deviceId} {
      allow read: if true;
      allow create, update: if
        request.resource.data.deviceId == deviceId;
      allow delete: if false;
    }

    match /app_settings/{documentId} {
      allow read, create, update: if true;
      allow delete: if false;
    }
  }
}
```

Peringatan:

- Rules ini masih dapat ditulis oleh klien mana pun yang mengetahui konfigurasi Firebase.
- Validasi `createdByRole` tidak membuktikan role asli.
- Jangan menyimpan data yang sangat sensitif.

---

## 20. Arsitektur Kode

```text
com.gempala.iuran/
├── GempalaIuranApp.kt
├── MainActivity.kt
├── core/
│   ├── constants/
│   ├── dispatcher/
│   ├── extensions/
│   ├── formatter/
│   ├── result/
│   ├── time/
│   └── utils/
├── data/
│   ├── local/
│   │   ├── AppPreferences.kt
│   │   ├── AppPreferencesSerializer.kt
│   │   ├── SessionStore.kt
│   │   ├── DeviceIdentityStore.kt
│   │   └── SequenceManager.kt
│   ├── remote/
│   │   ├── FirestoreCollections.kt
│   │   ├── FirestoreDataSource.kt
│   │   └── mapper/
│   ├── dto/
│   │   ├── ActivityDto.kt
│   │   ├── OfficerDto.kt
│   │   ├── ResidentDto.kt
│   │   ├── ParticipantDto.kt
│   │   ├── TransactionDto.kt
│   │   └── DeviceDto.kt
│   └── repository/
│       ├── AuthRepositoryImpl.kt
│       ├── ActivityRepositoryImpl.kt
│       ├── ResidentRepositoryImpl.kt
│       ├── OfficerRepositoryImpl.kt
│       ├── TransactionRepositoryImpl.kt
│       ├── ReportRepositoryImpl.kt
│       └── SyncRepositoryImpl.kt
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│       ├── auth/
│       ├── activity/
│       ├── resident/
│       ├── officer/
│       ├── transaction/
│       ├── report/
│       └── sync/
├── presentation/
│   ├── navigation/
│   ├── theme/
│   ├── components/
│   ├── splash/
│   ├── login/
│   ├── admin/
│   │   ├── dashboard/
│   │   ├── activities/
│   │   ├── residents/
│   │   ├── officers/
│   │   ├── transactions/
│   │   ├── reports/
│   │   └── profile/
│   └── officer/
│       ├── dashboard/
│       ├── residents/
│       ├── residentdetail/
│       ├── payment/
│       ├── history/
│       └── profile/
└── di/
    └── AppModule.kt
```

Gunakan satu source of truth per screen melalui ViewModel dan `StateFlow`.

---

## 21. Dependency Guidelines

Gunakan versi stabil terbaru yang saling kompatibel untuk:

- Android Gradle Plugin.
- Kotlin.
- Compose BOM.
- Material 3.
- Navigation Compose.
- Lifecycle ViewModel Compose.
- Kotlin Coroutines.
- Firebase BOM.
- Firebase Firestore KTX/API Kotlin yang direkomendasikan saat implementasi.
- DataStore.
- Hilt atau Koin untuk dependency injection; pilih satu dan gunakan konsisten.
- Coil hanya jika nanti ada gambar/avatar remote.
- kotlinx-datetime atau Java Time API dengan desugaring untuk waktu.

Jangan menambahkan library berat tanpa kebutuhan nyata.

---

## 22. Model Domain Inti

```kotlin
data class IuranActivity(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val startAtEpochMs: Long = 0L,
    val deadlineAtEpochMs: Long = 0L,
    val defaultTargetAmount: Long = 0L,
    val allowLatePayment: Boolean = true,
    val status: ActivityStatus = ActivityStatus.DRAFT,
    val assignedOfficerIds: List<String> = emptyList()
)
```

```kotlin
data class ActivityParticipant(
    val id: String = "",
    val activityId: String = "",
    val residentId: String = "",
    val targetAmount: Long = 0L,
    val isIncluded: Boolean = true,
    val notes: String = ""
)
```

```kotlin
data class PaymentTransaction(
    val transactionId: String = "",
    val type: TransactionType = TransactionType.PAYMENT,
    val activityId: String = "",
    val residentId: String = "",
    val officerId: String = "",
    val amount: Long = 0L,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val deviceId: String = "",
    val localSequence: Long = 0L,
    val paidAtDeviceEpochMs: Long = 0L,
    val paidDateKey: String = "",
    val timezoneId: String = "Asia/Jakarta",
    val relatedTransactionId: String? = null,
    val createdByRole: UserRole = UserRole.OFFICER
)
```

---

## 23. Repository Contract

Contoh:

```kotlin
interface TransactionRepository {
    fun observeResidentTransactions(
        activityId: String,
        residentId: String
    ): Flow<List<PaymentTransaction>>

    fun observeOfficerRecentTransactions(
        officerId: String,
        limit: Long = 20
    ): Flow<List<PaymentTransaction>>

    suspend fun createPayment(input: PaymentInput): Result<String>

    suspend fun createReversal(
        originalTransactionId: String,
        reason: String
    ): Result<String>
}
```

```kotlin
interface SyncRepository {
    val syncState: StateFlow<SyncState>
    suspend fun syncNow(): Result<Unit>
}
```

---

## 24. State Management

Gunakan immutable state.

```kotlin
data class OfficerDashboardUiState(
    val isLoading: Boolean = false,
    val officerName: String = "",
    val activities: List<ActivityCardUiModel> = emptyList(),
    val todayTransactionCount: Int = 0,
    val todayCollectedAmount: Long = 0L,
    val pendingSyncCount: Int = 0,
    val connectivityState: ConnectivityState = ConnectivityState.Unknown,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val lastSyncAtEpochMs: Long? = null,
    val errorMessage: String? = null
)
```

Event satu kali seperti snackbar atau navigation memakai `SharedFlow`/Channel, bukan field state yang terus terulang.

---

## 25. Search dan Filtering

Karena skala RT kecil, daftar warga dalam kegiatan dapat dimuat dari participant dan resident cache, lalu search/filter dilakukan client-side.

Normalisasi search:

- Lowercase.
- Trim.
- Hapus spasi ganda.
- Cocokkan `nameNormalized`, nomor rumah, block, dan alamat.

Jangan menjalankan query Firestore baru pada setiap karakter ketikan jika seluruh daftar sudah tersedia.

---

## 26. Format Data

## 26.1 Rupiah

```kotlin
fun Long.toRupiah(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this)
}
```

Output:

```text
Rp100.000
```

## 26.2 Tanggal

- UI tanggal: `12 Juli 2026`.
- UI waktu: `10.45 WIB`.
- Date key: `yyyy-MM-dd`.
- Zone: `Asia/Jakarta`.

## 26.3 Persentase

- Card: bilangan bulat.
- Detail: maksimal satu angka desimal.
- Visual progress dibatasi 100%.

---

## 27. Error Handling

Pesan ramah:

- `Username atau password tidak sesuai.`
- `Akun petugas sudah dinonaktifkan.`
- `Login pertama memerlukan koneksi internet.`
- `Pembayaran tersimpan di perangkat dan akan dikirim saat internet tersedia.`
- `Sinkronisasi belum berhasil. Data tetap aman di perangkat.`
- `Nominal pembayaran harus lebih dari Rp0.`
- `Pembayaran melebihi sisa target.`
- `Kegiatan sudah ditutup oleh admin.`
- `Perubahan data utama memerlukan koneksi internet.`
- `Data warga tidak ditemukan.`

Jangan menampilkan exception Firebase secara mentah.

Logging release tidak boleh berisi:

- Password.
- Password hash/salt.
- Kredensial admin.
- Nomor telepon lengkap jika tidak diperlukan.

---

## 28. Edge Cases Wajib

1. Petugas mencatat pembayaran ketika offline.
2. Aplikasi ditutup setelah local write tetapi sebelum server ack.
3. Petugas menekan simpan dua kali.
4. Petugas menekan sinkronisasi manual berkali-kali.
5. Dua petugas mencatat warga yang sama pada saat offline.
6. Total pembayaran menjadi lebih besar dari target.
7. Admin membuat reversal sementara perangkat petugas offline.
8. Kegiatan ditutup ketika petugas masih membuka detail warga.
9. Username petugas duplikat.
10. Warga dinonaktifkan tetapi memiliki histori lama.
11. Jam perangkat salah.
12. `createdAtServer` belum tersedia pada local snapshot.
13. Firestore meminta composite index.
14. Jaringan putus di tengah sinkronisasi.
15. Participant ada tetapi resident tidak ditemukan.
16. Target participant berubah setelah ada pembayaran.
17. Password petugas di-reset saat perangkat lama masih memiliki sesi.
18. Aplikasi reinstall sehingga device ID baru dibuat.
19. Sequence lokal memiliki gap.
20. Reversal dibuat dua kali.

Aturan penyelesaian:

- Gap sequence bukan error.
- Reinstall menghasilkan device ID baru.
- Missing resident ditampilkan sebagai `Data warga tidak tersedia` tetapi transaksi tidak dihapus.
- Target yang berubah harus langsung memperbarui progress tanpa mengubah histori transaksi.

---

## 29. Testing

## 29.1 Unit Test

- Validasi login admin.
- Normalisasi username.
- Hash dan verifikasi password petugas.
- Reservasi sequence.
- Generator transaction ID.
- Generator reversal ID.
- Perhitungan net paid.
- Status UNPAID/PARTIAL/PAID/OVERPAID.
- Progress dana.
- Progress partisipasi.
- Rupiah formatter.
- Date key Asia/Jakarta.

## 29.2 Integration Test dengan Firestore Emulator

- Create transaction online.
- Create transaction offline kemudian online.
- Retry `set()` dengan ID sama.
- Verifikasi tidak ada dokumen ganda.
- Dua device ID membuat transaksi berbeda.
- Reversal berhasil.
- Update/delete transaksi ditolak rules.
- CRUD master data sesuai rules.
- Query yang membutuhkan index.

## 29.3 UI Test

- Login admin.
- Login petugas.
- Session routing.
- Tambah kegiatan.
- Cari warga.
- Filter status.
- Tambah pembayaran.
- Double tap prevention.
- Overpayment confirmation.
- Kalender riwayat.
- Manual sync.
- Reversal confirmation.

## 29.4 Skenario Multi-Device Wajib

```text
Device A offline: warga X bayar Rp10.000
Device B offline: warga X bayar Rp20.000
Keduanya online kembali
Expected: terdapat dua dokumen transaksi, total Rp30.000, tidak ada overwrite
```

```text
Device A submit pembayaran dan aplikasi restart
Device A menekan sync manual
Expected: tetap satu dokumen dengan transactionId yang sama
```

---

## 30. Seed Data Pengembangan

### Admin

```text
admin@gempala.com / gempala2026
```

### Petugas

```text
Ahmad Fauzi / ahmad
Budi Santoso / budi
```

Gunakan password dummy yang di-hash pada seed development.

### Kegiatan

```text
Iuran HUT RI ke-81
Target default: Rp100.000
Mulai: 1 Juli 2026
Tenggat: 10 Agustus 2026
Status: ACTIVE
```

```text
Iuran Maulid Nabi 1448 H
Target default: Rp75.000
Status: DRAFT
```

### Warga

Buat minimal 20 data dummy dengan status pembayaran beragam untuk menguji UI.

---

## 31. Urutan Implementasi

## Fase 1 — Fondasi

1. Buat project Kotlin Compose.
2. Hubungkan Firebase Firestore.
3. Siapkan Firebase Emulator untuk testing.
4. Konfigurasi DataStore.
5. Buat device identity dan sequence manager.
6. Buat navigation dan theme.
7. Buat komponen UI dasar.

## Fase 2 — Login dan Sesi

1. Login admin hardcoded.
2. PBKDF2 utility.
3. CRUD petugas.
4. Login petugas.
5. Session persistence.
6. Splash routing.

## Fase 3 — Master Data

1. CRUD warga.
2. CRUD kegiatan.
3. Participant kegiatan.
4. Penugasan petugas.
5. Query kegiatan aktif.

## Fase 4 — Transaksi Offline-First

1. Transaction ID generator.
2. Create payment dengan `document(id).set()`.
3. Metadata pending writes.
4. Auto sync.
5. Manual sync.
6. Reversal.
7. Overpayment indicator.

## Fase 5 — UI Operasional

1. Dashboard admin.
2. Dashboard petugas.
3. Daftar warga.
4. Detail warga.
5. Input pembayaran.
6. Riwayat kalender.
7. Transaksi admin.
8. Laporan.

## Fase 6 — Stabilitas

1. Rules dan indexes.
2. Unit test.
3. Emulator integration test.
4. Multi-device test.
5. Offline/reconnect test.
6. Accessibility.
7. R8 release build.
8. Final UI polish.

---

## 32. Acceptance Criteria MVP

Aplikasi dianggap selesai hanya jika seluruh poin berikut terpenuhi:

### Login dan Role

- [ ] Admin dapat login dengan kredensial yang ditentukan.
- [ ] Admin dapat membuat akun petugas.
- [ ] Password petugas tidak disimpan plaintext.
- [ ] Petugas dapat login dengan akun Firestore.
- [ ] Petugas dapat membuka aplikasi offline setelah sesi pernah dibuat.
- [ ] UI admin dan petugas berbeda sesuai role.

### Kegiatan dan Warga

- [ ] Admin dapat membuat kegiatan dengan tanggal, tenggat, dan target.
- [ ] Admin dapat memilih warga peserta.
- [ ] Admin dapat menentukan petugas.
- [ ] Admin dapat menentukan target khusus per warga.
- [ ] Petugas hanya melihat kegiatan yang ditugaskan dan aktif.
- [ ] Petugas dapat mencari dan memfilter warga.

### Transaksi

- [ ] Petugas dapat mencatat cicilan.
- [ ] Transaksi langsung tampil ketika offline.
- [ ] Setiap transaksi memakai ID deterministik per operasi.
- [ ] Kode tidak menggunakan `.add()` untuk transaksi.
- [ ] Double tap tidak menghasilkan dua transaksi.
- [ ] Retry tidak menghasilkan transaksi baru.
- [ ] Dua petugas dapat mencatat warga yang sama tanpa overwrite.
- [ ] Reversal tidak menghapus transaksi asli.
- [ ] Reversal yang sama tidak dapat dibuat dua kali.

### Sinkronisasi

- [ ] Auto sync berjalan ketika internet kembali.
- [ ] Tombol manual sync bekerja.
- [ ] Manual sync aman ditekan berkali-kali.
- [ ] UI menampilkan pending, syncing, synced, dan failed.
- [ ] Pending write tetap ada setelah aplikasi restart.

### Perhitungan dan Laporan

- [ ] Total warga dihitung dari ledger.
- [ ] Total kegiatan dihitung dari ledger.
- [ ] Progress dana dan partisipasi terpisah.
- [ ] Kalender menampilkan marker tanggal pembayaran.
- [ ] Admin dapat melihat laporan kegiatan, warga, petugas, harian, dan metode.

### Kualitas

- [ ] Semua layar mempunyai loading, empty, error, online, dan offline state yang relevan.
- [ ] UI mengikuti design system.
- [ ] Nominal memakai `Long`.
- [ ] Timezone memakai Asia/Jakarta.
- [ ] Firestore rules tidak mempunyai catch-all write yang menimpa larangan transaksi.
- [ ] Test utama lulus pada Firestore Emulator.

---

## 33. Deliverables yang Harus Dihasilkan Agent

1. Source code Android lengkap.
2. README setup Firebase.
3. `google-services.json` tidak boleh di-commit ke repository publik.
4. `firestore.rules`.
5. `firestore.indexes.json`.
6. Panduan seed data development.
7. Panduan build debug/release.
8. Daftar known limitations.
9. Screenshot layar utama.
10. Laporan hasil unit/integration test.

---

## 34. Referensi Teknis Resmi

- Firestore offline persistence: <https://firebase.google.com/docs/firestore/manage-data/enable-offline>
- Firestore add/set document: <https://firebase.google.com/docs/firestore/manage-data/add-data>
- Firestore Android API: <https://firebase.google.com/docs/reference/kotlin/com/google/firebase/firestore/FirebaseFirestore>
- Firestore Security Rules structure: <https://firebase.google.com/docs/firestore/security/rules-structure>
- Firestore realtime listeners: <https://firebase.google.com/docs/firestore/query-data/listen>

---

## 35. Ringkasan Keputusan Akhir

```text
Satu aplikasi Android
Kotlin + Jetpack Compose Material 3
Firestore sebagai satu-satunya backend/database bisnis
Tanpa Firebase Authentication
Admin hardcoded
Petugas dibuat admin dan disimpan di Firestore
DataStore hanya untuk sesi/device/sequence/preferensi
Offline-first untuk transaksi petugas
Ledger append-only
Transaction ID = deviceId + localSequence
document(id).set(), bukan add()
Sinkronisasi otomatis + manual
Manual sync tidak membuat ulang transaksi
Reversal untuk koreksi
Progress dihitung dari agregasi transaksi
UI modern, ramah, dan mudah dipakai di lapangan
```

