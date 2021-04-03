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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
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
  private static Date startDate = new Date();
  private static Map<Advancement, AdvancementProgress> advancementProgressMap = new HashMap<>();
  private static Map<ResourceLocation, Set<AdvancementEntry>> advancementsMap = new HashMap<>();
  private static Set<AdvancementEntry> rootAdvancements = new HashSet<>();
  private static Set<AdvancementEntry> trackedAdvancements = new HashSet<>();
  private static Timer rateControlTimer;
  private static boolean areAdvancementsMapped = false;
  private static boolean hasAdvancements = false;
  private static boolean init = false;
  private static int backgroundAdvancementCheck = 0;
  private static int maxNumberOfTrackedAdvancements = ClientConfig.CLIENT.maxNumberOfTrackedAdvancements.get();
  private static boolean screenshotEnabled = ClientConfig.CLIENT.screenshotEnabled.get();
  private static long screenshotDelay = ClientConfig.CLIENT.screenshotDelay.get();
  private static List<String> trackedAdvancementsDefault = ClientConfig.CLIENT.trackedAdvancements.get();
  private static List<String> trackedAdvancementsLocal = ClientConfig.CLIENT.trackedAdvancementsLocal.get();
  private static List<String> trackedAdvancementsRemote = ClientConfig.CLIENT.trackedAdvancementsRemote.get();
  private static String serverId;

  protected AdvancementsManager() {
  }

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    if (init) {
      return;
    }
    reset();
    updateTrackerWidget();
    rateControlMapAdvancements();
    TimerTask task = new TimerTask() {
      public void run() {
        init = false;
        cancel();
      }
    };
    Timer timer = new Timer("Timer");
    timer.schedule(task, 1000L);
    init = true;
  }

  public static void reset() {
    startDate = new Date();
    Minecraft minecraft = Minecraft.getInstance();
    ServerData serverData = minecraft != null ? minecraft.getCurrentServer() : null;
    if (serverData != null) {
      serverId = String.format("%s:%s::", serverData.name.replaceAll("[^a-zA-Z0-9_]", "_"), serverData.protocol);
      log.debug("Resetting Advancement Manager and consider new advancements after {} with remote server: {} ...",
          startDate, serverId);
    } else {
      serverId = null;
      log.debug("Resetting Advancement Manager and consider new advancements after {} with local server ...",
          startDate);
    }
    maxNumberOfTrackedAdvancements = ClientConfig.CLIENT.maxNumberOfTrackedAdvancements.get();
    screenshotDelay = ClientConfig.CLIENT.screenshotDelay.get();
    screenshotEnabled = ClientConfig.CLIENT.screenshotEnabled.get();
    trackedAdvancementsDefault = ClientConfig.CLIENT.trackedAdvancements.get();
    trackedAdvancementsLocal = ClientConfig.CLIENT.trackedAdvancementsLocal.get();
    trackedAdvancementsRemote = ClientConfig.CLIENT.trackedAdvancementsRemote.get();
    if (screenshotEnabled) {
      log.info("Enable screenshot support with {} ms delay", screenshotDelay);
    } else {
      log.info("Disable screenshot support.");
    }
    advancementProgressMap = new HashMap<>();
    advancementsMap = new HashMap<>();
    rootAdvancements = new HashSet<>();
    trackedAdvancements = new HashSet<>();
    selectedAdvancement = null;
    selectedRootAdvancement = null;
    hasAdvancements = false;
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
    rateControlTimer = new Timer("mapAdvancements Timer");
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
        log.info("[Child Advancement] {}", advancementEntry);
        if (!hasAdvancements) {
          hasAdvancements = true;
        }
      }
    }

    // Adding AdvancementProgress if not already done.
    if (!AdvancementProgressManager.hasAdvancementProgressListener()) {
      AdvancementProgressManager.addListener();
    }

    loadTrackedAdvancements();

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

  public static AdvancementEntry getAdvancement(String id) {
    for (Set<AdvancementEntry> advancementEntries : advancementsMap.values()) {
      for (AdvancementEntry advancementEntry : advancementEntries) {
        if (id.equals(advancementEntry.id.toString())) {
          return advancementEntry;
        }
      }
    }
    return null;
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

  public static void updateAdvancementProgress(Advancement advancement, AdvancementProgress advancementProgress) {
    String advancementId = advancement.getId().toString();
    advancementProgressMap.put(advancement, advancementProgress);
    if (advancementProgress.isDone() && !advancementId.startsWith("minecraft:recipes")
        && !advancementId.startsWith("smallships:recipes") && advancement.getParent() != null
        && advancement.getDisplay() != null) {
      // Find last progression date to make sure we skip outdate advancements for
      // screenshots.
      if (screenshotEnabled) {
        Iterable<String> completedCriteria = advancementProgress.getCompletedCriteria();
        Date lastProgressionDate = startDate;
        if (completedCriteria != null) {
          for (String criteriaId : completedCriteria) {
            CriterionProgress criteriaProgress = advancementProgress.getCriterion(criteriaId);
            if (criteriaProgress.getObtained().after(lastProgressionDate)) {
              lastProgressionDate = criteriaProgress.getObtained();
            }
          }
        }

        if (lastProgressionDate.after(startDate)) {
          log.info("Found new advancements which was done on {}", lastProgressionDate);
          String screenshotFolder = advancementId.split("/")[0].replace(":", "_");
          String screenshotName = String.format("advancement-unknown-%s", new Random().nextInt(99));
          if (advancementId.contains("/") && advancementId.split("/").length > 1) {
            screenshotName = advancementId.split("/", 2)[1].replace("/", "_");
          } else if (advancementId.contains(":") && advancementId.split(":").length > 1) {
            screenshotFolder = advancementId.split(":")[0];
            screenshotName = advancementId.split(":", 2)[1];
          } else {
            log.warn("Unable to find unique name ({}) for advancement: {}", advancementId, advancement);
          }
          ScreenManager.saveScreenshot(new File(String.format("screenshots/%s", screenshotFolder)), screenshotName,
              screenshotDelay);
        }
      }
      untrackAdvancement(advancement);
    }
    rateControlMapAdvancements();
  }

  public static AdvancementProgress getAdvancementProgress(Advancement advancement) {
    if (!AdvancementProgressManager.hasAdvancementProgressListener()) {
      AdvancementProgressManager.addListener();
    }
    return advancementProgressMap.get(advancement);
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
      log.error("Number of tracked advancements {} exceeds the limit of {}", trackedAdvancements.size(), advancement);
    }
    trackedAdvancements.add(advancement);
    if (autosave) {
      saveTrackedAdvancements();
    }
    updateTrackerWidget();
  }

  private static void loadTrackedAdvancementsDefault() {
    if (trackedAdvancementsDefault.isEmpty()) {
      return;
    }
    for (String trackedAdvancementDefault : trackedAdvancementsDefault) {
      AdvancementEntry trackedAdvancementEntry = getAdvancement(trackedAdvancementDefault);
      if (trackedAdvancementEntry != null) {
        log.debug("Adding default tracked advancement {}", trackedAdvancementEntry);
        trackAdvancement(trackedAdvancementEntry, false);
      }
    }
  }

  private static void loadTrackedAdvancements() {
    // Loading default tracked advancements from config for better mod support.
    loadTrackedAdvancementsDefault();

    // Loading cached tracked advancements if list is empty or if tracking limit is
    // not exceeded.
    if (trackedAdvancements.isEmpty() || trackedAdvancements.size() < maxNumberOfTrackedAdvancements) {
      loadTrackedAdvancementsRemote();
      loadTrackedAdvancementsLocal();
    }
  }

  private static void loadTrackedAdvancementsRemote() {
    if (serverId == null || trackedAdvancementsRemote.isEmpty()) {
      return;
    }
    for (String cachedAdvancementEntry : trackedAdvancementsRemote) {
      if (!cachedAdvancementEntry.isEmpty() && !"".equals(cachedAdvancementEntry) && cachedAdvancementEntry.startsWith(serverId)) {
        AdvancementEntry trackedAdvancementEntry = getAdvancement(cachedAdvancementEntry.split("::", 2)[1]);
        if (trackedAdvancementEntry != null) {
          log.debug("Adding remote tracked advancement {}", trackedAdvancementEntry);
          trackAdvancement(trackedAdvancementEntry, false);
        }
      }
    }
  }

  private static void loadTrackedAdvancementsLocal() {
    if (serverId != null || trackedAdvancementsLocal.isEmpty()) {
      return;
    }
    for (String cachedAdvancementEntry : trackedAdvancementsLocal) {
      if (!cachedAdvancementEntry.isEmpty() && !"".equals(cachedAdvancementEntry)) {
        AdvancementEntry trackedAdvancementEntry = getAdvancement(cachedAdvancementEntry);
        if (trackedAdvancementEntry != null) {
          log.debug("Adding local tracked advancement {}", trackedAdvancementEntry);
          trackAdvancement(trackedAdvancementEntry, false);
        }
      }
    }
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
      if (!trackedAdvancementRemote.isEmpty() && !"".equals(trackedAdvancementRemote) && !trackedAdvancementRemote.startsWith(serverId)) {
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

  private static void updateTrackerWidget() {
    TrackerWidget.setTrackedAdvancements(trackedAdvancements);
  }

}
