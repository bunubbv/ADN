**ADN** (Advanced Display Name) is a lightweight Minecraft plugin for managing custom player nicknames.

## Features
### Tablist nicknames
<img width="372" height="52" alt="2026-04-25-22-54-50" src="https://github.com/user-attachments/assets/3775a6dd-8f62-40c9-8810-a80ce98f8c5d" />

### MiniMessage Formatting
<img width="1168" height="223" alt="2026-04-25-22-54-45" src="https://github.com/user-attachments/assets/f13f60af-6481-4793-bea8-a128dfe807e1" />

### System message nicknames (Requires [ProtocolLib](https://github.com/dmulloy2/ProtocolLib))
<img width="1025" height="49" alt="2026-04-25-22-55-35" src="https://github.com/user-attachments/assets/06e06cd5-626e-48ec-a289-6844bede196c" />
<img width="697" height="49" alt="2026-04-25-22-55-24" src="https://github.com/user-attachments/assets/93afe351-93a9-4609-81db-15b12b96103b" />

### Other Features
* Permissions support
* SQLite-based nickname storage
* Configurable nickname length limit
* Configurable nickname regex validation
* Nickname prefix support

## Configuration
* The plugin creates a config file at plugins/ADN/config.yml.
* Default settings:

```yaml
# config.yml
debug: false
nickname-prefix: ''
nickname-pattern: '[A-Za-z0-9ㄱ-ㅎㅏ-ㅣ가-힣]+'
nickname-max-length: 30
tablist-nickname: true

# messages.yml
prefix: '<gray><b>[ADN]</b></gray>'
info:
  help: '/adn <set|reload|reset>'
  reload: '<prefix> <green>Config reloaded successfully!'
error:
  no-permission: '<prefix> <red>You don''t have permission to use this command.'
  no-format-permission: '<prefix> <red>You used a formatting tag you do not have permission to use.'
  must-be-player: '<prefix> <red>A player is required to run this command here.'
  invalid-player: '<prefix> <red>Invalid player.'
  nickname-null: '<prefix> <red>Nickname is null. Check your formatting.'
  nickname-invalid: '<prefix> <red>Invalid nickname. It must match: <value>'
  nickname-too-long: '<prefix> <red>Nickname is too long. Max length is <value>'
  invalid-pattern: '<prefix> <red>''nick-pattern'' in config.yml is missing or malformed.'
nickname:
  set:
    self: '<prefix> Your nickname has been updated to <value><reset>.'
    other-sender: '<prefix> Updated <target>''s nickname to <value><reset>.'
    other-target: '<prefix> <gray><initiator> changed your nickname to <value><reset>.'
  reset:
    self: '<prefix> Your nickname has been reset.'
    other-sender: '<prefix> Reset <target>''s nickname.'
    other-target: '<prefix> <gray>Your nickname was reset by <initiator>.'
```

* After editing the config, run /adn reload to apply changes without restarting the server.

## Permissions
* adn.format:
    * default: true
* adn.reload:
    * default: 'op'
* adn.reset.self:
    * default: true
* adn.reset.user:
    * default: 'op'
* adn.set.self:
    * default: true
* adn.set.user:
    * default: 'op'

## Commands
* /adn set \<nickname>
    * _(admin)_ /adn set \<nickname> \<username>
* _(admin)_ /adn reload
* /adn reset
    * _(admin)_ /adn reset \<username>
