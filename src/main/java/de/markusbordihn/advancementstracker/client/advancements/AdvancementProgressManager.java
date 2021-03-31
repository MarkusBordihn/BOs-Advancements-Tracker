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

package de.markusbordihn.advancementstracker.client.advancements;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancementManager.IListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;

@EventBusSubscriber(Dist.CLIENT)
public class AdvancementProgressManager implements IListener {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static Timer rateControlTimer;
  private static boolean hasAdvancementProgressListener = false;
  private static boolean init = false;
  private static int backgroundAddListenerCheck = 0;

  protected AdvancementProgressManager() {
  }

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    if (init) {
      return;
    }
    log.info("Try to get advancements progress over event listener...");
    rateControlAddListener();
    init = true;
  }

  public static void rateControlAddListener() {
    if (rateControlTimer != null) {
      rateControlTimer.cancel();
    }
    TimerTask task = new TimerTask() {
      public void run() {
        addListener();
        cancel();
      }
    };
    rateControlTimer = new Timer("Timer");
    rateControlTimer.schedule(task, 250L);
  }

  public static void addListener() {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) {
      if (backgroundAddListenerCheck++ < 250) {
        log.debug("Listener not ready. Will try it later ({})...", backgroundAddListenerCheck);
        rateControlAddListener();
      } else {
        log.error("Unable to add listener after {} tries! Giving up ...", backgroundAddListenerCheck);
      }
      return;
    }
    log.debug("Added advancement progress listener...");
    minecraft.player.connection.getAdvancements().setListener(new AdvancementProgressManager());
    if (!hasAdvancementProgressListener) {
      hasAdvancementProgressListener = true;
    }
  }

  public void onUpdateAdvancementProgress(Advancement advancement, AdvancementProgress advancementProgress) {
    log.debug("Update advancement Progress for {} with {}", advancement, advancementProgress);
    AdvancementsManager.updateAdvancementProgress(advancement, advancementProgress);
  }

  @Override
  public void onAddAdvancementRoot(Advancement advancement) {
    AdvancementsManager.rateControlMapAdvancements();
  }

  @Override
  public void onRemoveAdvancementRoot(Advancement advancement) {
    // Not used.
  }

  @Override
  public void onAddAdvancementTask(Advancement advancement) {
    AdvancementsManager.rateControlMapAdvancements();
  }

  @Override
  public void onRemoveAdvancementTask(Advancement advancement) {
    // Not used.
  }

  @Override
  public void onAdvancementsCleared() {
    // Not used.
  }

  @Override
  public void onSelectedTabChanged(Advancement advancement) {
    // Not used.
  }

  public static boolean hasAdvancementProgressListener() {
    return hasAdvancementProgressListener;
  }

}
