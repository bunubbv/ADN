**ADN** (Advanced Display Name) is a lightweight Minecraft plugin for managing custom player nicknames.

## Features
### Tablist nicknames
<img width="308" height="60" alt="2026-02-13-15-17-07" src="https://github.com/user-attachments/assets/90fa9460-0fcf-4e70-872b-1f9c2cad3f01" />

### MiniMessage Formatting
<img width="887" height="253" alt="2026-02-13-15-17-01" src="https://github.com/user-attachments/assets/fe8099fb-84b9-4af5-a760-8e7ca5787618" />

### System message nicknames (Requires [ProtocolLib](https://github.com/dmulloy2/ProtocolLib))
<img width="875" height="38" alt="2026-02-13-15-17-42" src="https://github.com/user-attachments/assets/842abf6d-c17f-4fb8-ab31-08262f88c12f" />
<img width="625" height="38" alt="2026-02-13-15-17-53" src="https://github.com/user-attachments/assets/015da416-be94-4c85-9e26-9ad9df3ba6ad" />

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
