package name.blockrooms.block.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

import java.util.List;

public class ErrorCraftingShapedRecipe extends ErrorCraftingRecipe {
    public static final MapCodec<ErrorCraftingShapedRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    (i) -> i.group(
                            ShapedRecipePattern.MAP_CODEC.forGetter((o) -> o.pattern),
                            ItemStackTemplate.CODEC.fieldOf("result").forGetter((o) -> o.result)
                    ).apply(i, ErrorCraftingShapedRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingShapedRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ShapedRecipePattern.STREAM_CODEC, (o) -> o.pattern,
                    ItemStackTemplate.STREAM_CODEC, (o) -> o.result,
                    ErrorCraftingShapedRecipe::new);
    public static final RecipeSerializer<ErrorCraftingShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    public final ShapedRecipePattern pattern;
    private final ItemStackTemplate result;

    public ErrorCraftingShapedRecipe(ShapedRecipePattern pattern, ItemStackTemplate result) {
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public RecipeSerializer<? extends ErrorCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(this.pattern.ingredients());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return this.pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput) {
        return this.result.create();
    }

    public int getWidth() {
        return this.pattern.width();
    }

    public int getHeight() {
        return this.pattern.height();
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapedCraftingRecipeDisplay(this.pattern.width(), this.pattern.height(), this.pattern.ingredients().stream().map((e) -> (SlotDisplay)e.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE)).toList(), new SlotDisplay.ItemStackSlotDisplay(this.result), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}