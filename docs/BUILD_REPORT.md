# Отчёт о проверке Stage 1

Дата подготовки: 29 сентября 2026 года.

## Основа

Приватный репозиторий `Ansa1r/ProjectADHD`, ветка `main`, commit `cfc4071ee808e4d23712a736fd56c7101a10da1c`. Все 42 исходных файла получены через GitHub в режиме чтения. Размеры и Git blob SHA-1 проверены после восстановления файлов, включая бинарные ресурсы и Gradle Wrapper. Push, создание веток и Pull Request не выполнялись.

Изучены README, LICENSE, оба .gitignore, .gitattributes, Gradle-файлы, version catalog, manifest, Activity, Compose-тема, все XML-ресурсы, launcher-ресурсы, шаблонные unit/instrumentation-тесты и keep rules.

Исходник содержал Compose-шаблон, а не реализации Room, привычек и мониторинга. Основные исходные параметры: AGP 9.4.1, Gradle 9.6.0, Kotlin 2.2.10, Compose BOM 2026.02.01, SDK 37/37, minSdk 24, bytecode Java 11, daemon toolchain JDK 25. Эти значения сохранены. Namespace/applicationId перенесены в `com.ansa1r.projectadhd`. Проприетарная лицензия сохранена с исправленным названием.

## Фактически запущенные команды

| Проверка | Результат |
| --- | --- |
| `./gradlew testDebugUnitTest` на исходной конфигурации | Ошибка инфраструктуры до конфигурации проекта |
| `./gradlew testDebugUnitTest` после реализации | Exit code 1: не удалось загрузить Gradle |
| `./gradlew assembleDebug` после реализации | Exit code 1: не удалось загрузить Gradle |
| `python3 tools/verify_source.py` | Успешно |
| Android instrumentation / эмулятор / физический телефон | Не запускались: устройства и SDK нет |

Обе итоговые Gradle-команды завершились на шаге:

```text
Downloading https://services.gradle.org/distributions/gradle-9.6.0-bin.zip
Exception in thread "main" java.net.SocketException: Network is unreachable
```

В среде доступен Java runtime 17, но нет установленного Gradle, Kotlin-компилятора, полного JDK, Android SDK и эмулятора. Gradle, Android-плагины и зависимости не закешированы. Восстановление toolchain и зависимостей по сети недоступно. Это блокирует даже запуск unit-тестов через Android Gradle plugin.

**Успешная сборка не заявляется. Число реально выполненных JUnit-тестов: 0.** Подготовлены 46 unit-тестов и 3 instrumentation-теста. До компиляции исходников выполнение не дошло; отсутствие сообщений Kotlin не доказывает отсутствие ошибок компиляции.

## Проверки без Android SDK

- XML разобран стандартным XML-парсером; локальные ссылки ресурсов из XML и Kotlin разрешаются.
- Version catalog разобран TOML-парсером; Gradle aliases и ссылки версий существуют.
- Package-директории и namespace/applicationId соответствуют новому имени.
- Корни исходников настроены через актуальный AGP Kotlin source set, чтобы оставшиеся после распаковки старые шаблонные файлы не компилировались.
- Manifest содержит только PACKAGE_USAGE_STATS, POST_NOTIFICATIONS, FOREGROUND_SERVICE и FOREGROUND_SERVICE_SPECIAL_USE; сервис не экспортирован, есть subtype и launcher queries.
- Backup отключён, правила исключают базы и настройки из cloud backup и device transfer.
- Архив Gradle Wrapper JAR читается и содержит GradleWrapperMain; исключение для Wrapper сохранено в .gitignore.
- Все 15 SQL-запросов DAO подготовлены настоящим SQLite. Структура таблиц для этой проверки построена из полей Kotlin-сущностей. На SQLite выполнены сценарии уникальной отметки привычки, смены дня, исключения выключенной привычки, каскадного удаления отметок и порядка истории.
- Unit-тесты содержат реальные проверки движка, порогов, cooldown, Activity-переходов, блокировки, переключений приложений, дневных интервалов и часового пояса.
- Полнота ZIP, отсутствие запрещённых файлов и распаковка поверх исходной копии проверены. Проверка исходников выполнена также на копии после такой распаковки; старые шаблонные файлы остаются вне заданных корней компиляции.

Эти проверки не заменяют Kotlin-компилятор, KSP, Android lint или выполнение Room на устройстве. Схема Room будет экспортирована KSP при первой успешной сборке; выдуманный generated-код не добавлялся.

## Что проверить в Android Studio

Запустить `testDebugUnitTest`, `assembleDebug`, затем `connectedDebugAndroidTest` при подключённом устройстве. Пройти [MANUAL_TESTS.md](MANUAL_TESTS.md). Особое внимание уделить Android 13/14/17, отключению разрешений, блокировке, переходам между Activity и управлению процессами производителем телефона.

Официальные основания выбора FGS и настройки сборки приведены в README. Тип specialUse описывает выбранный сценарий, но не является гарантией приёма приложения в Google Play.
