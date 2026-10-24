package org.alticeworks.kemonobu.HNMT_Horse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class HorseListener implements Listener {

    private final HNMT_Horse plugin;

    private final NamespacedKey keyRunLevel;
    private final NamespacedKey keyRunExp;
    private final NamespacedKey keyJumpLevel;
    private final NamespacedKey keyJumpExp;
    private final NamespacedKey keyHealthLevel;
    private final NamespacedKey keyHealthExp;

    private final NamespacedKey keyStamina;
    private final NamespacedKey keyMaxStamina;

    public HorseListener(HNMT_Horse plugin) {
        this.plugin = plugin;
        this.keyRunLevel = new NamespacedKey(plugin, "run_level");
        this.keyRunExp = new NamespacedKey(plugin, "run_exp");
        this.keyJumpLevel = new NamespacedKey(plugin, "jump_level");
        this.keyJumpExp = new NamespacedKey(plugin, "jump_exp");
        this.keyHealthLevel = new NamespacedKey(plugin, "health_level");
        this.keyHealthExp = new NamespacedKey(plugin, "health_exp");

        this.keyStamina = new NamespacedKey(plugin, "stamina");
        this.keyMaxStamina = new NamespacedKey(plugin, "max_stamina");
    }

    // 必要EXPの計算 (レベル x 100)
    private double getRequiredExp(int level) {
        return level * 100.0;
    }

    @EventHandler
    public void onHorseSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Horse horse) {
            initHorseStats(horse);
        }
    }

    private void initHorseStats(Horse horse) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        if (!pdc.has(keyRunLevel, PersistentDataType.INTEGER)) {
            pdc.set(keyRunLevel, PersistentDataType.INTEGER, 1);
            pdc.set(keyRunExp, PersistentDataType.DOUBLE, 0.0);
            pdc.set(keyJumpLevel, PersistentDataType.INTEGER, 1);
            pdc.set(keyJumpExp, PersistentDataType.DOUBLE, 0.0);
            pdc.set(keyHealthLevel, PersistentDataType.INTEGER, 1);
            pdc.set(keyHealthExp, PersistentDataType.DOUBLE, 0.0);

            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);
            pdc.set(keyStamina, PersistentDataType.DOUBLE, 100.0);

            AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                double randomSpeed = 0.15 + (Math.random() * 0.05); // 初期速度
                speedAttr.setBaseValue(randomSpeed);
            }
        }
    }

    // プレイヤーが馬に右クリックしたときの処理（ステータス確認 & カンズメ投与）
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!(event.getRightClicked() instanceof Horse horse)) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();

        // 1. ホースカンズメをスニーク右クリックで与える処理
        if (player.isSneaking() && isCustomItem(handItem, CustomItems.keyHorseCanned)) {
            event.setCancelled(true);

            PersistentDataContainer pdc = horse.getPersistentDataContainer();
            double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);
            pdc.set(keyStamina, PersistentDataType.DOUBLE, maxStamina);

            handItem.setAmount(handItem.getAmount() - 1);
            player.playSound(horse.getLocation(), Sound.ENTITY_HORSE_EAT, 1.0f, 1.0f);
            player.sendMessage(ChatColor.GREEN + "🐴 ホースカンズメを与えてスタミナが全回復しました！");
            return;
        }

        // 2. スニーク右クリックでステータス表示
        if (player.isSneaking()) {
            event.setCancelled(true);

            PersistentDataContainer pdc = horse.getPersistentDataContainer();
            int runLvl = pdc.getOrDefault(keyRunLevel, PersistentDataType.INTEGER, 1);
            double runExp = pdc.getOrDefault(keyRunExp, PersistentDataType.DOUBLE, 0.0);

            int jumpLvl = pdc.getOrDefault(keyJumpLevel, PersistentDataType.INTEGER, 1);
            double jumpExp = pdc.getOrDefault(keyJumpExp, PersistentDataType.DOUBLE, 0.0);

            int healthLvl = pdc.getOrDefault(keyHealthLevel, PersistentDataType.INTEGER, 1);
            double healthExp = pdc.getOrDefault(keyHealthExp, PersistentDataType.DOUBLE, 0.0);

            double stamina = pdc.getOrDefault(keyStamina, PersistentDataType.DOUBLE, 100.0);
            double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);

            AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            AttributeInstance maxHpAttr = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH);

            double speed = (speedAttr != null) ? speedAttr.getValue() : 0.0;
            double curHp = horse.getHealth();
            double maxHp = (maxHpAttr != null) ? maxHpAttr.getValue() : 20.0;

            player.sendMessage(ChatColor.GOLD + "========================================");
            player.sendMessage(ChatColor.YELLOW + " 🐴 【 馬のステータス情報 】");
            player.sendMessage(ChatColor.GOLD + "----------------------------------------");
            player.sendMessage(ChatColor.GREEN + " • ランニングLV : " + ChatColor.WHITE + runLvl + ChatColor.GRAY + " (EXP: " + String.format("%.1f", runExp) + "/" + getRequiredExp(runLvl) + " | 速度: " + String.format("%.3f", speed) + ")");
            player.sendMessage(ChatColor.GREEN + " • ジャンプLV     : " + ChatColor.WHITE + jumpLvl + ChatColor.GRAY + " (EXP: " + String.format("%.1f", jumpExp) + "/" + getRequiredExp(jumpLvl) + " | 跳躍力: " + String.format("%.2f", horse.getJumpStrength()) + ")");
            player.sendMessage(ChatColor.GREEN + " • 体力LV         : " + ChatColor.WHITE + healthLvl + ChatColor.GRAY + " (EXP: " + String.format("%.1f", healthExp) + "/" + getRequiredExp(healthLvl) + " | HP: " + String.format("%.1f", curHp) + "/" + String.format("%.1f", maxHp) + ")");
            player.sendMessage(ChatColor.AQUA  + " • スタミナ       : " + ChatColor.WHITE + String.format("%.1f", stamina) + " / " + String.format("%.1f", maxStamina));
            player.sendMessage(ChatColor.GOLD + "========================================");
        }
    }

    // 鞭の使用処理（手持ちで右クリック）
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack handItem = event.getItem();

        if (handItem == null || !isCustomItem(handItem, CustomItems.keyWhip)) return;

        if (player.getVehicle() instanceof Horse horse) {
            event.setCancelled(true);

            PersistentDataContainer pdc = horse.getPersistentDataContainer();
            double stamina = pdc.getOrDefault(keyStamina, PersistentDataType.DOUBLE, 100.0);
            double curHealth = horse.getHealth();

            // 発動条件チェック: スタミナ10以上、体力0.5超過
            if (stamina < 10.0 || curHealth <= 0.5) {
                player.sendMessage(ChatColor.RED + "馬のスタミナまたは体力が足りず、鞭を使用できません！");
                return;
            }

            // 消費処理
            pdc.set(keyStamina, PersistentDataType.DOUBLE, stamina - 10.0);
            horse.setHealth(curHealth - 0.5);

            // 効果付与（スピード上昇効果を重ねがけ可能にするため持続時間を加算）
            int currentDuration = 0;
            if (horse.hasPotionEffect(PotionEffectType.SPEED)) {
                currentDuration = horse.getPotionEffect(PotionEffectType.SPEED).getDuration();
            }
            // 5秒（100 ticks）追加
            horse.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, currentDuration + 100, 1, false, true));

            player.playSound(horse.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK , 1.0f, 1.2f);
        }
    }

    // 走行・距離計算によるランニングEXP獲得 & レベルアップ
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Horse horse)) return;
        if (horse.getPassengers().isEmpty() || !(horse.getPassengers().get(0) instanceof Player player)) return;

        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0.05 || distance > 10.0) return; // 止まっているかTP等の移動を除外

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        double stamina = pdc.getOrDefault(keyStamina, PersistentDataType.DOUBLE, 100.0);

        // スタミナ消費 (1ブロックごとに0.2消費)
        if (stamina > 0) {
            stamina = Math.max(0, stamina - (distance * 0.2));
            pdc.set(keyStamina, PersistentDataType.DOUBLE, stamina);
        }

        // EXP加算（走った距離に応じてEXP獲得）
        int runLvl = pdc.getOrDefault(keyRunLevel, PersistentDataType.INTEGER, 1);
        double runExp = pdc.getOrDefault(keyRunExp, PersistentDataType.DOUBLE, 0.0) + distance;
        double reqExp = getRequiredExp(runLvl);

        if (runExp >= reqExp) {
            runExp -= reqExp;
            runLvl++;

            pdc.set(keyRunLevel, PersistentDataType.INTEGER, runLvl);
            double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0) + 15.0;
            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, maxStamina);

            // 移動速度の上昇
            AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(speedAttr.getBaseValue() + 0.008);
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            player.sendMessage(ChatColor.GOLD + "🐴 【レベルアップ】 馬のランニングレベルが " + runLvl + " になりました！ (移動速度・最大スタミナUP)");
        }

        pdc.set(keyRunExp, PersistentDataType.DOUBLE, runExp);
    }

    // ジャンプEXP獲得
    @EventHandler
    public void onHorseJump(HorseJumpEvent event) {
        @NotNull AbstractHorse horse = event.getEntity();
        if (horse.getPassengers().isEmpty() || !(horse.getPassengers().get(0) instanceof Player player)) return;

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        int jumpLvl = pdc.getOrDefault(keyJumpLevel, PersistentDataType.INTEGER, 1);
        double jumpExp = pdc.getOrDefault(keyJumpExp, PersistentDataType.DOUBLE, 0.0) + 15.0; // 1回につき15EXP
        double reqExp = getRequiredExp(jumpLvl);

        if (jumpExp >= reqExp) {
            jumpExp -= reqExp;
            jumpLvl++;

            pdc.set(keyJumpLevel, PersistentDataType.INTEGER, jumpLvl);
            double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0) + 10.0;
            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, maxStamina);

            horse.setJumpStrength(Math.min(1.0, horse.getJumpStrength() + 0.02));

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "⬆️ 【レベルアップ】 馬のジャンプレベルが " + jumpLvl + " になりました！ (跳躍力・最大スタミナUP)");
        }

        pdc.set(keyJumpExp, PersistentDataType.DOUBLE, jumpExp);
    }

    // 体力EXP獲得
    @EventHandler
    public void onHorseDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) return;

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        int healthLvl = pdc.getOrDefault(keyHealthLevel, PersistentDataType.INTEGER, 1);
        double healthExp = pdc.getOrDefault(keyHealthExp, PersistentDataType.DOUBLE, 0.0) + (event.getFinalDamage() * 10.0);
        double reqExp = getRequiredExp(healthLvl);

        if (healthExp >= reqExp) {
            healthExp -= reqExp;
            healthLvl++;

            pdc.set(keyHealthLevel, PersistentDataType.INTEGER, healthLvl);
            double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0) + 10.0;
            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, maxStamina);

            AttributeInstance maxHpAttr = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHpAttr != null) {
                double newMaxHp = maxHpAttr.getBaseValue() + 2.0;
                maxHpAttr.setBaseValue(newMaxHp);
            }

            if (!horse.getPassengers().isEmpty() && horse.getPassengers().get(0) instanceof Player player) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                player.sendMessage(ChatColor.RED + "🛡️ 【レベルアップ】 馬の体力レベルが " + healthLvl + " になりました！ (最大HP・最大スタミナUP)");
            }
        }

        pdc.set(keyHealthExp, PersistentDataType.DOUBLE, healthExp);
    }

    // カスタムアイテム識別用ヘルパー
    private boolean isCustomItem(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}