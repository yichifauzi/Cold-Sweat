package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.client.gui.tooltip.ClientSoulspringTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.InsulationTooltip;
import com.momosoftworks.coldsweat.client.gui.tooltip.SoulspringTooltip;
import com.momosoftworks.coldsweat.common.capability.handler.ItemInsulationManager;
import com.momosoftworks.coldsweat.common.item.SoulspringLampItem;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModAttributes;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class TooltipHandler
{
    public static final Style COLD = Style.EMPTY.withColor(3767039);
    public static final Style HOT = Style.EMPTY.withColor(16736574);
    public static final Component EXPAND_TOOLTIP = Component.literal("?").withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withUnderlined(true))
                                           .append(Component.literal(" 'Shift'").withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withUnderlined(false)));

    public static int getTooltipTitleIndex(List<Either<FormattedText, TooltipComponent>> tooltip, ItemStack stack)
    {
        if (tooltip.isEmpty()) return 0;

        int tooltipStartIndex;
        String hoverName = stack.getHoverName().getString();

        if (CompatManager.isIcebergLoaded())
        {   tooltipStartIndex = CompatManager.getLegendaryTTStartIndex(tooltip) + 1;
        }
        else for (tooltipStartIndex = 0; tooltipStartIndex < tooltip.size(); tooltipStartIndex++)
        {
            if (tooltip.get(tooltipStartIndex).left().map(FormattedText::getString).map(String::strip).orElse("").equals(hoverName))
            {   tooltipStartIndex++;
                break;
            }
        }
        tooltipStartIndex = CSMath.clamp(tooltipStartIndex, 0, tooltip.size());
        return tooltipStartIndex;
    }

    public static int getTooltipEndIndex(List<Either<FormattedText, TooltipComponent>> tooltip, ItemStack stack)
    {
        int tooltipEndIndex = tooltip.size();
        if (Minecraft.getInstance().options.advancedItemTooltips)
        {
            for (--tooltipEndIndex; tooltipEndIndex > 0; tooltipEndIndex--)
            {
                if (tooltip.get(tooltipEndIndex).left().map(text -> text.getString().equals(ForgeRegistries.ITEMS.getKey(stack.getItem()).toString())).orElse(false))
                {   break;
                }
            }
        }
        tooltipEndIndex = CSMath.clamp(tooltipEndIndex, 0, tooltip.size());
        return tooltipEndIndex;
    }

    public static void addModifierTooltipLines(List<Component> tooltip, AttributeModifierMap map)
    {
        map.getMap().asMap().forEach((attribute, modifiers) ->
        {
            for (AttributeModifier.Operation operation : AttributeModifier.Operation.values())
            {
                double value = 0;
                for (AttributeModifier modifier : modifiers.stream().filter(mod -> mod.getOperation() == operation).toList())
                {   value += modifier.getAmount();
                }
                if (value != 0)
                {   tooltip.add(getFormattedAttributeModifier(attribute, value, operation));
                }
            }
        });
    }

    public static MutableComponent getFormattedAttributeModifier(Attribute attribute, double amount, AttributeModifier.Operation operation)
    {
        if (attribute == null) return Component.empty();
        double value = amount;
        String attributeName = attribute.getDescriptionId().replace("attribute.", "");

        if (operation == AttributeModifier.Operation.ADDITION
        && (attribute == ModAttributes.FREEZING_POINT
        || attribute == ModAttributes.BURNING_POINT
        || attribute == ModAttributes.WORLD_TEMPERATURE
        || attribute == ModAttributes.BASE_BODY_TEMPERATURE))
        {
            value = Temperature.convert(value, Temperature.Units.MC, ConfigSettings.CELSIUS.get() ? Temperature.Units.C : Temperature.Units.F, false);
        }
        String operationString = operation == AttributeModifier.Operation.ADDITION ? "add" : "multiply";
        ChatFormatting color;
        String sign;
        if (value >= 0)
        {
            color = ChatFormatting.BLUE;
            sign = "+";
        }
        else
        {   color = ChatFormatting.RED;
            sign = "";
        }
        String percent;
        if (operation != AttributeModifier.Operation.ADDITION
        || attribute == ModAttributes.HEAT_RESISTANCE
        || attribute == ModAttributes.COLD_RESISTANCE
        || attribute == ModAttributes.HEAT_DAMPENING
        || attribute == ModAttributes.COLD_DAMPENING)
        {   percent = "%";
            value *= 100;
        }
        else
        {   percent = "";
        }
        return Component.translatable(String.format("attribute.cold_sweat.modifier.%s.%s", operationString, attributeName),
                                      sign + CSMath.formatDoubleOrInt(CSMath.round(value, 2))
                                           + percent)
                        .withStyle(color);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addCustomTooltips(RenderTooltipEvent.GatherComponents event)
    {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        var elements = event.getTooltipElements();
        boolean hideTooltips = ConfigSettings.HIDE_TOOLTIPS.get() && !Screen.hasShiftDown();
        if (stack.isEmpty()) return;

        // Get the index at which the tooltip should be inserted
        int tooltipStartIndex = getTooltipTitleIndex(elements, stack);
        // Get the index of the end of the tooltip, before the debug info (if enabled)
        int tooltipEndIndex = getTooltipEndIndex(elements, stack);

        Insulator itemInsul = null;
        Player player = Minecraft.getInstance().player;

        // If the item is a Soulspring Lamp
        if (stack.getItem() instanceof SoulspringLampItem)
        {   if (!Screen.hasShiftDown())
            {   elements.add(tooltipStartIndex, Either.left(EXPAND_TOOLTIP));
            }
            elements.add(tooltipStartIndex, Either.right(new SoulspringTooltip(stack.getOrCreateTag().getDouble("Fuel"))));
        }
        // If the item is edible
        else if (stack.getUseAnimation() == UseAnim.DRINK || stack.getUseAnimation() == UseAnim.EAT)
        {
            PredicateItem temp = ConfigSettings.FOOD_TEMPERATURES.get().get(stack.getItem());
            if (temp != null && temp.test(player, stack))
            {
                MutableComponent consumeEffects = temp.value() > 0
                                                  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp.value())).withStyle(HOT) :
                                                  temp.value() == 0
                                                  ? Component.translatable("tooltip.cold_sweat.temperature_effect", "+" + CSMath.formatDoubleOrInt(temp.value())) :
                                                  Component.translatable("tooltip.cold_sweat.temperature_effect", CSMath.formatDoubleOrInt(temp.value())).withStyle(COLD);
                // Add a duration to the tooltip if it exists
                if (temp.extraData().contains("duration", Tag.TAG_INT))
                {   consumeEffects.append(" (" + StringUtil.formatTickDuration(temp.extraData().getInt("duration")) + ")");
                }
                // Check if Diet has their own tooltip already
                int dietTooltipSectionIndex = CSMath.getIndexOf(elements, line -> line.left().map(text -> text.getString().equalsIgnoreCase(Component.translatable("tooltip.diet.eaten").getString())).orElse(false));
                int index = dietTooltipSectionIndex != -1
                            ? dietTooltipSectionIndex + 1
                            : tooltipEndIndex;
                // If the section already exists, add it under that section
                elements.add(index, Either.left(consumeEffects));
                // Don't add our own section title if one already exists
                if (dietTooltipSectionIndex == -1)
                {
                    elements.add(tooltipEndIndex, Either.left(Component.translatable("tooltip.cold_sweat.consumed").withStyle(ChatFormatting.GRAY)));
                    elements.add(tooltipEndIndex, Either.left(Component.empty()));
                }
            }
        }
        // If the item is an insulation ingredient, add the tooltip
        else if (!hideTooltips && (itemInsul = ConfigSettings.INSULATION_ITEMS.get().get(item)) != null && !itemInsul.insulation().isEmpty())
        {
            if (itemInsul.test(player, stack))
            {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(itemInsul.insulation().split(), Insulation.Slot.ITEM, stack)));
            }
        }
        // If the item is an insulating curio, add the tooltip
        else if (!hideTooltips && CompatManager.isCuriosLoaded() && (itemInsul = ConfigSettings.INSULATING_CURIOS.get().get(item)) != null && !itemInsul.insulation().isEmpty())
        {
            if (itemInsul.test(player, stack))
            {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(itemInsul.insulation().split(), Insulation.Slot.CURIO, stack)));
            }
        }
        // If the item is insulated armor
        Insulator armorInsulator = ConfigSettings.INSULATING_ARMORS.get().get(item);
        if (!hideTooltips && stack.getItem() instanceof Equipable && (!Objects.equals(armorInsulator, itemInsul) || armorInsulator == null))
        {
            List<Insulation> insulation = new ArrayList<>();

            ItemInsulationManager.getInsulationCap(stack).ifPresent(cap ->
            {
                cap.deserializeNBT(stack.getOrCreateTag());

                // Create the list of insulation pairs from NBT
                insulation.addAll(cap.getInsulation().stream()
                                  // Filter out insulation that doesn't match the predicate
                                  .filter(pair ->
                                  {
                                      ItemStack stack1 = pair.getFirst();
                                      return CSMath.getIfNotNull(ConfigSettings.INSULATION_ITEMS.get().get(stack1.getItem()),
                                                                 insulator -> insulator.test(player, stack),
                                                                 true);
                                  })
                                  .map(Pair::getSecond).flatMap(List::stream).toList());
            });

            // If the armor has intrinsic insulation due to configs, add it to the list
            if (armorInsulator != null)
            {
                if (armorInsulator.test(player, stack))
                {   insulation.addAll(armorInsulator.insulation().split());
                }
            }

            if (!insulation.isEmpty())
            {   elements.add(tooltipStartIndex, Either.right(new InsulationTooltip(insulation, Insulation.Slot.ARMOR, stack)));
            }
        }
    }

    static int FUEL_FADE_TIMER = 0;

    @SubscribeEvent
    public static void renderSoulLampInsertTooltip(ScreenEvent.Render.Post event)
    {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen)
        {
            if (screen.getSlotUnderMouse() != null && screen.getSlotUnderMouse().getItem().getItem() == ModItems.SOULSPRING_LAMP)
            {
                double fuel = screen.getSlotUnderMouse().getItem().getOrCreateTag().getDouble("Fuel");
                ItemStack carriedStack = screen.getMenu().getCarried();

                PredicateItem itemFuel;
                if (!carriedStack.isEmpty()
                && (itemFuel = ConfigSettings.SOULSPRING_LAMP_FUEL.get().get(carriedStack.getItem())) != null
                && itemFuel.test(carriedStack))
                {
                    double fuelValue = screen.getMenu().getCarried().getCount() * itemFuel.value();
                    int slotX = screen.getSlotUnderMouse().x + screen.getGuiLeft();
                    int slotY = screen.getSlotUnderMouse().y + screen.getGuiTop();

                    GuiGraphics graphics = event.getGuiGraphics();
                    PoseStack ps = graphics.pose();
                    if (event.getMouseY() < slotY + 8)
                    {   ps.translate(0, 32, 0);
                    }

                    graphics.renderTooltip(Minecraft.getInstance().font, List.of(Component.literal("       ")), Optional.empty(), slotX - 18, slotY + 1);

                    RenderSystem.defaultBlendFunc();

                    // Render background
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 0, 30, 8, 30, 34);

                    // Render ghost overlay
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1f, 1f, 1f, 0.15f + (float) ((Math.sin(FUEL_FADE_TIMER / 5f) + 1f) / 2f) * 0.4f);
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 8, Math.min(30, (int) ((fuel + fuelValue) / 2.1333f)), 8, 30, 34);
                    RenderSystem.disableBlend();

                    // Render fuel
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1f);
                    graphics.blit(ClientSoulspringTooltip.TOOLTIP_LOCATION.get(), slotX - 7, slotY - 11, 401, 0, 16, (int) (fuel / 2.1333f), 8, 30, 34);
                }
            }
        }
    }

    @SubscribeEvent
    public static void tickSoulLampInsertTooltip(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {   FUEL_FADE_TIMER++;
        }
    }
}
