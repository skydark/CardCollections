package me.skydark.card_collections.data;

import com.google.common.collect.ImmutableMap;
import me.skydark.card_collections.Mod;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class CardCollectionData {
    public String color_filter;
    public int[] colors;
    public Map<String, Integer> dimensions;
    public Map<ResourceLocation, CardData> cards;

    private String id;

    public void init(String id) {
        this.id = id;
        if (this.dimensions == null) {
            // TODO: default dimensions
            this.dimensions = ImmutableMap.of();
        }
        // TODO: filter invalid cards
        if (this.cards == null) {
            this.cards = new HashMap<>();
        }
        Mod.LOGGER.info("Collection {} is initialized", id);
    }

    public String getId() { return id; }

    public void registerCard(ResourceLocation rl, CardData card) {
        //Mod.LOGGER.info("Registering card {} into collection {}", rl, id);
        if (cards == null) {
            cards = new HashMap<>();
        }
        // TODO: normalize card dimensions
        cards.put(rl, card);
    }

    @Nullable
    public CardData getCard(ResourceLocation rl) {
        return cards.get(rl);
    }

    @Nullable
    public CardData getCard(String cardId) {
        return cards.get(new ResourceLocation(this.id, cardId));
    }

    public List<CardData> getValidCards(Map<String, Integer> dimFilters) {
        return cards.values().stream().filter(cardData -> cardData.isMatched(dimFilters)).collect(Collectors.toList());
    }
    
    @Nullable
    public CardData gacha(Map<String, Integer> dimFilters) {
        List<CardData> validCards = getValidCards(dimFilters);
        if (validCards.isEmpty()) {
            return null;
        }
        TreeMap<Integer, CardData> weightedValidCards = new TreeMap<>();
        int totalWeight = 0;
        for (CardData card : validCards) {
            if (card.weight > 0) {
                totalWeight += card.weight;
                weightedValidCards.put(totalWeight, card);
            }
        }
        if (weightedValidCards.isEmpty()) return null;
        Random rand = new Random();
        return weightedValidCards.ceilingEntry(rand.nextInt(totalWeight) + 1).getValue();
    }

    public int getColor(CardData cardData) {
        Integer dimValue = cardData.dimensions.get(color_filter);
        if (dimValue == null || dimValue == 0 || dimValue > colors.length) {
            return 0xFFFFFF;
        }
        return Math.max(0, Math.min(0xFFFFFF, colors[dimValue - 1]));
    }

    @Nullable
    public static CardCollectionData getCollectionData(ItemStack stack) {
        CompoundNBT compoundNBT = stack.getTag();
        if (compoundNBT == null) {
            compoundNBT = new CompoundNBT();
        }
        String collectionId = compoundNBT.getString("collection");
        if (collectionId.isEmpty()) {
            Mod.LOGGER.warn("NBT tag 'collection' is not found");
            return null;
        }
        CardCollectionData collectionData = CardCollectionDataManager.INSTANCE.getCollection(collectionId);
        if (collectionData == null) {
            Mod.LOGGER.warn("Card collection is not found: {}", collectionId);
        }
        return collectionData;
    }

    public List<Map<String, Integer>> getCreativeDimFilters() {
        List<Map<String, Integer>> dimFilters = new ArrayList<>();
        dimFilters.add(new HashMap<>());
        Integer size = dimensions.get(color_filter);
        if (size != null && size > 0) {
            for (int i = 1; i <= size; i++) {
                Map<String, Integer> dimFilter = new HashMap<>();
                dimFilter.put(color_filter, i);
                dimFilters.add(dimFilter);
            }
        }
        return dimFilters;
    }

    public Map<String, Integer> readDimFilters(ItemStack stack) {
        Map<String, Integer> dimFilters = new HashMap<>();
        CompoundNBT compoundNBT = stack.getTag();
        if (compoundNBT == null) {
            return dimFilters;
        }
        for (Map.Entry<String, Integer> entry : this.dimensions.entrySet()) {
            String dimKey = entry.getKey();
            Integer size = entry.getValue();
            int value = compoundNBT.getInt("m_" +dimKey);
            if (value > 0 && value <= size) {
                dimFilters.put(dimKey, value);
            }
        }
        return dimFilters;
    }

    public void writeDimFilters(ItemStack stack, @Nullable Map<String, Integer> dimFilters) {
        CompoundNBT compound = stack.getTag();
        if (compound == null) {
            compound = new CompoundNBT();
        }
        compound.putString("collection", this.getId());
        if (dimFilters != null) {
            SortedSet<String> keys = new TreeSet<>(dimFilters.keySet());
            for (String dimKey : keys) {
                Integer value = dimFilters.get(dimKey);
                compound.putInt("m_" + dimKey, value);
            }
        }
        stack.setTag(compound);
    }

    public String getTranslationKeyOfDim(String dimKey) {
        return String.format("card_collections.%s.m_%s", getId(), dimKey);
    }

    public String getTranslationKeyOfDimValue(String dimKey, int value) {
        return String.format("card_collections.%s.m_%s_%s", getId(), dimKey, value);
    }

    public String getTranslationKeyOfName() {
        return String.format("card_collections.%s.name", getId());
    }

    public void writeToBuffer(PacketBuffer buf) {
        buf.writeString(id);
        buf.writeString(color_filter);
        buf.writeVarIntArray(colors);

        buf.writeInt(dimensions.size());
        dimensions.forEach((dim, val) -> {
            buf.writeString(dim);
            buf.writeInt(val);
        });

        buf.writeInt(cards.size());
        cards.forEach((rl, card) -> {
            card.writeToBuffer(buf);
        });
    }

    public static CardCollectionData readFromBuffer(PacketBuffer buf) {
        CardCollectionData collection = new CardCollectionData();
        collection.init(buf.readString());
        collection.color_filter = buf.readString(32767);
        collection.colors = buf.readVarIntArray();

        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        int size = buf.readInt();
        for (int i=0; i<size; i++) {
            String key = buf.readString(32767);
            Integer value = buf.readInt();
            builder.put(key, value);
        }
        collection.dimensions = builder.build();

        for (int i = buf.readInt(); i > 0; i--) {
            CardData card = CardData.readFromBuffer(buf);
            collection.registerCard(card.getResourceLocation(), card);
        }
        return collection;
    }
}
