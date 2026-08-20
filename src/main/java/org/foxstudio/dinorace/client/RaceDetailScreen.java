package org.foxstudio.dinorace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.foxstudio.dinorace.DinoRace;
import org.foxstudio.dinorace.race.RaceData;
import org.foxstudio.dinorace.race.RaceDetailConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * GUI thông tin tộc — mở ra ngay sau khi chọn tộc xong.
 * Giao diện dạng SÁCH 2 TRANG (UI_TravelBook_BookPageLeft01a/Right01a), tự co theo màn hình.
 * Tab kiểu tab_above (left/middle/right) sát mép trên sách, nhô lên khỏi mép trên;
 * icon (chi_so/ky_nang/iten_hoa) trên tab, hover mới hiện chữ.
 * - Tab Chỉ Số/Kỹ Năng: cuộn được; bar dùng experience_bar_*.
 * - Tab Tiến Hóa: giữ nền cũ bên trong (tile + winged), thanh UI_TravelBook_Fill01a/b
 *   + cây điểm kéo/phóng to/thu nhỏ/click như puffish-skills.
 * Dữ liệu từ config riêng từng tộc (RaceDetailConfig).
 */
public class RaceDetailScreen extends Screen {

    /** Tộc người chơi đang dùng (được set khi chọn tộc / mở GUI) — dùng cho phím tắt. */
    private static int lastRaceIndex = -1;
    private static boolean pendingOpen = false;

    public static int lastRaceIndex() {
        return lastRaceIndex;
    }

    public static void markChosen(int index) {
        lastRaceIndex = index;
        if (pendingOpen && index >= 0) {
            pendingOpen = false;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new RaceDetailScreen(index));
            }
        }
    }

    public static void requestOpen() {
        pendingOpen = true;
        org.foxstudio.dinorace.network.RaceNetwork.CHANNEL.sendToServer(new org.foxstudio.dinorace.network.RaceRequestPacket());
    }

    private static final ResourceLocation BAR_BG = new ResourceLocation("dinorace", "textures/gui/race/experience_bar_background.png");
    private static final ResourceLocation BAR_FILL = new ResourceLocation("dinorace", "textures/gui/race/experience_bar_progress.png");
    private static final ResourceLocation EVO_BG = new ResourceLocation("dinorace", "textures/gui/race/UI_TravelBook_Fill01a.png");
    private static final ResourceLocation EVO_FILL = new ResourceLocation("dinorace", "textures/gui/race/UI_TravelBook_Fill01b.png");
    private static final ResourceLocation TILE = new ResourceLocation("dinorace", "textures/gui/race/tile_0014.png");
    private static final ResourceLocation CLASS = new ResourceLocation("dinorace", "textures/gui/race/class.png");
    private static final ResourceLocation LOCK_ICON = new ResourceLocation("dinorace", "textures/gui/race/locked_button.png");
    private static final ResourceLocation UNLOCK_ICON = new ResourceLocation("dinorace", "textures/gui/race/unlocked_button.png");
    private static final ResourceLocation CLASS_HOVER = new ResourceLocation("dinorace", "textures/gui/race/class_hover.png");
    private static final ResourceLocation DIRT_BG = new ResourceLocation("dinorace", "textures/gui/team/light_dirt_background.png");
    private static final ResourceLocation WING_BG = new ResourceLocation("dinorace", "textures/gui/race/winged.png");
    private static final ResourceLocation BOOK_LEFT = new ResourceLocation("dinorace", "textures/gui/race/UI_TravelBook_BookPageLeft01a.png");
    private static final ResourceLocation BOOK_RIGHT = new ResourceLocation("dinorace", "textures/gui/race/UI_TravelBook_BookPageRight01a.png");
    private static final ResourceLocation[] TAB_BG = {
            new ResourceLocation("dinorace", "textures/gui/race/tab_above_left.png"),
            new ResourceLocation("dinorace", "textures/gui/race/tab_above_middle.png"),
            new ResourceLocation("dinorace", "textures/gui/race/tab_above_middle.png"),
    };
    private static final ResourceLocation[] TAB_BG_SEL = {
            new ResourceLocation("dinorace", "textures/gui/race/tab_above_left_selected.png"),
            new ResourceLocation("dinorace", "textures/gui/race/tab_above_left_selected.png"),
            new ResourceLocation("dinorace", "textures/gui/race/tab_above_left_selected.png"),
    };
    private static final ResourceLocation[] TAB_ICONS = {
            new ResourceLocation("dinorace", "textures/gui/race/chi_so.png"),
            new ResourceLocation("dinorace", "textures/gui/race/ky_nang.png"),
            new ResourceLocation("dinorace", "textures/gui/race/iten_hoa.png")
    };
    private static final String[] TAB_LABELS = {"CHỈ SỐ", "KỸ NĂNG", "TIẾN HÓA"};

    private static final int MARGIN = 8;
    private static final double WORLD_W = 1000.0;
    private static final double WORLD_H = 1000.0;

    private final int raceIndex;
    private final String raceKey;
    private final RaceDetailConfig.Data cfg;

    private int tab = 0;
    private int scrollStats = 0;
    private int scrollSkills = 0;

    private double zoom = 1.0;
    private double panX = 0;
    private double panY = 0;
    private boolean dragging = false;
    private long dragStartNanos = 0;

    public RaceDetailScreen(int raceIndex) {
        super(Component.literal("Thông tin tộc"));
        this.raceIndex = raceIndex;
        this.raceKey = RaceData.key(raceIndex);
        this.cfg = RaceDetailConfig.get(raceKey);
        lastRaceIndex = raceIndex;
        if (this.cfg.raceName.isEmpty()) {
            this.cfg.raceName = RaceData.NAMES[raceIndex];
        }
        // Nếu player đang thuộc đúng tộc này ở Origins → đổ dữ liệu THẬT từ
        // Origins & Medieval Origins (chỉ số + kỹ năng) lên GUI
        OriginsReader.applyRealData(this.cfg, raceIndex);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ============ Layout — sách 2 trang (UI_TravelBook), tự co theo màn hình ============

    private double bookScale() {
        double availW = this.width - MARGIN * 2;
        double availH = this.height - MARGIN * 2;
        return Math.min(availW / 208.0, Math.min(availH / 147.0, (this.height * 0.66) / 147.0));
    }

    private int bookX() { return (int) Math.round((this.width - 208 * bookScale()) / 2); }
    // Cân giữa cả khối sách + phần tab nhô lên → khoảng trống trên/dưới đều nhau
    private int bookY() {
        double s = bookScale();
        double protrude = 13 * s;
        return (int) Math.round((this.height - (147 * s + protrude)) / 2);
    }
    private int bookW() { return (int) Math.round(208 * bookScale()); }
    private int bookH() { return (int) Math.round(147 * bookScale()); }
    private int pageW() { return (int) Math.round(104 * bookScale()); }
    private int pageH() { return (int) Math.round(147 * bookScale()); }

    private int leftX() { return bookX(); }
    private int leftW() { return pageW(); }
    private int leftY() { return bookY(); }
    private int leftH() { return pageH(); }

    private int rightX() { return bookX() + pageW(); }
    private int rightW() { return pageW(); }
    private int rightY() { return bookY(); }
    private int rightH() { return pageH(); }

    // Tab marker — trên mép sách, xếp từ mép TRÁI sang phải, nhô lên khỏi mép trên
    private int tabW() { return Math.max(10, (int) Math.round(20 * bookScale())); }
    private int tabH() { return Math.max(8, (int) Math.round(15 * bookScale())); }
    private int tabGap() { return Math.max(2, (int) Math.round(3 * bookScale())); }
    private int tabX(int i) { return bookX() + i * (tabW() + tabGap()); }
    private int tabY() { return bookY() - tabH() + (int) Math.round(2 * bookScale()); }
    private int tabIcon() { return Math.max(6, (int) Math.round(12 * bookScale())); }

    private static List<String> wrapText(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && (int) (test.length() * 5.5) > maxWidth) {
                out.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out.isEmpty() ? List.of(text) : out;
    }
    private int hitTab(double mx, double my) {
        for (int i = 0; i < 3; i++) {
            int tX = tabX(i);
            if (mx >= tX && mx <= tX + tabW() && my >= tabY() && my <= tabY() + tabH()) {
                return i;
            }
        }
        return -1;
    }

    private int contentTop() { return rightY() + 12; }
    private int contentX() { return rightX() + 12; }
    private int contentW() { return rightW() - 24; }
    private int contentBottom() { return rightY() + rightH() - 14; }
    private int contentH() { return contentBottom() - contentTop(); }

    // ============ Vẽ ============

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.blit(BOOK_LEFT, bookX(), bookY(), pageW(), pageH(), 0f, 0f, 1, 1, 1, 1);
        g.blit(BOOK_RIGHT, rightX(), rightY(), pageW(), pageH(), 0f, 0f, 1, 1, 1, 1);

        drawTabs(g, mouseX, mouseY);
        drawLeftPanel(g);
        if (tab == 0) {
            drawStatsTab(g);
        } else if (tab == 1) {
            drawSkillsTab(g, mouseX, mouseY);
        } else {
            drawEvoTab(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawLeftPanel(GuiGraphics g) {
        int x = leftX(), y = leftY(), w = leftW(), h = leftH();

        String name = cfg.raceName;
        int nameY = y + 12;
        g.drawString(this.font, name, x + (w - this.font.width(name)) / 2, nameY, 0xFFFFCC44, true);

        boolean hasLevel = RaceClientData.hasPlayerLevel();
        int bottomY = y + h - 34;
        int imgBottom = hasLevel ? y + h - 80 : bottomY;
        ResourceLocation img = new ResourceLocation("dinorace",
                "textures/gui/race/" + RaceData.textureFile(raceIndex));
        int imgW = w - 28;
        int imgH = (int) (imgW * 1.5f);
        if (nameY + 14 + imgH > imgBottom) {
            imgH = imgBottom - (nameY + 14);
            imgW = (int) (imgH * 2f / 3f);
        }
        g.blit(img, x + (w - imgW) / 2, nameY + 14, imgW, imgH, 0f, 0f, 1, 1, 1, 1);

        if (hasLevel) {
            int lx = x + 14;
            int lw = w - 28;
            String lv = "CẤP " + RaceClientData.level();
            g.drawString(this.font, lv, lx, y + h - 76, 0xFFFFCC44, true);
            String sp = "ĐIỂM KỸ NĂNG: " + RaceClientData.skillPoints();
            g.drawString(this.font, sp, x + w - 14 - this.font.width(sp), y + h - 76, 0xFF88CCFF, true);
            g.blit(BAR_BG, lx, y + h - 60, lw, 8, 0f, 0f, 182, 5, 182, 5);
            int next = RaceClientData.xpToNext();
            int xp = RaceClientData.xp();
            double frac = 0;
            if (next > 0 && xp > 0) {
                frac = Math.min(1, xp / (double) next);
                int fw = Math.max(2, (int) (lw * frac));
                g.blit(BAR_FILL, lx, y + h - 60, fw, 8, 0f, 0f, Math.max(1, (int) (182 * frac)), 5, 182, 5);
            }
            String pct = "EXP: " + xp + "/" + next + " (" + (int) (frac * 100) + "%)";
            g.drawString(this.font, pct, x + (w - this.font.width(pct)) / 2, y + h - 47, 0xFFE8E8E8, true);
        }

        String hint = "Bấm ESC để đóng";
        g.drawString(this.font, hint, x + (w - this.font.width(hint)) / 2, y + h - 22, 0xFF9A9A9A, false);
    }

    private void drawTabs(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < 3; i++) {
            int tX = tabX(i);
            boolean sel = i == tab;
            g.blit(sel ? TAB_BG_SEL[i] : TAB_BG[i], tX, tabY(), tabW(), tabH(), 0f, 0f, 1, 1, 1, 1);
            int ic = tabIcon();
            g.blit(TAB_ICONS[i], tX + (tabW() - ic) / 2, tabY() + (tabH() - ic) / 2, ic, ic, 0f, 0f, 1, 1, 1, 1);
        }
        int hover = hitTab(mouseX, mouseY);
        if (hover >= 0) {
            g.renderTooltip(this.font, Component.literal(TAB_LABELS[hover]), mouseX, mouseY);
        }
    }

    // ---------- Tab Chỉ Số (cuộn được) ----------

    private int statsMaxScroll() {
        int rows = cfg.stats.size();
        int h = rows * 36;
        return Math.max(0, h - contentH());
    }

    private void drawStatsTab(GuiGraphics g) {
        int cx = contentX(), cw = contentW();
        int top = contentTop();
        int bottom = contentBottom();
        scrollStats = Math.max(0, Math.min(scrollStats, statsMaxScroll()));

        g.enableScissor(cx - 2, top, cx + cw + 2, bottom);
        int y = top - scrollStats;
        List<RaceDetailConfig.Stat> stats = cfg.stats;
        int barX = cx;
        int barW = cw;
        for (RaceDetailConfig.Stat s : stats) {
            if (y + 30 > top && y < bottom - 6) {
                g.drawString(this.font, s.name, cx, y + 2, 0xFF3B3B3B, false);
                String pct = (int) Math.round(s.value * 100) + "%";
                g.drawString(this.font, pct, cx + cw - this.font.width(pct), y + 2, 0xFF9C7A00, true);
                g.blit(BAR_BG, barX, y + 13, barW, 8, 0f, 0f, 182, 5, 182, 5);
                if (s.value > 0.001) {
                    double frac = Math.min(1, s.value);
                    int fw = Math.max(2, (int) (barW * frac));
                    g.blit(BAR_FILL, barX, y + 13, fw, 8, 0f, 0f, Math.max(1, (int) (182 * (double) fw / barW)), 5, 182, 5);
                }
            }
            y += 36;
        }
        g.disableScissor();
    }

    // ---------- Tab Kỹ Năng (cuộn được) ----------

    private int skillsMaxScroll() {
        List<RaceDetailConfig.Skill> skills = cfg.skills.stream().filter(s -> s.active).toList();
        int h = 0;
        for (RaceDetailConfig.Skill sk : skills) {
            int lanes = this.font.split(Component.literal(sk.description), contentW() - 40).size();
            h += 30 + lanes * 10 + 8;
        }
        return Math.max(0, h - contentH());
    }

    private void drawSkillsTab(GuiGraphics g, int mouseX, int mouseY) {
        int cx = contentX(), cw = contentW();
        int top = contentTop();
        int bottom = contentBottom();
        List<RaceDetailConfig.Skill> skills = cfg.skills.stream().filter(s -> s.active).toList();
        scrollSkills = Math.max(0, Math.min(scrollSkills, skillsMaxScroll()));

        g.enableScissor(cx - 2, top, cx + cw + 2, bottom);
        if (skills.isEmpty()) {
            String msg = "Không có kỹ năng khả dụng.";
            g.drawString(this.font, msg, cx + (cw - this.font.width(msg)) / 2, top + 12, 0xFF3B3B3B, false);
            g.disableScissor();
            return;
        }
        int playerLevel = RaceClientData.level();
        int y = top - scrollSkills;
        int hovered = -1;
        for (RaceDetailConfig.Skill sk : skills) {
            List<FormattedCharSequence> lines = this.font.split(Component.literal(sk.description), cw - 40);
            int boxH = 30 + lines.size() * 10 + 8;
            boolean locked = sk.active && playerLevel < sk.requiredLevel;
            if (y + boxH > top && y < bottom) {
                nineSlice(g, CLASS, cx, y, cw, boxH, 6, 30);
                int nameColor = locked ? 0xFF999999 : (sk.active ? 0xFFFFCC66 : 0xFF3B3B3B);
                g.drawString(this.font, sk.name, cx + (sk.active ? 26 : 12), y + 10, nameColor, false);
                if (sk.active) {
                    if (locked) {
                        String req = "Cần cấp " + sk.requiredLevel;
                        g.drawString(this.font, req, cx + cw - 16 - this.font.width(req), y + 10, 0xFFCC5555, true);
                    } else {
                        String lv = "Cấp " + sk.level;
                        g.drawString(this.font, lv, cx + cw - 16 - this.font.width(lv), y + 10, 0xFF88CCFF, true);
                    }
                }
                int ic = 12;
                if (sk.active) {
                    g.blit(locked ? LOCK_ICON : UNLOCK_ICON, cx + 6, y + 9, ic, ic, 0f, 0f, 1, 1, 1, 1);
                }
                int ty = y + 24;
                for (FormattedCharSequence ln : lines) {
                    g.drawString(this.font, ln, cx + 22, ty, locked ? 0xFF888888 : 0xFFBBBBBB, false);
                    ty += 10;
                }
                if (mouseX >= cx && mouseX <= cx + cw && mouseY >= y && mouseY <= y + boxH) {
                    hovered = skills.indexOf(sk);
                }
            }
            y += boxH + 8;
        }
        g.disableScissor();

        if (hovered >= 0) {
            RaceDetailConfig.Skill sk = skills.get(hovered);
            boolean locked = sk.active && playerLevel < sk.requiredLevel;
            String status;
            if (sk.active) {
                status = locked ? "Chưa đủ cấp độ — cần cấp " + sk.requiredLevel : "Đã mở khóa";
            } else {
                status = "Kỹ năng thụ động";
            }
            int iconSize = 14;
            int tw = iconSize + 4 + Math.max(this.font.width(sk.name), this.font.width(status));
            int th = 6 + 10 + 10 + 6;
            int tX = mouseX + 14;
            int tY = mouseY + 10;
            if (tX + tw > cx + cw + 2) {
                tX = mouseX - tw - 12;
            }
            if (tX < cx) {
                tX = cx;
            }
            if (tY + th > contentBottom()) {
                tY = mouseY - th - 10;
            }
            if (tY < top + 6) {
                tY = top + 6;
            }
            g.fill(tX - 3, tY - 3, tX + tw + 3, tY + th + 3, 0x505000FF);
            g.fill(tX - 3, tY + 3, tX + tw + 3, tY + th - 3, 0x5028007F);
            g.fill(tX - 1, tY - 1, tX + tw + 1, tY + th + 1, 0xF0100010);
            g.blit(locked ? LOCK_ICON : UNLOCK_ICON, tX + 3, tY + 4, iconSize, iconSize, 0f, 0f, 1, 1, 1, 1);
            int tx = tX + 3 + iconSize + 4;
            g.drawString(this.font, sk.name, tx, tY + 4, locked ? 0xFFFF5555 : 0xFFFFCC44, true);
            g.drawString(this.font, status, tx, tY + 16, locked ? 0xFFFF6666 : 0xFF88CCFF, false);
        }
    }

    // ---------- Tab Tiến Hóa ----------

    private void drawEvoTab(GuiGraphics g, int mouseX, int mouseY) {
        int cx = contentX(), cw = contentW();
        int top = contentTop();

        // Thanh tiến trình tiến hóa — căn giữa bên trên, dùng UI_TravelBook_Fill01a/b
        int mw = cw;
        int mh = 6;
        int mx = cx + (cw - mw) / 2;
        int my = top + 10;
        g.blit(EVO_BG, mx, my, mw, mh, 0f, 0f, 30, 2, 30, 2);
        double prog = Math.max(0, Math.min(1, cfg.evolution.progress / 100.0));
        int fillW = (int) (mw * prog);
        if (fillW > 0) {
            g.blit(EVO_FILL, mx, my, fillW, mh, 0f, 0f, Math.max(1, (int) (30 * prog)), 2, 30, 2);
        }
        String pct = "TIẾN HÓA: " + (int) cfg.evolution.progress + "%";
        g.drawString(this.font, pct, cx + (cw - this.font.width(pct)) / 2, my - 12, 0xFFFFCC44, true);
        // Mốc tiến hóa
        for (double m : cfg.evolution.milestones) {
            int mxx = mx + (int) (mw * m / 100.0);
            g.fill(mxx - 2, my - 3, mxx + 2, my - 1, m <= cfg.evolution.progress ? 0xFFFFD88A : 0xFF6A6A6A);
        }

        // Khu vực cây tiến hóa
        int ty = my + mh + 18;
        int th = contentBottom() - ty;
        if (th <= 0) {
            return;
        }
        g.enableScissor(cx - 2, ty, cx + cw + 2, contentBottom());

        // Nền: light_dirt_background DẠNG LẶP (tile 16x16) — lưới tile CĂN theo tọa độ
        // tuyệt đối nên ô cuối là phần CẮT của ô đầy đủ (không bị bóp/méo mép phải & dưới)
        int tb1 = cx - 2, tb2 = cx + cw + 2;
        int tly = ty, tly2 = contentBottom();
        final int TILE_STEP = 48;
        int startX = tb1 - Math.floorMod(tb1, TILE_STEP);
        int startY = tly - Math.floorMod(tly, TILE_STEP);
        for (int xx = startX; xx < tb2; xx += TILE_STEP) {
            for (int yy = startY; yy < tly2; yy += TILE_STEP) {
                g.blit(DIRT_BG, xx, yy, TILE_STEP, TILE_STEP, 0f, 0f, 16, 16, 16, 16);
            }
        }
        g.setColor(1f, 1f, 1f, 0.16f);
        g.blit(WING_BG, tb1, tly, tb2 - tb1, tly2 - tly, 0f, 0f, 736, 736, 736, 736);
        g.setColor(1f, 1f, 1f, 1f);

        // Transform THỐNG NHẤT — dùng chung cho vẽ/clicks/kéo/phóng to
        double s = curScale();
        double ox = curOx();
        double oy = curOy();

        // Gợi ý thao tác
        String tip = "Kéo: di chuyển • Lăn chuột: phóng to • Rê chuột vào điểm: xem chi tiết";
        g.drawString(this.font, tip, cx + 6, ty + 4, 0xFFFFE9A8, false);

        List<RaceDetailConfig.EvoNode> nodes = cfg.evolution.nodes;
        Map<String, Integer> byId = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            byId.put(nodes.get(i).id, i);
        }

        // Đường nối trước (giống puffish: nối 2 điểm cây)
        for (List<String> link : cfg.evolution.links) {
            if (link == null || link.size() < 2) {
                continue;
            }
            Integer a = byId.get(link.get(0));
            Integer b = byId.get(link.get(1));
            if (a == null || b == null) {
                continue;
            }
            boolean hi = false;
            int ax = (int) (ox + nodes.get(a).x * WORLD_W * s);
            int ay = (int) (oy + nodes.get(a).y * WORLD_H * s);
            int bx2 = (int) (ox + nodes.get(b).x * WORLD_W * s);
            int by2 = (int) (oy + nodes.get(b).y * WORLD_H * s);
            drawLink(g, ax, ay, bx2, by2, hi ? 7 : 5, hi ? 0xCCFFD98A : 0x55E8C06A);
        }

        // Các điểm tiến hóa
        for (int i = 0; i < nodes.size(); i++) {
            RaceDetailConfig.EvoNode n = nodes.get(i);
            int nx = (int) (ox + n.x * WORLD_W * s);
            int ny = (int) (oy + n.y * WORLD_H * s);
            // Icon NHỎ, co theo mức zoom — không quá to
            int nodeSize = Math.max(10, (int) (18 * zoom));
            if (n.icon != null && !n.icon.isEmpty()) {
                ResourceLocation icon = new ResourceLocation("dinorace", "textures/gui/race/" + n.icon + ".png");
                g.blit(icon, nx - nodeSize / 2, ny - nodeSize / 2, nodeSize, nodeSize, 0f, 0f, 1, 1, 1, 1);
            } else {
                nineSlice(g, TILE, nx - nodeSize / 2, ny - nodeSize / 2, nodeSize, nodeSize, 4, 32);
                g.fill(nx - 4, ny - 4, nx + 4, ny + 4, 0xFFFFCC44);
            }
        }

        // Chỉ hiện tên + thông tin khi RÊ CHUỘT vào điểm — icon phóng to nhẹ + tooltip
        int hovered = hitNode(mouseX, mouseY);
        if (hovered >= 0 && hovered < nodes.size()) {
            RaceDetailConfig.EvoNode n = nodes.get(hovered);
            int nx = (int) (ox + n.x * WORLD_W * s);
            int ny = (int) (oy + n.y * WORLD_H * s);
            // Icon node phóng TO hơn chút (vẫn cùng texture, không đổi hình)
            int hoverSize = Math.max(12, (int) (25 * zoom));
            if (n.icon != null && !n.icon.isEmpty()) {
                ResourceLocation icon = new ResourceLocation("dinorace", "textures/gui/race/" + n.icon + ".png");
                g.blit(icon, nx - hoverSize / 2, ny - hoverSize / 2, hoverSize, hoverSize, 0f, 0f, 1, 1, 1, 1);
            } else {
                nineSlice(g, TILE, nx - hoverSize / 2, ny - hoverSize / 2, hoverSize, hoverSize, 4, 32);
                g.fill(nx - 5, ny - 5, nx + 5, ny + 5, 0xFFFFCC44);
            }
            List<String> descLines = wrapText(n.desc, 220);
            int tW = this.font.width(n.name);
            for (String ln : descLines) {
                tW = Math.max(tW, this.font.width(ln));
            }
            tW = Math.min(cw - 12, tW + 8);
            int tH = 6 + 10 + descLines.size() * 10 + 6;
            int tX = mouseX + 14;
            int tY = mouseY + 10;
            if (tX + tW > cx + cw + 2) {
                tX = mouseX - tW - 12;
            }
            if (tX < cx) {
                tX = cx;
            }
            if (tY + tH > contentBottom()) {
                tY = mouseY - tH - 10;
            }
            if (tY < ty + 6) {
                tY = ty + 6;
            }
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(n.name).withStyle(ChatFormatting.GOLD));
            for (String ln : descLines) {
                lines.add(Component.literal(ln));
            }
            g.renderTooltip(this.font, lines, Optional.empty(), tX, tY);
        }

        g.disableScissor();
    }

    // ============ Tương tác ============

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            // Tab trên — mép trên sách
            int t = hitTab(mx, my);
            if (t >= 0) {
                tab = t;
                return true;
            }
            // Cây tiến hóa: kéo di chuyển (chi tiết xem bằng hover, không cần click)
            if (tab == 2 && isInTreeArea(mx, my)) {
                long now = System.nanoTime();
                dragStartNanos = now;
                dragged = false;
                draggingStartX = mx;
                draggingStartY = my;
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private boolean dragged = false;
    private double draggingStartX;
    private double draggingStartY;

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) {
            double distX = mx - draggingStartX;
            double distY = my - draggingStartY;
            if (distX * distX + distY * distY > 16) {
                dragged = true;
            }
            panX += dx;
            panY += dy;
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        if (tab == 0) {
            scrollStats = (int) Math.max(0, Math.min(statsMaxScroll(), scrollStats - Math.signum(amount) * 24));
            return true;
        }
        if (tab == 1) {
            scrollSkills = (int) Math.max(0, Math.min(skillsMaxScroll(), scrollSkills - Math.signum(amount) * 24));
            return true;
        }
        if (tab == 2 && isInTreeArea(mx, my)) {
            double factor = amount > 0 ? 1.12 : 1 / 1.12;
            double old = zoom;
            double nz = Math.max(0.6, Math.min(3.2, zoom * factor));
            if (nz == old) {
                return true;
            }
            // Giữ nguyên điểm thế giới dưới con trỏ (giống puffish)
            double sOld = curScale();
            double wx = (mx - curOx()) / sOld;
            double wy = (my - curOy()) / sOld;
            zoom = nz;
            double sNew = curScale();
            panX = mx - (curOx() - panX) - wx * sNew;
            panY = my - (curOy() - panY) - wy * sNew;
            return true;
        }
        return super.mouseScrolled(mx, my, amount);
    }

    private boolean isInTreeArea(double mx, double my) {
        int cx = contentX();
        int ty = contentTop() + 34;
        return mx >= cx - 2 && mx <= cx + contentW() + 2 && my >= ty && my <= contentBottom();
    }

    private double treeScale() {
        int ty = contentTop() + 34;
        int th = contentBottom() - ty;
        if (th <= 0) {
            return 1;
        }
        return Math.min((contentW() * 0.92) / WORLD_W, (th * 0.90) / WORLD_H);
    }

    private double treeOriginX() {
        return contentX() + (contentW() - WORLD_W * treeScale()) / 2;
    }

    private double treeOriginY() {
        int ty = contentTop() + 34;
        int th = contentBottom() - ty;
        return ty + (th - WORLD_H * treeScale()) / 2;
    }

    // Transform duy nhất, dùng chung cho vẽ + click + kéo + phóng to
    private double curScale() {
        return treeScale() * zoom;
    }

    private double curOx() {
        return treeOriginX() + panX;
    }

    private double curOy() {
        return treeOriginY() + panY;
    }

    private int hitNode(double mx, double my) {
        List<RaceDetailConfig.EvoNode> nodes = cfg.evolution.nodes;
        double s = curScale();
        double ox = curOx();
        double oy = curOy();
        // Bán kính hit TÍNH THEO MÀN HÌNH (px cố định) — phóng to/thu nhỏ không làm
        // click bị lệch với icon đang vẽ (cách làm của puffish)
        double hitR = 26;
        int best = -1;
        double bestD = hitR * hitR;
        for (int i = 0; i < nodes.size(); i++) {
            double nx = ox + nodes.get(i).x * WORLD_W * s;
            double ny = oy + nodes.get(i).y * WORLD_H * s;
            double d = (mx - nx) * (mx - nx) + (my - ny) * (my - ny);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }

    // ============ Tiện ích ============

    private void drawLink(GuiGraphics g, int x1, int y1, int x2, int y2, int thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 1f) {
            return;
        }
        float ang = (float) Math.atan2(dy, dx);
        PoseStack p = g.pose();
        p.pushPose();
        p.translate(x1, y1, 0f);
        p.mulPose(com.mojang.math.Axis.ZP.rotation(ang));
        g.fill(0, -thickness / 2, (int) len, thickness / 2, color);
        p.popPose();
    }

    private static void nineSlice(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int b, int s) {
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