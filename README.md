# Multi Item Frame
This mod adds some Item Frames which has a few features.

## Features
- Toggle to show the background and transparent.
- Showing item can be put from inventory or JEI.
  - Right click to open the GUI.
  - Put the item into the setting slot.
  - Middle-click the slot to erase.
- Highlight color setting.
  - Click to toggle the mode button. No highlight / Frame / Fill background
  - Set the highlighting color
    - Click to toggle colors button
    - Or drag the dye into the button from inventory or JEI.
- Copy the settings and paste to another with;
  - **Memory Card** from AE2 (`ae2:memory_card`)
  - **Configuration Card** from Mekanism (`mekanism:configuration_card`)
- Shows the frame's contents (item / Fluid / Chemical / Energy) in **Jade**'s tooltip when looked at.


## Items
`1x2` and `2x1` / `1,2` and `2,1` are convertable by single craft.
  - e.g., put the `1,2` frame into the craft grid -> `2,1` is craftable, vice-versa.
### Multi Item Frame 1x1 (multiitemframe:frame_1x1)
```
#r
```
  - `#`: Item Frame (`minecraft:item_frame`)
  - `r`: Redstone Dust (`minecraft:redstone`)
### Multi Item Frame 1x2 (multiitemframe:frame_1x2)
Vertical stack version.
```
@
@
```
  - `@`: Multi Item Frame 1x1 (`multiitemframe:frame_1x1`)
### Multi Item Frame 2x1 (multiitemframe:frame_2x1)
Horizontal stack version.
```
@@
```
  - `@`: Multi Item Frame 1x1 (`multiitemframe:frame_1x1`)
### Multi Item Frame 1,2 (multiitemframe:frame_1and2)
Top side has 1 slot, bottom has 2.
#### Craft Recipe
```
@
@@
```
```
@
&
```
  - `@`: Multi Item Frame 1x1 (`multiitemframe:frame_1x1`)
  - `&`: Multi Item Frame 1x2 (`multiitemframe:frame_1x2`)
### Multi Item Frame 2,1 (multiitemframe:frame_2and1)
Top side has 2 slots, bottom has 1.
#### Craft Recipe
```
@@
@
```
```
&
@
```
  - `@`: Multi Item Frame 1x1 (`multiitemframe:frame_1x1`)
  - `&`: Multi Item Frame 1x2 (`multiitemframe:frame_1x2`)
### Multi Item Frame 2x2 (multiitemframe:frame_2x2)
#### Craft Recipe
```
@@
@@
```
```
&&
```
```
$
$
```
  - `@`: Multi Item Frame 1x1 (`multiitemframe:frame_1x1`)
  - `&`: Multi Item Frame 1x2 (`multiitemframe:frame_1x2`)
  - `$`: Multi Item Frame 2x1 (`multiitemframe:frame_2x1`)
### Glowing version
Also has glowing version (`multiitemframe:glow_frame_{size}`). This emits light as Level 9.
`Glowing Multi Item Frame 1x1` is craftable with Non-glow + Glowstone Dust (shapeless). Other sizes are using glowing correspondings.

## Dependencies
### Required
N/A
### Optional
- [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2)
- [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei)
- [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism)
- [Jade](https://www.curseforge.com/minecraft/mc-mods/jade)

## Supported targets
| Module         | Mod Loader | Minecraft |
| -------------- | ---------- | --------- |
| `neoforge`     | NeoForge   | 1.21.1    |
| `forge`        | Forge      | 1.20.1    |
