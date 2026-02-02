package dev.hypersystems.hyperguard.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.entity.entities.Player;
import dev.hypersystems.hyperguard.BuildInfo;
import dev.hypersystems.hyperguard.check.Check;
import dev.hypersystems.hyperguard.check.CheckManager;
import dev.hypersystems.hyperguard.config.HyperGuardConfig;
import dev.hypersystems.hyperguard.player.HGPlayerData;
import dev.hypersystems.hyperguard.player.HGPlayerManager;
import dev.hypersystems.hyperguard.violation.Violation;
import dev.hypersystems.hyperguard.violation.ViolationManager;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Main HyperGuard command handler.
 * Provides /hyperguard command with various subcommands.
 */
public class HyperGuardCommand extends AbstractCommand {

    // Colors for messaging
    private static final Color RED = new Color(255, 85, 85);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color GRAY = Color.GRAY;
    private static final Color WHITE = Color.WHITE;
    private static final Color CYAN = new Color(0, 255, 255);

    @SuppressWarnings("this-escape")
    public HyperGuardCommand() {
        super("hyperguard", "Anti-cheat management command");

        // Add subcommands
        addSubCommand(new AlertsSubCommand());
        addSubCommand(new CheckSubCommand());
        addSubCommand(new ViolationsSubCommand());
        addSubCommand(new ToggleSubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new ExemptSubCommand());
        addSubCommand(new DebugSubCommand());
        addSubCommand(new InfoSubCommand());

        // Add aliases
        addAliases("hg", "anticheat", "ac");
    }

    @Override
    protected CompletableFuture<Void> execute(CommandContext ctx) {
        ctx.sender().sendMessage(buildHelpMessage());
        return CompletableFuture.completedFuture(null);
    }

    private Message buildHelpMessage() {
        List<Message> parts = new ArrayList<>();
        String label = "HyperGuard v" + BuildInfo.VERSION;
        int width = 42;
        int padding = width - label.length() - 2;
        int left = 3;
        int right = Math.max(3, padding - left);

        parts.add(Message.raw("-".repeat(left) + " ").color(GRAY));
        parts.add(Message.raw(label).color(RED));
        parts.add(Message.raw(" " + "-".repeat(right) + "\n").color(GRAY));
        parts.add(Message.raw("  Server-side anti-cheat\n\n").color(WHITE));
        parts.add(Message.raw("  Commands:\n").color(GOLD));
        parts.add(Message.raw("    alerts").color(GREEN));
        parts.add(Message.raw(" - Toggle staff alerts\n").color(GRAY));
        parts.add(Message.raw("    check <player>").color(GREEN));
        parts.add(Message.raw(" - View player VLs\n").color(GRAY));
        parts.add(Message.raw("    violations [player]").color(GREEN));
        parts.add(Message.raw(" - View recent violations\n").color(GRAY));
        parts.add(Message.raw("    toggle <check>").color(GREEN));
        parts.add(Message.raw(" - Enable/disable check\n").color(GRAY));
        parts.add(Message.raw("    reload").color(GREEN));
        parts.add(Message.raw(" - Reload configuration\n").color(GRAY));
        parts.add(Message.raw("    exempt <player> [check]").color(GREEN));
        parts.add(Message.raw(" - Exempt player\n").color(GRAY));
        parts.add(Message.raw("    debug <player>").color(GREEN));
        parts.add(Message.raw(" - Toggle debug for player\n").color(GRAY));
        parts.add(Message.raw("    info").color(GREEN));
        parts.add(Message.raw(" - Plugin information\n").color(GRAY));
        parts.add(Message.raw("\n  Use /hg <command> for details\n").color(GRAY));
        parts.add(Message.raw("-".repeat(width) + "\n").color(GRAY));

        return Message.join(parts.toArray(new Message[0]));
    }

    /**
     * Send a success message.
     */
    private static void sendSuccess(CommandContext ctx, String message) {
        ctx.sender().sendMessage(Message.join(
            Message.raw("[HG] ").color(RED),
            Message.raw(message).color(GREEN)));
    }

    /**
     * Send an error message.
     */
    private static void sendError(CommandContext ctx, String message) {
        ctx.sender().sendMessage(Message.join(
            Message.raw("[HG] ").color(RED),
            Message.raw(message).color(RED)));
    }

    /**
     * Send an info message.
     */
    private static void sendInfo(CommandContext ctx, String message) {
        ctx.sender().sendMessage(Message.join(
            Message.raw("[HG] ").color(RED),
            Message.raw(message).color(WHITE)));
    }

    /**
     * Get player from command context.
     */
    private static Player getPlayer(CommandContext ctx) {
        if (ctx.sender() instanceof Player player) {
            return player;
        }
        return null;
    }

    // ========== Alerts Subcommand ==========

    private class AlertsSubCommand extends AbstractCommand {
        private final OptionalArg<String> stateArg;

        public AlertsSubCommand() {
            super("alerts", "Toggle staff alerts");
            this.stateArg = withOptionalArg("state", "on or off", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            Player player = getPlayer(ctx);
            if (player == null) {
                sendError(ctx, "This command can only be used by players");
                return CompletableFuture.completedFuture(null);
            }

            HGPlayerData data = HGPlayerManager.get().getPlayerData(player.getUuid());
            if (data == null) {
                sendError(ctx, "Player data not found");
                return CompletableFuture.completedFuture(null);
            }

            String state = ctx.get(stateArg);
            if (state == null) {
                // Toggle
                data.setAlertsEnabled(!data.areAlertsEnabled());
            } else if (state.equalsIgnoreCase("on") || state.equalsIgnoreCase("true")) {
                data.setAlertsEnabled(true);
            } else if (state.equalsIgnoreCase("off") || state.equalsIgnoreCase("false")) {
                data.setAlertsEnabled(false);
            } else {
                sendError(ctx, "Invalid state: " + state + " (use: on/off)");
                return CompletableFuture.completedFuture(null);
            }

            sendSuccess(ctx, "Alerts " + (data.areAlertsEnabled() ? "enabled" : "disabled"));
            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== Check Subcommand ==========

    private class CheckSubCommand extends AbstractCommand {
        private final RequiredArg<String> playerArg;

        public CheckSubCommand() {
            super("check", "View player violation levels");
            this.playerArg = withRequiredArg("player", "Player name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String playerName = ctx.get(playerArg);

            HGPlayerData data = HGPlayerManager.get().findByUsername(playerName);
            if (data == null) {
                sendError(ctx, "Player '" + playerName + "' not found or not online");
                return CompletableFuture.completedFuture(null);
            }

            List<Message> parts = new ArrayList<>();
            parts.add(Message.raw("--- " + data.getUsername() + " Violation Levels ---\n").color(RED));

            Map<String, Double> vls = data.getAllVLs();
            if (vls.isEmpty()) {
                parts.add(Message.raw("  No violations recorded\n").color(GREEN));
            } else {
                List<Map.Entry<String, Double>> sorted = new ArrayList<>(vls.entrySet());
                sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                for (Map.Entry<String, Double> entry : sorted) {
                    Color vlColor = getVLColor(entry.getValue());
                    parts.add(Message.raw("  " + entry.getKey() + ": ").color(GRAY));
                    parts.add(Message.raw(String.format("%.1f", entry.getValue()) + "\n").color(vlColor));
                }
            }

            // Show exemption status
            if (data.isGloballyExempt()) {
                parts.add(Message.raw("  Status: ").color(GRAY));
                parts.add(Message.raw("Globally Exempt\n").color(GOLD));
            } else if (!data.getExemptChecks().isEmpty()) {
                parts.add(Message.raw("  Exempt from: ").color(GRAY));
                parts.add(Message.raw(String.join(", ", data.getExemptChecks()) + "\n").color(GOLD));
            }

            ctx.sender().sendMessage(Message.join(parts.toArray(new Message[0])));
            return CompletableFuture.completedFuture(null);
        }

        private Color getVLColor(double vl) {
            if (vl >= 80) return RED;
            if (vl >= 50) return new Color(255, 170, 0); // Orange
            if (vl >= 20) return new Color(255, 255, 85); // Yellow
            return GREEN;
        }
    }

    // ========== Violations Subcommand ==========

    private class ViolationsSubCommand extends AbstractCommand {
        private final OptionalArg<String> playerArg;
        private final OptionalArg<Integer> limitArg;

        public ViolationsSubCommand() {
            super("violations", "View recent violations");
            this.playerArg = withOptionalArg("player", "Player name (optional)", ArgTypes.STRING);
            this.limitArg = withOptionalArg("limit", "Number of violations to show", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String playerName = ctx.get(playerArg);
            Integer limit = ctx.get(limitArg);
            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<Violation> violations;
            String title;

            if (playerName != null && !playerName.isEmpty()) {
                HGPlayerData data = HGPlayerManager.get().findByUsername(playerName);
                if (data == null) {
                    sendError(ctx, "Player '" + playerName + "' not found or not online");
                    return CompletableFuture.completedFuture(null);
                }
                violations = ViolationManager.get().getRecentViolations(data.getUuid(), limit);
                title = "--- " + data.getUsername() + " Recent Violations ---";
            } else {
                violations = ViolationManager.get().getRecentViolations(limit);
                title = "--- Recent Violations ---";
            }

            List<Message> parts = new ArrayList<>();
            parts.add(Message.raw(title + "\n").color(RED));

            if (violations.isEmpty()) {
                parts.add(Message.raw("  No violations recorded\n").color(GREEN));
            } else {
                for (Violation v : violations) {
                    long ageSeconds = v.getAgeSeconds();
                    String timeAgo = formatTimeAgo(ageSeconds);

                    parts.add(Message.raw("  " + v.username() + " ").color(WHITE));
                    parts.add(Message.raw(v.checkName() + " ").color(RED));
                    parts.add(Message.raw(String.format("VL:%.1f ", v.totalVL())).color(GOLD));
                    parts.add(Message.raw("(" + timeAgo + ")\n").color(GRAY));
                }
            }

            ctx.sender().sendMessage(Message.join(parts.toArray(new Message[0])));
            return CompletableFuture.completedFuture(null);
        }

        private String formatTimeAgo(long seconds) {
            if (seconds < 60) return seconds + "s ago";
            if (seconds < 3600) return (seconds / 60) + "m ago";
            if (seconds < 86400) return (seconds / 3600) + "h ago";
            return (seconds / 86400) + "d ago";
        }
    }

    // ========== Toggle Subcommand ==========

    private class ToggleSubCommand extends AbstractCommand {
        private final RequiredArg<String> checkArg;

        public ToggleSubCommand() {
            super("toggle", "Enable/disable a check");
            this.checkArg = withRequiredArg("check", "Check name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String checkName = ctx.get(checkArg);

            Check check = CheckManager.get().getCheck(checkName);
            if (check == null) {
                // List available checks
                Set<String> checks = CheckManager.get().getCheckNames();
                if (checks.isEmpty()) {
                    sendError(ctx, "No checks registered");
                } else {
                    sendError(ctx, "Unknown check: " + checkName);
                    sendInfo(ctx, "Available: " + String.join(", ", checks));
                }
                return CompletableFuture.completedFuture(null);
            }

            check.setEnabled(!check.isEnabled());
            sendSuccess(ctx, "Check '" + checkName + "' " + (check.isEnabled() ? "enabled" : "disabled"));

            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== Reload Subcommand ==========

    private class ReloadSubCommand extends AbstractCommand {
        public ReloadSubCommand() {
            super("reload", "Reload configuration");
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            HyperGuardConfig.get().reload();
            sendSuccess(ctx, "Configuration reloaded");
            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== Exempt Subcommand ==========

    private class ExemptSubCommand extends AbstractCommand {
        private final RequiredArg<String> playerArg;
        private final OptionalArg<String> checkArg;

        public ExemptSubCommand() {
            super("exempt", "Exempt a player from checks");
            this.playerArg = withRequiredArg("player", "Player name", ArgTypes.STRING);
            this.checkArg = withOptionalArg("check", "Check name (or 'all')", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String playerName = ctx.get(playerArg);
            String checkName = ctx.get(checkArg);

            HGPlayerData data = HGPlayerManager.get().findByUsername(playerName);
            if (data == null) {
                sendError(ctx, "Player '" + playerName + "' not found or not online");
                return CompletableFuture.completedFuture(null);
            }

            if (checkName == null || checkName.isEmpty() || checkName.equalsIgnoreCase("all")) {
                // Toggle global exemption
                data.setGloballyExempt(!data.isGloballyExempt());
                if (data.isGloballyExempt()) {
                    sendSuccess(ctx, "Player '" + playerName + "' is now globally exempt");
                } else {
                    sendSuccess(ctx, "Player '" + playerName + "' is no longer globally exempt");
                }
            } else {
                // Toggle specific check exemption
                boolean exempt = !data.isExempt(checkName);
                data.setExempt(checkName, exempt);
                if (exempt) {
                    sendSuccess(ctx, "Player '" + playerName + "' is now exempt from " + checkName);
                } else {
                    sendSuccess(ctx, "Player '" + playerName + "' is no longer exempt from " + checkName);
                }
            }

            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== Debug Subcommand ==========

    private class DebugSubCommand extends AbstractCommand {
        private final RequiredArg<String> playerArg;

        public DebugSubCommand() {
            super("debug", "Toggle debug mode for a player");
            this.playerArg = withRequiredArg("player", "Player name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            String playerName = ctx.get(playerArg);

            HGPlayerData data = HGPlayerManager.get().findByUsername(playerName);
            if (data == null) {
                sendError(ctx, "Player '" + playerName + "' not found or not online");
                return CompletableFuture.completedFuture(null);
            }

            data.setDebugMode(!data.isDebugMode());
            sendSuccess(ctx, "Debug mode for '" + playerName + "' " + (data.isDebugMode() ? "enabled" : "disabled"));

            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== Info Subcommand ==========

    private class InfoSubCommand extends AbstractCommand {
        public InfoSubCommand() {
            super("info", "Plugin information");
        }

        @Override
        protected CompletableFuture<Void> execute(CommandContext ctx) {
            List<Message> parts = new ArrayList<>();
            parts.add(Message.raw("--- HyperGuard Information ---\n").color(RED));
            parts.add(Message.raw("  Version: ").color(GRAY));
            parts.add(Message.raw(BuildInfo.VERSION + "\n").color(WHITE));
            parts.add(Message.raw("  Java: ").color(GRAY));
            parts.add(Message.raw(BuildInfo.JAVA_VERSION + "\n").color(WHITE));

            HyperGuardConfig config = HyperGuardConfig.get();
            parts.add(Message.raw("  Alerts: ").color(GRAY));
            parts.add(Message.raw((config.getGeneral().isAlertsEnabled() ? "Enabled" : "Disabled") + "\n")
                .color(config.getGeneral().isAlertsEnabled() ? GREEN : RED));
            parts.add(Message.raw("  Debug: ").color(GRAY));
            parts.add(Message.raw((config.getGeneral().isDebugMode() ? "Enabled" : "Disabled") + "\n")
                .color(config.getGeneral().isDebugMode() ? GOLD : WHITE));

            parts.add(Message.raw("  Registered Checks: ").color(GRAY));
            parts.add(Message.raw(CheckManager.get().getCheckCount() + "\n").color(WHITE));

            parts.add(Message.raw("  Players Tracked: ").color(GRAY));
            parts.add(Message.raw(HGPlayerManager.get().getPlayerCount() + "\n").color(WHITE));

            parts.add(Message.raw("  Total Violations: ").color(GRAY));
            parts.add(Message.raw(ViolationManager.get().getTotalViolationCount() + "\n").color(WHITE));

            ctx.sender().sendMessage(Message.join(parts.toArray(new Message[0])));
            return CompletableFuture.completedFuture(null);
        }
    }
}
