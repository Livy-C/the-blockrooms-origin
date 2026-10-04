package name.blockrooms;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /* // Demonstrates how to use Neo's config APIs
    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);
     */

    public static final ModConfigSpec.IntValue RUBY_TRANS_TIME = BUILDER
            .defineInRange("ruby_trans_time", 60 * 20, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.DoubleValue TREMBLING_CHANCE = BUILDER
            .defineInRange("trembling_chance", 0.05, 0, 1);
    public static final ModConfigSpec.BooleanValue DO_DISABLE_PAUSE_MENU_BUTTON = BUILDER
            .define("do_disable_pause_menu_button", true);
    public static final ModConfigSpec.BooleanValue DO_DISABLE_MOD_LIST_BUTTON = BUILDER
            .define("do_disable_mod_list_button", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(Identifier.parse(itemName));
    }
}
