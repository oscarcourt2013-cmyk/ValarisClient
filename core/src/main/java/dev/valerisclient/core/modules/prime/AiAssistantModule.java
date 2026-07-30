package dev.valerisclient.core.modules.prime;

import dev.valerisclient.core.ai.AiAssistantService;
import dev.valerisclient.core.event.ClientTickEvent;
import dev.valerisclient.core.module.BooleanSetting;
import dev.valerisclient.core.module.EnumSetting;
import dev.valerisclient.core.module.Module;
import dev.valerisclient.core.module.ModuleCategory;

/** In-game Groq assistant — {@code /ai} for module help and build advice. */
public final class AiAssistantModule extends Module {

    private final AiAssistantService ai;
    private final EnumSetting<AiAssistantService.Model> model =
            addSetting(new EnumSetting<>("model", "Model",
                    "Fast = 8B instant, Quality = 70B versatile",
                    AiAssistantService.Model.FAST));
    private final BooleanSetting includeContext =
            addSetting(new BooleanSetting("include-context", "Client context",
                    "Send enabled modules, server, FPS to the AI", true));
    private final BooleanSetting french =
            addSetting(new BooleanSetting("french", "French replies",
                    "Prefer French answers", true));

    public AiAssistantModule(AiAssistantService ai) {
        super("ai-assistant", "AI Assistant",
                "Ask /ai for module help, packs, FPS tips (Groq)",
                ModuleCategory.PRIME);
        this.ai = ai;
        listen(ClientTickEvent.class, event -> applySettings());
        applySettings();
    }

    @Override
    protected void onEnable() {
        applySettings();
    }

    @Override
    protected void onDisable() {
        applySettings();
    }

    private void applySettings() {
        ai.setEnabled(isEnabled());
        ai.setModel(model.get());
        ai.setIncludeContext(includeContext.get());
        ai.setFrench(french.get());
    }
}
