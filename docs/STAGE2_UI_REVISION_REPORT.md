# Stage 2 UI/UX Revision — отчёт

Дата: 2026-10-01 UTC. Версия: **0.2.2-pre-alpha**, code **4**. Package/namespace остаётся **com.ansa1r.projectadhd**.

## Исходная база и границы работы

Получен актуальный GitHub `Ansa1r/ProjectADHD`: commit `d18fa8bdf499199ff8902b025cbea7b11d29ba06` (2026-10-01 09:25:18 UTC), tree `eb6cf15afc576ecd76c76b664249aba7857025de`, 137 файлов. Каждый файл исходной копии проверен по Git blob SHA. Все 8 приложенных изображений открыты до правок. Работа выполнена только в локальной копии: commit/push/PR и другие изменения удалённого репозитория не выполнялись.

Это исправление UI существующего Stage 2. В исходной архитектуре сохранены UsageStats, SessionTracker, один FGS, Habit/Completion/TrackedApp/BlockSession, InterventionEngine, baseline-разблокировка, новый интервал после выполнения, Praise, notification fallback, cooldown, статистика, Room и Debug. Новых аккаунтов, серверов, аналитики, AI, AccessibilityService, root или kiosk нет.

## Что изменено

- Launcher: happy-маскот только из reference 08, зелёно-голубой XML-градиент, adaptive/round/legacy/themed варианты. Убраны 10 старых launcher WEBP; foreground отступы рассчитаны по всей alpha-маске. Исходник 08 и происхождение asset включены в ASSETS.md.
- Bottom navigation: Home/Habits/Apps/Stats/Profile, ровно пять иконок без видимых подписей. Сохранены contentDescription, выбранное состояние, подсветка и восстановление состояния вкладок.
- Settings открывается шестерёнкой справа сверху Home; Debug перенесён в Settings → Для разработчика → Отладка. Developer и Debug destination защищены BuildConfig.DEBUG.
- Settings стало меню с отдельными Permissions, Blocking Screen, Theme, Privacy и Developer. Все разделы открываются. Profile, Theme и Privacy — локальные заглушки. Сам Settings и подразделы используют спокойный тёмный фон; Debug сохраняет основной фон.
- Permission-кнопки вынесены из Home; на Главной остаётся только небольшое предупреждение/ссылка. Статусы обновляются после возврата из Android Settings. Сохранены notification channels и аварийный STOP.
- Startup: быстрый happy→название flip и fade в Home на градиенте 08.
- BLOCK: fullscreen отдельный полупрозрачный фиолетовый scrim поверх реального приложения, непрозрачные исходный BLOCKING mascot/текст/кнопка. Удалены фон 07 и внешняя карточка/рамка телефона. Касания перехватываются; системная навигация доступна.
- Настройка scrim 30–90%, шаг 5%, default 65%, мгновенный preview и DataStore/Flow без перезапуска. Прежние паузы Praise/fallback сохранены на экране Blocking Screen.
- Фон 07 сохранён на Home/Habits/Apps/Stats/Profile/Debug/Praise; реальные иконки PackageManager и фирменные purple-карточки сохранены.

## Startup

`MainActivity` вызывает `installSplashScreen()` до `super.onCreate()`, использует `Theme.ProjectADHD.Starting` и один Compose `StartupHost`, без второй Activity. Добавлена стабильная зависимость `androidx.core:core-splashscreen:1.2.0`; версии прежних инструментов/библиотек не менялись.

| Фаза | Номинальное время |
| --- | --- |
| Happy-маскот по центру | 420 мс |
| Поворот маскота 0°→90° | 300 мс |
| Смена на ProjectADHD у ребра, поворот -90°→0° | 300 мс |
| Пауза с названием | 420 мс |
| Затухание заставки и появление Home | 400 мс |
| Всего Compose-последовательность | **1840 мс** |

Градиент общий с launcher (`startup_top/green/blue`), фон 07 не применяется. Native splash имеет того же happy-маскота и однотонный зелёный фон; exit fade 120 мс раскрывает Compose без дополнительного keep-on-screen hold. Фактическое время до первого кадра зависит от загрузки процесса, ОС и скорости устройства; 1840 мс не является гарантией полного холодного старта. Плавность, совпадение размеров и отсутствие визуального двойного splash требуют устройства.

Заставка показывается на первом обычном ACTION_MAIN в процессе при отсутствии savedInstanceState. `startupShown` предотвращает повтор в живом процессе, пересоздание Activity её пропускает. OPEN_HABITS на холодном/тёплом запуске проходит сразу к привычкам и также прерывает текущую анимацию. Во время startup нижележащая навигация скрыта от accessibility и перекрыта от касаний; прозрачные системные панели меняют стиль иконок при завершении.

## Навигация и Settings

`Screen` содержит только пять основных маршрутов. SettingsRoutes отделяет корень Settings и его подразделы; нижняя панель на служебных экранах скрыта, есть Back. `SettingsRoutes.DEBUG` — отдельный маршрут `debug`, поэтому получает основной фон. Profile не является переименованными Settings.

PermissionsScreen использует прежний PermissionManager: Usage Access, Overlay, runtime notifications API 33+ и системные notification channels. `RefreshOnResume` обновляет статусы. Theme/Privacy/Profile имеют реальные Composable destination без сервисов и авторизации. В Blocking Screen находятся slider/preview и прежние интервалы уведомлений/Praise. Аварийный STOP остаётся в Settings.

## Live opacity

- Preferences DataStore остаётся прежним, имя **settings**; новый Int key — **blocking_overlay_opacity_percent**. Старые ключи не изменены.
- `BlockingOpacity` задаёт min=30, max=90, step=5, default=65. Сохранённые ошибочные числа сначала ограничиваются, затем округляются к ближайшим 5%. Float NaN/Infinity дают default.
- `BlockingSettingsViewModel` сразу меняет локальное значение и preview, затем сохраняет его через `AppPreferences`. Запись идёт в applicationScope и завершается при уходе со страницы. Версия запроса защищает от устаревших результатов при быстром перемещении slider; при ошибке видны сообщение и восстановленное сохранённое значение.
- `AppContainer.applicationScope` на Main постоянно собирает `blockingOverlayOpacity.distinctUntilChanged()` и обновляет **существующий** `OverlayController`, независимо от экрана/мониторинга.
- Setter меняет Compose-observable поле; уже показанный production/debug BLOCK перекомпоновывается без удаления/создания окна, нового timer или новой BlockSession. Сервис также берёт актуальный процент из прочитанного settings перед обработкой следующего показа.
- `BlockingContent` рисует отдельный `BlockingScrim`. Только цвет scrim получает alpha=percent/100; WindowManager LayoutParams.alpha=1, PixelFormat.TRANSLUCENT, MATCH_PARENT по обеим осям. Маскот/текст/кнопка не получают общую alpha. FLAG_NOT_FOCUSABLE оставляет системную навигацию; touch-consuming root и обычное touchable окно перехватывают касания.
- Preview рисует условный underlying content и тот же scrim, не создаёт окно, challenge, history или side effect движка. Открытие ProjectADHD по-прежнему скрывает production/test overlay; наблюдение Flow продолжает работать при скрытой Activity.

## Room и сохранность ядра

**Room schema не менялась: версия 2, новая миграция отсутствует.** MIGRATION_1_2 сохранена для старых установок Stage 1. Entities/DAO/repositories, схемы 1/2, domain/intervention, SessionTracker, UsageStatsReader, DailyUsageCalculator, PermissionManager, MonitoringController, ExcludedApps, NotificationHelper и компоненты чтения/показа app icons не изменялись. Побайтовая сверка 26 файлов с текущей базой записана в `validation/ui_revision_core_preservation.txt`.

В UsageMonitoringService добавлена только передача settings opacity в существующий controller. OverlayController изменён только для наблюдаемого процента, его диагностики, параметра BlockingContent и явного Window alpha=1. Правила show/hide, debug timeout, приоритет real BLOCK, CTA, session/cooldown/fallback не переписаны. Это подтверждение состава diff, а не заявление о пройденной Android-регрессии.

## Тесты и фактическая проверка

| Проверка | Фактический результат |
| --- | --- |
| XML, R references, catalog aliases, package/sourceSets, полный wrapper | PASS — verify_source.py |
| Manifest permissions, один сервис/Activity, backup disabled | PASS — verify_source.py |
| Реальный SQL MIGRATION_1_2, сохранение четырёх старых таблиц, schema v2, 21 DAO query, PK/FK/order | PASS — SQLite-проверка verify_source.py |
| SHA-256 пяти runtime assets и оригинала 08 | PASS — verify_source.py |
| 26 файлов ядра/Room/app-icons совпадают с GitHub-базой | PASS — побайтовая сверка |
| Alpha PNG и геометрия safe zone | PASS — статический расчёт, не launcher screenshot |
| `./gradlew testDebugUnitTest` | **НЕ ВЫПОЛНЕНА задача**: wrapper exit 1, Network is unreachable при загрузке Gradle 9.6.0 |
| `./gradlew assembleDebug` | **НЕ ВЫПОЛНЕНА задача**: wrapper exit 1, та же сетевая ошибка; APK отсутствует |
| Android/instrumented UI tests, lint, ручная приёмка | **НЕ ЗАПУСКАЛИСЬ**: нет SDK/устройства; Kotlin-компиляция не подтверждена |

Linux-скрипту `gradlew` возвращён executable bit. Изначальная попытка без него завершилась Permission denied; после исправления обе обязательные команды запущены отдельно и дошли до скачивания дистрибутива. Фактические stdout/stderr лежат в `validation/ui_revision_testDebugUnitTest.txt` и `validation/ui_revision_assembleDebug.txt`; source checks — `validation/ui_revision_source_checks.txt`. Исторические логи Stage 2 в той же папке не выдаются за проверки этой ревизии.

По активным исходникам объявлено **72 JUnit unit-теста**: прежние 65 и 7 новых (default 0.65, все допустимые значения, clamp включая Int extremes, округление, Float-ввод, alpha bounds, идемпотентность). Объявлено **13 Android-тестов**: прежние 7 и 6 новых — 3 DataStore/controller, 1 пиксельный Compose scrim/opaque button, 2 навигационных. MainActivitySmokeTest обновлён для icon-only навигации и ожидания startup. Остальные прежние тесты сохранены.

Persistence test закрывает DataStore и открывает его снова, проверяет default, все 13 значений, ошибочные сохранённые значения и сохранность lastPraise. Live test ждёт новое состояние того же controller без рестарта. Compose test проверяет фактический цвет scrim поверх белого контента при 30/65/90 и неизменность непрозрачной кнопки. Эти тесты **добавлены, но не пройдены в данной среде**. Live controller test не создаёт настоящее WindowManager окно; такой сценарий отдельно указан в ручном плане.

## Ограничения и приёмка

Сборка и Android-поведение остаются неподтверждёнными до запуска Gradle и устройства. Выполните [STAGE2_UI_REVISION_TEST.md](STAGE2_UI_REVISION_TEST.md) (A–F) и [STAGE2_MANUAL_TEST.md](STAGE2_MANUAL_TEST.md) (ядро): launcher masks, TalkBack, пять вкладок, permissions/Settings/Debug, cold/warm/CTA startup, прозрачность 30/40/50/60/65/70/80/90, касания и system navigation, live update и restart restoration, настоящий BLOCK→новое выполнение→новый интервал, PRAISE и fallback.

Android/OEM может задерживать UsageEvents, останавливать FGS, скрывать чужие overlay над защищёнными Activity или ограничивать запуск Activity. Системные панели остаются вне блокировки. На API 24–25 сохраняется notification fallback. Multi-window/PiP/дополнительные дисплеи требуют отдельной проверки. После process restart мониторинг включается вручную, как прежде. Прозрачность 30% требует визуальной оценки контраста на ярком underlying content; текст имеет тень, кнопка и mascot непрозрачны.

## Поставка

Полный `ProjectADHD_Stage2_UI_Revision.zip`: app/, docs/, gradle/ и Gradle-файлы непосредственно в корне, без лишней папки проекта. Не включены .idea/, .gradle/, build/, local.properties, ключи и keystore. Исходный package и лицензия сохранены. APK не приложен, поскольку assembleDebug не выполнилась.

## Полный список файлов относительно GitHub-базы

Пути ниже относительно корня проекта. Дополнительно изменён режим `gradlew` на 0755; содержимое wrapper не менялось.

### Добавлены (28)

- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/BlockingOpacityContentTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/BlockingOpacityPersistenceTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/NavigationRevisionTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/settings/BlockingOpacity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/ProfileScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/BlockingSettingsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/BlockingSettingsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/PermissionsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/PermissionsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/startup/StartupHost.kt`
- `app/src/main/res/drawable-nodpi/mascot_startup.png`
- `app/src/main/res/drawable/ic_back.xml`
- `app/src/main/res/drawable/ic_chevron_right.xml`
- `app/src/main/res/drawable/ic_developer.xml`
- `app/src/main/res/drawable/ic_lock.xml`
- `app/src/main/res/drawable/ic_nav_profile.xml`
- `app/src/main/res/drawable/ic_permission.xml`
- `app/src/main/res/drawable/ic_splash_mascot.xml`
- `app/src/main/res/drawable/ic_theme.xml`
- `app/src/test/kotlin/com/ansa1r/projectadhd/BlockingOpacityTest.kt`
- `docs/STAGE2_UI_REVISION_REPORT.md`
- `docs/STAGE2_UI_REVISION_TEST.md`
- `docs/assets/08_app_icon_reference.png`
- `docs/validation/ui_revision_assembleDebug.txt`
- `docs/validation/ui_revision_core_preservation.txt`
- `docs/validation/ui_revision_icon_geometry.txt`
- `docs/validation/ui_revision_source_checks.txt`
- `docs/validation/ui_revision_testDebugUnitTest.txt`

### Изменены (31)

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/MainActivitySmokeTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/ansa1r/projectadhd/AppContainer.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/MainActivity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/preferences/AppPreferences.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/model/Models.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/UsageMonitoringService.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/AppNavigation.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/Screen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/overlay/OverlayController.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/debug/DebugScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/InterventionContent.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/theme/Color.kt`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `docs/ASSETS.md`
- `docs/STAGE2_MANUAL_TEST.md`
- `docs/STAGE2_REPORT.md`
- `docs/TECH_SPEC_STAGE2.md`
- `gradle/libs.versions.toml`
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
