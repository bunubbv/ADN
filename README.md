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
locales:
  error:
    args:
      not-provided: '<prefix> <red>No arguments provided.'
      too-many: '<prefix> <red>Too many arguments provided.'
    invalid:
      command: '<prefix> <red>Invalid command.'
      nick: '<prefix> <red>Invalid nickname. It must match: <value>'
      nick-length: '<prefix> <red>Nickname is too long. Max length is <value>'
      nick-pattern: '<prefix> <red>''nick-pattern'' in config.yml is missing or malformed.'
      player: '<prefix> <red>Invalid player.'
      tags: '<prefix> <red>You used a formatting tag you do not have permission to use.'
    must-be-player: '<prefix> <red>A player is required to run this command here'
    nick:
      is-null: '<prefix> <red>Nickname is null. Check your formatting.'
      name-nonexistent: '<prefix> <red>That nickname does not exist.'
      other-players-nick: '<prefix> <red>You cannot use <value><reset><red>; that name belongs to another player.'
      remove-failure: '<prefix> <red>Failed to remove the nickname.'
      save-failure: '<prefix> <red>Failed to save the nickname.'
      too-many-to-save: '<prefix> <red>You have reached the limit of saved nicknames. Remove one with /adn remove <value>'
    no-permission: '<prefix> <red>You don''t have permission to use this command.'
  info:
    config-reloaded: '<prefix> <green>Config reloaded successfully!'
    help-message: '/adn <set|reload|remove|reset>'
    prefix: '<gray><b>[ADN]</b></gray>'
    shown-help: '<prefix> <target><reset><yellow> has been shown the help screen'
  nick:
    remove: '<prefix> The nickname <value> has been removed.'
    reset:
      by-user: '<prefix> <gray>Your nickname was reset by <initiator>.'
      self: '<prefix> Your nickname has been reset.'
      user: '<prefix> Reset <target>''s nickname.'
    set:
      by-user: '<prefix> <gray><initiator> changed your nickname to <value><reset>.'
      self: '<prefix> Your nickname has been updated to <value><reset>.'
      user: '<prefix> Updated <target>''s nickname to <value><reset>.'
options:
  nick-length: 30
  nick-pattern: '[가-힣a-zA-Z0-9]+'
  nick-prefix: ''
  nick-protection: -1
  tablist-nick: true

```

* After editing the config, run /adn reload to apply changes without restarting the server.

## Permissions
* adn.format:
    * default: true
* adn.reload:
    * default: 'op'
* adn.remove:
    * default: true
* adn.reset.self:
    * default: true
* adn.reset.user:
    * default: 'op'
* adn.set.self:
    * default: true
* adn.set.user:
    * default: 'op'
* adn.use:
    * default: true

## Commands
* /adn set \<nickname>
    * _(admin)_ /adn set \<nickname> \<username>
* _(admin)_ /adn reload
* /adn reset
    * _(admin)_ /adn reset \<username>
