package dev.momostudios.coldsweat.config;

import dev.momostudios.coldsweat.util.compat.CompatManager;
import dev.momostudios.coldsweat.util.serialization.ListBuilder;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class EntitySettingsConfig
{
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> insulatedEntities;
    private static final ForgeConfigSpec.ConfigValue<List<?>> llamaFurGrowth;
    private static final EntitySettingsConfig INSTANCE = new EntitySettingsConfig();
    private static ForgeConfigSpec.ConfigValue<List<? extends List<?>>> chameleonBiomes;
    private static ForgeConfigSpec.ConfigValue<List<? extends List<?>>> llamaBiomes;

    static
    {
        /*
         Insulated Entities
         */
        BUILDER.push("Entity Settings");
        insulatedEntities = BUILDER
                .comment("List of entities that will insulate the player when riding them",
                         "The rate at which the player's temperature changes is divided by the resistance value",
                         "Format: [\"entity_id\", coldResistance, hotResistance]")
                .defineListAllowEmpty(Collections.singletonList("Insulated Mounts"), () -> Arrays.asList(
                ),
                it ->
                {
                    if (it instanceof List<?>)
                    {   List<?> list = ((List<?>) it);
                        return list.size() == 3 && list.get(0) instanceof String && list.get(1) instanceof Number && list.get(2) instanceof Number;
                    }
                    return false;
                });

        llamaFurGrowth = BUILDER
                .comment("Defines how often a llama will try to grow its fur, the growth cooldown after shearing, and the chance of it succeeding",
                        "Format: [ticks, cooldown, chance]")
                .defineList("Goat Fur Growth Timings", Arrays.asList(
                        1200, 2400, 0.20
                ),
                it -> it instanceof Number);

        BUILDER.pop();

        BUILDER.push("Mob Spawning");
        chameleonBiomes = BUILDER
                .comment("Defines the biomes that Chameleons can spawn in",
                         "Format: [[\"biome_id\", weight], [\"biome_id\", weight], etc...]")
                .defineList("Chameleon Spawn Biomes", ListBuilder.begin(
                                Arrays.asList("minecraft:bamboo_jungle", 80),
                                Arrays.asList("minecraft:jungle", 80),
                                Arrays.asList("minecraft:sparse_jungle", 35),
                                Arrays.asList("minecraft:desert", 1))
                            .addIf(CompatManager.isBiomesOPlentyLoaded(),
                                () -> Arrays.asList("biomesoplenty:lush_desert", 3),
                                () -> Arrays.asList("biomesoplenty:rainforest", 40),
                                () -> Arrays.asList("biomesoplenty:rocky_rainforest", 15),
                                () -> Arrays.asList("biomesoplenty:fungal_jungle", 10),
                                () -> Arrays.asList("biomesoplenty:tropics", 8),
                                () -> Arrays.asList("biomesoplenty:outback", 2))
                            .addIf(CompatManager.isBiomesYoullGoLoaded(),
                                () -> Arrays.asList("byg:tropical_rainforest", 60),
                                () -> Arrays.asList("byg:jacaranda_forest", 3),
                                () -> Arrays.asList("byg:guiana_shield", 3),
                                () -> Arrays.asList("byg:crag_gardens", 4),
                                () -> Arrays.asList("byg:atacama_desert", 1),
                                () -> Arrays.asList("byg:cypress_swamplands", 3),
                                () -> Arrays.asList("byg:mojave_desert", 1),
                                () -> Arrays.asList("byg:windswept_desert", 2))
                            .addIf(CompatManager.isAtmosphericLoaded(),
                                () -> Arrays.asList("atmospheric:dunes", 0.75),
                                () -> Arrays.asList("atmospheric:flourishing_dunes", 1.5),
                                () -> Arrays.asList("atmospheric:rocky_dunes", 0.75),
                                () -> Arrays.asList("atmospheric:petrified_dunes", 0.5),
                                () -> Arrays.asList("atmospheric:rainforest", 70),
                                () -> Arrays.asList("atmospheric:sparse_rainforest", 40),
                                () -> Arrays.asList("atmospheric:rainforest_basin", 50),
                                () -> Arrays.asList("atmospheric:sparse_rainforest_basin", 30))
                           .addIf(CompatManager.isTerralithLoaded(),
                                () -> Arrays.asList("terralith:red_oasis", 3),
                                () -> Arrays.asList("terralith:desert_oasis", 3),
                                () -> Arrays.asList("terralith:tropical_jungle", 80),
                                () -> Arrays.asList("terralith:arid_highlands", 1.5),
                                () -> Arrays.asList("terralith:rocky_jungle", 80),
                                () -> Arrays.asList("terralith:brushland", 1.5))
                           .addIf(CompatManager.isWythersLoaded(),
                                () -> Arrays.asList("wythers:cactus_desert", 1),
                                () -> Arrays.asList("wythers:tropical_forest", 10),
                                () -> Arrays.asList("wythers:tropical_rainforest", 80)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });

        llamaBiomes = BUILDER
                .comment("Defines additional biomes that llamas can spawn in",
                         "Format: [[\"biome_id\", weight], [\"biome_id\", weight], etc...]")
                .defineList("Llama Spawn Biomes", ListBuilder.begin(
                                Arrays.asList("minecraft:mountains", 10),
                                Arrays.asList("minecraft:mountains_edge", 5),
                                Arrays.asList("minecraft:wooded_mountains", 12),
                                Arrays.asList("minecraft:snowy_taiga_mountains", 12),
                                Arrays.asList("minecraft:taiga_mountains", 10),
                                Arrays.asList("minecraft:gravelly_mountains", 8))
                            .addIf(CompatManager.isBiomesOPlentyLoaded(),
                                () -> Arrays.asList("biomesoplenty:boreal_forest", 5),
                                () -> Arrays.asList("biomesoplenty:jade_cliffs", 4),
                                () -> Arrays.asList("biomesoplenty:crag", 3))
                            .addIf(CompatManager.isBiomesYoullGoLoaded(),
                                () -> Arrays.asList("byg:alps", 16),
                                () -> Arrays.asList("byg:canadian_shield", 3),
                                () -> Arrays.asList("byg:guiana_shield", 3),
                                () -> Arrays.asList("byg:fragment_forest", 128),
                                () -> Arrays.asList("byg:howling_peaks", 6),
                                () -> Arrays.asList("byg:shattered_glacier", 6),
                                () -> Arrays.asList("byg:dacite_ridges", 5))
                            .addIf(CompatManager.isTerralithLoaded(),
                                () -> Arrays.asList("terralith:blooming_plateau", 5),
                                () -> Arrays.asList("terralith:rocky_mountains", 6),
                                () -> Arrays.asList("terralith:alpine_grove", 6),
                                () -> Arrays.asList("terralith:scarlet_mountains", 4),
                                () -> Arrays.asList("terralith:windswept_spires", 16),
                                () -> Arrays.asList("terralith:cloud_forest", 4),
                                () -> Arrays.asList("terralith:haze_mountain", 4)
                        ).build(),
                        it ->
                        {
                            if (it instanceof List<?>)
                            {   List<?> list = ((List<?>) it);
                                return list.size() == 2 && list.get(0) instanceof String && list.get(1) instanceof Number;
                            }
                            return false;
                        });
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "coldsweat");

        // Create the config folder
        try
        {   Files.createDirectory(csConfigPath);
        } catch (Exception ignored) {}

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "coldsweat/entity_settings.toml");
    }

    public static EntitySettingsConfig getInstance()
    {   return INSTANCE;
    }

    /*
     * Non-private values for use elsewhere
     */

    public List<? extends List<?>> getInsulatedEntities()
    {   return insulatedEntities.get();
    }

    public List<?> getGoatFurStats()
    {   return llamaFurGrowth.get();
    }
    public void setGoatFurStats(List<? extends Number> list)
    {   llamaFurGrowth.set(list);
    }

    public List<? extends List<?>> getChameleonSpawnBiomes()
    {   return chameleonBiomes.get();
    }

    public List<? extends List<?>> getGoatSpawnBiomes()
    {   return llamaBiomes.get();
    }
}
