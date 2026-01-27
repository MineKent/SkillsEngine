# Быстрый старт

## Установка
1. Соберите плагин (`mvn package`) или возьмите готовый `.jar`.
2. Положите `.jar` в `plugins/` вашего Paper-сервера.
3. Перезапустите сервер.

После первого запуска появятся:
- `plugins/SkillsEngine/config.yml`
- `plugins/SkillsEngine/skills/example.yml`
- `plugins/SkillsEngine/skills/_template.yml`

## Команды
- `/se reload` — перезагрузить все навыки из папки `skills/`
- `/se cast <skillId> [player]` — принудительно кастануть навык

Права:
- `skillsengine.admin` — доступ к `/se`

## Проверка, что всё работает
1. Откройте `plugins/SkillsEngine/skills/example.yml`
2. В игре выполните `/example`
3. Должны появиться частицы/звук и сообщение (см. пример навыка).

Если не работает:
- включите `debug: true` в `config.yml` и перезапустите сервер
- выполните `/se reload` и проверьте предупреждения/ошибки в консоли
