package me.skydark.card_collections.item;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardCollectionData;
import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.data.CardData;
import me.skydark.card_collections.entity.CardEntity;
import me.skydark.card_collections.init.ModItems;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class CardItem extends Item
{
    public CardItem(Properties properties) {
        super(properties);
    }

    protected boolean canPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        return !World.isOutsideBuildHeight(posIn) && !directionIn.getAxis().isVertical() && playerIn.canPlayerEdit(posIn, directionIn, itemStackIn);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        Direction direction = context.getFace();
        BlockPos blockpos = context.getPos().offset(direction);
        PlayerEntity playerentity = context.getPlayer();
        ItemStack itemstack = context.getItem();
        if (playerentity == null || !this.canPlace(playerentity, direction, itemstack, blockpos)) {
            return ActionResultType.FAIL;
        } else {
            CardData cardData = getCard(itemstack);
            if (cardData == null) { return ActionResultType.FAIL; }
            World world = context.getWorld();
            CardEntity cardEntity = new CardEntity(world, blockpos, direction, cardData);

            CompoundNBT compoundnbt = itemstack.getTag();
            if (compoundnbt != null) {
                EntityType.applyItemNBT(world, playerentity, cardEntity, compoundnbt);
            }

            if (cardEntity.onValidSurface()) {
                if (!world.isRemote) {
                    cardEntity.playPlaceSound();
                    world.addEntity(cardEntity);
                }

                itemstack.shrink(1);
                return ActionResultType.func_233537_a_(world.isRemote);
            } else {
                return ActionResultType.CONSUME;
            }
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (world.isRemote){
            CardData cardData = getCard(stack);
            if (cardData != null) {
                Mod.proxy.openCardGui(cardData);
            }
        }
        return ActionResult.resultSuccess(stack);
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        CardData cardData = getCard(stack);
        return cardData != null ? cardData.getTranslationKey() : super.getTranslationKey(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        CardData cardData = getCard(stack);
        if (cardData == null) {
            tooltip.add(new TranslationTextComponent("tooltip.card_collections.card.invalid").mergeStyle(TextFormatting.RED));
            return;
        }
        CardCollectionData collectionData = cardData.getCollection();
        if (collectionData == null) {
            tooltip.add(new TranslationTextComponent("tooltip.card_collections.card.invalid").mergeStyle(TextFormatting.RED));
            return;
        }
        tooltip.add(new TranslationTextComponent(collectionData.getTranslationKeyOfName()).mergeStyle(TextFormatting.ITALIC));

        if (!Screen.hasShiftDown()) {
            tooltip.add(new TranslationTextComponent("tooltip.card_collections.card").mergeStyle(TextFormatting.BLUE));
        } else {
            if (cardData.dimensions != null) {
                SortedSet<String> keys = new TreeSet<>(cardData.dimensions.keySet());
                for (String dimKey : keys) {
                    Integer value = cardData.dimensions.get(dimKey);
                    tooltip.add(new TranslationTextComponent(collectionData.getTranslationKeyOfDim(dimKey))
                            .mergeStyle(TextFormatting.DARK_GREEN)
                            .appendString(": ")
                            .appendSibling(new TranslationTextComponent(collectionData.getTranslationKeyOfDimValue(dimKey, value))
                                    .mergeStyle(TextFormatting.AQUA)));
                }
            }
        }
    }

    @Nullable
    public static CardData getCard(ItemStack stack) {
        if (!(stack.getItem() instanceof CardItem)) { return null; }
        CompoundNBT compound = stack.getTag();
        if (compound == null) { return null; }
        ResourceLocation rl = ResourceLocation.tryCreate(compound.getString("rl"));
        if (rl == null) { return null; }
        return CardCollectionDataManager.INSTANCE.getCard(rl);
    }

    public static ItemStack createStack(CardData cardData) {
        ItemStack stack = new ItemStack(ModItems.CARD.get());
        if (stack.getTag() == null)
        {
            stack.setTag(new CompoundNBT());
        }
        CompoundNBT compound = stack.getTag();
        compound.putString("collection", cardData.getCollectionId());
        compound.putString("rl", cardData.getResourceLocation().toString());
        stack.setTag(compound);
        return stack;
    }

    public static int getItemColor(ItemStack stack, int i) {
        CardData cardData = getCard(stack);
        if (cardData != null) {
            return cardData.getColor();
        }
        return 0xFFFFFF;
    }
}
