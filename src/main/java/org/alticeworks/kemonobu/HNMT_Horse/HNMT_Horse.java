package org.alticeworks.kemonobu.HNMT_Horse;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class HNMT_Horse extends JavaPlugin {

    private static HNMT_Horse instance;

    @Override
    public void onEnable() {
        instance = this;

        // 起動時の指定ログ出力
        printStartupBanner();

        // カスタムアイテム & レシピ登録
        CustomItems.register(this);

        // イベントリスナーの登録
        getServer().getPluginManager().registerEvents(new HorseListener(this), this);
    }

    @Override
    public void onDisable() {
        // シャットダウン処理
    }

    public static HNMT_Horse getInstance() {
        return instance;
    }

    private void printStartupBanner() {
        Logger log = getLogger();
        log.info("=======================================================");
        log.info("＼　　　＼ ＼　　　＼ ＼　　　＼ ＼＼＼＼＼");
        log.info("＼　　　＼ ＼＼　　＼ ＼＼　＼＼ 　　＼");
        log.info("＼＼＼＼＼ ＼　＼　＼ ＼　＼　＼ 　　＼");
        log.info("＼　　　＼ ＼　　＼＼ ＼　　　＼ 　　＼");
        log.info("＼　　　＼ ＼　　　＼ ＼　　　＼ 　　＼");
        log.info("HNMT Horse Plugin");
        log.info("   Created by Altice-Works Co.,Ltd.");
        ((Logger) log).info("      Copyright 2026 日ノ本鯖 All rights reserved. ");
        log.info("=======================================================");
    }
}