package org.foxstudio.dinorace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Màn hình "System Installing": overlay tối dần → ảnh winged.png hiện dần ở
 * giữa → chữ "SYSTEM INSTALLING" bên dưới → 1 thanh tiến trình duy nhất (giữa,
 * dưới chữ) chạy bằng experience_bar_background / experience_bar_progress, sau
 * đó mở GUI chọn chủng tộc.
 */
public class SystemInstallingScreen extends Screen {

    private static final ResourceLocation WING =
            new ResourceLocation("dinorace", "textures/gui/race/winged.png");
    private static final ResourceLocation BAR_BG =
            new ResourceLocation("dinorace", "textures/gui/race/experience_bar_background.png");
    private static final ResourceLocation BAR_FILL =
            new ResourceLocation("dinorace", "textures/gui/race/experience_bar_progress.png");

    private final long openedAt;

    public SystemInstallingScreen() {
        super(Component.literal("System Installing"));
        this.openedAt = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float t = (System.currentTimeMillis() - openedAt) / 1000.0f;

        // Sau khi fill xong (t=4.0) → mờ dần ra đen (curtain 0.6s) rồi mới mở GUI chọn tộc
        float dim = Math.max(0f, Math.min(1f, (t - 4.0f) / 0.6f));
        float texDim = 1f - dim;

        // Overlay tối dần trong 1.2s
        float fade = Math.min(1.0f, t / 1.2f);
        int alpha = (int) (0xF0 * fade);
        g.fill(0, 0, this.width, this.height, (alpha << 24) | 0x000000);

        if (t > 1.0f) {
            // Winged.png (vuông 736x736): căn GIỮA chính xác cửa sổ, hiện dần sau khi màn tối
            int sh = Math.min((int) (this.width * 0.38f), this.height - 140);
            int sw = sh;
            int bx = (this.width - sw) / 2;
            int by = (this.height - sh) / 2;

            // Hiện dần nhanh (1.0→1.6s) → mờ đi → hiện lại liên tục (chu kỳ 1.2s) như cũ
            float texAlpha;
            if (t <= 1.6f) {
                texAlpha = (t - 1.0f) / 0.6f;
            } else {
                texAlpha = 0.65f + 0.35f * (float) Math.sin((t - 1.6f) / 1.2f * Math.PI * 2.0 + Math.PI / 2.0);
            }
            texAlpha = Math.max(0f, Math.min(1f, texAlpha));
            texAlpha *= texDim;

            g.setColor(1f, 1f, 1f, texAlpha);
            g.blit(WING, bx, by, sw, sh, 0f, 0f, 736, 736, 736, 736);
            g.setColor(1f, 1f, 1f, 1f);

            // Chữ "SYSTEM INSTALLING" bên dưới ảnh
            String txt = "SYSTEM INSTALLING";
            int textY = by + sh + 26;
            g.drawString(this.font, txt,
                    (this.width - this.font.width(txt)) / 2, textY,
                    0xFF00FF66, false);

            // 1 thanh tiến trình duy nhất, giữa, ngay dưới chữ — cao 5px (đúng tỉ lệ 182x5),
            // bề ngang tự co giãn theo cửa sổ
            int barW = (int) (sw * 0.9f);
            int barH = 5;
            int barX = (this.width - barW) / 2;
            int barY = textY + 14;
            g.blit(BAR_BG, barX, barY, barW, barH, 0f, 0f, 182, 5, 182, 5);

            float progress = Math.max(0f, Math.min(1f, (t - 1.6f) / 2.4f));
            if (progress > 0f) {
                int fillW = (int) (barW * progress);
                g.blit(BAR_FILL, barX, barY, fillW, barH, 0f, 0f, (int) (182 * progress), 5, 182, 5);
            }
        }

        // Curtain mờ ra đen sau khi loading xong
        if (dim > 0f) {
            int da = (int) Math.min(240, dim * 240);
            g.fill(0, 0, this.width, this.height, (da << 24) | 0x000000);
        }

        // Mờ xong (t=4.6) → mở GUI chọn tộc
        if (t > 4.6f) {
            Minecraft.getInstance().setScreen(new RaceSelectScreen());
        }

        super.render(g, mouseX, mouseY, partialTick);
    }
}