/**
 * Copyright 2022 Markus Bordihn
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

package de.markusbordihn.advancementstracker.client.gui.screens;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.StringUtils;

import de.markusbordihn.advancementstracker.Constants;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementsManager;
import de.markusbordihn.advancementstracker.client.gui.panel.AdvancementCategoryPanel;
import de.markusbordihn.advancementstracker.client.gui.panel.AdvancementOverviewPanel;

@OnlyIn(Dist.CLIENT)
public class AdvancementsTrackerScreen extends Screen {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  // Layout
  private static final int PADDING = 6;
  private int buttonMargin = 1;

  private Screen parentScreen = null;

  // Sorting Support
  private enum SortType implements Comparator<AdvancementEntry> {
    NORMAL, A_TO_Z {
      @Override
      protected int compare(String name1, String name2) {
        return name1.compareTo(name2);
      }
    },
    Z_TO_A {
      @Override
      protected int compare(String name1, String name2) {
        return name2.compareTo(name1);
      }
    };

    Button button;

    protected int compare(String name1, String name2) {
      return 0;
    }

    @Override
    public int compare(AdvancementEntry o1, AdvancementEntry o2) {
      String name1 = StringUtils.toLowerCase(stripControlCodes(o1.title));
      String name2 = StringUtils.toLowerCase(stripControlCodes(o2.title));
      return compare(name1, name2);
    }

    Component getButtonText() {
      return new TranslatableComponent("fml.menu.mods." + StringUtils.toLowerCase(name()));
    }
  }

  private SortType sortType = SortType.NORMAL;
  private boolean sorted = false;

  // Advancements
  Set<AdvancementEntry> rootAdvancements;
  Set<AdvancementEntry> childAdvancements;
  private AdvancementEntry selectedRootAdvancement = null;
  private AdvancementEntry selectedChildAdvancement = null;

  // Panels
  private AdvancementCategoryPanel advancementCategoryPanel;
  private AdvancementOverviewPanel advancementOverviewPanel;

  // Layout specific settings
  private int listWidth;

  public AdvancementsTrackerScreen() {
    this(new TextComponent("Advancements Tracker"));
  }

  private static String stripControlCodes(String value) {
    return net.minecraft.util.StringUtil.stripColor(value);
  }

  public AdvancementsTrackerScreen(Component component) {
    this(null, component);
  }

  public AdvancementsTrackerScreen(Screen screen, Component component) {
    super(component);
    this.parentScreen = screen;
  }

  public Minecraft getMinecraftInstance() {
    return minecraft;
  }

  public Font getFontRenderer() {
    return font;
  }

  public <T extends ObjectSelectionList.Entry<T>> void buildRootAdvancementsList(
      Consumer<T> listViewConsumer, Function<AdvancementEntry, T> newEntry) {
    if (this.rootAdvancements == null) {
      this.reloadRootAdvancements();
    }
    this.rootAdvancements
        .forEach(advancementEntry -> listViewConsumer.accept(newEntry.apply(advancementEntry)));
  }

  public void reloadRootAdvancements() {
    this.reloadRootAdvancements(SortType.NORMAL);
  }

  public void reloadRootAdvancements(SortType sortType) {
    if (sortType == SortType.NORMAL) {
      this.rootAdvancements = AdvancementsManager.getRootAdvancements();
    } else {
      this.rootAdvancements = AdvancementsManager.getSortedRootAdvancements(sortType);
    }
  }

  private void resortRootAdvancements(SortType newSort) {
    this.sortType = newSort;

    for (SortType sort : SortType.values()) {
      if (sort.button != null)
        sort.button.active = sortType != sort;
    }
    sorted = false;
  }

  public void setSelectedRootAdvancement(AdvancementCategoryPanel.RootAdvancementEntry entry) {
    AdvancementEntry advancementEntry = entry.getAdvancementEntry();
    if (advancementEntry == null || this.selectedRootAdvancement == advancementEntry) {
      return;
    }
    this.selectedRootAdvancement = advancementEntry;
    log.info("Selected root entry ... {}", this.selectedRootAdvancement);
    this.reloadChildAdvancements();
  }

  public AdvancementEntry getSelectedRootAdvancement() {
    return this.selectedRootAdvancement;
  }

  public <T extends ObjectSelectionList.Entry<T>> void buildChildAdvancementsList(
      Consumer<T> listViewConsumer, Function<AdvancementEntry, T> newEntry) {
    if (this.childAdvancements == null) {
      return;
    }
    this.childAdvancements
        .forEach(advancementEntry -> listViewConsumer.accept(newEntry.apply(advancementEntry)));
  }

  public void reloadChildAdvancements() {
    if (this.selectedRootAdvancement == null) {
      return;
    }
    if (sortType == SortType.NORMAL) {
      this.childAdvancements = AdvancementsManager.getAdvancements(this.selectedRootAdvancement);
    } else {
      this.childAdvancements =
          AdvancementsManager.getSortedAdvancements(this.selectedRootAdvancement, sortType);
    }
    this.advancementOverviewPanel.refreshList();
  }

  public void setSelectedChildAdvancement(AdvancementOverviewPanel.ChildAdvancementEntry entry) {
    AdvancementEntry advancementEntry = entry.getAdvancementEntry();
    if (this.selectedChildAdvancement == advancementEntry) {
      return;
    }
    this.selectedChildAdvancement = advancementEntry;
    log.info("Selected child entry ... {}", this.selectedChildAdvancement);
  }

  public AdvancementEntry getSelectedChildAdvancement() {
    return this.selectedChildAdvancement;
  }

  @Override
  protected void init() {
    super.init();

    // Calculate viewport and general design
    listWidth = Math.max(width / 3, 100);
    int topPosition = PADDING + 20;

    // Advancements
    reloadRootAdvancements();
    reloadChildAdvancements();

    // Panel Positions
    int categoryPanelLeftPosition = 0;
    int overviewPanelLeftPosition = listWidth + PADDING;

    // Define areas
    this.advancementCategoryPanel = new AdvancementCategoryPanel(this, listWidth, topPosition,
        categoryPanelLeftPosition, height - PADDING);
    this.advancementOverviewPanel = new AdvancementOverviewPanel(this, listWidth * 2 - 13,
        topPosition, overviewPanelLeftPosition, height - PADDING);

    // Add Scroll panels for advancements
    this.addRenderableWidget(this.advancementCategoryPanel);
    this.addRenderableWidget(this.advancementOverviewPanel);

    // Sort Buttons
    int x = PADDING;
    addRenderableWidget(SortType.NORMAL.button = new Button(x, PADDING, 50, 20,
        SortType.NORMAL.getButtonText(), b -> resortRootAdvancements(SortType.NORMAL)));
    x += 50 + buttonMargin;
    addRenderableWidget(SortType.A_TO_Z.button = new Button(x, PADDING, 50, 20,
        SortType.A_TO_Z.getButtonText(), b -> resortRootAdvancements(SortType.A_TO_Z)));
    x += 50 + buttonMargin;
    addRenderableWidget(SortType.Z_TO_A.button = new Button(x, PADDING, 50, 20,
        SortType.Z_TO_A.getButtonText(), b -> resortRootAdvancements(SortType.Z_TO_A)));
    resortRootAdvancements(SortType.NORMAL);
  }

  @Override
  public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
    this.renderBackground(poseStack);

    this.advancementCategoryPanel.render(poseStack, mouseX, mouseY, partialTick);
    this.advancementOverviewPanel.render(poseStack, mouseX, mouseY, partialTick);

    super.render(poseStack, mouseX, mouseY, partialTick);

    drawCenteredString(poseStack, this.font, this.title, this.width / 2, 8, 16777215);
  }

  @Override
  public void tick() {

    if (!this.sorted) {
      reloadRootAdvancements(sortType);
      this.advancementCategoryPanel.refreshList();
      this.advancementOverviewPanel.refreshList();

      this.sorted = true;
    }
  }

}
