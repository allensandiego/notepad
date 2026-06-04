# Notepad: Dynamic Relational Data Manager

A privacy-first, offline-ready Android application that empowers users to build custom relational tables, manage records locally using SQLite/Room, and seamlessly sync data in real-time to their own Supabase or self-hosted PostgreSQL database.



## 🚀 Key Features

*   **Dynamic Schema Designer**: Define custom collections/tables and manage fields of various types (`TEXT`, `NUMBER`, `BOOLEAN`, `DATE`, `FILE`). Supports advanced field default types like `STATIC` values, current day (`TODAY`), random `UUID`s, and `AUTO_INCREMENT` sequence keys.
*   **One-to-Many Relationships**: Define child collections within parent collections (e.g., *Patients* -> *Visits* -> *Prescriptions*). Design recursively nested schemas and edit records in-place recursively using Compose navigation stacks inside dialogs, with automatic foreign-key pre-population.
*   **Offline-Ready Architecture**: All read and write operations are executed locally against a transactional Room database. Changes are queued and synchronised to Supabase's PostgREST database API when a network connection is available.
*   **File Attachments**: Upload and attach files (Images, PDFs, Documents) to records. Files are cached locally for offline accessibility, synced to Supabase Storage, and can be viewed or downloaded directly from the record editor or list cards.
*   **Bugsnag Crash Reporting & Telemetry**: Automatically records unhandled crashes and logs detailed screen-flow navigation transitions (breadcrumbs) and handled operation failures.
*   **CI/CD Deployment**: Integrated GitHub Actions workflow for building, signing, and releasing the production bundle directly to Google Play.

---

## 🛠️ Technology Stack

*   **Core UI**: Jetpack Compose (Kotlin DSL), Material 3 Design
*   **Local Database**: Android Room Persistence Library (SQLite)
*   **Networking & Sync**: Ktor HTTP Client (OkHttp engine), Kotlinx Serialization
*   **Database Connectivity**: PostgreSQL JDBC Driver (for initial schema definition)
*   **Crash Reporting & Monitoring**: Bugsnag Error Monitoring & Performance SDKs
*   **Build System**: Gradle Kotlin DSL (`.gradle.kts`) with Version Catalogs (`libs.versions.toml`)

---

## 📁 Project Structure

```
├── app
│   ├── src
│   │   ├── main
│   │   │   ├── AndroidManifest.xml          # Declares activities, providers, permissions, and Bugsnag metadata
│   │   │   ├── java/com/allensandiego/notepad
│   │   │   │   ├── MainActivity.kt          # Host Activity initializing Room and Sync Engines
│   │   │   │   ├── NotepadApplication.kt    # Custom Application class starting Bugsnag
│   │   │   │   ├── db/                      # Room database, DAO, entities, and migration helpers
│   │   │   │   ├── sync/                    # Supabase PostgREST sync engine and storage client
│   │   │   │   ├── ui/                      # Jetpack Compose UI screens (Main, Edit, List, Designer, Connection, Theme)
│   │   │   │   └── util/                    # FileHelper (view/download) and TestDetector (DB isolation)
│   │   │   └── res/
│   │   │       ├── values/                  # Strings, colors, and Material 3 themes configuration
│   │   │       └── xml/                     # FileProvider paths and backup rules
│   │   ├── test/                            # Local JVM JUnit & Mockito unit tests (Schema helper and validations)
│   │   └── androidTest/                     # Instrumented UI tests (Compose rules running on physical Android devices)
│   └── build.gradle.kts                     # App-level build configs
├── build.gradle.kts                         # Root-level build configs
└── gradle.properties                        # Environment injects (leaves APKs installed after connected tests)
```

---

## ⚙️ Setup & Configuration

### 1. Supabase Backend Setup

To enable sync and user file attachments, configure your Supabase backend:

1. **Create a Project**: Create a new project at [supabase.com](https://supabase.com).
2. **Execute Schema SQL**:
   In the Supabase Dashboard, open the **SQL Editor**, create a new query, copy-paste the entire contents of [setup.sql](supabase/setup.sql), and click **Run**.
3. **Get API Credentials**:
   Go to **Project Settings -> API** and copy:
   *   `Project URL`
   *   `anon public` API Key
4. **Configure Mobile App**:
   Launch the Android app, go to the connection setup screen, and paste these credentials.

#### Database Tables Created:
*   `public.profiles`: Auto-created on user signup via an `auth.users` trigger.
*   `custom_tables`: Declares collections and hierarchical parent relationships.
*   `custom_fields`: Defines schema field types, constraints, defaults, and system flags.
*   `custom_records`: Stores individual record metadata.
*   `custom_values`: Relates record values back to fields.

#### Storage Buckets Provisioned:
*   `user-uploads`: Public bucket for files/attachments with path-level RLS. Anyone can read, but authenticated users can only write/modify files in their own folder (`user-uploads/<user_uuid>/...`).
*   `attachments`: App default media storage bucket.

### 2. Bugsnag Setup
To receive crash telemetry and performance metrics:
1. Obtain an API key from your Bugsnag Dashboard.
2. In [AndroidManifest.xml](file:///home/allen/Projects/notepad/app/src/main/AndroidManifest.xml), replace `YOUR_BUGSNAG_API_KEY` with your actual key:
   ```xml
   <meta-data
       android:name="com.bugsnag.android.API_KEY"
       android:value="your-api-key-here" />
   ```

### 3. Build & Install
Build the application debug build and install it to a connected ADB device or emulator:
```bash
./gradlew installDebug
```

---

## 🧪 Testing

The codebase includes separate test suites for local logic validation and full instrumentation tests:

*   **JVM Unit Tests**: Tests schema generation, default value resolution, dynamic validation, and tree ordering.
    ```bash
    ./gradlew test
    ```
*   **Instrumented UI Tests**: End-to-end user flows verifying collection creation, record editing, recursive navigation, and connection configurations on a live physical device.
    ```bash
    ./gradlew connectedAndroidTest
    ```

> [!TIP]
> **Test Environment Isolation**: During instrumented tests, the app automatically switches to an isolated database (`notepad_database_test`) and test preferences (`supabase_prefs_test`). This ensures your live manual test data and connection configurations on the test device are never altered or wiped out.

---

## 🎨 Credits & Attributions

*   App Icon: Designed by [Magnific](https://www.magnific.com) (Source: [Notepad Icon](https://www.magnific.com/icon/notepad_4105782#fromView=search&page=1&position=4&uuid=c4d236cd-4c03-483d-9875-991eac4005e0))

