---
icon: power-off
---

# Быстрый старт

## Установка

1. Соберите плагин (`mvn package`) или возьмите готовый `.jar`.
2. Положите `.jar` в `plugins/` вашего Paper-сервера.
3. Перезапустите сервер.

После первого запуска появятся:

* `plugins/SkillsEngine/config.yml`
* `plugins/SkillsEngine/skills/`
* `plugins/SkillsEngine/skills/example.yml` *(только на самом первом запуске, когда папки плагина ещё не было)*

## Команды

* `/se reload` — перезагрузить навыки **из источников**, перечисленных в `config.yml -> skills.sources`.
* `/se cast <skillId> [player]` — принудительно кастануть навык (для тестов).

Права:

* `skillsengine.admin` — доступ к `/se` (админские команды).

## Проверка, что всё работает

1. Откройте `plugins/SkillsEngine/skills/example.yml`
2. В игре выполните `/example`
3. Должны появиться частицы/звук и actionbar-сообщение.

Если не работает:

* включите `debug: true` в `config.yml` и перезапустите сервер
* выполните `/se reload` и проверьте предупреждения/ошибки в чате и консоли

## Дальше

* [Формат навыков: Общее](<Настройки навыков/01-Общее.md>)
* [config.yml](03-config.md)
