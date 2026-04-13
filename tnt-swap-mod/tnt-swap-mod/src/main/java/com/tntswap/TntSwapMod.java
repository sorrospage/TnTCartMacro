package com.tntswap;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TntSwapMod implements ModInitializer {
    public static final String MOD_ID = "tntswap";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("TNT Swap Mod initialized!");
    }
}
