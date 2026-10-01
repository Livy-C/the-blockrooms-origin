package name.blockrooms.util;

import name.blockrooms.Blockrooms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Set;

public class ModLevels {
    private static final Set<ResourceKey<Level>> BLOCKLEVELS = new HashSet<>();
    //public static final TagKey<Level> IN_BLOCKROOMS = TagKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Blockrooms.MODID, "in_blockrooms"));

    public static final ResourceKey<Level> BLOCKLEVEL_0 = level("blocklevel0");
    public static final ResourceKey<Level> BLOCKLEVEL_1 = level("blocklevel1");
    public static final ResourceKey<Level> BLOCKLEVEL_2 = level("blocklevel2");
    public static final ResourceKey<Level> BLOCKLEVEL_3 = level("blocklevel3");
    public static final ResourceKey<Level> BLOCKLEVEL_4 = level("blocklevel4");
    public static final ResourceKey<Level> BLOCKLEVEL_NULL = level("blocklevel_null");
    public static final ResourceKey<Level> GALLERY = level("the_gallery");

    public static boolean isInBlockrooms(Entity entity) {
        return isInBlockrooms(entity.level().dimension());
    }

    public static boolean isInBlockrooms(ResourceKey<Level> key) {
        return BLOCKLEVELS.stream().anyMatch(key::equals);
    }

    private static ResourceKey<Level> level(String key) {
        var a = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(Blockrooms.MODID, key));
        BLOCKLEVELS.add(a);
        return a;
    }
}
