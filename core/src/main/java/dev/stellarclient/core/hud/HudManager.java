package dev.stellarclient.core.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.config.ConfigBinding;
import dev.stellarclient.core.hud.vanilla.VanillaHudComponent;
import dev.stellarclient.core.hud.vanilla.VanillaHudMeasurements;
import dev.stellarclient.core.hud.vanilla.VanillaHudProxyElement;
import dev.stellarclient.core.hud.vanilla.VanillaHudTransformHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry and renderer of all {@link HudElement}s. */
public final class HudManager implements ConfigBinding {

    private final Map<String, HudElement> byId = new LinkedHashMap<>();
    private HudElement[] renderList = new HudElement[0];
    private JsonObject pendingConfig;

    public <E extends HudElement> E register(E element) {
        HudElement previous = byId.putIfAbsent(element.id(), element);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate HUD element id: " + element.id());
        }
        if (pendingConfig != null) {
            JsonElement section = pendingConfig.get(element.id());
            if (section != null && section.isJsonObject()) {
                readElement(element, section.getAsJsonObject());
            }
        }
        renderList = byId.values().toArray(new HudElement[0]);
        return element;
    }

    public HudElement get(String id) {
        return byId.get(id);
    }

    public Collection<HudElement> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public void render(RenderContext ctx) {
        render(ctx, true);
    }

    /**
     * @param doLayout when {@code false}, draws using bounds from a prior {@link #layout} call
     *                 (HUD editor lays out once with hidden elements, then renders).
     */
    public void render(RenderContext ctx, boolean doLayout) {
        if (doLayout) {
            layout(ctx, false);
        }
        long now = System.currentTimeMillis();
        HudElement[] elements = this.renderList;
        for (int i = 0; i < elements.length; i++) {
            HudElement element = elements[i];
            if (!element.isVisible()) {
                continue;
            }
            drawElement(ctx, element, now);
        }
    }

    /** Computes visible element bounds without drawing. */
    public void layout(RenderContext ctx) {
        layout(ctx, false);
    }

    /**
     * Computes element bounds without drawing.
     *
     * @param includeHidden when {@code true}, also measures hidden elements (HUD editor hit-testing)
     */
    public void layout(RenderContext ctx, boolean includeHidden) {
        int screenWidth = ctx.screenWidth();
        int screenHeight = ctx.screenHeight();
        HudElement[] elements = this.renderList;
        for (int i = 0; i < elements.length; i++) {
            HudElement element = elements[i];
            if (!includeHidden && !element.isVisible()) {
                continue;
            }
            float scale = element.scale();
            int localWidth = element.measureWidth(ctx);
            int localHeight = element.measureHeight(ctx);
            float width = localWidth * scale;
            float height = localHeight * scale;
            float x;
            float y;
            if (element instanceof VanillaHudProxyElement proxy) {
                VanillaHudComponent component = proxy.component();
                if (component == VanillaHudComponent.SCOREBOARD) {
                    VanillaHudMeasurements.Bounds bounds = VanillaHudMeasurements.scoreboard();
                    if (bounds.valid()) {
                        x = VanillaHudTransformHelper.targetX(element, component, screenWidth, width);
                        y = VanillaHudTransformHelper.targetY(element, component, screenHeight, height);
                        element.setLastBounds(x, y, width, height);
                        continue;
                    }
                }
            }
            x = element.anchor().baseX(screenWidth, width) + element.offsetX();
            y = element.anchor().baseY(screenHeight, height) + element.offsetY();
            element.setLastBounds(x, y, width, height);
        }
    }

    private static void drawElement(RenderContext ctx, HudElement element, long now) {
        float scale = element.scale();
        float width = element.lastWidth();
        float height = element.lastHeight();
        float localWidth = width / scale;
        float localHeight = height / scale;
        float x = element.lastX();
        float y = element.lastY();

        float pivotX = localWidth / 2f;
        float pivotY = localHeight / 2f;
        ctx.pushTransform(x + width / 2f, y + height / 2f, scale,
                element.rotation(), pivotX, pivotY);
        ctx.setDrawOpacity(element.opacity());
        element.render(ctx, now);
        ctx.setDrawOpacity(1f);
        ctx.popTransform();
    }

    public HudElement elementAt(double x, double y) {
        return elementAt(x, y, false);
    }

    /** Editor hit-test â€” includes hidden elements so they can be re-selected. */
    public HudElement elementAt(double x, double y, boolean includeHidden) {
        HudElement[] elements = this.renderList;
        for (int i = elements.length - 1; i >= 0; i--) {
            HudElement element = elements[i];
            if ((includeHidden || element.isVisible()) && element.containsPoint(x, y)) {
                return element;
            }
        }
        return null;
    }

    @Override
    public String configKey() {
        return "hud";
    }

    @Override
    public JsonElement saveConfig() {
        JsonObject json = new JsonObject();
        for (HudElement element : byId.values()) {
            JsonObject section = new JsonObject();
            section.addProperty("anchor", element.anchor().name());
            section.addProperty("x", element.offsetX());
            section.addProperty("y", element.offsetY());
            section.addProperty("scale", element.scale());
            section.addProperty("rotation", element.rotation());
            section.addProperty("opacity", element.opacity());
            section.addProperty("visible", element.isVisible());
            if (element.tintArgb() != 0) {
                section.addProperty("tint", element.tintArgb());
            }
            json.add(element.id(), section);
        }
        return json;
    }

    @Override
    public void loadConfig(JsonElement element) {
        JsonObject json = element.getAsJsonObject();
        this.pendingConfig = json;
        for (HudElement hudElement : byId.values()) {
            JsonElement section = json.get(hudElement.id());
            if (section != null && section.isJsonObject()) {
                readElement(hudElement, section.getAsJsonObject());
            }
        }
    }

    private static void readElement(HudElement element, JsonObject json) {
        HudAnchor anchor = element.anchor();
        JsonElement anchorJson = json.get("anchor");
        if (anchorJson != null && anchorJson.isJsonPrimitive()) {
            try {
                anchor = HudAnchor.valueOf(anchorJson.getAsString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        float x = json.has("x") ? json.get("x").getAsFloat() : element.offsetX();
        float y = json.has("y") ? json.get("y").getAsFloat() : element.offsetY();
        element.setLayout(anchor, x, y);
        if (json.has("scale")) {
            element.setScale(json.get("scale").getAsFloat());
        }
        if (json.has("rotation")) {
            element.setRotation(json.get("rotation").getAsFloat());
        }
        if (json.has("opacity")) {
            element.setOpacity(json.get("opacity").getAsFloat());
        }
        if (json.has("visible")) {
            element.setVisible(json.get("visible").getAsBoolean());
        }
        if (json.has("tint")) {
            element.setTintArgb(json.get("tint").getAsInt());
        }
    }
}
