# Dungeon HUD (Forge 1.20.1)

Клиентский мод: HUD в стиле Minecraft Dungeons.

Текстуры (рамки, шары, слоты) лежат в `src/main/resources/assets/dungeonhud/textures/gui`
и нарисованы с нуля, никаких ассетов Mojang в проекте нет. Их можно заменить своими
с теми же именами и размерами:
orb_frame.png 64x64, slot_frame.png 44x22, slot_glow.png 30x30, plate.png 16x16, bar_caps.png 20x9.

## Фишки
Все выключаются в `config/dungeonhud-client.toml`.
Клавиша H (меняется в управлении) выбирает слот с лечением, если оно в хотбаре.

## Сборка
- GitHub Actions: залей папку в репозиторий, jar появится в артефактах запуска `build`.
- Локально: JDK 17 + Gradle 8.1.1, команда `gradle build`, jar в `build/libs/`.
  Для `./gradlew` скопируй gradlew, gradlew.bat и gradle/ из Forge 1.20.1 MDK.
