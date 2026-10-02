# Final Pre-alpha — техническое описание

Источник: GitHub `Ansa1r/ProjectADHD`, commit `e8ff81b15568d6bf79bd78fad7bffd87947a4654` от 2026-10-01 (`v0.32 pre-alfa`); 226 файлов сверены по Git blob SHA. Исходная версия в Gradle — 0.4.0-pre-alpha, code 6. Работа выполнена локально без commit/push/PR. Все 13 новых reference PNG совпадают с уже включёнными `docs/assets/stage4/`; runtime-изображения сохранены.

## Startup lifecycle

`ForegroundEntryTracker` принадлежит AppContainer, но управляется только MainActivity. Сервис не отмечает UI как видимый. Первый UI-вход в процессе холодный. `onStop` вне configuration change фиксирует wall timestamp и `SystemClock.elapsedRealtime()`. Возврат с разницей <600000 мс пропускает анимацию, при ≥600000 мс создаёт новую последовательность. Только монотонные часы участвуют в сравнении: перевод системной даты не меняет правило.

Фактическое уничтожение UI (не поворот) снова помечает следующий вход холодным, даже если FGS оставил процесс живым. После смерти всего процесса tracker создаётся заново. Поворот и обычная навигация не создают нового события. Навигация остаётся в существующем NavController и восстанавливает state там, где его сохраняет Android.

`last_ui_background_at` в DataStore — диагностический wall timestamp; решение не зависит от задержки его записи. В Debug `shouldShowStartupAnimation` показывает последнее решение при входе, а не обещание повторного показа прямо сейчас. `timeSinceBackground` — время с последнего ухода UI по монотонным часам, внутри текущего процесса.

Визуальная последовательность StartupHost сохранена: 1840 мс, без нового дизайна/маскот-анимационной системы. Coach скрыт, пока показывается StartupHost.

## XP текущего уровня

Domain `MascotXp(currentLevel, currentLevelXp, lifetimeXp)`. Room хранит эти значения явно. `MascotProgression.requiredXp(L)` — XP, которые нужно заработать на уровне L:

| L | 1 | 2 | 3 | 10 | 11 | 12 | 20 | 21 |
|---|---|---|---|---|---|---|---|---|
| required XP | 15 | 30 | 45 | 150 | 170 | 190 | 350 | 375 |

Для q=floor(L/10), r=L mod 10: `150q + 25q(q−1) + r(15+5q)`. Сумма требований служит вспомогательным способом переноса нескольких уровней; награждение начинается с сохранённых `currentLevel + currentLevelXp`, **не** определяет уровень напрямую по lifetime XP.

`advance` эквивалентен последовательному вычитанию требований из XP уровня. Для очень больших наград используется точная BigInteger-сумма и бинарный поиск, чтобы не выполнять миллионы шагов и не переполнять промежуточные вычисления. Lifetime counter насыщается на Long.MAX_VALUE; поддерживаемый тип уровня Int. Это числовые пределы представления, а не игровые ограничения.

Примеры: `1:14/15 +1 → 2:0/30`; `2:28/30 +5 → 3:3/45`; `1:0 +830 → 11:5/170`.

Награда Habit `ceil(5 × multiplier)`, Streak `ceil(10 × multiplier)`, multiplier `1.5^floor((L−1)/10)`. Уровень берётся до каждой конкретной награды; если Habit повышает L10→L11, последующая Streak использует 1.5. Существующие уникальные event keys, транзакционная связь completion/XP/unlock и правила streak сохранены.

Mascot UI показывает `currentLevelXp / requiredXp`, соответствующий progress bar и отдельно lifetime XP. Верхний title «Маскот» удалён, имя Боба и информация остались. Для невысокого экрана hero располагается компактным рядом.

## Room migration 3→4

Добавлены две NOT NULL колонки с defaults: `currentLevel INTEGER DEFAULT 1`, `currentLevelXp INTEGER DEFAULT 0`. Старый `totalXp` остаётся на месте и отображается в Kotlin через `@ColumnInfo(name="totalXp") val lifetimeXp`.

Для каждой существующей строки migration перераспределяет totalXp по сумме **новых per-level requirements**. Например 15→L2/0, 45→L3/0, 48→L3/3, 150→L5/0, 825→L11/0. Число уровня может стать меньше, чем в ошибочной старой модели; фактически заработанный XP не теряется. Награды не воспроизводятся и не пересчитываются по новому multiplier. Старые `levelBefore` в истории остаются историческими значениями.

Счётчик выполнений, streak, даты, привычки, накопленное время, app limits, блокировки и события не сбрасываются. Нет destructive migration. Схемы 1, 2 и реальная экспортированная 3 включены. `4.json` должен быть сгенерирован KSP при первой успешной сборке; поддельный identityHash не создавался. Instrumentation migration test открывает реальную v3 базу через Room v4 и проверяет повторное открытие.

## Onboarding state machine

Основные этапы: `WELCOME → HOME → HABITS → APPS → STATS → MASCOT → SETUP_APPS → SETUP_HABIT → SETUP_PERMISSIONS → FINAL → COMPLETED`.

Подэтапы редакторов сохраняются отдельно: `SELECT_APPS`, `CREATE_HABIT`, `GRANT_PERMISSIONS`. Они относятся к тому же onboardingStep. Profile и Settings не входят в информационную экскурсию; Permissions открывается только для реальной настройки.

WELCOME на настоящем Home содержит приветствие и представление Боба. Нажатие «Продолжить» только тогда начинает экскурсию. UI использует существующие NavHost destinations, без копий экранов, тестовых приложений и искусственной статистики.

Переходы выполняются с expected-step проверкой внутри одного DataStore edit. Двойной тап или старый callback не пропускают этап. App save и Habit save сохраняют реальные Room-данные, затем фиксируют следующий этап. Пустой выбор приложений в SELECT_APPS не разрешён. При сбое записи продолжения уже сохранённая Habit не создаётся повторно кнопкой retry. Завершённые данные доступны из setup через «Оставить сохранённый выбор» / «Продолжить с сохранённой привычкой»; это также путь восстановления после прерывания между Room и DataStore.

Back в редакторе лимитов возвращает в выбор приложений с текущим draft. Back из других setup-редакторов возвращает к соответствующей панели Боба. В информационной части Back не закрывает обязательный этап. Bottom navigation и случайные действия во время coach заблокированы; настоящие экраны видны. Несохранённые drafts после смерти процесса могут потребовать повторного ввода; сохранённые Room-данные остаются. Команды записи выполняются в applicationScope, чтобы уход со страницы не оборвал явный Save.

PermissionsScreen проверяет реальный статус на ON_RESUME и после результата notification permission launcher. Для setup требуется canMonitor (Usage Access + notification authorization + оба канала) и Overlay на поддерживаемых API ≥26. На API 24–25, где действующая overlay-архитектура использует fallback, Overlay не создаёт непроходимый этап. Отказ оставляет пользователя на Permissions с объяснением функции; открытие Android Settings не считается разрешением.

После успешной проверки открывается Home с финальным поздравлением. До нажатия «Готово» monitoring выключен. На этой кнопке снова проверяются наличие активного ограниченного приложения, активной Habit и permissions. Утрата условия возвращает в соответствующий setup. Затем один DataStore edit устанавливает completed=true, step=COMPLETED, monitoringEnabled=true; только после commit вызывается ensure сервиса.

## Persisted preferences

| Key | Назначение / default |
|---|---|
| `onboarding_completed` | Завершённость, false |
| `onboarding_step` | Основной или editor этап, WELCOME |
| `monitoring_enabled` | Выбор пользователя, false до completed |
| `last_ui_background_at` | Wall timestamp ухода UI, отсутствует до первого сохранения |

Отдельная destructive/очищающая DataStore migration не нужна: добавлены новые ключи в прежний `settings` store. Старые keys остаются. На обновлении с Stage4 стартует новый guided onboarding, существующие данные предлагается использовать. Completed=true всегда означает COMPLETED; неизвестный/некорректный незавершённый step восстанавливается в WELCOME. Нет автоматического reset completed.

## BobCoachOverlay

Общие `BobCoachPanel` и `BobCoachOverlay` принимают текст, primary, optional secondary, TOP/BOTTOM, enabled, допустимость dismiss и настроение. TOP — Боб слева, текст справа; BOTTOM — текст слева, Боб справа. WELCOME расположен сверху, освобождая нижнюю навигацию; остальные объяснения верхнего содержимого — снизу. На шаге HOME настоящий список прокручивается к карточке прогресса.

Fill только purple background = 0.50; текст и изображение непрозрачные, кнопки контрастные с touch target ≥48 dp. Панель учитывает safeDrawing/IME, ограничена 48% доступной высоты и прокручивается при нехватке места. Подложка блокирует случайные taps, сама панель не передаёт touches вниз. Семантика paneTitle/liveRegion и читаемые labels доступны assistive technology; underlying screen semantics скрыты во время coach.

Обычные компоненты берут 0.70 из BrandOpacity, blocking scrim — 0.85. Нет whole-composable alpha для этих поверхностей. Settings сохраняет plain dark background. Stop confirmation переиспользует BobCoachPanel в Dialog с «Остановить» / «Отмена».

## Monitoring

Единственный production start проходит через `MonitoringController.ensureIfEnabled()` или `enable()`. Без completed они не запускают сервис. В MainActivity.onResume ensure читает сохранённую политику и запускает отсутствующий сервис, сохраняя ручной OFF. Open useful app также вызывает ensure без изменения preference.

Сам сервис перед первым опросом и при poll проверяет сохранённые флаги. Даже явный старый service Intent до финального шага не запускает чтение UsageEvents или вмешательства. Постоянный FGS notification создаётся согласно существующей архитектуре; START_NOT_STICKY и отсутствие boot receiver сохранены.

Stop сначала фиксирует preference=false; если это не удалось, сервис не останавливается молча. После сохранения ставится stopRequested, закрываются overlays/debug tests, под gate завершаются BlockSession с MONITORING_STOPPED, сервис останавливается. История и пользовательские записи остаются. Команды сериализованы; ожидание уничтожения сервиса ограничено пятью секундами. Cancel не вызывает ни одного изменения. Ошибки запуска/permissions показываются в Settings; включённый preference сохраняется для следующего foreground ensure.

Notification Stop теперь открывает ту же Settings/Bob confirmation. Старый service Stop Intent тоже запрашивает UI-подтверждение. Intent counters и обработанные события сохраняются при configuration recreation; обычный возврат в Settings не повторяет старый запрос.

Habit engine, manual timer и foreground catch-up остаются самостоятельными: Stop выключает FGS-мониторинг лимитов и блокировки, но не стирает прогресс и не отменяет работающую ручную привычку. Background recovery зависит от Android; при следующем foreground выполняется ensure, если preference включён.

## Debug и границы проверки

Добавлены monitoringEnabled/serviceRunning, completed/step, background timestamp/elapsed, startup decision, current level/local XP/required XP/lifetime/multiplier. В существующем Debug доступны +15/+33 тестового XP: BuildConfig.DEBUG guard, отдельный DEBUG ledger event, без фальшивого выполнения Habit. Release не показывает эти действия.

Не добавлены GIF/Lottie/AI/cloud/shop, новые dangerous permissions, AccessibilityService, root или новая архитектура Habit Engine. Проверки сборки и устройства перечислены отдельно в отчёте; исходный код не считается прошедшей Android-приёмкой.
