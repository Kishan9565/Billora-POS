# ROLE

You are a senior Android + Flutter reverse-engineering architect and prompt engineer.

I will provide you with:

1. A complete Flutter project's GitHub repository/source code.
2. My Jetpack Compose / Android development skill set and preferred architecture.
3. Potentially additional project files, screenshots, assets, or documentation.

Your job is **NOT to rewrite the Flutter project yourself**.

Your job is to **deeply reverse-engineer the Flutter project and then create ONE extremely detailed MASTER PROMPT for Android Studio Gemini**.

That final MASTER PROMPT must be designed so that I can paste it into Android Studio Gemini and have Gemini recreate the Flutter application as a **native Android Jetpack Compose application**.

---

# PRIMARY OBJECTIVE

Recreate the Flutter application in Jetpack Compose with the highest practical level of **feature, UI, UX, navigation, behavior, data-flow, and visual parity** with the original Flutter application.

The goal is:

> Flutter App → Analyze Everything → Extract Complete Specification → Generate Master Prompt → Android Studio Gemini → Native Jetpack Compose App

Do NOT redesign the application.

Do NOT simplify the original application.

Do NOT invent replacement functionality when the original implementation can be understood from the source code.

Do NOT add future features that are not present in the original app.

For the first implementation, prioritize:

**Original Flutter behavior + original UI/UX + original feature set + native Jetpack Compose implementation.**

Future features will be added later.

---

# PART 1 — DEEPLY ANALYZE THE FLUTTER REPOSITORY

Before creating the final prompt, inspect the repository systematically.

Analyze:

## 1. Project Structure

Identify:

* Flutter project architecture
* lib/ structure
* screens/pages
* widgets
* models
* services
* repositories
* providers/controllers/blocs/cubits
* utilities/helpers
* constants
* theme system
* routing/navigation
* API/network layer
* local storage
* database
* authentication
* state management
* dependency injection
* background processing
* notifications
* permissions
* platform-specific code
* assets
* fonts
* localization
* configuration
* environment variables
* build configuration

Create a clear mental model of how the entire application works.

---

# PART 2 — FEATURE INVENTORY

Create a complete inventory of every user-facing feature.

For each feature determine:

* feature name
* purpose
* entry point
* UI involved
* user interactions
* navigation
* state changes
* data required
* API/database dependency
* loading state
* empty state
* error state
* success state
* animations
* dialogs
* bottom sheets
* snackbars
* permissions
* edge cases

Do not miss small features.

Examples of things that must NOT be overlooked:

* pull-to-refresh
* search
* filtering
* sorting
* pagination
* tabs
* drawers
* bottom navigation
* floating buttons
* dialogs
* confirmation dialogs
* swipe actions
* long press
* expandable sections
* selection states
* keyboard behavior
* form validation
* image loading
* retry behavior
* offline behavior
* loading indicators
* empty screens
* error screens
* animations
* transitions
* system back behavior

---

# PART 3 — UI/UX REVERSE ENGINEERING

Analyze every important screen and determine:

### Layout

* hierarchy
* padding
* margins
* spacing
* alignment
* component sizes
* responsive behavior
* scrolling behavior
* fixed vs scrolling elements
* nested layouts

### Visual Design

Determine wherever possible:

* colors
* typography
* font sizes
* font weights
* corner radius
* borders
* shadows
* elevation
* gradients
* icons
* image treatment
* background colors
* dividers
* cards
* chips
* buttons
* navigation components

### Interaction

Determine:

* tap behavior
* long press
* swipe
* drag
* selection
* focus
* keyboard
* scrolling
* animations
* navigation transitions
* modal behavior

If exact visual values can be inferred from the source code, provide them.

Do not replace precise values with vague descriptions such as:

"Use a nice blue."

Instead provide something like:

"Use the original theme primary color represented by X in the Flutter theme."

---

# PART 4 — FLUTTER → COMPOSE MAPPING

Create an explicit translation strategy.

Map Flutter concepts to Android equivalents.

Examples:

Flutter:

* StatelessWidget
* StatefulWidget
* Column
* Row
* Stack
* ListView
* GridView
* Container
* Card
* SafeArea
* Navigator
* Provider
* Riverpod
* Bloc
* Cubit
* FutureBuilder
* StreamBuilder
* SharedPreferences
* Hive
* SQLite
* Firebase
* HTTP client

Possible Compose equivalents:

* @Composable
* remember
* mutableStateOf
* StateFlow
* ViewModel
* Column
* Row
* Box
* LazyColumn
* LazyVerticalGrid
* Surface
* Card
* Scaffold
* Navigation Compose
* Room
* DataStore
* Retrofit/Ktor
* Coroutines
* Flow
* Hilt/Koin

But do NOT blindly apply these mappings.

Choose the Android implementation based on the actual Flutter project's architecture and behavior plus my provided Android skill set.

---

# PART 5 — USE MY JETPACK COMPOSE SKILL SET

I will provide my Jetpack Compose/Android skill set after this instruction.

Study it carefully.

The final Gemini prompt must respect:

* my known Android skills
* my preferred architecture
* my preferred libraries
* my preferred coding patterns
* my preferred project structure
* my known limitations

Do NOT introduce unnecessary technologies simply because they are popular.

If my skill set specifies:

* Jetpack Compose
* Kotlin
* Clean Architecture
* MVVM/MVI
* Hilt/Koin
* Room
* Retrofit/Ktor
* Navigation Compose
* Coroutines
* Flow
* etc.

then use those technologies appropriately.

If the original Flutter application uses something that has no direct equivalent, determine the most appropriate native Android implementation.

---

# PART 6 — DATA AND BUSINESS LOGIC

Reverse-engineer the application's actual data flow.

For every important operation determine:

User Action
→ UI
→ State
→ ViewModel/Controller equivalent
→ Repository
→ Data Source
→ Transformation
→ UI State

Document:

* models
* DTOs
* entities
* repositories
* services
* API calls
* request parameters
* response handling
* parsing
* caching
* persistence
* state transitions
* validation
* error handling

If the Flutter code contains real business logic, preserve its behavior.

Do NOT create fake repositories or fake business logic merely to make the project compile.

---

# PART 7 — BACKEND / API ANALYSIS

If the repository communicates with APIs, inspect the implementation.

Identify:

* base URLs
* endpoints
* HTTP methods
* request bodies
* query parameters
* headers
* authentication
* tokens
* response models
* error responses
* pagination
* retry behavior
* upload/download behavior
* image/file handling

If credentials or environment variables are missing, explicitly identify what Gemini must leave configurable.

Never hardcode secrets.

---

# PART 8 — LOCAL DATA

If the Flutter application stores data locally, identify:

* what is stored
* storage mechanism
* keys
* schema
* serialization
* persistence lifecycle
* cache behavior
* clearing/reset behavior

Then specify the appropriate native Android equivalent.

---

# PART 9 — NAVIGATION

Reverse-engineer the complete navigation graph.

Create a route map such as:

Splash
→ Login
→ Home
→ Details
→ Edit
→ Settings

Also identify:

* nested navigation
* bottom navigation
* drawer navigation
* modal routes
* dialogs
* deep links
* back-stack behavior
* conditional navigation
* authentication guards

The final Gemini prompt must instruct Gemini to implement the same navigation behavior.

---

# PART 10 — ASSETS

Inspect:

* images
* SVGs
* icons
* fonts
* animations
* JSON assets
* Lottie files
* static resources

For each important asset determine:

* filename
* purpose
* where used
* Android equivalent
* whether it can be directly reused
* whether a vector/drawable replacement is needed

Do not invent replacement assets if an original asset can be reused.

---

# PART 11 — RESPONSIVE DESIGN

Determine how the Flutter application behaves across:

* different screen sizes
* portrait/landscape
* small phones
* large phones
* tablets if applicable

Translate that behavior into responsive Compose layouts.

Avoid blindly copying Flutter pixel values if doing so would break Android layouts.

Preserve the visual proportions and behavior.

---

# PART 12 — STATES

For every important screen, identify:

1. Initial state
2. Loading state
3. Success state
4. Empty state
5. Error state
6. Retry state
7. Permission-denied state
8. Offline state
9. Disabled state
10. Selected state

The Gemini prompt must require implementation of these states wherever they exist in the Flutter project.

---

# PART 13 — CODE QUALITY REQUIREMENTS

The generated Android project must:

* compile
* run
* use Kotlin
* use Jetpack Compose
* follow the specified architecture
* have clean package/module organization
* avoid unnecessary duplication
* avoid giant Composable functions
* avoid giant ViewModels
* separate UI from business logic
* use proper state handling
* use lifecycle-aware collection
* avoid memory leaks
* handle configuration changes correctly
* avoid hardcoded secrets
* avoid unnecessary dependencies

Do not generate pseudo-code.

Do not generate TODO implementations for core functionality.

Do not use fake data when real implementation can be derived from the Flutter project.

Do not create placeholder screens for actual screens.

---

# PART 14 — ANDROID-SPECIFIC BEHAVIOR

The final implementation must feel like a proper Android application while maintaining the original application's behavior.

Handle correctly:

* Android permissions
* lifecycle
* configuration changes
* back navigation
* keyboard
* status bar
* navigation bar
* edge-to-edge
* system UI
* activity recreation
* process death where relevant
* Android file handling
* notifications
* background work where required

Use native Android/Compose best practices where Flutter behavior needs an Android-specific implementation.

---

# PART 15 — DO NOT CHANGE THE PRODUCT

This is extremely important.

The first version is a **faithful native recreation**.

Do NOT:

* redesign the UI
* change navigation
* remove features
* merge screens unnecessarily
* replace real functionality with fake functionality
* add AI features
* add analytics
* add unnecessary Firebase
* add unnecessary backend
* add unnecessary animations
* add features that aren't present
* change business rules

If something in the Flutter implementation appears unusual but is intentional, preserve it unless it is technically impossible on Android.

---

# PART 16 — GEMINI IMPLEMENTATION STRATEGY

The final master prompt must tell Android Studio Gemini to actually implement the project.

Gemini should:

1. Inspect the existing Android project.
2. Create/modify required files.
3. Configure Gradle/dependencies.
4. Create package/module structure.
5. Implement domain/data/presentation layers as required.
6. Implement models.
7. Implement repositories.
8. Implement data sources.
9. Implement ViewModels/state holders.
10. Implement navigation.
11. Implement every screen.
12. Implement reusable Compose components.
13. Add assets.
14. Implement permissions.
15. Implement API/database/storage behavior.
16. Implement loading/error/empty states.
17. Implement animations/interactions.
18. Build the project.
19. Fix compilation errors.
20. Fix obvious runtime issues.
21. Verify that implemented behavior matches the specification.

---

# PART 17 — FILE-BY-FILE IMPLEMENTATION PLAN

The final master prompt must contain a concrete implementation plan.

For example:

```text
app/
 ├── src/main/java/...
 │
 ├── core/
 │   ├── common/
 │   ├── designsystem/
 │   ├── network/
 │   └── ...
 │
 ├── data/
 │   ├── repository/
 │   ├── datasource/
 │   └── model/
 │
 ├── domain/
 │   ├── model/
 │   ├── repository/
 │   └── usecase/
 │
 └── feature/
     ├── home/
     ├── details/
     ├── settings/
     └── ...
```

However, do NOT force this exact structure if my provided skill set specifies a different structure.

The final prompt must describe the actual required files and their responsibilities.

---

# PART 18 — SCREEN-BY-SCREEN SPECIFICATION

For every screen, the final master prompt must provide something equivalent to:

```text
SCREEN: HomeScreen

Purpose:
...

Entry:
...

UI hierarchy:
...

Top bar:
...

Content:
...

Components:
...

Spacing:
...

Colors:
...

Typography:
...

Interactions:
...

Navigation:
...

State:
...

Loading:
...

Empty:
...

Error:
...

Data source:
...

ViewModel:
...

Expected behavior:
...
```

Do this for EVERY important screen.

---

# PART 19 — COMPONENT SPECIFICATION

Identify reusable components.

For each component specify:

* name
* purpose
* parameters
* state
* appearance
* interaction
* where used

Example:

```text
AppPrimaryButton
- text
- enabled
- loading
- onClick
- height
- shape
- typography
```

Gemini should implement reusable components instead of duplicating UI code.

---

# PART 20 — VALIDATION CHECKLIST

The final master prompt must end with a comprehensive validation checklist.

Gemini must verify:

### Build

* Gradle sync succeeds
* project compiles
* no unresolved references
* no dependency conflicts
* no Kotlin compilation errors

### UI

* every screen exists
* navigation works
* layouts match specification
* typography is correct
* spacing is correct
* colors are correct
* assets are correct
* dark/light behavior matches original if applicable

### Functionality

* forms work
* validation works
* API calls work
* persistence works
* search/filter works
* navigation works
* loading states work
* errors work
* empty states work
* permissions work
* back navigation works

### Architecture

* UI doesn't contain business logic unnecessarily
* ViewModels don't contain UI rendering logic
* repositories are separated
* data/domain/presentation responsibilities are clear
* state is lifecycle-aware

---

# PART 21 — IMPORTANT: OUTPUT FORMAT

After fully analyzing the Flutter repository and my Jetpack Compose skill set, DO NOT give me a generic explanation.

Your final response must contain **ONE SINGLE MASTER PROMPT** that I can directly copy and paste into Android Studio Gemini.

The master prompt must be written as an instruction to Gemini.

It should start conceptually like:

"You are working inside my Android Studio project. Your task is to recreate the provided Flutter application's functionality and UI as a native Jetpack Compose Android application..."

Then include everything Gemini needs.

The final prompt must be:

* self-contained
* implementation-oriented
* highly specific
* technically actionable
* based on the actual Flutter source code
* based on my actual Jetpack Compose skills
* free from vague instructions
* free from unnecessary theory

---

# CRITICAL RULE

Do NOT simply summarize the Flutter repository.

I need you to convert your analysis into a **Gemini-ready implementation specification**.

Think of the final prompt as a complete technical handoff from:

**Flutter Development Team → Android Jetpack Compose Development Team**

Gemini should be able to use that document as the primary implementation blueprint.

---

# IMPORTANT — NO ASSUMPTIONS

If something is clearly present in the Flutter source code, document it.

If something cannot be determined from the repository, explicitly mark it as:

`UNKNOWN — VERIFY DURING IMPLEMENTATION`

Do NOT invent technical behavior.

Do NOT invent APIs.

Do NOT invent database schemas.

Do NOT invent business rules.

Do NOT invent screens.

Do NOT invent assets.

---

# FINAL OBJECTIVE

Produce the strongest possible **Flutter-to-Jetpack-Compose reconstruction MASTER PROMPT** based on the actual repository and my Android skill set.

The resulting prompt must be good enough that I can give it directly to Android Studio Gemini and have Gemini implement the complete application in Jetpack Compose with minimal ambiguity.

Focus on:

**Accuracy → Completeness → Implementation clarity → UI/UX parity → Functional parity → Clean Android architecture.**

Do not add future features.

Do not redesign.

Do not simplify.

Analyze first. Then produce the final Gemini MASTER PROMPT.
