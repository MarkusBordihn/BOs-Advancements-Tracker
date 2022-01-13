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

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;

public class AdvancementEntry implements Comparator<AdvancementEntry> {

  Advancement advancement;
  Advancement rootAdvancement;
  AdvancementProgress advancementProgress;
  DisplayInfo displayInfo;
  Float progress;
  ResourceLocation rootId;
  String progressText;
  String[][] requirements;
  int rootLevel = 0;
  Map<String, CriterionProgress> criteriaMap = new HashMap<>();

  public Date firstProgressDate;
  public Date lastProgressDate;
  public FrameType frameType;
  public ItemStack icon;
  public Iterable<String> completedCriteria;
  public Iterable<String> remainingCriteria;
  public ResourceLocation background;
  public ResourceLocation id;
  public String description;
  public String idString;
  public String title;
  public boolean isDone;
  public int completedCriteriaNumber;
  public int criteriaNumber;
  public int descriptionColor = 0xFFCCCCCC;
  public int maxCriteraRequired;
  public int remainingCriteriaNumber;
  public int requirementsNumber;

  // Rewards
  private AdvancementRewards rewards = null;
  private ResourceLocation[] rewardsLoot = null;
  private ResourceLocation[] rewardsRecipes = null;
  private boolean hasRewards = false;
  private boolean hasRewardsData = false;
  private Integer rewardsExperience = null;

  AdvancementEntry(Advancement advancement, AdvancementProgress advancementProgress) {
    this.advancement = advancement;
    this.displayInfo = advancement.getDisplay();
    this.id = advancement.getId();
    this.idString = advancement.getId().toString();
    this.rootAdvancement = advancement.getParent();
    this.requirements = advancement.getRequirements();
    this.maxCriteraRequired = advancement.getMaxCriteraRequired();
    this.rewards = advancement.getRewards();
    this.hasRewards = this.rewards != null;

    if (this.rootAdvancement != null) {
      while (this.rootAdvancement.getParent() != null) {
        this.rootAdvancement = this.rootAdvancement.getParent();
        rootLevel++;
      }
      this.rootId = this.rootAdvancement.getId();
    }
    if (this.displayInfo != null) {
      this.background = this.displayInfo.getBackground();
      this.description = this.displayInfo.getDescription().getString();
      if (this.displayInfo.getDescription().getStyle().getColor() != null) {
        this.descriptionColor = this.displayInfo.getDescription().getStyle().getColor().getValue();
      }
      this.icon = this.displayInfo.getIcon();
      this.title = this.displayInfo.getTitle().getString();
      this.frameType = this.displayInfo.getFrame();
    } else {
      this.background = null;
      this.title = advancement.getId().toString();
    }
    if (advancementProgress != null) {
      this.addAdvancementProgress(advancementProgress);
    }
  }

  public boolean isTracked() {
    return TrackedAdvancementsManager.isTrackedAdvancement(advancement);
  }

  public ResourceLocation getId() {
    return this.id;
  }

  public String toString() {
    if (this.rootAdvancement == null) {
      return String.format("[Root Advancement] (%s) %s: %s %s", this.frameType, this.id, this.title,
          this.progress);
    }
    return String.format("[Advancement %s] (%s) %s => %s: %s %s", this.rootLevel, this.frameType,
        this.rootId, this.id, this.title, this.progress);
  }

  public void addAdvancementProgress(AdvancementProgress advancementProgress) {
    if (advancementProgress == null) {
      return;
    }
    this.advancementProgress = advancementProgress;
    this.isDone = advancementProgress.isDone();
    this.firstProgressDate = advancementProgress.getFirstProgressDate();
    this.progress = advancementProgress.getPercent();
    this.progressText = advancementProgress.getProgressText();

    // Handle completed Criteria
    this.completedCriteria = advancementProgress.getCompletedCriteria();
    this.completedCriteriaNumber = (int) this.completedCriteria.spliterator().getExactSizeIfKnown();
    for (String criteriaId : this.completedCriteria) {
      criteriaMap.put(criteriaId, advancementProgress.getCriterion(criteriaId));
    }

    // Handle remaining Criteria
    this.remainingCriteria = advancementProgress.getRemainingCriteria();
    this.remainingCriteriaNumber = (int) this.remainingCriteria.spliterator().getExactSizeIfKnown();
    for (String criteriaId : this.remainingCriteria) {
      criteriaMap.put(criteriaId, advancementProgress.getCriterion(criteriaId));
    }

    this.lastProgressDate = this.getLastProgressDate();
  }

  private Date getLastProgressDate() {
    Date date = null;
    for (CriterionProgress criterionProgress : this.criteriaMap.values()) {
      if (criterionProgress.isDone()
          && (date == null || criterionProgress.getObtained().after(date))) {
        date = criterionProgress.getObtained();
      }
    }
    return date;
  }

  public Integer getRewardsExperience() {
    if (this.hasRewards && this.rewardsExperience == null) {
      JsonObject rewardsData = getRewardsData();
      if (rewardsData != null) {
        // Getting rewards experience
        this.rewardsExperience = JSONUtils.getAsInt(rewardsData, "experience", 0);
        if (this.rewardsExperience > 0) {
          this.hasRewardsData = true;
        }
      }
    }
    return this.rewardsExperience;
  }

  public ResourceLocation[] getRewardsLoot() {
    if (this.hasRewards && this.rewardsLoot == null) {
      JsonObject rewardsData = getRewardsData();
      if (rewardsData != null) {
        // Getting Loot entries
        JsonArray lootArray = JSONUtils.getAsJsonArray(rewardsData, "loot", new JsonArray());
        this.rewardsLoot = new ResourceLocation[lootArray.size()];
        for (int j = 0; j < this.rewardsLoot.length; ++j) {
          this.rewardsLoot[j] =
              new ResourceLocation(JSONUtils.convertToString(lootArray.get(j), "loot[" + j + "]"));
          this.hasRewardsData = true;
        }
      }
    }
    return this.rewardsLoot;
  }

  public ResourceLocation[] getRewardsRecipes() {
    if (this.hasRewards && this.rewardsRecipes == null) {
      JsonObject rewardsData = getRewardsData();
      if (rewardsData != null) {
        // Getting recipes entries
        JsonArray recipesArray = JSONUtils.getAsJsonArray(rewardsData, "recipes", new JsonArray());
        this.rewardsRecipes = new ResourceLocation[recipesArray.size()];
        for (int k = 0; k < this.rewardsRecipes.length; ++k) {
          this.rewardsRecipes[k] = new ResourceLocation(
              JSONUtils.convertToString(recipesArray.get(k), "recipes[" + k + "]"));
          this.hasRewardsData = true;
        }
      }
    }
    return this.rewardsRecipes;
  }

  private JsonObject getRewardsData() {
    // There is no direct access to the rewards information, for this reason we
    // are using the JsonObject to get access to the relevant information.
    JsonElement rewardsJson = null;
    try {
      rewardsJson = this.rewards.serializeToJson();
      if (rewardsJson != null) {
        JsonObject rewardsObject = rewardsJson.getAsJsonObject();
        if (rewardsObject != null) {
          return rewardsObject;
        }
      }
    } catch (JsonParseException | IllegalStateException e) {
      // Ignore possible JSON Parse Exception and illegal state exceptions
    }
    return null;
  }

  public boolean hasRewards() {
    return this.hasRewards;
  }

  public boolean hasRewardsData() {
    return this.hasRewards && this.hasRewardsData;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AdvancementEntry)) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    return this.id == ((AdvancementEntry) obj).id;
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public int compare(AdvancementEntry firstAdvancementEntry,
      AdvancementEntry secondAdvancementEntry) {
    return firstAdvancementEntry.id.compareTo(secondAdvancementEntry.id);
  }

  public static Comparator<AdvancementEntry> sortByTitle() {
    return (AdvancementEntry firstAdvancementEntry, AdvancementEntry secondAdvancementEntry) -> {
      return firstAdvancementEntry.title.compareTo(secondAdvancementEntry.title);
    };
  }

  public static Comparator<AdvancementEntry> sortByStatus() {
    return (AdvancementEntry firstAdvancementEntry, AdvancementEntry secondAdvancementEntry) -> {
      int result = Boolean.compare(firstAdvancementEntry.isDone, secondAdvancementEntry.isDone);
      if (result == 0) {
        result = firstAdvancementEntry.title.compareTo(secondAdvancementEntry.title);
      }
      return result;
    };
  }

}
