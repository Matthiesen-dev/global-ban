package dev.matthiesen.global_ban.common.menu;

import dev.matthiesen.global_ban.common.registry.PermissionRegistry;
import dev.matthiesen.global_ban.common.utils.MenuUtilities;
import dev.matthiesen.matthiesen_core.common.api.permissions.Permission;
import dev.matthiesen.matthiesen_core.common.api.server_menu.ServerUIManager;
import dev.matthiesen.matthiesen_core.common.api.server_menu.button.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.button.linked.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.helpers.PaginationHelper;
import dev.matthiesen.matthiesen_core.common.api.server_menu.page.*;
import dev.matthiesen.matthiesen_core.common.api.server_menu.template.slot.TemplateSlotDelegate;
import dev.matthiesen.matthiesen_core.common.api.server_menu.template.types.ChestTemplate;
import dev.matthiesen.matthiesen_core.common.utility.SoundsPlayer;
import net.minecraft.ChatFormatting;
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

    protected abstract Permission requiredPermission();

    protected abstract Component getDisplayTitle();

    protected abstract List<Button> getContentButtons();

    private Page getPage() {
        Button frame = ServerButton.builder()
                .display(MenuUtilities.getBackgroundItem())
                .build();

        LinkedPageButton previous = LinkedPageButton.builder()
                .display(MenuUtilities.getNavPreviousItem())
                .linkType(LinkType.Previous)
                .onClick(() -> new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(player))
                .build();

        LinkedPageButton next = LinkedPageButton.builder()
                .display(MenuUtilities.getNavNextItem())
                .linkType(LinkType.Next)
                .onClick(() -> new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(player))
                .build();

        Button close = ServerButton.builder()
                .display(MenuUtilities.getCloseItem())
                .onClick(() -> {
                    ServerUIManager.closeUI(player);
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
        if (!PermissionRegistry.checkPermission(player, requiredPermission())) {
            player.sendSystemMessage(Component.literal("You do not have permission to view this menu.").withStyle(ChatFormatting.RED));
            return;
        }
        ServerUIManager.openUIForcefully(player, getPage());
        new SoundsPlayer(SoundEvents.UI_BUTTON_CLICK.value()).play(player);
    }

    private Button getInfoButton(int currentPage, int totalPages) {
        return ServerButton.builder()
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
