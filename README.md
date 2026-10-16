# CastSMP 26.2 — source project

A Cast-inspired Paper plugin project for Minecraft Java 26.2. This is an independent implementation; it is not the original BuiltByBit plugin and does not include its proprietary code or textures.

## Build
- Install JDK 25.
- Install Gradle 8.14+ (or open this project in IntelliJ IDEA with Gradle support).
- Run `gradle build` from this folder. Output JAR: `build/libs/CastSMP-26.2-1.0.0.jar`.
- Copy the JAR into the server's `plugins/` folder and start Paper 26.2.

## Commands
- `/spellbook` opens a selector GUI.
- `/cast give <cast> [player>` gives a cast (OP / `castsmp.admin`).
- `/cast reload` reloads config.
- `/cast spells` lists/open the selector.

Players right-click a cast item to activate it. Cooldowns are configurable in `plugins/CastSMP/config.yml`.

## Resource pack
The `resourcepack/` folder contains original, independently-created placeholder icons for this implementation. Zip its contents (not the enclosing folder) to make the pack. It is not the ARR-licensed Modrinth pack. The Modrinth Cast SMP pack currently lists Java 1.21.4–1.21.11, not 26.2, so it cannot be promised to work unchanged on 26.2. The official pack is not bundled or modified here. See the Modrinth page: https://modrinth.com/resourcepack/cast-smp-resource-pack

## Important compatibility note
This environment has Java 21 and no Gradle/Maven, so the JAR was not compiled or runtime-tested here. Compile with JDK 25 and the current Paper 26.2 API. Treat this as a source project needing a real server test before production use. Some abilities are gameplay-inspired implementations rather than verified copies of the proprietary original.
