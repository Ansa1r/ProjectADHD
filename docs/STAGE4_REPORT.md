# Stage4 Pre-alpha — отчёт

## Основа и границы результата

Получен актуальный GitHub head `76da494ca5063caf00a2881215ad544b603af7b4` (`v0.31 pre-alfa`, 2026-10-01T17:04:37Z).
Все **186 файлов** snapshot сверены с Git blob SHA, tree `e4df5ed9fa045cb1cfd990c01df76711d95bc6ed`, truncated=false. Это продолжение существующего проекта, не восстановление по памяти. AGENTS.md в дереве отсутствует.
Предоставленный assets ZIP содержит13 доступных PNG, CRC корректен. Исходный текст задания прочитан полностью.
Работа локальная; commit, push, PR и изменения remote не выполнялись.
Версия повышена с0.3.1/code5 до **0.4.0-pre-alpha/code6**; стадия остаётся **Pre-alpha**.

## Реализовано в исходниках

Profile пользователя + local Photo Picker avatar; отдельный Mascot с именем Боб и persistence XP/counter/streak; levels/formula/ceil; журнал уникальных наград; новая daily habit model; отдельный Create Habit; MANUAL timer/confirmation/NO+10; APP_BASED foreground accumulation/auto completion/replay; обе стороны app conflicts; BLOCK confirmation/release; uniform purple surfaces85%; fixed BLOCK scrim85%; удаление opacity controls; Stage4 Debug diagnostics.
Существующие app selection draft/individual limits/icons, Stats, permissions, service STOP, fallback, cooldown, Profile nickname и startup сохранены по назначению.
Анимационные интервалы/маскот/flip/fade сохранены; новый LocalStartupVisible только откладывает habit dialog до окончания cover.
Удалены устаревшие launcher WEBP overrides, а подготовленные Stage3 runtime PNG/launcher XML сохранены.

## Данные

Room **2→3** через `MIGRATION_2_3`; путь1→2→3 зарегистрирован, destructive migration отсутствует.
HabitEntity получил targetDurationMinutes/type/linkedAppPackage/activatedAt. Старые habits → MANUAL/30 минут, существующие completed dates остаются, выдуманных минут/XP нет.
Новые entities: HabitDailyEntity, MascotEntity, XpAwardEntity, HabitDayEntity; DAO HabitProgressDao.
Не теряются habits/completions/tracked_apps/limits/intervention_events/block_sessions и DataStore nickname/cooldowns.
Avatar хранится приватным файлом + имя файла в DataStore; mascot name имеет отдельный key.
Completion + XP + streak + release атомарны; XP journal PK препятствует повторам. История XP не каскадируется при удалении habit.
Формулы уровней/multiplier, политика пустых дней, rollover, reboot, usage windows и conflict recovery подробно зафиксированы в TECH_SPEC_STAGE4.md.
JSON схемы3 не выдуман: он будет экспортирован Room KSP при успешной компиляции. Android migration tests открывают старую БД через реальные migrations, чтобы Room проверил generated metadata3.

## Тесты и фактический статус

В итоговом проекте **112 JUnit unit tests** и **37 Android tests** (это число объявленных тестов, не число успешных запусков).
Новые pure tests: MascotProgressionTest, HabitProgressTest, HabitUsageTest. Новые Android suites: Stage4RepositoryTest, Stage4MigrationTest, AvatarPersistenceTest, Stage4NavigationTest.
Прежние UI/database/profile/scrim tests адаптированы к новой модели. Тесты удалённой регулируемой opacity удалены; фактический pixel test BLOCK проверяет fixed0.85 и opaque button.

| Проверка | Результат |
|---|---|
| `./gradlew testDebugUnitTest` | exit1, до компиляции: Network is unreachable при скачивании Gradle9.6.0 |
| `./gradlew assembleDebug` | exit1, та же причина; APK отсутствует |
| `connectedDebugAndroidTest` | Не запускался: нет SDK/adb/эмулятора и собранного APK |
| `python3 tools/verify_source.py` | PASS: actual SQL1→2→3, сохранность данных,37 DAO queries, FK/default/index/unique/rollback, ресурсы/manifest/wrapper/asset hashes |
| Kotlin compilation / lint | Не выполнены, синтаксис/типы не подтверждены compiler |
| Runtime/визуальные проверки | Не выполнены, сценарии в STAGE4_MANUAL_TEST.md |

Среда имеет Java runtime, но не javac/Gradle/kotlinc/adb в PATH. Gradle Wrapper не может получить дистрибутив. Логи лежат в docs/validation/stage4_*.log. **BUILD SUCCESSFUL не заявляется.**
Прохождение acceptance tests/build остаётся неподтверждённым: исходники готовы к локальной сборке и тестированию, а не помечены как проверенный Android binary.

## Известные ограничения и выбранная семантика

- Фактическая Android компиляция и UI/device validation обязательны локально. Возможны ошибки, которые Python/SQLite не выявляет.
- Новая habit duration задаётся1…1439 минут; для быстрых tests используйте00:01. Нет production fake XP/reset actions.
- Редактирование существующей привычки меняет название; для нового duration/type/app создаётся новая. Это сохраняет текущие sessions и историю.
- Одна активная MANUAL session. Pause/Resume сохраняют accumulated time; полночь требует нового Start, вчерашние неподтверждённые минуты не переносятся.
- Без живого процесса/FGS timer deadline не будит CPU специальным alarm; progress восстанавливается по monotonic checkpoint. После полного reboot неизвестный промежуток не начисляется.
- App replay зависит от доступных Android UsageEvents и сохранённой конфигурации активных привычек. Потерянные OS events/долгое отсутствие/резкие изменения системного времени не могут восстановить неизвестное время достоверно; отсутствующее время не начисляется умышленно.
- Для фонового app completion/release нужен monitoring FGS. STOP его прекращает; UI может восстановить linked usage позднее. Manufacturer battery restrictions нужно проверить на реальном телефоне.
- App replay пока перечитывает историю периода; оптимизация большого журнала — дальнейшая работа.
- Пустой день нейтрален для серии; архивирование/удаление меняет текущий активный набор. Добавление задачи после дневной награды не отзывает XP, но может прервать серию при незавершённом дне.
- History UI показывает200 последних awards, общий XP и полный ledger не ограничиваются этим отображением.
- Customization — заглушка по заданию; полноценные анимации/одежда/AI/cloud/accounts не реализовывались.

## Что проверить локально

STAGE4_MANUAL_TEST.md содержит A–L: Profile/avatar/Persistence; Bob/rename/customization; screen-off/manual pause/resume/process death/reboot; YES/NO twice; APP_BASED sessions/lock/restart; both conflicts; BLOCK+MANUAL; BLOCK+APP; early PRAISE; XP thresholds/streak/day rollover; UI/TalkBack/small screens/startup/STOP/fallback; migrations/rollback.
Запускать instrumentation suite на отдельном чистом эмуляторе: прежние app-selection fixtures временно меняют selected apps, что может отменить живые blocks.

## Изменённые файлы

Добавлено **43**, изменено по содержимому **52**, удалено **14**. Дополнительно `gradlew` получает executable mode0755.
Основные изменения: AppDatabase/Migrations/entities/DAOs, HabitRepository/MascotRepository/AvatarRepository, HabitRuntime/HabitClock, AppContainer/UsageMonitoringService/OverlayController, AppNavigation/Profile/Habits/Apps/Mascot/Debug, reusable components, README и Stage4 docs.
Полный список ниже; старые отчёты сохранены как история.

### Добавлены

- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/AvatarPersistenceTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage4MigrationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage4NavigationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage4RepositoryTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/dao/HabitProgressDao.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/entity/Stage4Entities.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/AvatarRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/MascotRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/habits/HabitProgress.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/habits/HabitUsageCalculator.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/mascot/MascotProgression.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/HabitClock.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/HabitRuntime.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/CreateHabitScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/CreateHabitViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/PendingHabitConfirmation.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/HabitConfirmation.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/MascotScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/MascotViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/UserAvatar.kt`
- `app/src/main/res/drawable/ic_nav_mascot.xml`
- `app/src/test/kotlin/com/ansa1r/projectadhd/HabitProgressTest.kt`
- `app/src/test/kotlin/com/ansa1r/projectadhd/HabitUsageTest.kt`
- `app/src/test/kotlin/com/ansa1r/projectadhd/MascotProgressionTest.kt`
- `docs/STAGE4_MANUAL_TEST.md`
- `docs/STAGE4_REPORT.md`
- `docs/TECH_SPEC_STAGE4.md`
- `docs/assets/stage4/01_ui_reference.png`
- `docs/assets/stage4/02_blocking_screen_reference.png`
- `docs/assets/stage4/03_mascot_happy_reference.png`
- `docs/assets/stage4/04_mascot_happy_alt_reference.png`
- `docs/assets/stage4/05_mascot_blocking.png`
- `docs/assets/stage4/06_mascot_idle.png`
- `docs/assets/stage4/07_background_main.png`
- `docs/assets/stage4/08_app_icon_reference.png`
- `docs/assets/stage4/09_current_home_reference.png`
- `docs/assets/stage4/10_current_habits_reference.png`
- `docs/assets/stage4/11_current_apps_reference.png`
- `docs/assets/stage4/12_current_stats_reference.png`
- `docs/assets/stage4/13_current_profile_reference.png`
- `docs/validation/stage4_assembleDebug.log`
- `docs/validation/stage4_source_checks.log`
- `docs/validation/stage4_testDebugUnitTest.log`

### Изменены

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/AppSelectionNavigationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/BlockingOpacityContentTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/NavigationRevisionTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/ProfilePersistenceTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage2DatabaseTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/AppContainer.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/MainActivity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/AppDatabase.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/Migrations.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/dao/HabitDao.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/entity/Entities.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/preferences/AppPreferences.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/BlockRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/HabitRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/TrackedAppRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/DailyUsageCalculator.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/SessionTracker.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/model/Models.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/model/UsageSignal.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/UsageMonitoringService.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/UsageStatsReader.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/AppNavigation.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/Screen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/overlay/OverlayController.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/InstalledAppIcon.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/SessionLimitScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/components/BrandComponents.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/components/Common.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/debug/DebugScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/debug/DebugViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/HabitsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/HabitsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/InterventionContent.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/EditProfileScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/EditProfileViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/ProfileScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/ProfileViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/BlockingSettingsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/PermissionsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/startup/StartupHost.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/stats/StatsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/util/TimeUtils.kt`
- `app/src/main/res/values/strings.xml`
- `docs/ASSETS.md`
- `docs/TECH_SPEC_STAGE2.md`
- `tools/verify_source.py`

### Удалены

- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/BlockingOpacityPersistenceTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/settings/BlockingOpacity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/BlockingSettingsViewModel.kt`
- `app/src/main/res/mipmap-hdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-hdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-mdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp`
- `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp`
- `app/src/test/kotlin/com/ansa1r/projectadhd/BlockingOpacityTest.kt`
