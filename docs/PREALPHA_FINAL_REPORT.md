# Final Pre-alpha — отчёт

**Результат:** изменения реализованы в локальной копии; успешная Android-сборка и приёмка пока не подтверждены. **Версия 0.4.1-pre-alpha, versionCode 7** (исходные 0.4.0-pre-alpha/code6). Целевой выпуск — 0.5.0 Alpha. Имя ZIP `ProjectADHD_Alpha_0.5.0.zip` сохранено по заданию, но это проект для проверки, не подтверждённый Alpha APK.

Источник — GitHub `Ansa1r/ProjectADHD`, commit `e8ff81b15568d6bf79bd78fad7bffd87947a4654` (`v0.32 pre-alfa`), 226 blobs сверены SHA. Реальная схема Room3 взята из этого commit. Remote не изменялся: **нет git commit, push или PR**.

## Что изменено

1. StartupHost сохранил дизайн; decision logic теперь разделяет cold UI/process, короткий фон и ≥10 минут. Монотонный timestamp не зависит от перевода даты. FGS не считается активным UI. Навигация и rotation не запускают новую последовательность.
2. Убран лишний верхний title Mascot. Bob name, level, XP, streak, customization/history сохранены; добавлен компактный hero для невысокого экрана.
3. `currentLevel + currentLevelXp` теперь определяют развитие; lifetime XP — отдельный счётчик. Requirements 15,30,45,…150,170,190,…; overflow и несколько уровней не теряются. Habit/Streak multiplier и ceil берутся до каждой награды.
4. Room migration 3→4 сохраняет старый totalXp и распределяет его по новым уровням. 48→L3/3, 150→L5/0. Исторические награды и их старые уровни не переписываются, новые XP не начисляются миграцией. Остальные таблицы и профиль не сбрасываются.
5. BrandOpacity централизует ordinary0.70/blocking0.85/coach0.50; прозрачность относится только к fill.
6. С Home удалено ручное Start/Stop. Внизу Settings — persistent monitoring preference, состояние и подтверждение Stop общим BobCoachPanel. Cancel ничего не меняет. Notification Stop открывает то же подтверждение.
7. WELCOME приветствует пользователя на Home после startup; затем настоящие Home/Habits/Apps/Stats/Mascot. Profile и Settings не включены в tour.
8. Setup использует настоящие app selection/limits, Create Habit и Permissions. Пустой выбор apps не завершает настройку. Сохранённые ранее данные можно оставить. Статусы permissions перепроверяются при возвращении и перед финалом.
9. Только финальная кнопка Боба на Home атомарно сохраняет completed=true, step=COMPLETED, monitoringEnabled=true; затем ensure сервиса. Другие start-пути закрыты до completed. Следующий foreground обеспечивает сервис, если preference=true; ручной OFF сохраняется.
10. BobCoachOverlay: TOP/Боб слева, BOTTOM/справа, читаемый текст, primary/optional secondary, safe area/IME, прокрутка при нехватке высоты, блокировка случайных taps и semantics. Settings сохраняет plain dark background.
11. DataStore keys: onboarding_completed, onboarding_step (включая persisted editor stages), monitoring_enabled, last_ui_background_at. Новые keys default WELCOME/OFF; существующие nickname/avatar/mascot name/cooldown остаются. Отдельного очищающего DataStore migration нет.
12. Debug показывает все запрошенные monitoring/onboarding/startup/XP поля; +15/+33 XP защищены BuildConfig.DEBUG и записываются как DEBUG awards.
13. Сохранены MANUAL/APP_BASED, daily accumulated time, +10 после NO, auto app completion, XP/streak ledger, app conflicts, BLOCK/PRAISE, profile/avatar/nickname/stats и существующие migrations. Удалены устаревшие opacity code/tests, обращавшиеся к уже отсутствующим API, и старые launcher WEBP из наложенного snapshot.

## Проверки

| Проверка | Фактический результат |
|---|---|
| `./gradlew testDebugUnitTest` | Команда выполнена, exit1 **до компиляции**: Network is unreachable при загрузке Gradle9.6.0. JUnit не запускался. |
| `./gradlew assembleDebug` | Команда выполнена, exit1 на той же загрузке. APK не создан. |
| `python3 tools/verify_source.py` | PASS: XML/resources, namespace/source roots/catalog/wrapper/manifest, реальные SQL migrations и 37 DAO queries, FK/уникальность/rollback/history count, SHA runtime assets. |
| SQL 3→4 | Реальные ALTER/UPDATE проверены SQLite с эталонными bindings 48→3/3; это **не исполнение Kotlin conversion**. Реальная KSP/Room metadata проверка описана отдельным instrumentation test. |
| Лексическая проверка Kotlin | Баланс строк/комментариев/скобок 122 файлов проверен; не является компиляцией или type checking. |
| Тесты в проекте | 131 unit и 43 Android @Test; **ни один не заявлен выполненным** в этой среде. |
| Device/manual | Не выполнялись: нет Android SDK/adb/эмулятора. Полный checklist в PREALPHA_FINAL_MANUAL_TEST.md. |
| Packaging | Полный корень проекта, без вложенного двойного ProjectADHD, .idea/.gradle/build/local.properties/keystore/секретов. CRC и соответствие содержимому проверяются при упаковке. |

Обновлены MascotProgressionTest и ForegroundEntryTrackerTest, добавлен OnboardingStateTest. Android: FinalizationMigrationTest (экспортированная3→Room4), OnboardingPersistenceTest (перезапуск DataStore, сохранение profile и OFF), BobCoachOverlayTest (стороны Боба/touch interception). Legacy navigation regression fixtures стартуют после onboarding с monitoring OFF. Старые migration/repository tests обновлены до новой модели и Room4.

Логи в `docs/validation/prealpha_final_*.log`. `BUILD SUCCESSFUL` не заявляется. Невозможность скачать toolchain не считается тестом компиляции и не скрывает возможные Kotlin/Compose/Room ошибки.

## Ограничения и дальнейшая приёмка

- Перевод в 0.5.0 Alpha запрещён до успешной сборки и прохождения acceptance criteria. После проверок обновить versionName и code (минимум8, если code7 уже устанавливался), README и собрать заново.
- Room4 JSON ещё не сгенерирован: требуется первый успешный KSP build. Экспортированные1/2/3 сохранены; 4.json не выдуман.
- Поворот, маленькие экраны, крупный шрифт, TalkBack, системные permissions и FGS на разных API требуют фактической проверки. Screenshots настоящего работающего приложения здесь не создавались.
- Room и DataStore — отдельные хранилища. При смерти процесса между Save и продолжением сохранённая сущность остаётся; setup позволяет продолжить с ней. Несохранённый form/selection draft может потребовать повторного ввода. При обычной ошибке продолжения повтор Save привычки не создаёт ещё одну запись.
- Старые данные получают WELCOME/OFF при отсутствии новых flags; пользователь проходит новое знакомство и может использовать уже сохранённые apps/Habit. Это выбранная стратегия обновления, без уничтожения данных.
- Android управляет сроком жизни FGS. При enabled=true сервис обеспечивается при foreground; boot auto-start не добавлен, START_NOT_STICKY сохранён. Permissions после setup могут быть отозваны; ошибки показываются в Settings, preference не сбрасывается.
- При Stop ручные Habit timer/progress и foreground catch-up сохраняют самостоятельную Stage4-логику; выключается FGS мониторинга лимитов и блокировок.
- Изменение модели может уменьшить отображаемый старый уровень; lifetime XP сохранён полностью, это перерасчёт стоимости уровней, а не потеря наград.
- Runtime assets не редактировались. Все13 supplied PNG совпадают с `docs/assets/stage4/`. Не добавлялись AI/cloud/GIF/Lottie/магазин/соцсеть/новые dangerous permissions.

## Файлы

Добавлено 18, изменено 50, удалено 13 относительно GitHub snapshot. Дополнительно `gradlew` получил executable mode0755.

### Добавлены

- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/BobCoachOverlayTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/CompletedOnboardingRule.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/FinalizationMigrationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/OnboardingPersistenceTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/onboarding/OnboardingState.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/onboarding/BobCoachOverlay.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/onboarding/OnboardingCoach.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/onboarding/OnboardingViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/MonitoringSettingsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/theme/BrandOpacity.kt`
- `app/src/test/kotlin/com/ansa1r/projectadhd/OnboardingStateTest.kt`
- `docs/PREALPHA_FINAL_MANUAL_TEST.md`
- `docs/PREALPHA_FINAL_REPORT.md`
- `docs/PREALPHA_FINAL_TECH_SPEC.md`
- `docs/validation/prealpha_final_assembleDebug.log`
- `docs/validation/prealpha_final_source_checks.log`
- `docs/validation/prealpha_final_structure.log`
- `docs/validation/prealpha_final_testDebugUnitTest.log`

### Изменены

- `README.md`
- `app/build.gradle.kts`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/AppSelectionNavigationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/MainActivitySmokeTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/NavigationRevisionTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage2DatabaseTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage4MigrationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage4NavigationTest.kt`
- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/Stage4RepositoryTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/AppContainer.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/MainActivity.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/AppDatabase.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/Migrations.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/local/entity/Stage4Entities.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/preferences/AppPreferences.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/data/repository/MascotRepository.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/mascot/MascotProgression.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/startup/ForegroundEntryTracker.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/MonitoringController.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/PermissionManager.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/monitoring/UsageMonitoringService.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/navigation/AppNavigation.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/notification/NotificationHelper.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/AppsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/apps/InstalledAppIcon.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/components/BrandComponents.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/components/Common.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/debug/DebugScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/debug/DebugViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/CreateHabitScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/CreateHabitViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/HabitsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/habits/HabitsViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/home/HomeViewModel.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/InterventionContent.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/mascot/MascotScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/profile/UserAvatar.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/PermissionsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/SettingsScreen.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/ui/settings/SettingsViewModel.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/test/kotlin/com/ansa1r/projectadhd/ForegroundEntryTrackerTest.kt`
- `app/src/test/kotlin/com/ansa1r/projectadhd/MascotProgressionTest.kt`
- `docs/ASSETS.md`
- `docs/STAGE4_MANUAL_TEST.md`
- `docs/STAGE4_REPORT.md`
- `docs/TECH_SPEC_STAGE2.md`
- `docs/TECH_SPEC_STAGE4.md`
- `tools/verify_source.py`

### Удалены

- `app/src/androidTest/kotlin/com/ansa1r/projectadhd/BlockingOpacityPersistenceTest.kt`
- `app/src/main/kotlin/com/ansa1r/projectadhd/domain/settings/BlockingOpacity.kt`
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

