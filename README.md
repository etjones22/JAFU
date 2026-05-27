# JAFU

JAFU (Just A Few Updates) is a client-side Fabric mod scaffold for Minecraft 1.21.11.

The mod currently adds a `/jafu` client command that opens a custom menu inspired by the dense, two-panel style of NEU, without copying NEU assets or code.

## Features

- `/jafu` opens the module menu.
- `/jafu layout` opens a drag-and-drop HUD editor for moving tracker panels.
- Powder Chest Tracker records Crystal Hollows treasure chest rewards.
- Etherwarp Helper shows held AOTE/AOTV timing information.

## Build

Use Java 21 or newer:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
.\gradlew.bat build
```

The built jar will be in `build/libs/`.

## Notes

Hypixel treats most client-side quality-of-life mods as "use at your own risk." Keep features informational and avoid automation, macros, packet manipulation, or gameplay decisions made for the player.
