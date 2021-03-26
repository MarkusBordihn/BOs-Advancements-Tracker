package de.markusbordihn.advancementstracker.client.advancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class AdvancementEntry {

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
      this.isDone = advancementProgress.isDone();
      this.progress = advancementProgress.getPercent();
      this.progressText = advancementProgress.getProgressText();
      this.remainingCriteria = advancementProgress.getRemainingCriteria();
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

  public String getTitle() {
    return this.title;
  }
}
