# Техническая спецификация Stage 2

Актуализировано для Stage 2 UI Revision, 2026-10-01. База: `Ansa1r/ProjectADHD`, commit `d18fa8bdf499199ff8902b025cbea7b11d29ba06`. Текущие рабочие исходники находятся в `app/src/*/kotlin`; унаследованные шаблоны `src/*/java` сохраняются в копии, но исключены настройкой sourceSets. Все активные исходники используют `com.ansa1r.projectadhd`.

## Компоненты

| Компонент | Реальная ответственность |
| --- | --- |
| AppContainer | Единый Room/AppPreferences, репозитории, monitoring state/controller, OverlayController; application-scope Flow прозрачности |
| HabitRepository | CRUD и текущий день; отметка выполнения + release в одной Room-транзакции |
| BlockRepository | Создать/проверить/закрыть устойчивую блокировку, записать историю атомарно |
| InterventionEngine | Pure Kotlin: NONE/BLOCK/PRAISE без Android и побочных действий |
| BlockCoordinator | Pure Kotlin: проверка baseline ID, даты, времени нового выполнения, причины отмены |
| SessionAllowance | Коррекция начала сессии по сохранённому releasedAt |
| SessionTracker | Сохранённый алгоритм Stage 1: активности, переходы, pause grace, выключение экрана |
| UsageMonitoringService | Единственный FGS; события Android, решение, оркестрация окна/уведомления |
| MonitoringController | Старт/STOP, mutex между polling и очисткой, отмена debug-запросов |
| OverlayController | Не более одного WindowManager-окна на Main, showBlocking/showPraise/hide |
| ExcludedApps | Собственный пакет, системные пакеты, разрешённые PackageManager queries Settings/Home |
| MainActivity/AppNavigation | Single Activity; OPEN_HABITS через cold intent/onNewIntent |
| MascotView/MascotBackdrop | Три отдельных PNG, исходный фон с Crop на всех основных экранах кроме Settings |
| BrandComponents / Color / Theme | Фиолетовые карточки и кнопки, центральная палитра; LocalCalmSurfaces для Settings |
| AppIconLoader / InstalledAppIcon | PackageManager → ограниченный bitmap на IO → Compose, LRU 4 MiB, запасной значок |
| Home/Settings/Permissions/BlockingSettings/Debug ViewModels | StateFlow, жизненный цикл UI, раздельные разрешения/opacity, диагностика и команды |

## Источник задач

`HabitEntity.isActive=true` означает задачу сегодня. `HabitCompletionEntity(habitId, localDate, completedAt)` — отметка, уникальная на пару привычка/локальная дата. `DailyTaskSnapshot` строится одним LEFT JOIN для активных привычек; `DailyTaskSummary(total, completed)` вычисляет incomplete. Отдельных таблиц задач нет. `currentDayFlow` Stage 1 обновляет UI при смене дня.

## Решение движка

Порядок: мониторинг включён → определён foreground → пакет не исключён → совпадающее включённое TrackedApp → total > 0. Затем действующая блокировка этого пакета с незавершёнными задачами даёт BLOCK даже при новой короткой foreground-сессии. Иначе проверяется достигнутый лимит. Если incomplete > 0 — BLOCK; если все выполнены — PRAISE при истекшем praise cooldown. Нулевая длина/недостигнутый лимит дают NONE. Praise cooldown не запрещает BLOCK.

`InterventionPayload` несёт package/name/duration/limit/incomplete; движок не знает о Compose, Room, WindowManager и NotificationManager. Проверка release предшествует решению в сервисе.

## Room 2 и миграция

UI Revision не меняет Room: entities, DAO, repository, schema JSON и MIGRATION_1_2 сохранены побайтово. Описанная ниже миграция относится к исходному Stage 2, а не к текущей UI-ревизии.

Исходные таблицы: `habits`, `habit_completions`, `tracked_apps`, `intervention_events`. Новая `block_sessions` содержит:

| Поле | Назначение |
| --- | --- |
| packageName TEXT PK | Один текущий/последний challenge на приложение |
| appName | Имя приложения для UI/истории |
| startedAt, localDate | Начало и локальная дата challenge |
| triggerSessionDurationMillis, limitMillis | Длительность/лимит на момент BLOCK |
| baselineCompletedCount | Диагностическое число выполненных задач при старте |
| eligibleHabitIds TEXT | Отсортированные ID незавершённых активных привычек через запятую |
| active | Закрытый challenge не участвует в блокировке |
| releasedAt, releaseReason | Устойчивый grant и причина снятия |

CSV состоит только из целочисленных первичных ключей, не SQL/пользовательского текста. Снимок ID не имеет FK: удаление привычки не переписывает baseline задним числом. Каждая новая блокировка заменяет предыдущую закрытую запись этого пакета; долговременный журнал находится в intervention_events.

`Migrations.MIGRATION_1_2` добавляет новую таблицу и две колонки истории: `type TEXT NOT NULL DEFAULT 'LEGACY_NOTIFICATION'`, `detail TEXT NOT NULL DEFAULT ''`. Старые записи сохраняются. Entity `@ColumnInfo(defaultValue=...)` согласованы с ALTER TABLE. `AppContainer` регистрирует миграцию; destructive migration отсутствует.

Схема 1 из GitHub сохранена побайтово. `2.json` отражает новую структуру. Из-за недоступной сборки snapshot подготовлен из схемы 1 и новых entity; identity hash вычислен по алгоритму Room SchemaIdentityKey (алгоритм проверен совпадением с исходным hash схемы 1). Это не результат запущенного здесь KSP. `exportSchema=true` и schemaLocation сохранены; KSP при локальной сборке сверит/обновит export. SQL обеих схем проверен на SQLite, Android-тест дополнительно открывает мигрированную БД через реальный AppDatabase для проверки Room.

## Транзакции и release

При start репозиторий внутри транзакции повторно читает существующую BlockSession, выбранное приложение и сегодняшние привычки. Если все задачи уже выполнены, устаревшее решение не создаёт challenge. Дублирование невозможно по PK; BLOCK_TRIGGERED пишется только при создании.

HabitRepository устанавливает время внутри транзакции. `INSERT OR IGNORE` возвращает ID вставки: повторное нажатие на уже выполненную привычку не создаёт переход. Новый переход проверяется по active + тому же дню + ID из baseline + completedAt > startedAt. В той же транзакции обновляются active/releasedAt/reason и пишется BLOCK_RELEASED. Немедленный undo после настоящего release не восстанавливает закрытый challenge. Выполненная до BLOCK привычка, даже переотмеченная позже, не подходит.

Одно выполнение закрывает все активные challenges, для которых эта привычка была допустима. Завершение Activity не требуется для release; сервис увидит сохранённое состояние на следующем опросе. Окно уже скрыто при открытии ProjectADHD.

`reconcile` отдельно отменяет устаревшие challenges: NEW_DAY, CLOCK_CHANGED, NO_TASKS, TASKS_CHANGED (все baseline-привычки удалены/неактивны), TRACKING_DISABLED. STOP использует MONITORING_STOPPED, Debug — DEBUG_CLEAR. Эти причины не выдаются за выполнение задачи.

После release `SessionAllowance` вычисляет начало как max(начало текущей UsageEvents-сессии, releasedAt). Если пользователь вернулся позже, новый foreground timestamp имеет приоритет. Это сохраняет полный интервал и после перезапуска процесса. Будущий releasedAt при переводе часов назад не удерживает длительность на нуле бесконечно.

## Сервис и жизненный цикл

Один `UsageMonitoringService`, `START_NOT_STICKY`, без BOOT receiver. Старт разрешён пользователем из UI с Usage Access и рабочими уведомлениями/каналами. Сохраняется официальный FGS `specialUse` с постоянным уведомлением и stop action.

Цикл: coroutine + delay 1 000 ms после обработки. Сам queryEvents выполняется на IO; Room suspend DAO использует свой executor. WindowManager и состояние Compose обслуживаются на Main. Stage 1 overlap 10 секунд, deduplication и обнаружение скачка системных часов сохранены. На неактивном/заблокированном экране SessionTracker сбрасывается, окно скрывается; persistent challenge сохраняется.

`MonitoringController.gate` сериализует poll и explicit STOP/Debug clear. stopRequested ставится до снятия окна; новые side effects подавляются. Persistent active rows закрываются под тем же mutex; существующий FGS сохраняется до завершения этой транзакции, затем сервис останавливается. На неожиданном уничтожении сервиса overlay удаляется, Compose lifecycle завершается, polling отменяется; challenge сохраняется для следующего ручного старта.

## Окно

Controller повторно проверяет permission перед show. API 26+: TYPE_APPLICATION_OVERLAY. API 30+: display/window context. ComposeView получает отдельные LifecycleOwner, SavedStateRegistryOwner и ViewModelStoreOwner. Hide удаляет view, отменяет auto-dismiss, disposeComposition и уничтожает owners; повторный show того же BLOCK обновляет payload, не создаёт второе окно.

BLOCK: MATCH_PARENT по обеим осям, FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT, Window alpha=1. Прозрачный touch-consuming Compose root содержит отдельный фиолетовый scrim и непрозрачные blocking PNG/текст/кнопку. Scrim alpha=0.30–0.90, default 0.65; фон 07 не используется. Контент прокручивается на небольших экранах/при большом шрифте. Системные панели не скрываются. Button использует OPEN_HABITS с CLEAR_TOP/SINGLE_TOP и NEW_TASK, не изменяя BlockSession.

PRAISE: WRAP_CONTENT по высоте, FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCHABLE, alpha <= системного maximum obscuring opacity (на Android 12+ читается из InputManager). Автоскрытие 4 секунды. Это ограничение необходимо, чтобы Android не блокировал касания под непрозрачным нетактильным overlay. Окно скрывается раньше при смене приложения.

`MainActivity.onStart` принудительно скрывает окно и подавляет overlay, пока UI ProjectADHD виден, в том числе в multi-window. ExcludedApps дополнительно исключает собственный пакет, android/SystemUI/PermissionController, доступные HOME и Settings handlers. Никакого Accessibility/hidden API/kill process нет.

## Fallback, cooldown, история

Недоступный API, отсутствие permission и RuntimeException при добавлении окна возвращают Failed с причиной в OverlaySnapshot. Сервис сохраняет обычное Stage 1 уведомление с типом FALLBACK_NOTIFICATION; `detail` содержит BLOCK/PRAISE и причину. Для BLOCK persistent challenge остаётся активным: выдача overlay permission позволяет показать его при следующем входе. Android 7 также использует fallback.

DataStore сохраняет отдельно `last_praise_at`/`praise_cooldown_minutes` и прежние `last_intervention_at`/`cooldown_minutes` для fallback. Оба интервала по умолчанию 30 минут, диапазон Settings 1–180. Timestamp резервируется до внешнего действия против спама при process death; редкий crash между записью и показом может пропустить одно вмешательство. Debug показывает именно последнюю попытку praise. Suppressed для собственного/другого foreground не отправляет fallback.

История: LEGACY_NOTIFICATION, BLOCK_TRIGGERED, BLOCK_RELEASED, PRAISE_SHOWN, FALLBACK_NOTIFICATION. Старые поля и сортировка 200 последних записей сохранены; clearing history не меняет привычки, active blocks или cooldown. Последний completion-release берётся из журнала, поэтому очистка истории очищает и это диагностическое значение.

## Debug и UI

Home показывает idle-маскота, today progress, active blocked app names, monitoring, компактное предупреждение о разрешениях и переходы к Habits/Apps/Stats. В нижней панели ровно 5 иконок с contentDescription: Home/Habits/Apps/Stats/Profile. Settings открывается шестерёнкой Home и содержит отдельные Permissions, Blocking Screen, Theme, Privacy и Developer. Последний пункт ведёт в Debug и существует только в debug build. Profile, Theme и Privacy — реальные локальные заглушки. Интервалы praise/fallback перенесены в Blocking Screen, аварийный STOP остаётся в корне Settings. Debug показывает пакеты, session/limit, summary, decision, все active sessions с baseline/startedAt, last unlock/praise, permission, registered window и последнюю ошибку.

Тестовые overlay не создают challenges/events и не меняют cooldown. Чтобы никогда не закрывать собственную Activity, кнопка вооружает тест на 30 секунд: пользователь открывает выбранное приложение; service показывает тест без ожидания лимита. BLOCK preview исчезает через 10 секунд, PRAISE через 4. Настоящее BLOCK имеет приоритет. Debug недоступен в release и не подменяет production decision.

## Оформление и assets

`AppNavigation` оборачивает NavHost в `MascotBackdrop(enabled = !SettingsRoutes.isSettings(route))`. Фон 07 охватывает Home, Habits, Apps, Stats, Profile и Debug; Settings и все его подразделы используют однотонный Background и `LocalCalmSurfaces=true`. PRAISE сохраняет отдельный фон в WindowManager. BLOCK содержит только прозрачную подложку, регулируемый scrim и непрозрачный контент. Фон 07 рисуется с ContentScale.Crop и тёмным слоем alpha 0.3.

`SectionCard`, `MenuCard`, `BrandButton` задают пурпурный fill, тёмно-фиолетовый border, скругления 20–24 dp, белый текст и небольшую elevation. Settings получает тихую тёмную поверхность. Цвета только в BrandColors, launcher — в Android `values/colors.xml`. BLOCK компонует настоящие Compose Text с длительностью в минутах, числом незавершённых задач, названием приложения и CTA «Посмотреть дела». Кнопка не меняет persistent challenge; длинный экран прокручивается.

`MascotView` использует отдельный `mascot_praise.png`, подготовленный из happy-референса 03 встроенным imagegen удалением фона; alpha проверен. IDLE и BLOCKING — исходные assets 06/05. Подробное происхождение и prompt — ASSETS.md. Нет emoji, встроенного в PNG текста или сетевой загрузки assets. Startup использует отдельный прозрачный happy-asset из reference 08 и gradient из Android colors, не фон 07. Compose StartupHost: hold 420 мс → flip 300+300 мс → title hold 420 мс → fade 400 мс. Системный SplashScreen стилизован тем же asset, его exit fade — 120 мс без дополнительного hold. Отдельная SplashActivity не создаётся; CTA OPEN_HABITS пропускает анимацию. Полный сценарий и ограничения времени холодного старта — в STAGE2_UI_REVISION_REPORT.md.

`AppIconLoader` существует в AppContainer и обслуживает только UI. В `Dispatchers.IO` он получает `getApplicationIcon(packageName)` и преобразует Drawable (включая adaptive/vector) через `toBitmap`; размер ограничен 32–192 px, кэш Bitmap — LruCache 4 MiB. Ключ содержит пакет, размер и revision списка. `produceState` отменяется при уходе элемента, учитывает новую ревизию/доступность; при NameNotFoundException/RuntimeException отображается встроенный нейтральный значок. Domain InstalledApp и Room Entity остаются без Drawable/Bitmap.

Launcher использует исходный idle PNG: adaptive XML раздельно задаёт фоновый градиент и foreground с пропорциональными отступами 16.6667%; прозрачные края самого PNG добавляют запас при маскировании. Monochrome использует alpha-силуэт того же foreground. На API 24–25 fallback — layer-list с круговым фоном и PNG. Дефолтные template WEBP удалены; новые anydpi-ресурсы также имеют приоритет при распаковке поверх старой копии. Preview разных OEM-масок и themed icon нужно проверить локально.

## Ограничения и источники

Успешный addView означает зарегистрированное окно, не доказательство того, что OEM показал его над защищённой Activity. Приложения могут скрывать чужие overlay без ошибки addView; надежного публичного API обнаружить каждый такой случай нет. Не обещается fallback для невыявляемого скрытия ОС. Выявляемые ошибки попадают в Debug и fallback. Background Activity launch также может быть заблокирован OEM; ошибка отображается в Debug, пользователь может открыть ProjectADHD через launcher.

Латентность — период опроса плюс задержка UsageEvents, не гарантия мгновенного переключения. Основной дисплей, обычный full-screen app — поддерживаемый сценарий; multi-window/PiP/дополнительные дисплеи и OEM требуют ручной проверки. API 24–25: уведомления. После reboot/process death нет автоматического старта. Приватные данные не передаются в сеть.

Проверены официальные источники:

- [WindowManager.LayoutParams: типы окон, focus/touch и opacity](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
- [Settings: canDrawOverlays и permission intent](https://developer.android.com/reference/android/provider/Settings)
- [ComposeView и view-tree owners](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ComposeView)
- [Room transactions](https://developer.android.com/reference/kotlin/androidx/room/package-summary)
- [Background activity launch](https://developer.android.com/guide/components/activities/secure-bal)
- [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Room SchemaIdentityKey](https://github.com/androidx/androidx/blob/d503771975385615d465665262c8e122f11c84c3/room/room-compiler/src/main/kotlin/androidx/room/vo/SchemaIdentityKey.kt)

- [Adaptive icon layers and safe zone](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).
- [PackageManager.getApplicationIcon](https://developer.android.com/reference/android/content/pm/PackageManager#getApplicationIcon(java.lang.String)).

## Live opacity

`BlockingOpacity` нормализует целые значения в 30–90 с шагом 5; отсутствующий ключ даёт 65. `AppPreferences` хранит Int `blocking_overlay_opacity_percent` в прежнем Preferences DataStore `settings`. `BlockingSettingsViewModel` обновляет preview синхронно и сохраняет значение в applicationScope; номера запросов не дают устаревшему ответу откатить новый slider. Уже начатая запись завершается после ухода со страницы.

`AppContainer.applicationScope` непрерывно собирает `blockingOverlayOpacity.distinctUntilChanged()` на Main и вызывает setter существующего `OverlayController`. Compose-observable поле обновляет показанный BLOCK без remove/addView, нового таймера или новой BlockSession. На следующем poll сервис также передаёт прочитанное settings значение перед показом. Production и debug/test BLOCK используют один `BlockingContent`; preview использует тот же scrim без WindowManager и без записей Room.
