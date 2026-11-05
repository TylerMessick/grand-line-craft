# Grand Line Craft (NeoForge 1.21.1)

One Piece devil fruits for Minecraft. Part 1 ships three fruits, each with every ability it shows in the anime:
the **Gomu Gomu no Mi** (all five Gears, Boundman and Snakeman included), the **Uo Uo no Mi, Model: Seiryu**
(hybrid and full azure dragon) and the **Pika Pika no Mi** (a true Logia body of light). Every fruit is a real 3D
item model built from cubes with its own swirl pattern, and every attack is drawn in 3D: stretched rubber limbs,
haki-black fists, lasers, dragon breath, lightning and voxel clouds.

## Build and play
1. Install **Java 21** (Temurin JDK 21).
2. `./gradlew runClient` (Windows: `gradlew.bat runClient`) to test. The first run downloads Minecraft + NeoForge.
3. `./gradlew build` → `build/libs/grandline-1.0.0.jar` into the `mods` folder of a **NeoForge 21.1.x** profile.
   Pushing to `main` also builds the jar on GitHub Actions and publishes it to the `jar` branch.

## Getting a fruit
- Devil fruits turn up in **shipwreck**, **buried treasure** and **end city** chests (12% each, configurable).
- Creative tab "Grand Line Craft", or `/fruit give <fruit>` (op).
- **Eat it.** It tastes horrible (nausea), then the power is yours. Eating a second fruit kills you.
- `/pirate` shows your fruit, form, bounty and passive.

## Controls (rebind under Options → Controls → Grand Line Craft)
| Key | Action |
|---|---|
| Z R G H J Y | Moves 1–6 of your **current form** |
| V | Shift form: Gears for rubber, Human → Hybrid → Dragon for the zoan, Light → Sun Burst for the logia |
| B | Fruit toggle: **Light Form** (Pika), **Flame Clouds** (Seiryu) |

The HUD left of the hotbar shows the fruit, the active form, bounty and tier progress, a stamina bar and the
key chips (grey = cooldown, lock = bounty too low). The move list of the current form sits at the left edge.

## Rules every fruit user lives by
- **Stamina** (0–100) pays for moves. Gears and transformed forms drain it every second; hit zero and your body
  gives out: Gear Second leaves you winded, Gear Fifth leaves you collapsed and weak for 20 seconds.
- **The sea hates you.** In water you sink like a stone, your limbs go limp (slowness, weakness), you drown fast,
  every form drops and no move works until you dry off. (`seaWeakness` in the config.)
- **Bounty.** Kills raise your bounty in Berries (hostile mobs ~1–3M, the Warden 150M, the Wither 300M, the Ender
  Dragon 600M, other fruit users a quarter of theirs). Tiers: Rookie → **Supernova** (50M) → **Warlord** (100M) →
  **Yonko Commander** (500M) → **Emperor** (1.5B). Forms and moves unlock by tier; creative mode ignores tiers.
- Optional (`loseFruitOnDeath`): die and the power leaves you; the fruit drops where you fell.

## Gomu Gomu no Mi — Paramecia
Rubber body: immune to lightning and fall damage, blunt hits (unarmed mobs, explosions) do 45%.

| Form (V) | Tier | Moves (Z R G H J Y) |
|---|---|---|
| Rubber | Rookie | Pistol · Gatling · Bazooka · Whip · Rocket (grapple/launch) · Balloon (reflects projectiles, bounces blunt hits) |
| Gear Second | Supernova | Jet Pistol · Jet Gatling · Jet Bazooka · Jet Whip · Jet Stamp · **Red Hawk** (haki + fire). Speed, steam, drains stamina; fatigue when it ends |
| Gear Third | Warlord | Gigant Pistol · Gigant Bazooka · **Elephant Gun** · Gigant Stamp · Elephant Gatling · Grizzly Magnum. Giant fists on the model, slower, armored |
| Gear Fourth: Boundman | Commander | Kong Gun · Rhino Schneider · Leo Bazooka · Culverin (homing) · Kong Organ · **King Kong Gun**. Haki muscle shell, always bouncing, flight, +20 HP |
| Gear Fourth: Snakeman | Commander | Jet Pistol · Jet Culverin · Python · Hydra · Black Mamba · **King Cobra**. Fastest form, every fist homes and curves |
| Gear Fifth | Emperor | Gigant (grow giant) · Dawn Whip · Dawn Rocket · Fusen · **Rubber World** (everything near you bounces) · **Bajrang Gun**. White glow, cloud wreath, the Drums of Liberation; collapse when it ends |

## Uo Uo no Mi, Model: Seiryu — Mythical Zoan
Fire immune, regenerates. Hybrid: +40 HP, armored, 1.5× size, horns, scales and a tail. Dragon: +100 HP, 3.4×
size, the full serpentine azure dragon model replaces you, you fly on flame clouds, and every melee hit knocks.

| Form (V) | Tier | Moves |
|---|---|---|
| Human | Rookie | Raimei Hakke (Thunder Bagua) · Kaifu (wind scythe) · Flame Clouds · Blast Breath · Dragon's Thunder · Tatsumaki |
| Hybrid Dragon | Supernova | Raimei Hakke · Kaifu · Flame Clouds · **Boro Breath** · Dragon's Thunder · Shoryu: Kaen Hakke |
| Azure Dragon | Warlord | Boro Breath · Kaifu Storm · Flame Clouds: Lift (raise everything, then drop it) · Bolo Breath · Thunderstorm · **Ragnaraku** |

B: **Flame Clouds** — flight on burning clouds in human and hybrid form.

## Pika Pika no Mi — Logia
Body of light: arrows, blades, blows, explosions and fire pass straight through you. Only Haki (hardened fruit
moves, the Warden, the Wither, the Ender Dragon), the sea and exhaustion can land a hit. Faster on foot, +2 attack.

| Form (V) | Tier | Moves |
|---|---|---|
| Light | Rookie | Yasakani no Magatama (light bullets) · Ama no Murakumo (light blade: melee burns and cuts for 12 s) · Yata no Kagami (light-speed teleport, behind a target or 48 blocks) · Light Speed Kick · Laser Beam · Blinding Flash |
| Sun Burst | Warlord | Yasakani Rain (from the sky) · Ama no Murakumo · Yata no Kagami · Light Speed Kick · Laser Volley · **Solar Lance** |

B: **Light Form** — light-speed flight, glowing, a trail of light.

## Testing commands (op)
`/fruit set <fruit>` · `/fruit give <fruit>` · `/fruit clear` · `/fruit bounty <tier|berries>` · `/fruit refill`

## Config
`saves/<world>/serverconfig/grandline-server.toml`: block damage, damage multiplier, stamina regen, sea weakness,
lose fruit on death, chest chance.

## Project layout
- `fruit/FruitData` — the fruits, forms and moves (ids, costs, cooldowns, tiers). Names in `lang/en_us.json`.
- `fruit/moves/*` — one class per fruit; every move is a function over a `Cast`.
- `fruit/Systems` — passives, drains, sea weakness, logia/rubber defences, bounty, eating.
- `fruit/Vfx` + `net/Fx` + `client/FxClient` — the 3D effect primitives (arm, beam, arc, ring, puff, bolt, sphere).
- `client/FormLayer`, `client/DragonModel` — what the forms look like on the player.
- `gen/gen_assets.py` — regenerates every texture and the fruit models.
- `preview/index.html` — the 3D preview page (fruits, dragon, forms, moves).
