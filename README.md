# ProjectADHD 0.4 Pre-alpha

Внутренний этап **Stage 4 — Habit System & Mascot Progression**. Приложение остаётся **Pre-alpha**.
Существующий Android Studio проект, Kotlin/Compose/MVVM/Room/DataStore, package `com.ansa1r.projectadhd`.
VersionName `0.4.0-pre-alpha`, versionCode `6`.

- **Profile пользователя** открывается через avatar/nickname в Home. Фото выбирается системным Picker, сохраняется локально и сразу обновляет UI; Боб не является аватаром. Nickname сохраняет прежний DataStore key.
- **Mascot** — отдельная последняя нижняя вкладка: Боб, локальное имя, сохранённые XP, level, completed count, streak, история наград и рабочая заглушка кастомизации.
- **MANUAL habit**: отдельный экран создания, цель HH:MM, Start/Pause/Resume, накопление за день даже с выключенным экраном. На цели нужен YES; NO сохраняет progress и добавляет +10 минут только сегодня.
- **APP_BASED habit**: связанное приложение и реальное foreground usage. Разные сессии суммируются, Home/lock/другое приложение останавливают счёт; target завершает автоматически.
- **BLOCK/PRAISE**: выполнение новой baseline привычки снимает блок. MANUAL — после YES, APP_BASED — автоматически. Все привычки выполнены заранее → прежний PRAISE.
- **Конфликты**: полезное habit app нельзя одновременно ограничить; disabled rows в обе стороны и transactional validation. Legacy conflicts видны, блокировка конфликтующего package подавляется без удаления данных.
- **Фирменный UI**: purple fill85%, outline/text/icons100%; BLOCK scrim85% фиксирован, slider удалён. Прежняя заставка и assets сохранены.

## XP / уровни / серия

Habit base5, completed-day base10. Награда `ceil(base × 1.5^floor((level−1)/10))`, уровень берётся перед каждой наградой.
Total threshold `T(L)=150q+25q(q−1)+r(15+5q)`, q=floor(L/10), r=L mod10.
T(1)=15, T(10)=150, T(11)=170, T(20)=350, T(21)=375. Total0…14 означает level1; total15 означает level2.
Journal PK исключает повторную награду habit/date и streak/date; общий XP хранится отдельно.
Все активные дела дня выполнены → серия+1 и одна награда. Незаконченный активный день сбрасывает серию; пустой день нейтрален.

## Работа и данные

Room schema **3**, migrations **1→2→3**, без destructive migration. Пять прежних таблиц/данных сохранены; добавлены daily progress, mascot state, XP ledger, day snapshots.
Старые habits: MANUAL/30 минут; старые completions сохранены без ретроактивного XP и без выдуманных таймерных минут.

Первый запуск: permissions → Apps → выбор → Continue → лимиты → Save; monitoring запускается на Home. Привычки можно выполнять до любой блокировки.
Для живого app tracking в фоне нужен прежний foreground service с разрешениями. После STOP фоновые usage-запросы прекращаются; UI позже уточняет linked usage по доступной истории.
Ручной таймер независим от monitoring. В том же boot время восстанавливается через elapsedRealtime; полный reboot сохраняет только последний надёжный checkpoint и ставит сессию на паузу.
В полночь новая дневная ручная сессия начинается с READY/0 и требует Start. Одновременно идёт одна MANUAL сессия.
Обычное редактирование привычки переименовывает её; новая duration/type/app задаётся новой привычкой.

Нет backend, облачной загрузки фото, аналитики, аккаунтов, AI, AccessibilityService, root или kiosk mode. Данные локальны, backup выключен.

## Открытие и сборка

Распаковать **в новый каталог**, открыть корень с `settings.gradle.kts`. Не накладывать ZIP на старое дерево: удалённые файлы могут остаться.
Используются существующие версии Gradle9.6.0 / AGP9.4.1 / compileSdk37, minSdk24, targetSdk37. SDK/dependencies устанавливаются обычным Android Studio setup.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
# На отдельном чистом тестовом эмуляторе:
./gradlew connectedDebugAndroidTest
python3 tools/verify_source.py
```

На Windows: `gradlew.bat`. APK при успешной сборке: `app/build/outputs/apk/debug/app-debug.apk`.

**Результат этой среды:** обе команды Gradle выполнены с exit1 до компиляции: Wrapper не загрузил дистрибутив, `java.net.SocketException: Network is unreachable`.
Kotlin/JUnit, Android instrumentation и визуальные проверки не запускались. **BUILD SUCCESSFUL не подтверждён; APK не создан.**
Python/SQLite source checks прошли: миграции и сохранность данных, 37 DAO queries, ресурсы/manifest/wrapper, уникальность наград и rollback.
Room JSON3 будет создан KSP при успешной сборке; проверка миграции against generated metadata предусмотрена instrumentation test.

## Документы

- [Техническая спецификация Stage4](docs/TECH_SPEC_STAGE4.md)
- [Отчёт с полным перечнем файлов, tests и ограничениями](docs/STAGE4_REPORT.md)
- [Ручные сценарии A–L](docs/STAGE4_MANUAL_TEST.md)
- [Логи проверки](docs/validation/)
- [Предоставленные Stage4 assets](docs/assets/stage4/)

Stage1–3 reports сохранены как история; актуальное поведение описано в Stage4 документах.
