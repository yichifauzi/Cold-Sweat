package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record EffectsRequirement(Map<MobEffect, Instance> effects)
{
    public static final Codec<EffectsRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ConfigHelper.tagOrRegistryObjectCodec(Registries.MOB_EFFECT, ForgeRegistries.MOB_EFFECTS), Instance.CODEC)
            .xmap(map ->
                  {
                        Map<MobEffect, Instance> effects = new HashMap<>();
                        for (Map.Entry<Either<TagKey<MobEffect>, MobEffect>, Instance> entry : map.entrySet())
                        {
                            entry.getKey().map(
                            tag ->
                            {   ForgeRegistries.MOB_EFFECTS.tags().getTag(tag).stream().forEach(effect -> effects.put(effect, entry.getValue()));
                                return null;
                            },
                            effect -> effects.put(effect, entry.getValue()));
                        }
                        return effects;
                  },
                  effects ->
                  {
                      Map<Either<TagKey<MobEffect>, MobEffect>, Instance> map = new HashMap<>();
                      for (Map.Entry<MobEffect, Instance> entry : effects.entrySet())
                      {
                          map.put(Either.right(entry.getKey()), entry.getValue());
                      }
                      return map;
                  }).fieldOf("effects").forGetter(predicate -> predicate.effects)
    ).apply(instance, EffectsRequirement::new));

    public boolean test(Entity entity)
    {   return !(entity instanceof LivingEntity living) || test(living.getActiveEffects());
    }

    public boolean test(Collection<MobEffectInstance> effects)
    {
        for (Map.Entry<MobEffect, Instance> entry : this.effects.entrySet())
        {
            MobEffect effect = entry.getKey();
            Instance instance = entry.getValue();
            int amplifier = 0;
            int duration = 0;
            boolean ambient = true;
            boolean visible = true;
            for (MobEffectInstance effectInstance : effects)
            {
                if (effectInstance.getEffect() == effect)
                {
                    amplifier = effectInstance.getAmplifier();
                    duration = effectInstance.getDuration();
                    ambient = effectInstance.isAmbient();
                    visible = effectInstance.isVisible();
                    break;
                }
            }
            if (!instance.amplifier().test(amplifier) || !instance.duration().test(duration))
            {   return false;
            }
            if (instance.ambient().isPresent() && instance.ambient().get() != ambient)
            {   return false;
            }
            if (instance.visible().isPresent() && instance.visible().get() != visible)
            {   return false;
            }
        }
        return true;
    }

    public boolean test(MobEffectInstance effect)
    {
        Instance instance = effects.get(effect.getEffect());
        if (instance == null)
        {   return true;
        }

        return instance.amplifier().test(effect.getAmplifier()) && instance.duration().test(effect.getDuration())
            && (instance.ambient().isEmpty() || instance.ambient().get() == effect.isAmbient())
            && (instance.visible().isEmpty() || instance.visible().get() == effect.isVisible());
    }

    public CompoundTag serialize()
    {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<MobEffect, Instance> entry : effects.entrySet())
        {
            tag.put(ForgeRegistries.MOB_EFFECTS.getKey(entry.getKey()).toString(), entry.getValue().serialize());
        }
        return tag;
    }

    public static EffectsRequirement deserialize(CompoundTag tag)
    {
        Map<MobEffect, Instance> effects = tag.getAllKeys().stream().collect(
                Collectors.toMap(key -> ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(key)),
                                                  key -> Instance.deserialize(tag.getCompound(key))));
        return new EffectsRequirement(effects);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        EffectsRequirement that = (EffectsRequirement) obj;

        return effects.equals(that.effects);
    }

    public record Instance(IntegerBounds amplifier, IntegerBounds duration, Optional<Boolean> ambient, Optional<Boolean> visible)
    {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                IntegerBounds.CODEC.fieldOf("amplifier").forGetter(effect -> effect.amplifier),
                IntegerBounds.CODEC.fieldOf("duration").forGetter(effect -> effect.duration),
                Codec.BOOL.optionalFieldOf("ambient").forGetter(effect -> effect.ambient),
                Codec.BOOL.optionalFieldOf("visible").forGetter(effect -> effect.visible)
        ).apply(instance, Instance::new));

        public CompoundTag serialize()
        {
            CompoundTag tag = new CompoundTag();
            tag.put("amplifier", amplifier.serialize());
            tag.put("duration", duration.serialize());
            ambient.ifPresent(value -> tag.putBoolean("ambient", value));
            visible.ifPresent(value -> tag.putBoolean("visible", value));
            return tag;
        }

        public static Instance deserialize(CompoundTag tag)
        {
            IntegerBounds amplifier = IntegerBounds.deserialize(tag.getCompound("amplifier"));
            IntegerBounds duration = IntegerBounds.deserialize(tag.getCompound("duration"));
            Optional<Boolean> ambient = tag.contains("ambient") ? Optional.of(tag.getBoolean("ambient")) : Optional.empty();
            Optional<Boolean> visible = tag.contains("visible") ? Optional.of(tag.getBoolean("visible")) : Optional.empty();
            return new Instance(amplifier, duration, ambient, visible);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {   return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {   return false;
            }

            Instance instance = (Instance) obj;

            if (!amplifier.equals(instance.amplifier))
            {   return false;
            }
            if (!duration.equals(instance.duration))
            {   return false;
            }
            if (!ambient.equals(instance.ambient))
            {   return false;
            }
            return visible.equals(instance.visible);
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append("Instance{amplifier=").append(amplifier);
            builder.append(", duration=").append(duration);
            ambient.ifPresent(value -> builder.append(", ambient=").append(value));
            visible.ifPresent(value -> builder.append(", visible=").append(value));
            builder.append('}');

            return builder.toString();
        }
    }

    @Override
    public String toString()
    {
        return "Effects{" + effects + '}';
    }
}
