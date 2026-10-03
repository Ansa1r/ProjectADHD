# Проверка WELCOME: 2026-10-03

База: актуальный GitHub `Ansa1r/ProjectADHD`, commit `9b63cc18055500486e822e64a9d5cd2591dcfd43`.
Все 315 исходных файлов сверены с Git blob SHA из дерева этого commit.

## Результат и предел проверки

В этой версии уже присутствует ранее внесённое изменение реальной Compose-разметки WELCOME. Новая причина появления панели снизу не найдена. Исправление наблюдаемого на устройстве дефекта не заявляется: установленный APK и скриншот недоступны, Android SDK и устройство отсутствуют. Эквивалентная перестановка alignment не выдается за найденную причину.

Production-код оставлен без изменений. Добавлен `app/src/androidTest/kotlin/com/ansa1r/projectadhd/WelcomeFirstLaunchTest.kt`, проверяющий настоящий MainActivity, и этот отчёт с логами команд.

## Почему предыдущий TOP не работал

Это не установлено. Текущий исходник не игнорирует TOP. Проверена цепочка:

1. Gradle компилирует `src/main/kotlin`; старый template MainActivity из `src/main/java` исключён существующим `sourceSets`.
2. `MainActivity` вызывает `StartupHost`, содержащий полноэкранный Box.
3. В `navigation/AppNavigation.kt` overlay — сосед Scaffold в полноэкранном Box. Он находится вне `bottomBar`, NavHost и padding Scaffold content.
4. `OnboardingCoach.kt` передаёт `position = coachPositionFor(step)`; WELCOME получает TOP, следующие coach steps сохраняют BOTTOM.
5. `BobCoachOverlay.kt` применяет position к `contentAlignment` полноэкранного `BoxWithConstraints`.

Ошибочный APK или сохранённый HOME могут объяснять расхождение, но ни одна из этих гипотез не подтверждена. Данные пользователя не сбрасывались, state machine не менялась.

## Фактическая layout-логика в проекте

Файл `app/src/main/kotlin/com/ansa1r/projectadhd/ui/onboarding/BobCoachOverlay.kt` уже содержит:

```kotlin
contentAlignment = when (position) {
    CoachPosition.TOP -> Alignment.TopCenter
    CoachPosition.BOTTOM -> Alignment.BottomCenter
}
```

TOP использует `WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)`. Это сохраняет защиту от status bar и cutout. Затем применяется обычный отступ 8 dp. Нижний safe inset и `imePadding()` относятся только к BOTTOM; высота Bottom Navigation не участвует в положении WELCOME. Offset, отрицательных отступов и компенсации высоты меню нет.

`BobCoachPanel` в ветке TOP помещает Боба перед текстовой Column, слева. Существующие текст, кнопка «Продолжить», ограничение высоты и обработчик перехода сохранены.

`ui/theme/BrandOpacity.kt`: Coach = 0.60, Ordinary = 0.70, Blocking = 0.85. Coach alpha применяется только к цвету Surface; alpha всего контейнера не добавлена. Эти значения уже были в проверенном commit и не изменялись.

Остальные onboarding steps, navigation, Startup Animation, Bob assets, Habit System, Monitoring, Blocking Overlay и Profile/Mascot/XP побайтово сохранены относительно GitHub.

## Новый регрессионный тест

`WelcomeFirstLaunchTest.actualActivityShowsWelcomeAtSafeTopAndContinuesToHome`:

- запускает production MainActivity с существующей Startup Animation;
- ждёт окончания Startup Animation и появления точного текста WELCOME;
- измеряет `boundsInRoot` панели, safe area, текста, Боба и реальной Bottom Navigation;
- проверяет safe top + 8 dp, отсутствие перекрытия status bar/cutout и нижнего меню, Боба слева;
- нажимает настоящую primary action и проверяет сохранённый шаг HOME и его прежнее нижнее положение.

Тест предназначен для отдельной чистой тестовой установки. Если onboarding уже продвинут, JUnit помечает его пропущенным с объяснением, не сбрасывая данные и не мешая остальным тестам. Пропуск не считается успешной проверкой первого запуска.

Существующие `OnboardingCoachPositionTest` и `BobCoachOverlayTest` сохранены. Новый instrumentation test не запускался и не компилировался в этой среде.

## Фактические результаты сборки

В среде Linux использован эквивалент `gradlew.bat` — `./gradlew`.

| Команда | Результат |
| --- | --- |
| `./gradlew testDebugUnitTest` (также повторено с `--info`) | Exit 1. После успешной загрузки Gradle 9.6.0 не удалось разрешить `org.gradle.toolchains.foojay-resolver-convention:1.0.0` в `settings.gradle.kts:14`. Компиляция и unit tests не начались. |
| `./gradlew assembleDebug` | Exit 1. Та же ошибка разрешения плагина на этапе settings. APK не собран. |

Первый запуск с ограниченной сетью завершился `java.net.SocketException: Operation not permitted`; повтор с разрешённой сетью загрузил Gradle и выявил описанную выше ошибку плагина. Gradle-конфигурация и версии зависимостей не менялись: ошибка не относится к WELCOME.

Актуальные полные логи: `docs/validation/welcome_current_testDebugUnitTest.log` и `docs/validation/welcome_current_assembleDebug.log`. BUILD SUCCESSFUL не заявляется.

На Windows с настроенной средой:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.ansa1r.projectadhd.WelcomeFirstLaunchTest"
```

Для завершения диагностики нужен APK, воспроизводящий нижний WELCOME, либо результат этого теста на чистом тестовом устройстве. Архив не содержит `.idea`, `.gradle`, build-кэшей, `local.properties`, keystore и временных файлов. Git commit/push/PR не выполнялись.
