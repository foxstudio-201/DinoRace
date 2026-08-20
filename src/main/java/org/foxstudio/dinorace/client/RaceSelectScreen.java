package org.foxstudio.dinorace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.foxstudio.dinorace.network.RaceNetwork;
import org.foxstudio.dinorace.network.RaceSelectPacket;
import org.foxstudio.dinorace.race.RaceData;
import org.foxstudio.dinorace.race.RaceInfo;
import org.foxstudio.dinorace.race.RacePower;
import org.foxstudio.dinorace.race.RaceSelectConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI chọn chủng tộc (carousel ngang, 5 thẻ / trang). Hover thẻ phóng to mượt;
 * khi chọn: ẩn thẻ khác, thẻ trượt sang trái, hiện thông tin tộc bên phải + nút Xác nhận.
 * Chỉ hiện các tộc được bật trong config race_select.json.
 */
public class RaceSelectScreen extends Screen {

    private static final ResourceLocation FRAME =
            new ResourceLocation("dinorace", "textures/gui/race/class.png");
    private static final ResourceLocation FRAME_HOVER =
            new ResourceLocation("dinorace", "textures/gui/race/class_hover.png");
    private static final ResourceLocation TILE =
            new ResourceLocation("dinorace", "textures/gui/race/tile_0014.png");
    private static final int B = 10;
    private static final int GAP = 10;
    private static final int MARGIN = 24;
    /** Vùng trống 2 bên dành riêng cho nút mũi tên — thẻ lùi vào trong, không đè/trùng hit với nút. */
    private static final int SIDE_PAD = 56;
    private static final float SLIDE_MS = 320f;
    private static final float SLIDE_IN_MS = 420f;
    private static final float STAGGER_MS = 130f;
    /** Chừ 600ms cho hiệu ứng nền (sparkle) chạy trước rồi thẻ mới bắt đầu trượt vào — tránh lag frame đầu. */
    private static final long CARD_WARMUP_MS = 600L;
    /** Số thẻ hiển thị cùng lúc trong 1 trang (scroll ngang theo trang). */
    private static final int PER_PAGE = 5;
    private static final ResourceLocation ARROW_L =
            new ResourceLocation("dinorace", "textures/gui/race/arrow_left.png");
    private static final ResourceLocation ARROW_R =
            new ResourceLocation("dinorace", "textures/gui/race/arrow_right.png");
    private static final int ARROW_SIZE = 32;

    private final long openedAt;
    /** Các index tộc (vào RaceData) được bật trong config, theo thứ tự hiển thị. */
    private final int[] order;
    private int selected = -1;
    private int selectedPos = -1;
    private long selectAt = 0;
    private int infoLoadedFor = -1;
    private String infoName = "";
    private String infoDesc = "";
    private final List<RacePower> infoPowers = new ArrayList<>();
    private int infoScroll;
    private final float[] cardScale = new float[RaceData.NAMES.length];
    /** Trượt ngang của carousel: offset hiện tại (px) trượt mượt về targetOffset. */
    private float offset = 0f;
    private float targetOffset = 0f;

    public RaceSelectScreen() {
        super(Component.literal("Chọn chủng tộc"));
        this.openedAt = System.currentTimeMillis();
        List<Integer> tmp = new ArrayList<>();
        for (int i = 0; i < RaceData.NAMES.length; i++) {
            if (RaceSelectConfig.isEnabled(i)) {
                tmp.add(i);
            }
        }
        this.order = tmp.stream().mapToInt(Integer::intValue).toArray();
        for (int i = 0; i < cardScale.length; i++) {
            cardScale[i] = 1.0f;
        }
        preloadTextures();
    }

    /** Nạp trước toàn bộ texture của thẻ vào GPU (ngay khi mở screen) để frame đầu vẽ không bị lag. */
    private void preloadTextures() {
        try {
            var tm = Minecraft.getInstance().getTextureManager();
            for (int i : order) {
                tm.getTexture(new ResourceLocation("dinorace",
                        "textures/gui/race/" + RaceData.textureFile(i)));
            }
            tm.getTexture(FRAME);
            tm.getTexture(FRAME_HOVER);
            tm.getTexture(TILE);
            tm.getTexture(ARROW_L);
            tm.getTexture(ARROW_R);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int cardWidth() {
        int n = PER_PAGE;
        int maxW = this.width - MARGIN * 2 - SIDE_PAD * 2;
        int w = (maxW - (n - 1) * GAP) / n;
        // Chỉ cap theo chiều cao khi tràn (màn hình thấp), tránh đè chữ tiêu đề
        int maxH = (int) (this.height * 0.80f);
        int h = (int) (w * 1.5f) + 30;
        if (h > maxH) {
            h = maxH;
            w = (int) ((h - 30) / 1.5f);
        }
        return w;
    }

    private int cardHeight() {
        return (int) (cardWidth() * 1.5f) + 30;
    }

    private int gridTop() {
        return (this.height - cardHeight()) / 2 - 10;
    }

    /** X trái của dãy thẻ (đã lùi vào trong để chừa chỗ cho nút mũi tên). */
    private int rowLeft() {
        return MARGIN + SIDE_PAD;
    }

    /** Y dọc của nút mũi tên (canh giữa theo chiều cao thẻ). */
    private int arrowY() {
        return gridTop() + (cardHeight() - ARROW_SIZE) / 2;
    }

    /* ---- Khung "content" chứa thẻ: full chiều cao màn hình, nút mũi tên nằm ngoài ---- */
    private int contentLeft() {
        return rowLeft();
    }

    private int contentRight() {
        return this.width - rowLeft();
    }

    private int contentTop() {
        return 0;
    }

    private int contentBottom() {
        return this.height;
    }

    /** Kiểm tra điểm (mx,my) có nằm TRONG khung content (chỗ chứa thẻ) không. */
    private boolean inContent(double mx, double my) {
        return mx >= contentLeft() && mx <= contentRight()
                && my >= contentTop() && my <= contentBottom();
    }

    private int rowWidth() {
        int n = order.length;
        int w = cardWidth();
        return n * w + (n - 1) * GAP;
    }

    private int maxScroll() {
        return Math.max(0, rowWidth() - (this.width - rowLeft() * 2));
    }

    /** Scan ngang từng thẻ (nút mũi tên và bánh xe con lăn): 1 nấc = 1 thẻ. */
    private void nudgeScroll(int dir) {
        targetOffset = Math.max(0f, Math.min(maxScroll(), targetOffset + dir * (cardWidth() + GAP)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            int cardW = cardWidth();
            int cardH = cardHeight();
            int gy = gridTop();
            // Nút mũi tên 2 bên: chỉ chuyển trang, KHÔNG chọn thẻ
            if (selected < 0) {
                int ay = arrowY();
                if (targetOffset > 1f
                        && mx >= MARGIN && mx <= MARGIN + ARROW_SIZE
                        && my >= ay && my <= ay + ARROW_SIZE) {
                    nudgeScroll(-1);
                    return true;
                }
                if (targetOffset < maxScroll() - 1f
                        && mx >= this.width - MARGIN - ARROW_SIZE && mx <= this.width - MARGIN
                        && my >= ay && my <= ay + ARROW_SIZE) {
                    nudgeScroll(1);
                    return true;
                }
            }
            if (selected >= 0) {
                int px = MARGIN + cardW + GAP;
                int pw = this.width - MARGIN - px;
                int panelBottom = this.height - MARGIN - 42;
                int btnW = 130;
                int btnH = 32;
                int btnGap = 12;
                int total = btnW * 2 + btnGap;
                int startX = px + (pw - total) / 2;
                int btnY = panelBottom + 10;
                int backX = startX;
                int confX = startX + btnW + btnGap;
                if (mx >= backX && mx <= backX + btnW && my >= btnY && my <= btnY + btnH) {
                    selected = -1;
                    infoScroll = 0;
                    return true;
                }
                if (mx >= confX && mx <= confX + btnW && my >= btnY && my <= btnY + btnH) {
                    selectRace(selected);
                    return true;
                }
                return true;
            }
            if (inContent(mx, my)) {
                int gx = contentLeft();
                int off = Math.round(offset);
                for (int pos = 0; pos < order.length; pos++) {
                    int i = order[pos];
                    int x = gx + pos * (cardW + GAP) - off;
                    if (mx >= x && mx <= x + cardW && my >= gy && my <= gy + cardH) {
                        selected = i;
                        selectedPos = pos;
                        selectAt = System.currentTimeMillis();
                        infoScroll = 0;
                        loadOriginInfo(i);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (selected >= 0) {
            int step = 14;
            int max = infoScrollMax();
            infoScroll = Math.max(0, Math.min(max, infoScroll - (int) Math.signum(amount) * step));
            return true;
        }
        // Chưa chọn tộc: bánh xe lăn = scroll ngang carousel (1 thẻ / 1 nấc)
        if (maxScroll() > 0) {
            nudgeScroll(amount > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    /** Số dòng nội dung info (tên + mô tả + quyền năng + mô tả power). */
    private int infoLineCount() {
        int cardW = cardWidth();
        int px = MARGIN + cardW + GAP;
        int pw = this.width - MARGIN - px;
        int contentW = pw - 32;
        int lines = 1; // tên
        lines += this.font.split(fromFormattedString(infoDesc), contentW).size();
        if (!infoPowers.isEmpty()) {
            lines += 1; // header
            for (RacePower rp : infoPowers) {
                lines += this.font.split(fromFormattedString("- " + rp.name), contentW).size();
                if (rp.description != null && !rp.description.isEmpty()) {
                    lines += this.font.split(fromFormattedString("    " + rp.description), contentW).size();
                }
            }
        }
        return lines;
    }

    /** Giới hạn cuộn toàn bộ nội dung info. */
    private int infoScrollMax() {
        int totalH = infoLineCount() * 10;
        int areaH = (this.height - MARGIN - 42) - 50 - 16;
        return Math.max(0, totalH - areaH);
    }

    private void selectRace(int index) {
        RaceDetailScreen.markChosen(index); // nhớ tộc người chơi đã chọn (mở bằng phím tắt)
        RaceNetwork.CHANNEL.sendToServer(new RaceSelectPacket(RaceData.ORIGINS[index]));
        this.onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xF0000000);

        // Hiệu ứng nền: ánh sáng xanh dưới đáy + hạt lấp lánh bay lên (vẽ bằng GuiGraphics)
        GuiSparkleFX.render(g, this.width, this.height);

        int cardW = cardWidth();
        int cardH = cardHeight();
        int gy = gridTop();

        if (selected < 0) {
            String title = "Chọn chủng tộc khởi đầu";
            g.drawString(this.font, title, (this.width - this.font.width(title)) / 2, 22,
                    0xFFFFCC44, false);
            int cl = contentLeft();
            int cr = contentRight();
            int ct = contentTop();
            int cb = contentBottom();

            int n = order.length;
            int gx = cl;
            int max = maxScroll();
            targetOffset = Math.max(0f, Math.min(max, targetOffset));
            if (max > 0) {
                offset += (targetOffset - offset) * 0.22f;
                if (Math.abs(targetOffset - offset) < 0.5f) {
                    offset = targetOffset;
                }
            } else {
                offset = 0f;
                targetOffset = 0f;
            }
            int off = (int) Math.round(offset);
            long now = System.currentTimeMillis();
            int center = n / 2;
            g.enableScissor(cl, ct, cr, cb);
            for (int pos = 0; pos < n; pos++) {
                int i = order[pos];
                int x = gx + pos * (cardW + GAP) - off;
                // Culling: bỏ thẻ nằm ngoài màn hình
                if (x > this.width || x + cardW < 0) {
                    continue;
                }
                // Trượt từ trên xuống: thẻ giữa trước → 2 thẻ cạnh → 2 thẻ ngoài cùng
                float delay = Math.abs(pos - center) * STAGGER_MS;
                float p = Math.max(0f, Math.min(1f,
                        (now - openedAt - CARD_WARMUP_MS - delay) / SLIDE_IN_MS));
                if (p <= 0f) {
                    continue;
                }
                float eased = 1f - (1f - p) * (1f - p) * (1f - p);
                int y = gy + (int) (-(cardH + 80) * (1f - eased));
                boolean over = inContent(mouseX, mouseY)
                        && mouseX >= x && mouseX <= x + cardW
                        && mouseY >= y && mouseY <= y + cardH;
                float target = over ? 1.08f : 1.0f;
                cardScale[i] += (target - cardScale[i]) * 0.18f;
                float s = cardScale[i];
                if (Math.abs(s - 1.0f) > 0.004f) {
                    int cx = x + cardW / 2;
                    int cy = y + cardH / 2;
                    g.pose().pushPose();
                    g.pose().translate(cx, cy, 0.0f);
                    g.pose().scale(s, s, 1.0f);
                    g.pose().translate(-cx, -cy, 0.0f);
                    drawCard(g, i, x, y, cardW, cardH, over);
                    g.pose().popPose();
                } else {
                    drawCard(g, i, x, y, cardW, cardH, over);
                }
            }
            g.disableScissor();
            drawArrows(g, max);
        } else {
            float p = Math.min(1f, (System.currentTimeMillis() - selectAt) / SLIDE_MS);
            float eased = 1f - (1f - p) * (1f - p) * (1f - p);
            int startX = rowLeft() + selectedPos * (cardW + GAP) - (int) Math.round(offset);
            int finalX = rowLeft();
            int cx = (int) (startX + (finalX - startX) * eased);
            drawCard(g, selected, cx, gy, cardW, cardH, false);

            if (p > 0.6f) {
                int px = finalX + cardW + GAP;
                int pw = this.width - MARGIN - px;
                int panelTop = 50;
                int panelBottom = this.height - MARGIN - 42;
                drawDetails(g, selected, px, panelTop, pw, panelBottom - panelTop, mouseX, mouseY);
            }
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    /** 2 nút mũi tên nhỏ 2 bên màn hình, nằm trong vùng SIDE_PAD riêng (không chồng lên thẻ). */
    private void drawArrows(GuiGraphics g, int maxScroll) {
        int ay = arrowY();
        boolean canL = targetOffset > 1f;
        boolean canR = targetOffset < maxScroll - 1f;
        if (canL) {
            g.blit(ARROW_L, MARGIN, ay, ARROW_SIZE, ARROW_SIZE, 0f, 0f, 1, 1, 1, 1);
        } else {
            g.setColor(1f, 1f, 1f, 0.25f);
            g.blit(ARROW_L, MARGIN, ay, ARROW_SIZE, ARROW_SIZE, 0f, 0f, 1, 1, 1, 1);
            g.setColor(1f, 1f, 1f, 1f);
        }
        int rx = this.width - MARGIN - ARROW_SIZE;
        if (canR) {
            g.blit(ARROW_R, rx, ay, ARROW_SIZE, ARROW_SIZE, 0f, 0f, 1, 1, 1, 1);
        } else {
            g.setColor(1f, 1f, 1f, 0.25f);
            g.blit(ARROW_R, rx, ay, ARROW_SIZE, ARROW_SIZE, 0f, 0f, 1, 1, 1, 1);
            g.setColor(1f, 1f, 1f, 1f);
        }
    }

    private void drawCard(GuiGraphics g, int i, int x, int y, int cardW, int cardH, boolean over) {
        blitNineSlice(g, over ? FRAME_HOVER : FRAME, x, y, cardW, cardH, B, 30);
        ResourceLocation img = new ResourceLocation("dinorace",
                "textures/gui/race/" + RaceData.textureFile(i));
        int pad = 14;
        int availW = cardW - pad * 2;
        int availH = cardH - 60;
        int iw = availW;
        int ih = (int) (availW * 1.5f);
        if (ih > availH) {
            ih = availH;
            iw = (int) (availH * 2f / 3f);
        }
        int imgX = x + (cardW - iw) / 2;
        int imgY = y + pad;
        g.blit(img, imgX, imgY, iw, ih, 0f, 0f, 1, 1, 1, 1);

        // Tên tộc
        String name = RaceData.NAMES[i];
        g.drawString(this.font, name,
                x + (cardW - this.font.width(name)) / 2, y + cardH - 44,
                over ? 0xFFFFCC66 : 0xFFFFFFFF, false);
    }

    private void drawDetails(GuiGraphics g, int i, int px, int py, int pw, int ph, int mouseX, int mouseY) {
        blitNineSlice(g, TILE, px, py, pw, ph, 10, 32);

        int innerX = px + 16;
        int areaTop = py + 8;
        int areaBottom = py + ph - 8;
        int areaH = areaBottom - areaTop;
        int contentW = pw - 32;

        // Xếp toàn bộ nội dung (tên, mô tả, quyền năng) thành 1 danh sách cuộn được
        java.util.List<Line> entries = new ArrayList<>();
        entries.add(new Line(FormattedCharSequence.forward(infoName, net.minecraft.network.chat.Style.EMPTY), 0xFFFFCC44));
        for (FormattedCharSequence seq : this.font.split(fromFormattedString(infoDesc), contentW)) {
            entries.add(new Line(seq, 0xFFE0E0E0));
        }
        if (!infoPowers.isEmpty()) {
            entries.add(new Line(FormattedCharSequence.forward("Quyền năng:", net.minecraft.network.chat.Style.EMPTY), 0xFF8888CC));
            for (RacePower rp : infoPowers) {
                for (FormattedCharSequence seq : this.font.split(fromFormattedString("- " + rp.name), contentW)) {
                    entries.add(new Line(seq, 0xFFFFCC66));
                }
                if (rp.description != null && !rp.description.isEmpty()) {
                    for (FormattedCharSequence seq : this.font.split(fromFormattedString("    " + rp.description), contentW)) {
                        entries.add(new Line(seq, 0xFFBBBBBB));
                    }
                }
            }
        }

        int totalH = entries.size() * 10;
        int maxScroll = Math.max(0, totalH - areaH);
        infoScroll = Math.max(0, Math.min(infoScroll, maxScroll));

        g.enableScissor(px + 4, areaTop, px + pw - 4, areaBottom);
        int y = areaTop - infoScroll;
        for (Line ln : entries) {
            if (y + 10 > areaTop && y < areaBottom) {
                g.drawString(this.font, ln.seq, innerX, y, ln.color, false);
            }
            y += 10;
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int thumbH = Math.max(16, (int) (areaH * (areaH / (float) totalH)));
            int thumbY = areaTop + Math.round((areaH - thumbH) * (infoScroll / (float) maxScroll));
            g.fill(px + pw - 6, areaTop, px + pw - 4, areaBottom, 0x22FFFFFF);
            g.fill(px + pw - 6, thumbY, px + pw - 4, thumbY + thumbH, 0x88FFB84D);
        }

        // Nút Trở về + Xác nhận nằm NGOÀI panel, dùng texture class 9-slice
        int btnW = 130;
        int btnH = 32;
        int btnGap = 12;
        int total = btnW * 2 + btnGap;
        int startX = px + (pw - total) / 2;
        int btnY = py + ph + 10;
        int backX = startX;
        int confX = startX + btnW + btnGap;
        boolean overBack = mouseX >= backX && mouseX <= backX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        boolean overConf = mouseX >= confX && mouseX <= confX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        blitNineSlice(g, overBack ? FRAME_HOVER : FRAME, backX, btnY, btnW, btnH, B, 30);
        blitNineSlice(g, overConf ? FRAME_HOVER : FRAME, confX, btnY, btnW, btnH, B, 30);
        String backLabel = "Trở về";
        String confLabel = "Xác nhận";
        g.drawString(this.font, backLabel,
                backX + (btnW - this.font.width(backLabel)) / 2, btnY + (btnH - 8) / 2,
                0xFFFFFFFF, false);
        g.drawString(this.font, confLabel,
                confX + (btnW - this.font.width(confLabel)) / 2, btnY + (btnH - 8) / 2,
                0xFFFFFFFF, false);
    }

    /** Parse chuỗi có mã § thành Component (để hiển thị màu/bold). */
    private static Component fromFormattedString(String s) {
        net.minecraft.network.chat.MutableComponent comp = Component.empty();
        net.minecraft.network.chat.Style current = net.minecraft.network.chat.Style.EMPTY;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00A7' && i + 1 < s.length()) {
                if (buf.length() > 0) {
                    comp.append(Component.literal(buf.toString()).withStyle(current));
                    buf.setLength(0);
                }
                net.minecraft.ChatFormatting fmt = net.minecraft.ChatFormatting.getByCode(s.charAt(i + 1));
                if (fmt != null) {
                    current = current.applyLegacyFormat(fmt);
                }
                i++;
            } else {
                buf.append(c);
            }
        }
        if (buf.length() > 0) {
            comp.append(Component.literal(buf.toString()).withStyle(current));
        }
        return comp;
    }

    private static final class Line {
        final FormattedCharSequence seq;
        final int color;

        Line(FormattedCharSequence seq, int color) {
            this.seq = seq;
            this.color = color;
        }
    }

    /** Lấy nội dung tộc từ config server gửi (RaceClientData). */
    private void loadOriginInfo(int index) {
        if (infoLoadedFor == index) {
            return;
        }
        infoLoadedFor = index;
        RaceInfo ri = RaceClientData.get(index);
        if (ri != null) {
            infoName = ri.name;
            infoDesc = ri.description == null ? "" : ri.description;
            infoPowers.clear();
            if (ri.powers != null) {
                infoPowers.addAll(ri.powers);
            }
        } else {
            infoName = RaceData.NAMES[index];
            infoDesc = "";
            infoPowers.clear();
        }
    }

    /** 9-slice từ nguồn kích thước s (30=class, 32=tile), border b. */
    private static void blitNineSlice(GuiGraphics g, ResourceLocation tex,
                                      int x, int y, int w, int h, int b, int s) {
        g.blit(tex, x, y, b, b, 0f, 0f, b, b, s, s);
        g.blit(tex, x + w - b, y, b, b, s - b, 0f, b, b, s, s);
        g.blit(tex, x, y + h - b, b, b, 0f, s - b, b, b, s, s);
        g.blit(tex, x + w - b, y + h - b, b, b, s - b, s - b, b, b, s, s);
        g.blit(tex, x + b, y, w - 2 * b, b, b, 0f, s - 2 * b, b, s, s);
        g.blit(tex, x + b, y + h - b, w - 2 * b, b, b, s - b, s - 2 * b, b, s, s);
        g.blit(tex, x, y + b, b, h - 2 * b, 0f, b, b, s - 2 * b, s, s);
        g.blit(tex, x + w - b, y + b, b, h - 2 * b, s - b, b, b, s - 2 * b, s, s);
        g.blit(tex, x + b, y + b, w - 2 * b, h - 2 * b, b, b, s - 2 * b, s - 2 * b, s, s);
    }
}