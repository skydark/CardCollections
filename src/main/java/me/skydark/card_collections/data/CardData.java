package me.skydark.card_collections.data;

import com.google.common.collect.ImmutableMap;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.Objects;

public class CardData {
    public Map<String, Integer> dimensions;
    public int weight = 1;

    private ResourceLocation resourceLocation;

    public ResourceLocation getResourceLocation() { return this.resourceLocation; }
    public void setResourceLocation(ResourceLocation rl) { this.resourceLocation = rl; }

    public String getCollectionId() {
        return resourceLocation.getNamespace();
    }

    public String getCardId() {
        return resourceLocation.getPath();
    }

    public ResourceLocation getTexture() {
        return new ResourceLocation(resourceLocation.getNamespace(), "textures/card/" + resourceLocation.getPath() + ".png");
    }

    public CardCollectionData getCollection() {
        return CardCollectionDataManager.INSTANCE.getCollection(getCollectionId());
    }

    public boolean isMatched(Map<String, Integer> dimFilters) {
        for (Map.Entry<String, Integer> entry : dimFilters.entrySet()) {
            if (!Objects.equals(dimensions.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public int getColor() {
        CardCollectionData collectionData = getCollection();
        if (collectionData == null) {
            return 0xFFFFFF;
        }
        return collectionData.getColor(this);
    }

    public String getTranslationKey() {
        return String.format("card.%s.%s.name", getCollectionId(), getCardId());
    }
    public String getTranslationKeyOfDesc() {
        return String.format("card.%s.%s.desc", getCollectionId(), getCardId());
    }

    public void writeToBuffer(PacketBuffer buf) {
        buf.writeResourceLocation(resourceLocation);
        if (dimensions == null) {
            buf.writeInt(0);
            return;
        }
        buf.writeInt(dimensions.size());
        dimensions.forEach((dim, val) -> {
            buf.writeString(dim);
            buf.writeInt(val);
        });
    }

    public static CardData readFromBuffer(PacketBuffer buf) {
        CardData card = new CardData();
        card.setResourceLocation(buf.readResourceLocation());
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        int size = buf.readInt();
        for (int i=0; i<size; i++) {
            String key = buf.readString(32767);
            Integer value = buf.readInt();
            builder.put(key, value);
        }
        card.dimensions = builder.build();
        return card;
    }
}
