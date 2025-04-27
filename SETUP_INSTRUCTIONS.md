# Nether Gauntlet Mod Setup Instructions

## Prerequisites

Before you can build and run the Nether Gauntlet mod, you need to install the following:

1. **Java Development Kit (JDK) 17**
   - Download from: https://adoptium.net/temurin/releases/?version=17
   - Select the Windows x64 installer
   - Run the installer and follow the instructions
   - Make sure to check the option to set JAVA_HOME environment variable

2. **Minecraft Java Edition**
   - You need to own and have installed Minecraft Java Edition

## Building the Mod

After installing JDK 17:

1. Open Command Prompt or PowerShell in the mod directory
2. Run: `.\gradlew.bat genEclipseRuns` (or `.\gradlew.bat genIntellijRuns` if you use IntelliJ IDEA)
3. Run: `.\gradlew.bat build`
4. The compiled mod will be in the `build/libs` directory

## Running the Mod

1. Install Minecraft Forge for version 1.19.2
   - Download from: https://files.minecraftforge.net/net/minecraftforge/forge/index_1.19.2.html
   - Run the installer and select "Install client"

2. Copy the mod JAR file from `build/libs` to:
   - `%APPDATA%\.minecraft\mods` folder

3. Launch Minecraft with the Forge profile

## Creating the Texture

Before running the mod, you need to create a texture for the Nether Gauntlet:

1. Create a 16x16 pixel image following the guide in:
   - `src/main/resources/assets/nethergauntlet/textures/item/nether_gauntlet.txt`
2. Save it as `nether_gauntlet.png` in:
   - `src/main/resources/assets/nethergauntlet/textures/item/`

## Using the Nether Gauntlet

Once in-game:

1. Craft the Nether Gauntlet using:
   - 4 Blaze Rods
   - 4 Netherite Ingots
   - 1 Magma Block

2. Use the gauntlet's abilities:
   - **Punch enemies** to create explosions
   - **Right-click** to shoot fireballs
   - **Walk on lava** while holding the gauntlet to surf on it

Enjoy your new powerful Nether Gauntlet!
