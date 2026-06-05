# Portal Gun (Fabric 1.20.1)

Мод в стиле **Portal 2** для Minecraft **1.20.1 / Fabric**, построенный поверх **[Immersive Portals](https://modrinth.com/mod/immersive-portals)** (imm_ptl). Мы не пишем рендер порталов с нуля — вся логика управляет сущностями `Portal` из IP.

## Возможности

- **Portal Gun** — кастомный предмет. **ЛКМ** → синий портал, **ПКМ** → оранжевый. Одна пара порталов на игрока; новый портал того же цвета удаляет старый.
- **Ориентация** — портал «прилипает» к стене/полу/потолку; поворот считается через кватернионы (`DQuaternion`).
- **Momentum** — сохранение модуля скорости при переходе (Mixin на `Portal`).
- **Жидкости** — вода/лава втекают в один портал и вытекают из другого (`PortalFluidManager`, серверный «насос» с защитой от бесконечного цикла).
- **Снаряды** — стрелы пролетают портал насквозь (Mixin на `PersistentProjectileEntity`).
- **Цветная рамка** — клиентская обводка портала цветом (синий/оранжевый).

## Сборка

```bash
# 1) сгенерировать Gradle wrapper (jar не хранится в репо)
gradle wrapper --gradle-version 8.6

# 2) собрать
./gradlew build

# 3) запустить клиент для теста
./gradlew runClient
```

Готовый jar — в `build/libs/`. Нужны Fabric Loader, Fabric API и Immersive Portals в папке mods.

Дать себе предмет: `/give @s portalgun:portal_gun`

## ⚠️ Важно перед сборкой

1. **Версия Immersive Portals в `build.gradle` иллюстративная.** Подставьте реальный тег с Modrinth (1.20.1, Fabric).
2. **Сигнатуры методов IP меняются между версиями.** После `./gradlew genSources` сверьте: `setRotationTransformationD`, `transformVelocity`, `transformPoint`, `transformLocalVecNonScale`, `isInside`, поле `extraData` у `Portal`. При несовпадении поправьте имена в одном месте.
3. **Текстура предмета** (`assets/portalgun/textures/item/portal_gun.png`) не вложена (бинарник) — добавьте свою или предмет будет с missing-texture.

## Структура

```
src/main/java/portalgun/
  PortalGunMod.java          — инициализатор, регистрация, эвенты
  PortalColor.java
  item/PortalGunItem.java    — Raycast, ЛКМ/ПКМ
  portal/PortalSpawnManager.java — спавн, ориентация, линковка, rotation
  portal/PortalColorAccess.java
  portal/PortalIndex.java
  fluid/PortalFluidManager.java  — жидкость сквозь портал
  client/PortalGunClient.java
  client/PortalFrameRenderer.java
  mixin/PortalVelocityMixin.java
  mixin/AbstractArrowMixin.java
```

Лицензия: MIT.
