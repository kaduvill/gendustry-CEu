1.6.6.0

- Industrial Apiary should no longer query six sides for redstone every tick when redstone mode does not need it
- Skip cover ticking in Industrial Apiaries without installed covers
- Added The One Probe integration based on the original Waila integration design
- Adopt Forestry Apiary flower cache refresh when opening Industrial Apiary GUI
- Fixed Advanced Mutatron sapling duplication when double-clicking the output (upstream #299)
- Improved Industrial Apiary performance when inventories are full by skipping unnecessary NBT comparisons
- Fixed cover duplication when breaking and replacing machines (upstream #240, #261)
- Fixed startup crash when langfile metadata is unavailable (upstream #271, #274, #276, #277, #279, #283, #284, #286, #291, #292, #296; PR #280).