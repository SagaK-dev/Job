# Job

Spigot 1.21.1 / Java 21 jobs, sell GUI, and built-in yen economy.

## Built-in yen

Job 2.0 stores an integer `yen` balance directly in each player's persistent Bukkit data. Vault is no longer required and Job yen is not the same balance as another economy plugin.

Both automatic work rewards and GUI sales pay into the same Job yen balance. Players under the configured cumulative-playtime beginner threshold receive the configured beginner multiplier on both sources.

## Sell GUI

Run `/job` (or `/job shop`) to open the categorized sell menu. Categories are mining/ores, wood, crops, terrain materials, and grass/plants.

Inside a category:

- Left-click an item: sell one configured unit.
- Right-click an item: sell every complete unit currently in the player's inventory.
- Custom-named/lore/PDC items are not consumed; only plain vanilla stacks are sellable.
- Prices use the same live `RateTable` as work rewards, so `/job rate set ...` updates the GUI immediately.

## Commands

- `/job` or `/job shop` - open the sell GUI.
- `/job balance` - show the built-in yen balance.
- `/job rates` - show live work/sell rates.
- `/job status` - show yen, beginner status, and work progress.
- `/job rate list` - list rates (admin).
- `/job rate get <key>` - inspect one rate (admin).
- `/job rate set <key> <yen>` - change payout only (admin).
- `/job rate set <key> <unit-count> <yen>` - change unit count and payout (admin).
- `/job setrate ...` - shorthand for rate set (admin).
- `/job reload` - reload configuration (admin).

## Anti-abuse

The existing placed-block tracking remains active for automatic work rewards. GUI sales only consume plain vanilla item stacks and do not accept custom-metadata items.
