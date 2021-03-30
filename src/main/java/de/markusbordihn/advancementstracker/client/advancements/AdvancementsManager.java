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

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.gui.widgets.TrackerWidget;
import de.markusbordihn.advancementstracker.client.screen.ScreenManager;
import de.markusbordihn.advancementstracker.config.ClientConfig;

@EventBusSubscriber(Dist.CLIENT)
public class AdvancementsManager {

  public static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static AdvancementEntry selectedAdvancement;
  private static AdvancementEntry selectedRootAdvancement;
  private static Map<Advancement, AdvancementProgress> advancementProgressMap = new HashMap<>();
  private static Map<ResourceLocation, Set<AdvancementEntry>> advancementsMap = new HashMap<>();
  private static Set<AdvancementEntry> rootAdvancements = new HashSet<>();
  private static Set<AdvancementEntry> trackedAdvancements = new HashSet<>();
  private static Timer rateControlTimer;
  private static boolean areAdvancementsMapped = false;
  private static boolean hasAdvancements = false;
  private static int backgroundAdvancementCheck = 0;
  private static int maxNumberOfTrackedAdvancements = ClientConfig.CLIENT.maxNumberOfTrackedAdvancements.get();

  protected AdvancementsManager() {
  }

  @SubscribeEvent
  public static void handlePlayerLoggedInEvent(PlayerEvent.PlayerLoggedInEvent event) {
    maxNumberOfTrackedAdvancements = ClientConfig.CLIENT.maxNumberOfTrackedAdvancements.get();
    log.info("Try to pre-map advancements ...");
    advancementProgressMap = new HashMap<>();
    advancementsMap = new HashMap<>();
    rootAdvancements = new HashSet<>();
    trackedAdvancements = new HashSet<>();
    selectedAdvancement = null;
    selectedRootAdvancement = null;
    hasAdvancements = false;
    updateTrackerWidget();
    rateControlMapAdvancements();
  }

  @SubscribeEvent
  public static void handleAdvancementEvent(AdvancementEvent event) {
    Advancement advancement = event.getAdvancement();
    String advancementId = advancement.getId().toString();
    if (!advancementId.startsWith("minecraft:recipes") && !advancementId.startsWith("smallships:recipes")) {
      ScreenManager.saveScreenshot(
          new File(String.format("screenshots/%s", advancementId.split("/")[0].replace(":", "_"))),
          advancementId.split("/")[1], 700L);
      untrackAdvancement(advancement);
      rateControlMapAdvancements();
    }
  }

  public static void rateControlMapAdvancements() {
    if (rateControlTimer != null) {
      rateControlTimer.cancel();
    }
    TimerTask task = new TimerTask() {
      public void run() {
        mapAdvancements();
        cancel();
      }
    };
    rateControlTimer = new Timer("Timer");
    rateControlTimer.schedule(task, 500L);
  }

  public static void mapAdvancements() {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null || minecraft.player == null || minecraft.player.connection == null
        || minecraft.player.connection.getAdvancements().getAdvancements().getAllAdvancements().isEmpty()) {
      if (backgroundAdvancementCheck++ < 100) {
        log.debug("Player Advancements are not ready yet. Will try it later ({})...", backgroundAdvancementCheck);
        rateControlMapAdvancements();
      } else {
        log.error("Unable to get Player Advancements after {} tries! Giving up ...", backgroundAdvancementCheck);
      }
      return;
    }

    // Clear existing Mappings
    advancementsMap = new HashMap<>();
    rootAdvancements = new HashSet<>();

    // Adding possible root Advancements
    Iterable<Advancement> rootAdvancementList = minecraft.player.connection.getAdvancements().getAdvancements()
        .getRoots();
    for (Advancement rootAdvancement : rootAdvancementList) {
      if (rootAdvancement.getDisplay() != null) {
        AdvancementProgress advancementProgress = getAdvancementProgress(rootAdvancement);
        log.debug("[Root Advancement] {}", rootAdvancement);
        rootAdvancements.add(new AdvancementEntry(rootAdvancement, advancementProgress));
      }
    }

    // Prepare advancements and convert them to the AdvancementEntry object for an
    // easy access and less cpu usage.
    Collection<Advancement> advancementList = minecraft.player.connection.getAdvancements().getAdvancements()
        .getAllAdvancements();
    log.debug("Mapping {} advancements ...", advancementList.size());
    for (Advancement advancement : advancementList) {
      // Skip Recipes and placeholder advancements.
      if (advancement.getId().toString().equals("minecraft:recipes/root") || advancement.getDisplay() == null) {
        continue;
      }

      // Handle possible missing root advancement which are shown up as main
      // categories.
      if (advancement.getParent() == null) {
        if (!hasRootAdvancement(advancement)) {
          log.debug("[Missing Root Advancement] {}", advancement);
          rootAdvancements.add(getRootAdvancement(advancement));
        }
      } else {
        // Handle child advancements and find corresponding root advancement.
        Advancement rootAdvancement = advancement.getParent();
        while (rootAdvancement.getParent() != null) {
          rootAdvancement = rootAdvancement.getParent();
        }
        AdvancementProgress advancementProgress = getAdvancementProgress(advancement);
        AdvancementEntry advancementEntry = new AdvancementEntry(advancement, advancementProgress);
        Set<AdvancementEntry> childAdvancements = advancementsMap.get(advancementEntry.rootId);
        if (childAdvancements == null) {
          childAdvancements = new HashSet<>();
          advancementsMap.put(advancementEntry.rootId, childAdvancements);
        }
        childAdvancements.add(advancementEntry);
        log.info("Adding child advancement {}", advancementEntry);
        if (!hasAdvancements) {
          hasAdvancements = true;
        }
      }
    }

    // Adding AdvancementProgress if not already done.
    if (!AdvancementProgressManager.hasAdvancementProgressListener()) {
      AdvancementProgressManager.addListener();
    }

    if (!areAdvancementsMapped) {
      areAdvancementsMapped = true;
    }
  }

  public static boolean hasRootAdvancement(Advancement advancement) {
    for (AdvancementEntry rootAdvancement : rootAdvancements) {
      if (advancement.getId() == rootAdvancement.id) {
        return true;
      }
    }
    return false;
  }

  public static AdvancementEntry getRootAdvancement(Advancement advancement) {
    for (AdvancementEntry rootAdvancement : rootAdvancements) {
      if (advancement.getId() == rootAdvancement.id) {
        return rootAdvancement;
      }
    }
    AdvancementProgress advancementProgress = getAdvancementProgress(advancement);
    return new AdvancementEntry(advancement, advancementProgress);
  }

  public static Set<AdvancementEntry> getRootAdvancements() {
    if (!areAdvancementsMapped) {
      mapAdvancements();
    }
    return rootAdvancements;
  }

  public static Set<AdvancementEntry> getSortedRootAdvancements(Comparator<AdvancementEntry> comparator) {
    Set<AdvancementEntry> advancements = getRootAdvancements();
    if (advancements == null) {
      return new HashSet<>();
    }
    return advancements.stream().sorted(comparator).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static Set<AdvancementEntry> getRootAdvancementsByTile() {
    return getSortedRootAdvancements(AdvancementEntry.sortByTitle());
  }

  public static Set<AdvancementEntry> getAdvancements(AdvancementEntry rootAdvancement) {
    if (!areAdvancementsMapped) {
      mapAdvancements();
    }
    if (rootAdvancement == null) {
      log.error("Unable to get advancements for root advancement {}", rootAdvancement);
      return new HashSet<>();
    }
    return advancementsMap.get(rootAdvancement.id);
  }

  public static Set<AdvancementEntry> getSortedAdvancements(AdvancementEntry rootAdvancement,
      Comparator<AdvancementEntry> comparator) {
    Set<AdvancementEntry> advancements = getAdvancements(rootAdvancement);
    if (advancements == null) {
      return new HashSet<>();
    }
    return advancements.stream().sorted(comparator).collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static Set<AdvancementEntry> getAdvancementsByTile(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByTitle());
  }

  public static Set<AdvancementEntry> getAdvancementsByStatus(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByStatus());
  }

  public static void setAdvancementProgress(Advancement advancement, AdvancementProgress advancementProgress) {
    advancementProgressMap.put(advancement, advancementProgress);
    rateControlMapAdvancements();
  }

  public static AdvancementProgress getAdvancementProgress(Advancement advancement) {
    if (!AdvancementProgressManager.hasAdvancementProgressListener()) {
      AdvancementProgressManager.addListener();
    }
    return advancementProgressMap.get(advancement);
  }

  public static int trackAdvancement(AdvancementEntry advancement) {
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      if (trackedAdvancementEntry.id == advancement.id) {
        log.warn("Advancement {} is already tracked.", advancement);
        return trackedAdvancements.size();
      }
    }
    if (trackedAdvancements.size() >= maxNumberOfTrackedAdvancements) {
      log.error("Number of tracked advancements {} exceeds the limit of {}", trackedAdvancements.size(), advancement);
      return trackedAdvancements.size();
    }
    trackedAdvancements.add(advancement);
    updateTrackerWidget();
    return trackedAdvancements.size();
  }

  public static int untrackAdvancement(Advancement advancement) {
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      if (trackedAdvancementEntry.id == advancement.getId()) {
        return untrackAdvancement(trackedAdvancementEntry);
      }
    }
    return trackedAdvancements.size();
  }

  public static int untrackAdvancement(AdvancementEntry advancement) {
    AdvancementEntry existingAdvancementEntry = null;
    for (AdvancementEntry trackedAdvancementEntry : trackedAdvancements) {
      if (trackedAdvancementEntry.id == advancement.id) {
        existingAdvancementEntry = trackedAdvancementEntry;
        break;
      }
    }
    if (existingAdvancementEntry != null) {
      trackedAdvancements.remove(existingAdvancementEntry);
      updateTrackerWidget();
    }
    return trackedAdvancements.size();
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

  public static AdvancementEntry getSelectedAdvancement() {
    if (selectedAdvancement == null && getSelectedRootAdvancement() != null) {
      Set<AdvancementEntry> possibleAdvancements = getAdvancements(getSelectedRootAdvancement());
      if (possibleAdvancements != null && possibleAdvancements.iterator().hasNext()) {
        selectedAdvancement = possibleAdvancements.iterator().next();
      }
    }
    return selectedAdvancement;
  }

  public static void setSelectedAdvancement(AdvancementEntry selectedAdvancement) {
    AdvancementsManager.selectedAdvancement = selectedAdvancement;
  }

  public static AdvancementEntry getSelectedRootAdvancement() {
    if (selectedRootAdvancement == null && rootAdvancements != null && rootAdvancements.iterator().hasNext()) {
      AdvancementEntry possibleRootAdvancement = rootAdvancements.iterator().next();
      if (possibleRootAdvancement != selectedRootAdvancement) {
        log.debug("Select root advancement: {}", selectedAdvancement);
        selectedRootAdvancement = possibleRootAdvancement;
        selectedAdvancement = null;
      }
    }
    return selectedRootAdvancement;
  }

  public static void setSelectedRootAdvancement(AdvancementEntry selectedRootAdvancement) {
    AdvancementsManager.selectedRootAdvancement = selectedRootAdvancement;
    if (selectedAdvancement != null && selectedRootAdvancement.id != selectedAdvancement.rootId) {
      selectedAdvancement = null;
    }
  }

  public static boolean hasAdvancements() {
    return hasAdvancements;
  }

}
