package com.momosoftworks.coldsweat.core.network.message;

import com.momosoftworks.coldsweat.config.MainSettingsConfig;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncConfigSettingsMessage
{
    public static final UUID EMPTY_UUID = new UUID(0, 0);

    Map<String, CompoundNBT> configValues;
    UUID menuOpener;

    public SyncConfigSettingsMessage()
    {
        this(EMPTY_UUID);
    }

    public SyncConfigSettingsMessage(UUID menuOpener)
    {
        this(ConfigSettings.encode(), menuOpener);
    }

    private SyncConfigSettingsMessage(Map<String, CompoundNBT> values, UUID menuOpener)
    {
        this.configValues = values;
        this.menuOpener = menuOpener;
    }

    public static void encode(SyncConfigSettingsMessage message, PacketBuffer buffer)
    {
        buffer.writeUUID(message.menuOpener);
        buffer.writeInt(message.configValues.size());
        for (Map.Entry<String, CompoundNBT> entry : message.configValues.entrySet())
        {
            buffer.writeUtf(entry.getKey());
            buffer.writeNbt(entry.getValue());
        }
    }

    public static SyncConfigSettingsMessage decode(PacketBuffer buffer)
    {   UUID menuOpener = buffer.readUUID();
        int size = buffer.readInt();
        Map<String, CompoundNBT> values = new HashMap<>();

        for (int i = 0; i < size; i++)
        {   values.put(buffer.readUtf(), buffer.readNbt());
        }

        return new SyncConfigSettingsMessage(values, menuOpener);
    }

    public static void handle(SyncConfigSettingsMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            message.configValues.forEach(ConfigSettings::decode);

            if (context.getDirection().getReceptionSide().isServer())
            {
                if (context.getSender() != null && context.getSender().hasPermissions(2))
                {   ConfigSettings.saveValues();
                    MainSettingsConfig.getInstance().save();
                }

                ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncConfigSettingsMessage(EMPTY_UUID));
            }
            else if (context.getDirection().getReceptionSide().isClient())
            {
                if (message.menuOpener.equals(ClientOnlyHelper.getClientPlayer().getUUID()))
                {   ClientOnlyHelper.openConfigScreen();
                }
            }
        });
        context.setPacketHandled(true);
    }
}
