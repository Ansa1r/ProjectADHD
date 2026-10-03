# ALPHA — Bob Dialogue System v1: отчёт

Дата: 2026-10-03. Namespace/applicationId: `com.ansa1r.projectadhd`.

## Исходная версия

Во вложениях были задание и `ProjectADHD_Bob_Dialogue_Audio_Assets_v1(1).zip`. Свежего ZIP проекта не было. Использован фактически прочитанный GitHub snapshot `aa57ae06f0eca9ca202eede2887336dd1faeccd2` («v0.51 Alpha»). Все 317 исходных файлов сверены с Git blob SHA. Проект не восстанавливался по памяти.

307 исходных файлов сохранены побайтово; изменены 10 файлов (5 production Kotlin, 4 существующих теста и README). Добавлены dialogue/audio классы, 8 исходных WAV, 2 набора unit tests и документация. Gradle metadata исходника `0.4.1-pre-alpha / versionCode 7` сохранены; название GitHub commit отличается от них.

## 1. Почему Боб говорил только в первом message

В production `OnboardingCoach.kt` было прямое условие `isTalking = step == OnboardingStep.WELCOME`. Для остальных шагов передавался false. Текст выводился сразу полным, аудио-сессий не было. Это подтверждённая причина в этой базе. Теперь все десять coach messages используют модель с отдельным ID и отдельной playback session.

## 2–3. Старый overlay и перекрытие меню

`navigation/AppNavigation.kt` вызывал `OnboardingCoach` после Scaffold, его соседом в полноэкранном Box. Вызванный `BobCoachOverlay` занимал всю область экрана и позиционировал panel независимо от `bottomBar`. WELCOME уже имел TOP, остальные coach steps — BOTTOM. TOP мог увести конкретную панель наверх, но не резервировал место в layout и не устранял пересечение нижних coach steps с навигацией. Причина прежнего наблюдения именно WELCOME снизу на конкретном устройстве не объявляется установленной.

## 4–5. Новая Scaffold architecture

Coach теперь находится первым элементом Column в `Scaffold.bottomBar`, непосредственно перед прежней NavigationBar. Основное содержимое использует фактический Scaffold padding и consumeWindowInsets. Coach не делит одну область размещения с контентом или нижним меню. Floating BobCoachOverlay из onboarding больше не вызывается.

Bob справа, текст слева. Панель имеет естественную высоту полного сообщения, ограничение максимума относительно viewport и прежнюю прокрутку для крупных шрифтов. После onboarding она удаляется из bottom slot. Для обычной навигации сохранены те же items, внешний вид и обработчики. Нет offset, отрицательных отступов или расчёта высоты меню вручную.

## 6–8. WAV, smooth speech и crossfade

В raw скопированы ровно предоставленные `bob_voice_01.wav`…`bob_voice_08.wav`, без перекодирования. Проверены исходные bytes и bytes каждого WAV внутри APK. Формат всех файлов: mono, PCM 16-bit, 44,100 Hz.

BobSpeechPlanner выбирает последовательность без соседних одинаковых samples. Длительность зависит от длины текста при базовой скорости 16 символов/сек. BobSpeechPlan содержит sequence, overlap, totalFrames и duration. Соседние samples смешиваются с линейным crossfade 60 ms / 2,646 frames в одну PCM-дорожку. Подготовка выполняется в фоне; PCM записывается в один static AudioTrack. Добавлены короткие attack/release на границах фразы, output gain 0.8. Pitch не меняется. Качество звучания требует прослушивания на устройстве.

## 9–10. Audio progress и текст

Источником времени служит `AudioTrack.playbackHeadPosition`, а не ожидаемая длительность или отдельный текстовый таймер. Played frames нормализуются к totalFrames. WeightedTextReveal учитывает пробелы и пунктуацию и возвращает безопасную границу строки. В конце воспроизведения всегда доступен полный текст.

Text измеряется по полному AnnotatedString с прозрачным ещё не показанным суффиксом. При раскрытии той же реплики размер панели, кнопок и положение Боба не меняются. Accessibility получает полную реплику без посимвольного потока объявлений.

## 11. Mouth

MascotView передаёт фактический playedMillis и isSpeaking в прежний векторный BobMascot. Для audio-driven dialogue самостоятельный mouth clock отключён. Рот начинает двигаться только после продвижения playhead, а после конца/skip становится IDLE, в том числе у FINAL с happy eyes. Blink, floating, wave, scarf, arms и существующие assets сохранены. Blocking/Praise и другие прежние callers используют прежние defaults; их dialogue/audio интеграция в этом обновлении не менялась.

## 12. Skip

Первое нажатие на панель, primary или secondary action во время подготовки/речи раскрывает текст, закрывает рот и отменяет speech. AudioTrack затухает за 60 ms и освобождается. Шаг не меняется. Следующее нажатие вызывает существующий callback. Очень быстрое второе нажатие ставится в очередь до завершения fade. Повторные advance подавляются, а existing expected-step guard остаётся в ViewModel.

## 13. Recomposition, lifecycle и release

Controller/engine сохраняются через remember. Новая сессия запускается по ID/text/profile; обычная recomposition не перезапускает звук. repeatOnLifecycle(RESUMED) прекращает речь при уходе в background; возвращение может начать текущую реплику заново. Dispose также отменяет сессию.

Токены сессии отбрасывают старые callbacks. Старые jobs отменяются и ожидаются, app-wide Mutex исключает одновременный вывод из разных экземпляров engine. NonCancellable cleanup выполняет fade, stop/release и abandonAudioFocus. Ошибка WAV/output, отказ/потеря audio focus или застрявший playhead раскрывает текст и оставляет продолжение доступным.

## 14. Проверенные шаги и тесты

Unit tests покрывают отдельную speaking session для WELCOME, HOME, HABITS, APPS, STATS, MASCOT, SETUP_APPS, SETUP_HABIT, SETUP_PERMISSIONS и FINAL. Проверены progress 0/0.5/1, пунктуация, границы Unicode, уникальные IDs, stale callback, skip до старта/во время речи, второе нажатие, background/restart, audio failure, оригинальный PCM format, длительность и плавное смешивание.

BobCoachOverlayTest обновлён для Scaffold/Column: content выше panel, panel выше navigation, Bob справа, bounds панели и меню стабильны при 0/50/100% текста, после удаления coach контент расширяется. WelcomeFirstLaunchTest проверяет production MainActivity, Startup, реальные bounds, skip и сохранённый переход HOME. Эти instrumentation tests скомпилированы, но не выполнялись на устройстве.

Устаревший BlockingOpacityPersistenceTest ссылался на отсутствующие в исходнике `setBlockingOpacity`, `blockingOverlayOpacity` и `blockingOpacityPercent`, блокируя компиляцию всех Android tests. Он обновлён под существующий фиксированный alpha 0.85 и сохранение обычных настроек при наличии старого preference key. Production Blocking Overlay/Preferences не менялись; существующий pixel test BlockingOpacityContentTest сохранён.

## Фактическая сборка

В Linux выполнены эквиваленты запрошенного Windows wrapper:

| Команда | Результат |
| --- | --- |
| `./gradlew testDebugUnitTest` | **BUILD SUCCESSFUL** — 153 tests, 0 failures, 0 errors, 0 skipped |
| `./gradlew assembleDebug` | **BUILD SUCCESSFUL** — debug APK собран |
| `./gradlew assembleDebugAndroidTest` | **BUILD SUCCESSFUL** — UI/instrumentation tests скомпилированы; запуск на устройстве не выполнялся |

Первоначальные проблемы среды (доверенные сертификаты скачанного JDK и отсутствующий SDK) устранены вне проекта. Установлены официальные SDK platform 37.0 и build-tools 37.0.0; версии Gradle/AGP/Kotlin проекта не менялись. APK успешно проверен apksigner, схема подписи v2. Логи и summary с SHA-256 расположены в `docs/validation/bob_dialogue_*`.

## 15. Ограничения и сохранённая логика

- В предоставленной через GitHub базе customization — ProfilePlaceholderScreen. Нет BobAppearance или сохранённых body/hair/scarf/blush/eyes. Доступный renderer и его цвета сохранены; совместимость с отдельной непредоставленной реализацией customization не проверена.
- Нет результатов запуска на устройстве, визуальной приёмки, прослушивания, проверки Bluetooth/audio latency и производительности. Для них подготовлен ALPHA_BOB_DIALOGUE_MANUAL_TEST.md. Сборка UI-test APK не равна выполнению UI tests.
- Debug APK подписан ключом среды сборки и предназначен для тестовой установки. Для обновления своей установки с другим debug-ключом и сохранением данных следует собрать проект своим ключом; пользовательские данные автоматически не удалялись.
- Startup Animation, assets, state machine, строки десяти реплик, onboarding persistence/final monitoring transition, Habits, Monitoring, Blocking Overlay, Profile/Mascot/XP/levels/streak, Permissions и DataStore/Room остаются прежними. Сверка исходников подтверждает сохранение этих файлов; поведенческая приёмка на устройстве остаётся необходимой.
- Coach fill 0.60, ordinary fill 0.70, Blocking scrim 0.85 сохранены. Alpha контейнера не добавлена.
- В ZIP нет IDE/Gradle/build-кэшей, local.properties, ключей или временных файлов. APK предоставлен отдельно. Git commit/push/PR не выполнялись.

Основные изменённые production файлы: AppNavigation.kt, OnboardingCoach.kt, BobCoachOverlay.kt, MascotView.kt, BobMascot.kt. Добавленные production классы: domain/dialogue/*, audio/AndroidBobSpeechEngine.kt, ui/onboarding/RememberBobDialogue.kt.
