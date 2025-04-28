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

## Testing with Forge Development Environment (No Minecraft Required)

If you want to test the mod without purchasing Minecraft, you can use the Forge development client:

1. Make sure you've completed the "Building the Mod" steps above
2. Run the following command in your mod directory:
   ```
   .\gradlew.bat runClient
   ```
3. This will launch a development version of Minecraft with your mod already installed
4. The development client gives you full access to test all mod features
5. You can record this gameplay using screen recording software like OBS Studio

### Recording Your Mod Gameplay:

1. Download and install OBS Studio from: https://obsproject.com/
2. Setup basic recording:
   - Open OBS Studio
   - Add a "Display Capture" source to capture your screen
   - Add an "Audio Output Capture" to record game sounds
3. Start the Forge development client using `.\gradlew.bat runClient`
4. Click "Start Recording" in OBS before testing your mod
5. Demonstrate all features of your Nether Gauntlet mod:
   - Show crafting the item
   - Demonstrate fireball shooting
   - Show explosion effects on enemies
   - Demonstrate lava surfing
   - Show the ring of fire ability
   - Demonstrate dimension teleportation if possible
6. Save the recording and edit as needed for your hackathon submission

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
