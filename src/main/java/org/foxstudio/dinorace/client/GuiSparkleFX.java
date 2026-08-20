package org.foxstudio.dinorace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * Hiệu ứng nền GUI KHÔNG dùng shader — vẽ bằng GuiGraphics như cách mod vẫn làm:
 * ánh sáng xanh dưới đáy mờ dần lên trên (fillGradient) + các hạt lấp lánh tròn
 * (texture sparkle.png) tự bay lên từ dưới, LẮC RẤT NHẸ (chậm, biên độ nhỏ).
 */
public final class GuiSparkleFX {

    private static final ResourceLocation SPARKLE = new ResourceLocation(
            "dinorace", "textures/gui/race/sparkle.png");

    private static final int COUNT = 36;
    private static final Random RND = new Random();
    private static final float[] X = new float[COUNT];
    private static final float[] Y = new float[COUNT];
    private static final float[] SPEED = new float[COUNT];
    private static final float[] SIZE = new float[COUNT];
    private static final float[] WOBBLE_FREQ = new float[COUNT];
    private static final float[] WOBBLE_AMP = new float[COUNT];
    private static final float[] TWINKLE_FREQ = new float[COUNT];
    private static final float[] PHASE = new float[COUNT];

    private static long lastNanos;

    private GuiSparkleFX() {
    }

    static {
        for (int i = 0; i < COUNT; i++) {
            reset(i, RND.nextFloat());
        }
    }

    private static void reset(int i, float startY) {
        X[i] = RND.nextFloat();
        Y[i] = startY;
        SPEED[i] = 0.06f + RND.nextFloat() * 0.12f; // % màn hình/giây
        SIZE[i] = 3.0f + RND.nextFloat() * 4.0f;    // px (bán kính hiển thị)
        WOBBLE_FREQ[i] = 0.15f + RND.nextFloat() * 0.30f; // Hz — lắc CHẬM
        WOBBLE_AMP[i] = 0.004f + RND.nextFloat() * 0.010f; // biên độ rất nhỏ
        TWINKLE_FREQ[i] = 1.0f + RND.nextFloat() * 1.5f;
        PHASE[i] = RND.nextFloat() * 6.28f;
    }

    /**
     * Vẽ hiệu ứng phủ toàn màn hình (gọi trong render() của Screen, sau nền tối).
     */
    public static void render(GuiGraphics g, int screenW, int screenH) {
        long now = System.nanoTime();
        if (lastNanos == 0) {
            lastNanos = now;
        }
        float dt = (now - lastNanos) / 1_000_000_000f;
        lastNanos = now;
        if (dt <= 0f || dt > 0.1f) {
            dt = 0.016f;
        }
        float sec = now / 1_000_000_000f;

        // Ánh sáng xanh dưới đáy, mờ dần lên trên (hít thở nhẹ)
        float breathe = 0.85f + 0.15f * (float) Math.sin(sec * 0.8f);
        int glowTop = 0x00163366;
        int glowBot = ((int) (0x44 * breathe) << 24) | 0x3399FF;
        int glowH = Math.max(60, screenH / 3);
        g.fillGradient(0, screenH - glowH, screenW, screenH, glowTop, glowBot);

        // Hạt lấp lánh tròn bay lên từ dưới, lắc nhẹ, nhấp nháy riêng từng hạt
        for (int i = 0; i < COUNT; i++) {
            Y[i] += SPEED[i] * dt;
            if (Y[i] > 1.15f) {
                reset(i, -0.15f);
            }
            float wob = (float) Math.sin(sec * WOBBLE_FREQ[i] * 6.28f + PHASE[i]) * WOBBLE_AMP[i];
            float tw = 0.45f + 0.55f * (float) Math.sin(sec * TWINKLE_FREQ[i] + PHASE[i] * 2.0f);
            tw = Math.max(0.08f, tw);

            float cx = (X[i] + wob) * screenW;
            float cy = screenH * (1f - Y[i]);
            int d = (int) (SIZE[i] * 2.5f);
            g.setColor(0.80f, 0.93f, 1.0f, Math.min(1f, tw));
            g.blit(SPARKLE, (int) cx - d / 2, (int) cy - d / 2, d, d,
                    0f, 0f, 32, 32, 32, 32);
            g.setColor(1f, 1f, 1f, 1f);
        }
    }
}