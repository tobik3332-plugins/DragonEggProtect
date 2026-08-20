package cz.craft.dragonegg;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {

    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    public static Component parse(String input) {
        if (input == null) return Component.empty();
        if (input.contains("<") && input.contains(">")) {
            return mm.deserialize(input);
        }
        return legacy.deserialize(input.replace("§", "&"));
    }
}
