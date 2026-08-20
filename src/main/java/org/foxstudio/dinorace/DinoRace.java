package org.foxstudio.dinorace;

import net.minecraftforge.fml.common.Mod;

/**
 * DinoRace — GUI thông tin tộc riêng, đọc dữ liệu từ Origins / Medieval Origins
 * / Puffish Skills. Tách khỏi dinocore để không gắn chặt vào dinocore.
 */
@Mod(DinoRace.MODID)
public final class DinoRace {

    public static final String MODID = "dinorace";

    public DinoRace() {
        org.foxstudio.dinorace.network.RaceNetwork.init();
        SkillGateConditions.register();
    }
}