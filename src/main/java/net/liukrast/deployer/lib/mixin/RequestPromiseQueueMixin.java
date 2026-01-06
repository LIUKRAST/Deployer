package net.liukrast.deployer.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.liukrast.deployer.lib.logistics.packager.StockInventoryType;
import net.liukrast.deployer.lib.logistics.packagerLink.GenericRequestPromise;
import net.liukrast.deployer.lib.mixinExtensions.RPQExtension;
import net.liukrast.deployer.lib.registry.DeployerRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(RequestPromiseQueue.class)
public class RequestPromiseQueueMixin implements RPQExtension {

    @Shadow private Runnable onChanged;
    @Unique private final Map<StockInventoryType<?,?,?>, Map<?, List<GenericRequestPromise<?>>>> deployer$promisesGeneric = new HashMap<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Runnable onChanged, CallbackInfo ci) {
        for(var type : DeployerRegistries.STOCK_INVENTORY)
            deployer$promisesGeneric.put(type, new IdentityHashMap<>());
    }

    @Override
    public <K,V,H> void deployer$add(StockInventoryType<K,V,H> type, GenericRequestPromise<V> promise) {
        //noinspection unchecked
        ((Map<K, List<GenericRequestPromise<V>>>) (Map<?,?>) deployer$promisesGeneric.get(type))
                .computeIfAbsent(type.valueHandler().fromValue(promise.promisedStack), $ -> new LinkedList<>())
                .add(promise);
        onChanged.run();
    }

    @Override
    public <K,V,H> int deployer$getTotalPromisedAndRemoveExpired(StockInventoryType<K,V,H> type, V stack, int expiryTime) {
        int promised = 0;
        @SuppressWarnings("unchecked")
        List<GenericRequestPromise<V>> list = (List<GenericRequestPromise<V>>) (List<?>) deployer$promisesGeneric.get(type).get(type.valueHandler().fromValue(stack));
        if (list == null)
            return promised;

        for (Iterator<GenericRequestPromise<V>> iterator = list.iterator(); iterator.hasNext();) {
            GenericRequestPromise<V> promise = iterator.next();
            if(!type.valueHandler().equalsIgnoreCount(promise.promisedStack, stack)) continue;
            if (expiryTime != -1 && promise.ticksExisted >= expiryTime) {
                iterator.remove();
                onChanged.run();
                continue;
            }

            promised += type.valueHandler().getCount(promise.promisedStack);
        }
        return promised;
    }

    @Override
    public <K,V,H> void deployer$forceClear(StockInventoryType<K,V,H> type, V stack) {
        @SuppressWarnings("unchecked") List<GenericRequestPromise<V>> list = (List<GenericRequestPromise<V>>) (List<?>)deployer$promisesGeneric.get(type).get(type.valueHandler().fromValue(stack));
        if (list == null)
            return;

        for (Iterator<GenericRequestPromise<V>> iterator = list.iterator(); iterator.hasNext();) {
            GenericRequestPromise<V> promise = iterator.next();
            if(!type.valueHandler().equalsIgnoreCount(promise.promisedStack, stack)) continue;
            iterator.remove();
            onChanged.run();
        }

        if (list.isEmpty())
            deployer$promisesGeneric.get(type).remove(type.valueHandler().fromValue(stack));
    }

    @Override
    public <K,V,H> void deployer$genericEnteredSystem(StockInventoryType<K,V,H> type, V entry, int amount) {
        @SuppressWarnings("unchecked")
        List<GenericRequestPromise<V>> list = (List<GenericRequestPromise<V>>) (List<?>) deployer$promisesGeneric.get(type).get(type.valueHandler().fromValue(entry));
        if (list == null)
            return;

        for (Iterator<GenericRequestPromise<V>> iterator = list.iterator(); iterator.hasNext();) {
            GenericRequestPromise<V> requestPromise = iterator.next();
            if(!type.valueHandler().equalsIgnoreCount(requestPromise.promisedStack, entry)) continue;

            int toSubtract = Math.min(amount, type.valueHandler().getCount(requestPromise.promisedStack));
            amount -= toSubtract;
            type.valueHandler().setCount(requestPromise.promisedStack, type.valueHandler().getCount(requestPromise.promisedStack) - toSubtract);

            if (type.valueHandler().getCount(requestPromise.promisedStack) <= 0) {
                iterator.remove();
                onChanged.run();
            }
            if (amount <= 0)
                break;
        }

        if(list.isEmpty())
            deployer$promisesGeneric.get(type).remove(type.valueHandler().fromValue(entry));
    }

    @Override
    public <K,V,H> List<GenericRequestPromise<V>> deployer$flatten(StockInventoryType<K,V,H> type, boolean sorted) {
        List<GenericRequestPromise<V>> all = new ArrayList<>();
        //noinspection unchecked,RedundantCast
        deployer$promisesGeneric.get(type).forEach((key, list) -> all.addAll((Collection<? extends GenericRequestPromise<V>>)(Collection<?>) list));
        if (sorted)
            all.sort(GenericRequestPromise.ageComparator());
        return all;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void write(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> cir) {
        var tag = cir.getReturnValue();
        var special = new CompoundTag();
        for(var type : DeployerRegistries.STOCK_INVENTORY)
            //noinspection DataFlowIssue
            special.put(DeployerRegistries.STOCK_INVENTORY.getKey(type).toString(), deployer$writeSingle(type, registries));
        tag.put("deployer$ExtraLists", special);
    }

    @Unique
    private <K,V,H> ListTag deployer$writeSingle(StockInventoryType<K,V,H> type, HolderLookup.Provider registries) {
        ListTag tagList = new ListTag();
        for(GenericRequestPromise<V> promise : deployer$flatten(type, false))
            tagList.add(CatnipCodecUtils.encode(type.networkHandler().requestCodec(), registries, promise).orElseThrow());
        return tagList;
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void read(CompoundTag tag, HolderLookup.Provider registries, Runnable onChanged, CallbackInfoReturnable<RequestPromiseQueue> cir) {
        var special = tag.getCompound("deployer$ExtraLists");
        RPQExtension result = (RPQExtension) cir.getReturnValue();
        for(var type : DeployerRegistries.STOCK_INVENTORY) {
            ListTag tagList = special.getList(String.valueOf(DeployerRegistries.STOCK_INVENTORY.getKey(type)), Tag.TAG_END); //TODO: Check if TAG_END is correct
            deployer$readSingle(type, tagList, result);
        }
    }

    @Unique
    private static <K,V,H> void deployer$readSingle(StockInventoryType<K,V,H> type, ListTag tagList, RPQExtension output) {
        for(Tag tag : tagList) {
            var elem = CatnipCodecUtils.decode(type.networkHandler().requestCodec(), tag).orElseThrow();
            output.deployer$add(type, elem);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci) {
        deployer$promisesGeneric
                .forEach((a,b) -> b
                        .forEach((c,d) -> d
                                .forEach(GenericRequestPromise::tick)));
    }

    @ModifyReturnValue(method = "isEmpty", at = @At("RETURN"))
    private boolean isEmpty(boolean original) {
        return original && deployer$promisesGeneric.values().stream().allMatch(Map::isEmpty);
    }
}
