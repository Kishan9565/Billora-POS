# BilloraPOS technical audit

## Scope and baseline

The existing Android behavior and models are the primary contract, per the user's correction. `PRODUCT_SPECIFICATION.md` supplies preservation and Android quality constraints only. No Flutter parity claim is made. All source, build scripts, resources, manifest, tests, repositories, state holders, DI registrations, Room entities/DAOs, and navigation destinations were inspected.

The specification was already `AM` in Git when supplied (staged addition plus unstaged content); `.idea/markdown.xml` was already untracked. Neither is edited by this audit. Initial specification SHA-256: `7E9FA824F3C8B22C10BA67882CE5E4ECCFD11333CB6D19CA4D8AE4F8A5EC76DF`.

## Established behavior preserved

- Home scans barcodes, looks up products, adds/increments cart lines, and applies a two-second per-barcode cooldown.
- Cart total is the sum of price times quantity. Decreasing a quantity to zero removes that line. No tax, discount, payment confirmation, sale ledger, stock decrement, reports, categories, customers, or backend exists in the baseline; none is invented.
- Checkout shows itemized amounts, a UPI QR when the shop has a UPI ID, and Bluetooth receipt printing. Back clears the cart. Printing does not clear the cart or create a sale record.
- Products have UUID, name, barcode, Double price, and Int stock. New stock is zero. Editing explicitly resets stock to zero, as in the baseline. Barcode editing remains unavailable. Duplicate barcodes are rejected on add; required names/barcodes, nonnegative prices, and zero-price allowance remain.
- Product search matches name or barcode case-insensitively; blank queries show all products.
- Shop details retain the existing default fallback for an absent database row, required name/address/phone, optional other fields, and 60-character footer input limit.
- Printer refresh tries bonded devices in the existing order and saves the first successful connection. No device discovery or new printer-selection workflow is added.
- Room database name, version 1, tables, columns, indices, keys and persisted money representation are unchanged. No destructive migration is introduced.
- Light theme and portrait orientation remain. All eight navigation destinations remain.

## Root causes and repairs

- Broken Windows wrapper invocation: removed the empty classpath argument before `-jar`.
- Missing Kotlin Android plugins made KAPT and Kotlin DSL configuration invalid. Fixed module plugins and dependency accessors. Added explicit coroutine, lifecycle Compose and icon dependencies where used.
- The original wrapper used Gradle 9.1 with AGP 8.8. Pinned Gradle 8.10.2 with its official SHA-256, matching [AGP 8.8's documented compatibility](https://developer.android.com/build/releases/agp-8-8-0-release-notes). Application and libraries target JVM 11 consistently.
- Invalid `kotlinx.flow`, theme import, camera selector and Material 3 field color APIs prevented compilation.
- Barcode uniqueness was checked against the filtered UI list. Moved the established add validation into a Room transaction without changing schema; barcode lookup now uses SQL. Missing updates report failure, and repositories propagate coroutine cancellation.
- Navigation mutated scanner-result state during composition and used one product ViewModel/event channel for multiple destinations. Results are consumed after delivery; forms get destination-scoped ViewModels; transient scanner state and draft fields are saveable.
- UI collection was not lifecycle-aware. State uses lifecycle-aware collection; events are consumed only by resumed screens, and channels are buffered. Save/print operations reject repeated taps.
- Camera analysis clients, executors, and use cases were not released. A shared native camera component owns and disposes its resources, closes frames on every path, gates results by lifecycle and reports startup/analysis failures. Existing scanning modes and cooldown are retained.
- Printer connection and writes blocked Main, lacked runtime permission handling, reused the wrong connected socket and reported saved devices as connected. Repaired IO dispatching, serialized socket access, connection state, permission handling and failure cleanup.
- Receipt creation read mutable cart state after suspend points. Printing now captures one consistent snapshot. Print errors release submitting state.
- Shop edits left stale checkout/settings values. Successful saves update state; resumed screens reload persisted details. Read failures are shown rather than silently printing default details.
- Binary arithmetic produced inconsistent amounts. Decimal multiplication/summation preserves the formula and Double storage model; no tax or rounding policy is added. Non-finite prices and numeric overflow are rejected. UPI parameters are escaped and its amount uses a locale-independent decimal point.
- Forms lost drafts, used integer price keyboards and permitted non-finite numeric input. Restored draft state, decimal keyboards, keyboard insets, validation and loading feedback. Checkout content now scrolls without overlapping fixed-height content. Increased touch targets and added meaningful icon descriptions. Footer label reflects its existing limit.
- Removed unused Bluetooth scan/location permissions; retained only camera, haptic feedback and bonded-device Bluetooth permissions. Added the previously missing vibration permission.

## Feature trace / second-pass checklist

| Screen or feature | End-to-end path | Audit target |
| --- | --- | --- |
| Home / cart | Camera -> BillingViewModel -> ProductRepository -> ProductDao -> cart StateFlow | Scanning, cooldown, quantity, removal, totals, camera/flash controls |
| Checkout | Shared billing state -> UPI URI/QR; ShopRepository + PrinterRepository -> DataStore / Bluetooth | Back clears cart, no sale ledger, print snapshot, errors, permissions |
| Product list | ProductDao Flow -> repository -> filter/state -> lifecycle-aware list | Search, scan result, add/edit routes, delete confirmation, error/retry/empty state |
| Add product | Saveable form -> ProductViewModel -> repository -> transactional DAO insert | Required values, zero price, finite decimal price, duplicate and repeated-tap protection |
| Edit product | Typed navigation arguments -> saveable form -> ViewModel -> DAO update | Read-only barcode, name/price change, established stock reset, missing row failure |
| Scanner | Camera permission -> owned CameraX/ML Kit -> one result -> origin destination | Single result, back, cleanup, restoration |
| Shop details | DAO -> repository -> ViewModel -> draft; save reverses path | Loading/error/retry, required fields, footer limit, persistence, refreshed consumers |
| Settings / printer | ShopRepository + DataStore + live printer connection -> PrinterViewModel -> UI | Management routes, system Bluetooth settings, permission, refresh, connection feedback |

DI is Koin, not Hilt. Application registration, Room singleton, DAO providers, repository bindings and all ViewModel constructors were checked. There are no workers/services/receivers, backend APIs or hidden use-case implementations to complete. Template instrumentation tests are not run.

## Verification

- `:app:assembleDebug :app:testDebugUnitTest`: PASS using Gradle 8.10.2 and JDK 21. All 15 tests passed (14 POS regressions plus the existing template test), with no failures or skipped tests.
- Debug APK located at `C:\Projects\BilloraPOS\app\build\outputs\apk\debug\app-debug.apk`.
- App `assembleDebug`: PASS.
- 15 available JVM tests: PASS (0 failures, 0 errors, 0 skipped).
- Debug APK: GENERATED at the path above.
- Full multi-module verification: NOT COMPLETED due to slow tooling dependency downloads; stopped at the user's request. No pass is claimed.
- `lintDebug`: NOT COMPLETED due to slow tooling dependency downloads; stopped at the user's request. No pass is claimed.
- Physical-device verification: PENDING. No emulator, AVD, or Android system image was created or downloaded.
- Work is closed for now at the user's request; no further source changes or network-dependent checks are planned.
- Gradle 8.10.2 was installed from the user's local `gradle-8.10.2-bin.zip.zip`; SHA-256 matches the checksum in the existing wrapper configuration. No Gradle distribution was downloaded after the user's stop instruction, and the version remains 8.10.2.
- Nine Material icon vectors are included locally with Apache 2.0 attribution and license, avoiding an unnecessary dependency on the entire extended icon collection.
- Second source/compliance pass completed against the existing Android baseline and the supplementary Markdown preservation constraints. No core TODO implementation or fake repository remains. Room-generated code confirms the duplicate-check/insert transaction. Koin registrations and all eight routes were checked. Physical-device behavior is not claimed as verified.

The JVM regression suite exercises financial arithmetic, locale/URI encoding, product uniqueness and cancellation, cart transitions, save/print double taps, receipt snapshots, shop refresh, and preserved stock-reset behavior. DAO test doubles do not prove physical SQLite concurrency or Bluetooth/camera behavior; Room's annotation processor validates SQL and generated transactions during compilation.

## Physical device verification required

- Install and launch on API 24/25 and current Android; first-run and revoked camera/Nearby devices permissions.
- Scan continuously, scan from add/list, toggle camera/torch, navigate away during scanning, background/restore, rotate via developer settings and recreate the activity.
- Add/edit/search/delete, persistence after relaunch, duplicate barcode under an active filter, zero/decimal price and error retry.
- Small screens, large font scale, keyboard visibility, system bars, TalkBack and touch targets.
- UPI QR decoding in a payment app, including names with spaces and reserved characters. QR display is not payment verification.
- Real paired thermal printer: connect/refresh, saved printer after relaunch, switching connections, paper-out/disconnection, long item names and printer-specific character encoding. Test physical output and hardware-dependent timeout behavior.
- Confirm checkout back clears cart and editing resets stock, as preserved from the baseline.

No emulator, AVD, system image, connected test, Python tooling, Git commit, or release signing is part of this audit.
