package name.blockrooms.item;

import name.blockrooms.Blockrooms;
import name.blockrooms.block.ModBlocks;
import name.blockrooms.entity.ModEntities;
import name.blockrooms.item.consumables.DamageEffect;
import name.blockrooms.item.impl.GlowstoneLanternItem;
import name.blockrooms.item.impl.GunBowItem;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Blockrooms.MODID);
    public static final DeferredItem<Item> ALMOND_MILK_BUCKET =
            ITEMS.registerSimpleItem("almond_milk_bucket",
                    properties -> properties.craftRemainder(Items.BUCKET)
                            .usingConvertsTo(Items.BUCKET).stacksTo(1)
                            .component(DataComponents.CONSUMABLE,
                            Consumables.defaultDrink()
                                    .onConsume(ClearAllStatusEffectsConsumeEffect.INSTANCE)
                                    .onConsume(new ApplyStatusEffectsConsumeEffect(
                                            List.of(
                                                    new MobEffectInstance(MobEffects.REGENERATION, 600, 1),
                                                    new MobEffectInstance(MobEffects.ABSORPTION, 600, 1)
                                            )
                                    ))
                                    .build()));
    public static final DeferredItem<Item> RUBY =
            ITEMS.registerSimpleItem("ruby");
    public static final DeferredItem<Item> RUBY_SWORD =
            ITEMS.registerSimpleItem("ruby_sword", properties -> properties.sword(ModToolMaterials.RUBY, 3.0F, -2.4F));
    public static final DeferredItem<Item> RUBY_SHOVEL =
            ITEMS.registerSimpleItem("ruby_shovel", properties -> properties.shovel(ModToolMaterials.RUBY, 1.5F, -3.0F));
    public static final DeferredItem<Item> RUBY_PICKAXE =
            ITEMS.registerSimpleItem("ruby_pickaxe", properties -> properties.pickaxe(ModToolMaterials.RUBY, 1.0F, -2.8F));
    public static final DeferredItem<Item> RUBY_AXE =
            ITEMS.registerSimpleItem("ruby_axe", properties -> properties.axe(ModToolMaterials.RUBY, 5.0F, -3.0F));
    public static final DeferredItem<Item> RUBY_HOE =
            ITEMS.registerSimpleItem("ruby_hoe", properties -> properties.hoe(ModToolMaterials.RUBY, -3.0F, 0.0F));
    public static final DeferredItem<Item> RUBY_SPEAR =
            ITEMS.registerSimpleItem("ruby_spear", properties -> properties.spear(ModToolMaterials.RUBY, 1.05F, 1.075F, 0.5F, 3.0F, 7.5F, 6.5F, 5.1F, 10.0F, 4.6F));
    public static final DeferredItem<Item> STRING_AXE =
            ITEMS.registerSimpleItem("string_axe", properties -> properties.axe(ModToolMaterials.STRING, 0.0F, -3.2F));
    public static final DeferredItem<Item> GLOWSTONE_LANTERN =
            ITEMS.registerItem("glowstone_lantern", p -> new GlowstoneLanternItem(p, 20),
                    properties -> properties.stacksTo(1).repairable(tag("repairs_glowstone_lantern")).durability(432)
                            .component(DataComponents.BREAK_SOUND, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.FIRE_EXTINGUISH)));
    public static final DeferredItem<Item> STICK_BUNDLE =
            ITEMS.registerSimpleItem("stick_bundle");

    public static final DeferredItem<SpawnEggItem> BLOOD_ZOMBIE_SPAWN_EGG =
            ITEMS.registerItem("blood_zombie_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(ModEntities.BLOOD_ZOMBIE.get()));
    public static final DeferredItem<SpawnEggItem> BLACKSTONE_SHULKER_SPAWN_EGG =
            ITEMS.registerItem("blackstone_shulker_spawn_egg", SpawnEggItem::new,
                    properties -> properties.spawnEgg(ModEntities.BLACKSTONE_SHULKER.get()));

    public static final DeferredItem<BlockItem> HEATED_IRON_BLOCK =
            ITEMS.registerSimpleBlockItem("heated_iron_block", ModBlocks.HEATED_IRON_BLOCK);
    public static final DeferredItem<BlockItem> ERROR_CRAFTING_TABLE =
            ITEMS.registerSimpleBlockItem("error_crafting_table", ModBlocks.ERROR_CRAFTING_TABLE);
    public static final DeferredItem<BlockItem> STONE_CRAFTING_TABLE =
            ITEMS.registerSimpleBlockItem("stone_crafting_table", ModBlocks.STONE_CRAFTING_TABLE);
    public static final DeferredItem<BlockItem> DETECTOR_TORCH =
            ITEMS.registerItem("detector_torch", properties -> new StandingAndWallBlockItem(ModBlocks.DETECTOR_TORCH.get(), ModBlocks.DETECTOR_WALL_TORCH.get(), Direction.DOWN, properties));
    public static final DeferredItem<BlockItem> DETECTOR_REDSTONE_LAMP =
            ITEMS.registerSimpleBlockItem("detector_redstone_lamp", ModBlocks.DETECTOR_REDSTONE_LAMP_BLOCK);
    public static final DeferredItem<GunBowItem> GUNBOW =
            ITEMS.registerItem("gunbow", properties -> new GunBowItem(properties.stacksTo(1)));
    public static final DeferredItem<BlockItem> QUARTZ_ELEVATOR =
            ITEMS.registerSimpleBlockItem("quartz_elevator", ModBlocks.QUARTZ_ELEVATOR);
    public static final DeferredItem<BlockItem> SOFT_COBBLESTONE =
            ITEMS.registerSimpleBlockItem("soft_cobblestone", ModBlocks.SOFT_COBBLESTONE,
                    properties -> properties
                            .component(DataComponents.CONSUMABLE,
                            Consumables.defaultFood()
                                    .onConsume(new DamageEffect(2.0f))
                                    .build())

            );
    public static final DeferredItem<BlockItem> PROCESSED_SOFT_COBBLESTONE =
            ITEMS.registerSimpleBlockItem("processed_soft_cobblestone", ModBlocks.PROCESSED_SOFT_COBBLESTONE,
                    properties -> properties
                            .food(new FoodProperties.Builder()
                                    .nutrition(3)
                                    .saturationModifier((float) 1 / 6)
                                            .build(),
                                    Consumables.defaultFood()
                                            .onConsume(
                                                    new ApplyStatusEffectsConsumeEffect(
                                                            new MobEffectInstance(MobEffects.RESISTANCE, 720)
                                                    )
                                            )
                                            .build()));
    public static final DeferredItem<BlockItem> TELEPORTER_BLOCK =
            ITEMS.registerSimpleBlockItem("teleporter_block", ModBlocks.TELEPORTER_BLOCK);
    public static final DeferredItem<BlockItem> FALLABLE_STONE =
            ITEMS.registerSimpleBlockItem("fallable_stone", ModBlocks.FALLABLE_STONE);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
    public static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Blockrooms.MODID, name));
    }
}
