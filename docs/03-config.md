---
icon: sliders-simple
---

# Конфигурация

Файл: `plugins/SkillsEngine/config.yml`

## skills.sources

`skills.sources` — **обязательный** список файлов/папок, из которых плагин загружает умения.

Пути задаются **относительно** папки `plugins/SkillsEngine/skills/`.

Поддерживаемые варианты:

* `"example.yml"` — один файл
* `"pvp/skills.yml"` — файл в подпапке
* `"classes"` или `"classes/"` — папка (будут загружены все `*.yml` **рекурсивно**)
* `"./"` или `"."` — вся папка `skills/` (эквивалент “загрузи всё”) 

Пример:

```yml
skills:
  sources:
    - "example.yml"
    - "classes/"
```

Важно:

* если `skills.sources` отсутствует или пустой — плагин не загрузит умения (и при старте отключится)
* `/se reload` перечитывает **этот список** и загружает только указанные источники

## debug

`debug: false|true`

Если включить, плагин будет логировать в консоль:

* диспатч триггеров
* попытки каста и причины отказа

## messages

Сообщения показываются игроку, когда каст запрещён и в навыке не задан `denyMessage`.

### Плейсхолдеры

* `{player}` — ник игрока.
* `{skill}` — `id` навыка.
* `{reason}` — причина отказа (человекочитаемая строка).
* `{code}` — машинный код причины (например `COOLDOWN`, `NO_PERMISSION`, `NO_TARGET`).
* `{payload}` — дополнительная часть причины (если есть).
* `{seconds}` — секунды (для `cooldown`).

### Ключи

* `messages.default`
* `messages.cooldown`
* `messages.no_permission`
* `messages.low_level`
* `messages.missing_item`
* `messages.cost_xp_levels`
* `messages.cost_item`
* `messages.no_target`
* `messages.world_not_allowed`
* `messages.world_denied`

### Приоритет сообщений

1. Если в навыке указан `denyMessage` — используется он.
2. Иначе используется шаблон из `messages.<key>`.

См. также: [Формат навыков: Общее](<Настройки навыков/01-Общее.md>)
