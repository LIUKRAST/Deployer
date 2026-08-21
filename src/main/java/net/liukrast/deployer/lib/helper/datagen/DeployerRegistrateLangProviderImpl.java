package net.liukrast.deployer.lib.helper.datagen;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import net.liukrast.deployer.lib.helper.Constants;
import net.minecraft.data.PackOutput;

public abstract class DeployerRegistrateLangProviderImpl extends RegistrateLangProvider implements DeployerLanguageProvider {
    private final String modid;
    public DeployerRegistrateLangProviderImpl(Constants constants, PackOutput packOutput) {
        super(constants.registrate(), packOutput);
        this.modid = constants.getModId();
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
