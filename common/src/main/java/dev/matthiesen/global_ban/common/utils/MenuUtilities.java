package dev.matthiesen.global_ban.common.utils;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.matthiesen_core.common.utility.item.ItemBuilder;
import dev.matthiesen.matthiesen_core.common.utility.player_data.ServerUser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public final class MenuUtilities {
    public static final Item BACKGROUND = Items.GRAY_STAINED_GLASS_PANE;
    public static final Item PAGE_PLACEHOLDER = Items.PAPER;
    public static final Item NAV_ITEM = Items.ARROW;
    public static final Item CLOSE_ITEM = Items.BARRIER;
    public static final Item PLAYER_RECORD_ITEM = Items.PLAYER_HEAD;
    public static final Item IP_RECORD_ITEM = Items.NAME_TAG;

    private static ItemStack builder(Item item, Component name) {
        return new ItemBuilder(item)
                .hideAdditional()
                .setCustomName(name)
                .build();
    }

    public static ItemStack getBackgroundItem() {
        return builder(BACKGROUND, Component.literal(" "));
    }

    public static ItemStack getPageItem(int page, int totalPages) {
        return builder(PAGE_PLACEHOLDER, Component.literal("Page " + page + " of " + totalPages));
    }

    public static ItemStack getNavNextItem() {
        return builder(NAV_ITEM, Component.literal("Next Page"));
    }

    public static ItemStack getNavPreviousItem() {
        return builder(NAV_ITEM, Component.literal("Previous Page"));
    }

    public static ItemStack getCloseItem() {
        return builder(CLOSE_ITEM, Component.literal("Close Menu"));
    }

    public static ItemStack getRecordItem(PunishmentRecord record) {
        return switch (record.type()) {
            case BAN -> getPlayerRecordItem(record);
            case IP_BAN -> getIpRecordItem(record);
            default -> throw new IllegalArgumentException("Unsupported punishment type: " + record.type());
        };
    }

    public static ItemStack getPlayerRecordItem(PunishmentRecord punishment) {
        ServerUser serverUser = new ServerUser(punishment.playerUuid());
        GameProfile gameProfile;
        if (serverUser.isOnline()) {
            gameProfile = serverUser.getOnlinePlayer().getGameProfile();
        } else {
            gameProfile = new GameProfile(serverUser.getUUID(), serverUser.getUsername());
        }
        ResolvableProfile profile = new ResolvableProfile(gameProfile);
        Component[] lore = new Component[]{
                Component.literal("Type: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.type().name()).setStyle(Style.EMPTY.withColor(ChatFormatting.RED))),
                Component.literal("Date: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.getFormattedDate()).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))),
                Component.literal("Expires: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.getFormattedExpirationDate()).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))),
                Component.literal("By: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.punisherDisplayName()).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))),
                Component.literal("Reason: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.reason()).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
        };
        return new ItemBuilder(PLAYER_RECORD_ITEM)
                .hideAdditional()
                .setCustomName(Component.literal(serverUser.getUsername()))
                .addLore(lore)
                .modifyStack(stack -> {
                    stack.set(DataComponents.PROFILE, profile);
                    return stack;
                })
                .build();
    }

    public static ItemStack getIpRecordItem(PunishmentRecord punishment) {
        Component[] lore = new Component[]{
                Component.literal("Type: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.type().name()).setStyle(Style.EMPTY.withColor(ChatFormatting.RED))),
                Component.literal("Date: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.getFormattedDate()).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))),
                Component.literal("Expires: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.getFormattedExpirationDate()).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))),
                Component.literal("By: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.punisherDisplayName()).setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN))),
                Component.literal("Reason: ").setStyle(Style.EMPTY.withBold(true))
                        .append(Component.literal(punishment.reason()).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)))
        };
        return new ItemBuilder(IP_RECORD_ITEM)
                .hideAdditional()
                .setCustomName(Component.literal(punishment.playerIp()))
                .addLore(lore)
                .build();
    }
}
