package me.skydark.card_collections;

import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.entity.CardEntityRender;
import me.skydark.card_collections.init.*;
import me.skydark.card_collections.item.CardItem;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@net.minecraftforge.fml.common.Mod(Mod.MOD_ID)
public class Mod
{
    public static final String MOD_ID = "card_collections";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public Mod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfiguration.COMMON_CONFIG);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::doClientStuff);
        modEventBus.addListener(this::registerItemColors);

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Registering messages");
        ModMessages.register();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        LOGGER.info("Registering client side stuff");
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CARD.get(), CardEntityRender::new);
    }

    private void registerItemColors(ColorHandlerEvent.Item event) {
        LOGGER.info("Registering item colors");
        event.getItemColors().register(CardItem::getItemColor, ModItems.CARD.get());
    }

    private void onAddReloadListener(final AddReloadListenerEvent event) {
        LOGGER.info("HELLO FROM AddReloadListenerEvent");
        event.addListener(CardCollectionDataManager.INSTANCE);
    }

    public static ItemGroup CREATIVE_TAB = new ItemGroup(MOD_ID)
    {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModItems.COLLECTION_BOOK.get());
        }
    };
}
