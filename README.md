# Advancements Tracker

![Advancements Tracker][logo]

Advancements Tracker is a Minecraft Forge Client Mod which give you an overlay which shows you the current process of your advancements to easier follow them.
It will also add additional features to make archiving the advancements more interesting and meaningful.

**WARNING: This version is still in development, so it could include some error and ugly UI parts.**

## Features

### Advancements Overview

Could be open with **STRG + L** and displays all current advancements and allow the tracking of single advancements.

![Advancement Overview][overview_example]

### Advancements Widget

Could be show/hide with **ALT + L** and displays the currently tracked advancements and their progress.

![Advancement Widget][widget_example]

### Automatic Screenshots

As soon you finished an advancements it will automatically take a screenshot.

## FAQ

### Is this a server side / client side mod ?

Its a pure client mode, but should not harm if it is installed on the server.

### Does it work with data packs ?

If the data pack is not using a custom advancement system it should work out of the box.
I recommending the [Expanded Advancement and Recipe Pack][platys_advancement_and_recipe_pack] data pack which adds a plenty of advancements and recipes.

### Do you plan to support 12.x, 13.x, 14.x, 15.x or a Fabric/Rift version ?

Unfortunately not because of my time constrains and missing knowledge about the api / engine mechanics in these versions.

### How can I adjust the position and size of the widget ?

You can adjust this settings in the config with percentage values.
E.g. left = 1 means 100% from left, 0.01 means 1% from left.

### Is this mod compatible with Better Advancements ?

This mod works great together with Better Advancements mod, but let me know if you get any issues.

## Recommended additional Mods

- [Better Advancements][better_advancements] to improve the UI and UX for the advancements system in minecraft

[logo]: logo.png
[overview_example]: example/overview_example.png
[widget_example]: example/widget_example.png
[better_advancements]: https://www.curseforge.com/minecraft/mc-mods/better-advancements
[platys_advancement_and_recipe_pack]: https://www.curseforge.com/minecraft/customization/platys-advancement-and-recipe-pack
