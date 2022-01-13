/**
 * Copyright 2021 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.advancementstracker.client.keybinds;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.TranslationTextComponent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {

  protected KeyBindings() {
  }

  private static final int KEY_L = 76;

  public static final KeyBinding SHOW_OVERVIEW = new KeyBinding(
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.showOverview").getString(), KeyConflictContext.IN_GAME,
      KeyModifier.CONTROL, InputMappings.Type.KEYSYM, KEY_L,
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.category").getString());

  public static final KeyBinding SHOW_GUI = new KeyBinding(
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.showGui").getString(), KeyConflictContext.IN_GAME,
      KeyModifier.ALT, InputMappings.Type.KEYSYM, KEY_L,
      new TranslationTextComponent(Constants.MOD_PREFIX + "keys.category").getString());

  @SubscribeEvent
  public static void setupClient(FMLClientSetupEvent event) {
    ClientRegistry.registerKeyBinding(KeyBindings.SHOW_OVERVIEW);
    ClientRegistry.registerKeyBinding(KeyBindings.SHOW_GUI);
  }
}
