# Job

Spigot 1.21.1 / Java 21 job-reward plugin using Vault economy.

## Requirements

- Spigot 1.21.1
- Java 21
- Vault
- A Vault-compatible economy plugin

## Beginner bonus

For the first 72 hours after a player's first login, payouts are multiplied by 3 by default.
Both the duration and multiplier are configurable in `config.yml`.

## Default rates

| Key | Work | Default rate |
| --- | --- | --- |
| `miner-stone` | stone / cobblestone / deepslate and similar | 16 blocks = 1 yen |
| `miner-low-ore` | coal / copper / iron ores, including deepslate variants | 1 block = 2 yen |
| `miner-mid-ore` | gold / redstone / lapis ores, including deepslate variants | 1 block = 4 yen |
| `miner-high-ore` | diamond / emerald ores, including deepslate variants | 1 block = 12 yen |
| `lumberjack` | logs / wood / stems / hyphae | 8 blocks = 1 yen |
| `farmer` | mature crop harvests | 8 harvests = 1 yen |
| `land-clearer` | dirt / sand / gravel / clay and similar | 16 blocks = 1 yen |
| `weeder` | short grass / fern / tall grass / dead bush | 128 blocks = 1 yen |

## Commands

- `/job rates` - show the live rate table.
- `/job status` - show beginner bonus status and saved work progress.
- `/job rate list` - show the live rate table (admin).
- `/job rate get <key>` - show one rate.
- `/job rate set <key> <yen>` - change only payout amount.
- `/job rate set <key> <unit-count> <yen>` - change count and payout.
- `/job setrate <key> <yen>` - shorthand.
- `/job setrate <key> <unit-count> <yen>` - shorthand.
- `/job reload` - reload config and rates.

Rate changes are saved immediately to `config.yml` and survive restarts.

## Anti-abuse behavior

- CREATIVE and SPECTATOR do not earn rewards.
- Player-placed reward blocks do not pay when broken.
- Player-placed markers survive restarts in chunk PDC.
- Markers follow piston-moved and falling blocks such as sand/gravel.
- Explosions clear placement markers for destroyed blocks.
- Only rewardable placed block types are tracked to keep chunk metadata small.

## Farming details

- Ageable crops only pay when mature.
- Melons, pumpkins, torchflowers and pitcher plants pay when harvested and are excluded when manually placed.
- Mature sweet berry bushes count when harvested by right-click; bone meal use is excluded.
