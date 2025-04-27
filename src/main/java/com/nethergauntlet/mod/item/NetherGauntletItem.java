package com.nethergauntlet.mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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
    private static final int PARTICLE_FREQUENCY = 5; // How often to spawn particles (ticks)
    private static final float PARTICLE_SPEED = 0.1F; // Speed of particles

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
        } else {
            // Client-side particle effects
            RandomSource random = level.getRandom();
            for (int i = 0; i < 20; i++) {
                double offsetX = random.nextGaussian() * 0.2;
                double offsetY = random.nextGaussian() * 0.2;
                double offsetZ = random.nextGaussian() * 0.2;

                level.addParticle(
                    ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    offsetX, offsetY + 0.1, offsetZ
                );

                level.addParticle(
                    ParticleTypes.LAVA,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    offsetX * 0.5, offsetY * 0.5, offsetZ * 0.5
                );
            }
        }

        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Check if player is on cooldown
        if (!player.getCooldowns().isOnCooldown(this)) {
            Vec3 look = player.getLookAngle();

            // Launch fireball
            if (!level.isClientSide) {
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
            } else {
                // Client-side particle trail effect
                RandomSource random = level.getRandom();
                double startX = player.getX() + look.x * 0.5;
                double startY = player.getY() + player.getEyeHeight() - 0.1;
                double startZ = player.getZ() + look.z * 0.5;

                // Create a trail of particles in the direction the player is looking
                for (int i = 0; i < 15; i++) {
                    double trailX = startX + look.x * i * 0.3;
                    double trailY = startY + look.y * i * 0.3;
                    double trailZ = startZ + look.z * i * 0.3;

                    // Add some randomness to the trail
                    double offsetX = random.nextGaussian() * 0.05;
                    double offsetY = random.nextGaussian() * 0.05;
                    double offsetZ = random.nextGaussian() * 0.05;

                    // Fire particles
                    level.addParticle(
                        ParticleTypes.FLAME,
                        trailX, trailY, trailZ,
                        offsetX, offsetY, offsetZ
                    );

                    // Smoke particles
                    if (i % 2 == 0) {
                        level.addParticle(
                            ParticleTypes.SMOKE,
                            trailX, trailY, trailZ,
                            offsetX * 0.5, offsetY * 0.5, offsetZ * 0.5
                        );
                    }
                }
            }

            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Check if entity is a player and the item is selected
        if (entity instanceof Player player && (isSelected || player.getOffhandItem() == stack)) {
            // Always make player fire resistant while holding the gauntlet
            player.setRemainingFireTicks(0);

            // Add ambient particles around the gauntlet when selected
            if (level.isClientSide && level.getGameTime() % PARTICLE_FREQUENCY == 0) {
                RandomSource random = level.getRandom();

                // Determine which hand is holding the gauntlet
                InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;

                // Get hand position (approximate)
                double handX = player.getX();
                double handY = player.getY() + player.getEyeHeight() - 0.5;
                double handZ = player.getZ();

                // Offset based on which hand
                Vec3 look = player.getLookAngle();
                Vec3 right = new Vec3(look.z, 0, -look.x).normalize().scale(0.4);

                if (hand == InteractionHand.MAIN_HAND) {
                    handX += right.x;
                    handZ += right.z;
                } else {
                    handX -= right.x;
                    handZ -= right.z;
                }

                // Add ambient particles
                for (int i = 0; i < 2; i++) {
                    double offsetX = random.nextGaussian() * 0.1;
                    double offsetY = random.nextGaussian() * 0.1;
                    double offsetZ = random.nextGaussian() * 0.1;

                    level.addParticle(
                        ParticleTypes.SMALL_FLAME,
                        handX, handY, handZ,
                        offsetX * PARTICLE_SPEED,
                        offsetY * PARTICLE_SPEED + 0.05,
                        offsetZ * PARTICLE_SPEED
                    );
                }
            }

            // Check if player is standing on lava
            BlockPos pos = player.blockPosition().below();

            if (level.getBlockState(pos).getMaterial() == Material.LAVA) {
                // Allow player to "surf" on lava
                player.fallDistance = 0.0F;
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.1, 0, 1.1));

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

                // Add lava surfing particles
                if (level.isClientSide && level.getGameTime() % 5 == 0) {
                    RandomSource random = level.getRandom();
                    for (int i = 0; i < 5; i++) {
                        double offsetX = random.nextGaussian() * 0.2;
                        double offsetZ = random.nextGaussian() * 0.2;

                        level.addParticle(
                            ParticleTypes.LAVA,
                            player.getX() + offsetX,
                            player.getY(),
                            player.getZ() + offsetZ,
                            0, 0.1, 0
                        );
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
