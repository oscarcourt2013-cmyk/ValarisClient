package dev.valerisclient.v1_21_11.mixin;

import dev.valerisclient.core.hook.ValerisHooks;
import dev.valerisclient.core.stream.StreamRedactor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Component ValerisClient$redactSimpleMessage(Component message) {
        return redact(message);
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Component ValerisClient$redactSignedMessage(Component message) {
        return redact(message);
    }

    @ModifyVariable(
            method = "addMessageToQueue(Lnet/minecraft/client/GuiMessage;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private GuiMessage ValerisClient$redactQueuedMessage(GuiMessage message) {
        return redact(message);
    }

    @ModifyVariable(
            method = "addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private GuiMessage ValerisClient$redactDisplayMessage(GuiMessage message) {
        return redact(message);
    }

    private static Component redact(Component message) {
        if (message == null) {
            return null;
        }
        if (!ValerisHooks.streamChatRedact()) {
            return message;
        }
        String plain = message.getString();
        String redacted = StreamRedactor.redactComponent(plain);
        if (redacted.equals(plain)) {
            return message;
        }
        return Component.literal(redacted).withStyle(message.getStyle());
    }

    private static GuiMessage redact(GuiMessage message) {
        if (!ValerisHooks.streamChatRedact() || message == null) {
            return message;
        }
        Component content = message.content();
        Component redacted = redact(content);
        if (redacted == content) {
            return message;
        }
        return new GuiMessage(message.addedTime(), redacted, message.signature(), message.tag());
    }
}
