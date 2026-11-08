---
name: offline-first-android
description: Use when touching Dhikr persistence — Room entities, DAOs, the database class, transactions, migrations and schema export, Proto DataStore preferences, repositories, the AppContainer wiring, or keeping state consistent between the app, widgets and the notification. Also use when choosing where a piece of state should live.
---

# Offline-first persistence for Dhikr

Everything is local. There is no network layer in V1. The database is the single source of truth for counts; UI, widgets and the notification all *observe* it.

## Where state lives

| State | Store | Why |
|---|---|---|
| Library, progress, sessions, daily summaries | **Room** | Relational, queryable, transactional, migratable |
| Preferences (audio, haptics, theme, last active Dhikr, open-on-launch) | **Proto DataStore** (`preferences.proto`) | Typed, atomic, Flow-based |
| Per-widget selected Dhikr | Glance `PreferencesGlanceStateDefinition` (per `GlanceId`) | Scoped to the widget instance (see android-widget-engineering) |
| Transient UI (pressed, sheet open) | Compose `remember` / ViewModel | Not persisted |

Never put lists or history in DataStore. Never use `SharedPreferences` in new code.

## Room conventions

- Room version from the version catalog. Use **KSP** (`androidx.room` Gradle plugin + `ksp`), never kapt. Use the Room Gradle plugin's `room { schemaDirectory("$projectDir/schemas") }` and **commit** `app/schemas/` (it's needed for migration tests).
- Entities live in `data/entity/`, DAOs in `data/dao/`, `DhikrDatabase` in `data/database/`.
- Foreign keys: `DailyDhikrProgressEntity.dhikrId → DhikrEntity.id` with `onDelete = RESTRICT`. History must never cascade away, which is why Dhikrs are soft-deleted (`deletedAt`). Index every FK column and `(dhikrId, localDate)` as unique.
- `localDate` is stored as an ISO `String` (`yyyy-MM-dd`) via a `TypeConverter`. It sorts correctly and is readable in Database Inspector. Timestamps (`createdAt`, `updatedAt`) are `Long` epoch millis UTC.
- DAOs: reads return `Flow<…>`, writes are `suspend`. Never use `allowMainThreadQueries()`.
- **Counting is a transaction:** one `@Transaction suspend fun increment(dhikrId, date, nowMillis)` in a DAO or repository does upsert progress row + `completedCount + 1` + recompute that date's `DailySummaryEntity`. Use SQL `UPDATE … SET completedCount = completedCount + 1`, not read-modify-write in Kotlin, so concurrent increments from the widget and app can't lose updates.
- Serialise writers: the repository funnels count mutations through a single `Mutex`, or relies on the atomic SQL above. Choose one and document it in TECHNICAL_DECISIONS.md.
- One `DhikrDatabase` instance per process (lazy singleton in `AppContainer`). Widgets run in the same process, so they get the same instance.

## Migrations

- `exportSchema = true`. Every schema change bumps `version` and ships a `Migration` (or `@AutoMigration` with a spec when it's safe). **No `fallbackToDestructiveMigration()`**: it would wipe users' history.
- Add a `MigrationTestHelper` instrumented test per migration (`androidTest`), with `room-testing` as `androidTestImplementation`.

## DataStore conventions

- Proto DataStore with a `Serializer<UserPreferences>`. Define `defaultValue` explicitly. Handle `CorruptionException` with a `ReplaceFileCorruptionHandler` returning defaults.
- Exactly one `DataStore` instance per file per process (top-level `by dataStore(...)` or held in `AppContainer`). Two instances on the same file throws.
- Expose `Flow<UserPreferences>` plus intent-named suspend setters (`setAudioEnabled(Boolean)`), not a generic `update`.
- Protobuf: use the `com.google.protobuf` Gradle plugin with `protobuf-kotlin-lite`. Verify the current compatible versions with AGP 9 before adding them. If protobuf tooling proves troublesome, the fallback is a typed DataStore with a kotlinx.serialization JSON serializer. Record the choice.

## Dependency wiring (no Hilt yet — D-008)

```kotlin
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    val database by lazy { DhikrDatabase.build(context) }
    val preferences by lazy { UserPreferencesRepository(context.userPreferencesStore) }
    val dhikrRepository by lazy { DhikrRepository(database, clock) }
}
class DhikrApplication : Application() { val container by lazy { AppContainer(this) } }
```

ViewModels get dependencies through a `ViewModelProvider.Factory` (`viewModelFactory { initializer { … } }`). Widgets and receivers reach the container via `(context.applicationContext as DhikrApplication).container`.

## Cross-surface consistency

- After any count mutation, from any surface, call a single `SurfaceRefresher.refreshAll()`. It updates Glance widgets (`updateAll`) and the ongoing notification. The app UI updates by itself via Flow.
- Widget and notification actions call the **same use case** as the app. They never write SQL or keep their own counters.

## Common mistakes

- Kotlin read-modify-write increments, which lose taps under concurrency.
- `fallbackToDestructiveMigration()` "temporarily".
- Creating `Room.databaseBuilder` in an Activity, which gives a second instance.
- Querying the DB on the main thread from a `BroadcastReceiver`. Use `goAsync()` + a coroutine, or delegate to the Glance action callback.
- Forgetting to commit `app/schemas/*.json`.

## Testing

- DAO tests with `Room.inMemoryDatabaseBuilder` (instrumented, or Robolectric if added later): unique `(dhikrId, date)`; `increment` is atomic (launch 200 concurrent increments → 200); FK `RESTRICT` blocks hard-deleting a Dhikr with history.
- Repository tests with a fake `Clock`.
- Migration tests for every version bump.
- DataStore: corruption handler returns defaults; setters persist across a new instance in a temp folder.
