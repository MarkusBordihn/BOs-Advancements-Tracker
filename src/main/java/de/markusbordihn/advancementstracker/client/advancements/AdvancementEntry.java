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

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.item.ItemStack;
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
  public ItemStack icon;
  public Iterable<String> completedCriteria;
  public Iterable<String> remainingCriteria;
  public int completedCriteriaNumber;
  public int remainingCriteriaNumber;
  public int criteriaNumber;
  public ResourceLocation background;
  public ResourceLocation id;
  public String description;
  public String title;
  public boolean isDone;
  public int descriptionColor = 0xFFCCCCCC;

  AdvancementEntry(Advancement advancement, AdvancementProgress advancementProgress) {
    this.advancement = advancement;
    this.advancementProgress = advancementProgress;
    this.displayInfo = advancement.getDisplay();
    this.id = advancement.getId();
    this.rootAdvancement = advancement.getParent();
    this.requirements = advancement.getRequirements();
    if (this.rootAdvancement != null) {
      while (this.rootAdvancement.getParent() != null) {
        this.rootAdvancement = this.rootAdvancement.getParent();
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
    } else {
      this.background = null;
      this.title = advancement.getId().toString();
    }
    if (advancementProgress != null) {
      this.completedCriteria = advancementProgress.getCompletedCriteria();
      this.completedCriteriaNumber = (int) this.completedCriteria.spliterator().getExactSizeIfKnown();
      this.isDone = advancementProgress.isDone();
      this.progress = advancementProgress.getPercent();
      this.progressText = advancementProgress.getProgressText();
      this.remainingCriteria = advancementProgress.getRemainingCriteria();
      this.remainingCriteriaNumber = (int) this.remainingCriteria.spliterator().getExactSizeIfKnown();
      this.criteriaNumber = completedCriteriaNumber + remainingCriteriaNumber;
    }
  }

  public boolean isTracked() {
    return AdvancementsManager.isTrackedAdvancement(advancement);
  }

  public String toString() {
    if (this.rootAdvancement == null) {
      return String.format("[Root Advancement] %s: %s %s", this.rootId, this.title, this.progress);
    }
    return String.format("[Advancement] %s: %s %s", this.rootId, this.title, this.progress);
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
  public int compare(AdvancementEntry firstAdvancementEntry, AdvancementEntry secondAdvancementEntry) {
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
