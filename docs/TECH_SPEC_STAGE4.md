> Исторический документ. Текущие startup, XP, onboarding и monitoring описаны в [PREALPHA_FINAL_TECH_SPEC.md](PREALPHA_FINAL_TECH_SPEC.md).

# ProjectADHD 0.4 Pre-alpha — Stage 4

Стадия зрелости: **Pre-alpha**. Внутренний этап: **Stage 4 — Habit System & Mascot Progression**.
Основа: существующий `Ansa1r/ProjectADHD`, commit `76da494ca5063caf00a2881215ad544b603af7b4`, 2026-10-01.
Package/namespace: `com.ansa1r.projectadhd`. VersionName `0.4.0-pre-alpha`, versionCode `6`.

## Profile и аватар

Profile относится к пользователю. Home header содержит аватар, nickname и прежнюю шестерёнку.
Нажатие аватара/имени открывает Profile. Приветствие удалено. Нижние вкладки: Home / Habits / Apps / Stats / Mascot, только иконки с contentDescription.
Profile показывает реальные выполненные/активные привычки дня, число ограниченных приложений и число вмешательств из полного журнала.
«Мой прогресс» открывает существующую статистику. Edit Profile сохраняет nickname через прежний `profile_nickname`; default «Пользователь».

`ActivityResultContracts.PickVisualMedia(ImageOnly)` использует системный Photo Picker с официальным fallback для старых устройств.
Новых разрешений на галерею и сетевых разрешений нет. `AvatarRepository` копирует только выбранное изображение в приватный `filesDir/avatars/avatar-UUID.jpg`.
Копирование ограничено 20 MiB входных данных. Bitmap декодируется с уменьшением длинной стороны до 1024 px, EXIF orientation применяется, JPEG создаётся без исходных метаданных.
Сначала полностью записывается новый файл, затем DataStore `profile_avatar_file` переключает ссылку; прежний файл удаляется после сохранения ссылки.
Flow немедленно обновляет Home, Profile и Edit Profile. Доступ к исходному URI после сохранения не нужен.
Отмена Picker ничего не меняет; ошибка оставляет предыдущий аватар. Аватар сохраняется сразу, кнопка «Сохранить» относится к nickname.
Placeholder — фирменный круг с символом пользователя, не Боб. Отключённый backup и отсутствие INTERNET сохранены.

## Mascot

Боб — отдельный персонаж. Имя в DataStore `mascot_name`, default «Боб», редактируется локально (1–32 Unicode code points, как nickname).
Mascot Screen: idle PNG, имя, уровень, прогресс внутри текущего уровня, общий XP, сохранённый счётчик завершений, серия.
«Кастомизация» — реально открываемая заглушка без предметов. «История развития» читает последние 200 записей журнала XP; весь журнал и общий XP сохраняются.
Полноценные анимации, магазин, валюта и аккаунты не добавлены.

## Уровни и награды

Для L ≥ 1, q = floor(L / 10), r = L mod 10:

```
T(L) = 150q + 25q(q − 1) + r(15 + 5q)
level(totalXp) = первый L, для которого totalXp < T(L)
multiplier(L) = 1.5 ^ floor((L − 1) / 10)
reward(base, L_before) = ceil(base × multiplier(L_before))
```

T(1)=15, T(2)=30, T(10)=150, T(11)=170, T(12)=190, T(20)=350, T(21)=375, T(100)=3750.
0…14 XP → level 1; 15…29 → level 2. Поиск уровня двоичный, без таблицы. Total XP — Long; уровень — Int, сложение защищено от переполнения.
Base XP привычки = 5; base XP завершённого дня = 10.
Habit XP на уровнях 1/11/21/31: 5/8/12/17. Streak XP: 10/15/23/34.
Каждая награда использует уровень ДО своей выдачи. Habit award выполняется перед проверкой streak: если привычка повысила уровень, streak reward уже использует новый multiplier.

XP хранится в `mascot_progress.totalXp`, не пересчитывается из количества записей.
`xp_awards.eventKey` — PK: `habit:<id>:<date>` либо `streak:<date>`; INSERT IGNORE блокирует повторные награды.
Completion, daily state, XP, mascot counter, streak и освобождение BlockSession проходят внутри одной Room transaction.
Повторные YES, polling, конкурирующие callbacks и перезапуск процесса не создают вторую награду.
Удаление привычки удаляет её daily/completion records, но не журнал XP и не исторический счётчик заработанных завершений.
Обычного undo completion больше нет; старый repository adapter не обходит confirmation.

## Серия

`habit_days` сохраняет localDate, число активных привычек, число выполненных и факт награды за день.
День завершается, когда все активные привычки выполнены и их больше нуля. Последняя completion выдаёт streak reward и увеличивает серию один раз.
При наступлении следующей даты незаконченный день с активными привычками обнуляет серию. Пустой день нейтрален: не увеличивает серию и не выдаёт XP.
Изменение active/delete обновляет набор задач дня; архивирование последней невыполненной задачи может завершить оставшийся непустой активный набор.
Создание новой задачи после уже полученной дневной награды не отнимает XP; если эта задача останется невыполненной к концу дня, серия сбросится при смене даты.
Если приложение не работало, APP_BASED replay обрабатывает доступные даты последовательно. За отсутствующие/неподтверждённые данные XP не выдаётся; пропущенный активный день сбрасывает серию.
Историческая активность после длительного отсутствия восстанавливается в пределах доступных UsageEvents и сохранённой конфигурации; приложение не придумывает отсутствующие события.
Календарные даты/полночь берутся из локального часового пояса через Calendar; это не интервалы по 24 часа.

## Модель привычки и дневное состояние

Habit: id, title (1–120 символов), isActive, createdAt, targetDurationMinutes (1…1439), type MANUAL/APP_BASED, optional linkedAppPackage, activatedAt.
Create Habit — отдельный экран: название, HH:MM, необязательное launchable приложение, сохранение. Строка времени не является DB source of truth.
Редактирование существующей привычки сохраняет прежнюю возможность переименования. Для изменения duration/type/app нужно создать новую привычку: текущие дневные сессии не переписываются.
Каждая дата имеет собственный `habit_daily_progress`, ключ `(habitId, localDate)`.
Состояния: READY, IN_PROGRESS, PAUSED, AWAITING_CONFIRMATION, COMPLETED.
Время хранится в миллисекундах; target — числовые минуты. Base + extra = effective target.
Новая дата начинается с нулевого progress, нулевых extras и READY; старые дневные записи и XP не удаляются.

## MANUAL

Start сохраняет elapsedRealtime checkpoint, BOOT_COUNT, wall clock и состояние IN_PROGRESS.
Одновременно идёт одна физическая ручная сессия: запуск другой ставит прежнюю на паузу, сохраняя progress.
При каждом контрольном обновлении delta монотонного времени добавляется к daily progress и sessionMillis, с ограничением effective target.
Экран, UI и процесс не обязаны всё время работать: при возвращении в том же boot elapsedRealtime восстанавливает время, включая deep sleep.
Pause сначала фиксирует delta, затем убирает checkpoint. Resume продолжает накопление без потери прогресса.
При target timer прекращает начисление, состояние AWAITING_CONFIRMATION; автоматического completion нет.
Idle Bob показывает название и требуемое время, YES/NO. На Habits подтверждение встроено в карточку; на других экранах — диалог после завершения заставки. «Позже» ничего не завершает.
YES создаёт completion и награды. NO сохраняет накопленные миллисекунды, добавляет 10 к extraTargetMinutes и запускает продолжение.
Каждое следующее NO добавляет ещё 10; пользовательского максимума extras нет (только технические границы Long).
Полночь закрывает текущую дневную сессию; новая дата требует нового Start. Вчерашнее неподтверждённое выполнение не засчитывается сегодня.
Полная перезагрузка определяется BOOT_COUNT даже если новое uptime больше старого. Сохраняется последний надёжный checkpoint, неизвестный промежуток не добавляется, активная сессия становится PAUSED.
При недоступном boot identifier восстановление консервативно ставит сессию на паузу.
Точного wakeup alarm и отдельного FGS для ручного таймера нет: deadline математически ограничен, обновление UI/confirmation выполняется при следующем доступном цикле/возврате.

## APP_BASED и monitoring

`HabitUsageCalculator` считает только foreground интервалы выбранного package по UsageEvents. Pause, другое приложение, Home, screen off, lock, shutdown прекращают интервал.
STARTUP отбрасывает незакрытый интервал через неизвестное выключение. Неинтерактивное устройство не получает неподтверждённый открытый хвост.
История читается с контекстом предыдущего дня, а начисляемое окно начинается не раньше текущей даты, создания и последней активации привычки.
Несколько foreground-сессий складываются; повторное чтение той же истории использует cumulative maximum, не добавление одного и того же delta.
После деактивации/повторной активации уже накопленная часть сохраняется отдельно от нового окна. После reboot сохраняется надёжная база и начинается окно текущей загрузки.
APP_BASED target автоматически создаёт COMPLETED, HabitCompletion, Habit XP, streak check и release соответствующих блокировок.
Открывать привычку для каждой сессии не нужно: при работающем мониторинге любой foreground linked app учитывается, в том числе до первой блокировки.
«Открыть приложение» запускает linked app и запрашивает запуск прежнего monitoring FGS при наличии разрешений. Без Usage Access открывается системная настройка; удалённое приложение выдаёт понятное сообщение.
`HabitRuntime` вызывается существующим UsageMonitoringService перед BLOCK/PRAISE decision; при открытом UI без сервиса тоже восстанавливает прогресс.
После STOP monitoring фоновые UsageEvents-запросы прекращаются; ручные checkpoints независимы. При возвращении UI история связанных привычек уточняется.
Постоянный фоновый realtime progress требует разрешений и работающего service; OEM может ограничить процесс. Android не гарантирует бесконечную историю UsageEvents, поэтому отсутствующее время не выдумывается.
На Stage 4 используется повторный расчёт по доступному дневному журналу; оптимизация большого количества привычек/событий остаётся дальнейшей работой.

## Конфликты и BLOCK/PRAISE

В выборе Habit app ограниченные packages disabled с «Ограничено». В Apps активные habit packages disabled с «Используется в привычке».
Обе repository transactions повторно проверяют правило при сохранении, включая активацию архивированной привычки. UI не является единственным барьером.
Если legacy/imported conflict уже существует, данные остаются; BLOCK для такого package подавляется, существующий конфликтующий блок отменяется через reconcile. Диагностика есть на Habits, Apps и Debug; выбранный конфликтующий tracked app можно снять с ограничений.

Baseline BlockSession и releaseReason сохранены. «Посмотреть дела» открывает Habits, само по себе блок не снимает.
При ожидании MANUAL confirmation для одной из baseline задач overlay показывает idle Bob и YES/NO; иначе blocking Bob. NO оставляет block active; YES освобождает только посредством новой реальной completion.
APP_BASED completion освобождает eligible active blocks автоматически. Новый отсчёт distracting session по releasedAt сохранён.
Все активные привычки выполнены заранее → прежний PRAISE; нет задач → прежний NONE. Cooldowns, fallback, STOP, Debug overlay timeout сохранены.

## Фирменные поверхности и заставка

Branded cards/buttons/fields используют fill alpha 0.85, непрозрачный purple outline и непрозрачные text/icons. Общие компоненты: SectionCard, MenuCard, BrandButton, BrandOutlinedButton, brandFieldColors.
В BLOCK окно остаётся alpha 1, отдельный full-screen purple scrim = 0.85, Bob/text/buttons = 1, touch-blocking root сохранён.
Opacity slider, preview, ViewModel и preference usage удалены; старый ключ DataStore безопасно игнорируется.
Пункт настроек теперь «Интервалы уведомлений»: сохранены реальные cooldown/PRAISE controls, пустого экрана нет.
Последовательность startup (420+300+300+420+400 ms), full mascot/flip/title/fade и background→foreground replay сохранены. Добавлен только CompositionLocal о видимости cover, чтобы confirmation dialog не перекрывал заставку.
Исходные runtime PNG и современные launcher XML сохранены побайтово. Убраны старые density WEBP overrides, оставшиеся от копирования прошлых ZIP; иконка legacy использует тот же подготовленный happy mascot, что и Stage 3.

## Room 2 → 3

| Объект | Изменение |
|---|---|
| habits | Добавлены targetDurationMinutes=30, type=MANUAL, linkedAppPackage=NULL, activatedAt=0 |
| habit_daily_progress | Новая таблица, составной PK, FK cascade на habits |
| mascot_progress | Новая singleton-запись totalXp, completedHabits, streak, lastStreakRewardDate, evaluatedDate |
| xp_awards | Новый immutable ledger с PK eventKey, индекс awardedAt, без cascade |
| habit_days | Новый календарный snapshot активного набора/завершений/дневной награды |
| Остальные 4 прежние таблицы | Структура и данные сохраняются |

`MIGRATION_2_3` добавлена к прежней `MIGRATION_1_2`; destructive migration отсутствует.
Исторические completions сохраняются. Initial completedHabits соответствует реально существующим completion records; totalXp=0, streak=0 — ретроактивных наград нет.
Старое завершение без timer record показывается как «Выполнено в прежней версии. Время не записывалось», без выдуманных минут.
`checkpointElapsed/bootCount/sessionMillis` относятся к manual session; для APP_BASED bootCount и checkpointWall обозначают текущую загрузку и начало восстанавливаемого usage window. appWindowBaseMillis сохраняет ранее накопленную часть.
Экспортированные JSON схемы 1 и 2 оставлены. JSON 3 должен быть создан Room KSP при первой успешной сборке; он не сфабрикован вручную. Instrumentation test проверяет реальное открытие v2 через миграцию и validation с generated Room metadata v3.

## Проверки и официальные API

Pure Kotlin: thresholds/level bounds, multiplier/ceil/timing, streak policy, manual/reboot/NO, foreground intervals и conflict rules.
Android tests: транзакции, concurrency/dedup, rollback, release, migration, DataStore avatar/nickname и Compose navigation/scrim.
`tools/verify_source.py` исполняет настоящий SQL обеих миграций в SQLite, проверяет сохранение данных, 37 DAO queries, FK/index/uniqueness/rollback и ресурсы. Это не Kotlin compiler и не Android runtime.

Официальная документация:
- https://developer.android.com/training/data-storage/shared/photo-picker
- https://developer.android.com/reference/android/os/SystemClock
- https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT
