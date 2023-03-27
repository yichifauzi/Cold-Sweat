package dev.momostudios.coldsweat.util.config;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.config.ColdSweatConfig;
import dev.momostudios.coldsweat.config.EntitySettingsConfig;
import dev.momostudios.coldsweat.config.ItemSettingsConfig;
import dev.momostudios.coldsweat.config.WorldSettingsConfig;
import dev.momostudios.coldsweat.util.compat.CompatManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Holds almost all configs for Cold Sweat in memory for easy access.
 * Handles syncing configs between the client/server.
 */
public class ConfigSettings
{
    public static final Map<String, ValueLoader<?>> SYNCED_SETTINGS = new HashMap<>();

    // Settings visible in the config screen
    public static ValueLoader<Integer> DIFFICULTY;
    public static ValueLoader<Double> MAX_TEMP;
    public static ValueLoader<Double> MIN_TEMP;
    public static ValueLoader<Double> TEMP_RATE;
    public static ValueLoader<Boolean> FIRE_RESISTANCE_ENABLED;
    public static ValueLoader<Boolean> ICE_RESISTANCE_ENABLED;
    public static ValueLoader<Boolean> DAMAGE_SCALING;
    public static ValueLoader<Boolean> REQUIRE_THERMOMETER;
    public static ValueLoader<Integer> GRACE_LENGTH;
    public static ValueLoader<Boolean> GRACE_ENABLED;

    // World Settings
    public static ValueLoader<Map<ResourceLocation, Pair<Double, Double>>> BIOME_TEMPS;
    public static ValueLoader<Map<ResourceLocation, Pair<Double, Double>>> BIOME_OFFSETS;
    public static ValueLoader<Map<ResourceLocation, Double>> DIMENSION_TEMPS;
    public static ValueLoader<Map<ResourceLocation, Double>> DIMENSION_OFFSETS;
    public static ValueLoader<Double[]> SUMMER_TEMPS;
    public static ValueLoader<Double[]> AUTUMN_TEMPS;
    public static ValueLoader<Double[]> WINTER_TEMPS;
    public static ValueLoader<Double[]> SPRING_TEMPS;

    // Item settings
    public static ValueLoader<Map<Item, Pair<Double, Double>>> INSULATION_ITEMS;
    public static ValueLoader<Map<Item, Pair<Double, Double>>> INSULATING_ARMORS;

    public static ValueLoader<Map<Item, Double>> TEMPERATURE_FOODS;

    public static ValueLoader<Integer> WATERSKIN_STRENGTH;

    public static ValueLoader<Map<Item, Integer>> LAMP_FUEL_ITEMS;

    public static ValueLoader<List<String>> LAMP_DIMENSIONS;

    public static ValueLoader<Map<Item, Double>> BOILER_FUEL;
    public static ValueLoader<Map<Item, Double>> ICEBOX_FUEL;
    public static ValueLoader<Map<Item, Double>> HEARTH_FUEL;

    public static ValueLoader<Triplet<Integer, Integer, Double>> GOAT_FUR_TIMINGS;

    // Entity Settings
    public static ValueLoader<Map<String, Integer>> CHAMELEON_BIOMES;
    public static ValueLoader<Map<String, Integer>> GOAT_BIOMES;
    public static ValueLoader<Map<Item, Integer>> CHAMELEON_TAME_ITEMS;


    // Makes the settings instantiation collapsible & easier to read
    static
    {
        DIFFICULTY = addSyncedSetting("difficulty", () -> ColdSweatConfig.getInstance().getDifficulty(),
        encoder -> ConfigHelper.writeNBTInt(encoder, "Difficulty"),
        decoder -> decoder.getInt("Difficulty"),
        saver -> ColdSweatConfig.getInstance().setDifficulty(saver));

        MAX_TEMP = addSyncedSetting("max_temp", () -> ColdSweatConfig.getInstance().getMaxTempHabitable(),
        encoder -> ConfigHelper.writeNBTDouble(encoder, "MaxTemp"),
        decoder -> decoder.getDouble("MaxTemp"),
        saver -> ColdSweatConfig.getInstance().setMaxHabitable(saver));

        MIN_TEMP = addSyncedSetting("min_temp", () -> ColdSweatConfig.getInstance().getMinTempHabitable(),
        encoder -> ConfigHelper.writeNBTDouble(encoder, "MinTemp"),
        decoder -> decoder.getDouble("MinTemp"),
        saver -> ColdSweatConfig.getInstance().setMinHabitable(saver));

        TEMP_RATE = addSyncedSetting("temp_rate", () -> ColdSweatConfig.getInstance().getRateMultiplier(),
        encoder -> ConfigHelper.writeNBTDouble(encoder, "TempRate"),
        decoder -> decoder.getDouble("TempRate"),
        saver -> ColdSweatConfig.getInstance().setRateMultiplier(saver));

        FIRE_RESISTANCE_ENABLED = addSyncedSetting("fire_resistance_enabled", () -> ColdSweatConfig.getInstance().isFireResistanceEnabled(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "FireResistanceEnabled"),
        decoder -> decoder.getBoolean("FireResistanceEnabled"),
        saver -> ColdSweatConfig.getInstance().setFireResistanceEnabled(saver));

        ICE_RESISTANCE_ENABLED = addSyncedSetting("ice_resistance_enabled", () -> ColdSweatConfig.getInstance().isIceResistanceEnabled(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "IceResistanceEnabled"),
        decoder -> decoder.getBoolean("IceResistanceEnabled"),
        saver -> ColdSweatConfig.getInstance().setIceResistanceEnabled(saver));

        DAMAGE_SCALING = addSyncedSetting("damage_scaling", () -> ColdSweatConfig.getInstance().doDamageScaling(),
        encoder -> ConfigHelper.writeNBTBoolean( encoder, "DamageScaling"),
        decoder -> decoder.getBoolean("DamageScaling"),
        saver -> ColdSweatConfig.getInstance().setDamageScaling(saver));

        REQUIRE_THERMOMETER = addSyncedSetting("require_thermometer", () -> ColdSweatConfig.getInstance().thermometerRequired(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "RequireThermometer"),
        decoder -> decoder.getBoolean("RequireThermometer"),
        saver -> ColdSweatConfig.getInstance().setRequireThermometer(saver));

        GRACE_LENGTH = addSyncedSetting("grace_length", () -> ColdSweatConfig.getInstance().getGracePeriodLength(),
        encoder -> ConfigHelper.writeNBTInt(encoder, "GraceLength"),
        decoder -> decoder.getInt("GraceLength"),
        saver -> ColdSweatConfig.getInstance().setGracePeriodLength(saver));

        GRACE_ENABLED = addSyncedSetting("grace_enabled", () -> ColdSweatConfig.getInstance().isGracePeriodEnabled(),
        encoder -> ConfigHelper.writeNBTBoolean(encoder, "GraceEnabled"),
        decoder -> decoder.getBoolean("GraceEnabled"),
        saver -> ColdSweatConfig.getInstance().setGracePeriodEnabled(saver));

        BIOME_TEMPS = addSyncedSetting("biome_temps", () -> ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().biomeTemperatures(), true),
        encoder -> ConfigHelper.writeNBTPairMap(encoder, "BiomeTemps"),
        decoder -> ConfigHelper.readNBTPairMap(decoder, "BiomeTemps"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<ResourceLocation, Pair<Double, Double>> entry : saver.entrySet())
            {   list.add(Arrays.asList(entry.getKey().toString(), entry.getValue().getFirst(), entry.getValue().getSecond()));
            }
            WorldSettingsConfig.getInstance().setBiomeTemperatures(list);
        });

        BIOME_OFFSETS = addSyncedSetting("biome_offsets", () -> ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().biomeOffsets(), false),
        encoder -> ConfigHelper.writeNBTPairMap(encoder, "BiomeOffsets"),
        decoder -> ConfigHelper.readNBTPairMap(decoder, "BiomeOffsets"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<ResourceLocation, Pair<Double, Double>> entry : saver.entrySet())
            {   list.add(Arrays.asList(entry.getKey().toString(), entry.getValue().getFirst(), entry.getValue().getSecond()));
            }
            WorldSettingsConfig.getInstance().setBiomeOffsets(list);
        });

        DIMENSION_TEMPS = addSyncedSetting("dimension_temps", () ->
        {
            Map<ResourceLocation, Double> map = new HashMap<>();
            for (List<?> entry : WorldSettingsConfig.getInstance().dimensionTemperatures())
            {   map.put(new ResourceLocation((String) entry.get(0)), ((Number) entry.get(1)).doubleValue());
            }
            return map;
        },
        encoder -> ConfigHelper.writeNBTDoubleMap(encoder, "DimensionTemps"),
        decoder -> ConfigHelper.readNBTDoubleMap(decoder, "DimensionTemps"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<ResourceLocation, Double> entry : saver.entrySet())
            {   list.add(Arrays.asList(entry.getKey().toString(), entry.getValue()));
            }
            WorldSettingsConfig.getInstance().setDimensionTemperatures(list);
        });

        DIMENSION_OFFSETS = addSyncedSetting("dimension_offsets", () ->
        {
            Map<ResourceLocation, Double> map = new HashMap<>();
            for (List<?> entry : WorldSettingsConfig.getInstance().dimensionOffsets())
            {   map.put(new ResourceLocation((String) entry.get(0)), ((Number) entry.get(1)).doubleValue());
            }
            return map;
        },
        encoder -> ConfigHelper.writeNBTDoubleMap(encoder, "DimensionOffsets"),
        decoder -> ConfigHelper.readNBTDoubleMap(decoder, "DimensionOffsets"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<ResourceLocation, Double> entry : saver.entrySet())
            {   list.add(Arrays.asList(entry.getKey().toString(), entry.getValue()));
            }
            WorldSettingsConfig.getInstance().setDimensionOffsets(list);
        });

        BOILER_FUEL = ValueLoader.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().boilerItems()));
        HEARTH_FUEL = ValueLoader.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().hearthItems()));
        ICEBOX_FUEL = ValueLoader.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().iceboxItems()));

        INSULATION_ITEMS = addSyncedSetting("insulation_items", () ->
        {
            Map<Item, Pair<Double, Double>> map = new HashMap<>();
            for (List<?> entry : ItemSettingsConfig.getInstance().insulatingItems())
            {
                String itemID = (String) entry.get(0);
                for (Item item : ConfigHelper.getItems(itemID))
                {   map.put(item, Pair.of(((Number) entry.get(1)).doubleValue(), ((Number) entry.get(2)).doubleValue()));
                }
            }
            return map;
        },
        encoder -> ConfigHelper.writeNBTItemMap(encoder, "InsulationItems"),
        decoder -> ConfigHelper.readNBTItemMap(decoder, "InsulationItems"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<Item, Pair<Double, Double>> entry : saver.entrySet())
            {   list.add(Arrays.asList(ForgeRegistries.ITEMS.getKey(entry.getKey()).toString(), entry.getValue().getFirst(), entry.getValue().getSecond()));
            }
            ItemSettingsConfig.getInstance().setInsulatingItems(list);
        });

        INSULATING_ARMORS = addSyncedSetting("insulating_armors", () ->
        {
            Map<Item, Pair<Double, Double>> map = new HashMap<>();
            for (List<?> entry : ItemSettingsConfig.getInstance().insulatingArmor())
            {
                String itemID = (String) entry.get(0);
                for (Item item : ConfigHelper.getItems(itemID))
                {   map.put(item, Pair.of(((Number) entry.get(1)).doubleValue(), ((Number) entry.get(2)).doubleValue()));
                }
            }
            return map;
        },
        encoder -> ConfigHelper.writeNBTItemMap(encoder, "InsulatingArmors"),
        decoder -> ConfigHelper.readNBTItemMap(decoder, "InsulatingArmors"),
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<Item, Pair<Double, Double>> entry : saver.entrySet())
            {   list.add(Arrays.asList(ForgeRegistries.ITEMS.getKey(entry.getKey()).toString(), entry.getValue().getFirst(), entry.getValue().getSecond()));
            }
            ItemSettingsConfig.getInstance().setInsulatingArmor(list);
        });

        TEMPERATURE_FOODS = ValueLoader.of(() -> ConfigHelper.getItemsWithValues(ItemSettingsConfig.getInstance().temperatureFoods()));

        WATERSKIN_STRENGTH = ValueLoader.of(() -> ItemSettingsConfig.getInstance().waterskinStrength());

        LAMP_FUEL_ITEMS = addSyncedSetting("lamp_fuel_items", () ->
        {
            Map<Item, Integer> list = new HashMap<>();
            for (List<?> item : ItemSettingsConfig.getInstance().soulLampItems())
            {
                ConfigHelper.getItems((String) item.get(0)).forEach(i -> list.put(i, (Integer) item.get(1)));
            }
            return list;
        },
        encoder ->
        {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<Item, Integer> entry : encoder.entrySet())
            {
                tag.putInt(ForgeRegistries.ITEMS.getKey(entry.getKey()).toString(), entry.getValue());
            }
            return tag;
        },
        decoder ->
        {
            Map<Item, Integer> map = new HashMap<>();
            for (String key : decoder.getAllKeys())
            {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(key));
                if (item != null)
                {   map.put(item, decoder.getInt(key));
                }
            }
            return map;
        },
        saver ->
        {
            List<List<?>> list = new ArrayList<>();
            for (Map.Entry<Item, Integer> entry : saver.entrySet())
            {   list.add(Arrays.asList(ForgeRegistries.ITEMS.getKey(entry.getKey()).toString(), entry.getValue()));
            }
            ItemSettingsConfig.getInstance().setSoulLampItems(list);
        });

        LAMP_DIMENSIONS = ValueLoader.of(() -> new ArrayList<>(ItemSettingsConfig.getInstance().soulLampDimensions()));

        GOAT_FUR_TIMINGS = addSyncedSetting("goat_fur_timings", () ->
        {
            List<?> entry = EntitySettingsConfig.getInstance().getGoatFurStats();
            return new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue());
        },
        triplet ->
        {
            CompoundTag tag = new CompoundTag();
            tag.put("Interval", IntTag.valueOf(triplet.getA()));
            tag.put("Cooldown", IntTag.valueOf(triplet.getB()));
            tag.put("Chance", DoubleTag.valueOf(triplet.getC()));
            return tag;
        },
        tag ->
        {
            int interval = tag.getInt("Interval");
            int cooldown = tag.getInt("Cooldown");
            double chance = tag.getDouble("Chance");
            return new Triplet<>(interval, cooldown, chance);
        },
        triplet ->
        {
            List<Number> list = new ArrayList<>();
            list.add(triplet.getA());
            list.add(triplet.getB());
            list.add(triplet.getC());
            EntitySettingsConfig.getInstance().setGoatFurStats(list);
        });

        CHAMELEON_BIOMES = ValueLoader.of(() ->
        {
            Map<String, Integer> map = new HashMap<>();
            for (List<?> entry : EntitySettingsConfig.getInstance().getChameleonSpawnBiomes())
            {
                map.put((String) entry.get(0), ((Number) entry.get(1)).intValue());
            }
            return map;
        });

        GOAT_BIOMES = ValueLoader.of(() ->
        {
            Map<String, Integer> map = new HashMap<>();
            for (List<?> entry : EntitySettingsConfig.getInstance().getGoatSpawnBiomes())
            {
                map.put((String) entry.get(0), ((Number) entry.get(1)).intValue());
            }
            return map;
        });

        CHAMELEON_TAME_ITEMS = ValueLoader.of(() ->
        {
            Map<Item, Integer> map = new HashMap<>();
            for (List<?> entry : EntitySettingsConfig.getInstance().getChameleonTameItems())
            {
                String itemID = (String) entry.get(0);
                for (Item item : ConfigHelper.getItems(itemID))
                {
                    map.put(item, ((Number) entry.get(1)).intValue());
                }
            }
            return map;
        });

        if (CompatManager.isSereneSeasonsLoaded())
        {
            SUMMER_TEMPS = ValueLoader.of(() -> WorldSettingsConfig.getInstance().summerTemps());
            AUTUMN_TEMPS = ValueLoader.of(() -> WorldSettingsConfig.getInstance().autumnTemps());
            WINTER_TEMPS = ValueLoader.of(() -> WorldSettingsConfig.getInstance().winterTemps());
            SPRING_TEMPS = ValueLoader.of(() -> WorldSettingsConfig.getInstance().springTemps());
        }
    }

    public static <T> ValueLoader<T> addSyncedSetting(String id, Supplier<T> supplier, Function<T, CompoundTag> writer, Function<CompoundTag, T> reader, Consumer<T> saver)
    {
        ValueLoader<T> loader = ValueLoader.synced(supplier, writer, reader, saver);
        SYNCED_SETTINGS.put(id, loader);
        return loader;
    }

    public static Map<String, CompoundTag> encode()
    {
        Map<String, CompoundTag> map = new HashMap<>();
        SYNCED_SETTINGS.forEach((key, value) ->
        {
            if (value.isSynced())
                map.put(key, value.encode());
        });
        return map;
    }

    public static void decode(String key, CompoundTag tag)
    {
        SYNCED_SETTINGS.computeIfPresent(key, (k, value) ->
        {
            value.decode(tag);
            return value;
        });
    }

    public static void saveValues()
    {
        SYNCED_SETTINGS.values().forEach(loader ->
        {
            if (loader.isSynced())
                loader.save();
        });
    }
}
