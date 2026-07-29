package dev.stellarclient.core.gui.social;

import dev.stellarclient.core.adapter.MinecraftAdapter;
import dev.stellarclient.core.adapter.RenderContext;
import dev.stellarclient.core.design.PrimeDesign;
import dev.stellarclient.core.gui.GuiLayout;
import dev.stellarclient.core.gui.UiChrome;
import dev.stellarclient.core.social.SocialClient;
import dev.stellarclient.core.social.SocialService;
import dev.stellarclient.core.theme.Theme;
import dev.stellarclient.core.util.ColorUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * In-game social overlay â€” Friends / Chat / Party tabs.
 * Same backend as the launcher.
 */
public final class SocialHubUi {

    private enum Tab { FRIENDS, CHAT, PARTY }

    private static final int PANEL_W = 440;
    private static final int ROW_H = 36;
    private static final int PAD = 14;
    private static final int ACTION_W = 56;

    private final SocialService social;
    private final MinecraftAdapter adapter;
    private Tab tab = Tab.FRIENDS;
    private String status = "";
    private boolean statusError;
    private long lastRefreshMs;
    private int scroll;
    private String draftAdd = "";
    private String draftChat = "";
    private String activeConversationId;
    private String activeChatName = "";
    private List<SocialClient.Conversation> conversations = List.of();
    private List<SocialClient.ChatMessage> chatMessages = List.of();
    private boolean loadingChat;

    public SocialHubUi(SocialService social, MinecraftAdapter adapter) {
        this.social = social;
        this.adapter = adapter;
    }

    public void onOpen() {
        scroll = 0;
        status = "";
        statusError = false;
        refresh(true);
        reloadConversations();
    }

    public void refresh(boolean forceConnect) {
        lastRefreshMs = System.currentTimeMillis();
        if (forceConnect || !social.client().connected()) {
            social.ensureConnected();
        }
        if (social.client().connected()) {
            social.client().refreshFriends();
            CompletableFuture.runAsync(() -> social.client().refreshParty());
        }
    }

    private void reloadConversations() {
        CompletableFuture.runAsync(() -> {
            conversations = social.client().listConversations();
        });
    }

    private void openChatWith(String friendUuid, String friendName) {
        loadingChat = true;
        activeChatName = friendName;
        CompletableFuture.runAsync(() -> {
            String id = social.client().openDm(friendUuid);
            if (id != null) {
                activeConversationId = id;
                chatMessages = social.client().loadMessages(id);
                reloadConversations();
            }
            loadingChat = false;
        });
    }

    public void render(RenderContext ctx, Theme theme, double mouseX, double mouseY) {
        if (System.currentTimeMillis() - lastRefreshMs > 8_000) {
            refresh(false);
        }

        int sw = ctx.screenWidth();
        int sh = ctx.screenHeight();
        int panelH = Math.min(500, sh - 28);
        int x = (sw - PANEL_W) / 2;
        int y = (sh - panelH) / 2;

        ctx.fillRect(0, 0, sw, sh, 0x88000000);
        UiChrome.glassPanel(ctx, theme, x, y, PANEL_W, panelH);

        ctx.drawSmoothText("Social", x + PAD, y + 12, theme.foreground(), 1.12f);
        drawStatusPill(ctx, theme, x, y);

        boolean refreshHover = hit(mouseX, mouseY, x + PANEL_W - PAD - 64, y + 10, 64, 18);
        UiChrome.button(ctx, theme, x + PANEL_W - PAD - 64, y + 10, 64, 18, refreshHover, false);
        ctx.drawSmoothText("Refresh", x + PANEL_W - PAD - 52, y + 14, theme.foreground(), 0.78f);

        int tabY = y + 36;
        drawTab(ctx, theme, x + PAD, tabY, "Friends", tab == Tab.FRIENDS, mouseX, mouseY);
        drawTab(ctx, theme, x + PAD + 78, tabY, "Chat", tab == Tab.CHAT, mouseX, mouseY);
        drawTab(ctx, theme, x + PAD + 148, tabY, "Party", tab == Tab.PARTY, mouseX, mouseY);

        int contentTop = tabY + 24;
        int contentBottom = y + panelH - 26;
        ctx.pushClip(x + 4, contentTop, PANEL_W - 8, contentBottom - contentTop);
        switch (tab) {
            case FRIENDS -> renderFriends(ctx, theme, x, contentTop, contentBottom, mouseX, mouseY);
            case CHAT -> renderChat(ctx, theme, x, contentTop, contentBottom, mouseX, mouseY);
            case PARTY -> renderParty(ctx, theme, x, contentTop, contentBottom, mouseX, mouseY);
        }
        ctx.popClip();

        String footer = !status.isBlank()
                ? status
                : "Esc close  Â·  Same friends & chat as the launcher";
        ctx.drawSmoothText(footer, x + PAD, y + panelH - 18,
                statusError ? 0xFFFF6B6B : theme.foregroundMuted(), 0.72f);
    }

    private void renderFriends(RenderContext ctx, Theme theme, int x, int top, int bottom,
                               double mouseX, double mouseY) {
        int cursorY = top;
        String joinAddr = resolveJoinAddress();
        if (joinAddr != null && !joinAddr.isBlank()) {
            boolean hover = hit(mouseX, mouseY, x + PAD, cursorY, PANEL_W - PAD * 2, 34);
            UiChrome.card(ctx, theme, x + PAD, cursorY, PANEL_W - PAD * 2, 34, hover);
            ctx.drawSmoothText("Party server", x + PAD + 10, cursorY + 6, theme.foregroundMuted(), 0.7f);
            ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, joinAddr, PANEL_W - 120),
                    x + PAD + 10, cursorY + 18, theme.foreground(), 0.85f);
            cursorY += 42;
        }

        // Add friend field
        ctx.fillRoundedRect(x + PAD, cursorY, PANEL_W - PAD * 2 - 70, 22, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(theme.background(), 0.55f));
        String addText = draftAdd.isEmpty()
                ? "Add friend by usernameâ€¦"
                : draftAdd + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        ctx.drawSmoothText(addText, x + PAD + 8, cursorY + 6,
                draftAdd.isEmpty() ? theme.foregroundMuted() : theme.foreground(), 0.8f);
        boolean addHover = hit(mouseX, mouseY, x + PANEL_W - PAD - 64, cursorY, 64, 22);
        UiChrome.button(ctx, theme, x + PANEL_W - PAD - 64, cursorY, 64, 22, addHover, true);
        ctx.drawSmoothText("Add", x + PANEL_W - PAD - 46, cursorY + 6, 0xFFFFFFFF, 0.8f);
        cursorY += 30;

        List<SocialClient.Friend> friends = sortedFriends();
        int listTop = cursorY;
        int listH = bottom - listTop;
        if (friends.isEmpty()) {
            UiChrome.cardLite(ctx, theme, x + PAD, listTop, PANEL_W - PAD * 2, 70, false);
            boolean connected = social.client().connected();
            ctx.drawSmoothText(connected ? "No friends yet" : "Not connected",
                    x + PAD + 12, listTop + 16, theme.foreground(), 0.92f);
            ctx.drawSmoothText(connected
                            ? "Add a friend above â€” same list as the launcher"
                            : "Tap Refresh to connect",
                    x + PAD + 12, listTop + 34, theme.foregroundMuted(), 0.75f);
            return;
        }

        int i = 0;
        for (SocialClient.Friend f : friends) {
            int rowY = listTop + (i - scroll) * (ROW_H + 4);
            i++;
            if (rowY + ROW_H < listTop || rowY > listTop + listH) {
                continue;
            }
            boolean hover = hit(mouseX, mouseY, x + PAD, rowY, PANEL_W - PAD * 2, ROW_H);
            UiChrome.cardLite(ctx, theme, x + PAD, rowY, PANEL_W - PAD * 2, ROW_H, hover);
            ctx.fillRoundedRect(x + PAD + 8, rowY + 14, 8, 8, 4, statusColor(theme, f.status()));
            ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, f.username(), 110),
                    x + PAD + 22, rowY + 6, theme.foreground(), 0.86f);
            ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, formatMeta(f), PANEL_W - 200),
                    x + PAD + 22, rowY + 20, theme.foregroundMuted(), 0.7f);

            int btnX = x + PANEL_W - PAD - 10 - ACTION_W;
            if (f.pending() && f.incoming()) {
                drawAction(ctx, theme, btnX, rowY + 9, ACTION_W, "Accept", mouseX, mouseY);
            } else if (!f.pending()) {
                drawAction(ctx, theme, btnX - ACTION_W - 4, rowY + 9, ACTION_W, "Chat", mouseX, mouseY);
                drawAction(ctx, theme, btnX, rowY + 9, ACTION_W, "Invite", mouseX, mouseY);
            }
        }
    }

    private void renderChat(RenderContext ctx, Theme theme, int x, int top, int bottom,
                            double mouseX, double mouseY) {
        int listW = 118;
        int listX = x + PAD;
        int chatX = listX + listW + 8;
        int chatW = PANEL_W - PAD * 2 - listW - 8;

        UiChrome.cardLite(ctx, theme, listX, top, listW, bottom - top - 30, false);
        int row = 0;
        List<SocialClient.Friend> chatFriends = sortedFriends().stream()
                .filter(f -> !f.pending())
                .toList();
        for (SocialClient.Friend f : chatFriends) {
            int ry = top + 4 + row * 22;
            row++;
            if (ry > bottom - 40) break;
            boolean sel = activeChatName.equals(f.username());
            boolean hover = hit(mouseX, mouseY, listX + 2, ry, listW - 4, 20);
            if (sel || hover) {
                ctx.fillRoundedRect(listX + 2, ry, listW - 4, 20, 4,
                        ColorUtil.withAlpha(sel ? theme.accent() : theme.backgroundLight(), sel ? 0.35f : 0.5f));
            }
            ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, f.username(), listW - 12),
                    listX + 8, ry + 5, theme.foreground(), 0.78f);
        }

        UiChrome.cardLite(ctx, theme, chatX, top, chatW, bottom - top - 30, false);
        if (activeConversationId == null) {
            ctx.drawSmoothText("Select a friend to chat", chatX + 12, top + 16, theme.foregroundMuted(), 0.85f);
        } else {
            ctx.drawSmoothText(activeChatName, chatX + 10, top + 8, theme.foreground(), 0.9f);
            List<SocialClient.ChatMessage> msgs = social.client().cachedMessages(activeConversationId);
            if (msgs.isEmpty()) {
                msgs = chatMessages;
            }
            int my = top + 26;
            int start = Math.max(0, msgs.size() - 10);
            for (int i = start; i < msgs.size(); i++) {
                SocialClient.ChatMessage m = msgs.get(i);
                boolean mine = m.senderUuid().equalsIgnoreCase(social.client().selfUuid());
                String line = (mine ? "You: " : m.senderUsername() + ": ") + m.text();
                ctx.drawSmoothText(GuiLayout.trimToWidth(ctx, line, chatW - 16),
                        chatX + 8, my, mine ? theme.accent() : theme.foreground(), 0.78f);
                my += 14;
                if (my > bottom - 56) break;
            }
        }

        int fieldY = bottom - 26;
        ctx.fillRoundedRect(chatX, fieldY, chatW - 70, 22, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(theme.background(), 0.55f));
        String field = draftChat.isEmpty()
                ? "Messageâ€¦"
                : draftChat + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        ctx.drawSmoothText(field, chatX + 8, fieldY + 6,
                draftChat.isEmpty() ? theme.foregroundMuted() : theme.foreground(), 0.8f);
        boolean sendHover = hit(mouseX, mouseY, chatX + chatW - 64, fieldY, 64, 22);
        UiChrome.button(ctx, theme, chatX + chatW - 64, fieldY, 64, 22, sendHover, true);
        ctx.drawSmoothText("Send", chatX + chatW - 46, fieldY + 6, 0xFFFFFFFF, 0.8f);
    }

    private void renderParty(RenderContext ctx, Theme theme, int x, int top, int bottom,
                             double mouseX, double mouseY) {
        int cursorY = top;
        SocialClient.Party party = social.client().party();
        List<SocialClient.PartyInvite> invites = social.client().partyInvites();

        if (!invites.isEmpty()) {
            ctx.drawSmoothText("INVITES", x + PAD, cursorY, theme.foregroundMuted(), 0.72f);
            cursorY += 14;
            for (SocialClient.PartyInvite inv : invites) {
                UiChrome.cardLite(ctx, theme, x + PAD, cursorY, PANEL_W - PAD * 2, 34, false);
                ctx.drawSmoothText(inv.fromUsername() + " invited you",
                        x + PAD + 10, cursorY + 6, theme.foreground(), 0.85f);
                drawAction(ctx, theme, x + PANEL_W - PAD - 10 - ACTION_W * 2 - 4, cursorY + 8,
                        ACTION_W, "Accept", mouseX, mouseY);
                drawAction(ctx, theme, x + PANEL_W - PAD - 10 - ACTION_W, cursorY + 8,
                        ACTION_W, "Decline", mouseX, mouseY);
                cursorY += 40;
            }
        }

        if (party == null) {
            UiChrome.cardLite(ctx, theme, x + PAD, cursorY, PANEL_W - PAD * 2, 56, false);
            ctx.drawSmoothText("Not in a party", x + PAD + 12, cursorY + 12, theme.foreground(), 0.9f);
            boolean createHover = hit(mouseX, mouseY, x + PAD + 12, cursorY + 30, 120, 18);
            UiChrome.button(ctx, theme, x + PAD + 12, cursorY + 30, 120, 18, createHover, true);
            ctx.drawSmoothText("Create party", x + PAD + 28, cursorY + 34, 0xFFFFFFFF, 0.78f);
            return;
        }

        boolean leaveHover = hit(mouseX, mouseY, x + PAD, cursorY, 88, 18);
        UiChrome.button(ctx, theme, x + PAD, cursorY, 88, 18, leaveHover, false);
        ctx.drawSmoothText("Leave", x + PAD + 28, cursorY + 4, theme.foreground(), 0.78f);
        ctx.drawSmoothText(party.members().size() + " members",
                x + PAD + 100, cursorY + 4, theme.foregroundMuted(), 0.78f);
        cursorY += 26;

        if (party.serverAddress() != null && !party.serverAddress().isBlank()) {
            ctx.drawSmoothText("Server: " + party.serverAddress(),
                    x + PAD, cursorY, theme.accent(), 0.8f);
            cursorY += 16;
        }

        for (SocialClient.PartyMember m : party.members()) {
            ctx.drawSmoothText((m.leader() ? "â˜… " : "  ") + m.username(),
                    x + PAD, cursorY, theme.foreground(), 0.85f);
            cursorY += 14;
            if (cursorY > bottom - 8) break;
        }
    }

    public boolean mousePressed(double mouseX, double mouseY, int button, int sw, int sh) {
        if (button != 0) {
            return true;
        }
        int panelH = Math.min(500, sh - 28);
        int x = (sw - PANEL_W) / 2;
        int y = (sh - panelH) / 2;

        if (hit(mouseX, mouseY, x + PANEL_W - PAD - 64, y + 10, 64, 18)) {
            refresh(true);
            status = "Refreshingâ€¦";
            statusError = false;
            return true;
        }

        int tabY = y + 36;
        if (hit(mouseX, mouseY, x + PAD, tabY, 72, 18)) {
            tab = Tab.FRIENDS;
            return true;
        }
        if (hit(mouseX, mouseY, x + PAD + 78, tabY, 64, 18)) {
            tab = Tab.CHAT;
            reloadConversations();
            return true;
        }
        if (hit(mouseX, mouseY, x + PAD + 148, tabY, 64, 18)) {
            tab = Tab.PARTY;
            CompletableFuture.runAsync(() -> social.client().refreshParty());
            return true;
        }

        int contentTop = tabY + 24;
        return switch (tab) {
            case FRIENDS -> clickFriends(mouseX, mouseY, x, contentTop);
            case CHAT -> clickChat(mouseX, mouseY, x, contentTop, y + panelH);
            case PARTY -> clickParty(mouseX, mouseY, x, contentTop);
        };
    }

    private boolean clickFriends(double mouseX, double mouseY, int x, int top) {
        int cursorY = top;
        String joinAddr = resolveJoinAddress();
        if (joinAddr != null && !joinAddr.isBlank()) {
            if (hit(mouseX, mouseY, x + PAD, cursorY, PANEL_W - PAD * 2, 34)) {
                adapter.joinMultiplayerServer(joinAddr);
                social.consumePendingPartyServer();
                status = "Joining " + joinAddr;
                return true;
            }
            cursorY += 42;
        }
        if (hit(mouseX, mouseY, x + PANEL_W - PAD - 64, cursorY, 64, 22)) {
            String name = draftAdd.trim();
            if (name.length() >= 3) {
                boolean ok = social.client().requestFriend(name);
                status = ok ? "Request sent to " + name : "Add failed â€” they must open Prime once";
                statusError = !ok;
                if (ok) draftAdd = "";
            }
            return true;
        }
        cursorY += 30;
        List<SocialClient.Friend> friends = sortedFriends();
        int i = 0;
        for (SocialClient.Friend f : friends) {
            int rowY = cursorY + (i - scroll) * (ROW_H + 4);
            i++;
            int btnX = x + PANEL_W - PAD - 10 - ACTION_W;
            if (f.pending() && f.incoming()
                    && hit(mouseX, mouseY, btnX, rowY + 9, ACTION_W, 16)) {
                boolean ok = social.client().acceptFriend(f.uuid());
                status = ok ? "Accepted " + f.username() : "Accept failed";
                statusError = !ok;
                return true;
            }
            if (!f.pending()) {
                if (hit(mouseX, mouseY, btnX - ACTION_W - 4, rowY + 9, ACTION_W, 16)) {
                    tab = Tab.CHAT;
                    openChatWith(f.uuid(), f.username());
                    return true;
                }
                if (hit(mouseX, mouseY, btnX, rowY + 9, ACTION_W, 16)) {
                    boolean ok = social.client().inviteParty(f.uuid());
                    status = ok ? "Invited " + f.username() : "Invite failed";
                    statusError = !ok;
                    return true;
                }
            }
        }
        return true;
    }

    private boolean clickChat(double mouseX, double mouseY, int x, int top, int panelBottom) {
        int listW = 118;
        int listX = x + PAD;
        int chatX = listX + listW + 8;
        int chatW = PANEL_W - PAD * 2 - listW - 8;
        int row = 0;
        for (SocialClient.Friend f : sortedFriends()) {
            if (f.pending()) continue;
            int ry = top + 4 + row * 22;
            row++;
            if (hit(mouseX, mouseY, listX + 2, ry, listW - 4, 20)) {
                openChatWith(f.uuid(), f.username());
                return true;
            }
        }
        int fieldY = panelBottom - 26 - 28; // contentBottom approx
        // Use same geometry as render: contentBottom = panel bottom - 26 from panel top... 
        // In mousePressed we don't have contentBottom; approximate from panel.
        int contentBottom = panelBottom - (panelBottom - top > 0 ? 26 : 26);
        // Fix: pass correct field Y - panelBottom here is y+panelH from mousePressed
        fieldY = panelBottom - 52;
        if (hit(mouseX, mouseY, chatX + chatW - 64, fieldY, 64, 22)) {
            sendDraft();
            return true;
        }
        return true;
    }

    private boolean clickParty(double mouseX, double mouseY, int x, int top) {
        int cursorY = top;
        List<SocialClient.PartyInvite> invites = social.client().partyInvites();
        if (!invites.isEmpty()) {
            cursorY += 14;
            for (SocialClient.PartyInvite inv : invites) {
                if (hit(mouseX, mouseY, x + PANEL_W - PAD - 10 - ACTION_W * 2 - 4, cursorY + 8, ACTION_W, 16)) {
                    boolean ok = social.client().acceptPartyInvite(inv.id());
                    status = ok ? "Joined party" : "Accept failed";
                    statusError = !ok;
                    return true;
                }
                if (hit(mouseX, mouseY, x + PANEL_W - PAD - 10 - ACTION_W, cursorY + 8, ACTION_W, 16)) {
                    social.client().declinePartyInvite(inv.id());
                    status = "Invite declined";
                    return true;
                }
                cursorY += 40;
            }
        }
        SocialClient.Party party = social.client().party();
        if (party == null) {
            if (hit(mouseX, mouseY, x + PAD + 12, cursorY + 30, 120, 18)) {
                boolean ok = social.client().createParty();
                status = ok ? "Party created" : "Create failed";
                statusError = !ok;
                if (ok) {
                    CompletableFuture.runAsync(() -> social.client().refreshParty());
                }
                return true;
            }
            return true;
        }
        if (hit(mouseX, mouseY, x + PAD, cursorY, 88, 18)) {
            boolean ok = social.client().leaveParty();
            status = ok ? "Left party" : "Leave failed";
            statusError = !ok;
            return true;
        }
        return true;
    }

    private void sendDraft() {
        if (activeConversationId == null || draftChat.isBlank()) {
            return;
        }
        String text = draftChat.trim();
        draftChat = "";
        social.client().sendTyping(activeConversationId);
        CompletableFuture.runAsync(() -> {
            boolean ok = social.client().sendMessage(activeConversationId, text);
            status = ok ? "" : "Send failed";
            statusError = !ok;
            if (ok) {
                chatMessages = social.client().cachedMessages(activeConversationId);
            }
        });
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int max = Math.max(0, social.client().friends().size() - 8);
        scroll = GuiLayout.clamp(scroll - (int) Math.signum(amount), 0, max);
        return true;
    }

    public boolean charTyped(char c) {
        if (c < 32 || c >= 127) {
            return true;
        }
        if (tab == Tab.FRIENDS) {
            if (draftAdd.length() < 16 && (Character.isLetterOrDigit(c) || c == '_')) {
                draftAdd += c;
            }
        } else if (tab == Tab.CHAT && draftChat.length() < 120) {
            draftChat += c;
            if (activeConversationId != null) {
                social.client().sendTyping(activeConversationId);
            }
        }
        return true;
    }

    public boolean keyPressed(int key) {
        if (key == 256) {
            adapter.closeCurrentScreen();
            return true;
        }
        if (key == 294) {
            refresh(true);
            return true;
        }
        if (key == 259) {
            if (tab == Tab.FRIENDS && !draftAdd.isEmpty()) {
                draftAdd = draftAdd.substring(0, draftAdd.length() - 1);
            } else if (tab == Tab.CHAT && !draftChat.isEmpty()) {
                draftChat = draftChat.substring(0, draftChat.length() - 1);
            }
            return true;
        }
        if ((key == 257 || key == 335) && tab == Tab.CHAT) {
            sendDraft();
            return true;
        }
        if ((key == 257 || key == 335) && tab == Tab.FRIENDS && !draftAdd.isBlank()) {
            boolean ok = social.client().requestFriend(draftAdd.trim());
            status = ok ? "Request sent" : "Add failed";
            statusError = !ok;
            if (ok) draftAdd = "";
            return true;
        }
        return true;
    }

    private List<SocialClient.Friend> sortedFriends() {
        List<SocialClient.Friend> friends = new ArrayList<>(social.client().friends().values());
        friends.sort(Comparator
                .comparingInt((SocialClient.Friend f) -> f.pending() ? -1 : statusRank(f.status()))
                .thenComparing(f -> f.username(), String.CASE_INSENSITIVE_ORDER));
        return friends;
    }

    private void drawTab(RenderContext ctx, Theme theme, int x, int y, String label,
                         boolean selected, double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, 70, 18);
        ctx.fillRoundedRect(x, y, 70, 18, PrimeDesign.RADIUS_SM,
                selected ? theme.surfaceElevated() : ColorUtil.withAlpha(theme.backgroundLight(), hover ? 0.8f : 0.5f));
        ctx.drawSmoothText(label, x + 12, y + 4, selected ? theme.accent() : theme.foregroundMuted(), 0.8f);
    }

    private void drawStatusPill(RenderContext ctx, Theme theme, int panelX, int panelY) {
        SocialClient.ConnState state = social.client().state();
        String label = switch (state) {
            case CONNECTED -> "Online";
            case CONNECTING -> "Connecting";
            case ERROR -> "Error";
            case DISCONNECTED -> "Offline";
        };
        int color = switch (state) {
            case CONNECTED -> 0xFF22C55E;
            case CONNECTING -> 0xFFEAB308;
            case ERROR -> 0xFFEF4444;
            case DISCONNECTED -> theme.foregroundMuted();
        };
        int pillX = panelX + PAD + ctx.smoothTextWidth("Social", 1.12f) + 10;
        int pillW = ctx.smoothTextWidth(label, 0.72f) + 18;
        ctx.fillRoundedRect(pillX, panelY + 12, pillW, 14, PrimeDesign.RADIUS_SM,
                ColorUtil.withAlpha(color, 0.18f));
        ctx.fillRoundedRect(pillX + 5, panelY + 16, 6, 6, 3, color);
        ctx.drawSmoothText(label, pillX + 14, panelY + 15, color, 0.72f);
    }

    private static void drawAction(RenderContext ctx, Theme theme, int x, int y, int w,
                                   String label, double mouseX, double mouseY) {
        boolean hover = hit(mouseX, mouseY, x, y, w, 16);
        ctx.fillRoundedRect(x, y, w, 16, PrimeDesign.RADIUS_SM,
                hover ? theme.accent() : theme.backgroundLight());
        int tw = ctx.smoothTextWidth(label, 0.68f);
        ctx.drawSmoothText(label, x + (w - tw) / 2, y + 4,
                hover ? 0xFFFFFFFF : theme.foregroundMuted(), 0.68f);
    }

    private String resolveJoinAddress() {
        SocialClient.Party party = social.client().party();
        if (party != null && party.serverAddress() != null && !party.serverAddress().isBlank()) {
            return party.serverAddress();
        }
        return social.pendingPartyServer();
    }

    private static int statusRank(String status) {
        return switch (normalize(status)) {
            case "in-game", "ingame" -> 0;
            case "online" -> 1;
            case "away" -> 2;
            default -> 3;
        };
    }

    private static int statusColor(Theme theme, String status) {
        return switch (normalize(status)) {
            case "in-game", "ingame" -> theme.accent();
            case "online" -> 0xFF22C55E;
            case "away" -> 0xFFEAB308;
            default -> 0xFF6B7280;
        };
    }

    private static String formatMeta(SocialClient.Friend f) {
        if (f.pending()) {
            return f.incoming() ? "Incoming request" : "Pending request";
        }
        if (f.serverAddress() != null && !f.serverAddress().isBlank()) {
            return f.serverAddress();
        }
        if (f.activity() != null && !f.activity().isBlank()) {
            return f.activity();
        }
        return switch (normalize(f.status())) {
            case "in-game", "ingame" -> "In game";
            case "online" -> "Online";
            case "away" -> "Away";
            default -> "Offline";
        };
    }

    private static String normalize(String status) {
        return status == null ? "" : status.toLowerCase(Locale.ROOT).trim();
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
