# Locomotion Slop

Minecraft **1.21.1** / **NeoForge** client mod.

One project that runs **Trainguy9512's Locomotion first-person player animations** and **Fresh
Animations' player add-on third-person animations** at the same time, without either one breaking
the other.

---

## How the two systems avoid each other

| | First person | Third person |
|---|---|---|
| Animation source | Locomotion's first-person rig + 108 sequences (this mod) | Your model pack's JEM/JPM, played by EMF |
| Who renders | Vanilla's `PlayerRenderer` arm draw calls, with animated transforms | EMF/ETF, completely untouched |
| This mod's involvement | Injects into `ItemInHandRenderer` and `GameRenderer` | **None** — third person is never intercepted |

The seam is the model parts. Instead of drawing its own arm geometry, this mod computes a pose and
hands it to the *existing* `ModelPart`s (`rightArm`, `rightSleeve`, `leftArm`, `leftSleeve`) that
`PlayerRenderer#renderRightHand`/`renderLeftHand` already draw. Whatever model and skin are in play
— EMF custom geometry, an ETF skin variant, a slim model, a custom cape — is exactly what gets
animated. That is why EMF can stay in charge of third person and Fresh Animations keeps working
unmodified.

The only thing this mod asks of EMF is that it not also animate the first-person hands. That is
EMF's own `preventFirstPersonHandAnimating` config switch; `EmfIntegration` flips it through
reflection while this mod's first-person layer is on, and restores vanilla/EMF behaviour the moment
you turn the layer off.

## Requirements

- Minecraft **1.21.1**, NeoForge **21.1.x** (built against 21.1.249)
- Java **21**
- Recommended: [Entity Model Features](https://modrinth.com/mod/emf) +
  [Entity Texture Features](https://modrinth.com/mod/etf) — these are what play a player model pack.
  Both are optional dependencies; the mod loads without them.
- Bring your own copy of the **Fresh Animations Player Add-on** (early-access build by FreshLX). It
  is *not* bundled and must not be. Install it as a resource pack.
- The vendored Locomotion animation data is **All Rights Reserved** — see
  [`../VENDORED-NOTICE.md`](../VENDORED-NOTICE.md) before publishing anything built from here.

## Building

```bash
cd mod
./gradlew build        # jar lands in mod/build/libs
```

The first run downloads the NeoForge userdev artifacts, so it needs network access.

## Config

`config/locomotion_slop.json`

| Key | Default | Effect |
|---|---|---|
| `firstPersonAnimations` | `true` | Master switch. Off = 100% vanilla hands and view bob. |
| `animationViewBob` | `true` | Drive the camera bob from the rig's `camera_jnt` instead of vanilla's sine bob. Only active while the vanilla "View Bobbing" option is on. |
| `emfDeferFirstPersonHands` | `true` | Set EMF's `preventFirstPersonHandAnimating`. |
| `emfExposeVariables` | `true` | Publish `slop_*` animation variables to EMF. |
| `logCompatDiagnostics` | `true` | Log EMF/ETF detection and which pack provides the player model. |

### Animation variables exposed to EMF packs

`slop_first_person_active`, `slop_mining`, `slop_attacking`, `slop_using_item`, `slop_sprinting`,
`slop_sneaking`, `slop_falling`, `slop_swimming` — usable in a JPM the same way as any other EMF
animation variable.

## Swapping the animation data

The rig and sequences are loaded through the vanilla `ResourceManager`, so a resource pack providing
`assets/locomotion_slop/skeletons/**/*.json` and `assets/locomotion_slop/sequences/**/*.json`
(format version ≥ 5) overrides or extends them with no rebuild. Deleting the vendored data leaves
the mod working — first person simply falls back to vanilla.

## Layout

```
mod/src/main/java/com/locomotionslop/
├── LocomotionSlop.java              client entry point, event wiring
├── SlopConfig.java                  config/locomotion_slop.json
├── access/MatrixModelPart.java      duck interface for the one-shot arm matrix
├── mixin/
│   ├── MixinModelPart.java          applies the joint matrix in translateAndRotate
│   ├── MixinItemInHandRenderer.java renders both arms + items, cancels vanilla's pass
│   └── MixinGameRenderer.java       view bob from the rig's camera joint
├── render/FirstPersonArmRenderer.java
├── animation/                       ported animation engine (Locomotion, ARR - see notice)
│   ├── animator/                    state machine, hand layers, drivers
│   ├── joint/, pose/, sequence/, util/
├── resource/                        skeleton + sequence loading, Gson deserializers
└── compat/                          EMF / ETF / player-model-pack detection (all optional)
```

## Conflicts

- **Locomotion itself** is declared an `incompatible` dependency: both would animate the same arms.
- Other mods that render first-person hands through `ClientHooks.renderSpecificFirstPersonHand`
  will draw *in addition to* these arms, because this mod renders at the head of
  `renderHandsWithItems` and then cancels vanilla's per-hand pass.
