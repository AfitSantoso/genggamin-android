# 🔄 Real-Time Progress Tracking System

## 📊 Bagaimana Sistem Bekerja

### **100% Real-time dari Backend - NO Hardcode!**

Sistem ini menggunakan **StateFlow** dan **Auto-Polling** untuk mendapatkan update status secara real-time dari backend.

---

## ⚙️ Mekanisme Polling Real-time

### **Polling Configuration**
```kotlin
Interval: 3 detik (lebih responsive!)
Duration: 10 menit (200 polls)
Action: Refresh data dari backend setiap poll
```

### **Flow Kerja:**

```
1. User buka Progress Tracker
   ↓
2. ViewModel startTracking(loanId)
   ↓
3. Initialize progress steps
   ↓
4. Start polling loop:
   │
   ├─→ Setiap 3 detik:
   │   ├─ refreshLoans() dari API
   │   ├─ getLoanById(loanId) dari database
   │   ├─ updateProgressBasedOnStatus(status)
   │   └─ Update UI via StateFlow
   │
   └─→ Stop jika status final:
       - APPROVED
       - DISBURSED
       - REJECTED
```

---

## 🎯 Status Mapping (Real-time dari Backend)

### **Status dari Backend → UI Progress**

| Backend Status | Current Step | UI Message | Icon | Color |
|---------------|--------------|------------|------|-------|
| **SUBMITTED, PENDING** | 0 | "Pengajuan Diterima" | Send | Primary |
| **UNDER_REVIEW** | 1 | "Sedang Diverifikasi" | Verified | Primary (Loading) |
| **APPROVED** | 2 | "Pengajuan Disetujui!" | ThumbUp | Green ✅ |
| **DISBURSED** | 3 | "Dana Telah Cair!" | Wallet | Green ✅ |
| **REJECTED** | 1 | "Pengajuan Ditolak" | Cancel | Red ❌ |

### **Progress Indicator Behavior:**

#### ✅ **Scenario 1: Under Review (Verifikasi)**
```kotlin
Status: "UNDER_REVIEW"
Step: 1 (Verifikasi Data)
Behavior:
  - Icon: Verified (berputar/loading)
  - CircularProgressIndicator: Muncul
  - Background: Primary container (biru muda)
  - Message: "Sistem sedang memverifikasi skor kredit Anda..."

Tetap berputar sampai:
  → Backend update ke "APPROVED" atau "REJECTED"
```

#### ✅ **Scenario 2: Approved (Menunggu Approval)**
```kotlin
Status: "APPROVED"
Step: 2 (Persetujuan)
Behavior:
  - Icon: Check (static, no loading)
  - Background: Tertiary container (hijau muda)
  - Color: Green
  - Message: "Pengajuan Disetujui!"

Auto move ke step 3 jika backend update ke "DISBURSED"
```

#### ❌ **Scenario 3: Rejected**
```kotlin
Status: "REJECTED"
Step: 1 (stuck di verifikasi)
Behavior:
  - Icon: Close (X)
  - Background: Error container (merah muda)
  - Color: Red
  - Message: "Pengajuan Ditolak"
  - NO loading indicator
  - Polling STOP (final status)

Warna merah sama seperti LoanHistoryScreen ✅
```

---

## 🔍 Code Implementation

### **1. ViewModel (LoanProgressViewModel.kt)**

#### **Auto-Polling with RefreshLoans**
```kotlin
private suspend fun pollLoanStatus(loanId: Long) {
    var pollCount = 0
    val maxPolls = 200 // 10 minutes
    
    while (pollCount < maxPolls) {
        delay(3000) // 3 seconds
        
        try {
            // 🔥 Key: Refresh dari backend!
            loanRepository.refreshLoans()
            
            // Get updated loan
            loanRepository.getLoanById(loanId)?.let { loan ->
                updateProgressBasedOnStatus(loan.status)
                
                // Stop jika final status
                if (loan.status.lowercase() in 
                    listOf("approved", "disbursed", "rejected")) {
                    return // Stop polling
                }
            }
        } catch (e: Exception) {
            // Keep polling even if error
        }
        
        pollCount++
    }
}
```

#### **Status-based Progress Update**
```kotlin
private fun updateProgressBasedOnStatus(status: String) {
    val (step, message) = when (status.lowercase()) {
        "submitted", "pending" -> 0 to "Pengajuan Diterima"
        "under_review" -> 1 to "Sedang Diverifikasi"
        "approved" -> 2 to "Pengajuan Disetujui!"
        "disbursed" -> 3 to "Dana Telah Cair!"
        "rejected" -> 1 to "Pengajuan Ditolak"
        else -> currentStep to currentMessage
    }
    
    _uiState.update { state ->
        state.copy(
            currentStep = step,
            status = status,
            statusMessage = message,
            progressSteps = updateTimestamps(step)
        )
    }
}
```

### **2. UI Layer (LoanProgressTrackerScreen.kt)**

#### **Reactive UI dengan StateFlow**
```kotlin
@Composable
fun LoanProgressTrackerScreen(...) {
    // 🔥 StateFlow auto-update UI saat data berubah
    val uiState by viewModel.uiState.collectAsState()
    
    // LaunchedEffect hanya run sekali
    LaunchedEffect(loanId) {
        viewModel.startTracking(loanId)
    }
    
    // UI reactive terhadap uiState changes
    ProgressStepItem(
        isActive = index == uiState.currentStep,
        isCompleted = index < uiState.currentStep,
        isFailed = uiState.status == "REJECTED"
    )
}
```

#### **Loading Indicator (Hanya untuk Active Steps)**
```kotlin
// Hanya muncul jika step aktif DAN belum failed
if (isActive && !isFailed) {
    CircularProgressIndicator(
        modifier = Modifier.size(22.dp),
        strokeWidth = 2.5.dp,
        color = MaterialTheme.colorScheme.primary
    )
}
```

---

## 🎨 Visual States

### **Verifikasi (Under Review) - Loading State**
```
┌─────────────────────────────────┐
│ [●] Pengajuan Diterima    ✓     │  ← Completed (green)
├─────────────────────────────────┤
│ [⟳] Verifikasi Data       ⟲     │  ← Active (blue + loading)
│     "Sistem sedang memverifikasi │
│      skor kredit Anda..."        │
├─────────────────────────────────┤
│ [2] Persetujuan                 │  ← Pending (gray)
├─────────────────────────────────┤
│ [3] Pencairan Dana              │  ← Pending (gray)
└─────────────────────────────────┘
```

### **Approved - Success State**
```
┌─────────────────────────────────┐
│ [✓] Pengajuan Diterima    ✓     │  ← Completed
├─────────────────────────────────┤
│ [✓] Verifikasi Data       ✓     │  ← Completed
├─────────────────────────────────┤
│ [✓] Persetujuan          ✓      │  ← Active & Completed (green)
│     13:45:22                     │
├─────────────────────────────────┤
│ [4] Pencairan Dana              │  ← Next (waiting for DISBURSED)
└─────────────────────────────────┘
```

### **Rejected - Error State**
```
┌─────────────────────────────────┐
│ [✓] Pengajuan Diterima    ✓     │  ← Completed
├─────────────────────────────────┤
│ [✗] Verifikasi Data       ✗     │  ← Failed (red background)
│     "Pengajuan Ditolak"          │
│     13:45:22                     │
├─────────────────────────────────┤
│ [3] Persetujuan                 │  ← Not reached
├─────────────────────────────────┤
│ [4] Pencairan Dana              │  ← Not reached
└─────────────────────────────────┘
```

---

## 📈 Performance Optimization

### **Why 3 seconds polling?**
- ✅ Balance antara responsiveness dan server load
- ✅ Lebih cepat dari 5 detik = lebih real-time
- ✅ User merasakan "live" experience
- ✅ 200 polls dalam 10 menit = sustainable

### **Auto-Stop Mechanism**
```kotlin
// Polling stops automatically when reaching final status
if (status in ["approved", "disbursed", "rejected"]) {
    return // No more polling!
}
```

### **Error Handling**
```kotlin
try {
    refreshLoans()
    updateProgress()
} catch (e: Exception) {
    // Don't crash, keep polling
    // User won't notice network hiccups
}
```

---

## 🧪 Testing Scenarios

### **Test 1: Normal Flow (Happy Path)**
```
1. Submit loan → Status: SUBMITTED
   Expected: Step 0, "Pengajuan Diterima"
   
2. Backend process → Status: UNDER_REVIEW
   Expected: Step 1, Loading indicator muncul
   
3. Backend approve → Status: APPROVED
   Expected: Step 2, Green checkmark, no loading
   
4. Backend disburse → Status: DISBURSED
   Expected: Step 3, "Dana Telah Cair!", polling stop
```

### **Test 2: Rejection Flow**
```
1. Submit loan → Status: SUBMITTED
   Expected: Step 0
   
2. Backend review → Status: UNDER_REVIEW
   Expected: Step 1, Loading
   
3. Backend reject → Status: REJECTED
   Expected: Step 1, Red background, X icon, polling stop
```

### **Test 3: Network Issues**
```
1. Polling active
2. Network down temporarily
3. Error caught, polling continues
4. Network back
5. Data syncs, UI updates
```

---

## 🚀 Best Practices Implemented

✅ **StateFlow for Reactive UI** - UI auto-update tanpa manual refresh  
✅ **Automatic Polling** - Background sync setiap 3 detik  
✅ **Smart Stop** - Polling berhenti saat status final  
✅ **Error Resilient** - Network issues tidak crash app  
✅ **Timestamp Tracking** - Setiap step tercatat waktunya  
✅ **Visual Feedback** - Loading, success, error states jelas  
✅ **No Simulation** - 100% data dari backend  

---

## 📌 Summary

| Feature | Implementation |
|---------|---------------|
| **Update Method** | Auto-polling every 3s via StateFlow |
| **Duration** | 10 minutes (200 polls) |
| **Status Source** | 100% from backend (refreshLoans) |
| **UI Update** | Automatic (collectAsState) |
| **Loading State** | Shows when step is active & not failed |
| **Final States** | APPROVED, DISBURSED, REJECTED (polling stops) |
| **Error Color** | Red (same as LoanHistoryScreen) |

---

**Version**: 2.0.0 (Real-time)  
**Updated**: 2026-02-05  
**Polling**: Every 3 seconds  
**Target**: 10 minutes max  
**Status**: ✅ Fully Reactive from Backend
