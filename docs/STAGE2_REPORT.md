# Отчёт Stage 2

> Исторический отчёт первичной реализации. Актуальные изменения UI Revision, включая launcher, навигацию и прозрачный BLOCK, описаны в [STAGE2_UI_REVISION_REPORT.md](STAGE2_UI_REVISION_REPORT.md).

Дата подготовки: 2026-09-30 UTC. Обновление оформления: 0.2.1-pre-alpha. Основа — актуальный на момент чтения GitHub commit `a4ca37a6e72fc0a7a8ee6c735f911c2cb5c17cf0`, 106 файлов. Все исходные байты сверены с Git blob SHA до правок. Работа выполнена в отдельной копии; push, коммиты и PR в удалённом репозитории не выполнялись.

## Реализовано

- Pure Kotlin решения NONE/BLOCK/PRAISE, summary активных привычек без отдельной Task-системы.
- Устойчивые per-app BlockSession в Room; единственный active challenge на пакет; скрытие при уходе и повторный показ при возвращении.
- Baseline незавершённых ID + completedAt; атомарные completion/release/history; старые completed задачи не дают unlock; новая сессия после release.
- Реальный WindowManager overlay с Compose lifecycle owners, touch-consuming BLOCK, системной навигацией, прямым переходом к привычкам и явным STOP.
- Небольшой Praise на 4 секунды с touch-through opacity, независимым постоянным cooldown и без BlockSession.
- Permission flow, повторная проверка после Settings и fallback notification с типизированной историей/причинами ошибок.
- Единственный унаследованный monitoring FGS, polling раз в секунду, исключение собственных/системных экранов, очистка ресурсов и защита STOP от гонки с poll.
- Полный restyle по семи открытым материалам: насыщенные фиолетовые карточки/кнопки, тёмная обводка, крупные скругления, центральные BrandColors/BrandComponents.
- Фон на Home/Habits/Apps/Stats/Debug и обоих overlay; спокойный однотонный Settings. Home с прогрессом, idle-маскотом, разрешениями и переходами.
- BLOCK по композиции референса, динамические данные и «Посмотреть дела». Отдельный happy drawable с настоящей прозрачностью для PRAISE, подготовленный из референса 03; исходные background/idle/blocking сохранены побайтово.
- Реальные PackageManager icons в списке приложений: IO, bounded bitmap/LRU cache, безопасный fallback, без Drawable в Room.
- Собственная launcher icon из idle-маскота и фирменного фона, adaptive/round/monochrome и legacy API 24–25 XML. Дефолтные template WEBP удалены.
- Version code 3, name 0.2.1-pre-alpha (выше ранее подготовленного Stage 2). Существующие версии Gradle/AGP/Kotlin/SDK/библиотек сохранены; новых SDK/сетевых разрешений не добавлено.

## Схема и миграция

Restyle не меняет схему дополнительно. Относительно Stage 1 Room 1 → 2: `Migrations.MIGRATION_1_2`, новая `block_sessions`, поля `intervention_events.type/detail` с SQL default. Исходная schema 1 не изменена; snapshot 2 добавлен. Destructive migration отсутствует. Из-за недоступной KSP-сборки snapshot 2 подготовлен из entity/SQL; identity hash восстановлен по официальному алгоритму Room с проверкой совпадения на schema 1. Подробности в TECH_SPEC_STAGE2. При первом локальном KSP build export следует сверить.

Запущенный SQLite-проверяющий скрипт исполняет именно SQL из Migration, предварительно создавая исходную БД по schema 1 и наполняя все четыре таблицы. После миграции проверены данные, defaults, PK/FK/index metadata и совпадение со schema 2. Это не заменяет Android Room validation; соответствующий instrumentation test включён.

## Тесты и фактическая валидация

| Проверка | Фактический результат |
| --- | --- |
| `python3 tools/verify_source.py` | PASS: XML/resources, namespace/source roots, dependency aliases, wrapper, permissions, single service/activity, SQL migration и 21 DAO-запрос, ограничения PK/FK и порядок событий, хеши четырёх PNG и RGBA praise asset |
| `./gradlew testDebugUnitTest` | Запущена; exit 1 при скачивании Gradle 9.6.0: `java.net.SocketException: Network is unreachable` |
| `./gradlew assembleDebug` | Запущена; тот же exit 1 на скачивании Gradle; Android/Kotlin compilation не началась |
| `connectedDebugAndroidTest` / manual emulator plan | Не запускались: работающего Android SDK/emulator/build в среде нет |
| Kotlin/JUnit | Тесты написаны, успешный запуск не заявляется |
| APK | Не создан: неподтверждённый бинарный артефакт в ZIP не включён |

Фактические логи попыток находятся в `docs/validation/testDebugUnitTest.txt` и `docs/validation/assembleDebug.txt`; успешных Gradle task outputs нет.

В suite 65 pure unit tests: 21 InterventionEngine, 16 BlockCoordinator/SessionAllowance и 28 сохранённых Stage 1 SessionTracker/DailyUsageCalculator/Time. Покрываются все 11 требуемых сценариев, точные границы, исключённые/выключенные приложения, повторный вход, нулевые задачи, прошлые отметки, дата, cooldown и полный интервал после release.

Сохранение исходных SessionTracker/DailyUsageCalculator/UsageStatsReader, schema 1, dependency catalog, Gradle wrapper JAR и LICENSE отдельно подтверждено побайтовым сравнением с базой. Структурная проверка парных скобок 67 активных Kotlin-файлов выполнена, но не заменяет проверку типов/компиляцию.

7 Android-тестов: 3 сохранённых Stage 1, 2 Stage2DatabaseTest (upgrade с сохранением данных + persistent challenge/atomic release/reopen), 2 InterventionContentTest (BLOCK CTA/маскот и неблокирующий Praise content). Android UI и screenshot QA в этой среде не выполнялись. Зелёная статическая проверка не означает BUILD SUCCESSFUL.

## Осознанные ограничения

- API 24–25: notification fallback. TYPE_APPLICATION_OVERLAY начинается с API 26.
- Нет абсолютного kiosk/system lock. Окно перекрывает app content и перехватывает touch; системная навигация и аппаратный ввод не захватываются.
- Возврат/скрытие зависит от UsageEvents: примерно 1 секунда polling плюс задержки Android; над ProjectADHD окно скрывается прямо в onStart.
- Android может скрыть overlay без исключения addView. Visible в Debug означает зарегистрированное окно, не гарантированную видимость над любой Activity. Выявляемые ошибки и missing permission дают fallback; невыявляемое скрытие ОС нельзя надёжно определить публичным API.
- Повторный ручной старт нужен после смерти процесса/перезагрузки. Сохраняется challenge, а не обещание непрерывного сервиса.
- Multi-window/PiP/другие дисплеи и разные OEM требуют отдельной приёмки; основной сценарий — full-screen приложение на основном дисплее.
- Timestamp cooldown резервируется до внешнего side effect. Между DataStore/notification и Room нет общей системной транзакции: crash может пропустить одно уведомление/историческое событие, но не создаёт spam. Completion/release/его история атомарны в Room.
- Baseline задачи определяются при старте BLOCK. Удаление всех доступных baseline-задач, смена дня и отключение tracking административно отменяют challenge с собственной причиной; они не выдаются за выполнение.
- Debug preview вооружается в ProjectADHD и показывается после открытия выбранного приложения, чтобы не перекрывать собственный интерфейс. Production BLOCK имеет приоритет.
- Требуется подтвердить реальную сборку и ручные acceptance criteria в рабочей среде пользователя. Архив содержит реализацию Stage 2, а не сертификат пройденной device-приёмки.

## Дальнейшая работа

Перед расширением Stage 3 — выполнить приложенный manual plan, получить логи/скриншоты на API 26/31/33+/OEM, проверить расход батареи polling и длительный lifecycle. Затем можно развивать дополнительные состояния маскота/анимацию и onboarding в существующих точках расширения. AI, сервер, аккаунты, XP, подписки, Accessibility и экономика персонажа не добавлялись.

## Файлы относительно базового коммита

### Добавлены (31)

- `app/schemas/com.ansa1r.projectadhd.data.local.AppDatabase/2.json`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/InterventionContentTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage2DatabaseTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/Migrations.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/dao/BlockSessionDao.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/entity/BlockSessionEntity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/BlockRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/intervention/BlockCoordinator.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/model/BlockingModels.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/ExcludedApps.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/overlay/OverlayController.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppIconLoader.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/InstalledAppIcon.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/components/BrandComponents.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/InterventionContent.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/MascotView.kt`
- `app/src/main/res/drawable-nodpi/background_main.png`
- `app/src/main/res/drawable-nodpi/mascot_blocking.png`
- `app/src/main/res/drawable-nodpi/mascot_idle.png`
- `app/src/main/res/drawable-nodpi/mascot_praise.png`
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
- `app/src/test/kotlin/com/ansa1r/projectadhd/BlockCoordinatorTest.kt`
- `docs/ARCHIVE_STAGE1_README.md`
- `docs/ASSETS.md`
- `docs/STAGE2_MANUAL_TEST.md`
- `docs/STAGE2_REPORT.md`
- `docs/TECH_SPEC_STAGE2.md`
- `docs/validation/assembleDebug.txt`
- `docs/validation/source_checks.txt`
- `docs/validation/testDebugUnitTest.txt`

### Изменены (41)

- `README.md`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/ansa1r/projectadhd/AppContainer.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/MainActivity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/AppDatabase.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/dao/HabitDao.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/entity/Entities.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/preferences/AppPreferences.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/HabitRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/InterventionRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/intervention/InterventionEngine.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/model/Models.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/InstalledAppReader.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/MonitoringController.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/MonitoringState.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/PermissionManager.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/UsageMonitoringService.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/AppNavigation.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/notification/NotificationHelper.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/components/Common.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/debug/DebugScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/debug/DebugViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/HabitsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/SettingsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/stats/StatsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/theme/Color.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/theme/Theme.kt`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/com/ansa1r/projectadhd/InterventionEngineTest.kt`
- `tools/verify_source.py`

### Удалены (10)

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

Остальные исходные файлы сохранены, включая Gradle wrapper JAR, LICENSE и исторические документы Stage 1. Удалены только дефолтные launcher WEBP, заменённые ресурсами маскота. ZIP не содержит .git/.idea/.gradle, build outputs, local.properties, signing keys или временных рабочих файлов.
