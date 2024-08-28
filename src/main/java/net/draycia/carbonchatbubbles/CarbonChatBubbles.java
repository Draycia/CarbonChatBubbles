package net.draycia.carbonchatbubbles;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.event.events.CarbonChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
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

    private HashMap<UUID, UUID> displayCache = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        CarbonChatProvider.carbonChat().eventHandler().subscribe(CarbonChatEvent.class, 1000, false, event -> {
            String content = PlainTextComponentSerializer.plainText().serialize(event.message());

            // Not sure if necessary, I just fixed this but content was still being wrapped?
            final String wrappedContent = WordUtils.wrap(content, this.getConfig().getInt("word-wrap-length"), "\n", false);

            Bukkit.getScheduler().runTask(this, () -> {
                final Player player = Objects.requireNonNull(Bukkit.getPlayer(event.sender().uuid()));

                final TextDisplay display = (TextDisplay) player.getWorld().spawnEntity(player.getEyeLocation().add(0, 1, 0), EntityType.TEXT_DISPLAY);

                display.setDefaultBackground(false);
                display.setBackgroundColor(this.getConfig().getColor("background-color"));
                display.setShadowed(false);
                display.setSeeThrough(true);
                display.setBillboard(Display.Billboard.CENTER);
                display.text(Component.text(wrappedContent, TextColor.color(this.getConfig().getColor("text-color").asRGB())));
                display.getTransformation().getTranslation().add(0f, 0.5f, 0f);
                display.setInterpolationDuration(0);
                display.setInterpolationDelay(0);

                player.addPassenger(display);

                display.setTransformation(new Transformation(
                    new Vector3f(0, 0.5f, 0), // offset
                    new AxisAngle4f(0, 0, 0, 0), // left rotation
                    display.getTransformation().getScale(),
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
