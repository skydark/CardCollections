package me.skydark.card_collections.data;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import me.skydark.card_collections.Mod;
import me.skydark.card_collections.init.ModConfiguration;
import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class CardCollectionDataManager extends JsonReloadListener {
    private static final Gson GSON = (new GsonBuilder())
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .setPrettyPrinting()
            .create();
    public static final CardCollectionDataManager INSTANCE = new CardCollectionDataManager("card_collections");

    private static final String COLLECTION_PATH = "main";
    private Map<String, CardCollectionData> collections = ImmutableMap.of();

    private CardCollectionDataManager(String name) {
        super(GSON, name);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
        Mod.LOGGER.info("Reloading card collections ...");
        Map<String, CardCollectionData> collections = new HashMap<>();
        Map<ResourceLocation, CardData> cards = new HashMap<>();
        Set<String> collectionWhitelist = ModConfiguration.COLLECTION_WHITELIST.get().stream().collect(Collectors.toSet());
        Set<String> collectionBlacklist = ModConfiguration.COLLECTION_BLACKLIST.get().stream().collect(Collectors.toSet());
        objectIn.forEach((rl, element) -> {
            if (COLLECTION_PATH.equalsIgnoreCase(rl.getPath())) {
                // this is collection definition json
                try {
                    String collectionId = rl.getNamespace();
                    if (collectionWhitelist.isEmpty() && collectionBlacklist.contains(collectionId)
                     || !collectionWhitelist.isEmpty() && !collectionWhitelist.contains(collectionId)) {
                        Mod.LOGGER.info("Skip to load card collection {} due to configuration.", collectionId);
                        return;
                    }
                    CardCollectionData collectionData = GSON.fromJson(element, CardCollectionData.class);
                    collections.put(collectionId, collectionData);
                    Mod.LOGGER.info("Loaded card collection {}", rl);
                } catch (Exception exception) {
                    Mod.LOGGER.error("Couldn't parse card collection {}", rl, exception);
                }
            } else {
                // this is card data
                try {
                    CardData cardData = GSON.fromJson(element, CardData.class);
                    cards.put(rl, cardData);
                    //Mod.LOGGER.info("Loaded card data {}", rl);
                } catch (Exception exception) {
                    Mod.LOGGER.error("Couldn't parse card data {}", rl, exception);
                }
            }
        });

        // register collections
        collections.forEach((id, collectionData) -> {
            collectionData.init(id);
        });
        this.collections = ImmutableMap.copyOf(collections);

        // register cards to collections
        cards.forEach((rl, card) -> {
            card.setResourceLocation(rl);
            // TODO: if this is already registered, skip
            CardCollectionData _collectionData = CardCollectionDataManager.INSTANCE.getCollection(rl.getNamespace());
            if (_collectionData == null){
                Mod.LOGGER.warn("Couldn't register card (collection not found) {}", rl);
            } else {
                _collectionData.registerCard(rl, card);
            }
        });
    }

    public List<CardCollectionData> getCollections() {
        return new ArrayList<>(collections.values());
    }

    @Nullable
    public CardCollectionData getCollection(String id) {
        return collections.get(id);
    }

    @Nullable
    public CardData getCard(ResourceLocation rl) {
        if (rl == null) { return null; }
        CardCollectionData collectionData = getCollection(rl.getNamespace());
        if (collectionData == null) { return null; }
        return collectionData.getCard(rl);
    }
}
