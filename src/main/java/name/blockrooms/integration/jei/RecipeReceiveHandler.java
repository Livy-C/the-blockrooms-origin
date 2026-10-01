package name.blockrooms.integration.jei;

import name.blockrooms.Blockrooms;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;

@EventBusSubscriber(modid = Blockrooms.MODID, value = Dist.CLIENT)
public class RecipeReceiveHandler {
    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        if (BlockroomsJeiPlugin.isJEIAvailable()) {
            BlockroomsJeiPlugin.recipes = event.getRecipeMap();
        }
    }
}
