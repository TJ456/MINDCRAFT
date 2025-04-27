package com.nethergauntlet.mod.init;

import com.nethergauntlet.mod.NetherGauntletMod;
import com.nethergauntlet.mod.item.NetherGauntletItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit {
    // Create a Deferred Register for items
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NetherGauntletMod.MOD_ID);
    
    // Register the Nether Gauntlet item
    public static final RegistryObject<Item> NETHER_GAUNTLET = ITEMS.register("nether_gauntlet",
            () -> new NetherGauntletItem(new Item.Properties().tab(NetherGauntletMod.NETHER_GAUNTLET_TAB).fireResistant().durability(500)));
}
