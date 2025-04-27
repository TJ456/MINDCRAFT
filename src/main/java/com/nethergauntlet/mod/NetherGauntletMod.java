package com.nethergauntlet.mod;

import com.nethergauntlet.mod.init.ItemInit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;

@Mod(NetherGauntletMod.MOD_ID)
public class NetherGauntletMod {
    public static final String MOD_ID = "nethergauntlet";

    public NetherGauntletMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register our items
        ItemInit.ITEMS.register(modEventBus);
        
        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    // Creative tab for our mod
    public static final CreativeModeTab NETHER_GAUNTLET_TAB = new CreativeModeTab(MOD_ID) {
        @Override
        public @NotNull ItemStack makeIcon() {
            return new ItemStack(ItemInit.NETHER_GAUNTLET.get());
        }
    };
}
