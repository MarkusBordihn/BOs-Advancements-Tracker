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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import de.markusbordihn.advancementstracker.Constants;
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
  private static Set<String> advancementsIndex = new HashSet<>();
  private static Set<String> screenshotIndex = new HashSet<>();
  private static boolean hasAdvancements = false;
  private static boolean screenshotEnabled = ClientConfig.CLIENT.screenshotEnabled.get();
  private static long screenshotDelay = ClientConfig.CLIENT.screenshotDelay.get();

  protected AdvancementsManager() {}

  @SubscribeEvent
  public static void handleWorldEventLoad(WorldEvent.Load event) {
    if (!event.getWorld().isClientSide()) {
      return;
    }
    reset();
  }

  public static void reset() {
    log.info("Reset Advancements Manager ...");
    startDate = new Date();
    screenshotDelay = ClientConfig.CLIENT.screenshotDelay.get();
    screenshotEnabled = ClientConfig.CLIENT.screenshotEnabled.get();
    if (screenshotEnabled) {
      log.info("Enable screenshot support with {} ms delay", screenshotDelay);
    } else {
      log.info("Disable screenshot support.");
    }
    advancementProgressMap = new HashMap<>();
    advancementsIndex = new HashSet<>();
    advancementsMap = new HashMap<>();
    hasAdvancements = false;
    rootAdvancements = new HashSet<>();
    screenshotIndex = new HashSet<>();
    selectedAdvancement = null;
    selectedRootAdvancement = null;
  }

  public static void addAdvancementRoot(Advancement advancement) {
    String advancementId = advancement.getId().toString();
    if (hasAdvancement(advancementId)) {
      return;
    }
    AdvancementProgress advancementProgress = getAdvancementProgress(advancement);
    AdvancementEntry advancementEntry = new AdvancementEntry(advancement, advancementProgress);
    rootAdvancements.add(advancementEntry);
    advancementsIndex.add(advancementId);
    log.info("{}", advancementEntry);
  }

  public static void addAdvancementTask(Advancement advancement) {
    String advancementId = advancement.getId().toString();
    Advancement rootAdvancement = advancement.getParent();
    if (rootAdvancement != null) {
      while (rootAdvancement.getParent() != null) {
        rootAdvancement = rootAdvancement.getParent();
      }
      addAdvancementRoot(rootAdvancement);
    }
    if (hasAdvancement(advancementId)) {
      return;
    }
    AdvancementProgress advancementProgress = getAdvancementProgress(advancement);
    AdvancementEntry advancementEntry = new AdvancementEntry(advancement, advancementProgress);
    Set<AdvancementEntry> childAdvancements = advancementsMap.get(advancementEntry.rootId);
    if (childAdvancements == null) {
      childAdvancements = new HashSet<>();
      advancementsMap.put(advancementEntry.rootId, childAdvancements);
    }
    childAdvancements.add(advancementEntry);
    advancementsIndex.add(advancementId);
    if (!hasAdvancements) {
      hasAdvancements = true;
    }
    log.info("{}", advancementEntry);
    TrackedAdvancementsManager.checkForTrackedAdvancement(advancementEntry);
  }

  public static boolean hasAdvancement(Advancement advancement) {
    return hasAdvancement(advancement.getId().toString());
  }

  public static boolean hasAdvancement(String advancementId) {
    return advancementsIndex.contains(advancementId);
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
    return null;
  }

  public static Set<AdvancementEntry> getRootAdvancements() {
    return rootAdvancements;
  }

  public static Set<AdvancementEntry> getSortedRootAdvancements(
      Comparator<AdvancementEntry> comparator) {
    Set<AdvancementEntry> advancements = getRootAdvancements();
    if (advancements == null) {
      return new HashSet<>();
    }
    return advancements.stream().sorted(comparator)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static Set<AdvancementEntry> getRootAdvancementsByTile() {
    return getSortedRootAdvancements(AdvancementEntry.sortByTitle());
  }

  public static AdvancementEntry getAdvancement(Advancement advancement) {
    return getAdvancement(advancement.getId().toString());
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
    return advancements.stream().sorted(comparator)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  public static Set<AdvancementEntry> getAdvancementsByTile(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByTitle());
  }

  public static Set<AdvancementEntry> getAdvancementsByStatus(AdvancementEntry rootAdvancement) {
    return getSortedAdvancements(rootAdvancement, AdvancementEntry.sortByStatus());
  }

  public static void updateAdvancementProgress(Advancement advancement,
      AdvancementProgress advancementProgress) {
    String advancementId = advancement.getId().toString();
    advancementProgressMap.put(advancement, advancementProgress);
    AdvancementEntry advancementEntry = getAdvancement(advancement);
    if (advancementEntry == null) {
      advancementEntry = getRootAdvancement(advancement);
      if (advancementEntry == null) {
        log.error("Unable to find entry for advancement {} with progress {}", advancement,
            advancementProgress);
        return;
      }
    }
    advancementEntry.addAdvancementProgress(advancementProgress);
    if (advancementProgress.isDone()) {
      // Find last progression date to make sure we skip outdate advancements for
      // screenshots.
      if (screenshotEnabled) {
        if (screenshotIndex.contains(advancementId)) {
          log.debug("Screenshot was already taken for {}, skipping ...", advancementId);
        } else {
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
            log.info("Found new advancement {} which was done on {}", advancementId,
                lastProgressionDate);
            String screenshotFolder = advancementId.split("/")[0].replace(":", "_");
            String screenshotName =
                String.format("advancement-unknown-%s", new Random().nextInt(99));
            if (advancementId.contains("/") && advancementId.split("/").length > 1) {
              screenshotName = advancementId.split("/", 2)[1].replace("/", "_");
            } else if (advancementId.contains(":") && advancementId.split(":").length > 1) {
              screenshotFolder = advancementId.split(":")[0];
              screenshotName = advancementId.split(":", 2)[1];
            } else {
              log.warn("Unable to find unique name ({}) for advancement: {}", advancementId,
                  advancement);
            }
            ScreenManager.saveScreenshot(
                new File(String.format("screenshots/%s", screenshotFolder)), screenshotName,
                screenshotDelay);
            screenshotIndex.add(advancementId);
          }
        }
      }
      TrackedAdvancementsManager.untrackAdvancement(advancement);
    }
  }

  public static AdvancementProgress getAdvancementProgress(Advancement advancement) {
    return advancementProgressMap.get(advancement);
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
    if (selectedRootAdvancement == null && rootAdvancements != null
        && rootAdvancements.iterator().hasNext()) {
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
