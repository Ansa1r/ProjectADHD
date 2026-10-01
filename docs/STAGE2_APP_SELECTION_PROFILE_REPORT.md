# Stage 2 — App Selection & Profile Fix

Дата подготовки: 2026-10-01 UTC. Версия **0.3.1-pre-alpha**, code **5**. Package/namespace **com.ansa1r.projectadhd** сохранён.

## База и доступ

Получен актуальный GitHub commit **30b4c5bc73a9461f907eddd5eeed4968acceb573**, сообщение «v0.3 pre-alfa», время 2026-10-01 11:36:08 UTC, tree **41972f1744bb904bd99f657d3679d2e3190143bf**. Все 165 файлов сверены с Git blob SHA; прежние локальные файлы использованы только при точном совпадении. Восемь файлов нового архива изображений побайтово совпадают с ранее открытыми reference 01–08; полный прозрачный startup PNG повторно просмотрен. Работа выполнена в отдельной локальной копии. Commit, push, PR и изменения remote не выполнялись.

## Навигационный баг и исправление

Пользователь сообщил о невозможности вернуться Home после входа в Apps. На устройстве в этой среде баг не воспроизводился: Android runtime отсутствует. В исходниках подтверждены немедленная запись checkbox в production repository, редактирование лимита в строке, отдельный путь Home-card navigation и восстановление сохранённых tab back stacks. Поэтому фикс проверяется по новому безусловному контракту перехода, а точная причина конкретного runtime-сбоя не выдаётся за доказанную.

Все основные переходы используют один `openMain`: popUpTo стартового Home, launchSingleTop, без восстановления прежнего Apps stack. В нём нет проверки selection, лимита, saving или числа приложений. Home доступен из selection и limits при любом draft. Нижняя панель сохраняет Home/Habits/Apps/Stats/Profile, только icons с contentDescription и selected indicator; она присутствует и на limits/подстраницах Profile. Settings/Debug не возвращены в нижнюю панель.

Apps реализован вложенным graph `apps_setup` с `apps` и `apps/limits`. Один graph-scoped AppsViewModel держит draft между шагами и при конфигурации. Выход через нижнюю вкладку удаляет graph/черновик; последующий вход загружает persistence. Обычный Back из limits возвращает selection, сохраняя draft. Состояние остальных вкладок при их смене создаётся заново из репозиториев; form draft не переносится между отдельными посещениями.

## Двухэтапный Apps flow

Selection показывает только настоящий PackageManager icon, display name и checkbox. Package используется как внутренний ключ/test tag, но не выводится пользователю и не участвует в текстовом поиске. Список исключает собственный и системные пакеты. Сохранённое недоступное приложение можно снять; fallback icon остаётся.

`AppSelectionDraft` — immutable domain state с persisted baseline и отдельной map выбранных приложений/ввода лимита. Checkbox, индивидуальный ввод и Apply all не вызывают DAO. При изменении непустого выбора появляется «Продолжить»; при снятии всего — «Сохранить». При неизменном непустом выборе доступно «Настроить лимиты», чтобы редактирование лимита не требовало искусственного изменения checkbox.

Session Limit Setup показывает только выбранные приложения. Существующий лимит подставляется из TrackedApp; новый получает прежний default 15 минут. Допустимы целые 1–180. Общий лимит применяется ко всем только по отдельной кнопке, после чего любое приложение можно изменить индивидуально. Save закреплена внизу; невалидный ввод её отключает, но навигация остаётся доступной.

Свежий, ещё не редактируемый draft принимает изменения persistence, в том числе завершение ранее нажатого Save. Активно редактируемый draft сохраняет ввод и обновляет только persisted baseline. При возврате в новый flow данные читаются заново. Несохранённый draft не записывается на диск при уничтожении процесса.

## Атомарное сохранение и удаление tracked apps

`AppContainer.saveTrackedSelection` выполняется под существующим `MonitoringController.gate`, общим с polling и STOP. `TrackedAppRepository.replaceSelection` внутри `Room.withTransaction` удаляет прежний набор, upsert-ит весь выбранный список с индивидуальными лимитами и вызывает прежний BlockRepository.reconcile. Это одна транзакция: удаление package, отмена его active BlockSession и событие TRACKING_DISABLED фиксируются вместе. Ошибка вставки откатывает удаление и историю. Дубликаты, пустой package, disabled/self-selection не принимаются.

После успешного commit controller убирает окно удалённого пакета, отменяет armed test при пустом наборе и актуализирует диагностический limit/decision monitoring. Следующий poll читает новый repository snapshot; перезапуск Activity/FGS/мониторинга не нужен. Существующий действующий challenge выбранного приложения сохраняет прежние правила: смена лимита сама по себе не разблокирует его.

Save запускается в applicationScope, поэтому явный запрос завершается даже при быстром уходе со страницы. Навигационный callback выполняется только если пользователь ещё находится на том же destination; поздняя запись не уводит с другой вкладки. До нажатия Save изменений production данных нет. При ошибке draft остаётся доступен для повторной попытки.

## Home CTA

HomeViewModel наблюдает реальный TrackedApp repository вместе с привычками/monitoring. «Выбрать приложения» показывается после загрузки, если число **enabled** tracked apps равно нулю; при положительном количестве скрывается. Старые disabled-строки не считаются отслеживанием и удаляются при следующем полном Save. Apps tab всегда доступен. Удаление всего selection возвращает CTA без restart.

## Startup и определение foreground

Сохранены градиент, happy→flip→ProjectADHD→fade и номинальные **1840 мс** (420+300+300+420+400). `StartupHost` использует полный существующий RGBA PNG 256 dp с ContentScale.Fit, без CircleShape, clip или круглого контейнера. Asset не перерисовывался.

В Compose-версии исходника уже не было CircleShape; возможная отдельная круглая маска находилась на системном splash-пути. Чтобы исключить её визуально, обе Starting theme используют прозрачный vector `ic_splash_empty`. Система рисует зелёный фон, затем exit fade 80 мс раскрывает единственную Compose-анимацию с полным PNG. Launcher adaptive mask от этого не меняется. Десять старых density launcher WEBP, вновь присутствовавшие в базе, удалены, чтобы они не перекрывали правильный launcher.

Вместо process-wide `startupShown` добавлен `ForegroundEntryTracker`, которым управляют только **MainActivity.onStart/onStop**. Для текущей single-Activity архитектуры это сигнал видимости UI, отдельный от FGS. Реальный onStop без configuration change помечает UI скрытым; следующий onStart выдаёт новый entry id. Внутренние Compose-маршруты lifecycle Activity не меняют. isChangingConfigurations сохраняет состояние, поэтому поворот не запускает новый цикл. Холодный процесс начинает с нового tracker, даже при восстановлении navigation state.

StartupHost сбрасывает только свои Animatable/state по entry id, не пересоздаёт NavHost. При простом background/foreground живой draft и текущая вкладка сохраняются. Выход во время заставки отменяет текущую анимацию. При возвращении по OPEN_HABITS из фона она тоже показывается согласно новому правилу «при каждом открытии UI», затем остаются привычки, а BlockSession сохраняется. Потеря фокуса без скрытия Activity (например, split-screen или полупрозрачный диалог) отдельным входом не считается.

Основания: [Activity lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle), [isChangingConfigurations](https://developer.android.com/reference/android/app/Activity#isChangingConfigurations()), [SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen). Фактическое время холодного старта ОС и визуальная плавность требуют устройства.

## Локальный Profile

ProfileScreen использует background_main, исходного IDLE-маскота, purple cards, тёмные outlines и существующую theme. Это реальный локальный экран: никнейм, сегодня выполнено X/Y активных Habit, N enabled TrackedApp, число зарегистрированных вмешательств за локальную дату. Progress и Achievements открывают отдельные честные placeholders. Аккаунтов, email, backend, cloud, XP/streak и придуманных достижений нет.

Никнейм хранится как **String `profile_nickname`** в прежнем Preferences DataStore **settings**. До сохранения Profile показывает локализованное «Пользователь»; редактор позволяет ввести имя и сохранить. Валидатор trim-ит внешние пробелы, принимает 1–32 Unicode code points и запрещает внутренние control characters. AppPreferences Flow сразу обновляет Profile. Явная запись заканчивается и после ухода со страницы, без перезапуска; несохранённый ввод при выходе не применяется.

Daily intervention count использует новый SQL COUNT по **всей истории**, а не observeRecent с LIMIT 200. Границы — [локальная полночь, следующая полночь), обновляются через Flow, учитывают DST и timezone. Учитываются BLOCK_TRIGGERED, PRAISE_SHOWN, LEGACY_NOTIFICATION и fallback PRAISE. BLOCK_RELEASED и fallback BLOCK не удваивают один block challenge. Счётчик отражает зарегистрированные события/попытки BLOCK, а не недоступное приложению доказательство видимости окна на каждом OEM. Очистка истории меняет показатель.

## Room и сохранность Stage 2

**Room schema остаётся версии 2. Новых entities/колонок/миграции нет.** Схемы 1.json/2.json и MIGRATION_1_2 сохранены. Изменены только методы двух DAO и соответствующих repository; никнейм хранится в DataStore.

28 файлов ядра/Settings/схем побайтово совпадают с базой: InterventionEngine/BlockCoordinator/SessionAllowance, SessionTracker, UsageStatsReader, DailyUsageCalculator, UsageMonitoringService, OverlayController, InterventionContent, BlockingOpacity, BlockRepository, HabitRepository, AppDatabase/Migrations, PermissionManager, MonitoringController, NotificationHelper, app-icon компоненты, весь Settings package и обе Room schema. Список с SHA-256 — `validation/app_selection_preservation.txt`.

Fullscreen прозрачный BLOCK, alpha только scrim, живой opacity setting, перехват касаний, системная навигация, baseline unlock/новый интервал, Praise/cooldown, fallback и Debug не переписаны. Это подтверждение diff; Android-регрессия в среде не выполнялась.

## Фактическая проверка

| Проверка | Результат |
| --- | --- |
| Получение базы, сверка 165 Git blobs, оригиналы 01–08 | Выполнено |
| XML/R references, catalog/sourceSets/package, wrapper, Manifest | PASS — verify_source.py |
| Настоящий SQL MIGRATION_1_2, 23 DAO queries, PK/FK, uniqueness/order | PASS — SQLite verify_source.py |
| Новый COUNT: больше 200 записей, границы суток, без двойного BLOCK fallback/release | PASS — фактический DAO SQL на SQLite |
| Пять runtime PNG и reference 08 | PASS — SHA-256, assets сохранены |
| 28 файлов core/Settings/schema | PASS — побайтовая сверка |
| `./gradlew testDebugUnitTest` | Exit 1 на загрузке Gradle 9.6.0, `Network is unreachable`; **тесты не выполнялись** |
| `./gradlew assembleDebug` | Exit 1 на той же загрузке; **компиляция не выполнялась, APK отсутствует** |
| Instrumentation/UI, lint, ручная приёмка | **Не запускались**: нет Android SDK/эмулятора |

Есть только Java runtime, нет javac/Gradle/Kotlin compiler/adb. `gradlew` сохранён исполняемым в Linux. Логи — `validation/app_selection_testDebugUnitTest.txt`, `app_selection_assembleDebug.txt`, `app_selection_source_checks.txt`. Старые ui_revision логи оставлены как исторические и не считаются текущим результатом. Проверка скобок 92 Kotlin-файлов прошла, но не заменяет компиляцию/проверку API.

Объявлено **92 unit-теста**: 72 прежних + 20 новых (draft/лимиты/CTA, nickname, foreground entries/configuration, границы локального дня/DST). Объявлено **24 Android-теста**: 13 прежних + 11 новых (4 repository/transaction, 2 DataStore/count, 5 selection/navigation/profile). Прежний NavigationRevisionTest обновлён под реальный профиль; остальные существующие tests сохранены. Новые тесты проверяют rollback при abort-trigger, удаление active block, пустой selection, изоляцию draft, индивидуальные лимиты после Apply all, возврат Home и nickname после закрытия/открытия DataStore. **Наличие тестов не означает их успешный запуск.**

## Ограничения и локальная приёмка

Выполните [STAGE2_APP_SELECTION_PROFILE_TEST.md](STAGE2_APP_SELECTION_PROFILE_TEST.md): A first selection, B Home navigation, C unsaved draft, D remove all/active block, E startup с живым FGS, F nickname/реальная статистика; затем регрессию Stage 2. В частности нужны device-проверки backdrop/масок, IME/малых экранов, конфигурации, touch-blocking, обновления существующей установки и compile/API совместимости.

Несохранённый draft живёт в ViewModel: переживает конфигурацию и простой уход UI в фон, но не обещан после process death или ухода с Apps graph. Мониторинг после process restart по-прежнему включается вручную. Android/OEM restrictions на UsageEvents, background launches, overlay над защищёнными окнами, API 24–25 notification fallback и multi-window не изменены. В intervention count не восстанавливаются удалённые из history события.

## Поставка

Полный **ProjectADHD_Stage2_AppSelection_Profile_Fix.zip**, с app/, docs/, gradle/ и Gradle-файлами сразу в корне. Нет .idea/, .gradle/, build/, local.properties, ключей/keystore и лишней обёртки папкой. APK не приложен из-за невозможности выполнения assembleDebug. README, актуальный ручной план, этот отчёт, исходники и полные фактические логи включены.

## Полный список файлов относительно GitHub-базы

Пути относительно корня проекта. Отдельно `gradlew` получил executable bit 0755; его содержимое не менялось.

### Добавлены (21)

- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/AppSelectionNavigationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/AppSelectionRepositoryTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/ProfilePersistenceTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/apps/AppSelectionDraft.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/profile/Nickname.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/startup/ForegroundEntryTracker.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/SessionLimitScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/EditProfileScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/EditProfileViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/ProfileViewModel.kt`
- `app/src/main/res/drawable/ic_splash_empty.xml`
- `app/src/test/kotlin/com/ansa1r/projectadhd/AppSelectionDraftTest.kt`
- `app/src/test/kotlin/com/ansa1r/projectadhd/ForegroundEntryTrackerTest.kt`
- `app/src/test/kotlin/com/ansa1r/projectadhd/NicknameTest.kt`
- `app/src/test/kotlin/com/ansa1r/projectadhd/ProfileDayBoundsTest.kt`
- `docs/STAGE2_APP_SELECTION_PROFILE_REPORT.md`
- `docs/STAGE2_APP_SELECTION_PROFILE_TEST.md`
- `docs/validation/app_selection_assembleDebug.txt`
- `docs/validation/app_selection_preservation.txt`
- `docs/validation/app_selection_source_checks.txt`
- `docs/validation/app_selection_testDebugUnitTest.txt`

### Изменены (29)

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/NavigationRevisionTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/AppContainer.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/MainActivity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/dao/InterventionDao.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/dao/TrackedAppDao.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/preferences/AppPreferences.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/InterventionRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/TrackedAppRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/MonitoringState.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/AppNavigation.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/Screen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/ProfileScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/startup/StartupHost.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/util/TimeUtils.kt`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `docs/ASSETS.md`
- `docs/STAGE2_MANUAL_TEST.md`
- `docs/STAGE2_UI_REVISION_REPORT.md`
- `docs/STAGE2_UI_REVISION_TEST.md`
- `docs/TECH_SPEC_STAGE2.md`
- `tools/verify_source.py`

### Удалены старые launcher assets (10)

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
