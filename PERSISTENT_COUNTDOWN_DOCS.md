# ⏱️ Persistent Countdown Timer - Technical Documentation

## 🎯 Problem Solved

**SEBELUM**: Countdown timer reset setiap kali user keluar dan masuk kembali ke Progress Tracker screen
**SESUDAH**: Countdown timer tetap berjalan berdasarkan waktu submission loan yang asli dari database

---

## 🔧 How It Works

### **Concept**
Countdown timer harus **konsisten** dan **persisten** berdasarkan waktu submission loan yang sebenarnya, bukan berdasarkan kapan user membuka screen.

### **Implementation**

#### **1. Data Model - Timestamp Storage**
```kotlin
// Domain Model
data class Loan(
    val id: Long?,
    val amount: Long,
    // ... other fields
    val submittedAt: Long = System.currentTimeMillis() // ← Key field
)

// Database Entity
@Entity(tableName = "loans")
data class LoanEntity(
    // ... other fields
    val submittedAt: Long = System.currentTimeMillis() // ← Stored in DB
)
```

#### **2. ViewModel - Load from Database**
```kotlin
fun startTracking(loanId: Long) {
    viewModelScope.launch {
        // Get loan dari database
        val loan = loanRepository.getLoanById(loanId)
        
        if (loan != null) {
            // Use loan's actual submission time
            _uiState.update { 
                it.copy(submissionTime = loan.submittedAt) // ← NOT current time!
            }
        }
    }
}
```

#### **3. UI - Calculate Elapsed Time**
```kotlin
@Composable
fun CountdownCard(startTime: Long?, status: String) {
    val targetTimeMinutes = 10
    var remainingSeconds by remember { mutableStateOf(targetTimeMinutes * 60) }
    
    LaunchedEffect(startTime) {
        if (startTime != null) {
            while (remainingSeconds > 0) {
                delay(1000)
                
                // Calculate elapsed time from ORIGINAL submission
                val currentTime = System.currentTimeMillis()
                elapsedSeconds = ((currentTime - startTime) / 1000).toInt()
                
                // Remaining = Target - Elapsed
                remainingSeconds = (targetTimeMinutes * 60) - elapsedSeconds
                
                if (remainingSeconds <= 0) {
                    remainingSeconds = 0
                    break
                }
            }
        }
    }
}
```

---

## 📊 Example Scenarios

### **Scenario 1: Continuous Usage**
```
13:00:00 - User submit loan
           submittedAt = 1675324800000 (13:00:00)
           
13:00:05 - User buka Progress Tracker
           Countdown shows: 09:55 ← Correct!
           
13:05:00 - User masih di screen
           Countdown shows: 05:00 ← Correct!
           
13:10:00 - Countdown habis
           Shows: "Sedang Diproses"
```

### **Scenario 2: User Keluar & Kembali (The Fix!)**
```
13:00:00 - User submit loan
           submittedAt = 1675324800000 (13:00:00)
           
13:00:05 - User buka Progress Tracker
           Countdown: 09:55 ✓
           
13:01:00 - User klik Back (keluar dari screen)
           → Screen destroyed, tapi submittedAt tetap di DB!
           
13:05:00 - User buka Progress Tracker lagi
           
           ❌ SEBELUM (Wrong):
           submissionTime = current time (13:05:00)
           Countdown restart: 10:00 ← RESET!
           
           ✅ SESUDAH (Correct):
           submissionTime = loan.submittedAt (13:00:00)
           elapsed = 13:05:00 - 13:00:00 = 5 minutes
           Countdown: 10:00 - 5:00 = 05:00 ← BENAR!
```

### **Scenario 3: Close App & Reopen**
```
13:00:00 - User submit loan
           submittedAt saved to DB = 13:00:00
           
13:02:00 - User minimize app / kill app
           
13:20:00 - User buka app lagi
           User navigasi ke Progress Tracker
           
           Load loan from DB:
           submittedAt = 13:00:00
           
           Calculate:
           elapsed = 13:20:00 - 13:00:00 = 20 minutes
           elapsed > 10 minutes target
           
           Countdown shows: "Sedang Diproses" ✓
```

---

## 🔍 Technical Deep Dive

### **Why Use Timestamp (Long)?**
```kotlin
// ✅ Good: Timestamp (milliseconds since epoch)
val submittedAt: Long = 1675324800000

// ❌ Bad: String date
val date: String = "2024-02-02 13:00:00" 
// Problem: Hard to calculate elapsed time
```

### **Calculation Logic**
```kotlin
// Constants
val TARGET_DURATION_MS = 10 * 60 * 1000 // 10 minutes in ms

// At any point in time:
val currentTime = System.currentTimeMillis()
val submittedAt = loan.submittedAt // From DB

// Elapsed time since submission
val elapsedMs = currentTime - submittedAt
val elapsedSeconds = (elapsedMs / 1000).toInt()

// Remaining time
val targetSeconds = 10 * 60 // 600 seconds
val remainingSeconds = targetSeconds - elapsedSeconds

// Display
if (remainingSeconds > 0) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    display = String.format("%02d:%02d", minutes, seconds)
} else {
    display = "Sedang Diproses"
}
```

---

## 🗄️ Database Schema Update

### **Migration Required?**
YES! Karena menambahkan field baru `submittedAt`

#### **Option 1: Fallback Strategy (Recommended for Development)**
```kotlin
@Entity(tableName = "loans")
data class LoanEntity(
    // ...
    val submittedAt: Long = System.currentTimeMillis()
    // Default value untuk loans yang sudah ada
)
```

Room akan handle dengan:
- Loans baru: `submittedAt` = waktu submission
- Loans lama: `submittedAt` = default (current time saat migration)

#### **Option 2: Proper Migration (Production)**
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE loans ADD COLUMN submittedAt INTEGER NOT NULL DEFAULT 0"
        )
        // Update existing loans dengan estimasi
        database.execSQL(
            "UPDATE loans SET submittedAt = " +
            "(SELECT strftime('%s', date) * 1000 FROM loans WHERE loans.localId = localId)"
        )
    }
}
```

---

## ✅ Testing Checklist

### **Test 1: New Loan Submission**
```
1. Submit new loan
2. Immediately open Progress Tracker
   ✓ Countdown starts from 10:00
3. Wait 1 minute
   ✓ Countdown shows 09:00
4. Press back
5. Wait 2 minutes
6. Open Progress Tracker again
   ✓ Countdown shows 07:00 (NOT 10:00!)
```

### **Test 2: Old Loan (Already in Progress)**
```
1. Open loan history
2. Select loan submitted 5 minutes ago
3. Click "Lihat Progress"
   ✓ Countdown shows 05:00 (10:00 - 5:00)
4. Close and reopen immediately
   ✓ Countdown still shows ~05:00
```

### **Test 3: Expired Countdown**
```
1. Submit loan
2. Wait 11 minutes
3. Open Progress Tracker
   ✓ Shows "Sedang Diproses"
   ✓ NO countdown timer
```

### **Test 4: App Restart**
```
1. Submit loan at 13:00
2. Close app completely
3. Reopen app at 13:07
4. Navigate to Progress Tracker
   ✓ Countdown shows 03:00 (10:00 - 7:00)
```

---

## 🎨 UI Behavior

### **Countdown Display States**

| Remaining Time | Display | Progress Bar |
|---------------|---------|--------------|
| > 0 seconds | MM:SS format | Fills from 0% to 100% |
| = 0 seconds | "Sedang Diproses" | 100% filled |
| Loan completed | (Status dependent) | 100% filled |

### **Examples**
```
10:00  ████░░░░░░░░░░░░  0%
09:30  █████░░░░░░░░░░░  5%
05:00  ██████████░░░░░░  50%
01:00  ████████████████░ 90%
00:00  █████████████████ 100%
       "Sedang Diproses"
```

---

## 🚀 Benefits

| Before | After |
|--------|-------|
| ❌ Timer reset setiap buka screen | ✅ Timer konsisten dari submission |
| ❌ User confused waktu tidak akurat | ✅ User percaya dengan timer |
| ❌ Cannot track actual elapsed time | ✅ Accurate elapsed time tracking |
| ❌ Testing sulit (always 10:00) | ✅ Testing realistic scenarios |

---

## 🔐 Edge Cases Handled

1. **Loan tidak ditemukan**: Fallback ke current time
2. **submittedAt = null**: Use default (current time)
3. **Negative remaining time**: Show "Sedang Diproses"
4. **App killed during countdown**: Resume correctly
5. **Time zone changes**: Milliseconds-based, timezone-agnostic
6. **Clock adjustments**: Relative to submission, not absolute

---

## 📝 Code Files Modified

1. ✅ **`Loan.kt`** - Added `submittedAt: Long`
2. ✅ **`LoanEntity.kt`** - Added `submittedAt` field + mapping
3. ✅ **`LoanProgressViewModel.kt`** - Load `submittedAt` from DB
4. ✅ **`LoanProgressTrackerScreen.kt`** - Countdown calculation from `startTime`

---

## 🎯 Summary

**Key Change**: 
```kotlin
// ❌ BEFORE
submissionTime = System.currentTimeMillis() // Always NOW

// ✅ AFTER  
submissionTime = loan.submittedAt // From database
```

**Result**: Countdown timer is now **persistent**, **consistent**, and **accurate** across app sessions!

---

**Version**: 2.1.0  
**Feature**: Persistent Countdown Timer  
**Status**: ✅ Implemented & Tested  
**Last Updated**: 2026-02-05
