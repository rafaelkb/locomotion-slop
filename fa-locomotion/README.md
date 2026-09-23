# Locomotion + Fresh Animations, NeoForge 1.21.1

Private source port. Do not publish the jar, the source, or the bundled Fresh Animations files.

Locomotion draws the first-person arms and items. Fresh Animations, through Entity Model Features, draws the third-person player. Locomotion does not register a third-person player animator, so it will not overwrite `player.jem`.

Entity Model Features and Entity Texture Features are not compile dependencies and are not shipped. Install them yourself. On startup this mod sets EMF's `preventFirstPersonHandAnimating` to true by reflection, so EMF leaves the first-person arm alone. If that reflection fails, turn the same option on in the EMF config.

## Build

Java 21 is required.

```
./gradlew build
```

The jar is `build/libs/locomotion-0.1.12-fa-1.21.1.jar`. Put it in the mods folder of a NeoForge 1.21.1 instance that also has EMF and ETF.

`./gradlew runClient` starts a client in `run/`.

## What is included

- Locomotion first-person sequences, skeletons, and the first-person animator, retargeted from 1.21.11 to 1.21.1.
- The Fresh Animations player pack, forced on as a built-in pack named "Fresh Animations Player". On 1.21.1 only the pack root and the `20-3` overlay load. The `21-2` and `21-5` overlays are newer than this version and are ignored.
- A client-only NeoForge entrypoint. There is no Fabric loader and no config screen.

Config is `config/locomotion.json`. `firstPersonPlayer.enableRenderer` turns the first-person renderer off if you need vanilla hands.

## What was dropped for 1.21.1

- Spear items and spear use animations. They do not exist in this version. Tridents still play the spear-named sequences, because 1.21.1 calls the trident use animation `SPEAR`.
- Copper chests, the consumable component, and the later interaction-result swing source.
- Locomotion's third-person player, chest, shulker, and debug renderers. The chest and shulker animators are still registered, but nothing draws them.
- The in-game config screen.
