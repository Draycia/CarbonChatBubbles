package net.draycia.carbonchatbubbles;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.event.events.CarbonChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class CarbonChatBubbles extends JavaPlugin {

    private final HashMap<UUID, UUID> displayCache = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        CarbonChatProvider.carbonChat().eventHandler().subscribe(CarbonChatEvent.class, 1000, false, event -> {
            String content = PlainTextComponentSerializer.plainText().serialize(event.message());

            // Not sure if necessary, I just fixed this but content was still being wrapped?
            final String wrappedContent = WordUtils.wrap(content, this.getConfig().getInt("word-wrap-length"), "\n", false);

            Bukkit.getScheduler().runTask(this, () -> {
                final Player player = Bukkit.getPlayer(event.sender().uuid());
                if (player == null) {
                    return;
                }

                if (displayCache.containsKey(player.getUniqueId())) {
                    TextDisplay oldDisplay = (TextDisplay) Bukkit.getEntity(displayCache.get(player.getUniqueId()));
                    if (oldDisplay != null) {
                        oldDisplay.remove();
                    }
                }

                final TextDisplay display = (TextDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(0, 1, 0), EntityType.TEXT_DISPLAY);

                display.setDefaultBackground(false);

                String backgroundColorName = this.getConfig().getString("background-color").toLowerCase();
                TextColor backgroundColor = NamedTextColor.NAMES.value(backgroundColorName) != null
                        ? NamedTextColor.NAMES.value(backgroundColorName)
                        : TextColor.fromHexString(backgroundColorName);

                display.setBackgroundColor(backgroundColor != null ? Color.fromRGB(backgroundColor.value()) : Color.WHITE);

                display.setShadowed(this.getConfig().getBoolean("shadowed", false));
                display.setSeeThrough(this.getConfig().getBoolean("see-through", false));

                String billboard = this.getConfig().getString("billboard-style", "CENTER");
                try {
                    display.setBillboard(Display.Billboard.valueOf(billboard));
                } catch (IllegalArgumentException ignored) {
                    this.getLogger().warning("Invalid billboard type [" + billboard + ", allowed types " + Arrays.toString(Display.Billboard.values()));
                    display.setBillboard(Display.Billboard.CENTER);
                }

                String textColorName = this.getConfig().getString("text-color", "BLACK").toLowerCase();
                TextColor textColor = NamedTextColor.NAMES.value(textColorName) != null
                        ? NamedTextColor.NAMES.value(textColorName)
                        : TextColor.fromHexString(textColorName);

                display.text(Component.text(wrappedContent, textColor != null ? textColor : NamedTextColor.BLACK));

                display.getTransformation().getTranslation().add(
                        (float) this.getConfig().getDouble("offsets.x", 0.0f),
                        (float) this.getConfig().getDouble("offsets.y", 0.5f),
                        (float) this.getConfig().getDouble("offsets.z", 0.0f)
                );
                display.setInterpolationDuration(0);
                display.setInterpolationDelay(0);

                player.addPassenger(display);

                display.setTransformation(new Transformation(
                    new Vector3f(
                            (float) this.getConfig().getDouble("offsets.x", 0.0f),
                            (float) this.getConfig().getDouble("offsets.y", 0.5f),
                            (float) this.getConfig().getDouble("offsets.z", 0.0f)
                    ), // offset
                    new AxisAngle4f(0, 0, 0, 0), // left rotation
                    display.getTransformation().getScale().mul((float) this.getConfig().getDouble("scale", 1.0f)),
                    new AxisAngle4f(0, 0, 0, 0) // right rotation
                ));

                this.displayCache.put(player.getUniqueId(), display.getUniqueId());

                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    display.remove();
                    this.displayCache.remove(player.getUniqueId(), display.getUniqueId());
                }, 20 * this.getConfig().getLong("message-duration"));
            });

        });
    }

    @Override
    public void onDisable() {
        this.displayCache.forEach((key, value) -> {
            final Entity entity = Bukkit.getEntity(value);

            if (entity != null) {
                entity.remove();
            }
        });
    }

}
