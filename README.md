
# 🍏 NeuroDiet – AI Powered Health & Lifestyle App  
**Android (Kotlin + XML) + Appwrite Backend + OSM Maps**

NeuroDiet is an AI-assisted **health & lifestyle coaching app** that focuses on hydration, nutrition, smart reminders, and daily motivation.  
The app uses **Appwrite** as backend and **OpenStreetMap (OSM)** for map-based features.

---

## 📱 Features

### 🏠 Home Dashboard  
- Daily water tracking  
- Calorie & meal summary  
- Weight / progress graphs  
- Wellness score  
- Quick actions (Log Water, Scan Food, Today's Plan)  

### 🍽 Diet Assistant  
- AI-powered meal plan generator  
- OCR-based food scanning  
- Daily meal logging  
- Weekly shopping list generator  

### 🗺 Maps (Using OSM)  
- Nearby **dieticians**  
- Nearby **gyms**  
- OSM-based location picker  
- Route preview & distance estimation  

### 🎮 Gamification  
- Reward points  
- Streak tracking  
- Healthy habit badges  

### 🔐 Authentication  
- **Login with Google (via Appwrite OAuth)**  
- Automatic profile creation  
- Secure per-user data  

---

## 🏗 Tech Stack

### Android  
- **Kotlin + XML**  
- MVVM architecture  
- Coroutines + Flow  
- Coil (images)  
- MPAndroidChart (graphs)  
- OSMdroid (OpenStreetMap)  

### Backend  
- **Appwrite**  
  - Auth (Google OAuth)  
  - Database (document collections)  
  - Storage (OCR images, profile pics)  
  - Functions (AI endpoints integration)  

### AI (Handled by other team members)  
- Meal plan generation  
- Food OCR → nutrition estimation  
- Wellness score computation  
- Smart notifications engine  

---

## 🗺 OSM (OpenStreetMap) Integration

NeuroDiet uses **OSMdroid** for rendering maps and showing nearby health services.

### Libraries  
Add to `build.gradle`:

```gradle
implementation 'org.osmdroid:osmdroid-android:6.1.16'
implementation 'org.osmdroid:osmbonuspack:6.7.0'
```

### Features implemented with OSM  
- Show user location (GPS)  
- Load OSM tiles  
- Custom markers for dieticians & gyms  
- Bottom sheet with place details  
- Long-press to choose custom location  
- MapView lifecycle handled properly  

---

## 🗂 Appwrite Setup

## Required Services  
✔ Users (Google OAuth)  
✔ Database  
✔ Storage  
✔ Functions (for AI integrations)

---

## 🔒 Authentication (Google Login)

1. Enable **Google OAuth2** in Appwrite Console  
2. Add package name + SHA1 fingerprint  
3. In Android, call:

```kotlin
account.createOAuth2Session("google", successUrl, failureUrl)
```

User is redirected back with a valid Appwrite session.

---

# 🧱 Database Schema (Appwrite Collections)

### 1. `user_profiles`
Stores user health basics:
- name  
- height, weight  
- goals  
- activity level  
- dietary preferences  

### 2. `daily_logs`
- date  
- water_ml  
- calories  
- wellness_score  

### 3. `meal_logs`
- meal_type  
- calories  
- nutrition macros  
- source = (manual / ocr / ai_plan)

### 4. `meal_plans`
AI-generated flexible meal plans.

### 5. `water_logs`
Timestamped water entries.

### 6. `shopping_lists`
Auto-generated + user edited lists.

### 7. `rewards`
Points, streaks, badges.

Each document uses:
⚠ **Permissions → Only user can read/write their own data**

---

# 📦 Project Structure

```
app/
 ├─ data/
 │   ├─ models/
 │   ├─ repositories/
 │   ├─ remote/ (Appwrite services)
 │   └─ ai/ (interfaces + mock implementations)
 ├─ ui/
 │   ├─ auth/
 │   ├─ home/
 │   ├─ diet/
 │   ├─ maps/
 │   └─ profile/
 ├─ utils/
 └─ App.kt
```

---

# 🧩 AI Integration (Stubs Only)

Your Android side defines **interfaces**:

```kotlin
interface MealPlanAIService {
    suspend fun generateMealPlan(profile: UserProfile, date: String): MealPlan
}

interface NutritionAIService {
    suspend fun analyzeFoodImage(image: ByteArray): OcrNutritionResult
}

interface WellnessScoreAIService {
    suspend fun computeScore(log: DailyLog, meals: List<MealLog>): Int
}

interface SmartNotificationAIService {
    suspend fun getSuggestions(date: String): List<SmartNotification>
}
```

Your teammates replace mock implementations with real endpoints later.

---

# 🎨 UI (XML)

NeuroDiet uses Material Design + wellness aesthetics.

Includes:
- `activity_auth.xml`
- `activity_main.xml`
- `fragment_home.xml`
- `fragment_diet.xml`
- `fragment_maps.xml` (OSM MapView placeholder)
- `fragment_profile.xml`
- List items (`item_meal.xml`, `item_place.xml`, etc.)

---

# 🚀 Running the App

### 1. Clone the repo  
```
git clone https://github.com/yourname/NeuroDiet.git
```

### 2. Add `local.properties`  
```
APPWRITE_ENDPOINT=https://cloud.appwrite.io/v1
APPWRITE_PROJECT_ID=XXXXXXXX
APPWRITE_DATABASE_ID=XXXXXXXX
OSM_USER_AGENT=your.package.name
```

### 3. Run on physical device (recommended)  
OSM needs real GPS data for accurate map display.

---

# 🛡 Security

✔ All Appwrite collections use `userId == loggedInUserId`  
✔ No API keys hardcoded in code  
✔ OSM user agent required to avoid throttling  
✔ Local caching enabled for map tiles  

---

# 🤝 Contributing

PRs welcome!  
AI model developers should implement the interfaces inside `data/ai/`.

---

# 📜 License

MIT License
