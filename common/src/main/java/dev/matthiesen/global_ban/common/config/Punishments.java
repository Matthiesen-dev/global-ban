package dev.matthiesen.global_ban.common.config;

import com.electronwill.nightconfig.core.Config;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class Punishments {
    public ModConfigSpec.ConfigValue<List<? extends Config>> punished;
    public ModConfigSpec.ConfigValue<List<? extends Config>> punished_pendingSync;

    public Punishments(ModConfigSpec.Builder builder) {
        punished = builder
                .comment("List of punished accounts. Each entry is a serialized PunishmentRecord object.")
                .defineListAllowEmpty(
                        List.of("accounts"),
                        List.of(),
                        null,
                        obj -> obj instanceof Config config && PunishmentRecord.isValid(config)
                );
        punished_pendingSync = builder
                .comment("List of punished accounts that are pending sync. Each entry is a serialized PunishmentRecord object.")
                .defineListAllowEmpty(
                        List.of("accounts_pending_sync"),
                        List.of(),
                        null,
                        obj -> obj instanceof Config config && PunishmentRecord.Sync.isValid(config)
                );
    }
}
