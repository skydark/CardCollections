package me.skydark.card_collections.init;

import me.skydark.card_collections.Mod;
import me.skydark.card_collections.entity.CardEntityRender;
import me.skydark.card_collections.item.CardItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MOD_ID, value = Dist.CLIENT, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
public class ModClient {
    public static void init(final FMLClientSetupEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CARD.get(), CardEntityRender::new);
    }

    @SubscribeEvent
    public static void onItemColor(ColorHandlerEvent.Item event) {
        event.getItemColors().register(CardItem::getItemColor, ModItems.CARD.get());
    }
}
