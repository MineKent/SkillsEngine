# Формат навыков (skills)

Навыки лежат в: `plugins/SkillsEngine/skills/*.yml`.

Каждый файл описывает **один** навык.
Если `id` не указан, используется имя файла без `.yml`.

## Минимальный пример
```yml
id: fire
name: "Fire"

trigger:
  type: COMMAND
  command: "fire"

target:
  type: TARGET
  range: 5

actions:
  - type: SET_FIRE
    seconds: 3
```

## Поля верхнего уровня
- `id: string` — уникальный id
- `name: string` — отображаемое имя (пока используется только как данные)
- `type: string` — произвольная категория (не влияет на логику)
- `cooldown: 500ms|10s|2m|1h|число` — кулдаун (число = секунды)
- `denyMessage: string` — сообщение игроку при отказе (переопределяет `config.yml`)
- `trigger: {...}` — триггер
- `target: {...}` — таргетинг
- `conditions: [ ... ]` — список условий
- `cost: {...}` — стоимость
- `actions: [ ... ]` — список действий

---

## trigger
`trigger.type` поддерживает:
- `COMMAND` — по команде игрока
  - поля: `command: "example"`
- `RIGHT_CLICK` — правый клик
  - поля (опц.): `material: "DIAMOND_SWORD"` (только если предмет в main hand)
- `LEFT_CLICK` — левый клик
  - поля (опц.): `material: "DIAMOND_SWORD"`
- `ON_HIT` — игрок ударил сущность
- `ON_DAMAGE` — игрок получил урон

---

## target
`target.type` поддерживает:
- `SELF` — цель = кастер
- `TARGET` — цель = entity
  - поля: `range: number` (если цель не пришла из события, берётся entity в прицеле)
- `AREA` — список целей в радиусе
  - поля: `radius: number`
  - поля (опц.): `center: CASTER|TARGET` (по умолчанию CASTER)

---

## conditions
Каждый элемент — map с `type`.

Поддерживаются:
- `HAS_PERMISSION`
  - `permission: "my.perm"`
- `LEVEL_AT_LEAST`
  - `min: 10` (или `level: 10`)
  - уровень берётся из vanilla `Player#getLevel`
- `HAS_ITEM`
  - `material: "EMERALD"`
  - `amount: 2` (по умолчанию 1)
  - `where: INVENTORY|HAND|MAIN_HAND` (по умолчанию INVENTORY)
- `WORLD_ALLOWED`
  - `allowed: ["world", "world_nether"]` (опционально)
  - `denied: ["arena"]` (опционально)

Примечание: `COOLDOWN_READY` считается deprecated — кулдаун проверяется автоматически, если задан `cooldown`.

---

## cost
Поддерживается:
- `xpLevels: 1` — списать уровни
- `material: "EMERALD"`
- `amount: 2`

Если не хватает ресурсов — каст отклоняется.

---

## actions
Каждый элемент — map с `type`.

Поддерживаются:

### DAMAGE
Нанести урон всем целям.
- `amount: 4.0`

### HEAL
Лечить цели.
- `amount: 6.0`

### POTION
Дать эффект.
- `effect: "SPEED"`
- `duration: 5` (сек) или `durationTicks: 100`
- `amplifier: 1`
- `ambient: false`
- `particles: true`
- `icon: true`

### DASH
Рывок кастера вперёд.
- `power: 1.2`
- `keepY: true`

### PARTICLES
Спавн частиц в мире.
- `particle: "CRIT"`
- `count: 10`
- `at: CASTER|TARGET|CENTER` (опц.)
- `offset/offsetX/offsetY/offsetZ`
- `speed`
- `show_all_players: false` (Paper)
- для `DUST`/`DUST_COLOR_TRANSITION`:
  - `color: "#ff0000"` или `"255,0,0"`
  - `toColor: "#ffffff"` (только для transition)
  - `size: 1.0`

### SOUND
Проиграть звук.
- `sound: "ENTITY_PLAYER_LEVELUP"`
- `volume: 1.0`
- `pitch: 1.0`
- `category: "MASTER"` (опц.)
- `mode: WORLD|CASTER|TARGETS`
- `at: CASTER|TARGET|CENTER` (опц.)

### MESSAGE
Сообщение кастеру.
- `text: "&aHello"`
- `mode: CHAT|ACTIONBAR|TITLE` (опц.; для полноценного title лучше `TITLE` action)

### ACTIONBAR
- `text: "&e..."`

### TITLE
- `title: "&6Title"`
- `subtitle: "&fSubtitle"`
- `fadeIn: 10`
- `stay: 40`
- `fadeOut: 10`

### COMMAND
Выполнить команду.
- `executor: CONSOLE|PLAYER` (по умолчанию CONSOLE)
- `command: "say {player} cast {skill}"`

### TELEPORT
Телепорт кастера.
- `to: CASTER|TARGET|CENTER|LOCATION`
- для `LOCATION`: `world`, `x`, `y`, `z`, (опц.) `yaw`, `pitch`

### KNOCKBACK
Оттолкнуть цели от кастера.
- `strength: 1.0`
- `y: 0.35`

### PULL
Притянуть цели к кастеру.
- `strength: 1.0`
- `y: 0.35`

### SET_FIRE
Поджечь цели.
- `seconds: 3` (или `ticks`)

### EXPLOSION
Создать взрыв.
- `power: 2.0`
- `setFire: false`
- `breakBlocks: false`
- `damageEntities: true`

### GIVE_ITEM
Выдать предмет кастеру.
- `material: "EMERALD"`
- `amount: 1`

### TAKE_ITEM
Забрать предмет у кастера.
- `material: "EMERALD"`
- `amount: 1`

### SPAWN_ENTITY
Заспавнить сущность.
- `entity: "ZOMBIE"`
- `count: 1`

---

## Примеры
Смотрите `skills/example.yml` и `skills/_template.yml` в папке плагина.
