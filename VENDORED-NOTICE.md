# Vendored assets, and what may / may not be redistributed

This project does **not** contain only original work. Read this before publishing anything built
from this repository.

---

## 1. First-person rig + animation data — Locomotion (NOT free to redistribute)

**Where it lives**

| Path | Files |
|---|---|
| `mod/src/main/resources/assets/locomotion_slop/skeletons/entity/player/first_person.json` | 1 joint skeleton (11 joints) |
| `mod/src/main/resources/assets/locomotion_slop/sequences/entity/player/first_person/**` | 108 animation sequences |

**Provenance** — copied from the `Locomotion/` checkout in this repository
(`src/main/resources/assets/locomotion/skeletons|sequences/entity/player/first_person*`), with one
mechanical change: the asset namespace was rewritten from `locomotion:` to `locomotion_slop:` so the
two mods can coexist on disk. No keyframe values were edited.

**Author** — James Pelter (Trainguy9512), https://github.com/Trainguy9512/trainguys-animation-overhaul

**License — this is the part that matters.** The upstream repository is **All Rights Reserved**, not
GPLv3. From its `LICENSE` file:

> All Rights Reserved
>
> Copyright (c) 2026 James Pelter

and from its `README.md`:

> - Do not redistribute or publish compiled versions, forks, or ports of the mod publicly to places
>   such as Curseforge or Modrinth **without explicit written permission from me**.
> - The project is under an All Rights Reserved license for the time being until I begin publishing
>   builds on Modrinth and CurseForge.

(`gradle.properties` in that repository still says `mod.license = GPLv3`, which contradicts both the
`LICENSE` file and the README. Treat the README/LICENSE as authoritative and ask before relying on
the metadata.)

The Java code in `mod/src/main/java/com/locomotionslop/animation/**` is likewise a port of
Locomotion's animation engine, written against its JSON format and derived from its source. It
carries the same restriction.

**What you may do**

- ✅ Compile and play this locally, on your own machine or your own server.
- ✅ Modify the animations for your own use.
- ❌ Publish the jar, a fork, a modpack containing it, or the animation JSON anywhere public
  (CurseForge, Modrinth, GitHub, a file host) **without written permission from Trainguy9512**.

**How to remove the vendored data**

Delete `mod/src/main/resources/assets/locomotion_slop/skeletons/` and
`mod/src/main/resources/assets/locomotion_slop/sequences/`. Nothing else needs to change: the
loader finds no skeletons, `FirstPersonPlayerAnimator#getPose()` returns `null`, the
`ItemInHandRenderer` and `GameRenderer` injections decline to cancel anything, and first person
falls back to 100% vanilla behaviour. Third person is unaffected either way.

**How to supply your own data instead**

The loader reads through the vanilla `ResourceManager`, so any resource pack that provides
`assets/locomotion_slop/skeletons/**/*.json` and `assets/locomotion_slop/sequences/**/*.json`
(format version ≥ 5, Locomotion's export format) is picked up — including at runtime, without a
rebuild. Write your own rig, or obtain permission and use Locomotion's.

---

## 2. Fresh Animations Player Add-on — NOT included, must be installed separately

`Fresh Animations player/` in this repository is an early-access build by **FreshLX**. Its
`early-access.txt` states:

> Do not distribute! Do not include assets from this pack in your own resource pack, mod pack,
> server, or other!

Accordingly, **no asset from that pack is copied into `mod/`**. Nothing in
`mod/src/main/resources/` comes from Fresh Animations. The mod integrates with it at runtime only:

- `com.locomotionslop.compat.PlayerModelPackProbe` looks for `assets/minecraft/emf/cem/player.jem`
  (and friends) in the active resource packs and logs which pack provides them.
- Third-person rendering is never intercepted, so EMF plays the pack's models and animations
  exactly as it would with this mod uninstalled.

You must install the Fresh Animations Player Add-on yourself, from FreshLX, and follow its own
licensing terms.

---

## 3. EMF / ETF — used as an optional runtime dependency, source only as reference

`Entity_Model_Features-master/` and `Entity_Texture_Features-ETF-Main/` are checkouts of Traben's
EMF (v3.3.9) and ETF, present in this repository as reference material for the API calls in
`com.locomotionslop.compat`. Neither is vendored into `mod/`: EMF/ETF are declared `type="optional"`
dependencies in `neoforge.mods.toml` and are reached exclusively through reflection. Install them
separately from Modrinth/CurseForge.

---

## 4. Original parts of this project

Everything under `mod/src/main/java/com/locomotionslop/` **except** `animation/**` (the ported
engine), plus the build files, templates, lang files and this documentation, is original to this
project.
