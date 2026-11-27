# Frenchie outfits in Blockbench

This folder in the active Frenchies checkout is the working location for the outfit projects. Open the `.bbmodel` files from here and use **Ctrl+S** to save back to the repo. The projects load their textures from the PNGs beside them using relative paths. The fawn coat, brindle coat, and outfit atlas are shared by the projects that use them. Keep the PNGs with the models when moving or sharing the folder.

Use this repo folder as the only working copy. Screenshots and temporary exports can live outside the checkout.

1. Use **File → Open Model** in Blockbench, or double-click a `.bbmodel`.
2. In **Edit** mode, expand **Outliner → root → body**. Look under **head** for headwear and goggles, **chest** for clothing and backpacks, and the four **leg** groups for boots or flippers.
3. Select a piece in the Outliner and use **Move** or **Resize**. Use the eye icon on the glass group to get a clear view of nearby parts while you work.
4. Save the edited `.bbmodel` and any changed linked PNGs. If you’re working with Codex, ask it to apply the saved outfit edits to the mod and verify them in-game.
5. Apply the changes to the matching Java model and runtime textures as needed. Review the diff, preview the affected outfit in Minecraft, and commit the editable files and corresponding runtime changes together.

The mod loads its outfit geometry from [AdventureOutfitModels.java](../../../src/main/java/net/j40climb/frenchies/common/entity/frenchie/client/AdventureOutfitModels.java), its carrier geometry from [FrontpackModel.java](../../../src/main/java/net/j40climb/frenchies/common/entity/frenchie/frontpack/client/FrontpackModel.java), and its dog and outfit textures from the [runtime texture folder](../../../src/main/resources/assets/florafauna/textures/entity/frenchie/). Saving in Blockbench updates the editable files. The Gradle build reads the Java models and runtime textures directly.

## What is here

- **Captain, Scuba, Mining, Winter, Nether, Astronaut, Aviator** are the automatic outfit looks. Captain includes the longer boating head and life jacket. Scuba includes four flippers and its air tank. Astronaut and Scuba have separately editable clear panes.
- **Explorer** and **Pirate** are previews for the structure-map gear. Each includes its headwear, satchel, rolled map and compass charm.
- **Front Pack – Upright** and **Front Pack – Elytra** show the two pouch fits. The pouch and straps are editable cubes. The dog's preview mesh is posed so you can check head and paw clearance. Edit the standalone **Aviator** file for changes to the cap, goggles or scarf before adapting them to the carried pose.

The outfit projects use Blockbench's **Generic Model** format for separate outfit, coat and glass textures. They’re editing references for the current gear. The game supplies the automatic outfit selection, lighting and extra motion. The original animated rig is available for preview in the standalone outfit files.
