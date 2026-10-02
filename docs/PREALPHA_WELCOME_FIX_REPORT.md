# PRE-ALPHA: WELCOME layout

Дата: 2026-10-02. База — предыдущий ProjectADHD_PreAlpha_Welcome_Fix с Coach alpha 0.60, основанный на GitHub commit `88000c41ac76c7cc2ac34f6b7f298ae43b5c715e`. Последний commit повторно проверен; он не изменился. Версия приложения сохранена: 0.4.1-pre-alpha, versionCode 7.

## Что установлено о предыдущей попытке

Предыдущий архив действительно изменял только `BrandOpacity.Coach`: 0.50 → 0.60. Ни Compose-разметка, ни размещение панели тогда не менялись. Вывод о правильном положении был сделан по исходнику без проверки координат на устройстве; этого оказалось недостаточно.

В доступном исходнике **не обнаружена подтверждённая причина размещения WELCOME снизу**. Уже присутствовали передача TOP и `Modifier.align(Alignment.TopCenter)`. Родитель не закрепляет overlay снизу:

1. Компилируются `app/src/main/kotlin`; прежние template-файлы в `src/main/java` исключены существующим sourceSets.
2. `MainActivity` → `StartupHost`: полноэкранные Box.
3. `AppNavigation.kt`: `OnboardingCoach` находится в полноэкранном Box рядом со Scaffold, после него. Он вне `bottomBar`, вне NavHost и вне padding Scaffold content.
4. `OnboardingCoach.kt`: WELCOME → TOP; другие coach steps → BOTTOM.
5. `BobCoachOverlay.kt`: единственная реализация overlay; отдельного bottom slot или второго WELCOME нет.

Таким образом, объяснение вида «найден hardcoded BottomCenter, игнорировавший TOP» не подтверждено. Старый `align` в показанной структуре также должен был размещать TOP сверху. Расхождение с установленным приложением здесь не воспроизведено: нет Android SDK, adb и эмулятора. Ни устаревший APK, ни другой сохранённый шаг не объявляются установленной причиной.

## Конкретное изменение layout

Файл: `app/src/main/kotlin/com/ansa1r/projectadhd/ui/onboarding/BobCoachOverlay.kt`.

- Выбор alignment перенесён с modifier дочерней панели в **contentAlignment самого полноэкранного BoxWithConstraints**:

```kotlin
contentAlignment = when (position) {
    CoachPosition.TOP -> Alignment.TopCenter
    CoachPosition.BOTTOM -> Alignment.BottomCenter
}
```

- Для TOP используется `WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)`: сохраняются status bar и соответствующие display/camera cutout insets.
- TOP больше не получает нижний safe inset и `imePadding()`. Высота Bottom Navigation не участвует в его размещении.
- BOTTOM сохраняет `WindowInsets.safeDrawing` со всеми сторонами и прежний `imePadding()`.
- После safe inset остаётся обычный небольшой отступ 8 dp. Горизонтальный отступ 12 dp, существующий компактный BobCoachPanel и ограничение максимальной высоты сохранены.
- Добавлен `bob_coach_safe_area` test tag для измерения положения панели.

Это изменение layout и обработки insets, а не только enum. Оно делает размещение явным на уровне родителя; само по себе оно **не доказывает**, что причина наблюдавшегося поведения найдена. Offset, отрицательные отступы, device-specific значения и компенсация высоты нижнего меню не используются.

## Где WELCOME получает TOP

`app/src/main/kotlin/com/ansa1r/projectadhd/ui/onboarding/OnboardingCoach.kt`:

```kotlin
internal fun coachPositionFor(step: OnboardingStep): CoachPosition =
    if (step == OnboardingStep.WELCOME) CoachPosition.TOP else CoachPosition.BOTTOM
```

Та же функция используется production-вызовом BobCoachOverlay и тестами. Это извлечение прежнего условия без изменения значений других шагов. State machine, callback кнопки, надпись «Продолжить» и тексты остались прежними. Ветка TOP в существующем BobCoachPanel выводит Боба перед текстовой Column, слева.

## Проверки

- Добавлен `app/src/test/kotlin/com/ansa1r/projectadhd/OnboardingCoachPositionTest.kt`: WELCOME → TOP, остальные девять coach steps → BOTTOM.
- Дополнен `app/src/androidTest/kotlin/com/ansa1r/projectadhd/BobCoachOverlayTest.kt`: полноэкранный Box со Scaffold и NavigationBar; проверяются фактические `boundsInRoot`, верх панели на safe top + 8 dp, отсутствие пересечения с нижним меню, Боб слева от текста, кнопка и переход доменной модели WELCOME → HOME с размещением HOME снизу. Это проверка компонента в соответствующей иерархии, а не запуск MainActivity или production ViewModel.
- Исходная цепочка родителей проверена чтением кода. Побайтовое сравнение подтверждает сохранение navigation, Startup Animation, state machine, assets, Habits, Monitoring, Blocking Overlay, Profile/Mascot/XP.
- Заливка Coach 0.60, Ordinary 0.70 и Blocking 0.85 сохранена. BobCoachPanel и его цвета не менялись; alpha контейнера не добавлена.

## Фактические результаты сборки

В Linux реально выполнены эквиваленты Windows wrapper:

| Команда | Результат |
|---|---|
| `./gradlew testDebugUnitTest` | Exit 1 до компиляции: `java.net.SocketException: Operation not permitted` при загрузке Gradle 9.6.0. Unit tests не выполнялись. |
| `./gradlew assembleDebug` | Exit 1 на той же загрузке Gradle. APK не собран. |

Полные актуальные логи: `docs/validation/welcome_testDebugUnitTest.log` и `docs/validation/welcome_assembleDebug.log`.

Compose/UI test добавлен, но не запускался. Успешная компиляция, визуальная проверка на устройстве и исправление исходного наблюдавшегося дефекта пока не подтверждены. BUILD SUCCESSFUL не заявляется.

На Windows с настроенным Android SDK:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.ansa1r.projectadhd.BobCoachOverlayTest"
```

Последняя команда требует тестового устройства или эмулятора. Для ручной проверки именно WELCOME нужны чистые тестовые данные onboarding; установка поверх существующего приложения сохраняет текущий шаг. Удаление пользовательских данных автоматически не выполнялось.

В архив не включены IDE/Gradle/build-кэши, local.properties, keystore и временные файлы. Git commit/push/PR не выполнялись.
