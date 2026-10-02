package io.github.taikun24.bettertnt.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Leaves out the optimization mixins when a mod that rewrites the same vanilla code is installed;
 * two mods redirecting the same call would fail to load.
 */
public class BetterTNTMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger();
    // Forge ports of Lithium, which have their own explosion and collision optimizations.
    private static final List<String> CONFLICTING_MODS = Arrays.asList("canary", "radium", "roadrunner");
    private static final String PACKAGE = "io.github.taikun24.bettertnt.mixin.";
    private static final List<String> OPTIMIZATION_MIXINS = Arrays.asList(
            PACKAGE + "ExplosionRaysMixin", PACKAGE + "ExposureClipMixin", PACKAGE + "EntityCollideMixin",
            PACKAGE + "ExplosionRayGridMixin");

    private String conflict;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = FMLLoader.getLoadingModList();
        if (mods == null) {
            return;
        }
        for (String id : CONFLICTING_MODS) {
            if (mods.getModFileById(id) != null) {
                conflict = id;
                LOGGER.info("Better TNT: {} is installed, leaving vanilla explosion code to it (throttling still active)", id);
                return;
            }
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return conflict == null || !OPTIMIZATION_MIXINS.contains(mixinClassName);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
