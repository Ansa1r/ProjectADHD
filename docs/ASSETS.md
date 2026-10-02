# Stage 2 UI Revision — изображения

Все восемь файлов из приложенного архива распакованы и открыты до изменения проекта. Доступ к актуальным исходникам и запись в рабочую копию проверены.

| Материал | Использование в ревизии |
| --- | --- |
| 01_ui_reference.png | Формы, фиолетовая палитра, тёмная обводка; посторонние декоративные элементы не используются |
| 02_blocking_screen_reference.png | Композиция BLOCK; без нарисованного телефона и непрозрачного фона |
| 03_mascot_happy_reference.png | Источник существующего PRAISE asset, сохранённого из Stage 2 |
| 04_mascot_happy_alt_reference.png | Открытый альтернативный референс; не launcher |
| 05_mascot_blocking.png | Исходный BLOCKING asset, сохранён побайтово |
| 06_mascot_idle.png | Исходный IDLE asset, сохранён побайтово; больше не используется в launcher |
| 07_background_main.png | Home, Habits, Apps, Stats, Profile, Debug и PRAISE; не Settings, startup или BLOCK |
| 08_app_icon_reference.png | Единственный источник happy-маскота и зелёно-голубого оформления launcher/startup; исходник включён в docs/assets/ |

## Подготовленные happy assets

`app/src/main/res/drawable-nodpi/mascot_praise.png` — прежний Stage 2 asset из 03, не изменён текущей ревизией. Исходная операция imagegen: удалить только фон и овальную нижнюю тень, сохранив персонажа, закрытые глаза, улыбку, зелёный шарф, капли, позу и рисованную фактуру.

`app/src/main/res/drawable-nodpi/mascot_startup.png` — новый asset именно из приложенного **08**. Режим встроенного imagegen: редактирование / background-extraction; `transparent_background=true`; локальный исходник `ui_revision_images/08_app_icon_reference.png`. Инструкция обработки: убрать только серо-зелёно-голубой фон и овальную тень; сохранить счастливого фиолетового персонажа, его закрытые смеющиеся глаза, улыбку, щёки, зелёный шарф и брызги, исходную позу, пропорции, цвета, контур и фактуру; ничего не добавлять и не менять стиль. Результат — квадратный RGBA PNG 1254×1254 с настоящей прозрачностью, прозрачными углами и без фонового прямоугольника. Результат открыт и визуально проверен. Это обработка imagegen, а не утверждение о побайтовой неизменности вырезанного персонажа.

Вызовов генерации изображений и сетевых зависимостей для assets в Android-приложении нет. `tools/verify_source.py` закрепляет SHA-256 всех пяти runtime PNG и исходника 08. IDLE, BLOCKING, PRAISE и основной фон совпадают с исходным проектом. Текст и UI рисуются Compose, не встроены в изображения.

## Launcher и безопасная зона

Adaptive background — XML-градиент `startup_top=#D2E1DE`, `startup_green=#97EA93` (позиция 0.48), `startup_blue=#54A7D2`, сверху вниз. Foreground — только `mascot_startup`, отступы по 28% на слое 108 dp. `mipmap-anydpi-v26` содержит adaptive/round варианты; monochrome использует alpha-силуэт того же happy asset. API 24–25 получает legacy XML с фоном 48 dp и маскотом 36 dp. Десять унаследованных launcher WEBP удалены, чтобы Android не выбрал старый значок по density.

Проверка геометрии учитывает **все пиксели с alpha > 0**, включая почти невидимые края после обработки. Максимальный радиус после размещения: около 31.23 dp внутри центрального круга радиусом 33 dp для adaptive icon; около 23.66 dp внутри legacy-круга радиусом 24 dp. Это статический расчёт; circle, squircle, rounded-square маски и themed icon должны пройти визуальную проверку на launcher устройства. Отступы выбраны с запасом, чтобы не обрезать ни персонажа, ни капли.

## Startup и BLOCK

Startup использует тот же happy PNG поверх полноэкранного Compose-градиента с общими XML-цветами. В App Selection/Profile Fix Compose напрямую читает полный PNG в области 256 dp с ContentScale.Fit, без круглого clip. Системный SplashScreen использует `ic_splash_empty`: прозрачный vector, который не показывает маскот под системной маской. Системный фон однотонный зелёный; exit fade 80 мс раскрывает полноэкранный градиент. Файл PNG не перерисовывался в этой ревизии. Launcher и его Android masks отделены от собственного startup.

BLOCK использует исходный `mascot_blocking.png`. На нём нет фона 07: отдельный scrim `#461062` получает настраиваемую alpha 0.30–0.90; персонаж, белый текст с тенью и фиолетовая кнопка остаются непрозрачными. Preview рисует условное приложение Compose-компонентами и тот же scrim. На ярком реальном контенте читабельность, особенно при 30%, проверяется вручную.

Иконки установленных приложений по-прежнему загружаются из PackageManager через `AppIconLoader` и `InstalledAppIcon`. Эти компоненты не изменены; Drawable/Bitmap в Room не сохраняются.

Официальные основания: [adaptive icon и safe zone](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [SplashScreen](https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen), [миграция splash](https://developer.android.com/develop/ui/views/launch/splash-screen/migrate), [core-splashscreen 1.2.0](https://developer.android.com/jetpack/androidx/releases/core).


## Stage 4

Исходные 01–13 из нового архива находятся в `docs/assets/stage4`. Все изображения просмотрены перед работой. Runtime background/idle/blocking/praise/startup PNG сохранены побайтово. Новая векторная `ic_nav_mascot.xml` — навигационная пиктограмма. UserAvatar использует выбранную локальную фотографию или `ic_nav_profile`, не PNG Боба.


## Final Pre-alpha references

Все 13 PNG из ProjectADHD_Stage4_Finalization_Assets совпали побайтово с `docs/assets/stage4/`. Они распакованы и просмотрены. Runtime idle/blocking/praise/startup/background и launcher XML сохранены. Удалены старые launcher WEBP, возвращённые при наложении прежних ZIP; исходный текущий launcher использует существующие XML/подготовленные изображения.
