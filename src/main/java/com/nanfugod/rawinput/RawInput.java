package com.nanfugod.rawinput;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = RawInput.MODID, version = RawInput.VERSION)
public class RawInput {
    public static final String MODID = "rawinput";
    public static final String VERSION = "1.0";

    private static Mouse activeMouse = null;
    private static List<Mouse> mice = new ArrayList<>();
    private static long[] lastMoveTime;
    private static boolean autoSelected = false;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        try {
            Controller[] cs = ControllerEnvironment.getDefaultEnvironment().getControllers();
            for (Controller c : cs) {
                if (c.getType() == Controller.Type.MOUSE) {
                    mice.add((Mouse) c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lastMoveTime = new long[mice.size()];

        FMLLog.info("Found " + mice.size() + " mouse devices:");
        for (int i = 0; i < mice.size(); i++) {
            FMLLog.info("  #" + i + ": " + mice.get(i).getName());
        }
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            RawInput.update();
        }
    }

    public static void update() {
        if (autoSelected) {
            activeMouse.poll();
            return;
        }

        long now = System.currentTimeMillis();

        for (int i = 0; i < mice.size(); i++) {
            Mouse m = mice.get(i);
            m.poll();

            // 2. 如果有移动
            int dx = m.getX().getPollData() != 0 ? 1 : 0;
            int dy = m.getY().getPollData() != 0 ? 1 : 0;

            if (dx != 0 || dy != 0) {
                lastMoveTime[i] = now;
            }
        }

        long bestTime = 0;
        int bestMouse = -1;
        for (int i = 0; i < lastMoveTime.length; i++) {
            if (lastMoveTime[i] > bestTime) {
                bestTime = lastMoveTime[i];
                bestMouse = i;
            }
        }

        if (bestMouse != -1 && now - bestTime < 200) {
            activeMouse = mice.get(bestMouse);
            autoSelected = true;

            FMLLog.info("✔ Auto-selected mouse: " + activeMouse.getName());
        }
    }
}
