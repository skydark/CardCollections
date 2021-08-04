package me.skydark.card_collections.init;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.entity.CardEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, Mod.MOD_ID);
    public static final RegistryObject<EntityType<CardEntity>> CARD = registerEntity("card",
        EntityType.Builder.<CardEntity>create(CardEntity::new, EntityClassification.MISC)
            .updateInterval(Integer.MAX_VALUE)
    );

    private static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String id, EntityType.Builder builder)
    {
        return ModEntities.ENTITY_TYPES.register(id, () -> builder.build(id));
    }
}
