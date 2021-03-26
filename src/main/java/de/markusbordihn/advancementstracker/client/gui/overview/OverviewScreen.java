package de.markusbordihn.advancementstracker.client.gui.overview;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.text.TranslationTextComponent;

import com.mojang.blaze3d.matrix.MatrixStack;

import de.markusbordihn.advancementstracker.client.advancements.AdvancementEntry;
import de.markusbordihn.advancementstracker.client.advancements.AdvancementsManager;
import de.markusbordihn.advancementstracker.client.gui.ScreenBuilder;
import de.markusbordihn.advancementstracker.client.gui.Textures;
import de.markusbordihn.advancementstracker.client.gui.component.ScrollPanelContent;
import de.markusbordihn.advancementstracker.client.gui.component.ScrollPanelManager;

public class OverviewScreen extends ScreenBuilder {

  AdvancementCategoryPanel advancementCategoryPanel;
  AdvancementContentPanel advancementContentPanel;
  AdvancementInfoPanel advancementInfoPanel;

  public OverviewScreen() {
    super("advancementScreen.title");
  }

  class AdvancementCategoryContent extends ScrollPanelContent {

    AdvancementEntry advancement;

    AdvancementCategoryContent(AdvancementEntry advancement, int width) {
      super(advancement.id.toString(), width, 40);
      this.advancement = advancement;
      this.background = advancement.background;
    }

    @Override
    protected void drawContent(MatrixStack matrixStack, int entryRight, int relativeY, Tessellator tessellator,
        int mouseX, int mouseY) {
      if (this.advancement.icon != null) {
        this.minecraft.getItemRenderer().renderGuiItem(this.advancement.icon, this.x + 2, this.y + 10);
      }
      int yNext = drawTextWithShadow(matrixStack, this.advancement.title, this.x + 20, this.y + 4, 0xFFFFFF);
      drawText(matrixStack, this.advancement.description, x + 20, yNext + 2, this.advancement.descriptionColor);
    }

    public AdvancementEntry getAdvancement() {
      return this.advancement;
    }
  }

  class AdvancementContent extends ScrollPanelContent {

    AdvancementEntry advancement;

    AdvancementContent(AdvancementEntry advancement, int width) {
      super(advancement.id.toString(), width, 40);
      this.advancement = advancement;
    }

    @Override
    protected void drawContent(MatrixStack matrixStack, int entryRight, int relativeY, Tessellator tess, int mouseX,
        int mouseY) {
      if (this.advancement.icon != null) {
        this.minecraft.getItemRenderer().renderGuiItem(this.advancement.icon, x + 2, y + 2);
      }
      this.textureManager.bind(Textures.ICONS);
      if (Boolean.TRUE.equals(this.advancement.isDone)) {
        blit(matrixStack, xMax - 20, y + 2, 0, 0, 18, 18);
      } else if (Boolean.TRUE.equals(this.advancement.isTracked())) {
        blit(matrixStack, xMax - 18, y + 2, 40, 2, 14, 14);
      } else if (AdvancementsManager.hasReachedTrackedAdvancementLimit()) {
        blit(matrixStack, xMax - 18, y + 2, 60, 2, 14, 14);
      } else {
        blit(matrixStack, xMax - 18, y + 2, 20, 2, 14, 14);
      }

      int yNext = drawTrimmedTextWithShadow(matrixStack, this.advancement.title, width - 42, x + 20, y + 6, 0xFFFFFF);
      drawText(matrixStack, this.advancement.description, x + 20, yNext + 2, this.advancement.descriptionColor);
      hLine(matrixStack, x + 40, xMax - 40, yMax, 0x20CCCCCC);
    }

    @Override
    protected void handleClick(double mouseX, double mouseY, int button) {
      if (Boolean.TRUE.equals(this.advancement.isDone)) {
        return;
      }
      if ((xMax - 18 < mouseX && mouseX < xMax) && (baseY + 2 < mouseY && mouseY < baseY + 16)) {
        if (Boolean.TRUE.equals(this.advancement.isTracked())) {
          AdvancementsManager.untrackAdvancement(advancement);
        } else if (!AdvancementsManager.hasReachedTrackedAdvancementLimit()) {
          AdvancementsManager.trackAdvancement(advancement);
        }
      }
    }

    public AdvancementEntry getAdvancement() {
      return this.advancement;
    }
  }

  class AdvancementInfo extends ScrollPanelContent {

    AdvancementEntry advancement;
    String output = "";

    AdvancementInfo(AdvancementEntry advancement, int width) {
      super(advancement.id.toString(), width, 200);
      this.advancement = advancement;
      if (this.advancement.description != null) {
        this.output += String.format("%s\n", this.advancement.description);
      }
      if (this.advancement.completedCriteria != null && this.advancement.completedCriteria.iterator().hasNext()) {
        this.output += String.format("\nCompleted Criteria\n");
        for (String criteria : this.advancement.completedCriteria) {
          this.output += String.format("%s\n", criteria);
        }
      }
      if (this.advancement.remainingCriteria != null && this.advancement.remainingCriteria.iterator().hasNext()) {
        this.output += String.format("\nRemaining Criteria\n");
        for (String criteria : this.advancement.remainingCriteria) {
          this.output += String.format("%s\n", criteria);
        }
      }
    }

    @Override
    protected void drawContent(MatrixStack matrixStack, int entryRight, int relativeY, Tessellator tess, int mouseX,
        int mouseY) {
      int yNext = drawText(matrixStack, this.advancement.title, x + 5, y + 5, 0xFFFFFF);
      drawText(matrixStack, this.output, x + 5, yNext + 2, 0xFFFFFF);
    }

  }

  class AdvancementCategoryPanel extends ScrollPanelManager {

    AdvancementContentPanel advancementContentPanel;

    AdvancementCategoryPanel(Minecraft minecraft, int width, int height, int top, int left) {
      super(minecraft, width, height, top, left);
      Set<AdvancementEntry> rootAdvancements = AdvancementsManager.getSortedRootAdvancements();
      if (rootAdvancements == null) {
        return;
      }
      AdvancementEntry selectedRootAdvancement = AdvancementsManager.getSelectedRootAdvancement();
      for (AdvancementEntry advancement : rootAdvancements) {
        String contentName = advancement.id.toString();
        this.addContent(contentName, new AdvancementCategoryContent(advancement, this.width),
            advancement == selectedRootAdvancement);
      }
      this.preRender();
    }

    @Override
    public void handleClick(ScrollPanelContent scrollPanelContent, int button) {
      AdvancementCategoryContent advancementCategoryContent = (AdvancementCategoryContent) scrollPanelContent;
      AdvancementEntry clickedRootAdvancement = advancementCategoryContent.getAdvancement();
      AdvancementsManager.setSelectedRootAdvancement(clickedRootAdvancement);
      if (this.advancementContentPanel != null) {
        this.advancementContentPanel.updateContent(AdvancementsManager.getSelectedRootAdvancement());
      }
    }

    public void setAdvancementContentPanel(AdvancementContentPanel advancementContentPanel) {
      this.advancementContentPanel = advancementContentPanel;
    }
  }

  class AdvancementContentPanel extends ScrollPanelManager {

    AdvancementInfoPanel advancementInfoPanel;

    AdvancementContentPanel(Minecraft minecraft, int width, int height, int top, int left,
        AdvancementEntry rootAdvancement) {
      super(minecraft, width, height, top, left);
      updateContent(rootAdvancement);
    }

    public void updateContent(AdvancementEntry rootAdvancement) {
      if (rootAdvancement == null) {
        log.error("Unable to get content for advancement category {}", rootAdvancement);
        return;
      }
      this.background = rootAdvancement.background;
      this.clearContent();
      Set<AdvancementEntry> advancements = AdvancementsManager.getAdvancements(rootAdvancement);
      if (advancements == null) {
        log.error("Unable to get content for root advancement {}", rootAdvancement);
        return;
      }
      for (AdvancementEntry advancement : advancements) {
        String contentName = advancement.id.toString();
        this.addContent(contentName, new AdvancementContent(advancement, this.width),
            advancement == AdvancementsManager.getSelectedAdvancement());
      }
      if (this.advancementInfoPanel != null) {
        this.advancementInfoPanel.updateContent(AdvancementsManager.getSelectedAdvancement());
      }
      this.preRender();
    }

    @Override
    public void handleClick(ScrollPanelContent scrollPanelContent, int button) {
      AdvancementContent advancementContent = (AdvancementContent) scrollPanelContent;
      AdvancementEntry clickedAdvancement = advancementContent.getAdvancement();
      AdvancementsManager.setSelectedAdvancement(clickedAdvancement);
      if (this.advancementInfoPanel != null) {
        this.advancementInfoPanel.updateContent(AdvancementsManager.getSelectedAdvancement());
      }
    }

    public void setAdvancementInfoPanel(AdvancementInfoPanel advancementInfoPanel) {
      this.advancementInfoPanel = advancementInfoPanel;
    }
  }

  class AdvancementInfoPanel extends ScrollPanelManager {

    AdvancementInfoPanel(Minecraft minecraft, int width, int height, int top, int left, AdvancementEntry advancement) {
      super(minecraft, width, height, top, left);
      updateContent(advancement);
    }

    public void updateContent(AdvancementEntry advancement) {
      if (advancement == null) {
        log.error("Unable to get info for advancement {}", advancement);
        return;
      }
      this.clearContent();
      String contentName = advancement.id.toString();
      this.addContent(contentName, new AdvancementInfo(AdvancementsManager.getSelectedAdvancement(), this.width), true);
      this.preRender();
    }

  }

  @Override
  public void init() {
    // Defining Panel Layout
    int panelPadding = 5;
    int panelPaddingBottom = 25;
    int panelTop = 25;
    int panelWidth = (this.width - (panelPadding * 4)) / 3;
    int panelHeight = this.height - panelTop - panelPaddingBottom;

    // Define different scroll panels.
    this.advancementCategoryPanel = new AdvancementCategoryPanel(this.minecraft, panelWidth, panelHeight, panelTop,
        panelPadding);
    this.advancementContentPanel = new AdvancementContentPanel(this.minecraft, panelWidth, panelHeight, panelTop,
        panelPadding + (panelPadding + panelWidth) * 1, AdvancementsManager.getSelectedRootAdvancement());
    this.advancementInfoPanel = new AdvancementInfoPanel(this.minecraft, panelWidth, panelHeight, panelTop,
        panelPadding + (panelPadding + panelWidth) * 2, AdvancementsManager.getSelectedAdvancement());

    // Adding links between views
    this.advancementCategoryPanel.setAdvancementContentPanel(this.advancementContentPanel);
    this.advancementContentPanel.setAdvancementInfoPanel(this.advancementInfoPanel);

    // Adding views
    children.add(this.advancementCategoryPanel);
    children.add(this.advancementContentPanel);
    children.add(this.advancementInfoPanel);

    // Add Close Button
    this.addButton(new Button((this.width - 200) / 2, this.height - 22, 200, 20,
        new TranslationTextComponent("gui.done"), button -> this.closeScreen()));
  }

  public void closeScreen() {
    this.minecraft.setScreen((Screen) null);
  }

  @Override
  public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(matrixStack);
    drawCenteredString(matrixStack, this.font, this.title.getString(), this.width / 2, 10, 0xFFFFFF);
    this.advancementCategoryPanel.render(matrixStack, mouseX, mouseY, partialTicks);
    this.advancementContentPanel.render(matrixStack, mouseX, mouseY, partialTicks);
    this.advancementInfoPanel.render(matrixStack, mouseX, mouseY, partialTicks);
    super.render(matrixStack, mouseX, mouseY, partialTicks);
  }

}
