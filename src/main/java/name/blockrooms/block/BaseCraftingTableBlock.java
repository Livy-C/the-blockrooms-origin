package name.blockrooms.block;

import com.mojang.serialization.MapCodec;
import name.blockrooms.block.inventory.BaseCraftingMenu;
import name.blockrooms.util.TriFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BaseCraftingTableBlock extends Block {
    private final Component title;
    private final TriFunction<Integer, Inventory, ContainerLevelAccess, AbstractContainerMenu> factory;
    public BaseCraftingTableBlock(Properties properties) {
        this(BaseCraftingMenu::new, properties);
    }
    public BaseCraftingTableBlock(TriFunction<Integer, Inventory, ContainerLevelAccess, AbstractContainerMenu> factory, Properties properties) {
        this(Component.translatable("container.crafting"), factory, properties);
    }
    public BaseCraftingTableBlock(Component title, TriFunction<Integer, Inventory, ContainerLevelAccess, AbstractContainerMenu> factory, Properties properties) {
        super(properties);
        this.title = title;
        this.factory = factory;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(properties -> new BaseCraftingTableBlock(title, factory, properties));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, access) -> factory.apply(id, inventory, ContainerLevelAccess.create(level, pos)), title);
    }
}
