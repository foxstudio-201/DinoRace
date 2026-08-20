package org.foxstudio.dinorace;

import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.foxstudio.dinorace.player.PlayerLevels;

/**
 * Custom Apoli entity conditions used to gate locked active skills server-side.
 * The datapack override of the medievalorigins active powers adds
 * "condition": { "type": "dinorace:skill_unlocked", "level": N } so the power
 * only activates once the player's dinorace level is >= N.
 */
public final class SkillGateConditions {

    private SkillGateConditions() {
    }

    public static void register() {
        registerCondition(new ConditionFactory<>(
                new ResourceLocation(DinoRace.MODID, "skill_unlocked"),
                new SerializableData().add("level", SerializableDataTypes.INT),
                (instance, entity) -> {
                    int required = instance.getInt("level");
                    return entity instanceof ServerPlayer sp
                            && sp.getPersistentData().getInt(PlayerLevels.KEY_LEVEL) >= required;
                }
        ));
    }

    private static void registerCondition(ConditionFactory<Entity> factory) {
        if (ApoliRegistries.ENTITY_CONDITION.get(factory.getSerializerId()) == null) {
            net.minecraft.core.Registry.register(ApoliRegistries.ENTITY_CONDITION,
                    factory.getSerializerId(), factory);
        }
    }
}