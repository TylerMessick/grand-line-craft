package com.messickmedia.grandline.client;

import com.messickmedia.grandline.net.ModNetwork;

import java.util.HashMap;
import java.util.Map;

/** Client copy of the local player's fruit state (HUD) and everyone's current form (render layers). */
public final class ClientState {
    private ClientState() {}

    public static String fruit = "";
    public static String form = "";
    public static float stamina = 100;
    public static boolean toggle;
    public static int bounty;
    public static int bountyProgress;
    public static long berries;
    public static final int[] cooldownPct = new int[8];
    public static int lockMask;
    public static final String[] moves = new String[6];
    public static int exhaust;
    public static boolean wet;
    public static int formColor = 0xFFFFFF;

    public static final Map<Integer, ModNetwork.FormView> VIEWS = new HashMap<>();

    public static boolean hasFruit() {
        return !fruit.isEmpty();
    }

    public static ModNetwork.FormView view(int entityId) {
        return VIEWS.get(entityId);
    }

    public static void accept(ModNetwork.Sync s) {
        fruit = s.fruit();
        form = s.form();
        stamina = s.stamina();
        toggle = s.toggle();
        bounty = s.bounty();
        bountyProgress = s.bountyProgress();
        berries = s.berries();
        System.arraycopy(s.cd(), 0, cooldownPct, 0, 8);
        lockMask = s.lockMask();
        System.arraycopy(s.moves(), 0, moves, 0, 6);
        exhaust = s.exhaust();
        wet = s.wet();
        formColor = s.formColor();
    }

    public static void accept(ModNetwork.FormView v) {
        if (v.fruit().isEmpty()) VIEWS.remove(v.entityId());
        else VIEWS.put(v.entityId(), v);
    }

    public static void clear() {
        VIEWS.clear();
        fruit = "";
        form = "";
    }
}
