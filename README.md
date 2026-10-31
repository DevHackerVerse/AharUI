# AharUI - AI-Powered Nutrition Tracker

AharUI is an Android application designed to help users track their dietary intake and achieve their health goals. It features an intelligent food scanning system that uses OCR and the Gemini AI to automatically extract and parse nutrition information from food labels, making meal logging quick and effortless.

## Key Features

*   **AI-Powered Food Scanning:** Scan food labels using your device's camera. The app utilizes ML Kit OCR to extract text and then leverages the Gemini AI to intelligently parse complex nutritional information.
*   **Diet Logging:** Keep a detailed log of your daily meals, including nutritional values like calories, protein, carbohydrates, and fat.
*   **Meal Planning:** Generate weekly meal plans tailored to your profile and dietary goals.
*   **User Profile:** Personalize your experience by setting up your profile with details like height, weight, target weight, activity level, and primary health goals.
*   **Progress Tracking:** Monitor your daily progress towards your nutritional targets.
*   **Water Tracking:** Log your daily water intake to stay hydrated.

## Technology Stack

*   **Language:** Kotlin
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **UI:** Android XML Layouts with Material Design Components
*   **Asynchronous Programming:** Kotlin Coroutines for managing background threads.
*   **Android Jetpack:**
    *   **Navigation Component:** For handling all in-app navigation.
    *   **ViewModel & LiveData:** For managing UI-related data in a lifecycle-conscious way.
    *   **Room Persistence Library:** For local, persistent storage of user data, meal logs, and more.
    *   **CameraX:** For a simplified and robust in-app camera implementation.
    *   **WorkManager:** For scheduling deferrable background tasks like notifications.
*   **Dependency Injection:** Hilt (Dagger) for managing dependencies.
*   **Networking:** Retrofit 2 & Gson for making API calls.
*   **AI & Machine Learning:**
    *   **Google's Gemini AI:** For enhanced, intelligent parsing of nutritional data from text.
    *   **Google's ML Kit:** For on-device text recognition (OCR) from images.

## Setup

1.  **Clone the Repository**
    ```bash
    git clone <your-repository-url>
    ```
2.  **Open in Android Studio**
    Open the cloned directory in Android Studio.
3.  **API Keys**
    You need to provide an API key for the Gemini AI service.
    *   Create a `local.properties` file in the root of the project.
    *   Add your API key to the file like this:
        ```properties
        GEMINI_API_KEY="YOUR_API_KEY_HERE"
        ```
    *   Sync the project with Gradle files.
4.  **Build and Run**
    Build the project and run it on an Android device or emulator.

