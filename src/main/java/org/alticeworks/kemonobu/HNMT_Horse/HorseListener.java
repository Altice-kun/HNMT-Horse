package org.alticeworks.kemonobu.HNMT_Horse;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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

    // ランニング（スピード）専用の必要距離(EXP)計算: 10 + (0.5 * レベル)
    private double getRequiredRunExp(int level) {
        return 10.0 + (0.5 * level);
    }

    // ジャンプ・体力の必要EXP計算
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
                double randomSpeed = 0.15 + (Math.random() * 0.05);
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
            player.sendMessage(ChatColor.GREEN + " • ランニングLV : " + ChatColor.WHITE + runLvl + ChatColor.GRAY + " (走破: " + String.format("%.1f", runExp) + "/" + String.format("%.1f", getRequiredRunExp(runLvl)) + "m | 速度: " + String.format("%.3f", speed) + ")");
            player.sendMessage(ChatColor.GREEN + " • ジャンプLV     : " + ChatColor.WHITE + jumpLvl + ChatColor.GRAY + " (EXP: " + String.format("%.1f", jumpExp) + "/" + getRequiredExp(jumpLvl) + " | 跳躍力: " + String.format("%.2f", horse.getJumpStrength()) + ")");
            player.sendMessage(ChatColor.GREEN + " • 体力LV         : " + ChatColor.WHITE + healthLvl + ChatColor.GRAY + " (EXP: " + String.format("%.1f", healthExp) + "/" + getRequiredExp(healthLvl) + " | HP: " + String.format("%.1f", curHp) + "/" + String.format("%.1f", maxHp) + ")");
            player.sendMessage(ChatColor.AQUA  + " • スタミナ       : " + ChatColor.WHITE + String.format("%.1f", stamina) + " / " + String.format("%.1f", maxStamina));
            player.sendMessage(ChatColor.GOLD + "========================================");
        }
    }

    // 鞭の使用処理
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

            if (stamina < 10.0 || curHealth <= 0.5) {
                player.sendMessage(ChatColor.RED + "馬のスタミナまたは体力が足りず、鞭を使用できません！");
                return;
            }

            pdc.set(keyStamina, PersistentDataType.DOUBLE, stamina - 10.0);
            horse.setHealth(curHealth - 0.5);

            int currentDuration = 0;
            if (horse.hasPotionEffect(PotionEffectType.SPEED)) {
                currentDuration = horse.getPotionEffect(PotionEffectType.SPEED).getDuration();
            }
            horse.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, currentDuration + 100, 1, false, true));

            player.playSound(horse.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.2f);
            player.sendActionBar(ChatColor.YELLOW + "⚡ 鞭を使用！ (スタミナ-10, HP-0.5) 速度アップ時間が延長されました！");
        }
    }

    // 走行距離に応じたランニングレベルアップ判定
    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Horse horse)) return;
        if (horse.getPassengers().isEmpty() || !(horse.getPassengers().get(0) instanceof Player player)) return;

        double distance = event.getFrom().distance(event.getTo());
        if (distance <= 0.05 || distance > 10.0) return;

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        double stamina = pdc.getOrDefault(keyStamina, PersistentDataType.DOUBLE, 100.0);

        // スタミナ消費
        if (stamina > 0) {
            stamina = Math.max(0, stamina - (distance * 0.2));
            pdc.set(keyStamina, PersistentDataType.DOUBLE, stamina);
        }

        // 走行距離をそのままEXP（メートル/ブロック単位）として蓄積
        int runLvl = pdc.getOrDefault(keyRunLevel, PersistentDataType.INTEGER, 1);
        double runExp = pdc.getOrDefault(keyRunExp, PersistentDataType.DOUBLE, 0.0) + distance;
        double reqExp = getRequiredRunExp(runLvl);

        // 新しい計算式 [10 + (0.5 * レベル)] を満たしたらレベルアップ
        while (runExp >= reqExp) {
            runExp -= reqExp;
            runLvl++;
            reqExp = getRequiredRunExp(runLvl);

            double maxStamina = pdc.getOrDefault(keyMaxStamina, PersistentDataType.DOUBLE, 100.0) + 15.0;
            pdc.set(keyMaxStamina, PersistentDataType.DOUBLE, maxStamina);

            AttributeInstance speedAttr = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(speedAttr.getBaseValue() + 0.005);
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            player.sendMessage(ChatColor.GOLD + "🐴 【レベルアップ】 馬のランニングレベルが " + runLvl + " に上がりました！ (速度・最大スタミナUP)");
        }

        pdc.set(keyRunLevel, PersistentDataType.INTEGER, runLvl);
        pdc.set(keyRunExp, PersistentDataType.DOUBLE, runExp);
    }

    // ジャンプEXP獲得
    @EventHandler
    public void onHorseJump(HorseJumpEvent event) {
        Horse horse = (Horse) event.getEntity();
        if (horse.getPassengers().isEmpty() || !(horse.getPassengers().get(0) instanceof Player player)) return;

        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        int jumpLvl = pdc.getOrDefault(keyJumpLevel, PersistentDataType.INTEGER, 1);
        double jumpExp = pdc.getOrDefault(keyJumpExp, PersistentDataType.DOUBLE, 0.0) + 15.0;
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

    private boolean isCustomItem(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}