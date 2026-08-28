# User Test Checklist Template

This is a reusable template for release-readiness user testing. It is **not** meant to be
copied into a version-specific file and committed to the repository — instead:

1. Copy the checklist body below into the release PR description (the PR created for
   `develop` or `release/vX.Y.Z` → `main`), filling in the "Changes in this release" section
   with the actual features/fixes being validated.
2. Have the user run through the checklist and record results directly in the PR body/comments.
3. Once the release PR is merged, the filled-in checklist lives in the PR history — no
   separate file needs to be kept in the repository.

---

## User Test Checklist — vX.Y.Z

### Changes in this release

- (list the features/fixes being validated in this release)

Target loaders: **Forge 1.20.1** / **NeoForge 1.21.1**
(verify on both unless a change is clearly loader-specific)

---

### Prerequisites

- [ ] Mod jar deployed to the test environment for the target loader/version
- [ ] Applied Energistics 2 installed (required for Memory Card tests)
- [ ] Mekanism installed (required for Configuration Card tests)
- [ ] JEI installed (required for drag-and-drop tests)
- [ ] At least one item of each frame size available in creative/survival inventory, plus a
      Redstone Dust and Glowstone Dust for crafting, and a few dye items of different colors

---

### 1. Crafting

- [ ] `Multi Item Frame 1x1` crafts from an Item Frame + Redstone Dust
- [ ] Two `1x1` frames combine into a `1x2` or `2x1` frame depending on orientation in the grid
- [ ] `1x2`/`2x1` and `1-and-2`/`2-and-1` frames convert into each other by placing them together
      in the crafting grid as described in the README
- [ ] `2x2` crafts correctly from each of the documented patterns (four `1x1`, two `1x2`, two `2x1`)
- [ ] Crafting any non-glowing frame with Glowstone Dust produces the corresponding glowing frame

---

### 2. Placement and GUI

- [ ] Placing a frame in the world behaves like a vanilla Item Frame (attaches to a solid block
      face, drops if the supporting block is removed)
- [ ] Right-clicking a placed frame opens the settings screen
- [ ] Dragging an item from the player inventory into a frame slot displays it in-world
- [ ] Middle-clicking (creative clone) a filled frame slot in the GUI clears that slot instead of
      duplicating the item
- [ ] The background-visibility toggle button shows/hides the frame's background
- [ ] The mode-toggle button cycles a slot's highlight mode (none → frame → fill → none)
- [ ] The color-toggle button cycles a slot's highlight color through all dye colors and back to
      no color
- [ ] Closing and reopening the screen (or relogging) preserves all slot contents, background
      visibility, and highlight settings

---

### 3. JEI integration

- [ ] With the settings screen open, dragging an item from JEI's ingredient list directly onto a
      frame slot places it in that slot (same as dragging from the player inventory)
- [ ] Dragging a dye item from JEI directly onto a slot's color-toggle button sets that slot's
      highlight color to the dragged dye's color
- [ ] No crash or stuck-drag state occurs when dragging a non-dye ingredient over a color button
      (the drop is simply ignored)

---

### 4. Applied Energistics 2 — Memory Card

- [ ] Shift-right-clicking a frame with an AE2 Memory Card saves that frame's settings (slot
      contents excluded — only highlight modes/colors and background visibility) to the card, with
      a chat message confirming the save
- [ ] Right-clicking a **different** frame with that card pastes the saved settings onto it, with a
      chat confirmation
- [ ] Right-clicking a frame with a Memory Card that has no saved Multi Item Frame data shows an
      "invalid" message instead of applying anything
- [ ] Behavior is identical on Forge 1.20.1 and NeoForge 1.21.1 despite AE2's internal API
      differences between the two versions

---

### 5. Mekanism — Configuration Card

- [ ] Shift-right-clicking a frame with a Mekanism Configuration Card saves that frame's settings
      to the card, with a chat confirmation, and the card's vanilla tooltip shows it now "has data"
- [ ] Right-clicking a **different** frame with that card pastes the saved settings onto it, with a
      chat confirmation
- [ ] Right-clicking a frame with a Configuration Card holding unrelated (non-Multi-Item-Frame)
      data shows an "invalid" message instead of applying anything
- [ ] Behavior is identical on Forge 1.20.1 and NeoForge 1.21.1

---

### 6. Compatibility

- [ ] No crash log when Multi Item Frame is installed alone (no AE2, Mekanism, or JEI present)
- [ ] No crash log when only one of AE2 / Mekanism / JEI is installed alongside Multi Item Frame
- [ ] Holding a AE2 Memory Card or Mekanism Configuration Card and right-clicking a frame does
      **not** open the settings screen (the card interaction takes over instead)
- [ ] Holding any other item and right-clicking a frame opens the settings screen as normal

---

### Result summary

| Section | Forge 1.20.1 | NeoForge 1.21.1 | Notes |
| --- | --- | --- | --- |
| 1. Crafting | OK / NG | OK / NG | |
| 2. Placement and GUI | OK / NG | OK / NG | |
| 3. JEI integration | OK / NG | OK / NG | |
| 4. AE2 Memory Card | OK / NG | OK / NG | |
| 5. Mekanism Configuration Card | OK / NG | OK / NG | |
| 6. Compatibility | OK / NG | OK / NG | |

If any item is NG, attach the relevant environment's `logs/latest.log`.
