package me.skydark.card_collections.init;

import me.skydark.card_collections.Mod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Mod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModConfiguration {
    public static final String CATEGORY_GENERAL = "general";

    public static ForgeConfigSpec COMMON_CONFIG;

    public static ForgeConfigSpec.ConfigValue<List<? extends String>> COLLECTION_WHITELIST;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> COLLECTION_BLACKLIST;
    public static ForgeConfigSpec.BooleanValue GACHA_TO_BOOK;

    static {
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();

        COMMON_BUILDER.comment("General settings").push(CATEGORY_GENERAL);
        COLLECTION_WHITELIST = COMMON_BUILDER
                .comment("List of card collection id whitelist, leave empty to load all datapacks")
                .defineList("collectionWhitelist", new ArrayList<>(), o -> o instanceof String);
        COLLECTION_BLACKLIST = COMMON_BUILDER
                .comment("List of card collection id blacklist, ignore when whitelist is not empty")
                .defineList("collectionBlacklist", new ArrayList<>(), o -> o instanceof String);
        GACHA_TO_BOOK = COMMON_BUILDER
                .comment("Allow shift-right click to convert gacha to collection book")
                        .define("gachaToBook", true);
        COMMON_BUILDER.pop();

        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    @SubscribeEvent
    public static void onCommonReload(ModConfig.ModConfigEvent ev){
    }
}
