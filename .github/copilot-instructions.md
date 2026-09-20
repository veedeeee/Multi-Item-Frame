
# Copilot Instructions

## Project Overview
- The user-facing overview is in the `README.md`. The assistant should read that file to understand the project and its goals.

## Mod features and dependencies
- This mod has no required dependencies.
- If the player also installed Applied Energistics 2, the mod will support the copying and pasting the Item Frame settings with `ae2:memory_card`
- If the player also installed Mekanism, the mod will support the copying and pasting the Item Frame settings with `mekanism:configuration_card`
- If the player also installed Just Enough Items (JEI), this mod allows to choose the showing item from the JEI interface.

## Development Commands
- Build all modules: `.\gradlew.bat build --console=plain`
- Compile common + NeoForge only: `.\gradlew.bat :common:compileJava :neoforge:compileJava --console=plain`
- Run NeoForge client: `.\gradlew.bat :neoforge:runClient --console=plain`
- Run Forge client: `.\gradlew.bat :forge:runClient --console=plain`

### Test Environments
- `D:\curseforge\minecraft\Instances`
  - `MultiIF-Forge 1.20.1\`
  - `MultiIF-NeoForge 1.21.1\`
