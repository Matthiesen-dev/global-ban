package dev.matthiesen.global_ban.common.utils;

import com.mojang.authlib.GameProfile;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import dev.matthiesen.matthiesen_core.common.utility.item.ItemBuilder;
import dev.matthiesen.matthiesen_core.common.utility.player_data.ServerUser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
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
    public static final Item UNPUNISH_ITEM = Items.GREEN_DYE;
    public static final Item PUNISH_ITEM = Items.RED_DYE;
    public static final Item PARDON_ITEM = Items.GREEN_DYE;
    public static final Item DURATION_ITEM = Items.CLOCK;
    public static final Item REASON_ITEM = Items.PAPER;

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

    public static ItemStack getCancelItem() {
        return builder(CLOSE_ITEM, Component.literal("Cancel"));
    }

    public static ItemStack getUnpunishItem(String playerNameOrIp) {
        return builder(UNPUNISH_ITEM, Component.literal("Unpunish " + playerNameOrIp));
    }

    public static ItemStack getPardonItem(String playerNameOrIp) {
        return builder(PARDON_ITEM, Component.literal("Pardon " + playerNameOrIp));
    }

    public static ItemStack getPunishItem(String playerNameOrIp) {
        return builder(PUNISH_ITEM, Component.literal("Punish " + playerNameOrIp));
    }

    public static ItemStack getDurationItem(String duration) {
        return builder(DURATION_ITEM, Component.literal("Duration: " + duration));
    }

    public static ItemStack getReasonItem(String reason) {
        return builder(REASON_ITEM, Component.literal("Reason: " + reason));
    }

    public static ItemStack getRecordItem(PunishmentRecord record) {
        ItemBuilder base = getRecordItemBuilder(record);
        return switch (record.type()) {
            case BAN -> base.modifyStack(stack -> {
                stack.set(DataComponents.PROFILE, getResolvableProfile(record));
                return stack;
            }).build();
            case IP_BAN -> base.build();
            default -> throw new IllegalArgumentException("Unsupported punishment type: " + record.type());
        };
    }

    public static ItemStack getPlayerHeadItem(ServerPlayer player) {
        ItemBuilder builder = new ItemBuilder(Items.PLAYER_HEAD)
                .hideAdditional()
                .setCustomName(player.getName());
        GameProfile profile = player.getGameProfile();
        builder.modifyStack(stack -> {
            stack.set(DataComponents.PROFILE, new ResolvableProfile(profile));
            return stack;
        });
        return builder.build();
    }

    public static ItemBuilder getRecordItemBuilder(PunishmentRecord record) {
        Item item;
        String name;
        Component[] lore = getRecordLore(record);
        switch (record.type()) {
            case BAN -> {
                item = PLAYER_RECORD_ITEM;
                String username = new ServerUser(record.playerUuid()).getUsername();
                name = username != null ? username : record.playerDisplayName();
            }
            case IP_BAN -> {
                item = IP_RECORD_ITEM;
                name = record.playerIp();
            }
            default -> throw new IllegalArgumentException("Unsupported punishment type: " + record.type());
        }
        if (name == null) name = PunishmentRecord.UNKNOWN_PLAYER_NAME;
        return new ItemBuilder(item).hideAdditional().setCustomName(Component.literal(name)).addLore(lore);
    }

    private static Component[] getRecordLore(PunishmentRecord punishment) {
        return new Component[]{
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
    }

    private static ResolvableProfile getResolvableProfile(PunishmentRecord punishment) {
        ServerUser serverUser = new ServerUser(punishment.playerUuid());
        GameProfile gameProfile;
        if (serverUser.isOnline()) {
            gameProfile = serverUser.getOnlinePlayer().getGameProfile();
        } else {
            gameProfile = new GameProfile(punishment.playerUuid(), punishment.playerDisplayName());
        }
        return new ResolvableProfile(gameProfile);
    }
}
