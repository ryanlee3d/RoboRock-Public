# 165 Final Robo-Rock
A final project for CSUS 165 - Computer Game Architecture  
Ryan & Nick  
Robo-Rock is a Multiplayer FPS Bullet Hell  
Using TAGE: Another Tiny Game Engine developed by Professor Scott Gordon with help from CSUS students.  

1. buildTAGE.bat
2. compile.bat
3. run.bat  
Games on the same local network can run multiplayer. Select Host on the first game and then Join on consecutive games.

Watch a demo here: [https://www.youtube.com/watch?v=0ycpejxl0us](https://www.youtube.com/watch?v=0ycpejxl0us)
<img width="1263" height="708" alt="Screenshot 2026-05-13 213936" src="https://github.com/user-attachments/assets/e87c6830-d1f8-43f9-9308-5ba3c49aab3e" />
<img width="1263" height="706" alt="Screenshot 2026-05-13 223348" src="https://github.com/user-attachments/assets/25b94df6-aca7-4115-a538-a441a1adcefd" />
<img width="1256" height="707" alt="Screenshot 2026-05-13 214005" src="https://github.com/user-attachments/assets/c0a0b089-eb73-4859-9fc6-11e902e98744" />
<img width="1259" height="706" alt="Screenshot 2026-05-13 214142" src="https://github.com/user-attachments/assets/93bc5c31-f8ea-4e19-b389-7d6d411cd2f7" />
<img width="1255" height="707" alt="Screenshot 2026-05-13 213947" src="https://github.com/user-attachments/assets/f127bd16-b779-4ebc-a744-36927b70d526" />


Aliens are invading and you need to fend off waves of robot apes, once you clear a group of apes another ufo will drop more apes until the mothership appears. Finish off the last apes and ride the tractor beam to take the fight to their planet. A giant brain knocks you down to the alien infested field below, pick them off until one drops the grapple, use it to get back up to the brain and start the final battle. Defeat the brain and you win!

## Controls

### Menu Controls

- **Up Arrow / Gamepad Left Stick Up:** Move menu selection up / move vending selection up
- **Down Arrow / Gamepad Left Stick Down:** Move menu selection down / move vending selection down
- **Left Arrow / Gamepad Left Stick Left:** Change map selection left / move menu selection left
- **Right Arrow / Gamepad Left Stick Right:** Change map selection right / move menu selection right
- **Enter / Gamepad A Button:** Confirm menu option / buy selected vending item

### Robot Selection

- **A or Left Arrow / Gamepad Left Stick Left:** Choose previous robot
- **D or Right Arrow / Gamepad Left Stick Right:** Choose next robot
- **Enter / Gamepad A Button:** Confirm robot and start game
- **Escape:** Return to main menu

### Gameplay Controls

- **W / Gamepad Left Stick Up:** Move forward
- **S / Gamepad Left Stick Down:** Move backward
- **A / Gamepad Left Stick Left:** Strafe left
- **D / Gamepad Left Stick Right:** Strafe right
- **Mouse Movement / Gamepad Right Stick:** Look and aim
- **Left Mouse Button / Gamepad Right Trigger or Right Bumper:** Fire weapon
- **R / Gamepad X Button:** Reload
- **E / Gamepad B Button:** Grapple / Enter Vending Machine / Exit Vending Machine
- **Mouse Wheel Up / Gamepad Y Button:** Switch to next weapon
- **Mouse Wheel Down / Gamepad Left Bumper:** Switch to previous weapon
- **B:** Toggle plasma rifle fire mode
- **F:** Toggle first-person mode
- **Escape:** Pause game
- **Enter / Gamepad A Button:** Continue after death up to 3 lives

### Overhead Camera Controls

- **Numpad +:** Zoom overhead camera in
- **Numpad -:** Zoom overhead camera out
- **1:** Vending buy full health / Gamepad A Button when selected
- **2:** Vending buy full ammo / Gamepad A Button when selected
- **3:** Vending buy damage multiplier upgrade up to 4 times / Gamepad A Button when selected
- **4:** Vending buy ammo multiplier upgrade up to 4 times / Gamepad A Button when selected
- **Arrow Keys:** Pan overhead camera
- **C:** Recenter overhead camera
- **P:** Toggle physics debug view

### Debugging Controls

- **Backslash:** Restart game / return to menu
- **T:** Queue final UFO tractor beam test
- **Y:** Spawn debug skinny loadout
- **U:** Force level two arrival event

## Asset Credits

### Textures

- **Map 0 Terrain:** `coast_sand_rocks_02_diff_1k.jpg`  
  Coast Sand Rocks 02 Texture, Poly Haven  
  License: CC0

- **Map 1 Terrain:** `airbase_radar_panels.jpg`  
  Golgotha Textures: airbase_radar_panels.jpg, OpenGameArt.org  
  License: CC0 1.0 Universal

- **Bullets:** https://www.freepik.com/free-vector/gold-metal-background_34294136.htm  
  Credit: juicy_fish

- **Plasma Bullets:** https://www.freepik.com/free-vector/abstract-hand-painted-alcohol-ink-background-design_50500296.htm  
  Credit: kjpargeter

- **Crosshairs:** https://www.hiclipart.com/free-transparent-background-png-clipart-mryvr

- **Brain Texture:** `brain.jpg`  
  Texture was saved from Blender after importing the Brain Run `.glb` file by LostBoyz2078 and finding its base color in the shading tab.

- **Futuristic Skyscraper Texture:** `centerBuilding.jpg`  
  Texture was derived from the Futuristic Skyscraper model.  
  Credit: MOjackal

- **Sci-Fi Buildings Pack Texture:**  
  Texture was derived from the Sci-Fi Buildings Pack model.  
  Credit: Golukumar

- **Sci-Fi Building Texture:**  
  Texture was derived from the Sci-Fi Building model.

- **Robot Texture Variants:** `robot1.jpg`, `robot2.jpg`, `robot3.jpg`  
  https://pinetools.com/invert-image-colors

### Models

- **Robocop** — used to create `robot.obj` and `robot.jpg`  
  https://sketchfab.com/3d-models/robocop-b37bee32032d468c9c1cbd041e8142c1

- **Plasma Rifle** — used to create `plasmaRifle.obj` and `plasmaRifle.jpg`  
  https://sketchfab.com/3d-models/plasma-rifle-bf439e8a1f8e41888d7e94592c352807

- **Pistol** — used to create `pistol.obj`  
  https://www.turbosquid.com/FullPreview/922989

- **Terminal / Vending Machine** — used to create `Terminal UV_1.obj` and `uv_checker_material_uv_grid_2048x2048_Roug.png`  
  Sci-fi Terminal  
  Credit: Tronin Dmitry / @kosmotron  
  Source ID: 78ac943

- **Skinwalker Creature**  
  https://sketchfab.com/3d-models/skinwalker-creature-1536d87ab17b4d9e889d9195f5ceca0b

- **Apeman Futura**  
  https://sketchfab.com/3d-models/apeman-futurastargate-5c334a1f9d8e4f87b62be7e62e71606e

- **Low-Poly Vertical DBS**  
  https://sketchfab.com/3d-models/low-poly-vertical-dbs-709ba08245a24b34b1495e4f46255e9f  
  Credit: Streljay_Ataman

- **Futuristic Modular Rifle**  
  https://sketchfab.com/3d-models/futuristic-modular-rifle-2389f96cfac442b58272a489d7cdd606  
  Credit: Ramhat

- **UFO**  
  https://sketchfab.com/3d-models/ufo-75655f43c4fc4c56ab60746caf7119fe  
  Credit: slebsom

- **Futuristic Skyscraper**  
  https://sketchfab.com/3d-models/futuristic-skyscraper-b79ec00a53cf4b72ac0e8d6f055bc21c  
  Credit: MOjackal

- **Sci-Fi Buildings Pack**  
  https://sketchfab.com/3d-models/sci-fi-buildings-pack-cc74d81f4e0e4db3acf6f3433c678250  
  Credit: Golukumar

- **Sci-Fi Building**  
  https://sketchfab.com/3d-models/sci-fi-building-0cc82a32dfb84e858f76813e1c42b268

- **Grapple Gun**  
  https://sketchfab.com/3d-models/grapple-gun-aeac4217c98a4bb489688a65fe831d74  
  Credit: Al Garcia

- **Brain Run** — used to create `brain.obj` and `brain.jpg`  
  https://sketchfab.com/3d-models/brain-run-a84217d7f44146d2ad22c018ab9976ed  
  Credit: LostBoyz2078  
  License: CC non-commercial

- **Animations:**  
  Animations were made from `robot.obj`, `ape.obj`, `skinny.obj`, and `brain.obj` using Professor Gordon’s `.rkm`, `.rks`, and `.rka` file exporter.

### Sounds

- **Ammo Pick-Up:** https://freesound.org/people/Dpoggioli/sounds/213607/

- **Health Pick-Up:** https://freesound.org/people/juancamiloorjuela/sounds/204318/

- **Shotgun Glock:** https://freesound.org/people/LuannWepener/sounds/326119/  
  Credit: LuannWepener

- **Shotgun Shoot Only:** https://freesound.org/people/bolkmar/sounds/455915/

- **Pistol Shoot Only / Rifle:** https://freesound.org/people/bolkmar/sounds/455922/  
  Credit: bolkmar

- **Weapon Swap:** https://freesound.org/people/twisterman/sounds/159450/  
  Credit: twisterOrtiz

- **Robo Gun:** `Sytrus,rsmpl,multiprcsng)serial.wav`  
  https://freesound.org/people/newlocknew/sounds/514074/  
  Credit: Newlocknew

- **Shotgun and Pistol:** https://f8studios.itch.io/snakes-authentic-gun-sounds  
  Credit: SnakeF8

- **Ape Laser Gun:** https://pixabay.com/sound-effects/film-special-effects-laser-zap-90575/  
  License: https://pixabay.com/service/license-summary/

- **Ape Death:** https://pixabay.com/sound-effects/film-special-effects-death-408455/

- **Alien Death:** https://pixabay.com/sound-effects/film-special-effects-alien-high-pitch-312010/

- **Brain Roar:** https://pixabay.com/sound-effects/film-special-effects-epic-dragon-roar-364481/

- **Player Death:** https://pixabay.com/sound-effects/film-special-effects-retro-video-game-death-95730/

### Game Music

- **Main Menu / Level 1:**  
  https://pixabay.com/music/video-games-90s-game-music-no-copyright-352850/

- **Level 2:**  
  https://pixabay.com/music/video-games-spaceship-arcade-shooter-game-background-soundtrack-318508/

- **Boss Music:**  
  https://pixabay.com/music/upbeat-video-game-boss-fiight-259885/

- **Pixabay Game Music:**  
  Free to use.

### Skyboxes

- **Planet Surface Skyboxes:** https://screamingbrainstudios.itch.io/planet-surface-skyboxes  
  License: CC0

- **Space Skyboxes:** OpenGameArt.org  
  License: CC0 / CC0 1.0 Universal

- **Cubemap Splitter Tool:** https://screamingbrainstudios.itch.io/cubemap-splitter
