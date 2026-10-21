# Dungeon HUD (Forge 1.20.1)

Клиентский мод: HUD в стиле Minecraft Dungeons. Без текстур — всё рисуется кодом.

## Сборка
1. Скачай Forge 1.20.1 MDK (files.minecraftforge.net) и скопируй из него в эту папку
   `gradlew`, `gradlew.bat` и папку `gradle/` (Gradle wrapper).
2. Нужна Java 17.
3. `./gradlew build` -> jar в `build/libs/`
   Запуск для теста: `./gradlew runClient`
4. Поменяй `mod_group_id`, `mod_authors` в gradle.properties и пакет `com.example.dungeonhud` на свой.
