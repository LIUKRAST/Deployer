package net.liukrast.deployer.lib.helper.ponder;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;

public interface SmartPonderRegistrationHelper<T> {

    static <T> SmartPonderRegistrationHelper<T> of(PonderSceneRegistrationHelper<T> helper) {
        return new SmartPonderRegistrationHelper<T>() {
            @Override
            public PonderSceneRegistrationHelper<T> getHelper() {
                return helper;
            }
        };
    }

    PonderSceneRegistrationHelper<T> getHelper();

    default SmartMultiSceneBuilder forComponents(T... components) {
        return SmartMultiSceneBuilder.of(getHelper().forComponents(components));
    }
}
