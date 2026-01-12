# TV Mod for Minecraft 1.16.5

Minecraft Forge mod that adds a functional TV block capable of playing YouTube videos.

## Features

- YouTube video playback using MCEF browser
- Spatial audio - volume decreases with distance
- Full playback controls - play, pause, stop, volume
- Multiplayer support - synchronized playback
- Persistent settings - URL, volume, screen size saved
- Multiple screen sizes - 1, 2, 4, 6, 8, 10, 12 blocks
- Multiple video sources - YouTube, Invidious instances
- Playback speed control - 0.25x to 2x

## Requirements

- Minecraft 1.16.5
- Forge 36.2.x or higher
- MCEF (Minecraft Chromium Embedded Framework) mod

## Installation

1. Install Minecraft Forge 1.16.5
2. Download MCEF mod
3. Download tvmod-1.0.0.jar
4. Place both mods in your `mods` folder

## Usage

1. Craft a TV using glass and iron ingots
2. Place the TV block
3. Right-click to open the control GUI
4. Enter a YouTube URL and click Play

## Building

```bash
./gradlew build
```

The built jar will be in `build/libs/`

## License

All rights reserved.
