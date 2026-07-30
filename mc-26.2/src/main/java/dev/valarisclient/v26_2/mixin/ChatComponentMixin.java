package dev.valarisclient.v26_2.mixin;

import dev.valarisclient.core.hook.ValarisHooks;
import dev.valarisclient.core.stream.StreamRedactor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Component ValarisClient$redactMessageContents(Component contents) {
        return redactComponent(contents);
    }

    @ModifyVariable(
            method = "addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private GuiMessage ValarisClient$redactQueuedMessage(GuiMessage message) {
        return redactMessage(message);
    }

    @ModifyVariable(
            method = "addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private GuiMessage ValarisClient$redactDisplayMessage(GuiMessage message) {
        return redactMessage(message);
    }

    private static Component redactComponent(Component message) {
        if (message == null || !ValarisHooks.streamChatRedact()) {
            return message;
        }
        String plain = message.getString();
        String redacted = StreamRedactor.redactComponent(plain);
        if (redacted.equals(plain)) {
            return message;
        }
        return Component.literal(redacted).withStyle(message.getStyle());
    }

    private static GuiMessage redactMessage(GuiMessage message) {
        if (!ValarisHooks.streamChatRedact() || message == null) {
            return message;
        }
        Component content = message.content();
        Component redacted = redactComponent(content);
        if (redacted == content) {
            return message;
        }
        return new GuiMessage(message.addedTime(), redacted, message.signature(), message.source(), message.tag());
    }
}
