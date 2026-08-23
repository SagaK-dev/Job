# Job

Spigot 1.21.1 economy reward plugin using Vault.

## Requirements

- Java 21
- Spigot 1.21.1
- Vault
- A Vault-compatible economy plugin

## Rates

| Work | Target | Rate |
|---|---|---:|
| Miner | stone / cobblestone / deepslate and common stone variants | 16 blocks = ¥1 |
| Miner | coal / copper / iron ore, including deepslate variants | 1 block = ¥2 |
| Miner | gold / redstone / lapis ore, including deepslate variants | 1 block = ¥4 |
| Miner | diamond / emerald ore, including deepslate variants | 1 block = ¥12 |
| Lumberjack | logs / wood / stems / hyphae, including stripped variants | 8 blocks = ¥1 |
| Farmer | mature crops | 8 harvests = ¥1 |
| Land clearer | dirt / sand / gravel / clay and common terrain variants | 16 blocks = ¥1 |
| Weeder | short grass / fern / large fern / tall grass / dead bush | 128 blocks = ¥1 |

Players within 72 hours of their first login receive **3x the completed-unit payout**. For example, 16 stone pays ¥3 to a beginner, rather than changing the 16-block unit size.

## Anti-abuse

Player-placed blocks are marked in the owning chunk's PersistentDataContainer. Breaking those marked blocks does not pay mining, lumberjack, land-clearing, or weeding rewards. Mature planted crops are still eligible because farming is expected to involve planting. Manually placed melons/pumpkins are excluded.

## Commands

- `/job rates`
- `/job status`
- `/job reload` (OP / `job.admin`)
