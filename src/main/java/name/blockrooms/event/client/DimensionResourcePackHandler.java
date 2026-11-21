package name.blockrooms.event.client;

import name.blockrooms.Blockrooms;
import name.blockrooms.util.ModLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Blockrooms.MODID, value = Dist.CLIENT)
public class DimensionResourcePackHandler {

    private static final Map<ResourceKey<Level>, String> DIMENSION_PACKS = Map.of(
            ModLevels.BLOCKLEVEL_1, "high_contrast"
    );

    private static ResourceKey<Level> lastDimension;

    private static final Set<String> ENABLED_BY_MOD = new LinkedHashSet<>();

    private static boolean stateLoaded;
    private static Path stateFile;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        ResourceKey<Level> current = mc.level.dimension();
        if (current.equals(lastDimension)) {
            return;
        }
        lastDimension = current;
        if (!stateLoaded) {
            stateLoaded = true;
            ENABLED_BY_MOD.addAll(loadState());
        }

        PackRepository repo = mc.getResourcePackRepository();
        String targetPack = DIMENSION_PACKS.get(current);

        if (targetPack != null && !ENABLED_BY_MOD.contains(targetPack)) {
            enablePack(repo, targetPack);
        } else if (!DIMENSION_PACKS.containsKey(current) && !ENABLED_BY_MOD.isEmpty()) {
            restorePacks(repo);
        }
    }
    private static void enablePack(PackRepository repo, String packId) {
        Pack pack = repo.getPack(packId);
        if (pack == null) {
            Blockrooms.LOGGER.warn("DimensionResourcePack: pack '{}' not found", packId);
            return;
        }
        Set<String> selected = new LinkedHashSet<>();
        for (Pack p : repo.getSelectedPacks()) {
            selected.add(p.getId());
        }
        selected.add(packId);
        ENABLED_BY_MOD.add(packId);
        saveState();
        repo.setSelected(selected);
        Minecraft.getInstance().delayTextureReload();
        Blockrooms.LOGGER.info("DimensionResourcePack: enabled '{}' in {}", packId, Minecraft.getInstance().level.dimension());
    }

    private static void restorePacks(PackRepository repo) {
        if (ENABLED_BY_MOD.isEmpty()) {
            return;
        }
        Set<String> selected = new LinkedHashSet<>();
        for (Pack p : repo.getSelectedPacks()) {
            selected.add(p.getId());
        }
        selected.removeAll(ENABLED_BY_MOD);
        ENABLED_BY_MOD.clear();
        saveState();
        repo.setSelected(selected);
        Minecraft.getInstance().delayTextureReload();
        Blockrooms.LOGGER.info("DimensionResourcePack: restored packs in {}", Minecraft.getInstance().level.dimension());
    }



    private static Path stateFile() {
        if (stateFile == null) {
            stateFile = Minecraft.getInstance().gameDirectory.toPath().resolve("blockrooms_dimension_packs.txt");
        }
        return stateFile;
    }

    private static Set<String> loadState() {
        Set<String> ids = new LinkedHashSet<>();
        try {
            Path f = stateFile();
            if (Files.isRegularFile(f)) {
                for (String line : Files.readAllLines(f)) {
                    String s = line.trim();
                    if (!s.isEmpty()) {
                        ids.add(s);
                    }
                }
            }
        } catch (IOException e) {
            Blockrooms.LOGGER.warn("DimensionResourcePack: failed to read state file", e);
        }
        return ids;
    }

    private static void saveState() {
        try {
            Files.write(stateFile(), ENABLED_BY_MOD);
        } catch (IOException e) {
            Blockrooms.LOGGER.warn("DimensionResourcePack: failed to write state file", e);
        }
    }
}
