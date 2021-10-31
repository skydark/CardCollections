package me.skydark.card_collections;

import me.skydark.card_collections.data.CardCollectionDataManager;
import me.skydark.card_collections.init.*;
import me.skydark.card_collections.proxy.ClientProxy;
import me.skydark.card_collections.proxy.IProxy;
import me.skydark.card_collections.proxy.ServerProxy;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@net.minecraftforge.fml.common.Mod(Mod.MOD_ID)
public class Mod
{
    public static final String MOD_ID = "card_collections";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static IProxy proxy = DistExecutor.safeRunForDist(() -> ClientProxy::new, () -> ServerProxy::new);

    public Mod()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfiguration.COMMON_CONFIG);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ModClient::init);

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Registering messages");
        ModMessages.register();
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        CardCollectionDataManager.INSTANCE.syncToClient(event.getPlayer());
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
