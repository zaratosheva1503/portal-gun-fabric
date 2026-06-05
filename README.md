# Portal Gun (Fabric 1.20.1)

Мод в стиле **Portal 2** для Minecraft **1.20.1 / Fabric**, построенный поверх **[Immersive Portals](https://modrinth.com/mod/immersiveportals)** (imm_ptl). Рендер порталов не пишется с нуля — вся логика управляет сущностями `Portal` из IP.

## Возможности

- **Portal Gun** — кастомный предмет. **ЛКМ** → синий портал, **ПКМ** → оранжевый. Одна пара на игрока; новый портал того же цвета удаляет старый.
- **Ориентация** — портал «прилипает» к стене/полу/потолку; поворот через кватернионы (`DQuaternion`).
- **Momentum** — сохранение модуля скорости при переходе (Mixin).
- **Жидкости** — вода/лава текут сквозь портал.
- **Снаряды** — стрелы пролетают насквозь.

## Две версии сборки

| Версия | Команда | Что нужно в папке mods |
|--------|---------|----------------------|
| **SLIM** (`portalgun-x.jar`) | `gradle build` | Fabric Loader + Fabric API + Immersive Portals установлены отдельно |
| **BUNDLED** (`portalgun-bundled-x.jar`) | `gradle build -PbundleDeps=true` | Только Fabric Loader — Fabric API и IP уже внутри (Jar-in-Jar) |

> BUNDLED версия вкладывает Immersive Portals (Apache-2.0) и Fabric API внутрь через штатный Fabric Jar-in-Jar (`include`).

## CI / компилятор

В репо настроен **GitHub Actions** (`.github/workflows/build.yml`): на каждый push он компилирует обе версии и выкладывает jar-ы в **Artifacts**. Смотри вкладку **Actions** — там же видны ошибки компиляции, если они есть.

## Локальная сборка

```bash
gradle wrapper --gradle-version 8.6   # если нет ./gradlew
./gradlew clean build                 # SLIM
./gradlew clean build -PbundleDeps=true  # BUNDLED
./gradlew runClient                   # тест
```

В игре: `/give @s portalgun:portal_gun`

## ⚠️ Честный статус

Код сверен с исходниками IP (ветка `1.20.1`): `DQuaternion`, `McHelper.spawnServerEntity` и математика кватернионов подтверждены. Исправлены: координата зависимости (`immersiveportals:5.2.0-mc1.20.1-fabric`), `reloadAndSyncToClientNextTick()`, `setRotation()`.

Остаются вызовы `Portal`, которые нужно подтвердить компилятором (имена могут отличаться в этой версии IP): `Portal.entityType`, `setOriginPos`, `setOrientationAndSize`, `setDestination`, `setDestinationDimension`, `getOriginDim`, `axisW`/`axisH`, `transformPoint`, `transformLocalVecNonScale`, `transformVelocity`. CI покажет точные ошибки — правь по декомпиляции (`./gradlew genSources`).

Текстуру `assets/portalgun/textures/item/portal_gun.png` добавь сам (бинарник нельзя было залить текстом).

Лицензия: MIT.
