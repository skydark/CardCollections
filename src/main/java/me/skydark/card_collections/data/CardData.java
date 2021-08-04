package me.skydark.card_collections.data;

import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.Objects;

public class CardData {
    public Map<String, Integer> dimensions;

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
}
