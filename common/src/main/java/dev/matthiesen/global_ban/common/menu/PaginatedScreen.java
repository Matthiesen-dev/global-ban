package dev.matthiesen.global_ban.common.menu;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.PlaceholderButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.page.Page;
import ca.landonjw.gooeylibs2.api.template.slot.TemplateSlotDelegate;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import dev.matthiesen.matthiesen_core.common.utility.SoundsPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

public abstract class PaginatedScreen {
    private static final int PREVIOUS_SLOT = 45;
    private static final int CLOSE_SLOT = 47;
    private static final int INFO_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    protected final ServerPlayer player;

    protected PaginatedScreen(ServerPlayer player) {
        this.player = player;
    }

    public abstract Component getDisplayTitle();

    public abstract List<Button> getContentButtons();

    public Page getPage() {
        Button frame = GooeyButton.builder()
                .display(MenuUtilities.getBackgroundItem())
                .build();

        LinkedPageButton previous = LinkedPageButton.builder()
                .display(MenuUtilities.getNavPreviousItem())
                .linkType(LinkType.Previous)
                .build();

        LinkedPageButton next = LinkedPageButton.builder()
                .display(MenuUtilities.getNavNextItem())
                .linkType(LinkType.Next)
                .build();

        Button close = GooeyButton.builder()
                .display(MenuUtilities.getCloseItem())
                .onClick(() -> {
                    UIManager.closeUI(player);
                    new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(player);
                })
                .build();

        ChestTemplate.Builder templateBuilder = ChestTemplate.builder(6)
                .rectangle(0, 0, 5, 9, new PlaceholderButton())
                .set(PREVIOUS_SLOT, previous)
                .set(CLOSE_SLOT, close)
                .set(INFO_SLOT, getInfoButton(1, 1))
                .set(NEXT_SLOT, next);

        ChestTemplate template = templateBuilder.fill(frame).build();
        LinkedPage page = PaginationHelper.createPagesFromPlaceholders(template, getContentButtons(), null);
        decoratePages(page);
        return page;
    }

    public void open() {
        UIManager.openUIForcefully(player, getPage());
    }

    private Button getInfoButton(int currentPage, int totalPages) {
        return GooeyButton.builder()
                .display(MenuUtilities.getPageItem(currentPage, totalPages))
                .build();
    }

    private void decoratePages(LinkedPage first) {
        int totalPages = Math.max(1, first.getTotalPages());
        Component title = getDisplayTitle();
        for (LinkedPage page = first; page != null; page = page.getNext()) {
            page.setTitle(title);
            page.getTemplate().setSlot(INFO_SLOT,
                    new TemplateSlotDelegate(getInfoButton(Math.max(1, page.getCurrentPage()), totalPages), INFO_SLOT));
        }
    }
}
