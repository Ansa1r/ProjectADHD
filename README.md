# ProjectADHD — Bob Dialogue System v1

Обновление Alpha: Coach Panel внутри Scaffold над Bottom Navigation, PCM pseudo-speech, рот и typewriter-текст от единого audio playhead. Сборочные metadata из исходного проекта сохранены: `versionName = 0.4.1-pre-alpha`, `versionCode = 7`.

Проверка 2026-10-03: `testDebugUnitTest` — 153 теста, 0 ошибок; `assembleDebug` — BUILD SUCCESSFUL, debug APK собран. Сценарии интерфейса и звучание на устройстве требуют ручной приёмки. Актуальные результаты и ограничения: [ALPHA_BOB_DIALOGUE_REPORT](docs/ALPHA_BOB_DIALOGUE_REPORT.md).

ProjectADHD — локальное Android-приложение для привычек и ограничения отвлекающих приложений. Namespace и applicationId: `com.ansa1r.projectadhd`.

## Возможности

- Guided onboarding с Бобом на настоящих Home, Habits, Apps, Stats и Mascot. Coach Panel резервирует место над нижним меню; основное содержимое учитывает Scaffold padding. Каждая реплика запускает отдельную плавную PCM-фразу. Первое нажатие раскрывает текст и гасит звук, следующее продолжает прежнюю последовательность настройки.
- Выбор приложений и индивидуальных лимитов, создание первой привычки, проверка Usage Access, Overlay и уведомлений. На API 24–25 сохраняется существующий notification fallback.
- Автоматический мониторинг только после финального «Готово» Боба. При следующих открытиях сервис проверяется и запускается, если сохранённое предпочтение включено.
- Постоянная настройка мониторинга внизу Settings; остановка с подтверждением Боба. Отмена ничего не меняет. Stop переживает перезапуск.
- Startup-анимация при холодном запуске UI/процесса и возвращении после ≥10 минут в фоне. Короткий возврат, внутренние переходы и поворот экрана не запускают её заново.
- Profile: пользовательские аватар и nickname, реальные показатели.
- Mascot: имя Боба, уровень, XP внутри уровня, lifetime XP, streak и история наград.
- MANUAL/APP_BASED Habits, накопление времени, подтверждение ручной привычки, +10 минут после «Нет», автоматическое выполнение app-based привычки, защита от конфликтов с ограничениями.
- Существующие BlockSession, Blocking Overlay, PRAISE, fallback notifications, статистика и Debug.
- Purple fill: обычный UI 0.70, blocking scrim 0.85, панель Боба 0.60; прозрачность не применяется ко всему компоненту.

## Сборка и проверка

Сохранён текущий toolchain проекта: Gradle 9.6.0, AGP 9.4.1, Kotlin/Compose plugin 2.2.10, compile/target SDK 37, minSdk 24. Необходимы совместимый JDK, Android SDK и доступ к репозиториям зависимостей.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
# На подключённом тестовом устройстве/эмуляторе:
./gradlew connectedDebugAndroidTest
python3 tools/verify_source.py
```

Windows: `gradlew.bat testDebugUnitTest` и `gradlew.bat assembleDebug`. APK после успешной локальной сборки: `app/build/outputs/apk/debug/app-debug.apk`.

Python-проверка проверяет XML, ресурсы, SQL и исходные assets. Она **не заменяет** компиляцию Kotlin, JUnit или проверки на устройстве.

## Данные и обновление

Room 4; зарегистрирована цепочка миграций 1→2→3→4. Старый `totalXp` сохранён как SQL-колонка lifetime XP. Новые `currentLevel` и `currentLevelXp` рассчитываются один раз по новым требованиям, без сброса наград и истории.

DataStore сохраняет этап onboarding и выбор мониторинга. Отсутствующие новые ключи означают WELCOME и monitoring OFF. Старые профиль, avatar, nickname, имя маскота и настройки не очищаются. Уже сохранённые приложения и привычки можно использовать в setup.

После копирования проекта не оставляйте старые удалённые файлы поверх него: распакуйте архив в отдельную чистую папку. `local.properties`, локальные SDK-пути и ключи в архив не включены.

## Документация

- [Архитектура Bob Dialogue](docs/ALPHA_BOB_DIALOGUE_ARCHITECTURE.md)
- [Ручная проверка диалогов](docs/ALPHA_BOB_DIALOGUE_MANUAL_TEST.md)
- [Отчёт по текущему обновлению](docs/ALPHA_BOB_DIALOGUE_REPORT.md)

- [Техническое описание](docs/PREALPHA_FINAL_TECH_SPEC.md)
- [Ручная приёмка A–O, UI и регрессии](docs/PREALPHA_FINAL_MANUAL_TEST.md)
- [Отчёт, полный список файлов и ограничения](docs/PREALPHA_FINAL_REPORT.md)
- [Исходные Stage4-материалы](docs/assets/stage4/)

Исторические Stage 2/4 и WELCOME-only отчёты сохранены. Для Coach Panel/audio/typewriter актуальны `ALPHA_BOB_DIALOGUE_*`; логика запуска, XP и мониторинга из `PREALPHA_FINAL_*` не перерабатывалась.
