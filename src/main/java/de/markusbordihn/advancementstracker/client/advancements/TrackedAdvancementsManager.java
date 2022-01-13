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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.widgets.TrackerWidget;
import de.markusbordihn.advancementstracker.config.ClientConfig;

@EventBusSubscriber(Dist.CLIENT)
public class TrackedAdvancementsManager {

  public static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static Set<AdvancementEntry> trackedAdvancements = new HashSet<>();
  private static int maxNumberOfTrackedAdvancements =
      ClientConfig.CLIENT.maxNumberOfTrackedAdvancements.get();
  private static List<String> trackedAdvancementsDefault =
      ClientConfig.CLIENT.trackedAdvancements.get();
  private static List<String> trackedAdvancementsLocal =
      ClientConfig.CLIENT.trackedAdvancementsLocal.get();
  private static List<String> trackedAdvancementsRemote =
      ClientConfig.CLIENT.trackedAdvancementsRemote.get();
  private static String serverId;

  protected TrackedAdvancementsManager() {}

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    if (!event.getWorld().isClientSide()) {
      return;
    }
    Minecraft minecraft = Minecraft.getInstance();
    ServerData serverData = minecraft != null ? minecraft.getCurrentServer() : null;
    if (serverData != null) {
      serverId = String.format("%s:%s::", serverData.name.replaceAll("[^a-zA-Z0-9_]", "_"),
          serverData.protocol);
    } else {
      serverId = null;
    }
    trackedAdvancements = new HashSet<>();
    maxNumberOfTrackedAdvancements = ClientConfig.CLIENT.maxNumberOfTrackedAdvancements.get();
    log.info("Loading tracked advancements with max. {} ...", maxNumberOfTrackedAdvancements);

    trackedAdvancementsDefault = ClientConfig.CLIENT.trackedAdvancements.get();
    if (!trackedAdvancementsDefault.isEmpty()) {
      log.info("Loading default tracked advancements {} ...", trackedAdvancementsDefault);
    }
    trackedAdvancementsLocal = ClientConfig.CLIENT.trackedAdvancementsLocal.get();
    if (!trackedAdvancementsLocal.isEmpty()) {
      log.info("Loading local tracked advancements {} ...", trackedAdvancementsLocal);
    }
    trackedAdvancementsRemote = ClientConfig.CLIENT.trackedAdvancementsRemote.get();
    if (!trackedAdvancementsRemote.isEmpty()) {
      log.info("Loading remote tracked advancements {} ...", trackedAdvancementsRemote);
    }
    updateTrackerWidget();
  }

  public static void checkForTrackedAdvancement(AdvancementEntry advancement) {
    if (advancement.isDone || trackedAdvancements.size() >= maxNumberOfTrackedAdvancements) {
      return;
    }
    AdvancementEntry trackedAdvancement = null;

    // Check first for default tracked advancement
    if (!trackedAdvancementsDefault.isEmpty()) {
      for (String trackedAdvancementDefault : trackedAdvancementsDefault) {
        if (advancement.idString.equals(trackedAdvancementDefault)) {
          log.debug("Adding default tracked advancement {}", advancement);
          trackedAdvancement = advancement;
          break;
        }
      }
    }

    // Check for remote tracked advancement
    if (!trackedAdvancementsRemote.isEmpty() && serverId != null) {
      for (String cachedAdvancementEntry : trackedAdvancementsRemote) {
        if (!cachedAdvancementEntry.isEmpty() && !"".equals(cachedAdvancementEntry)
            && cachedAdvancementEntry.startsWith(serverId)
            && advancement.idString.equals(cachedAdvancementEntry.split("::", 2)[1])) {
          log.debug("Adding remote tracked advancement {}", advancement);
          trackedAdvancement = advancement;
          break;
        }
      }
    }

    // Check for local tracked advancement
    if (!trackedAdvancementsLocal.isEmpty() && serverId == null) {
      for (String cachedAdvancementEntry : trackedAdvancementsLocal) {
        if (advancement.idString.equals(cachedAdvancementEntry)) {
          log.debug("Adding local tracked advancement {}", advancement);
          trackedAdvancement = advancement;
          break;
        }
      }
    }

    if (trackedAdvancement != null) {
      trackAdvancement(trackedAdvancement, false);
    }

  }

  public static void trackAdvancement(AdvancementEntry advancement) {
    trackAdvancement(advancement, true);
  }

  public static void trackAdvancement(AdvancementEntry advancement, boolean autosave) {
    if (advancement.isDone) {
      log.warn("Advancement {} is already done, no need to track it.", advancement);
      return;
    }
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      if (trackedAdvancementEntry.id == advancement.id) {
        log.warn("Advancement {} is already tracked.", advancement);
        return;
      }
    }
    if (trackedAdvancements.size() >= maxNumberOfTrackedAdvancements) {
      log.error("Number of tracked advancements {} exceeds the limit of {}",
          trackedAdvancements.size(), advancement);
    }
    log.info("Track Advancement {}", advancement);
    trackedAdvancements.add(advancement);
    if (autosave) {
      saveTrackedAdvancements();
    }
    updateTrackerWidget();
  }

  private static void saveTrackedAdvancements() {
    saveTrackedAdvancementsRemote();
    saveTrackedAdvancementsLocal();
  }

  private static void saveTrackedAdvancementsRemote() {
    if (serverId == null) {
      return;
    }
    List<String> trackedAdvancementsToSave = new ArrayList<>();
    // Adding existing entries, but ignore entries for current server.
    for (String trackedAdvancementRemote : trackedAdvancementsRemote) {
      if (!trackedAdvancementRemote.isEmpty() && !"".equals(trackedAdvancementRemote)
          && !trackedAdvancementRemote.startsWith(serverId)) {
        trackedAdvancementsToSave.add(trackedAdvancementRemote);
      }
    }
    // Adding entries for current server.
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      trackedAdvancementsToSave.add(serverId + trackedAdvancementEntry.id.toString());
    }
    ClientConfig.CLIENT.trackedAdvancementsRemote
        .set(trackedAdvancementsToSave.stream().distinct().collect(Collectors.toList()));
    trackedAdvancementsRemote = ClientConfig.CLIENT.trackedAdvancementsRemote.get();
    ClientConfig.CLIENT.trackedAdvancements.save();
  }

  private static void saveTrackedAdvancementsLocal() {
    if (serverId != null) {
      return;
    }
    List<String> trackedAdvancementsToSave = new ArrayList<>();
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      trackedAdvancementsToSave.add(trackedAdvancementEntry.id.toString());
    }
    ClientConfig.CLIENT.trackedAdvancementsLocal
        .set(trackedAdvancementsToSave.stream().distinct().collect(Collectors.toList()));
    trackedAdvancementsLocal = ClientConfig.CLIENT.trackedAdvancementsLocal.get();
    ClientConfig.CLIENT.trackedAdvancements.save();
  }

  public static void untrackAdvancement(Advancement advancement) {
    untrackAdvancement(advancement.getId());
  }

  public static void untrackAdvancement(AdvancementEntry advancement) {
    untrackAdvancement(advancement.getId());
  }

  public static void untrackAdvancement(ResourceLocation advancementId) {
    AdvancementEntry existingAdvancementEntry = null;
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      if (trackedAdvancementEntry.id == advancementId) {
        existingAdvancementEntry = trackedAdvancementEntry;
        break;
      }
    }
    if (existingAdvancementEntry != null) {
      trackedAdvancements.remove(existingAdvancementEntry);
      saveTrackedAdvancements();
      updateTrackerWidget();
    }
  }

  public static int numOfTrackedAdvancements() {
    return trackedAdvancements.size();
  }

  public static boolean hasReachedTrackedAdvancementLimit() {
    return trackedAdvancements.size() >= maxNumberOfTrackedAdvancements;
  }

  public static boolean isTrackedAdvancement(Advancement advancement) {
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      if (trackedAdvancementEntry.id == advancement.getId()) {
        return true;
      }
    }
    return false;
  }

  private static void updateTrackerWidget() {
    TrackerWidget.setTrackedAdvancements(trackedAdvancements);
  }

}
