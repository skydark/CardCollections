package me.skydark.card_collections.init;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.item.CardItem;
import me.skydark.card_collections.item.CollectionBookItem;
import me.skydark.card_collections.item.GachaItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Mod.MOD_ID);
    public static final RegistryObject<CardItem> CARD = register("card", new CardItem(new Item.Properties().group(Mod.CREATIVE_TAB)));
    public static final RegistryObject<GachaItem> GACHA = register("gacha", new GachaItem(new Item.Properties().group(Mod.CREATIVE_TAB)));
    public static final RegistryObject<CollectionBookItem> COLLECTION_BOOK = register("collection_book", new CollectionBookItem(new Item.Properties().maxStackSize(1).group(Mod.CREATIVE_TAB)));

    private static <T extends Item> RegistryObject<T> register(String name, T item)
    {
        return ModItems.ITEMS.register(name, () -> item);
    }
}
