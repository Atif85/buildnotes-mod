package net.atif.buildnotes;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Buildnotes implements ModInitializer {
    public static final String MOD_ID = "buildnotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This is the common entrypoint.
        // For now, we will leave this empty as our logic is client-side.
        LOGGER.info("BuildNotes Initialized!");
    }
}