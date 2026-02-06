# Timer Persisten dengan Foreground Service & AlarmManager

## Ringkasan
Implementasi timer yang **tidak akan reset** meskipun:
- Aplikasi di-force close
- HP mati/restart
- Aplikasi di-minimize dan dibuka lagi

---

## Arsitektur Solusi

### 1. **TimerPreferencesManager** (Data Layer)
Menyimpan `endTime` (timestamp kapan timer berakhir) ke SharedPreferences.

**Best Practice:** Simpan WAKTU SELESAI, bukan sisa detik!

```kotlin
// ❌ SALAH - Variabel in-memory hilang saat app mati
var sisaDetik = 600

// ✅ BENAR - Simpan timestamp kapan selesai
val endTime = submissionTime + (10 * 60 * 1000L)
// Simpan ke SharedPreferences
```

**Lokasi:** `data/local/datastore/TimerPreferencesManager.kt`

---

### 2. **LoanTimerService** (Foreground Service)
Menampilkan countdown timer di area notifikasi.

**Keuntungan:**
- User bisa melihat countdown saat membuka WhatsApp, Instagram, dll
- Android tidak mematikan service karena ada notifikasi permanen
- Update setiap detik secara real-time

**Lokasi:** `core/service/LoanTimerService.kt`

---

### 3. **TimerExpiredReceiver** (BroadcastReceiver)
Dipanggil oleh AlarmManager saat timer habis.

**Keuntungan:**
- Jaminan notifikasi "Waktu Habis" meskipun Service mati
- AlarmManager akan "membangunkan" aplikasi
- Reliable untuk aplikasi finansial

**Lokasi:** `core/service/TimerExpiredReceiver.kt`

---

## Flow Diagram

```
User Ajukan Pinjaman (10:00)
         │
         ▼
┌─────────────────────────────────────────────┐
│  1. Hitung endTime = 10:00 + 10 menit       │
│     = 10:10 (dalam milidetik)               │
│  2. Simpan ke SharedPreferences             │
│  3. Start Foreground Service                │
│  4. Set AlarmManager untuk jam 10:10        │
└─────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│  Foreground Service berjalan:               │
│  - Setiap detik: sisaWaktu = endTime - now  │
│  - Update notifikasi dengan countdown       │
└─────────────────────────────────────────────┘
         │
         ▼ (Aplikasi di-force close)
┌─────────────────────────────────────────────┐
│  AlarmManager tetap terjadwal!              │
│  Service mati, tapi alarm masih aktif       │
└─────────────────────────────────────────────┘
         │
         ▼ (Jam 10:10)
┌─────────────────────────────────────────────┐
│  TimerExpiredReceiver dipanggil             │
│  - Tampilkan notifikasi "Waktu Habis!"      │
│  - Mainkan suara notifikasi                 │
└─────────────────────────────────────────────┘
```

---

## Permissions yang Diperlukan

```xml
<!-- Timer Service Permissions -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## Cara Kerja di UI (LoanProgressTrackerScreen)

1. **Saat halaman dibuka:**
   - Hitung `endTime` = `submissionTime` + 10 menit
   - Start `LoanTimerService` dengan endTime
   - Timer di UI membaca dari endTime, bukan variabel in-memory

2. **Setiap detik:**
   - UI: `remainingSeconds = endTime - currentTime`
   - Service: Update notifikasi dengan countdown

3. **Saat status final (Approved/Rejected/Disbursed):**
   - Stop `LoanTimerService`
   - Clear timer dari preferences
   - Cancel AlarmManager

---

## Testing

### Test 1: Force Close App
1. Buka halaman tracking dengan timer berjalan
2. Force close aplikasi (Settings > Apps > Force Stop)
3. Buka kembali aplikasi
4. **Expected:** Timer melanjutkan countdown, tidak reset ke 10:00

### Test 2: Device Restart
1. Buka halaman tracking dengan timer berjalan
2. Restart HP
3. Buka kembali aplikasi
4. **Expected:** Timer melanjutkan countdown berdasarkan waktu yang tersisa

### Test 3: Notifikasi Background
1. Buka halaman tracking
2. Minimize aplikasi, buka WhatsApp atau Instagram
3. **Expected:** Countdown terlihat di notification bar

### Test 4: Timer Expired saat App Closed
1. Buka halaman tracking dengan sisa waktu 1 menit
2. Force close aplikasi
3. Tunggu sampai waktu habis
4. **Expected:** Notifikasi "Waktu Proses Selesai!" muncul

---

## Catatan Penting

1. **Android 12+:** Perlu izin `SCHEDULE_EXACT_ALARM` untuk alarm tepat waktu
2. **Doze Mode:** Menggunakan `setAlarmClock()` yang lebih reliable daripada `setExact()`
3. **Battery Optimization:** User mungkin perlu menonaktifkan battery optimization untuk app ini agar service tidak dimatikan
