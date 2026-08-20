package org.foxstudio.dinorace.race;

import com.momosoftworks.coldsweat.api.temperature.modifier.SimpleTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.foxstudio.dinorace.DinoRace;
import org.foxstudio.dinorace.client.RaceDetailScreen;

import java.util.List;

/**
 * Cơ chế riêng của tộc Long Nhân (Dragonkin):
 * - CHỐNG LẠNH/NÓNG: cơ thể vảy rồng đã quen với khí hậu khắc nghiệt —
 *   lượng nhiệt đến từ thế giới được giảm bớt (mod ColdSweat).
 * - KHINH TRANG BỊ: không thể mặc giáp dưới cấp sắt (da / vàng / xích / mai rùa).
 * - TAY ĐẬP ĐÁ: tay không vẫn đập vỡ được đá thường (không cần cuốc) và nhặt được drop.
 * (Nộ Rồng giờ là kỹ năng bấm chủ động của origin medievalorigins:dragonkin;
 * vóc dáng đồ sộ + bậc thầy vũ khí + chỉ số do origin xử lý.)
 */
@Mod.EventBusSubscriber(modid = DinoRace.MODID)
public final class DragonkinHandler {

    /** Nhãn đánh dấu modifier chống nhiệt do tộc Dragonkin thêm vào (tránh xung đột cả stack). */
    private static final String CS_TAG = "dinorace_dragon";

    /** Chỉ tra cứu một lần khi chạy (ColdSweat có thể không được cài trên server). */
    private static Boolean coldSweatLoaded;

    private DragonkinHandler() {
    }

    private static boolean isDragonkin(Player p) {
        return p.getPersistentData().getInt("dinocore_race_index") == RaceData.DRAGONKIN_INDEX;
    }

    private static boolean isDragonkinClient() {
        return RaceDetailScreen.lastRaceIndex() == RaceData.DRAGONKIN_INDEX;
    }

    private static boolean coldSweatPresent() {
        if (coldSweatLoaded == null) {
            coldSweatLoaded = ModList.get().isLoaded("cold_sweat");
        }
        return coldSweatLoaded;
    }

    private static boolean isOurs(TempModifier m) {
        if (!(m instanceof SimpleTempModifier)) {
            return false;
        }
        CompoundTag tag = m.getNBT();
        return tag != null && tag.contains(CS_TAG);
    }

    /**
     * Duy trì modifier chống nhiệt (giảm % ảnh hưởng nhiệt độ thế giới) cho Dragonkin.
     * Không phải Dragonkin -> gỡ modifier còn sót lại (đổi chủng tộc).
     */
    private static void updateColdSweat(ServerPlayer p) {
        if (!coldSweatPresent()) {
            return;
        }
        List<TempModifier> mods = Temperature.getModifiers(p, Temperature.Trait.WORLD, DragonkinHandler::isOurs);
        boolean dragonkin = isDragonkin(p);
        if (dragonkin && mods.isEmpty()) {
            SimpleTempModifier sm = new SimpleTempModifier(0.4, SimpleTempModifier.Operation.MULTIPLY);
            sm.getNBT().putBoolean(CS_TAG, true);
            sm.tickRate(10).expires(30);
            Temperature.addModifier(p, sm, Temperature.Trait.WORLD, Placement.LAST);
            Temperature.updateTemperature(p);
        } else if (!dragonkin && !mods.isEmpty()) {
            Temperature.removeModifiers(p, Temperature.Trait.WORLD, DragonkinHandler::isOurs);
            Temperature.updateModifiers(p);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer p)) {
            return;
        }
        if (p.level().getGameTime() % 15 == 0) {
            updateColdSweat(p);
        }
        unequipBelowIron(p);
    }

    private static boolean isBelowIron(ArmorItem ai) {
        var mat = ai.getMaterial();
        return mat == ArmorMaterials.LEATHER
                || mat == ArmorMaterials.CHAIN
                || mat == ArmorMaterials.GOLD
                || mat == ArmorMaterials.TURTLE;
    }

    /** Tước bỏ giáp dưới cấp sắt (trả về túi đồ / rơi ra) + báo player. */
    private static void unequipBelowIron(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.armor.size(); i++) {
            ItemStack s = inv.armor.get(i);
            if (s.isEmpty() || !(s.getItem() instanceof ArmorItem ai)) {
                continue;
            }
            if (!isBelowIron(ai)) {
                continue;
            }
            inv.armor.set(i, ItemStack.EMPTY);
            if (!inv.add(s)) {
                p.drop(s, false);
            }
            if (p.level().getGameTime() % 40 == 0) {
                p.displayClientMessage(Component.literal("Vảy rồng của ngươi không chấp nhận giáp yếu hơn sắt!"), true);
            }
        }
    }

    private static boolean isStoneClass(BlockState state) {
        if (!state.requiresCorrectToolForDrops()) {
            return false;
        }
        if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
            return true;
        }
        if (state.is(BlockTags.NEEDS_IRON_TOOL) || state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return false;
        }
        return true;
    }

    /** Tay không vẫn thu hoạch được đá thường (có drop). */
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        Player p = event.getEntity();
        if (p.getMainHandItem().isEmpty()
                && isStoneClass(event.getTargetBlock())
                && (p.level().isClientSide ? isDragonkinClient() : isDragonkin(p))) {
            event.setCanHarvest(true);
        }
    }

    /** Bỏ penalty tốc độ phá block khi tay không đập đá. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        if (event.getState().requiresCorrectToolForDrops()
                && p.getMainHandItem().isEmpty()
                && isDragonkin(p)) {
            event.setNewSpeed(Math.max(event.getNewSpeed(), event.getOriginalSpeed() * 2.5f));
        }
    }
}