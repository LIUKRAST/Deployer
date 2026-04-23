package net.liukrast.deployer.lib.helper.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.util.function.BiConsumer;

public interface DeployerLanguageProvider {

    String getModId();

    void addI(String key, String value);

    default void createPonder(Item item, String header, String... tooltips) {
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        createPonder(id, header, tooltips);
    }

    default void createPonder(String id, String header, String... tooltips) {
        addReplaced("%s.ponder." + id + ".header", header);
        for(int i = 0; i < tooltips.length; i++) {
            addReplaced("%s.ponder." + id + ".text_" + (i+1), tooltips[i]);
        }
    }

    default void addReplaced(String key, String value) {
        addI(String.format(key, getModId()), value);
    }

    default void addPrefixed(String key, String value) {
        addReplaced("%s."+key, value);
    }

    default String autoName(String id) {
        String[] words = id.split("_");
        for(int i = 0; i < words.length; i++) {
            words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1);
        }
        return String.join(" ", words);
    }

    default ShiftBuilder addShiftSummary(ItemLike key, String summary) {
        return new ShiftBuilder(key, summary, this::addI);
    }

    class ShiftBuilder {
        private final String key;
        private final BiConsumer<String, String> adder;
        int counter = 1;

        private ShiftBuilder(ItemLike key, String summary, BiConsumer<String, String> adder) {
            this.key = key.asItem().getDescriptionId();
            this.adder = adder;
            adder.accept(key + ".tooltip.summary", summary);
        }

        public void addLine(String condition, String behaviour) {
            adder.accept(key + ".tooltip.condition" + counter, condition);
            adder.accept(key + ".tooltip.behaviour" + counter, behaviour);
            counter++;
        }
    }
}
