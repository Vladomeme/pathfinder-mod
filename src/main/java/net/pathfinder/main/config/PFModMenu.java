package net.pathfinder.main.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.text.Text;

/**
 * Mod Menu config screen hook.
 */
public class PFModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return PFConfig.INSTANCE::create;
        }
        return parent -> new NoticeScreen(() -> MinecraftClient.getInstance().setScreen(parent),
                Text.of("Pathfinder"), Text.of("Mod requires YACL to be able to show the config."));
    }
}
