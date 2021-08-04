package me.skydark.card_collections.item;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardCollectionData;
import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.data.CardData;
import me.skydark.card_collections.init.ModConfiguration;
import me.skydark.card_collections.init.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class GachaItem extends Item
{
    public GachaItem(Properties properties) {
        super(properties);
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            for (CardCollectionData collectionData: CardCollectionDataManager.INSTANCE.getCollections()) {
                for (Map<String, Integer> dimFilters : collectionData.getCreativeDimFilters()) {
                    ItemStack stack = new ItemStack(ModItems.GACHA.get());
                    collectionData.writeDimFilters(stack, dimFilters);
                    items.add(stack);
                }
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        playerIn.addStat(Stats.ITEM_USED.get(this));
        playerIn.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
        ItemStack gachaItemStack = playerIn.getHeldItem(handIn);
        if (!(playerIn instanceof ServerPlayerEntity)) {
            return new ActionResult<>(ActionResultType.SUCCESS, gachaItemStack);
        }

        CardData cardData = construct(gachaItemStack);
        if (cardData == null) {
            if (!worldIn.isRemote) {
                playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.gacha.invalid"), Util.DUMMY_UUID);
            }
            return ActionResult.resultFail(gachaItemStack);
        }
        if (!worldIn.isRemote) {
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.gacha.get")
                            .appendSibling(new TranslationTextComponent(cardData.getTranslationKey()))
                    , Util.DUMMY_UUID);
        }

        ItemStack cardItemStack = CardItem.createStack(cardData);

        if (playerIn.isSneaking()) {
            if (!ModConfiguration.GACHA_TO_BOOK.get()) {
                if (!worldIn.isRemote) {
                    playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.gacha.no_gacha_to_book"), Util.DUMMY_UUID);
                }
                return ActionResult.resultFail(gachaItemStack);
            }
            CardCollectionData collectionData = cardData.getCollection();
            if (collectionData == null) {
                return ActionResult.resultFail(gachaItemStack);
            }
            ItemStack collectionBookStack = CollectionBookItem.createStack(collectionData, collectionData.readDimFilters(gachaItemStack));
            if (!CollectionBookItem.addCard(collectionBookStack, cardItemStack)) {
                if (!worldIn.isRemote) {
                    playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.gacha.invalid"), Util.DUMMY_UUID);
                }
                return ActionResult.resultFail(gachaItemStack);
            }
            return ActionResult.func_233538_a_(DrinkHelper.fill(gachaItemStack, playerIn, collectionBookStack, false), worldIn.isRemote());
        } else {
            return ActionResult.func_233538_a_(DrinkHelper.fill(gachaItemStack, playerIn, cardItemStack, false), worldIn.isRemote());
        }
    }

    // random select, return null when failed
    @Nullable
    private static CardData construct(ItemStack stack) {
        CardCollectionData collectionData = CardCollectionData.getCollectionData(stack);
        if (collectionData == null) {
            return null;
        }
        Map<String, Integer> dimFilters = collectionData.readDimFilters(stack);
        CardData cardData = collectionData.gacha(dimFilters);
        if (cardData == null) {
            Mod.LOGGER.warn("No valid cards to gacha in collection {}: {}", collectionData.getId(), dimFilters);
            return null;
        }
        Mod.LOGGER.debug("Get card: {}", cardData.getResourceLocation());
        return cardData;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        CardCollectionData collectionData = CardCollectionData.getCollectionData(stack);
        if (collectionData == null) {
            tooltip.add(new TranslationTextComponent("tooltip.card_collections.gacha.invalid").mergeStyle(TextFormatting.RED));
            return;
        }
        tooltip.add(new TranslationTextComponent(collectionData.getTranslationKeyOfName()).mergeStyle(TextFormatting.ITALIC));
        tooltip.add(new TranslationTextComponent("tooltip.card_collections.gacha").mergeStyle(TextFormatting.BLUE));

        Map<String, Integer> dimFilters = collectionData.readDimFilters(stack);
        SortedSet<String> keys = new TreeSet<>(dimFilters.keySet());
        for (String dimKey : keys) {
            Integer value = dimFilters.get(dimKey);
            tooltip.add(new TranslationTextComponent(collectionData.getTranslationKeyOfDim(dimKey))
                    .mergeStyle(TextFormatting.DARK_GREEN)
                    .appendString(": ")
                    .appendSibling(new TranslationTextComponent(collectionData.getTranslationKeyOfDimValue(dimKey, value))
                            .mergeStyle(TextFormatting.AQUA)));
        }
    }
}
