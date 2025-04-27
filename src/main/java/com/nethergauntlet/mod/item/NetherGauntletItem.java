package com.nethergauntlet.mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

public class NetherGauntletItem extends Item {
    private static final int FIREBALL_COOLDOWN = 20; // 1 second cooldown
    private static final float EXPLOSION_POWER = 1.5F; // Smaller than TNT but still effective
    private static final int LAVA_SURF_DURATION = 5; // Duration in seconds for lava surfing effect

    public NetherGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level;
        
        // Create explosion when hitting enemies
        if (!level.isClientSide) {
            level.explode(attacker, target.getX(), target.getY(), target.getZ(), 
                    EXPLOSION_POWER, Explosion.BlockInteraction.NONE);
            
            // Play explosion sound
            level.playSound(null, target.getX(), target.getY(), target.getZ(), 
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.0F);
            
            // Damage the item
            stack.hurtAndBreak(1, attacker, (entity) -> entity.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
        
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        // Check if player is on cooldown
        if (!player.getCooldowns().isOnCooldown(this)) {
            // Launch fireball
            if (!level.isClientSide) {
                Vec3 look = player.getLookAngle();
                SmallFireball fireball = new SmallFireball(
                        level, 
                        player, 
                        look.x, look.y, look.z);
                
                // Position the fireball in front of the player
                fireball.setPos(
                        player.getX() + look.x * 1.5, 
                        player.getY() + player.getEyeHeight() - 0.1, 
                        player.getZ() + look.z * 1.5);
                
                level.addFreshEntity(fireball);
                
                // Play fireball sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                
                // Set cooldown
                player.getCooldowns().addCooldown(this, FIREBALL_COOLDOWN);
                
                // Damage the item
                stack.hurtAndBreak(2, player, (entity) -> entity.broadcastBreakEvent(hand));
            }
            
            return InteractionResultHolder.success(stack);
        }
        
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Check if entity is a player and the item is selected
        if (entity instanceof Player player && (isSelected || player.getOffhandItem() == stack)) {
            // Check if player is standing on lava
            BlockPos pos = player.blockPosition().below();
            
            if (level.getBlockState(pos).getMaterial() == Material.LAVA) {
                // Allow player to "surf" on lava
                player.fallDistance = 0.0F;
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.1, 0, 1.1));
                
                // Make player fire resistant while on lava
                player.setRemainingFireTicks(0);
                
                // Visual effect - create temporary obsidian under player
                if (!level.isClientSide && level.getGameTime() % 10 == 0) {
                    BlockPos tempBlock = player.blockPosition().below();
                    if (level.getBlockState(tempBlock).getMaterial() == Material.LAVA) {
                        // Remember the original block
                        level.setBlockAndUpdate(tempBlock, Blocks.OBSIDIAN.defaultBlockState());
                        
                        // Schedule the block to return to lava
                        level.scheduleTick(tempBlock, Blocks.OBSIDIAN, 20 * LAVA_SURF_DURATION);
                    }
                }
                
                // Damage the item slightly when surfing on lava
                if (!level.isClientSide && level.getGameTime() % 40 == 0) {
                    stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
                }
            }
        }
        
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
