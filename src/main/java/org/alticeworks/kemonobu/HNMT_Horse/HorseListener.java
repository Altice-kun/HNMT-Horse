package org.alticeworks.kemonobu.HNMT_Horse;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class HorseListener implements Listener {

    private final HNMT_Horse plugin;
    private final NamespacedKey keyRunLevel;
    private final NamespacedKey keyJumpLevel;
    private final NamespacedKey keyHealthLevel;
    private final NamespacedKey keyStamina;
    private final NamespacedKey keyMaxStamina;

    public HorseListener(HNMT_Horse plugin) {
        this.plugin = plugin;
        this.keyRunLevel = new NamespacedKey(plugin, "run_level");
        this.keyJumpLevel = new NamespacedKey(plugin, "jump_level");
        this.keyHealthLevel = new NamespacedKey(plugin, "health_level");
        this.keyStamina = new NamespacedKey(plugin, "stamina");
        this.keyMaxStamina = new NamespacedKey(plugin, "max_stamina");
    }

    // シフト右クリックで馬のステータスを表示
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // メインハンドのイベントのみ処理（二重呼び出し防止）
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        // プレイヤーがスニーク（シフト）しているか確認
        if (!player.isSneaking()) return;

        // 対象が馬であるか確認
        if (!(event.getRightClicked() instanceof Horse horse)) return;

        // 馬の試乗やインベントリ開きイベントをキャンセル
        event.setCancelled(true);

        // データ読み込み
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        int runLevel = pdc.getOrDefault(keyRunLevel, PersistentDataType.INTEGER, 1);
        int jumpLevel = pdc.getOrDefault(keyJumpLevel, PersistentDataType.INTEGER, 1);
        int healthLevel = pdc.getOrDefault(keyHealthLevel, PersistentDataType.INTEGER, 1);
        double stamina = pdc.getOrDefault(keyStamina, PersistentDataType.DOUBLE, 100.0);
        double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);

        // 各種属性値の取得
        AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        AttributeInstance healthAttr = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        double speed = (speedAttr != null) ? speedAttr.getValue() : 0.0;
        double currentHealth = horse.getHealth();
        double maxHealth = (healthAttr != null) ? healthAttr.getValue() : 20.0;
        double jumpStrength = horse.getJumpStrength();

        // メッセージ送信
        player.sendMessage(ChatColor.GOLD + "========================================");
        player.sendMessage(ChatColor.YELLOW + " 🐴 【 馬のステータス情報 】");
        player.sendMessage(ChatColor.GOLD + "----------------------------------------");
        player.sendMessage(ChatColor.GREEN + " • ランニングLV : " + ChatColor.WHITE + runLevel + ChatColor.GRAY + " (速度: " + String.format("%.3f", speed) + ")");
        player.sendMessage(ChatColor.GREEN + " • ジャンプLV     : " + ChatColor.WHITE + jumpLevel + ChatColor.GRAY + " (跳躍力: " + String.format("%.2f", jumpStrength) + ")");
        player.sendMessage(ChatColor.GREEN + " • 体力LV         : " + ChatColor.WHITE + healthLevel + ChatColor.GRAY + " (HP: " + String.format("%.1f", currentHealth) + " / " + String.format("%.1f", maxHealth) + ")");
        player.sendMessage(ChatColor.AQUA  + " • スタミナ       : " + ChatColor.WHITE + String.format("%.1f", stamina) + " / " + String.format("%.1f", maxStamina));
        player.sendMessage(ChatColor.GOLD + "========================================");
    }

    // 馬がスポーンした時に個体値と初期ステータスをランダム付与
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
            pdc.set(keyJumpLevel, PersistentDataType.INTEGER, 1);
            pdc.set(keyHealthLevel, PersistentDataType.INTEGER, 1);
            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);
            pdc.set(keyStamina, PersistentDataType.DOUBLE, 100.0);

            AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                double randomSpeed = 0.11 + (Math.random() * 0.13);
                speedAttr.setBaseValue(randomSpeed);
            }
        }
    }

    // 馬に乗って移動したときのランニングレベル育成 & スタミナ消費
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Horse horse)) return;

        if (horse.getPassengers().isEmpty() || !(horse.getPassengers().get(0) instanceof Player player)) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0.01) return;

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        int runLevel = pdc.getOrDefault(keyRunLevel, PersistentDataType.INTEGER, 1);
        double stamina = pdc.getOrDefault(keyStamina, PersistentDataType.DOUBLE, 100.0);
        double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);

        if (stamina > 0) {
            stamina = Math.max(0, stamina - (distance * 0.5));
            pdc.set(keyStamina, PersistentDataType.DOUBLE, stamina);
        } else {
            AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(0.08);
            }
        }

        if (Math.random() < 0.005 * (1.0 / runLevel)) {
            runLevel++;
            pdc.set(keyRunLevel, PersistentDataType.INTEGER, runLevel);
            maxStamina += 10.0;
            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, maxStamina);
            pdc.set(keyStamina, PersistentDataType.DOUBLE, maxStamina);

            AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                double newSpeed = Math.min(0.35, speedAttr.getBaseValue() + 0.005);
                speedAttr.setBaseValue(newSpeed);
            }

            player.sendActionBar(ChatColor.GOLD + "🐴 馬のランニングレベルが " + runLevel + " に上がった！ (速度・スタミナUP)");
        }
    }

    // ジャンプ時のジャンプレベル育成
    @EventHandler
    public void onHorseJump(HorseJumpEvent event) {
        Horse horse = (Horse) event.getEntity();
        if (horse.getPassengers().isEmpty() || !(horse.getPassengers().get(0) instanceof Player player)) {
            return;
        }

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        int jumpLevel = pdc.getOrDefault(keyJumpLevel, PersistentDataType.INTEGER, 1);
        double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);

        if (Math.random() < 0.25) {
            jumpLevel++;
            pdc.set(keyJumpLevel, PersistentDataType.INTEGER, jumpLevel);
            maxStamina += 5.0;
            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, maxStamina);

            double newJump = Math.min(1.0, horse.getJumpStrength() + 0.02);
            horse.setJumpStrength(newJump);

            player.sendActionBar(ChatColor.GREEN + "⬆️ 馬のジャンプレベルが " + jumpLevel + " に上がった！ (ジャンプ力UP)");
        }
    }

    // ダメージを受けたときの体力レベル育成
    @EventHandler
    public void onHorseDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Horse horse)) return;

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        int healthLevel = pdc.getOrDefault(keyHealthLevel, PersistentDataType.INTEGER, 1);
        double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0);

        healthLevel++;
        pdc.set(keyHealthLevel, PersistentDataType.INTEGER, healthLevel);
        maxStamina += 8.0;
        pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, maxStamina);

        AttributeInstance maxHealthAttr = horse.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr != null) {
            double newMaxHealth = Math.min(60.0, maxHealthAttr.getBaseValue() + 1.0);
            maxHealthAttr.setBaseValue(newMaxHealth);
            horse.setHealth(Math.min(newMaxHealth, horse.getHealth() + 2.0));
        }

        if (!horse.getPassengers().isEmpty() && horse.getPassengers().get(0) instanceof Player player) {
            player.sendActionBar(ChatColor.RED + "🛡️ 馬の体力レベルが " + healthLevel + " に上がった！");
        }
    }
}