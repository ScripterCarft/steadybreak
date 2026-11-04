# Changelog

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [1.0.0-alpha.1] - 2026-11-04

First alpha for Minecraft 26.3 on Fabric.

### Fixed

- Held-item updates no longer reset block break progress. This covers Mending damage changes and
  Hypixel SkyBlock fuel, lore and counter updates.
- The same updates no longer replay the equip animation.
- Moving the crosshair to another face of a block no longer stops the break on Hypixel. The new 26.3
  `CHANGE_DESTROY_DIRECTION` packet is no longer sent.
