# DegradeExplodedBlocks

## Basics

### What does this mod do?
This mod causes some blocks that are dropped as a result of TNT, creepers, or
other explosions to be dropped as a different item than they originally were.

### How do I use this mod?
You need Minecraft Forge installed first. Once that's done, just drop
degradeexplodedblocks-*version*.jar in your Minecraft instance's mods/
directory. Optionally, you can configure it to taste (see below).

### What settings does this mod have?
You can choose exactly which replacements are made. The first two columns of
each entry are the old block's name and a state predicate, the same as
/testforblock uses. The second two columns are the new block's name and a data
value or block state. The final column is optional. Examples:
- minecraft:cobblestone * minecraft:gravel
- minecraft:stone variant=granite minecraft:sand variant=red_sand

The first entry will cause any cobblestone destroyed by an explosion to be
dropped as gravel. The second entry will cause any granite destroyed by an
explosion to be dropped as red sand.

### Why do destroyed blocks often not appear?
Not every block destroyed by an explosion will be dropped. The chance of a drop
 is inversely proportional to the explosion's power, so blocks destroyed by TNT
will be dropped 1/4 of the time, and blocks destroyed by creepers will be
dropped 1/3 of the time. This is a vanilla mechanic and this mod does not
change it.

## Development

### How do I compile this mod from source?
You need a JDK installed first. Start a command prompt or terminal in the
directory you downloaded the source to. If you're on Windows, type
`gradlew.bat build`. Otherwise, type `./gradlew build`. Once it's done, the mod
will be saved to build/libs/degradeexplodedblocks-*version*.jar.

### How can I contribute to this mod's development?
Send pull requests. Note that by doing so, you agree to release your
contributions under this mod's license.

## Licensing/Permissions

### What license is this released under?
It's released under the GPL v2 or later.

### Can I use this in my modpack?
Yes, even if you monetize it with adf.ly or something, and you don't need to
ask me for my permission first.
