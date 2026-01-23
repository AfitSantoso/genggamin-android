UI -> viewmodel -> LoginUseCase / RegisterUsecase / GetProducts -> Repository -> Datasource


Depdeencies injection -> Koin / Hilt
Appreference -> Data Persistence -> Local data storage (Mini)
Sqlite -> Room Database
Retrofit -> Untuk hit api  Pastikan setiap component itu reusable
Pastikan Project ini memenuhi SOLID PRINCIPLE
Pastikan project ini menggunakan design pattern MVVM
Pastikan project ini Offline First
Pastikan project ini terintegrasi dengan firebase untuk menerima push notifications    # Jetpack Compose Best Practice Folder Structure
Pastikan gunakan best response ya
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/yourcompany/yourapp/
│   │   │   ├── MainActivity.kt
│   │   │   ├── MyApplication.kt
│   │   │   │
│   │   │   ├── core/
│   │   │   │   ├── navigation/
│   │   │   │   │   ├── AppNavigation.kt
│   │   │   │   │   ├── NavigationDestinations.kt
│   │   │   │   │   └── NavigationArgs.kt
│   │   │   │   │
│   │   │   │   ├── network/
│   │   │   │   │   ├── ApiService.kt
│   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   └── interceptors/
│   │   │   │   │
│   │   │   │   ├── database/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   └── converters/
│   │   │   │   │
│   │   │   │   ├── util/
│   │   │   │   │   ├── Extensions.kt
│   │   │   │   │   ├── Constants.kt
│   │   │   │   │   └── DateTimeUtils.kt
│   │   │   │   │
│   │   │   │   └── di/
│   │   │   │       ├── AppModule.kt
│   │   │   │       ├── RepositoryModule.kt
│   │   │   │       └── UseCaseModule.kt
│   │   │   │
│   │   │   ├── data/
│   │   │   │   ├── model/
│   │   │   │   │   ├── dto/              # Data Transfer Objects (API responses)
│   │   │   │   │   │   ├── UserDto.kt
│   │   │   │   │   │   └── ProductDto.kt
│   │   │   │   │   │
│   │   │   │   │   └── entity/           # Database entities
│   │   │   │   │       ├── UserEntity.kt
│   │   │   │   │       └── ProductEntity.kt
│   │   │   │   │
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.kt
│   │   │   │   │   ├── UserRepositoryImpl.kt
│   │   │   │   │   └── ProductRepository.kt
│   │   │   │   │
│   │   │   │   ├── local/
│   │   │   │   │   ├── dao/
│   │   │   │   │   │   ├── UserDao.kt
│   │   │   │   │   │   └── ProductDao.kt
│   │   │   │   │   │
│   │   │   │   │   └── datastore/
│   │   │   │   │       └── PreferencesManager.kt
│   │   │   │   │
│   │   │   │   └── remote/
│   │   │   │       ├── api/
│   │   │   │       │   ├── UserApi.kt
│   │   │   │       │   └── ProductApi.kt
│   │   │   │       │
│   │   │   │       └── datasource/
│   │   │   │           ├── UserRemoteDataSource.kt
│   │   │   │           └── ProductRemoteDataSource.kt
│   │   │   │
│   │   │   ├── domain/
│   │   │   │   ├── model/                # Domain models (business logic)
│   │   │   │   │   ├── User.kt
│   │   │   │   │   └── Product.kt
│   │   │   │   │
│   │   │   │   ├── repository/           # Repository interfaces
│   │   │   │   │   ├── IUserRepository.kt
│   │   │   │   │   └── IProductRepository.kt
│   │   │   │   │
│   │   │   │   └── usecase/
│   │   │   │       ├── user/
│   │   │   │       │   ├── GetUserUseCase.kt
│   │   │   │       │   ├── UpdateUserUseCase.kt
│   │   │   │       │   └── DeleteUserUseCase.kt
│   │   │   │       │
│   │   │   │       └── product/
│   │   │   │           ├── GetProductsUseCase.kt
│   │   │   │           └── SearchProductsUseCase.kt
│   │   │   │
│   │   │   └── ui/
│   │   │       ├── theme/
│   │   │       │   ├── Color.kt
│   │   │       │   ├── Theme.kt
│   │   │       │   ├── Type.kt
│   │   │       │   └── Shape.kt
│   │   │       │
│   │   │       ├── components/           # Reusable UI components
│   │   │       │   ├── buttons/
│   │   │       │   │   ├── PrimaryButton.kt
│   │   │       │   │   └── SecondaryButton.kt
│   │   │       │   │
│   │   │       │   ├── cards/
│   │   │       │   │   └── ProductCard.kt
│   │   │       │   │
│   │   │       │   ├── dialogs/
│   │   │       │   │   └── ConfirmationDialog.kt
│   │   │       │   │
│   │   │       │   ├── inputs/
│   │   │       │   │   ├── CustomTextField.kt
│   │   │       │   │   └── SearchBar.kt
│   │   │       │   │
│   │   │       │   └── loading/
│   │   │       │       └── LoadingIndicator.kt
│   │   │       │
│   │   │       └── features/             # Feature modules
│   │   │           ├── home/
│   │   │           │   ├── HomeScreen.kt
│   │   │           │   ├── HomeViewModel.kt
│   │   │           │   ├── HomeUiState.kt
│   │   │           │   ├── HomeUiEvent.kt
│   │   │           │   └── components/
│   │   │           │       └── HomeHeader.kt
│   │   │           │
│   │   │           ├── profile/
│   │   │           │   ├── ProfileScreen.kt
│   │   │           │   ├── ProfileViewModel.kt
│   │   │           │   ├── ProfileUiState.kt
│   │   │           │   └── components/
│   │   │           │
│   │   │           ├── details/
│   │   │           │   ├── DetailsScreen.kt
│   │   │           │   ├── DetailsViewModel.kt
│   │   │           │   └── DetailsUiState.kt
│   │   │           │
│   │   │           └── auth/
│   │   │               ├── login/
│   │   │               │   ├── LoginScreen.kt
│   │   │               │   ├── LoginViewModel.kt
│   │   │               │   └── LoginUiState.kt
│   │   │               │
│   │   │               └── register/
│   │   │                   ├── RegisterScreen.kt
│   │   │                   ├── RegisterViewModel.kt
│   │   │                   └── RegisterUiState.kt
│   │   │
│   │   ├── res/
│   │   │   ├── drawable/
│   │   │   ├── mipmap/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │
│   │   └── AndroidManifest.xml
│   │
│   ├── test/                             # Unit tests
│   │   └── java/com/yourcompany/yourapp/
│   │       ├── domain/
│   │       │   └── usecase/
│   │       ├── data/
│   │       │   └── repository/
│   │       └── ui/
│   │           └── viewmodel/
│   │
│   └── androidTest/                      # Instrumented tests
│       └── java/com/yourcompany/yourapp/
│           ├── ui/
│           │   └── features/
│           └── database/
│
└── build.gradle.kts
```

## Key Architecture Principles

### 1. **Clean Architecture Layers**
- **Presentation Layer** (`ui/`): Composables, ViewModels, UI States
- **Domain Layer** (`domain/`): Business logic, Use Cases, Domain Models
- **Data Layer** (`data/`): Repositories, Data Sources, DTOs, Entities

### 2. **Feature-Based Organization**
Each feature in `ui/features/` contains:
- Screen composable
- ViewModel
- UI State classes
- UI Event classes
- Feature-specific components

### 3. **Separation of Concerns**
- **DTOs**: API response models
- **Entities**: Database models
- **Domain Models**: Pure business logic models
- **UI States**: Screen state representation

### 4. **Dependency Rules**
- Domain layer has no dependencies
- Data layer depends on Domain
- UI layer depends on Domain (and indirectly on Data through DI)

### 5. **Reusable Components**
Shared UI components in `ui/components/` organized by type (buttons, cards, inputs, etc.)

### 6. **Navigation**
Centralized navigation logic in `core/navigation/` using Compose Navigation

### 7. **Dependency Injection**
DI modules in `core/di/` using Hilt or Koin

## Benefits

- **Scalability**: Easy to add new features
- **Testability**: Clear separation enables isolated unit testing
- **Maintainability**: Predictable structure for team collaboration
- **Reusability**: Shared components and logic
- **Single Responsibility**: Each package has a clear purpose