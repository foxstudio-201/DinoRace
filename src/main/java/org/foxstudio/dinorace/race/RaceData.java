package org.foxstudio.dinorace.race;

public final class RaceData {

    /** Index của tộc Long Nhân trong mảng KEYS — dùng cho các logic cơ chế riêng. */
    public static final int DRAGONKIN_INDEX = 12;

    /** Khóa tộc — dùng cho tên file texture (ảnh lớn + icon) và config. */
    public static final String[] KEYS = {
            "dwarf", "elf", "pixie", "human", "undead",
            "alfiq", "goblin", "incubus", "valkyrie", "wood_elf", "yeti", "fae",
            "dragonkin", "ogre"
    };

    /** Tên hiển thị ngắn trên thẻ chọn tộc. */
    public static final String[] NAMES = {
            "Dwarf", "Elf", "Pixie", "Human", "Undead",
            "Alfiq", "Goblin", "Incubus", "Valkyrie", "Wood Elf", "Yeti", "Fae",
            "Dragonkin", "Ogre"
    };

    public static final String[] ORIGINS = {
            "medievalorigins:dwarf",
            "medievalorigins:high_elf",
            "medievalorigins:pixie",
            "origins:human",
            "medievalorigins:revenant",
            "medievalorigins:alfiq",
            "medievalorigins:goblin",
            "medievalorigins:incubus",
            "medievalorigins:valkyrie",
            "medievalorigins:wood_elf",
            "medievalorigins:yeti",
            "medievalorigins:fae",
            "medievalorigins:dragonkin",
            "medievalorigins:ogre"
    };

    public static final String[] DESCRIPTIONS = {
            "Chủng tộc vạm vỡ, kiên cường của núi rừng.\nBền bỉ, kháng sát thương, thích hợp chiến đấu cận chiến.",
            "Loài tinh linh cao lớn, nhanh nhẹn và tinh tế.\nDi chuyển linh hoạt, hợp với cung tên và ma thuật.",
            "Yêu tinh tí hon của thế giới thần tiên.\nBay lượn tự do, nhỏ gọn nhanh nhẹn, thích phá phách và may mắn.",
            "Loài người đa năng, cân bằng.\nDễ thích nghi với mọi hoàn cảnh và con đường rèn luyện.",
            "Xác sống bất tử, bóng tối ôm lấy.\nKhông cần hít thở, kháng độc và hồi phục theo cách riêng.",
            "Giống mèo khổng lồ của vùng sa mạc.\nNhanh nhẹn, sắc bén, thích hợp rình rập và chiến đấu linh hoạt.",
            "Kẻ xảo quyệt của hang tối.\nNhỏ con lanh lợi, chế tạo thủ công và đầy những trò bẩn thỉu.",
            "Ác mộng của màn đêm.\nMạnh mẽ hút sinh khí, quyến rũ nhưng khốn khổ với ánh nắng.",
            "Chiến binh thiên giới oai hùng.\nĐôi cánh thần thánh, dũng mãnh lao thẳng vào lòng trận địa.",
            "Tinh linh rừng sâu gắn bó với thiên nhiên.\nẨn mình tài giỏi, điều khiển cây cỏ và săn bắn thành thạo.",
            "Quái vật tuyết vùng núi cao.\nSức mạnh khủng khiếp, chịu lạnh tốt, có thể đập tan mọi thứ.",
            "Tiên lành của thế giới thần tiên.\nLướt nhẹ trong gió, phép thuật dịu dàng nhưng quyền năng, thích thiên nhiên và may mắn.",
            "Long Nhân — dòng máu rồng ngàn đời.\nVảy rồng kiên cố, sức mạnh và thể chất vượt trội, thân hình đồ sộ hơn cả ogre.\nNộ rồng khi máu dưới một nửa, đập đá bằng tay không, sát thương vũ khí tăng theo loại.\nKhông thể mặc giáp dưới sắt và không lọt được lối đi cao 2 khối.",
            "Ogre — gã khổng lồ háu ăn của núi rừng.\nThân hình to lớn, sức mạnh rìu vượt trội, bụng chì miễn nhiễm độc và đói.\nKhát máu bùng nổ khi kề cận cái chết, nhưng chậm chạp và không thể dùng khiên.\nThân hình dềnh dàng không lọt lối đi cao 2 khối."
    };

    public static String textureFile(int index) {
        return KEYS[index] + ".png";
    }

    public static String iconFile(int index) {
        return "icon_" + KEYS[index] + ".png";
    }

    public static String key(int index) {
        return KEYS[index];
    }

    /** Tìm index tộc từ origin hiện tại của player (fallback khi chưa lưu dinocore_race_index). */
    private static final java.util.Map<String, Integer> POWER_TO_INDEX = new java.util.HashMap<>();

    static {
        POWER_TO_INDEX.put("medievalorigins:dragonkin/dragon_fury", 12);
        POWER_TO_INDEX.put("medievalorigins:dwarf/potent_brew", 0);
        POWER_TO_INDEX.put("medievalorigins:pixie/flight", 2);
        POWER_TO_INDEX.put("medievalorigins:pixie/mischief_maketh_man", 2);
        POWER_TO_INDEX.put("medievalorigins:revenant/hellraiser", 4);
        POWER_TO_INDEX.put("medievalorigins:alfiq/meow", 5);
        POWER_TO_INDEX.put("medievalorigins:incubus/unholy_deal", 7);
        POWER_TO_INDEX.put("medievalorigins:valkyrie/intervention", 8);
        POWER_TO_INDEX.put("medievalorigins:yeti/frigid_pulse", 10);
        POWER_TO_INDEX.put("medievalorigins:fae/levitation", 11);
        POWER_TO_INDEX.put("medievalorigins:fae/natures_blessing", 11);
        POWER_TO_INDEX.put("medievalorigins:ogre/bloodlust", 13);
        POWER_TO_INDEX.put("medievalorigins:high_elf/ebonbreath", 1);
        POWER_TO_INDEX.put("medievalorigins:high_elf/blazenbreath", 1);
    }

    public static int indexFromOrigin(net.minecraft.server.level.ServerPlayer player) {
        try {
            io.github.apace100.origins.component.OriginComponent oc =
                    io.github.apace100.origins.registry.ModComponents.ORIGIN.get(player);
            for (io.github.apace100.origins.origin.Origin origin : oc.getOrigins().values()) {
                if (origin == null) {
                    continue;
                }
                for (io.github.apace100.apoli.power.PowerType<?> pt : origin.getPowerTypes()) {
                    Integer idx = POWER_TO_INDEX.get(pt.getIdentifier().toString());
                    if (idx != null) {
                        return idx;
                    }
                }
                try {
                    String id = ((net.minecraft.resources.ResourceLocation) origin.getClass()
                            .getMethod("getIdentifier").invoke(origin)).toString();
                    for (int i = 0; i < ORIGINS.length; i++) {
                        if (ORIGINS[i].equals(id)) {
                            return i;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    private RaceData() {
    }
}