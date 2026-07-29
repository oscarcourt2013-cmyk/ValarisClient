package dev.stellarclient.core.event;

/**
 * Fired when a chat message is received or sent.
 *
 * @param text     plain text content
 * @param outgoing {@code true} when the local player sent the message
 */
public record ChatMessageEvent(String text, boolean outgoing) {
}
