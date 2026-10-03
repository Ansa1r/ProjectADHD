# Bob Idle Vector Animation v1 — отчёт

## База проекта

Использован актуальный GitHub `Ansa1r/ProjectADHD`, commit `6f5b4bbf0634023fe53011781e1d2db0b2597952` от 2026-10-02, tree `8ee2bf82387931c4bd20717c710106c462e2507e`. Все 242 исходных Git blob проверены по SHA. Приложенный ZIP содержит художественные материалы, не проект.

В задании указан статус 0.5.0 Alpha, но в актуальном `app/build.gradle.kts` задано 0.4.1-pre-alpha, versionCode 7. Эти значения сохранены; произвольного обновления версии не сделано. Package / namespace: `com.ansa1r.projectadhd`.

## Настоящие векторы

Созданы 36 редактируемых SVG, всего 99 вручную построенных исходных paths. Все слои имеют viewBox 400×400. Это чистая векторная реконструкция узнаваемого Боба с фиолетовым телом, большими голубыми глазами, зелёными волосами и шарфом. Не применялся плотный автоматический trace растра.

Ориентиры: оригинальные части из приложенного `source_parts`, `bob_composite_reference.png` и текущие runtime изображения для idle/blocking/praise выражений. Полосы и повреждения альфа-маски в `тело.png` не переносились. Некоторые детали с именами «рука» содержат отделённые декоративные элементы; реальные боковые части восстановлены по цельному Бобу. Сложная кистевая фактура упрощена до небольшого числа контуров и градиентов, без подмены PNG. Generated reference sheets использовались только как подсказки для разделения и состояний лица.

Из `тело.png` и цельного reference отдельно восстановлены BODY_BASE, HAIR, SCARF_NECK и SCARF_TAIL. Нижние четыре детали, боковые части, глаза, брови, румянец и рот имеют отдельные SVG. Ghost GIF использовался только как пример мягкого нижнего края; горизонтальная ходьба не скопирована. Красный шарф из reference не заменяет зелёный шарф Боба.

Созданные файлы SVG перечислены в `app/src/main/assets/bob/vector/layers.json`:

- `source/`: body_base, hair, scarf_neck, scarf_tail.
- `body/`: arm_left/right, bottom_1…4.
- `face/`: blush; четыре eyebrow-слоя; OPEN/HALF/CLOSED и HAPPY/HAPPY_HALF/HAPPY_CLOSED отдельно для каждого глаза; mouth_idle/smile/small/medium/wide и четыре blocking-mouth варианта.

SVG преобразуются в кэшированные Compose Path/Brush файлом `tools/generate_bob_vectors.py`. Runtime использует Canvas и векторные transforms. Нет GIF/WebP/video/sprite sheet/набора raster frames, skeletal rig, звука или lip sync. Все supplied references сохранены в `art/bob/reference` вне runtime Android assets. Старые PNG ресурсов не удалены и не используются новым renderer; startup PNG остаётся в прежнем отдельном startup flow.

## Реализация и подключения

Добавлены:

- `ui/mascot/animation/BobAnimationState.kt`: enum состояний, default IDLE/silent, чистая логика blink/talking и периодических движений.
- `ui/mascot/animation/BobMascot.kt`: единый renderer, Compose clocks, lifecycle, слои и pivots.
- `ui/mascot/animation/BobVectorAssets.kt`: генерируемые кэшированные paths/brushes.
- `ui/debug/BobAnimationPreview.kt`: debug-only preview, эмоции, Talking, Blink now, bounds/pivots.
- `BobAnimationTimelineTest.kt`: семь unit tests для defaults, речи, blink, интервалов, амплитуд и бесшовного цикла.
- SVG sources, generator, три документа, SVG/PNG-предпросмотры и актуальные build logs.

Изменены только пять существующих Kotlin-файлов:

| Файл | Изменение |
|---|---|
| `ui/mascot/MascotView.kt` | Общий адаптер теперь вызывает BobMascot вместо raster Image; сохраняет mood mapping |
| `ui/mascot/InterventionContent.kt` | Talking=true только в ветке Blocking dialogue |
| `ui/onboarding/BobCoachOverlay.kt` | Передаёт новый необязательный isTalking в маскота; layout не менялся |
| `ui/onboarding/OnboardingCoach.kt` | Talking только при OnboardingStep.WELCOME |
| `ui/debug/DebugScreen.kt` | В существующую debug-область добавлен preview |

Через общий адаптер animated Bob появляется на Home, Mascot Screen, Bob Coach/onboarding, Settings confirmation, подтверждении привычки, Blocking и Praise. Навигационный glyph остаётся статичным символом интерфейса. После WELCOME рот возвращается к покою; Blocking сохраняет frown, Praise — радостные глаза/рот. Talking в debug включается только вручную для проверки.

## Параметры

Float: цикл 2.8 с, около ±4.93 dp при размере 224 dp, без движения корпуса по X. Нижняя волна: ±1.4°, Y ±1.25 viewBox units, scaleY ±1.5%, четыре разных фазы. Scarf tail: ±2.4° с отставанием по фазе от парения; neck scarf и волосы прикреплены к root. Arms: ±0.8°.

Blink: отдых 2 с, цикл OPEN → HALF → CLOSED → HALF → OPEN за 300 мс; CLOSED держится 70 мс. Это замена векторных форм, не scaleY глаз. Mouth: IDLE/SMALL/MEDIUM/WIDE, неравномерная фраза 1.45 с; у Blocking отдельные формы. Внутри Canvas считываются clocks, Compose занимается кадрами; ручного 60 Hz Timer нет.

## Сохранённые системы

Побайтовая проверка исходных файлов подтверждает: launcher icons, StartupHost и startup resources, MainActivity, navigation, onboarding state machine, тексты и позиционирование WELCOME, Habit System, XP/Levels/Streak, Monitoring/UsageStats, OverlayController/blocking decisions, Profile/Avatar, Statistics, Database, Permissions и App limits не изменены. UI opacity constants также сохранены: Coach 0.60, Ordinary 0.70, Blocking 0.85. Git commit/push/PR не выполнялись.

## Фактически выполненные проверки

- Генератор SVG → Compose запущен; `python tools/generate_bob_vectors.py --check` прошёл. Все SVG path-only, в общей системе координат; generated runtime соответствует sources.
- SVG отрисованы Inkscape и просмотрены: OPEN/HALF/CLOSED, idle/blocking/talking/happy и крайние положения фаз парения. По этим статическим vector renders нет видимых отрывов низа/шарфа. Это не визуальный прогон Android приложения.
- Проверены текущие вызовы MascotView/BobMascot и границы изменений. Исходные artwork references сохранены, источник GIF находится только в reference-каталоге.

В среде Linux реально запущены эквиваленты Windows wrapper:

| Команда | Результат |
|---|---|
| `./gradlew testDebugUnitTest` | Exit 1 до компиляции: `java.net.SocketException: Operation not permitted` при загрузке Gradle 9.6.0. Unit tests не выполнялись. |
| `./gradlew assembleDebug` | Exit 1 на том же скачивании Gradle. APK не создан. |

Полные логи: `docs/validation/bob_idle_testDebugUnitTest.log`, `docs/validation/bob_idle_assembleDebug.log`. BUILD SUCCESSFUL не заявляется.

## Known limitations

- Сохранены узнаваемые форма, пропорции и палитра, но тонкая painterly texture не воспроизведена пиксель-в-пиксель. Все детали нового renderer векторные; raster fallback отсутствует.
- Декоративные FX вокруг Боба и floor shadow не входят в Idle v1.
- Полноценные HAPPY/BLOCKING/CONFIRMATION/LEVEL_UP/ONBOARDING motion-профили ещё не созданы; они используют IDLE movement и соответствующее доступное выражение.
- Talking — визуальный цикл пока диалог видим; определения конца речи/аудио нет.
- Android-компиляция, запуск на устройстве, реальные lifecycle/UI сценарии и 60 FPS не подтверждены: загрузка Gradle заблокирована, Android SDK/эмулятор недоступны. Для этих проверок приложен `ALPHA_BOB_IDLE_MANUAL_TEST.md`.
- Позиционирование WELCOME сохранено из актуального проекта и в этой задаче не исправлялось.

Архив содержит полный проект без `.idea`, `.gradle`, `build`, `local.properties`, keystore и временных файлов. `gradlew` имеет executable bit для Linux, содержимое wrapper сохранено.
