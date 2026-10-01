# Stage 2 — изображения и оформление

Все семь исходных изображений открыты до правок кода. Проект был доступен для чтения и записи. Референсы используются как ориентир; их чужие декоративные элементы не копировались.

| Материал | Использование |
| --- | --- |
| 01_ui_reference.png | Фиолетовая палитра, тёмная обводка, скругления, вертикальные карточки |
| 02_blocking_screen_reference.png | Композиция BLOCK и текст; реализованы настоящими Compose-компонентами |
| 03_mascot_happy_reference.png | Выбран для PRAISE; подготовлен отдельный прозрачный drawable |
| 04_mascot_happy_alt_reference.png | Открыт и рассмотрен как альтернативный happy-референс |
| 05_mascot_blocking.png | `app/src/main/res/drawable-nodpi/mascot_blocking.png`, байты сохранены |
| 06_mascot_idle.png | `app/src/main/res/drawable-nodpi/mascot_idle.png`, байты сохранены; также launcher foreground |
| 07_background_main.png | `app/src/main/res/drawable-nodpi/background_main.png`, байты сохранены |

## Happy asset

Итоговый файл: `app/src/main/res/drawable-nodpi/mascot_praise.png`, RGBA, 1254×1254. Проверено наличие полностью прозрачных пикселей и прозрачных внешних областей; лишний gradient-background и нижняя тень отсутствуют. Визуально проверен результат подготовки. Фактический рендер в Android ещё нужно проверить на устройстве.

Использован встроенный imagegen в режиме редактирования (`transparent_background=true`), без CLI/API и без дополнительной сетевой зависимости в приложении. Промпт:

```text
Use case: background-extraction. Asset type: Android app PRAISE mascot transparent drawable. Edit target: the supplied 03_mascot_happy_reference.png. Remove ONLY the grey/green/blue gradient reference background and the oval ground shadow. Preserve this exact purple happy character, closed laughing eyes, open smiling mouth, pink cheeks, green scarf and green hair splash, original pose, proportions, colours, paint texture, dark outline, and the existing floating green droplets/emotion strokes. Do not redraw in another style or redesign any features. Clean cutout edges, genuine alpha transparency, no white/coloured rectangle and no new objects or lettering. Keep the whole character and detached droplets visible with a small transparent margin; square canvas.
```

Подготовленный asset сохраняет характер персонажа и его радостную позу; это imagegen-обработка, а не побайтовая операция над оригиналом. Исходные IDLE/BLOCKING не обрабатывались. Проверяющий скрипт фиксирует SHA-256 всех четырёх используемых PNG, поэтому замена исходников не может пройти незамеченной.

## Launcher

Адаптивная иконка собирается XML: `ic_launcher_background.xml` содержит фирменный фиолетово-синий градиент, `ic_launcher_foreground.xml` — неизменённый idle PNG с пропорциональными отступами. `mipmap-anydpi-v26` используется на API 26+, `mipmap-anydpi` даёт legacy layer-list для API 24–25. Round icon использует тот же маскот, monochrome — его alpha-силуэт. Дефолтные Android Studio WEBP удалены из проекта.

Реальные иконки других приложений не включаются в assets: они загружаются локально из PackageManager на устройстве. Интернет для этого не нужен.

## Проверка оформления

Белый текст на фиолетовой карточке имеет расчётный контраст 8.67:1, на основной кнопке — 6.47:1, на глубокой фиолетовой поверхности BLOCK — 16.70:1. Это расчёт по значениям BrandColors; проверка чтения/масштабирования на устройстве описана в STAGE2_MANUAL_TEST.md.
