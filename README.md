# Global Ban

<div>
  <img src="https://mods.matthiesen.dev/badges/matthiesenCore.svg" alt="Matthiesen Core">
  <img src="https://mods.matthiesen.dev/badges/gooeylibs_optional.svg" alt="GooeyLibs">
</div>

Server side ban management plugin, sync multiple servers, or connect multiple communities together. Gives server owner's the 
ability to ban players across multiple servers, and even across multiple communities.

Custom ban messages, reasons, and durations can be set for the different punishment types, and can be configured to be different for each server. 
This allows for a more personalized experience for the player, and can help to reduce confusion when a player is banned from multiple servers.

## Commands

| Command                                 | Description                                                                                                               |
|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `/global-ban`                           | Root command for global-ban specific commands.                                                                            |
| `/global-ban reload`                    | Reloads the configuration files for global-ban.                                                                           |
| `/global-ban import [cleanup boolean]`  | Imports your existing vanilla ban list into the global-ban system.                                                        |
| `/global-ban banlist [page integer]`    | Lists all players that are currently in your global-ban list.                                                             |
| `/kick <player> [reason]`               | Kicks a player from the server with an optional reason.                                                                   |
| `/ban <player> [reason]`                | Bans a player from the server with an optional reason.                                                                    |
| `/tempban <player> <duration> [reason]` | Temporarily bans a player from the server for a specified duration with an optional reason.                               |
| `/ban-ip <ip> [reason]`                 | Bans an IP address from the server with an optional reason.                                                               |
| `/tempban-ip <ip> <duration> [reason]`  | Temporarily bans an IP address from the server for a specified duration with an optional reason.                          |
| `/unban <player>`                       | Unbans a player from the server.                                                                                          |
| `/unban-ip <ip>`                        | Unbans an IP address from the server.                                                                                     |
| `/pardon <player>`                      | Pardons a player from the server, removing all punishments associated with that player and its related accounts.          |
| `/pardon-ip <ip>`                       | Pardons an IP address from the server, removing all punishments associated with that IP address and its related accounts. |

### Overridden Vanilla Commands

The following vanilla commands have been overridden to use the global-ban system instead of the vanilla ban system. This allows for a more 
seamless experience for server owners and players, as they can use the same commands they are used to, but with the added functionality of the global-ban system.

To continue using the vanilla commands, you can use the `minecraft:` prefix before the command. For example, to use the vanilla `ban` command, you would use `/minecraft:ban <player> [reason]`.

| Vanilla Command | Replaced with         |
|-----------------|-----------------------|
| `ban`           | `minecraft:ban`       |
| `ban-ip`        | `minecraft:ban-ip`    |
| `banlist`       | `minecraft:banlist`   |
| `kick`          | `minecraft:kick`      |
| `pardon`        | `minecraft:pardon`    |
| `pardon-ip`     | `minecraft:pardon-ip` |

## Permission Nodes

The following permission nodes can be used to control access to the various commands and features of the global-ban system. 
These permission nodes can be used with any permission management system, such as LuckPerms, to control access to the 
commands and features of the global-ban system.

| Permission Node                        | Description                                                             |
|----------------------------------------|-------------------------------------------------------------------------|
| `global_ban.see_known_accounts`        | Allows Admins to see all known accounts for a player when they connect. |
| `global_ban.see_bans`                  | Allows Admins to see bans when they happen, and when they are applied.  |
| `global_ban.can_ban_admins`            | Allows Admins to ban other Admins.                                      |
| `global_ban.block_punishments`         | Blocks all punishments from being applied to the player.                |
| `global_ban.command.global_ban`        | Allows Admins to use the `/global-ban` command.                         |
| `global_ban.command.global_ban.reload` | Allows Admins to use the `/global-ban reload` command.                  |
| `global_ban.command.global_ban.import` | Allows Admins to use the `/global-ban import` command.                  |
| `global_ban.command.kick`              | Allows Admins to use the `/kick` command.                               |
| `global_ban.command.ban`               | Allows Admins to use the `/ban` command.                                |
| `global_ban.command.temp_ban`          | Allows Admins to use the `/tempban` command.                            |
| `global_ban.command.ban_ip`            | Allows Admins to use the `/ban-ip` command.                             |
| `global_ban.command.temp_ban_ip`       | Allows Admins to use the `/tempban-ip` command.                         |
| `global_ban.command.unban`             | Allows Admins to use the `/unban` command.                              |
| `global_ban.command.unban_ip`          | Allows Admins to use the `/unban-ip` command.                           |
| `global_ban.command.pardon`            | Allows Admins to use the `/pardon` command.                             |
| `global_ban.command.pardon_ip`         | Allows Admins to use the `/pardon-ip` command.                          |
| `global_ban.command.ban_list`          | Allows Admins to use the `/global-ban banlist` & `/banlist` commands.   |

## Remote API Sync

Global Ban has the ability to sync punishments to a remote API, allowing for multiple servers to share the same ban list.
This allows for Admins to ban players across multiple servers, and if desired, across multiple connected communities. 
This feature is disabled by default, and can be enabled in the configuration file.

A Remote API server implementation is required to use this feature, and is not included with the mod itself, and must be hosted separately. 

> **Note:** This feature is still in development, and does not have a Remote API server implementation available yet. 
> If you are interested in helping to develop this feature, please reach out to the developer in the [Discord](https://discord.gg/4ePfVRgexS) server.

## Requirements

- [Matthiesen Core](https://modrinth.com/mod/matthiesen-core)
- [Fabric API](https://modrinth.com/mod/fabric-api) (Fabric only)
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) (Fabric only)

### Optional Dependencies

- [GooeyLibs](https://modrinth.com/mod/gooeylibs) - Required for GUI-based moderation tools, otherwise all moderation is done via commands.
- [LuckPerms](https://modrinth.com/mod/luckperms) - Required for permission management, otherwise all permissions are managed via the Vanilla Permissions System.

## Docs

Documentation for this mod can be found at [mods.matthiesen.dev](https://mods.matthiesen.dev/global-ban/)

## Version Compatibility

| Minecraft Version | Matthiesen Core Version | Mod Version |
|-------------------|-------------------------|-------------|
| 1.21.1            | 1.x.x                   | 1.x.x       |

## FastStats Metrics

This mod uses [FastStats](https://faststats.dev) to collect anonymous usage statistics. This helps the developer understand
how this mod is being used and improve it over time. You can learn more about the data collected and how it is used by visiting
[FastStats: Information](https://faststats.dev/info).

You can also view the data collected by this mod on the [FastStats: Global Ban](https://faststats.dev/project/global-ban) page.

To opt out of this data collection, set the `enabled` property to `false` in the `<game_directory>/config/matthiesen_core/metrics.properties` file.

## License

MIT - see `LICENSE`.
