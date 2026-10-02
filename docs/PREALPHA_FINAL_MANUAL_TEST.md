# Final Pre-alpha — ручная приёмка

Статус всех сценариев ниже: **не выполнялись на Android в среде подготовки**. Не отмечать их пройденными по наличию кода. Записать устройство, API, ориентацию, font scale, версию APK и фактический результат. Использовать тестовый профиль/устройство; Clear app data удаляет локальные данные.

## A–O

| ID | Действия | Ожидаемый результат |
|---|---|---|
| A — Cold start | После завершённого setup убить процесс и открыть приложение. Отдельно закрыть UI при работающем FGS. | Один показ существующей startup animation; сервис не подменяет жизненный цикл UI. |
| B — Short Overview | Открыть Stats, выйти Home/Overview на 30 с и вернуться. | Без startup; прежний экран. Повторить при работающем FGS и при OFF. |
| C — ≥10 минут | Уйти в фон на 9:59, вернуться; повторить для 10:00 и 15:00. | 9:59 — skip; 10:00/15:00 — show. Перевод wall clock не влияет на длительность. |
| D — Mascot | Открыть Mascot, переименовать Боба, перезапустить. | Нет лишнего верхнего title; имя, уровень, XP, streak, history и customization сохранены. |
| E — XP | На отдельной свежей базе завершить onboarding, открыть Settings → Для разработчика → Отладка. +15, затем +33. | L1 0/15 → L2 0/30 → L3 3/45, lifetime 48. DEBUG history отличается от Habit/Streak. |
| F — Greeting | Чистая установка/данные → launch. | Startup → настоящий Home → «Привет! Я Боб»; только после Continue начинается экскурсия. FGS не работает. |
| G — Tour | Пройти Home → Habits → Apps → Stats → Mascot. Нажать доступный фон и Back во время coach. | Реальные экраны, без Profile/Settings tour и выдуманной статистики. Панель не закрывает верхний target; accidental taps не активируют его. |
| H — Apps | «Выбрать приложения» → попробовать сохранить пустой выбор → выбрать ≥1 app → индивидуальные лимиты → Save. | Пустой setup не завершается. После настоящего Save приложения и лимиты в Room, следующий шаг — Habit. |
| I — Habit | Создать MANUAL 00:01; отдельно повторить на чистых данных с APP_BASED. | Реальная Habit сохранена. Перезапуск не удаляет её. Conflicts с ограниченными apps не разрешены. |
| J — Permissions | Открыть Usage Settings без выдачи и вернуться; затем выдать Usage, Overlay, Notifications/каналы по очереди. | Каждый статус перечитывается; при отказе нет продвижения. Возврат <10 минут не показывает startup. API 24–25 использует действующий fallback. |
| K — Final | После всех условий вернуться на Home; до кнопки проверить отсутствие FGS; нажать «Готово». | Финальный Боб виден до старта. После кнопки completed=true, monitoringEnabled=true, FGS запускается. Повторный tap не дублирует сервис. |
| L — Restart | После K закрыть UI; затем убить процесс/остановить сервис через инструменты Android; открыть заново. | Greeting/tour не повторяются; enabled=true приводит к ensure. При запрете permissions видимая ошибка, без crash. |
| M — Stop | Settings внизу → Остановить → Отмена; затем повторить и подтвердить. Повторить Stop из notification. | Отмена сохраняет preference/сервис. Подтверждение сохраняет OFF, закрывает overlay и завершает активную BlockSession, оставляет пользовательские данные. |
| N — Persistent Stop | После Stop перезапустить UI и процесс, открыть useful app из Habit. | monitoringEnabled=false остаётся false; FGS сам не запускается. Полезная привычка и её время не удаляются. |
| O — Reenable | Settings → Включить мониторинг; при нужных разрешениях вернуться в отслеживаемое приложение. | preference=true, сервис стартует/ensure; Settings сразу отражает состояние. Home не содержит Start/Stop. |

## Дополнительные обязательные проверки

- [ ] Внутренние переходы всех вкладок, Profile и Settings не запускают startup.
- [ ] Поворот/смена размера окна на startup, каждом coach, редакторе и финале не сбрасывает сохранённый шаг.
- [ ] Прервать процесс на WELCOME, SETUP_APPS, SELECT_APPS/limits, CREATE_HABIT, GRANT_PERMISSIONS и FINAL. Вернуться на соответствующий этап после cold startup. Подтвердить отсутствие повтора completed greeting.
- [ ] Back из лимитов сохраняет draft до ухода из flow; Back из setup редактора возвращает к Bob intro. Сохранённые apps/Habit и реальные permissions не теряются.
- [ ] Прервать сразу после Save. Если запись Room уже есть, «Оставить сохранённый выбор»/«Продолжить с сохранённой привычкой» позволяет продолжить без дубликата.
- [ ] Отозвать permission на FINAL до кнопки: вернуться в permissions, не стартовать FGS. Аналогично удалить/деактивировать необходимые данные на тестовой базе.
- [ ] Портрет/ландшафт, экран 320 dp шириной, font scale 1.0 и 1.5/2.0, TalkBack. Кнопки достижимы, текст не обрезан без возможности прокрутки, target виден. TOP Bob слева, BOTTOM справа.
- [ ] Fill обычных purple buttons/cards/fields/icons containers/navigation indicator = 0.70; outline/text/icons = 1.00. Нет общей alpha 0.70 на компоненте.
- [ ] Blocking scrim = 0.85, overlay controls непрозрачны. Coach fill = 0.50; Settings background plain dark.
- [ ] Cancel Stop, вращение экрана с confirmation, повторный Stop из notification и обычный повторный вход Settings не приводят к случайной остановке/повтору старого диалога.
- [ ] При failed start показать ошибку в Settings; OFF после ручной остановки не сбрасывается обычным ON_RESUME.

## Регрессии Stage4

- [ ] MANUAL: Start/Pause/Resume, сумма нескольких сессий, переход AWAITING_CONFIRMATION, «Нет» дважды даёт +20 мин, «Да» завершает один раз.
- [ ] Midnight/process reopen/reboot не приписывают неизвестное время; прежние checkpoint semantics сохранены.
- [ ] APP_BASED: несколько foreground-сессий суммируются, другие приложения/lock не прибавляют время, цель завершает автоматически.
- [ ] Повторный опрос/повторное подтверждение не дублируют Habit XP или Streak XP. Day streak/пропущенный день/пустой день работают по Stage4.
- [ ] Пересечение limited app и useful app запрещено; старые конфликтные строки не удаляются и не создают ошибочный BLOCK.
- [ ] BLOCK/PRAISE/fallback, Bob manual confirmation на блокировке, доступ к полезной Habit app и fresh session после unlock.
- [ ] Profile avatar после потери исходного URI/перезапуска, nickname, имя Боба, настоящие статистика и история.
- [ ] Debug содержит все новые поля, +15/+33 только в Debug; release не показывает инструменты разработчика.
- [ ] Открыть реальную Room 3 базу с XP 48, прогрессом, streak, historical awards и всеми Stage4 данными. После миграции Room 4: lifetime48/L3/local3, остальные данные прежние; повторное открытие не конвертирует второй раз.
- [ ] Полные legacy migration 1→4 и 2→4; DataStore profile/avatar/cooldown не очищаются.

## Gate перехода в Alpha

- [ ] `./gradlew testDebugUnitTest` — успешно, приложить вывод.
- [ ] `./gradlew assembleDebug` — успешно, приложить вывод и APK.
- [ ] `./gradlew connectedDebugAndroidTest` — успешно на тестовом устройстве; проверить реальные Room migrations и Compose сценарии.
- [ ] Все обязательные A–O, UI и Stage4 регрессии выше выполнены, замечания исправлены.
- [ ] Сохранить KSP-exported `app/schemas/com.ansa1r.projectadhd.data.local.AppDatabase/4.json`.
- [ ] Только теперь присвоить `0.5.0-alpha` и увеличить code относительно установленного (если 7 уже установлен, минимум 8); обновить README/отчёт и собрать заново.

До прохождения gate версия остаётся Pre-alpha. Не отмечать BUILD SUCCESSFUL по Python source checks.
