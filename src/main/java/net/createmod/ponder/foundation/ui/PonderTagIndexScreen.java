package net.createmod.ponder.foundation.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class PonderTagIndexScreen extends PonderCatalogScreen {
    @Override
    public void initGui() {
        buttonList.clear(); entries.clear();
        PonderCatalog catalog = PonderCatalog.current();
        List<PonderTag> themes = new ArrayList<>(catalog.themes.values());
        themes.sort(Comparator.comparing(PonderTag::getTitle, String.CASE_INSENSITIVE_ORDER));
        for (PonderTag tag : themes) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add(tag.getTitle());
            for (String line : fontRenderer.listFormattedStringToWidth(tag.getDescription(), Math.min(300, width - 32)))
                tooltip.add("\u00a77" + line);
            tooltip.add("\u00a78" + I18n.format("ponder.ui.catalog.items", catalog.filter(null, tag.getId(), false, "", id -> "").size()));
            ItemStack icon = tag.getItemIcon();
            if (icon.isEmpty()) icon = new ItemStack(Items.BOOK);
            entries.add(new GridEntry(tag.getId(), icon, tooltip, tag.getTitle(), () -> ScreenOpener.open(new PonderTagScreen(tag))));
        }
        layoutGrid(12, 48, width - 12, height - 44, Math.max(160, Math.min(220, (width - 50) / 2)), 52, 32);
        addButton(new PonderButton(1, 8, height - 29, 22, 20, PonderButton.Icon.INDEX)
            .withTooltip("ponder.ui.index_title").withCallback(() -> ScreenOpener.open(new PonderIndexScreen())));
        addCloseButton();
    }

    @Override
    protected void renderCatalogHeader(int mouseX, int mouseY, float partialTicks) {
        drawTitle(I18n.format("ponder.ui.catalog.theme_index") + "  (" + entries.size() + ")");
    }
}
