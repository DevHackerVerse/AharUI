# AharAI — Project Audit & Engineering Report (Sprint 0)
**AI-Powered Health & Nutrition Platform for Android**

* **Version:** 1.0.0
* **Date:** October 31, 2026
* **Repository:** `DevHackerVerse/AharUI`
* **Reference Document:** `AharAI_Execution_Plan_Antigravity.docx`

---

## 1. Executive Summary

**AharAI** is an AI-powered health and nutrition platform for Android engineered for Indian dietary context (dal, thali, sabzi, roti, biryani). Originating from the *Mumbai Hacks* hackathon (as *AharUI*), the project is transitioning from a hackathon prototype into a production-grade, offline-first Android application.

### Core Value Drivers:
1. **Indian-First Multimodal Vision:** Gemini 1.5 Flash multimodal food scanner tuned to decompose Indian meals into component items with accurate portion estimates.
2. **Zero-Trust Client Security:** Complete elimination of client-side API keys. Multimodal vision and AI coach queries are proxied via Appwrite Serverless Functions.
3. **Offline-First Resilience:** Room database caching guarantees full dashboard accessibility, manual meal logging, and water/streak tracking in zero-connectivity environments.
4. **Contextual Nutrition Intelligence:** 7-day intake trend evaluation, macro deficit alerts, and personalized BMR/TDEE tracking (Mifflin-St Jeor formula).

---

## 2. Technical Stack Matrix

| Layer | Approved Technology | Status in Codebase |
| :--- | :--- | :--- |
| **Android Language** | Kotlin 2.0.21 | Implemented |
| **UI Layer** | XML Layouts (Migrating to Jetpack Compose) | XML Layouts present |
| **Architecture** | MVVM + Clean Architecture + Hilt | Implemented |
| **Camera** | CameraX 1.3.1 | Implemented |
| **Offline DB** | Room Persistence 2.6.1 | Implemented (6 entities) |
| **Preferences** | Jetpack DataStore / SharedPreferences | Implemented |
| **Networking** | Retrofit 2.9.0 + OkHttp 4.12.0 | Implemented |
| **Cloud Backend** | Appwrite Cloud (`fra.cloud.appwrite.io`) | Pending SDK hookup |
| **Primary AI** | Gemini 1.5 Flash (via Appwrite Functions) | Direct SDK in client (needs migration) |
| **Barcode / Food DB** | ML Kit + Open Food Facts API | Scaffolded |
| **Maps & Locations** | OSMdroid 6.1.18 + Overpass API | Implemented |
| **Background Tasks**| AndroidX WorkManager | Implemented |

---

## 3. Sprint 0 Codebase Audit Findings

### 3.1 What Exists & Functions
* **Local Room Database (`AppDatabase.kt`)**: Fully configured with `UserProfileEntity`, `MealLogEntity`, `DailyLogEntity`, `WaterLogEntity`, `ShoppingListEntity`, and `RewardEntity` with TypeConverters.
* **Camera Capture (`ScanFoodFragment.kt`)**: CameraX `ProcessCameraProvider` and `ImageCapture` lifecycle bindings functional.
* **Domain Layer Use Cases**: Complete use cases for calorie calculation (`CalculateCalorieTargetUseCase`), meal logging (`LogMealUseCase`), water logging (`LogWaterUseCase`), streaks (`UpdateStreakUseCase`), and rewards (`BadgeUnlockUseCase`).
* **Geospatial & Mapping**: OSMdroid integration with `OverpassApiHelper` for nearby gym and dietitian queries.
* **Background Workers**: `MealReminderWorker` and `WaterReminderWorker` scheduled via WorkManager.

### 3.2 What Is Mocked, Partial, or Violating Guidelines
* **Security Non-Compliance (`GeminiService.kt`)**: Currently instantiates `com.google.ai.client.generativeai.GenerativeModel` directly inside the Android client with `BuildConfig.GEMINI_API_KEY`. Must be replaced with Appwrite Functions execution proxy.
* **Scan Pipeline Architecture**: Current food scanner relies on ML Kit OCR for text recognition on packaged labels rather than direct multimodal food image recognition.
* **Prompt Bias**: Current prompts generate Western foods (Oatmeal, Salmon, Quinoa) instead of Indian staples (Dal, Roti, Sabzi, Rice, Thali).
* **Mocked Auth Flow (`AuthActivity.kt`)**: Creates client-side random UUIDs saved to Room rather than Appwrite Google OAuth sessions.

### 3.3 Missing Components
* Appwrite Cloud Functions for Gemini 1.5 Flash proxy.
* ML Kit Barcode scanning pipeline for Open Food Facts.
* RAG Knowledge Base (ICMR / WHO guidelines) with vector database (Qdrant / Pinecone).
* GitHub Actions CI/CD pipeline.

---

## 4. Build System & Toolchain Notes

* **AGP Compatibility**: Android Gradle Plugin `8.13.2` is configured with Kotlin `2.0.21`.
* **Kapt vs Room Issue**: Room 2.6.1 with Kapt encountered a metadata version conflict when coroutines `1.11.0` was introduced by newer libraries.
* **Recommendation**: Migrate annotation processing from Kapt to KSP (`com.google.devtools.ksp`) for faster builds and seamless Kotlin 2.0+ metadata support.

---

## 5. 9-Sprint Execution Roadmap

* **Sprint 0:** Audit & Toolchain setup (KSP migration, CI/CD).
* **Sprint 1:** Appwrite Auth (Google OAuth), 5-step onboarding wizard, BMR calculator.
* **Sprint 2:** CameraX + Appwrite Function (Gemini Vision) + Indian food portion correction UI.
* **Sprint 3:** Barcode scanner (Open Food Facts) + 4-slot daily meal logging.
* **Sprint 4:** Interactive Dashboard (Calorie ring, macro bars, water counter, streak).
* **Sprint 5:** AI Nutrition Coach chat with 7-day user history context injection.
* **Sprint 6:** 7-Day Indian meal planner & auto-generated grocery shopping list.
* **Sprint 7:** RAG integration with ICMR guidelines (verified citations).
* **Sprint 8:** AI Memory, weekly PDF health report export, and gamification badges.
* **Sprint 9:** Production polish, Firebase Crashlytics, and portfolio presentation.
