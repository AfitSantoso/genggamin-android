# Dokumentasi Endpoint API - Genggamin Application

Dokumentasi lengkap semua endpoint yang tersedia dalam aplikasi Genggamin untuk memudahkan integrasi frontend.

**Base URL**: `http://localhost:8080` (development)

**Format Response Standar**:
Endpoint menggunakan wrapper `ApiResponse<T>`:

```json
{
  "success": true/false,
  "message": "Pesan status",
  "data": {} // berisi data response sesuai endpoint
}
```

---

## 1. Auth Controller (`/auth`)

Controller untuk menangani autentikasi dan otorisasi user.

**File Terkait:**

- **Controller**: `controller/AuthController.java`
- **Service**: `service/UserService.java`, `service/TokenBlacklistService.java`, `service/PasswordResetService.java`, `service/EmailService.java`
- **Repository**: `repository/UserRepository.java`, `repository/PasswordResetTokenRepository.java`
- **Entity**: `entity/User.java`, `entity/Role.java`, `entity/PasswordResetToken.java`
- **DTO**: `dto/LoginRequest.java`, `dto/LoginResponse.java`, `dto/CreateUserRequest.java`, `dto/ForgotPasswordRequest.java`, `dto/ResetPasswordRequest.java`
- **Security**: `security/JwtUtil.java`
- **Config**: `config/SecurityConfig.java`, `config/RedisConfig.java`

### 1.1 POST `/auth/login`

**Deskripsi**: Login user dan mendapatkan JWT token  
**URL**: `POST /auth/login`  
**Akses**: Public (tanpa autentikasi)

**Request Body**:

```json
{
  "username": "string",
  "password": "string"
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "isActive": true,
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Error Response (401 Unauthorized)**:

```json
{
  "success": false,
  "message": "Invalid credentials",
  "data": null
}
```

**Fungsi**: Memvalidasi kredensial user, menggenerate JWT token yang berisi username dan roles. Token ini harus disimpan dan digunakan di header `Authorization: Bearer <token>` untuk endpoint yang memerlukan autentikasi.

---

### 1.2 POST `/auth/register`

**Deskripsi**: Mendaftarkan user baru  
**URL**: `POST /auth/register`  
**Akses**: Public (tanpa autentikasi)

**Request Body**:

```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "fullName": "string",
  "roles": ["CUSTOMER"] // optional, default: CUSTOMER
}
```

**Success Response (201 Created)**:

```json
{
  "success": true,
  "message": "User created",
  "data": {
    "id": 1
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Username already exists",
  "data": null
}
```

**Fungsi**: Membuat user baru dengan role default CUSTOMER. Field fullName akan digunakan untuk data customer. Field `roles` bersifat optional - jika tidak diisi, otomatis akan menjadi CUSTOMER.

**Role yang tersedia**: CUSTOMER, MARKETING, BRANCH_MANAGER, BACK_OFFICE, ADMIN

**Catatan Khusus**: Endpoint ini secara otomatis akan menetapkan role **CUSTOMER** kepada user yang mendaftar, mengabaikan role apapun yang dikirim dalam request body. Tindakan ini dilakukan untuk alasan keamanan agar user publik tidak bisa mendaftarkan diri sebagai staff atau admin.

---

### 1.3 POST `/auth/logout`

**Deskripsi**: Logout user dan blacklist JWT token  
**URL**: `POST /auth/logout`  
**Akses**: Authenticated (memerlukan token)  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Logout successful. Token has been invalidated.",
  "data": null
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Authorization header is missing or invalid",
  "data": null
}
```

**Fungsi**: Menambahkan token ke blacklist agar tidak bisa digunakan lagi, menggunakan Redis untuk menyimpan blacklist. Setelah logout, token yang sama tidak bisa digunakan lagi.

---

### 1.4 POST `/auth/forgot-password`

**Deskripsi**: Request reset password via email  
**URL**: `POST /auth/forgot-password`  
**Akses**: Public

**Request Body**:

```json
{
  "email": "user@example.com"
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Link reset password telah dikirim ke email Anda. Silakan cek inbox atau spam folder.",
  "data": {
    "token": "reset-token-example"
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Email not found",
  "data": null
}
```

**Fungsi**: Generate token reset password dan kirim link ke email user, token disimpan di Redis dengan expiry time. Link yang dikirim berisi token yang harus digunakan di endpoint reset password.

---

### 1.5 POST `/auth/reset-password`

**Deskripsi**: Reset password menggunakan token dari email  
**URL**: `POST /auth/reset-password`  
**Akses**: Public

**Request Body**:

```json
{
  "token": "string",
  "newPassword": "string"
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Password berhasil direset. Silakan login dengan password baru Anda.",
  "data": null
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null
}
```

**Fungsi**: Validasi token dari email, update password baru, dan hapus token dari Redis. Token hanya bisa digunakan sekali dan memiliki waktu expired.

---

### 1.6 POST `/auth/register/staff`

**Deskripsi**: Mendaftarkan akun untuk staff operational (Marketing, Branch Manager, Backoffice)  
**URL**: `POST /auth/register/staff`  
**Akses**: Authenticated (ADMIN Only)  
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "username": "budi_marketing",
  "password": "password123",
  "email": "budi@genggamin.co.id",
  "fullName": "Budi Santoso",
  "roles": ["MARKETING"]
}
```

**Success Response (201 Created)**:

```json
{
  "success": true,
  "message": "Staff user created successfully",
  "data": {
    "id": 4,
    "username": "budi_marketing",
    "email": "budi@genggamin.co.id",
    "fullName": "Budi Santoso",
    "isActive": true,
    "roles": ["MARKETING"]
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Role tidak valid untuk staff: ADMIN",
  "data": null
}
```

**Fungsi**: Khusus untuk Admin mendaftarkan staff internal. Melakukan validasi tambahan bahwa role yang diassign **HARUS** salah satu dari: `MARKETING`, `BRANCH_MANAGER`, atau `BACKOFFICE`. Tidak bisa digunakan untuk membuat akun Customer atau Admin.

---

## 2. User Controller (`/users`)

Controller untuk mengelola data user.

**File Terkait:**

- **Controller**: `controller/UserController.java`
- **Service**: `service/UserService.java`
- **Repository**: `repository/UserRepository.java`, `repository/RoleRepository.java`
- **Entity**: `entity/User.java`, `entity/Role.java`
- **DTO**: `dto/UserResponse.java`, `dto/CreateUserRequest.java`, `dto/ApiResponse.java`

### 2.1 GET `/users`

**Deskripsi**: Mendapatkan semua user  
**URL**: `GET /users`  
**Akses**: Authenticated  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Users retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "johndoe",
      "email": "john@example.com",
      "fullName": "John Doe",
      "isActive": true,
      "roles": ["CUSTOMER"]
    },
    {
      "id": 2,
      "username": "admin",
      "email": "admin@example.com",
      "fullName": "Admin User",
      "isActive": true,
      "roles": ["ADMIN"]
    }
  ]
}
```

**Empty Response (200 OK)**:

```json
{
  "success": true,
  "message": "No users found",
  "data": []
}
```

**Fungsi**: Retrieve semua user dari database dan convert ke UserResponse DTO (tanpa password).

---

### 2.2 POST `/users`

**Deskripsi**: Membuat user baru dengan role apapun (khusus Admin)
**URL**: `POST /users`  
**Akses**: Authenticated (ADMIN Only)
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "fullName": "string",
  "roles": ["ADMIN"]
}
```

**Success Response (201 Created)**:

```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": 3,
    "username": "newadmin",
    "email": "newadmin@example.com",
    "fullName": "New Admin User",
    "isActive": true,
    "roles": ["ADMIN"]
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Username already exists",
  "data": null
}
```

**Fungsi**: Membuat user baru dengan role bebas. Endpoint ini hanya bisa diakses oleh user dengan authority **ADMIN**. Response menggunakan format baku `ApiResponse<UserResponse>`.

---

### 2.3 GET `/users/{userId}`

**Deskripsi**: Mendapatkan data user berdasarkan ID  
**URL**: `GET /users/{userId}`  
**Akses**: User yang sedang login (hanya bisa akses data sendiri)  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "User retrieved successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "isActive": true,
    "roles": ["CUSTOMER"]
  }
}
```

**Error Response (403 Forbidden)**:

```json
{
  "success": false,
  "message": "You don't have permission to access this user",
  "data": null
}
```

**Fungsi**: Validasi bahwa user yang request adalah user yang sama dengan ID yang diminta, mencegah user lain mengakses data pribadi.

---

### 2.4 PUT `/users/staff/{id}`

**Deskripsi**: Mengubah data user staff (Marketing, Branch Manager, Backoffice)  
**URL**: `PUT /users/staff/{id}`  
**Akses**: Authenticated (ADMIN Only)  
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "username": "budi_updated",
  "email": "budi_baru@genggamin.co.id",
  "fullName": "Budi Santoso Update",
  "password": "newpassword123", // Opsional
  "isActive": true,
  "roles": ["BRANCH_MANAGER"]
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Staff user updated successfully",
  "data": {
    "id": 4,
    "username": "budi_updated",
    "email": "budi_baru@genggamin.co.id",
    "fullName": "Budi Santoso Update",
    "isActive": true,
    "roles": ["BRANCH_MANAGER"]
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Role tidak valid untuk staff: ADMIN",
  "data": null
}
```

**Fungsi**: Khusus untuk Admin mengubah data staff internal. Validasi role sama dengan endpoint register staff (hanya `MARKETING`, `BRANCH_MANAGER`, `BACK_OFFICE`).

---

## 3. Role Controller (`/roles`)

Controller untuk mengelola role/permissions.

**File Terkait:**

- **Controller**: `controller/RoleController.java`
- **Service**: `service/RoleService.java`
- **Repository**: `repository/RoleRepository.java`
- **Entity**: `entity/Role.java`
- **Config**: `config/DataInitializer.java` (untuk inisialisasi default roles)

### 3.1 GET `/roles`

**Deskripsi**: Mendapatkan semua role yang tersedia  
**URL**: `GET /roles`  
**Akses**: Authenticated  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Roles retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "CUSTOMER",
      "description": "Customer role for loan applications"
    },
    {
      "id": 2,
      "name": "MARKETING",
      "description": "Marketing role for reviewing loans"
    },
    {
      "id": 3,
      "name": "BRANCH_MANAGER",
      "description": "Branch Manager role for approving loans"
    },
    {
      "id": 4,
      "name": "BACK_OFFICE",
      "description": "Back Office role for disbursing loans"
    },
    {
      "id": 5,
      "name": "ADMIN",
      "description": "Administrator with full access"
    }
  ]
}
```

**Fungsi**: Retrieve semua role dari database untuk keperluan assignment atau dropdown selection.

---

### 3.2 POST `/roles`

**Deskripsi**: Membuat role baru  
**URL**: `POST /roles`  
**Akses**: Authenticated  
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "name": "SUPERVISOR",
  "description": "Supervisor role"
}
```

**Success Response (201 Created)**:

```json
{
  "success": true,
  "message": "Role created successfully",
  "data": {
    "id": 6,
    "name": "SUPERVISOR",
    "description": "Supervisor role"
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Role name already exists",
  "data": null
}
```

**Fungsi**: Create role baru untuk sistem RBAC. Role name harus unique.

---

## 4. Customer Controller (`/customers`)

Controller untuk mengelola data profil customer/nasabah.

**File Terkait:**

- **Controller**: `controller/CustomerController.java`
- **Service**: `service/CustomerService.java`, `service/UserService.java`
- **Repository**: `repository/CustomerRepository.java`, `repository/EmergencyContactRepository.java`, `repository/UserRepository.java`
- **Entity**: `entity/Customer.java`, `entity/EmergencyContact.java`, `entity/User.java`
- **DTO**: `dto/CustomerRequest.java`, `dto/CustomerResponse.java`, `dto/EmergencyContactRequest.java`, `dto/EmergencyContactResponse.java`, `dto/ApiResponse.java`

### 4.1 POST `/customers/profile`

**Deskripsi**: Membuat atau update profil customer dengan foto KTP, Selfie, dan Slip Gaji (Payslip)  
**URL**: `POST /customers/profile`  
**Akses**: Authenticated (user yang sedang login)  
**Header**: `Authorization: Bearer <token>`
**Content-Type**: `multipart/form-data`

**Request Body (Multipart Form Data)**:

| Key       | Type       | Description                                                                    |
| :-------- | :--------- | :----------------------------------------------------------------------------- |
| `data`    | JSONString | Data profil customer dalam bentuk JSON String (Content-Type: application/json) |
| `ktp`     | File       | File gambar KTP (Wajib untuk user baru)                                        |
| `selfie`  | File       | File gambar Selfie dengan KTP (Wajib untuk user baru)                          |
| `payslip` | File       | File gambar Slip Gaji (Optional tapi direkomendasikan)                         |

**Contoh value untuk `data`**:

```json
{
  "nik": "3201234567890123",
  "dateOfBirth": "1990-05-15",
  "placeOfBirth": "Jakarta",
  "address": "Jl. Sudirman No. 123, Jakarta",
  "phone": "081234567890",
  "monthlyIncome": 10000000,
  "occupation": "Software Engineer",
  "currentAddress": "Jl. Thamrin No. 456, Jakarta",
  "motherMaidenName": "Siti Aminah",
  "accountNumber": "1234567890",
  "accountHolderName": "John Doe",
  "emergencyContact": {
    "name": "Jane Doe",
    "relationship": "Spouse",
    "phone": "081234567899"
  }
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Customer profile saved successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "nik": "3201234567890123",
    "address": "Jl. Sudirman No. 123, Jakarta",
    "dateOfBirth": "1990-05-15",
    "placeOfBirth": "Jakarta",
    "monthlyIncome": 10000000,
    "occupation": "Software Engineer",
    "customerPhone": "081234567890",
    "currentAddress": "Jl. Thamrin No. 456, Jakarta",
    "motherMaidenName": "Siti Aminah",
    "accountNumber": "1234567890",
    "accountHolderName": "John Doe",
    "ktpImagePath": "http://localhost:8080/uploads/kyc/John_Doe-3201234567890123_KTP.jpg",
    "selfieImagePath": "http://localhost:8080/uploads/kyc/John_Doe-3201234567890123_SELFIE.jpg",
    "payslipImagePath": "http://localhost:8080/uploads/kyc/John_Doe-3201234567890123_PAYSLIP.jpg",
    "emergencyContacts": [
      {
        "id": 1,
        "name": "Jane Doe",
        "relationship": "Spouse",
        "phone": "081234567899"
      }
    ],
    "createdAt": "2026-01-09T10:30:00"
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "KTP and Selfie photos are mandatory for new profile profile.",
  "data": null
}
```

**Fungsi**: Jika user belum punya data customer, akan create baru. Jika sudah ada, akan update data yang ada. Field email dan fullName diambil dari tabel users (saat register).

---

### 4.2 GET `/customers/profile`

**Deskripsi**: Mendapatkan profil customer user yang sedang login  
**URL**: `GET /customers/profile`  
**Akses**: Authenticated  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Customer profile retrieved successfully",
  "data": {
    "id": 1,
    "userId": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "nik": "3201234567890123",
    "address": "Jl. Sudirman No. 123, Jakarta",
    "dateOfBirth": "1990-05-15",
    "placeOfBirth": "Jakarta",
    "monthlyIncome": 10000000,
    "occupation": "Software Engineer",
    "customerPhone": "081234567890",
    "currentAddress": "Jl. Thamrin No. 456, Jakarta",
    "motherMaidenName": "Siti Aminah",
    "accountNumber": "1234567890",
    "accountHolderName": "John Doe",
    "ktpImagePath": "http://localhost:8080/uploads/kyc/John_Doe-3201234567890123_KTP.jpg",
    "selfieImagePath": "http://localhost:8080/uploads/kyc/John_Doe-3201234567890123_SELFIE.jpg",
    "payslipImagePath": "http://localhost:8080/uploads/kyc/John_Doe-3201234567890123_PAYSLIP.jpg",
    "emergencyContacts": [
      {
        "id": 1,
        "name": "Jane Doe",
        "relationship": "Spouse",
        "phone": "081234567899"
      }
    ],
    "createdAt": "2026-01-09T10:30:00"
  }
}
```

**Error Response (404 Not Found)**:

```json
{
  "success": false,
  "message": "Customer profile not found",
  "data": null
}
```

**Fungsi**: Retrieve data customer lengkap termasuk emergency contact berdasarkan user ID yang sedang login.

---

### 4.3 GET `/customers/has-profile`

**Deskripsi**: Cek apakah user sudah memiliki data customer  
**URL**: `GET /customers/has-profile`  
**Akses**: Authenticated  
**Header**: `Authorization: Bearer <token>`

**Success Response - Has Profile (200 OK)**:

```json
{
  "success": true,
  "message": "Customer profile exists",
  "data": true
}
```

**Success Response - No Profile (200 OK)**:

```json
{
  "success": true,
  "message": "Customer profile not found",
  "data": false
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Error checking profile",
  "data": false
}
```

**Fungsi**: Quick check untuk menentukan apakah user perlu mengisi data customer atau tidak. Berguna untuk conditional rendering di frontend (tampilkan form jika belum ada profile).

---

### 4.4 PUT `/customers/profile`

**Deskripsi**: Update profil customer (Data + File Dokumen)  
**URL**: `PUT /customers/profile`  
**Akses**: Authenticated  
**Header**: `Authorization: Bearer <token>`
**Content-Type**: `multipart/form-data`

**Request Body**: Sama seperti POST `/customers/profile` (Multipart Form Data)

**Success Response (200 OK)**:
Success response sama seperti POST `/customers/profile`.

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Update failed",
  "data": null
}
```

**Fungsi**: Khusus untuk update data customer yang sudah ada. Support partial update untuk file (jika file tidak dikirim/null, file lama tetap dipertahankan).
{
"success": false,
"message": "Customer profile not found",
"data": null
}

````

**Fungsi**: Khusus untuk update data customer yang sudah ada (alternative dari POST yang bisa create atau update).

---

### 4.5 GET `/customers/{customerId}`

**Deskripsi**: Mendapatkan detail profil customer berdasarkan ID customer (bukan User ID).
**URL**: `GET /customers/{customerId}`
**Akses**: Role MARKETING, BRANCH_MANAGER, BACK_OFFICE, ADMIN
**Header**: `Authorization: Bearer <token>`

**Path Variable**: `customerId` (Long) - ID dari customer

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Customer profile retrieved successfully",
  "data": {
    "id": 5,
    "userId": 10,
    "username": "customer_target",
    "email": "customer@example.com",
    "fullName": "Target Customer",
    "nik": "3201234567890123",
    "address": "Jl. Customer No. 123",
    "dateOfBirth": "1995-10-20",
    "placeOfBirth": "Surabaya",
    "monthlyIncome": 15000000,
    "occupation": "Manager",
    "customerPhone": "081234567800",
    "currentAddress": "Jl. Customer No. 123",
    "motherMaidenName": "Ibu Customer",
    "ktpImagePath": "http://localhost:8080/uploads/kyc/Customer_Target-3201234567890123_KTP.jpg",
    "selfieImagePath": "http://localhost:8080/uploads/kyc/Customer_Target-3201234567890123_SELFIE.jpg",
    "payslipImagePath": "http://localhost:8080/uploads/kyc/Customer_Target-3201234567890123_PAYSLIP.jpg",
    "emergencyContacts": [],
    "createdAt": "2026-01-11T14:30:00"
  }
}
```

**Error Response (404 Not Found)**:

```json
{
  "success": false,
  "message": "Customer not found with id: 5",
  "data": null
}
```

**Error Response (403 Forbidden)**:

```json
{
  "timestamp": "2026-01-12T10:00:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "path": "/customers/5"
}
```

**Fungsi**: Digunakan oleh staff (Marketing, Branch Manager, Back Office) untuk melihat detail data customer, termasuk mengecek dokumen KYC (KTP, Selfie, Slip Gaji) melalui URL yang disediakan.

---

## 5. Loan Controller (`/loans`)

Controller untuk mengelola pengajuan dan proses approval pinjaman dengan workflow RBAC.

**File Terkait:**

- **Controller**: `controller/LoanController.java`
- **Service**: `service/LoanService.java`, `service/CustomerService.java`, `service/PlafondService.java`
- **Repository**: `repository/LoanRepository.java`, `repository/CustomerRepository.java`, `repository/PlafondRepository.java`, `repository/UserRepository.java`
- **Entity**: `entity/Loan.java`, `entity/Customer.java`, `entity/Plafond.java`, `entity/User.java`, `entity/LoanReview.java`, `entity/LoanApproval.java`, `entity/LoanDisbursement.java`
- **DTO**: `dto/LoanRequest.java`, `dto/LoanResponse.java`, `dto/LoanActionRequest.java`, `dto/LoanWithReviewResponse.java`, `dto/LoanWithApprovalResponse.java`, `dto/LoanWithDisbursementResponse.java`, `dto/ApiResponse.java`
- **Enum**: `LoanStatus` (SUBMITTED, REVIEWED, APPROVED, DISBURSED, REJECTED)

### 5.1 POST `/loans/submit`

**Deskripsi**: Customer submit pengajuan pinjaman
**URL**: `POST /loans/submit`
**Akses**: Role CUSTOMER atau ADMIN
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "plafondId": 1,
  "amount": 50000000,
  "purpose": "Kredit Pemilikan Rumah",
  "tenor": 12
}
````

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loan submitted successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "plafondId": 1,
    "amount": 50000000,
    "tenureMonths": 12,
    "interestRate": 8.5,
    "purpose": "Kredit Pemilikan Rumah",
    "status": "SUBMITTED",
    "reviewNotes": null,
    "approvalNotes": null,
    "disbursementNotes": null,
    "submittedAt": "2026-01-09T10:30:00",
    "reviewedAt": null,
    "approvedAt": null,
    "disbursedAt": null,
    "reviewedBy": null,
    "approvedBy": null,
    "disbursedBy": null
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Customer profile is incomplete. Please complete your profile first."
}
```

**Fungsi**: Customer mengajukan pinjaman, validasi kelengkapan data customer dan plafond yang dipilih. Interest rate diambil otomatis dari plafond.

---

### 5.1.a GET `/loans/my-limits`

**Deskripsi**: Mendapatkan informasi limit pinjaman customer (total dan sisa limit per plafond)
**URL**: `GET /loans/my-limits`
**Akses**: Role CUSTOMER atau ADMIN
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Limits retrieved successfully",
  "data": [
    {
      "id": 1,
      "plafondId": 1,
      "plafondTitle": "Pinjaman Personal",
      "totalLimit": 50000000,
      "availableLimit": 45000000,
      "isLocked": false
    }
  ]
}
```

**Fungsi**: Customer dapat melihat berapa limit yang mereka miliki untuk setiap jenis plafond, serta sisa limit yang masih bisa digunakan.

---

### 5.2 GET `/loans/my-loans`

**Deskripsi**: Customer melihat semua pengajuan pinjaman miliknya  
**URL**: `GET /loans/my-loans`  
**Akses**: Role CUSTOMER atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "johndoe",
      "plafondId": 1,
      "amount": 50000000,
      "tenureMonths": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "SUBMITTED",
      "reviewNotes": null,
      "approvalNotes": null,
      "disbursementNotes": null,
      "submittedAt": "2026-01-09T10:30:00",
      "reviewedAt": null,
      "approvedAt": null,
      "disbursedAt": null,
      "reviewedBy": null,
      "approvedBy": null,
      "disbursedBy": null
    },
    {
      "id": 2,
      "username": "johndoe",
      "plafondId": 2,
      "amount": 30000000,
      "tenureMonths": 24,
      "interestRate": 9.0,
      "purpose": "Kredit Kendaraan",
      "status": "APPROVED",
      "reviewNotes": "Data lengkap, disetujui untuk review",
      "approvalNotes": "Disetujui untuk pencairan",
      "disbursementNotes": null,
      "submittedAt": "2026-01-08T10:30:00",
      "reviewedAt": "2026-01-08T14:00:00",
      "approvedAt": "2026-01-08T16:00:00",
      "disbursedAt": null,
      "reviewedBy": "marketing1",
      "approvedBy": "manager1",
      "disbursedBy": null
    }
  ]
}
```

**Fungsi**: Retrieve semua loan yang disubmit oleh customer yang sedang login, lengkap dengan history approval.

---

### 5.3 GET `/loans/review`

**Deskripsi**: Marketing melihat loan yang perlu direview  
**URL**: `GET /loans/review`  
**Akses**: Role MARKETING atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "johndoe",
      "plafondId": 1,
      "amount": 50000000,
      "tenureMonths": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "SUBMITTED",
      "reviewNotes": null,
      "approvalNotes": null,
      "disbursementNotes": null,
      "submittedAt": "2026-01-09T10:30:00",
      "reviewedAt": null,
      "approvedAt": null,
      "disbursedAt": null,
      "reviewedBy": null,
      "approvedBy": null,
      "disbursedBy": null
    }
  ]
}
```

**Fungsi**: Menampilkan antrian loan dengan status SUBMITTED yang perlu direview oleh marketing.

---

### 5.4 POST `/loans/review/{loanId}`

**Deskripsi**: Marketing mereview loan (approve/reject)  
**URL**: `POST /loans/review/{loanId}`  
**Akses**: Role MARKETING atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "action": "APPROVE",
  "notes": "Data lengkap, disetujui untuk review lanjutan"
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loan reviewed successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "plafondId": 1,
    "amount": 50000000,
    "tenureMonths": 12,
    "interestRate": 8.5,
    "purpose": "Kredit Pemilikan Rumah",
    "status": "REVIEWED",
    "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
    "approvalNotes": null,
    "disbursementNotes": null,
    "submittedAt": "2026-01-09T10:30:00",
    "reviewedAt": "2026-01-09T11:00:00",
    "approvedAt": null,
    "disbursedAt": null,
    "reviewedBy": "marketing1",
    "approvedBy": null,
    "disbursedBy": null
  }
}
```

**Request Body untuk Reject**:

```json
{
  "action": "REJECT",
  "notes": "Dokumen tidak lengkap"
}
```

**Success Response untuk Reject (200 OK)**:

```json
{
  "success": true,
  "message": "Loan reviewed successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "plafondId": 1,
    "amount": 50000000,
    "tenureMonths": 12,
    "interestRate": 8.5,
    "purpose": "Kredit Pemilikan Rumah",
    "status": "REJECTED",
    "reviewNotes": "Dokumen tidak lengkap",
    "approvalNotes": null,
    "disbursementNotes": null,
    "submittedAt": "2026-01-09T10:30:00",
    "reviewedAt": "2026-01-09T11:00:00",
    "approvedAt": null,
    "disbursedAt": null,
    "reviewedBy": "marketing1",
    "approvedBy": null,
    "disbursedBy": null
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Loan not found or not in SUBMITTED status"
}
```

**Fungsi**: Marketing review loan, jika APPROVE akan lanjut ke status REVIEWED (menunggu Branch Manager), jika REJECT akan langsung REJECTED.

**Action yang tersedia**: APPROVE, REJECT

---

### 5.5 GET `/loans/reviewed`

**Deskripsi**: Marketing/Admin melihat semua loan yang sudah direview  
**URL**: `GET /loans/reviewed`  
**Akses**: Role MARKETING atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Reviewed loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "customerId": 1,
      "plafondId": 1,
      "loanAmount": 50000000,
      "tenorMonth": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "REVIEWED",
      "submissionDate": "2026-01-09T10:30:00",
      "createdAt": "2026-01-09T10:30:00",
      "updatedAt": "2026-01-09T11:00:00",
      "reviewId": 1,
      "reviewedBy": 2,
      "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
      "reviewStatus": "APPROVED",
      "reviewedAt": "2026-01-09T11:00:00"
    }
  ]
}
```

**Fungsi**: Menampilkan semua loan yang sudah melalui proses review dengan detail review lengkap.

---

### 5.6 GET `/loans/approve`

**Deskripsi**: Branch Manager melihat loan yang perlu approval  
**URL**: `GET /loans/approve`  
**Akses**: Role BRANCH_MANAGER atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "johndoe",
      "plafondId": 1,
      "amount": 50000000,
      "tenureMonths": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "REVIEWED",
      "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
      "approvalNotes": null,
      "disbursementNotes": null,
      "submittedAt": "2026-01-09T10:30:00",
      "reviewedAt": "2026-01-09T11:00:00",
      "approvedAt": null,
      "disbursedAt": null,
      "reviewedBy": "marketing1",
      "approvedBy": null,
      "disbursedBy": null
    }
  ]
}
```

**Fungsi**: Menampilkan loan dengan status REVIEWED yang sudah direview marketing dan menunggu approval branch manager.

---

### 5.7 POST `/loans/approve/{loanId}`

**Deskripsi**: Branch Manager approve/reject loan  
**URL**: `POST /loans/approve/{loanId}`  
**Akses**: Role BRANCH_MANAGER atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "action": "APPROVE",
  "notes": "Disetujui untuk pencairan"
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loan processed successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "plafondId": 1,
    "amount": 50000000,
    "tenureMonths": 12,
    "interestRate": 8.5,
    "purpose": "Kredit Pemilikan Rumah",
    "status": "APPROVED",
    "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
    "approvalNotes": "Disetujui untuk pencairan",
    "disbursementNotes": null,
    "submittedAt": "2026-01-09T10:30:00",
    "reviewedAt": "2026-01-09T11:00:00",
    "approvedAt": "2026-01-09T12:00:00",
    "disbursedAt": null,
    "reviewedBy": "marketing1",
    "approvedBy": "manager1",
    "disbursedBy": null
  }
}
```

**Request Body untuk Reject**:

```json
{
  "action": "REJECT",
  "notes": "Tidak memenuhi kriteria kredit"
}
```

**Success Response untuk Reject (200 OK)**:

```json
{
  "success": true,
  "message": "Loan processed successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "plafondId": 1,
    "amount": 50000000,
    "tenureMonths": 12,
    "interestRate": 8.5,
    "purpose": "Kredit Pemilikan Rumah",
    "status": "REJECTED",
    "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
    "approvalNotes": "Tidak memenuhi kriteria kredit",
    "disbursementNotes": null,
    "submittedAt": "2026-01-09T10:30:00",
    "reviewedAt": "2026-01-09T11:00:00",
    "approvedAt": "2026-01-09T12:00:00",
    "disbursedAt": null,
    "reviewedBy": "marketing1",
    "approvedBy": "manager1",
    "disbursedBy": null
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Loan not found or not in REVIEWED status"
}
```

**Fungsi**: Branch manager melakukan final approval, jika APPROVE akan lanjut ke status APPROVED (siap dicairkan), jika REJECT akan REJECTED.

**Action yang tersedia**: APPROVE, REJECT

---

### 5.8 GET `/loans/approved`

**Deskripsi**: Branch Manager/Admin melihat semua loan yang sudah diapprove/reject  
**URL**: `GET /loans/approved`  
**Akses**: Role BRANCH_MANAGER atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Approved loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "customerId": 1,
      "plafondId": 1,
      "loanAmount": 50000000,
      "tenorMonth": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "APPROVED",
      "submissionDate": "2026-01-09T10:30:00",
      "createdAt": "2026-01-09T10:30:00",
      "updatedAt": "2026-01-09T12:00:00",
      "approvalId": 1,
      "approvedBy": 3,
      "approvalStatus": "APPROVED",
      "approvalNotes": "Disetujui untuk pencairan",
      "approvedAt": "2026-01-09T12:00:00"
    }
  ]
}
```

**Fungsi**: Menampilkan semua loan yang sudah melalui proses approval dengan detail approval lengkap.

---

### 5.9 GET `/loans/disburse`

**Deskripsi**: Back Office melihat loan yang siap dicairkan  
**URL**: `GET /loans/disburse`  
**Akses**: Role BACK_OFFICE atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "johndoe",
      "plafondId": 1,
      "amount": 50000000,
      "tenureMonths": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "APPROVED",
      "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
      "approvalNotes": "Disetujui untuk pencairan",
      "disbursementNotes": null,
      "submittedAt": "2026-01-09T10:30:00",
      "reviewedAt": "2026-01-09T11:00:00",
      "approvedAt": "2026-01-09T12:00:00",
      "disbursedAt": null,
      "reviewedBy": "marketing1",
      "approvedBy": "manager1",
      "disbursedBy": null
    }
  ]
}
```

**Fungsi**: Menampilkan loan dengan status APPROVED yang sudah diapprove dan siap untuk proses pencairan.

---

### 5.10 POST `/loans/disburse/{loanId}`

**Deskripsi**: Back Office mencairkan dana loan  
**URL**: `POST /loans/disburse/{loanId}`  
**Akses**: Role BACK_OFFICE atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "action": "DISBURSE",
  "notes": "Dana telah ditransfer ke rekening customer"
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loan disbursed successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "plafondId": 1,
    "amount": 50000000,
    "tenureMonths": 12,
    "interestRate": 8.5,
    "purpose": "Kredit Pemilikan Rumah",
    "status": "DISBURSED",
    "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
    "approvalNotes": "Disetujui untuk pencairan",
    "disbursementNotes": "Dana telah ditransfer ke rekening customer",
    "submittedAt": "2026-01-09T10:30:00",
    "reviewedAt": "2026-01-09T11:00:00",
    "approvedAt": "2026-01-09T12:00:00",
    "disbursedAt": "2026-01-09T13:00:00",
    "reviewedBy": "marketing1",
    "approvedBy": "manager1",
    "disbursedBy": "backoffice1"
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Loan not found or not in APPROVED status"
}
```

**Fungsi**: Back office melakukan pencairan dana, mengubah status loan menjadi DISBURSED. Ini adalah tahap final dalam workflow loan.

**Action yang tersedia**: DISBURSE

---

### 5.11 GET `/loans/disbursed`

**Deskripsi**: Back Office/Admin melihat semua loan yang sudah dicairkan  
**URL**: `GET /loans/disbursed`  
**Akses**: Role BACK_OFFICE atau ADMIN  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Disbursed loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "customerId": 1,
      "plafondId": 1,
      "loanAmount": 50000000,
      "tenorMonth": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "DISBURSED",
      "submissionDate": "2026-01-09T10:30:00",
      "createdAt": "2026-01-09T10:30:00",
      "updatedAt": "2026-01-09T13:00:00",
      "disbursementId": 1,
      "disbursedBy": 4,
      "disbursementAmount": 50000000,
      "disbursementDate": "2026-01-09T13:00:00",
      "bankAccount": "1234567890",
      "disbursementStatus": "COMPLETED"
    }
  ]
}
```

**Fungsi**: Menampilkan semua loan yang sudah dicairkan dengan detail disbursement lengkap.

---

### 5.12 GET `/loans/all`

**Deskripsi**: Admin melihat semua loan  
**URL**: `GET /loans/all`  
**Akses**: Role ADMIN only  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "All loans retrieved successfully",
  "data": [
    {
      "id": 1,
      "username": "johndoe",
      "plafondId": 1,
      "amount": 50000000,
      "tenureMonths": 12,
      "interestRate": 8.5,
      "purpose": "Kredit Pemilikan Rumah",
      "status": "DISBURSED",
      "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
      "approvalNotes": "Disetujui untuk pencairan",
      "disbursementNotes": "Dana telah ditransfer ke rekening customer",
      "submittedAt": "2026-01-09T10:30:00",
      "reviewedAt": "2026-01-09T11:00:00",
      "approvedAt": "2026-01-09T12:00:00",
      "disbursedAt": "2026-01-09T13:00:00",
      "reviewedBy": "marketing1",
      "approvedBy": "manager1",
      "disbursedBy": "backoffice1"
    },
    {
      "id": 2,
      "username": "janedoe",
      "plafondId": 2,
      "amount": 30000000,
      "tenureMonths": 24,
      "interestRate": 9.0,
      "purpose": "Kredit Kendaraan",
      "status": "SUBMITTED",
      "reviewNotes": null,
      "approvalNotes": null,
      "disbursementNotes": null,
      "submittedAt": "2026-01-09T14:00:00",
      "reviewedAt": null,
      "approvedAt": null,
      "disbursedAt": null,
      "reviewedBy": null,
      "approvedBy": null,
      "disbursedBy": null
    }
  ]
}
```

**Fungsi**: Admin dashboard untuk monitoring semua loan dari semua customer, semua status.

---

### 5.13 GET `/loans/{loanId}`

**Deskripsi**: Melihat detail loan berdasarkan ID  
**URL**: `GET /loans/{loanId}`  
**Akses**: Authenticated users  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loan retrieved successfully",
  "data": {
    "id": 1,
    "username": "johndoe",
    "plafondId": 1,
    "amount": 50000000,
    "tenureMonths": 12,
    "interestRate": 8.5,
    "purpose": "Kredit Pemilikan Rumah",
    "status": "DISBURSED",
    "reviewNotes": "Data lengkap, disetujui untuk review lanjutan",
    "approvalNotes": "Disetujui untuk pencairan",
    "disbursementNotes": "Dana telah ditransfer ke rekening customer",
    "submittedAt": "2026-01-09T10:30:00",
    "reviewedAt": "2026-01-09T11:00:00",
    "approvedAt": "2026-01-09T12:00:00",
    "disbursedAt": "2026-01-09T13:00:00",
    "reviewedBy": "marketing1",
    "approvedBy": "manager1",
    "disbursedBy": "backoffice1"
  }
}
```

**Error Response (400 Bad Request)**:

```json
{
  "success": false,
  "message": "Loan not found"
}
```

**Fungsi**: Retrieve detail loan tertentu berdasarkan ID, lengkap dengan history approval.

---

## 6. Plafond Controller (`/plafonds`)

Controller untuk mengelola master data plafond pinjaman (limit kredit berdasarkan income).

**File Terkait:**

- **Controller**: `controller/PlafondController.java`
- **Service**: `service/PlafondService.java`
- **Repository**: `repository/PlafondRepository.java`
- **Entity**: `entity/Plafond.java`
- **DTO**: `dto/PlafondRequest.java`, `dto/PlafondResponse.java`, `dto/ApiResponse.java`

### 6.1 GET `/plafonds`

**Deskripsi**: Mendapatkan semua plafond  
**URL**: `GET /plafonds`  
**Akses**: Public (tanpa autentikasi)

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Plafonds retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "Pinjaman Personal",
      "minIncome": 5000000,
      "maxAmount": 50000000,
      "tenorMonth": 12,
      "interestRate": 8.5,
      "isActive": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    },
    {
      "id": 2,
      "title": "Pinjaman Usaha",
      "minIncome": 10000000,
      "maxAmount": 100000000,
      "tenorMonth": 24,
      "interestRate": 7.5,
      "isActive": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    },
    {
      "id": 3,
      "title": "Pinjaman Kilat",
      "minIncome": 3000000,
      "maxAmount": 30000000,
      "tenorMonth": 12,
      "interestRate": 9.0,
      "isActive": false,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-05T00:00:00"
    }
  ]
}
```

**Empty Response (200 OK)**:

```json
{
  "success": true,
  "message": "No plafonds found",
  "data": []
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to retrieve plafonds: Database connection error"
}
```

**Fungsi**: Retrieve semua plafond termasuk yang tidak aktif untuk keperluan informasi dan administrasi.

---

### 6.2 GET `/plafonds/active`

**Deskripsi**: Mendapatkan hanya plafond yang aktif  
**URL**: `GET /plafonds/active`  
**Akses**: Public

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Active plafonds retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "Pinjaman Personal",
      "minIncome": 5000000,
      "maxAmount": 50000000,
      "tenorMonth": 12,
      "interestRate": 8.5,
      "isActive": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    },
    {
      "id": 2,
      "title": "Pinjaman Usaha",
      "minIncome": 10000000,
      "maxAmount": 100000000,
      "tenorMonth": 24,
      "interestRate": 7.5,
      "isActive": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    }
  ]
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to retrieve active plafonds: Database connection error"
}
```

**Fungsi**: Menampilkan plafond dengan status isActive = true yang tersedia untuk customer. Gunakan endpoint ini untuk dropdown selection di form pengajuan pinjaman.

---

### 6.3 GET `/plafonds/{id}`

**Deskripsi**: Mendapatkan detail plafond berdasarkan ID  
**URL**: `GET /plafonds/{id}`  
**Akses**: Public

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Plafond retrieved successfully",
  "data": {
    "id": 1,
    "title": "Pinjaman Personal",
    "minIncome": 5000000,
    "maxAmount": 50000000,
    "tenorMonth": 12,
    "interestRate": 8.5,
    "isActive": true,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
}
```

**Error Response (404 Not Found)**:

```json
{
  "success": false,
  "message": "Plafond not found with id: 99"
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to retrieve plafond: Database connection error"
}
```

**Fungsi**: Retrieve informasi detail plafond tertentu berdasarkan ID.

---

### 6.4 GET `/plafonds/by-income/{income}`

**Deskripsi**: Mendapatkan plafond berdasarkan income  
**URL**: `GET /plafonds/by-income/{income}`  
**Akses**: Public (untuk cek eligibilitas)

**Example**: `GET /plafonds/by-income/7500000`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Plafonds for income retrieved successfully",
  "data": [
    {
      "id": 1,
      "title": "Pinjaman Personal",
      "minIncome": 5000000,
      "maxAmount": 50000000,
      "tenorMonth": 12,
      "interestRate": 8.5,
      "isActive": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    }
  ]
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to retrieve plafonds: Invalid income format"
}
```

**Fungsi**: Customer bisa cek plafond apa saja yang eligible berdasarkan monthly income mereka. Plafond dikembalikan jika income >= minIncome dan hanya yang aktif.

---

### 6.4.a GET `/plafonds/simulate`

**Deskripsi**: Menghitung simulasi cicilan pinjaman (GET method)
**URL**: `GET /plafonds/simulate`
**Akses**: Public
**Query Params**:
- `amount` (BigDecimal, required): Jumlah pinjaman
- `tenor` (Long, required): Jangka waktu dalam bulan
- `plafondId` (Long, optional): ID plafond spesifik (untuk rate spesifik)

**Example**: `/plafonds/simulate?amount=10000000&tenor=12`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Loan simulation successful",
  "data": {
    "loanAmount": 10000000,
    "tenorMonth": 12,
    "interestRate": 0.83, // Monthly rate
    "monthlyInstallment": 916333,
    "totalInterest": 996000,
    "totalPayment": 10996000,
    "plafondId": 1
  }
}
```

**Fungsi**: Melakukan simulasi perhitungan cicilan tanpa harus login.

---

### 6.4.b POST `/plafonds/simulate`

**Deskripsi**: Menghitung simulasi cicilan pinjaman (POST method)
**URL**: `POST /plafonds/simulate`
**Akses**: Public

**Request Body**:

```json
{
  "amount": 10000000,
  "tenor": 12,
  "plafondId": 1 // Optional
}
```

**Success Response**: Sama dengan GET `/plafonds/simulate`

**Fungsi**: Alternatif simulasi menggunakan POST body.

---

### 6.5 POST `/plafonds`

**Deskripsi**: Membuat plafond baru  
**URL**: `POST /plafonds`  
**Akses**: Role ADMIN only  
**Header**: `Authorization: Bearer <token>`

**Request Body**:

```json
{
  "title": "Plafond Premium",
  "minIncome": 15000000,
  "maxAmount": 150000000,
  "tenorMonth": 36,
  "interestRate": 7.0,
  "isActive": true
}
```

**Success Response (201 Created)**:

```json
{
  "success": true,
  "message": "Plafond created successfully",
  "data": {
    "id": 4,
    "title": "Plafond Premium",
    "minIncome": 15000000,
    "maxAmount": 150000000,
    "tenorMonth": 36,
    "interestRate": 7.0,
    "isActive": true,
    "createdAt": "2026-01-09T10:30:00",
    "updatedAt": "2026-01-09T10:30:00"
  }
}
```

**Error Response (400 Bad Request) - Validation Error**:

```json
{
  "success": false,
  "message": "Validation error: Income range overlaps with existing plafond"
}
```

**Error Response (400 Bad Request) - Duplicate**:

```json
{
  "success": false,
  "message": "Plafond with this income range already exists"
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to create plafond: Database error"
}
```

**Fungsi**: Admin membuat konfigurasi plafond baru dengan validasi income range tidak overlap dengan plafond lain.

**Validasi**:

- minIncome harus lebih kecil dari maxIncome
- maxLoanAmount harus positif
- interestRate harus positif
- maxTenor harus positif
- Income range tidak boleh overlap dengan plafond lain yang aktif

---

### 6.6 PUT `/plafonds/{id}`

**Deskripsi**: Update plafond existing  
**URL**: `PUT /plafonds/{id}`  
**Akses**: Role ADMIN only  
**Header**: `Authorization: Bearer <token>`

**Request Body**: Sama seperti POST

```json
{
  "title": "Plafond Premium Updated",
  "minIncome": 15000000,
  "maxAmount": 200000000,
  "tenorMonth": 48,
  "interestRate": 6.5,
  "isActive": true
}
```

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Plafond updated successfully",
  "data": {
    "id": 4,
    "title": "Plafond Premium Updated",
    "minIncome": 15000000,
    "maxAmount": 200000000,
    "tenorMonth": 48,
    "interestRate": 6.5,
    "isActive": true,
    "createdAt": "2026-01-09T10:30:00",
    "updatedAt": "2026-01-09T11:00:00"
  }
}
```

**Error Response (400 Bad Request) - Validation Error**:

```json
{
  "success": false,
  "message": "Validation error: Income range overlaps with existing plafond"
}
```

**Error Response (404 Not Found)**:

```json
{
  "success": false,
  "message": "Plafond not found with id: 99"
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to update plafond: Database error"
}
```

**Fungsi**: Admin update konfigurasi plafond. Validasi yang sama dengan create berlaku.

---

### 6.7 DELETE `/plafonds/{id}`

**Deskripsi**: Soft delete plafond  
**URL**: `DELETE /plafonds/{id}`  
**Akses**: Role ADMIN only  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Plafond deleted successfully"
}
```

**Error Response (404 Not Found)**:

```json
{
  "success": false,
  "message": "Plafond not found with id: 99"
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to delete plafond: Database error"
}
```

**Fungsi**: Menandai plafond sebagai deleted (soft delete) dan mencatat siapa yang menghapus dan kapan. Data tidak benar-benar dihapus dari database, hanya ditandai dengan flag deleted. Plafond yang di-soft delete tidak akan muncul di endpoint `/plafonds/active` tapi masih muncul di `/plafonds`.

**Note**: Username admin yang melakukan delete akan tercatat di field `deletedBy`, dan timestamp di field `deletedAt`.

---

### 6.8 PATCH `/plafonds/{id}/toggle-status`

**Deskripsi**: Toggle status aktif/non-aktif plafond  
**URL**: `PATCH /plafonds/{id}/toggle-status`  
**Akses**: Role ADMIN only  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Plafond status toggled successfully",
  "data": {
    "id": 1,
    "title": "Pinjaman Personal",
    "minIncome": 5000000,
    "maxAmount": 50000000,
    "tenorMonth": 12,
    "interestRate": 8.5,
    "isActive": false,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-09T11:30:00"
  }
}
```

**Error Response (404 Not Found)**:

```json
{
  "success": false,
  "message": "Plafond not found with id: 99"
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to toggle plafond status: Database error"
}
```

**Fungsi**: Mengubah isActive menjadi true/false. Jika sebelumnya true akan menjadi false, dan sebaliknya. Berguna untuk temporarily menonaktifkan plafond tanpa menghapusnya. Plafond yang tidak aktif tidak akan muncul di `/plafonds/active` tapi masih bisa dilihat di `/plafonds`.

---

### 6.9 PATCH `/plafonds/{id}/restore`

**Deskripsi**: Restore plafond yang di-soft delete  
**URL**: `PATCH /plafonds/{id}/restore`  
**Akses**: Role ADMIN only  
**Header**: `Authorization: Bearer <token>`

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Plafond restored successfully",
  "data": {
    "id": 3,
    "title": "Pinjaman Kilat",
    "minIncome": 3000000,
    "maxAmount": 30000000,
    "tenorMonth": 12,
    "interestRate": 9.0,
    "isActive": true,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-09T12:00:00"
  }
}
```

**Error Response (404 Not Found)**:

```json
{
  "success": false,
  "message": "Plafond not found or not deleted"
}
```

**Error Response (500 Internal Server Error)**:

```json
{
  "success": false,
  "message": "Failed to restore plafond: Database error"
}
```

**Fungsi**: Mengembalikan plafond yang sudah di-soft delete, menghilangkan flag deleted dan deletedAt/deletedBy. Plafond akan kembali aktif dan tersedia untuk digunakan.

---

## 7. Notification Controller (`/api/notifications`)

Controller untuk mengelola notifikasi user.

**File Terkait:**

- **Controller**: `controller/NotificationController.java`
- **Service**: `service/NotificationService.java`
- **Repository**: `repository/NotificationRepository.java`
- **Entity**: `entity/Notification.java`
- **DTO**: `dto/NotificationResponse.java`

### 7.1 GET `/api/notifications`

**Deskripsi**: Mendapatkan daftar notifikasi user yang sedang login (Paged)
**URL**: `GET /api/notifications`
**Akses**: Authenticated (Customer & Staff)
**Query Parameters**:

- `page` (optional): Halaman ke-berapa (default: 0)
- `size` (optional): Jumlah item per halaman (default: 20)

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Notifications fetched",
  "data": {
    "content": [
      {
        "id": 1,
        "type": "LOAN_APPROVED",
        "title": "Loan Approved!",
        "message": "Your loan application has been approved.",
        "read": false,
        "createdAt": "2026-01-12T10:00:00",
        "loanId": 5
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
      },
      "offset": 0,
      "unpaged": false,
      "paged": true
    },
    "totalPages": 1,
    "totalElements": 1,
    "last": true,
    "size": 20,
    "number": 0,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "numberOfElements": 1,
    "first": true,
    "empty": false
  }
}
```

**Fungsi**: Menampilkan history notifikasi user.

---

### 7.2 GET `/api/notifications/unread-count`

**Deskripsi**: Mendapatkan jumlah notifikasi yang belum dibaca
**URL**: `GET /api/notifications/unread-count`
**Akses**: Authenticated

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Unread count",
  "data": 5
}
```

**Fungsi**: Untuk badge notifikasi di UI.

---

### 7.3 PATCH `/api/notifications/{id}/read`

**Deskripsi**: Menandai satu notifikasi sebagai sudah dibaca
**URL**: `PATCH /api/notifications/{id}/read`
**Akses**: Authenticated

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Marked as read",
  "data": null
}
```

**Fungsi**: Dipanggil saat user mengklik/membuka notifikasi tertentu.

---

### 7.4 PATCH `/api/notifications/read-all`

**Deskripsi**: Menandai semua notifikasi user sebagai sudah dibaca
**URL**: `PATCH /api/notifications/read-all`
**Akses**: Authenticated

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "All marked as read",
  "data": null
}
```

**Fungsi**: Fitur "Mark all as read".

---

---

## 8. General / Health Check

Controller untuk pengecekan status aplikasi.

**File Terkait:**

- **Controller**: `controller/HealthController.java`

### 8.1 GET `/`

**Deskripsi**: Root endpoint untuk informasi aplikasi
**URL**: `GET /`
**Akses**: Public

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Application info retrieved successfully",
  "data": {
    "application": "Genggamin API",
    "status": "running",
    "timestamp": "2026-01-25T18:00:00",
    "message": "Welcome to Genggamin Loan Management API",
    "documentation": "/swagger-ui.html"
  }
}
```

---

### 8.2 GET `/health`

**Deskripsi**: Health check endpoint untuk monitoring
**URL**: `GET /health` atau `GET /api/health`
**Akses**: Public

**Success Response (200 OK)**:

```json
{
  "success": true,
  "message": "Application is healthy",
  "data": {
    "status": "UP",
    "timestamp": "2026-01-25T18:00:00"
  }
}
```

---

## Workflow Proses Loan

Berikut adalah alur lengkap proses pengajuan pinjaman dari awal hingga pencairan:

```
1. CUSTOMER submit loan → Status: SUBMITTED
   POST /loans/submit
   ↓
2. MARKETING review → Status: REVIEWED (approve) / REJECTED (reject)
   GET /loans/review (untuk lihat antrian)
   POST /loans/review/{loanId} dengan action APPROVE/REJECT
   ↓
3. BRANCH_MANAGER approve → Status: APPROVED (approve) / REJECTED (reject)
   GET /loans/approve (untuk lihat antrian)
   POST /loans/approve/{loanId} dengan action APPROVE/REJECT
   ↓
4. BACK_OFFICE disburse → Status: DISBURSED
   GET /loans/disburse (untuk lihat antrian)
   POST /loans/disburse/{loanId} dengan action DISBURSE
```

**Status Loan**:

- `SUBMITTED`: Loan baru diajukan customer, menunggu review marketing
- `REVIEWED`: Loan sudah direview dan disetujui marketing, menunggu approval branch manager
- `APPROVED`: Loan sudah diapprove branch manager, menunggu pencairan back office
- `DISBURSED`: Loan sudah dicairkan, proses selesai
- `REJECTED`: Loan ditolak (bisa di tahap review atau approval)

---

## Fitur Keamanan

1. **JWT Authentication**: Semua endpoint (kecuali public) memerlukan JWT token di header `Authorization: Bearer <token>`
2. **Role-Based Access Control (RBAC)**: Setiap endpoint memiliki pembatasan role tertentu, tidak bisa diakses oleh role lain
3. **Token Blacklist**: Logout menggunakan Redis untuk blacklist token, token yang sudah di-logout tidak bisa digunakan lagi
4. **Password Reset**: Menggunakan token dengan expiry time di Redis, token hanya bisa digunakan sekali
5. **Soft Delete**: Data penting tidak dihapus permanen dari database, hanya ditandai sebagai deleted
6. **User Data Privacy**: User hanya bisa akses data dirinya sendiri, tidak bisa akses data user lain

---

## Roles dalam Sistem

1. **CUSTOMER**: Mengajukan pinjaman dan melihat status pinjaman mereka
    - POST /loans/submit
    - GET /loans/my-loans
2. **MARKETING**: Review pengajuan pinjaman customer
    - GET /loans/review
    - POST /loans/review/{loanId}
    - GET /loans/reviewed
3. **BRANCH_MANAGER**: Approve/reject pinjaman yang sudah direview
    - GET /loans/approve
    - POST /loans/approve/{loanId}
    - GET /loans/approved
4. **BACK_OFFICE**: Mencairkan dana pinjaman yang sudah diapprove
    - GET /loans/disburse
    - POST /loans/disburse/{loanId}
    - GET /loans/disbursed
5. **ADMIN**: Akses penuh ke semua endpoint, manage master data
    - Semua endpoint di atas
    - GET /loans/all
    - POST /plafonds, PUT /plafonds/{id}, DELETE /plafonds/{id}
    - PATCH /plafonds/{id}/toggle-status, PATCH /plafonds/{id}/restore

---

## Format Data Standar

### Tanggal dan Waktu

- **Format Input**: `yyyy-MM-dd` untuk tanggal (contoh: `2026-01-09`)
- **Format Output**: `yyyy-MM-ddTHH:mm:ss` untuk datetime (contoh: `2026-01-09T10:30:00`)

### Decimal/Numeric

- **monthlyIncome**: BigDecimal, contoh: `10000000` (tanpa titik atau koma)
- **amount**: BigDecimal, contoh: `50000000`
- **interestRate**: BigDecimal (persentase), contoh: `8.5` untuk 8.5%

### Boolean

- **isActive**: `true` atau `false`
- **hasProfile**: `true` atau `false`

---

## Error Handling

Semua endpoint menggunakan format error response yang konsisten:

### Error Response Format

```json
{
  "success": false,
  "message": "Error message here",
  "data": null
}
```

### HTTP Status Codes

- **200 OK**: Request berhasil
- **201 Created**: Resource berhasil dibuat
- **400 Bad Request**: Validation error atau bad input
- **401 Unauthorized**: Token tidak valid atau missing
- **403 Forbidden**: User tidak memiliki akses ke resource
- **404 Not Found**: Resource tidak ditemukan
- **500 Internal Server Error**: Server error

---

## Teknologi yang Digunakan

- **Spring Boot 3.x**: Framework utama
- **Spring Security**: Autentikasi dan otorisasi
- **JWT (JSON Web Token)**: Token-based authentication
- **Redis**: Token blacklist dan password reset token caching
- **PostgreSQL**: Database relational utama
- **Lombok**: Reduce boilerplate code
- **JPA/Hibernate**: ORM untuk database operations

---

## Tips Implementasi Frontend

### 1. Authentication Flow

```javascript
// Login
const login = async (username, password) => {
  const response = await fetch("http://localhost:8080/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  const data = await response.json();
  // Simpan token di localStorage atau sessionStorage
  localStorage.setItem("token", data.token);
  localStorage.setItem("user", JSON.stringify(data));
};

// Gunakan token di setiap request
const fetchWithAuth = async (url, options = {}) => {
  const token = localStorage.getItem("token");
  return fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });
};
```

### 2. Check Profile Completion

```javascript
// Cek apakah user sudah mengisi profile customer
const checkProfile = async () => {
  const response = await fetchWithAuth(
    "http://localhost:8080/customers/has-profile",
  );
  const data = await response.json();
  if (!data.data) {
    // Redirect ke halaman isi profile
    router.push("/profile/create");
  }
};
```

### 3. Handle Response

```javascript
// Handle API response dengan ApiResponse wrapper
const handleResponse = async (response) => {
  const data = await response.json();

  if (data.success) {
    return data.data;
  } else {
    throw new Error(data.message);
  }
};

// Contoh penggunaan
try {
  const response = await fetchWithAuth("http://localhost:8080/loans/my-loans");
  const loans = await handleResponse(response);
  console.log(loans);
} catch (error) {
  console.error("Error:", error.message);
  // Show error message to user
}
```

### 4. Loan Status Display

```javascript
// Helper untuk display status loan dengan warna
const getLoanStatusBadge = (status) => {
  const statusConfig = {
    SUBMITTED: { color: "blue", text: "Menunggu Review" },
    REVIEWED: { color: "yellow", text: "Menunggu Approval" },
    APPROVED: { color: "green", text: "Menunggu Pencairan" },
    DISBURSED: { color: "success", text: "Selesai" },
    REJECTED: { color: "red", text: "Ditolak" },
  };
  return statusConfig[status];
};
```

### 5. Form Validation

```javascript
// Validasi sebelum submit loan
const validateLoanSubmission = (formData) => {
  const errors = {};

  if (!formData.plafondId) {
    errors.plafondId = "Plafond harus dipilih";
  }

  if (!formData.amount || formData.amount <= 0) {
    errors.amount = "Jumlah pinjaman harus lebih dari 0";
  }

  if (!formData.purpose || formData.purpose.trim() === "") {
    errors.purpose = "Tujuan pinjaman harus diisi";
  }

  if (!formData.tenor || formData.tenor <= 0) {
    errors.tenor = "Tenor harus lebih dari 0";
  }

  return errors;
};
```

### 6. Date Formatting

```javascript
// Format date untuk display
const formatDate = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleDateString("id-ID", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

// Format date untuk input (yyyy-MM-dd)
const formatDateForInput = (date) => {
  return date.toISOString().split("T")[0];
};
```

### 7. Currency Formatting

```javascript
// Format currency untuk display
const formatCurrency = (amount) => {
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    minimumFractionDigits: 0,
  }).format(amount);
};
```

---

## Contoh Sequence Diagram

### Customer Loan Application Flow

```
Customer -> Frontend: Submit loan application
Frontend -> API: POST /loans/submit
API -> Database: Save loan (status: SUBMITTED)
API -> Frontend: Return loan data
Frontend -> Customer: Show success message

Marketing -> Frontend: View pending loans
Frontend -> API: GET /loans/review
API -> Database: Get SUBMITTED loans
API -> Frontend: Return loan list
Frontend -> Marketing: Display loan list

Marketing -> Frontend: Review loan (APPROVE)
Frontend -> API: POST /loans/review/{id}
API -> Database: Update loan (status: REVIEWED)
API -> Frontend: Return updated loan
Frontend -> Marketing: Show success message

... (continue with Branch Manager and Back Office)
```

---

## Troubleshooting

### 1. Token Expired atau Invalid

**Problem**: Response 401 Unauthorized  
**Solution**: Token mungkin sudah expired atau invalid. Lakukan login ulang untuk mendapatkan token baru.

### 2. Forbidden Access

**Problem**: Response 403 Forbidden  
**Solution**: User tidak memiliki role yang tepat untuk mengakses endpoint. Cek role user dan pastikan endpoint yang diakses sesuai dengan role.

### 3. Profile Not Complete

**Problem**: Error "Customer profile is incomplete"  
**Solution**: User harus mengisi data customer terlebih dahulu di endpoint POST /customers/profile sebelum bisa submit loan.

### 4. Validation Error

**Problem**: Response 400 Bad Request dengan validation message  
**Solution**: Cek kembali format data yang dikirim, pastikan semua field required sudah diisi dengan format yang benar.

---

## Changelog

### Version 1.0.0 (Current)

- Auth Controller: Login, Register, Logout, Forgot Password, Reset Password
- User Controller: CRUD users, get user by ID
- Role Controller: Manage roles
- Customer Controller: Create/Update/Get customer profile
- Loan Controller: Complete loan workflow (Submit, Review, Approve, Disburse)
- Plafond Controller: Manage plafond master data with soft delete
- JWT Authentication with Redis blacklist
- Role-Based Access Control (RBAC)
- Soft delete implementation for Plafond

---

## Contact & Support

Untuk pertanyaan atau bantuan lebih lanjut mengenai API ini, silakan hubungi:

- **Email**: support@genggamin.com
- **Documentation**: [Project GitHub](https://github.com/yourproject/genggamin)

---

**Last Updated**: January 9, 2026  
**API Version**: 1.0.0
