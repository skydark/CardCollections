package me.skydark.card_collections.init;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardCollectionData;
import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.data.CardData;
import me.skydark.card_collections.item.CardItem;
import me.skydark.card_collections.item.CollectionBookItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.horse.SkeletonHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.codehaus.plexus.util.StringOutputStream;

import javax.annotation.Nullable;

@EventBusSubscriber
public class ModEventHandlers {
    public static final String TAG_ON_FIRST_JOIN = "__" + Mod.MOD_ID + "__FIRST_JOIN";

    @SubscribeEvent
    public static void onEntityJoinWorld(final EntityJoinWorldEvent event) {
        if (event.getEntity().getEntityWorld().isRemote)
            return;
        if (event.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntity();
            if (!player.getTags().contains(TAG_ON_FIRST_JOIN)) {
                ItemStack encyclopedia = CollectionBookItem.getMcMobsEncyclopedia();
                if (!encyclopedia.isEmpty()) {
                    if (!player.addItemStackToInventory(encyclopedia)) {
                        player.dropItem(encyclopedia, false);
                    }
                    player.addTag(TAG_ON_FIRST_JOIN);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(final PlayerInteractEvent.EntityInteract event) {
        PlayerEntity playerIn = event.getPlayer();
        Entity target = event.getTarget();
        Hand hand = event.getHand();
        ItemStack stack = playerIn.getHeldItem(hand);
        if (!CollectionBookItem.isMcMobsEncyclopedia(stack)) {
            return;
        }
        if (playerIn.getEntityWorld().isRemote) {
            event.setCancellationResult(ActionResultType.SUCCESS);
            event.setCanceled(true);
            return;
        }
        ResourceLocation resourceLocation = EntityType.getKey(target.getType());
        //Mod.LOGGER.debug("Seen entity: {}", resourceLocation);
        if (!"minecraft".equals(resourceLocation.getNamespace())) {
            Mod.LOGGER.info("Not a vanilla entity: {}", resourceLocation);
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.failed",
                            new TranslationTextComponent(resourceLocation.toString())),
                    Util.DUMMY_UUID);
            event.setCancellationResult(ActionResultType.SUCCESS);
            event.setCanceled(true);
            return;
        }
        CardCollectionData mcmobs = CardCollectionDataManager.INSTANCE.getCollection("mcmobs");
        if (mcmobs == null) {
            Mod.LOGGER.warn("Card collection mcmobs is not loaded");
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.failed",
                            new TranslationTextComponent(resourceLocation.toString())),
                    Util.DUMMY_UUID);
            event.setCancellationResult(ActionResultType.SUCCESS);
            event.setCanceled(true);
            return;
        }
        String cardId = resourceLocation.getPath();
        // special cases:
        if (target instanceof LivingEntity && ((LivingEntity) target).isChild() && ("piglin".equals(cardId) || "polar_bear".equals(cardId))) {
            cardId = cardId + "_baby";
        } else {
            if (target.isBeingRidden()) {
                String newId = getJockey(target);
                if (newId != null) cardId = newId;
            } else if (target.getRidingEntity() != null) {
                String newId = getJockey(target.getRidingEntity());
                if (newId != null) cardId = newId;
            }
        }

        CardData cardData = mcmobs.getCard(cardId);
        if (cardData == null) {
            Mod.LOGGER.warn("Unknown entity: {}", resourceLocation);
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.failed",
                            new TranslationTextComponent(resourceLocation.toString())),
                    Util.DUMMY_UUID);
            event.setCancellationResult(ActionResultType.SUCCESS);
            event.setCanceled(true);
            return;
        }

        //Mod.LOGGER.debug("Try to consume entity: {}", resourceLocation);
        ItemStack mobCard = CardItem.createStack(cardData);
        if (CollectionBookItem.addCard(stack, mobCard)) {
            playerIn.setHeldItem(hand, stack.copy());
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.consumed",
                            new TranslationTextComponent(mobCard.getTranslationKey())),
                    Util.DUMMY_UUID);
        } else {
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.duplicated"), Util.DUMMY_UUID);
        }
        event.setCancellationResult(ActionResultType.SUCCESS);
        event.setCanceled(true);
    }

    @Nullable
    private static String getJockey(Entity beingRidden) {
        if (beingRidden instanceof SpiderEntity && beingRidden.isPassenger(SkeletonEntity.class)) {
            return "spider_jockey";
        } else if (beingRidden instanceof ChickenEntity && beingRidden.isPassenger(ZombieEntity.class)) {
            return  "chicken_jockey";
        } else if (beingRidden instanceof RavagerEntity && beingRidden.getControllingPassenger() instanceof MobEntity) {
            return  "ravager_jockey";
        } else if (beingRidden instanceof SkeletonHorseEntity && beingRidden.getControllingPassenger() instanceof SkeletonEntity) {
            return  "skeleton_horseman";
        }
        return null;
    }
}
