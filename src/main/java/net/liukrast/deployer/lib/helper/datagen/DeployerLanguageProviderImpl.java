package net.liukrast.deployer.lib.helper.datagen;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public abstract class DeployerLanguageProviderImpl extends LanguageProvider implements DeployerLanguageProvider {
    private final String modid;
    public DeployerLanguageProviderImpl(PackOutput output, String modid, String locale) {
        super(output, modid, locale);
        this.modid = modid;
    }

    @Override
    public String getModId() {
        return modid;
    }

    @Override
    public void addI(String key, String value) {
        add(key, value);
    }
}
