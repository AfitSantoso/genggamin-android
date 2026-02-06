# ✅ Testing Checklist - Progress Pulse Feature

## 🎯 Testing Flow

### Test 1: Basic Navigation Flow
- [ ] Login ke aplikasi
- [ ] Navigasi ke "Pinjaman" tab dari bottom navigation
- [ ] Pastikan ada loan yang masih dalam status "PENDING" atau "UNDER_REVIEW"
- [ ] Klik pada loan card
- [ ] Cek apakah tombol "Lihat Progress Real-Time" muncul (biru dengan icon Timeline)
- [ ] Klik tombol tersebut
- [ ] Pastikan navigasi ke Progress Tracker screen berhasil

### Test 2: Countdown Timer
- [ ] Cek countdown timer tampil dengan format MM:SS (contoh: 05:00, 04:59)
- [ ] Tunggu beberapa detik, pastikan countdown berkurang
- [ ] Progress bar bergerak sesuai waktu yang berlalu
- [ ] Setelah 5 menit, countdown berubah jadi "Sedang Diproses"

### Test 3: Progress Animation
- [ ] Icon tengah beranimasi (pulse effect)
- [ ] Warna berubah sesuai status:
  - Biru untuk "Sedang Diproses"
  - Hijau untuk "Approved/Disbursed"
  - Merah untuk "Rejected"
- [ ] Status message tampil dengan jelas

### Test 4: Progress Steps
- [ ] 4 tahap progress tampil:
  1. Pengajuan Diterima
  2. Verifikasi Data
  3. Persetujuan
  4. Pencairan Dana
- [ ] Step yang sudah selesai ada checkmark hijau
- [ ] Step yang aktif tampil dengan loading indicator
- [ ] Timestamp muncul untuk step yang sudah dilewati

### Test 5: Auto-Polling
- [ ] Biarkan screen terbuka selama 15-30 detik
- [ ] Cek apakah progress update otomatis (simulasi progress setiap 20 detik)
- [ ] Tidak perlu manual refresh

### Test 6: Back Navigation
- [ ] Klik tombol back
- [ ] Kembali ke Loan History screen
- [ ] Tidak ada crash

### Test 7: Different Loan Status
- [ ] Test dengan loan status "SUBMITTED" → Step 0
- [ ] Test dengan loan status "UNDER_REVIEW" → Step 1
- [ ] Test dengan loan status "APPROVED" → Step 2
- [ ] Test dengan loan status "DISBURSED" → Step 3
- [ ] Test dengan loan status "REJECTED" → Step 1 with error state

### Test 8: Edge Cases
- [ ] Loan dengan ID yang tidak valid
- [ ] Network offline (harus handle gracefully)
- [ ] Rotate device (state harus preserved)
- [ ] Minimize app dan kembali (polling harus continue)

---

## 🐛 Known Issues to Watch

1. **Lottie Animation**: Jika file Lottie belum ada, akan fallback ke animated icon
2. **Repository Method**: `getLoanById` mencari dari Flow, pastikan data tersedia
3. **Polling Stop**: Polling harus stop ketika status final tercapai

---

## 📝 Expected Results

### Success Indicators ✅
- Countdown timer jalan smooth
- Progress update setiap 5 detik
- Animasi tidak lag
- Navigation flow lancar
- Tombol hanya muncul untuk loan in-progress

### Failure Indicators ❌
- Countdown tidak bergerak
- Progress stuck di satu tahap
- Crash saat navigasi
- Tombol muncul untuk semua loan (harusnya hanya in-progress)

---

## 🔧 Quick Fixes

### Jika Countdown Tidak Jalan
Cek `startTime` di ViewModel, pastikan `submissionTime` di-set dengan benar

### Jika Progress Tidak Update
Cek `pollLoanStatus()` di ViewModel, pastikan polling berjalan

### Jika Tombol Tidak Muncul
Cek kondisi `isInProgress` di `LoanDetailContent`, pastikan status mapping benar

### Jika Navigasi Crash
Cek parameter `loanId` di NavGraph, pastikan type Long sudah benar

---

**Note**: Centang setiap item setelah di-test! 📌
