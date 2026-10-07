package dev.matthiesen.global_ban.common.config;

import com.electronwill.nightconfig.core.Config;
import dev.matthiesen.global_ban.common.def.PunishmentRecord;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class Punishments {
    public ModConfigSpec.ConfigValue<List<? extends Config>> punished;

    public Punishments(ModConfigSpec.Builder builder) {
        punished = builder
                .comment("List of punished accounts. Each entry is a serialized PunishmentRecord object.")
                .defineListAllowEmpty(
                        List.of("accounts"),
                        List.of(),
                        null,
                        obj -> obj instanceof Config config && PunishmentRecord.isValid(config)
                );
    }
}
