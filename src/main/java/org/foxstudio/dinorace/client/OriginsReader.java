package org.foxstudio.dinorace.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import org.foxstudio.dinorace.race.RaceData;
import org.foxstudio.dinorace.race.RaceDetailConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Đọc DỮ LIỆU THẬT từ Origins + Medieval Origins ở runtime VIA REFLECTION (không
 * cần biên dịch kèm jar API — tránh mâu thuẫn SRG/official):
 * - KỸ NĂNG: powers của origin (tên + mô tả).
 * - CHỈ SỐ: attribute modifiers của attribute powers.
 * Mọi truy cập bọc try/catch — thiếu mod vẫn hoạt động bằng config JSON.
 */
public final class OriginsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger("dinorace");
    private static final String ORIGIN_CLS = "io.github.apace100.origins.origin.Origin";
    private static final String POWER_TYPE_CLS = "io.github.apace100.apoli.power.PowerType";
    private static final String ATTRIBUTE_POWER_CLS = "io.github.apace100.apoli.power.AttributePower";

    private static boolean checked = false;
    private static boolean ready = false;

    private OriginsReader() {
    }

    public static synchronized boolean available() {
        if (!checked) {
            checked = true;
            try {
                Class.forName(ORIGIN_CLS);
                Class.forName(POWER_TYPE_CLS);
                Class.forName(ATTRIBUTE_POWER_CLS);
                ready = true;
            } catch (Throwable t) {
                ready = false;
            }
        }
        return ready;
    }

    /** Danh sách origin thật (excl origins:empty) của player hiện tại. */
    private static List<Object> origins() {
        List<Object> out = new ArrayList<>();
        try {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return out;
            }
            Method get = null;
            for (Method m : Class.forName(ORIGIN_CLS).getMethods()) {
                if (m.getName().equals("get") && m.getParameterCount() == 1 && !m.getReturnType().isPrimitive()) {
                    get = m;
                    break;
                }
            }
            if (get == null) {
                return out;
            }
            Object map = get.invoke(null, (Object) player);
            if (!(map instanceof Map<?, ?>)) {
                return out;
            }
            for (Object o : ((Map<?, ?>) map).values()) {
                if (o == null) {
                    continue;
                }
                Object id = invoke(o, "getIdentifier");
                if (id == null || "origins:empty".equals(id.toString())) {
                    continue;
                }
                out.add(o);
            }
        } catch (Throwable t) {
            LOGGER.debug("Đọc origin lỗi: {}", t.getMessage());
        }
        return out;
    }

    /** Đổ dữ liệu thật vào cfg nếu player thuộc đúng ORIGINS[raceIndex]. */
    public static void applyRealData(RaceDetailConfig.Data cfg, int raceIndex) {
        if (!available() || cfg == null || raceIndex < 0 || raceIndex >= RaceData.ORIGINS.length) {
            return;
        }
        for (Object origin : origins()) {
            Object id = invoke(origin, "getIdentifier");
            if (id == null || !id.toString().equals(RaceData.ORIGINS[raceIndex])) {
                continue;
            }
            loadSkills(cfg, origin);
            loadStats(cfg, origin);
            return;
        }
    }

    private static void loadSkills(RaceDetailConfig.Data cfg, Object origin) {
        List<RaceDetailConfig.Skill> list = new ArrayList<>();
        try {
            Object pts = invoke(origin, "getPowerTypes");
            if (!(pts instanceof Iterable<?>)) {
                return;
            }
            for (Object pt : (Iterable<?>) pts) {
                try {
                    boolean hidden = Boolean.TRUE.equals(invoke(pt, "isHidden"));
                    if (hidden) {
                        continue;
                    }
                    String name = compText(invoke(pt, "getName"));
                    if (name == null || name.trim().isEmpty()) {
                        name = ptId(pt);
                    }
                    RaceDetailConfig.Skill sk = new RaceDetailConfig.Skill();
                    sk.name = name;
                    sk.description = compText(invoke(pt, "getDescription"));
                    sk.level = 1;
                    list.add(sk);
                } catch (Throwable t) {
                    LOGGER.debug("Bỏ qua power: {}", t.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Đọc skills lỗi: {}", t.getMessage());
        }
        if (!list.isEmpty()) {
            cfg.skills = list;
        }
    }

    private static void loadStats(RaceDetailConfig.Data cfg, Object origin) {
        Map<String, RaceDetailConfig.Stat> byAttr = new LinkedHashMap<>();
        try {
            Player player = Minecraft.getInstance().player;
            Object pts = invoke(origin, "getPowerTypes");
            if (!(pts instanceof Iterable<?>)) {
                return;
            }
            for (Object pt : (Iterable<?>) pts) {
                Object power;
                try {
                    power = invoke(pt, "get", player);
                } catch (Throwable t) {
                    power = null;
                }
                if (power == null) {
                    continue;
                }
                List<?> mods = attributeModifiers(power);
                if (mods == null) {
                    continue;
                }
                for (Object m : mods) {
                    try {
                        Object rawAttr = invoke(m, "getAttribute");
                        Object rawMod = invoke(m, "getModifier");
                        if (!(rawAttr instanceof Attribute attr) || !(rawMod instanceof AttributeModifier mod)) {
                            continue;
                        }
                        double amount = mod.getAmount();
                        String attrId = attrKey(attr);
                        if (attrId == null) {
                            continue;
                        }
                        String disp = attrDisplay(attr);
                        RaceDetailConfig.Stat s = byAttr.get(attrId);
                        if (s == null) {
                            s = new RaceDetailConfig.Stat();
                            s.name = disp + " " + signed(amount);
                            byAttr.put(attrId, s);
                        }
                        s.value = clamp01(s.value + amount);
                    } catch (Throwable t) {
                        LOGGER.debug("Bỏ qua modifier: {}", t.getMessage());
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Đọc stats lỗi: {}", t.getMessage());
        }
        if (!byAttr.isEmpty()) {
            cfg.stats = new ArrayList<>(byAttr.values());
        }
    }

    private static List<?> attributeModifiers(Object power) {
        try {
            for (Class<?> c = power.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    Field f = c.getDeclaredField("modifiers");
                    if (List.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object v = f.get(power);
                        return v instanceof List<?> l ? l : null;
                    }
                } catch (NoSuchFieldException nsf) {
                    // đi tiếp lên superclass
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Field modifiers lỗi: {}", t.getMessage());
        }
        return null;
    }

    private static Object invoke(Object target, String name, Object... args) {
        if (target == null) {
            return null;
        }
        try {
            for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                        m.setAccessible(true);
                        return m.invoke(target, args);
                    }
                }
            }
        } catch (Throwable t) {
            return null;
        }
        return null;
    }

    private static String ptId(Object pt) {
        try {
            Object id = invoke(pt, "getIdentifier");
            return id == null ? "" : id.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    /** Component (runtime) → String; ép kiểu về Component official rồi gọi trực tiếp. */
    private static String compText(Object comp) {
        try {
            if (comp instanceof Component c) {
                return c.getString();
            }
        } catch (Throwable t) {
            LOGGER.debug("compText lỗi: {}", t.getMessage());
        }
        return "";
    }

    private static String attrKey(Attribute attr) {
        try {
            var key = ForgeRegistries.ATTRIBUTES.getKey(attr);
            return key == null ? null : key.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static String attrDisplay(Attribute attr) {
        try {
            return Component.translatable(attr.getDescriptionId()).getString();
        } catch (Throwable t) {
            try {
                return attr.getDescriptionId();
            } catch (Throwable t2) {
                return "attribute";
            }
        }
    }

    private static String signed(double v) {
        return (v >= 0 ? "+" : "") + (Math.abs(v) < 1
                ? String.format(Locale.ROOT, "%.2f", v)
                : String.format(Locale.ROOT, "%.0f", v));
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}