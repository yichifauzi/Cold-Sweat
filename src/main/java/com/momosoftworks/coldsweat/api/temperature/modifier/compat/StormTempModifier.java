package com.momosoftworks.coldsweat.api.temperature.modifier.compat;

import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.world.entity.LivingEntity;
import weather2.ServerTickHandler;
import weather2.weathersystem.WeatherManagerServer;
import weather2.weathersystem.storm.WeatherObjectParticleStorm;

import java.util.function.Function;

/**
 * Special TempModifier class for Weather 2
 */
public class StormTempModifier extends TempModifier
{
    public StormTempModifier() {}

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Trait trait)
    {
        if (!entity.level().isClientSide())
        {
            WeatherManagerServer weatherManager = ServerTickHandler.getWeatherManagerFor(entity.level().dimension());
            WeatherObjectParticleStorm snowStorm = weatherManager.getClosestParticleStormByIntensity(entity.position(), WeatherObjectParticleStorm.StormType.SNOWSTORM);
            if (snowStorm != null)
            {
                double distance = snowStorm.pos.distanceTo(entity.position());
                if (distance > snowStorm.getSize())
                    return temp -> temp;
                return temp -> temp - CSMath.blend(1, 0, distance, 100, snowStorm.getSize()) * snowStorm.getIntensity();
            }
        }
        return temp -> temp;
    }
}
