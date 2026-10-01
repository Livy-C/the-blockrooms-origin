package name.blockrooms.block.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;

public class ErrorCraftingShapelessRecipe extends ErrorCraftingRecipe {
    public static final MapCodec<ErrorCraftingShapelessRecipe> MAP_CODEC =
            RecordCodecBuilder.mapCodec(
                    (i) -> i.group(
                            ItemStackTemplate.CODEC.fieldOf("result").forGetter((o) -> o.result),
                            Codec.lazyInitialized(() -> Ingredient.CODEC.listOf(1, ShapedRecipePattern.getMaxHeight() * ShapedRecipePattern.getMaxWidth()))
                                    .fieldOf("ingredients").forGetter((o) -> o.ingredients)
                    ).apply(i, ErrorCraftingShapelessRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ErrorCraftingShapelessRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStackTemplate.STREAM_CODEC, (o) -> o.result,
                    Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), (o) -> o.ingredients,
                    ErrorCraftingShapelessRecipe::new);
    public static final RecipeSerializer<ErrorCraftingShapelessRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);
    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;
    private final boolean isSimple;

    public ErrorCraftingShapelessRecipe(ItemStackTemplate result, List<Ingredient> ingredients) {
        this.result = result;
        this.ingredients = ingredients;
        this.isSimple = ingredients.stream().allMatch(Ingredient::isSimple);
    }

    @Override
    public RecipeSerializer<? extends ErrorCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.create(this.ingredients);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        } else if (!this.isSimple) {
            ArrayList<ItemStack> nonEmptyItems = new ArrayList<>(input.ingredientCount());
            for (ItemStack item : input.items()) {
                if (!item.isEmpty()) {
                    nonEmptyItems.add(item);
                }
            }
            return RecipeMatcher.findMatches(nonEmptyItems, this.ingredients) != null;
        } else if (input.size() == 1 && this.ingredients.size() == 1) {
            return this.ingredients.getFirst().test(input.getItem(0));
        } else {
            return input.stackedContents().canCraft(this, null);
        }
    }

    @Override
    public ItemStack assemble(CraftingInput craftingInput) {
        return this.result.create();
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(this.ingredients.stream().map(Ingredient::display).toList(), new SlotDisplay.ItemStackSlotDisplay(this.result), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}