package com.momosoftworks.coldsweat.mixin;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.client.event.TooltipHandler;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(ItemStack.class)
public class MixinItemTooltip
{
    ItemStack stack = (ItemStack) (Object) this;

    @Inject(method = "getTooltipLines", at = @At(value = "FIELD", target = "Lnet/minecraft/world/item/ItemStack$TooltipPart;MODIFIERS:Lnet/minecraft/world/item/ItemStack$TooltipPart;", shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectBeforeAttributes(Player player, TooltipFlag advanced, CallbackInfoReturnable<List<Component>> cir,
                                      List<Component> tooltip)
    {
        ItemStack stack = (ItemStack) (Object) this;
        Optional.ofNullable(ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem())).ifPresent(insulator ->
        {
            if (insulator.test(Minecraft.getInstance().player, stack))
            {
                if (!insulator.attributes().getMap().isEmpty())
                {
                    tooltip.add(CommonComponents.EMPTY);
                    tooltip.add(Component.translatable("item.modifiers.insulation").withStyle(ChatFormatting.GRAY));
                    TooltipHandler.addModifierTooltipLines(tooltip, insulator.attributes());
                }
            }
        });
        Optional.ofNullable(ConfigSettings.INSULATING_CURIOS.get().get(stack.getItem())).ifPresent(insulator ->
        {
            if (insulator.test(Minecraft.getInstance().player, stack))
            {
                if (!insulator.attributes().getMap().isEmpty())
                {
                    tooltip.add(CommonComponents.EMPTY);
                    tooltip.add(Component.translatable("item.modifiers.curio").withStyle(ChatFormatting.GRAY));
                    TooltipHandler.addModifierTooltipLines(tooltip, insulator.attributes());
                }
            }
        });
    }

    private static EquipmentSlot CURRENT_SLOT_QUERY = null;

    @Inject(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getAttributeModifiers(Lnet/minecraft/world/entity/EquipmentSlot;)Lcom/google/common/collect/Multimap;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void setCurrentSlot(Player player, TooltipFlag advanced, CallbackInfoReturnable<List<MutableComponent>> cir,
                                List<MutableComponent> tooltip, MutableComponent name, int hideFlags, EquipmentSlot[] allSlots, int var7, int var8, EquipmentSlot slot)
    {
        CURRENT_SLOT_QUERY = slot;
    }

    @ModifyVariable(method = "getTooltipLines", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private Multimap<Attribute, AttributeModifier> modifyAttributeModifiers(Multimap<Attribute, AttributeModifier> original, Player player, TooltipFlag advanced)
    {
        Multimap<Attribute, AttributeModifier> modifiers = MultimapBuilder.linkedHashKeys().arrayListValues().build(original);
        if (player != null && stack.equals(player.getItemBySlot(CURRENT_SLOT_QUERY)))
        {
            Optional.ofNullable(ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem())).ifPresent(insulator ->
            {
                if (insulator.test(player, stack))
                {   modifiers.putAll(insulator.attributes().getMap());
                }
            });
            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                cap.getInsulation().stream().map(Pair::getFirst).forEach(item ->
                {
                    Optional.ofNullable(ConfigSettings.INSULATION_ITEMS.get().get(item.getItem())).ifPresent(insulator ->
                    {
                        if (insulator.test(player, item))
                        {   modifiers.putAll(insulator.attributes().getMap());
                        }
                    });
                });
            });
        }
        return modifiers;
    }

    private static List<Component> TOOLTIP = null;
    private static Map.Entry<Attribute, AttributeModifier> ENTRY = null;
    private static AttributeModifier MODIFIER = null;

    @Inject(method = "getTooltipLines",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
            slice = @Slice
                    (
                            from = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 7),
                            to = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 9)
                    ),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void setupCustomAttributeDisplay(Player pPlayer, TooltipFlag pIsAdvanced, CallbackInfoReturnable<List<Component>> cir,
                                             // Locals
                                             List<Component> tooltip, MutableComponent name, int hideFlags, EquipmentSlot[] var6, int var7, int var8,
                                             EquipmentSlot equipmentslot, Multimap<Attribute, AttributeModifier> attributeMap, Iterator<AttributeModifier> entryIterator,
                                             Map.Entry<Attribute, AttributeModifier> entry, AttributeModifier modifier, double d0, boolean flag, double d1)
    {
        TOOLTIP = tooltip;
        ENTRY = entry;
        MODIFIER = modifier;
    }

    @ModifyArg(method = "getTooltipLines",
               at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"),
               slice = @Slice
               (
                   from = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 7),
                   to = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 9)
               ))
    private <E> E customAttributeFormatting(E obj)
    {
        if (obj instanceof MutableComponent component)
        {
            List<Component> siblings = component.getSiblings();
            if (TOOLTIP != null && ENTRY != null && MODIFIER != null
            && EntityTempManager.isTemperatureAttribute(ENTRY.getKey()))
            {
                MutableComponent newline = TooltipHandler.getFormattedAttributeModifier(ENTRY.getKey(), MODIFIER.getAmount(), MODIFIER.getOperation());
                for (Component sibling : siblings)
                {   newline = newline.append(sibling);
                }
                return (E) newline;
            }
        }
        return obj;
    }
}
