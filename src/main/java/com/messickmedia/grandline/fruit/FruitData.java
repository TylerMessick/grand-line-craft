package com.messickmedia.grandline.fruit;

import java.util.List;

import static com.messickmedia.grandline.fruit.Bounty.*;

/** The fruits, their forms and their moves. Names and descriptions live in the lang file. */
public final class FruitData {
    private FruitData() {}

    private static Move m(String id, int cost, int cd, Bounty tier) { return new Move(id, cost, cd, tier); }
    private static FormInfo f(String id, Bounty tier, float drain, int color, Move... moves) { return new FormInfo(id, tier, drain, color, List.of(moves)); }

    public static final FruitInfo GOMU = new FruitInfo("gomu_gomu", FruitInfo.Type.PARAMECIA, 0xE0357A, "", List.of(
            f("gomu_base", ROOKIE, 0, 0xE0357A,
                    m("pistol", 8, 16, ROOKIE), m("gatling", 18, 80, ROOKIE), m("bazooka", 14, 60, ROOKIE),
                    m("whip", 12, 50, ROOKIE), m("rocket", 8, 36, ROOKIE), m("balloon", 14, 160, ROOKIE)),
            f("gear_second", SUPERNOVA, 1.4F, 0xFF4A6A,
                    m("jet_pistol", 7, 12, SUPERNOVA), m("jet_gatling", 18, 70, SUPERNOVA), m("jet_bazooka", 14, 50, SUPERNOVA),
                    m("jet_whip", 10, 40, SUPERNOVA), m("jet_stamp", 10, 30, SUPERNOVA), m("red_hawk", 24, 120, SUPERNOVA)),
            f("gear_third", WARLORD, 1.8F, 0xC77B5C,
                    m("gigant_pistol", 20, 60, WARLORD), m("gigant_bazooka", 26, 100, WARLORD), m("elephant_gun", 30, 120, WARLORD),
                    m("gigant_stamp", 22, 80, WARLORD), m("elephant_gatling", 34, 160, WARLORD), m("grizzly_magnum", 40, 220, WARLORD)),
            f("boundman", COMMANDER, 2.8F, 0x2B2B2B,
                    m("kong_gun", 24, 60, COMMANDER), m("rhino_schneider", 20, 50, COMMANDER), m("leo_bazooka", 30, 100, COMMANDER),
                    m("culverin", 24, 80, COMMANDER), m("kong_organ", 40, 200, COMMANDER), m("king_kong_gun", 60, 400, COMMANDER)),
            f("snakeman", COMMANDER, 2.8F, 0xB8323C,
                    m("snake_pistol", 10, 14, COMMANDER), m("jet_culverin", 20, 40, COMMANDER), m("python", 24, 80, COMMANDER),
                    m("hydra", 38, 160, COMMANDER), m("black_mamba", 34, 140, COMMANDER), m("king_cobra", 50, 300, COMMANDER)),
            f("gear_fifth", EMPEROR, 3.6F, 0xFFFFFF,
                    m("gigant", 30, 200, EMPEROR), m("dawn_whip", 24, 80, EMPEROR), m("dawn_rocket", 28, 100, EMPEROR),
                    m("fusen", 20, 120, EMPEROR), m("rubber_world", 40, 300, EMPEROR), m("bajrang_gun", 80, 600, EMPEROR))
    ));

    public static final FruitInfo PIKA = new FruitInfo("pika_pika", FruitInfo.Type.LOGIA, 0xFFE24A, "light_form", List.of(
            f("light", ROOKIE, 0, 0xFFE24A,
                    m("yasakani", 18, 60, ROOKIE), m("murakumo", 14, 100, ROOKIE), m("yata", 8, 30, ROOKIE),
                    m("light_kick", 14, 50, ROOKIE), m("laser", 18, 70, SUPERNOVA), m("flash", 10, 120, ROOKIE)),
            f("sun_burst", WARLORD, 2.0F, 0xFFF6C0,
                    m("yasakani_rain", 30, 120, WARLORD), m("murakumo", 14, 100, ROOKIE), m("yata", 8, 20, ROOKIE),
                    m("light_kick", 14, 40, ROOKIE), m("laser_volley", 32, 140, WARLORD), m("solar_lance", 60, 500, COMMANDER))
    ));

    public static final FruitInfo SEIRYU = new FruitInfo("uo_uo_seiryu", FruitInfo.Type.ZOAN, 0x3FA7D6, "flame_clouds", List.of(
            f("seiryu_human", ROOKIE, 0, 0x3FA7D6,
                    m("raimei_hakke", 14, 40, ROOKIE), m("kaifu", 14, 60, ROOKIE), m("flame_cloud", 10, 50, ROOKIE),
                    m("blast_breath", 18, 80, ROOKIE), m("thunder", 20, 100, SUPERNOVA), m("tatsumaki", 30, 200, SUPERNOVA)),
            f("seiryu_hybrid", SUPERNOVA, 1.0F, 0x2E7FB0,
                    m("raimei_hakke", 14, 36, ROOKIE), m("kaifu", 14, 50, ROOKIE), m("flame_cloud", 10, 40, ROOKIE),
                    m("boro_breath", 34, 140, SUPERNOVA), m("thunder", 20, 90, SUPERNOVA), m("kaen_hakke", 30, 120, WARLORD)),
            f("seiryu_dragon", WARLORD, 2.0F, 0x1F5F9A,
                    m("boro_breath", 34, 120, SUPERNOVA), m("kaifu_storm", 30, 120, WARLORD), m("cloud_lift", 24, 100, WARLORD),
                    m("bolo_breath", 24, 70, WARLORD), m("thunderstorm", 40, 200, COMMANDER), m("ragnaraku", 50, 300, COMMANDER))
    ));

    public static final List<FruitInfo> ALL = List.of(GOMU, PIKA, SEIRYU);

    public static FruitInfo byId(String id) {
        for (FruitInfo f : ALL) if (f.id().equals(id)) return f;
        return null;
    }

    /** Which fruit a move id belongs to (the first match). */
    public static FruitInfo fruitOf(String moveId) {
        for (FruitInfo f : ALL) for (FormInfo fo : f.forms()) for (Move m : fo.moves()) if (m.id().equals(moveId)) return f;
        return null;
    }
}
