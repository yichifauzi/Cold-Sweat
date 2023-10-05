package com.momosoftworks.coldsweat.api.temperature.block_temp;

import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.BlockPos;
import com.momosoftworks.coldsweat.util.world.BlockState;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class LavaBlockTemp extends BlockTemp
{
    public LavaBlockTemp()
    {   super(Blocks.lava, Blocks.flowing_lava);
    }

    @Override
    public double getTemperature(World world, EntityLivingBase entity, BlockState state, BlockPos pos, double distance)
    {
        double temp = state.getMeta() > 7 ? 0.25 : CSMath.blend(0.25, 0, state.getMeta(), 0, 7);
        return CSMath.blend(temp, 0, distance, 0.5, 7);
    }

    @Override
    public double maxEffect()
    {   return Temperature.convertUnits(300, Temperature.Units.F, Temperature.Units.MC, false);
    }

    @Override
    public double maxTemperature()
    {   return Temperature.convertUnits(1000, Temperature.Units.F, Temperature.Units.MC, true);
    }
}
