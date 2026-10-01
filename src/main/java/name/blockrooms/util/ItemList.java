package name.blockrooms.util;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemList extends ArrayList<ItemStack> {
    public ItemList(List<ItemStack> stackList){
        super(stackList);
    }
    public ItemList(){

    }
    @Override
    public boolean add(ItemStack itemStack) {
        boolean merged = false;
        for (ItemStack existing : this) {
            if (existing.is(itemStack.getItem())) {
                existing.setCount(existing.getCount() + itemStack.getCount());
                merged = true;
                break;
            }
        }
        return merged || super.add(itemStack);
    }
}
