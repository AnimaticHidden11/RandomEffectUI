package com.randomeffectsui;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class RandomEffectsUI extends JavaPlugin implements Listener {

    private final Random random = new Random();
    private FileConfiguration config;

    private int interval;
    private int badChance;
    private int minDuration;
    private int maxDuration;
    private int minLevel;
    private int maxLevel;
    private String lang;
    private List<String> disabledEffects;
    private List<String> enabledWorlds;

    private boolean commandOnlyForOp;
    private boolean showJoinMessage;

    private final List<PotionEffectType> badEffects = new ArrayList<>();
    private final List<PotionEffectType> goodEffects = new ArrayList<>();

    private BukkitRunnable task;
    private boolean isRunning = false;

    @Override
    public void onEnable() {
        loadConfig();
        initEffects();
        Bukkit.getPluginManager().registerEvents(this, this);
        printStartupInfo();
        getLogger().info("RandomEffectsUI enabled! Use /Effect RandomEffectsUI true/false");
    }

    @Override
    public void onDisable() {
        stopTask();
        getLogger().info("RandomEffectsUI disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!showJoinMessage) return;

        Player player = event.getPlayer();
        if (commandOnlyForOp && !player.isOp()) return;

        String msg = lang.equals("ru") ?
                "§eДля управления плагином RandomEffectsUI используйте: §f/Effect RandomEffectsUI true §e(вкл) или §f/Effect RandomEffectsUI false §e(выкл)" :
                "§eTo manage RandomEffectsUI plugin type: §f/Effect RandomEffectsUI true §e(on) or §f/Effect RandomEffectsUI false §e(off)";
        player.sendMessage("§7§m----------------------------------------");
        player.sendMessage(msg);
        player.sendMessage("§7§m----------------------------------------");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (commandOnlyForOp && !player.isOp()) {
            player.sendMessage(lang.equals("ru") ? "§cУ вас нет прав на использование этой команды!" : "§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(lang.equals("ru") ? "§cИспользуй: /Effect RandomEffectsUI <true/false>" :
                    "§cUse: /Effect RandomEffectsUI <true/false>");
            return true;
        }

        if (!args[0].equalsIgnoreCase("RandomEffectsUI")) {
            player.sendMessage(lang.equals("ru") ? "§cНеизвестная команда." : "§cUnknown command.");
            return true;
        }

        if (args[1].equalsIgnoreCase("true")) {
            if (isRunning) {
                player.sendMessage(lang.equals("ru") ? "§cЭффекты уже запущены!" : "§cEffects are already running!");
            } else {
                startTask();
                player.sendMessage(lang.equals("ru") ? "§aЭффекты запущены!" : "§aEffects started!");
                Bukkit.broadcastMessage(lang.equals("ru") ? "§aИгрок §e" + player.getName() + " §aзапустил эффекты!" :
                        "§aPlayer §e" + player.getName() + " §astarted effects!");
            }
            return true;
        }

        if (args[1].equalsIgnoreCase("false")) {
            if (!isRunning) {
                player.sendMessage(lang.equals("ru") ? "§cЭффекты уже остановлены!" : "§cEffects are already stopped!");
            } else {
                stopTask();
                player.sendMessage(lang.equals("ru") ? "§eЭффекты остановлены." : "§eEffects stopped.");
                Bukkit.broadcastMessage(lang.equals("ru") ? "§eИгрок §e" + player.getName() + " §eостановил эффекты." :
                        "§ePlayer §e" + player.getName() + " §estopped effects.");
            }
            return true;
        }

        player.sendMessage(lang.equals("ru") ? "§cИспользуй true или false." : "§cUse true or false.");
        return true;
    }

    private void startTask() {
        if (task != null) return;
        isRunning = true;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                applyEffectToAllPlayers();
            }
        };
        task.runTaskTimer(this, interval * 20L, interval * 20L);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        isRunning = false;
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();

        interval = Math.max(1, config.getInt("interval-seconds", 30));
        badChance = Math.max(0, Math.min(100, config.getInt("bad-chance", 80)));
        minDuration = Math.max(1, config.getInt("duration-min", 10));
        maxDuration = Math.max(minDuration, config.getInt("duration-max", 20));
        minLevel = Math.max(1, config.getInt("min-level", 1));
        maxLevel = Math.max(minLevel, config.getInt("max-level", 4));
        lang = config.getString("language", "ru");
        disabledEffects = config.getStringList("disabled-effects");
        enabledWorlds = config.getStringList("enabled-worlds");

        commandOnlyForOp = config.getBoolean("command-only-for-op", false);
        showJoinMessage = config.getBoolean("show-join-message", true);
    }

    private void initEffects() {
        badEffects.addAll(Arrays.asList(
                PotionEffectType.BLINDNESS,
                PotionEffectType.SLOWNESS,
                PotionEffectType.WEAKNESS,
                PotionEffectType.POISON,
                PotionEffectType.WITHER,
                PotionEffectType.HUNGER,
                PotionEffectType.NAUSEA,
                PotionEffectType.LEVITATION,
                PotionEffectType.DARKNESS,
                PotionEffectType.MINING_FATIGUE,
                PotionEffectType.UNLUCK,
                PotionEffectType.GLOWING
        ));

        goodEffects.addAll(Arrays.asList(
                PotionEffectType.SPEED,
                PotionEffectType.STRENGTH,
                PotionEffectType.REGENERATION,
                PotionEffectType.RESISTANCE,
                PotionEffectType.FIRE_RESISTANCE,
                PotionEffectType.WATER_BREATHING,
                PotionEffectType.NIGHT_VISION,
                PotionEffectType.JUMP_BOOST,
                PotionEffectType.LUCK,
                PotionEffectType.DOLPHINS_GRACE,
                PotionEffectType.CONDUIT_POWER,
                PotionEffectType.SLOW_FALLING,
                PotionEffectType.HERO_OF_THE_VILLAGE,
                PotionEffectType.HASTE,
                PotionEffectType.HEALTH_BOOST,
                PotionEffectType.ABSORPTION,
                PotionEffectType.INVISIBILITY,
                PotionEffectType.SATURATION
        ));
    }

    private void applyEffectToAllPlayers() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;

        boolean isBad = random.nextInt(100) < badChance;
        PotionEffectType type;
        int level;
        int duration;

        if (isBad) {
            type = getRandomEffect(badEffects);
            if (type == null) return;
            level = minLevel + random.nextInt(maxLevel - minLevel + 1);
            duration = minDuration + random.nextInt(maxDuration - minDuration + 1);
        } else {
            type = getRandomEffect(goodEffects);
            if (type == null) return;
            level = minLevel + random.nextInt(maxLevel - minLevel + 1);
            duration = minDuration + random.nextInt(maxDuration - minDuration + 1);
        }

        String name = getEffectName(type);
        String msg = isBad ?(lang.equals("ru") ? "§c⚠ Новый эффект: §f" + name + " §cуровень §f" + (level + 1) :
                             "§c⚠ New effect: §f" + name + " §clevel §f" + (level + 1)) :
                (lang.equals("ru") ? "§a✅ Новый эффект: §f" + name + " §aуровень §f" + (level + 1) :
                 "§a✅ New effect: §f" + name + " §alevel §f" + (level + 1));

        for (Player p : Bukkit.getOnlinePlayers()) {
            String worldName = p.getWorld().getName();
            if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(worldName)) continue;
            p.addPotionEffect(new PotionEffect(type, duration * 20, level));
            p.sendMessage(msg);
        }
    }

    private PotionEffectType getRandomEffect(List<PotionEffectType> effects) {
        List<PotionEffectType> available = new ArrayList<>();
        for (PotionEffectType type : effects) {
            if (!disabledEffects.contains(type.getKey().getKey().toUpperCase())) {
                available.add(type);
            }
        }
        return available.isEmpty() ? null : available.get(random.nextInt(available.size()));
    }

    private String getEffectName(PotionEffectType type) {
        String key = type.getKey().getKey();
        if (lang.equals("ru")) {
            switch (key) {
                case "speed": return "Скорость";
                case "slowness": return "Медлительность";
                case "strength": return "Сила";
                case "weakness": return "Слабость";
                case "jump_boost": return "Прыгучесть";
                case "regeneration": return "Регенерация";
                case "poison": return "Отравление";
                case "wither": return "Иссушение";
                case "hunger": return "Голод";
                case "blindness": return "Слепота";
                case "levitation": return "Левитация";
                case "darkness": return "Тьма";
                case "mining_fatigue": return "Усталость";
                case "resistance": return "Сопротивление";
                case "fire_resistance": return "Огнестойкость";
                case "water_breathing": return "Подводное дыхание";
                case "night_vision": return "Ночное зрение";
                case "haste": return "Спешка";
                case "slow_falling": return "Медленное падение";
                case "luck": return "Удача";
                case "unluck": return "Неудача";
                case "dolphins_grace": return "Грация дельфина";
                case "conduit_power": return "Сила кондуита";
                case "hero_of_the_village": return "Герой деревни";
                case "health_boost": return "Здоровье";
                case "absorption": return "Поглощение";
                case "invisibility": return "Невидимость";
                case "saturation": return "Насыщение";
                case "glowing": return "Свечение";
                case "nausea": return "Тошнота";
                default: return key;
            }
        } else {
            return key.substring(0, 1).toUpperCase() + key.substring(1).replace("_", " ");
        }
    }

    private void printStartupInfo() {
        getLogger().info("=== RandomEffectsUI v2.0 ===");
        getLogger().info("Interval: " + interval + "s");
        getLogger().info("Bad chance: " + badChance + "%");
        getLogger().info("Duration: " + minDuration + "-" + maxDuration + "s");
        getLogger().info("Level: " + minLevel + "-" + maxLevel);
        getLogger().info("Language: " + lang);
        getLogger().info("Disabled: " + disabledEffects);
        getLogger().info("Worlds: " + enabledWorlds);
        getLogger().info("Command only for OP: " + commandOnlyForOp);
        getLogger().info("Show join message: " + showJoinMessage);
        getLogger().info("Status: OFF (use /Effect RandomEffectsUI true)");
        getLogger().info("================================");
    }
}