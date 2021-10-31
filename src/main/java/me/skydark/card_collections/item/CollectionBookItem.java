package me.skydark.card_collections.item;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.data.CardCollectionData;
import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.data.CardData;
import me.skydark.card_collections.init.ModConfiguration;
import me.skydark.card_collections.init.ModItems;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;

public class CollectionBookItem extends Item
{
    public CollectionBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            ItemStack encyclopedia = getMcMobsEncyclopedia();
            if (!encyclopedia.isEmpty()) {
                items.add(getMcMobsEncyclopedia());
            }
            for(CardCollectionData collectionData: CardCollectionDataManager.INSTANCE.getCollections()) {
                Map<String, Integer> dimFilters = new HashMap<>();
                items.add(createFullStack(collectionData, dimFilters));
            }
        }
    }

    private static final String TAG_MCMOBs_ENCYCLOPEDIA = "special_collection_book";
    private static final String TAG_VALUE_MCMOBs_ENCYCLOPEDIA = "mcmobs_encyclopedia";
    private static final String TAG_MCMOBs_ENCYCLOPEDIA_COLLECTION_ID = "mcmobs";

    public static ItemStack getMcMobsEncyclopedia() {
        if (!ModConfiguration.SPAWN_MCMOBS_ENCYCLOPEDIA.get()) {
            return ItemStack.EMPTY;
        }
        if (CardCollectionDataManager.INSTANCE.getCollection(TAG_MCMOBs_ENCYCLOPEDIA_COLLECTION_ID) == null) {
            return ItemStack.EMPTY;
        }
        ItemStack encyclopedia = new ItemStack(ModItems.COLLECTION_BOOK.get());
        CompoundNBT tag = encyclopedia.getOrCreateTag();
        tag.putString("collection", TAG_MCMOBs_ENCYCLOPEDIA_COLLECTION_ID);
        tag.putString(TAG_MCMOBs_ENCYCLOPEDIA, TAG_VALUE_MCMOBs_ENCYCLOPEDIA);
        encyclopedia.setTag(tag);
        return encyclopedia;
    }

    public static boolean isMcMobsEncyclopedia(ItemStack itemStack) {
        if (itemStack.isEmpty() || itemStack.getItem() != ModItems.COLLECTION_BOOK.get() || itemStack.getTag() == null) return false;
        String tagValue = itemStack.getTag().getString(TAG_MCMOBs_ENCYCLOPEDIA);
        return TAG_VALUE_MCMOBs_ENCYCLOPEDIA.equals(tagValue);
    }

    //@Override
    public ActionResultType itemInteractionForEntityX(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        if (!CollectionBookItem.isMcMobsEncyclopedia(stack)) {
            return super.itemInteractionForEntity(stack, playerIn, target, hand);
        }
        if (playerIn.getEntityWorld().isRemote) {
            return ActionResultType.SUCCESS;
        }
        String entityString = target.getEntityString();
        Mod.LOGGER.debug("Seen entityS: {}", entityString);
        EntityType<?> entitytype = target.getType();
        ResourceLocation resourceLocation = EntityType.getKey(entitytype);
        //ResourceLocation resourceLocation = EntityType.getKey(target.getType());
        Mod.LOGGER.debug("Seen entity: {}", resourceLocation);
        if (!"minecraft".equals(resourceLocation.getNamespace())) {
            Mod.LOGGER.info("Not a vanilla entity: {}", resourceLocation);
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.failed",
                            new TranslationTextComponent(resourceLocation.toString())),
                    Util.DUMMY_UUID);
            return ActionResultType.SUCCESS;
        }
        CardCollectionData mcmobs = CardCollectionDataManager.INSTANCE.getCollection("mcmobs");
        if (mcmobs == null) {
            Mod.LOGGER.warn("Card collection mcmobs is not loaded");
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.failed",
                            new TranslationTextComponent(resourceLocation.toString())),
                    Util.DUMMY_UUID);
            return ActionResultType.SUCCESS;
        }
        String cardId = resourceLocation.getPath();
        // special cases:
        if (target.isChild() && ("piglin".equals(cardId) || "polar_bear".equals(cardId))) {
            cardId = cardId + "_baby";
        }

        CardData cardData = mcmobs.getCard(cardId);
        if (cardData == null) {
            Mod.LOGGER.warn("Unknown entity: {}", resourceLocation);
            playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.mcmobs_encyclopedia.failed",
                            new TranslationTextComponent(resourceLocation.toString())),
                    Util.DUMMY_UUID);
            return ActionResultType.SUCCESS;
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
        return ActionResultType.SUCCESS;
    }

    private static ItemStack createFullStack(CardCollectionData collectionData, Map<String, Integer> dimFilters) {
        ItemStack collectionBook = createStack(collectionData, dimFilters);

        CompoundNBT compoundNBT = collectionBook.getTag();
        if (compoundNBT == null) { compoundNBT = new CompoundNBT(); }
        ListNBT cardListNBT = compoundNBT.getList("cards", 8);      // get string list
        for (CardData cardData: collectionData.getValidCards(dimFilters)) {
            String cardId = cardData.getCardId();
            StringNBT cardIdNBT = StringNBT.valueOf(cardId);
            cardListNBT.add(cardIdNBT);
        }
        compoundNBT.put("cards", cardListNBT);
        collectionBook.setTag(compoundNBT);
        return collectionBook;
    }

    public static ItemStack createStack(CardCollectionData collectionData, @Nullable Map<String, Integer> dimFilters) {
        ItemStack stack = new ItemStack(ModItems.COLLECTION_BOOK.get());
        collectionData.writeDimFilters(stack, dimFilters);
        return stack;
    }

    public static boolean addCard(ItemStack collectionBook, ItemStack card) {
        CardCollectionData collectionData = CardCollectionData.getCollectionData(collectionBook);
        if (collectionData == null) { return false; }
        Map<String, Integer> dimFilters = collectionData.readDimFilters(collectionBook);

        CardData cardData = CardItem.getCard(card);
        if (cardData == null) { return false; }
        if (!collectionData.getId().equals(cardData.getCollectionId())) { return false; }
        if (!cardData.isMatched(dimFilters)) { return false; }

        CompoundNBT compoundNBT = collectionBook.getTag();
        if (compoundNBT == null) { compoundNBT = new CompoundNBT(); }
        ListNBT cardListNBT = compoundNBT.getList("cards", 8);      // get string list

        String cardId = cardData.getCardId();
        StringNBT cardIdNBT = StringNBT.valueOf(cardId);
        if (cardListNBT.contains(cardIdNBT)) { return false; }

        cardListNBT.add(cardIdNBT);
        compoundNBT.put("cards", cardListNBT);
        collectionBook.setTag(compoundNBT);
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack handStack = playerIn.getHeldItem(handIn);
        if (playerIn.isSneaking()) {
            // collect cards from inventory
            playerIn.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
            return collectCards(worldIn, playerIn, handStack);
        } else {
            // show gui
            playerIn.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F);
            if (worldIn.isRemote) {
                if (handStack.getItem() == ModItems.COLLECTION_BOOK.get()) {
                    Mod.proxy.openCollectionBookGui(playerIn, handStack, handIn);
                }
            }
            //playerIn.addStat(Stats.ITEM_USED.get(this));
            return ActionResult.func_233538_a_(handStack, worldIn.isRemote);
        }
    }

    private ActionResult<ItemStack> collectCards(World worldIn, PlayerEntity playerIn, ItemStack handStack) {
        if (!worldIn.isRemote) {
            int consumed = 0;
            for(ItemStack curStack : playerIn.inventory.mainInventory) {
                if (!curStack.isEmpty() && curStack.getItem() == ModItems.CARD.get()) {
                    if (addCard(handStack, curStack)) {
                        curStack.shrink(1);
                        consumed++;
                    }
                }
            }
            if (playerIn instanceof ServerPlayerEntity) {
                playerIn.sendMessage(new TranslationTextComponent("chat.card_collections.collection_book.consumed", consumed), Util.DUMMY_UUID);
            }
        }

        return ActionResult.func_233538_a_(handStack, worldIn.isRemote);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        CardCollectionData collectionData = CardCollectionData.getCollectionData(stack);
        if (collectionData == null) {
            tooltip.add(new TranslationTextComponent("tooltip.card_collections.collection_book.invalid").mergeStyle(TextFormatting.RED));
            return;
        }

        if (isMcMobsEncyclopedia(stack)) {
            tooltip.add(new TranslationTextComponent("tooltip.card_collections.collection_book.mcmobs_encyclopedia").mergeStyle(TextFormatting.RED));
            tooltip.add(new TranslationTextComponent("tooltip.card_collections.collection_book.mcmobs_encyclopedia.desc").mergeStyle(TextFormatting.BLUE));
        }

        CompoundNBT compoundNBT = stack.getTag();
        if (compoundNBT == null) {
            compoundNBT = new CompoundNBT();
        }

        Map<String, Integer> dimFilters = collectionData.readDimFilters(stack);
        // get string list
        ListNBT cardListNBT = compoundNBT.getList("cards", 8);
        tooltip.add(new TranslationTextComponent(collectionData.getTranslationKeyOfName())
                .mergeStyle(TextFormatting.ITALIC)
                .appendString(": " + cardListNBT.size() + " / " + collectionData.getValidCards(dimFilters).size()));

        tooltip.add(new TranslationTextComponent("tooltip.card_collections.collection_book").mergeStyle(TextFormatting.BLUE));

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
