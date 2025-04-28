package com.nethergauntlet.mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetherGauntletItem extends Item {
    private static final int FIREBALL_COOLDOWN = 20; // 1 second cooldown
    private static final float EXPLOSION_POWER = 1.5F; // Smaller than TNT but still effective
    private static final int LAVA_SURF_DURATION = 5; // Duration in seconds for lava surfing effect
    private static final int PARTICLE_FREQUENCY = 5; // How often to spawn particles (ticks)
    private static final float PARTICLE_SPEED = 0.1F; // Speed of particles
    private static final int EMPOWERMENT_DURATION = 200; // 10 seconds of empowerment
    private static final int NETHER_TELEPORT_COOLDOWN = 600; // 30 seconds cooldown for nether teleportation

    // Track the empowerment state for each player - using UUID for persistence when players disconnect/reconnect
    private static final Map<UUID, Integer> empowermentTicks = new ConcurrentHashMap<>();
    // Track the teleport cooldown for each player
    private static final Map<UUID, Integer> teleportCooldowns = new ConcurrentHashMap<>();
    // Track charged state for special ability
    private static final Map<UUID, Boolean> isCharged = new ConcurrentHashMap<>();

    public NetherGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§6A powerful gauntlet infused with the essence of the Nether"));
        tooltip.add(Component.literal("§e• Left click to create explosions"));
        tooltip.add(Component.literal("§e• Right click to shoot fireballs"));
        tooltip.add(Component.literal("§e• Surf on lava"));
        tooltip.add(Component.literal("§e• Shift + Right click to create a ring of fire"));
        tooltip.add(Component.literal("§e• Sneak for 5 seconds near lava to charge, then use special ability"));
        tooltip.add(Component.literal("§c• When charged: Crouch + Right-click to teleport between dimensions"));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = attacker.level;

        // Create explosion when hitting enemies
        if (!level.isClientSide) {
            boolean isEmpowered = attacker instanceof Player && 
                                 empowermentTicks.getOrDefault(((Player) attacker).getUUID(), 0) > 0;

            // Enhanced explosion if empowered
            float power = isEmpowered ? EXPLOSION_POWER * 2.0F : EXPLOSION_POWER;

            level.explode(attacker, target.getX(), target.getY(), target.getZ(),
                    power, Explosion.BlockInteraction.NONE);

            // Play explosion sound
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.0F);

            // Extra effect when empowered: Set enemies on fire and apply weakness
            if (isEmpowered) {
                target.setSecondsOnFire(10);
                if (target instanceof LivingEntity) {
                    ((LivingEntity) target).addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                }

                // Chance to summon a friendly Blaze that will attack your enemies
                if (level.random.nextFloat() < 0.25f) {
                    Blaze blaze = EntityType.BLAZE.create(level);
                    if (blaze != null) {
                        blaze.setPos(target.getX(), target.getY() + 1, target.getZ());
                        blaze.setCustomName(Component.literal("Nether Guardian"));
                        level.addFreshEntity(blaze);

                        // Make the blaze temporary (will despawn after 30 seconds)
                        blaze.getPersistentData().putBoolean("NetherGauntletSummon", true);
                        blaze.getPersistentData().putLong("DespawnTime", level.getGameTime() + 600L);

                        // Blaze should target the entity you attacked
                        if (target instanceof LivingEntity) {
                            blaze.setLastHurtByMob((LivingEntity) target);
                        }
                    }
                }
            }

            // Damage the item
            stack.hurtAndBreak(1, attacker, (entity) -> entity.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        } else {
            // Client-side particle effects
            RandomSource random = level.getRandom();

            // More particles if empowered
            boolean isEmpowered = attacker instanceof Player && 
                                 empowermentTicks.getOrDefault(((Player) attacker).getUUID(), 0) > 0;
            int particleCount = isEmpowered ? 40 : 20;

            for (int i = 0; i < particleCount; i++) {
                double offsetX = random.nextGaussian() * 0.2;
                double offsetY = random.nextGaussian() * 0.2;
                double offsetZ = random.nextGaussian() * 0.2;

                level.addParticle(
                    ParticleTypes.FLAME,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    offsetX, offsetY + 0.1, offsetZ
                );

                level.addParticle(
                    isEmpowered ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.LAVA,
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

        // Handle nether teleportation when charged and sneaking
        if (player.isCrouching() && isCharged.getOrDefault(player.getUUID(), false)) {
            if (!level.isClientSide) {
                if (teleportCooldowns.getOrDefault(player.getUUID(), 0) <= 0) {
                    // Start teleportation
                    handleNetherTeleport(player, level);

                    // Reset charged state
                    isCharged.put(player.getUUID(), false);

                    // Set cooldown
                    teleportCooldowns.put(player.getUUID(), NETHER_TELEPORT_COOLDOWN);

                    // Damage the item more for this powerful ability
                    stack.hurtAndBreak(20, player, (entity) -> entity.broadcastBreakEvent(hand));

                    return InteractionResultHolder.success(stack);
                } else {
                    // Inform player of remaining cooldown
                    int remainingSeconds = teleportCooldowns.get(player.getUUID()) / 20;
                    player.displayClientMessage(Component.literal("§cTeleportation on cooldown for " +
                                              remainingSeconds + " more seconds"), true);
                }
            }
            return InteractionResultHolder.fail(stack);
        }

        // Check if player is on cooldown for normal attacks
        if (!player.getCooldowns().isOnCooldown(this)) {
            // If player is holding shift, create a ring of fire instead of a fireball
            if (player.isCrouching()) {
                if (!level.isClientSide) {
                    createRingOfFire(level, player);

                    // Play sound
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.8F);

                    // Set cooldown (longer for this special attack)
                    player.getCooldowns().addCooldown(this, FIREBALL_COOLDOWN * 3);

                    // Damage the item
                    stack.hurtAndBreak(5, player, (entity) -> entity.broadcastBreakEvent(hand));
                } else {
                    // Client-side ring particles
                    createRingOfFireParticles(level, player);
                }

                return InteractionResultHolder.success(stack);
            }

            Vec3 look = player.getLookAngle();
            boolean isEmpowered = empowermentTicks.getOrDefault(player.getUUID(), 0) > 0;

            // Launch fireball
            if (!level.isClientSide) {
                // Create multiple fireballs if empowered
                int fireballCount = isEmpowered ? 3 : 1;

                for (int i = 0; i < fireballCount; i++) {
                    SmallFireball fireball = new SmallFireball(
                            level,
                            player,
                            look.x + (i == 0 ? 0 : (i == 1 ? 0.1 : -0.1)),
                            look.y,
                            look.z + (i == 0 ? 0 : (i == 1 ? 0.1 : -0.1)));

                    // Position the fireball in front of the player
                    fireball.setPos(
                            player.getX() + look.x * 1.5,
                            player.getY() + player.getEyeHeight() - 0.1,
                            player.getZ() + look.z * 1.5);

                    // Make empowered fireballs more powerful
                    if (isEmpowered) {
                        fireball.getPersistentData().putBoolean("EmpoweredFireball", true);
                    }

                    level.addFreshEntity(fireball);
                }

                // Play fireball sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

                // Set cooldown (reduced when empowered)
                player.getCooldowns().addCooldown(this, isEmpowered ? FIREBALL_COOLDOWN / 2 : FIREBALL_COOLDOWN);

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
                        isEmpowered ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
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

    // Create a ring of fire around the player
    private void createRingOfFire(Level level, Player player) {
        int radius = 3;
        BlockPos playerPos = player.blockPosition();
        boolean isEmpowered = empowermentTicks.getOrDefault(player.getUUID(), 0) > 0;

        // Larger and more powerful ring when empowered
        if (isEmpowered) {
            radius = 5;
        }

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Skip the center where the player is standing
                if (x == 0 && z == 0) continue;

                // Create a circle/ring
                if (x*x + z*z <= radius*radius && x*x + z*z >= (radius-1)*(radius-1)) {
                    BlockPos firePos = playerPos.offset(x, 0, z);
                    BlockPos belowPos = firePos.below();

                    // Only place fire if there's a solid block below and air at the position
                    if (level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP) &&
                        level.getBlockState(firePos).isAir()) {

                        // Place fire block
                        level.setBlockAndUpdate(firePos, Blocks.FIRE.defaultBlockState());

                        // If empowered, damage and push back nearby enemies
                        if (isEmpowered) {
                            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                                                    new AABB(firePos).inflate(1.5))) {
                                if (entity != player) {
                                    entity.hurt(DamageSource.IN_FIRE, 4);

                                    // Push entities away from center
                                    Vec3 pushDir = entity.position().subtract(player.position()).normalize();
                                    entity.push(pushDir.x * 1.5, 0.5, pushDir.z * 1.5);

                                    // Set on fire
                                    entity.setSecondsOnFire(5);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create particle effects for the ring of fire
    private void createRingOfFireParticles(Level level, Player player) {
        int radius = 3;
        boolean isEmpowered = empowermentTicks.getOrDefault(player.getUUID(), 0) > 0;

        // Larger ring when empowered
        if (isEmpowered) {
            radius = 5;
        }

        RandomSource random = level.getRandom();

        // Create particles in a ring around the player
        for (int i = 0; i < 60; i++) {
            double angle = 2 * Math.PI * random.nextDouble();
            double distance = (radius - 0.5) + random.nextDouble();

            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;

            level.addParticle(
                isEmpowered ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                player.getX() + x,
                player.getY() + 0.1,
                player.getZ() + z,
                0, 0.1, 0
            );

            // Add extra soul particles for empowered
            if (isEmpowered && i % 3 == 0) {
                level.addParticle(
                    ParticleTypes.SOUL,
                    player.getX() + x,
                    player.getY() + 0.5,
                    player.getZ() + z,
                    0, 0.05, 0
                );
            }
        }
    }

    // Handle dimensional teleportation
    private void handleNetherTeleport(Player player, Level level) {
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerLevel destination;

            // If in the nether, go to the overworld. If in the overworld, go to the nether.
            if (level.dimension().equals(Level.NETHER)) {
                destination = serverPlayer.server.getLevel(Level.OVERWORLD);
                player.displayClientMessage(Component.literal("§6Returning to the Overworld..."), true);
            } else {
                destination = serverPlayer.server.getLevel(Level.NETHER);
                player.displayClientMessage(Component.literal("§cEntering the Nether..."), true);
            }

            if (destination != null) {
                // Store original position to calculate appropriate destination
                double scale = level.dimension().equals(Level.NETHER) ? 8.0 : 0.125;

                // Calculate target position
                BlockPos targetPos = new BlockPos(
                    player.getX() * scale,
                    player.getY(),
                    player.getZ() * scale
                );

                // Find a safe spot to teleport to
                BlockPos safePos = findSafeSpot(destination, targetPos);

                // Teleport player
                serverPlayer.teleportTo(destination,
                    safePos.getX() + 0.5,
                    safePos.getY(),
                    safePos.getZ() + 0.5,
                    player.getYRot(),
                    player.getXRot());

                // Play teleport sound
                destination.playSound(null, safePos,
                    SoundEvents.PORTAL_TRAVEL,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F);

                // Create portal effect particles
                for(int i = 0; i < 50; i++) {
                    destination.sendParticles(
                        ParticleTypes.PORTAL,
                        safePos.getX() + 0.5,
                        safePos.getY() + 1.0,
                        safePos.getZ() + 0.5,
                        1,
                        destination.random.nextGaussian() * 0.5,
                        destination.random.nextGaussian() * 0.5,
                        destination.random.nextGaussian() * 0.5,
                        0.5
                    );
                }

                // Grant temporary fire resistance after teleportation
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
            }
        }
    }

    // Find a safe location to teleport to
    private BlockPos findSafeSpot(ServerLevel level, BlockPos targetPos) {
        // Start checking at the target position and work up and down
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(targetPos.getX(), targetPos.getY(), targetPos.getZ());

        // Check the target position first
        if (isSafeSpot(level, mutablePos)) {
            return mutablePos.immutable();
        }

        // Check above - more likely to find air in the Nether
        for (int y = 1; y < 20; y++) {
            mutablePos.set(targetPos.getX(), targetPos.getY() + y, targetPos.getZ());
            if (isSafeSpot(level, mutablePos)) {
                return mutablePos.immutable();
            }
        }

        // Check below
        for (int y = 1; y < 20; y++) {
            mutablePos.set(targetPos.getX(), targetPos.getY() - y, targetPos.getZ());
            if (isSafeSpot(level, mutablePos)) {
                return mutablePos.immutable();
            }
        }

        // If we couldn't find a safe spot, try a random offset
        for (int attempts = 0; attempts < 16; attempts++) {
            int offsetX = level.getRandom().nextInt(16) - 8;
            int offsetZ = level.getRandom().nextInt(16) - 8;

            for (int y = -10; y < 10; y++) {
                mutablePos.set(targetPos.getX() + offsetX, targetPos.getY() + y, targetPos.getZ() + offsetZ);
                if (isSafeSpot(level, mutablePos)) {
                    return mutablePos.immutable();
                }
            }
        }

        // If we still couldn't find a spot, just use the target position
        // The player might suffocate, but they have the gauntlet to help them out
        return targetPos;
    }

    // Check if a position is safe to teleport to (2 blocks of clearance)
    private boolean isSafeSpot(ServerLevel level, BlockPos pos) {
        // Need two blocks of clearance for the player
        boolean blockClear = level.getBlockState(pos).getMaterial().isReplaceable() &&
                           level.getBlockState(pos.above()).getMaterial().isReplaceable();

        // Need something solid below
        boolean hasSolidGround = !level.getBlockState(pos.below()).getMaterial().isReplaceable() ||
                                level.getBlockState(pos.below()).getMaterial() == Material.LAVA;

        // Check for dangerous blocks
        boolean isDangerous = level.getBlockState(pos).is(Blocks.LAVA) ||
                           level.getBlockState(pos).is(Blocks.FIRE) ||
                           level.getBlockState(pos.above()).is(Blocks.LAVA) ||
                           level.getBlockState(pos.above()).is(Blocks.FIRE);

        return blockClear && hasSolidGround && !isDangerous;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // Check if entity is a player
        if (entity instanceof Player) {
            Player player = (Player) entity;
            boolean holdingGauntlet = isSelected || player.getOffhandItem() == stack;

            // Update cooldowns
            if (!level.isClientSide) {
                // Update teleport cooldown
                if (teleportCooldowns.containsKey(player.getUUID()) && teleportCooldowns.get(player.getUUID()) > 0) {
                    teleportCooldowns.put(player.getUUID(), teleportCooldowns.get(player.getUUID()) - 1);
                }

                // Update empowerment
                if (empowermentTicks.containsKey(player.getUUID()) && empowermentTicks.get(player.getUUID()) > 0) {
                    empowermentTicks.put(player.getUUID(), empowermentTicks.get(player.getUUID()) - 1);

                    // Notify player when empowerment ends
                    if (empowermentTicks.get(player.getUUID()) == 0) {
                        player.displayClientMessage(Component.literal("§cThe gauntlet's power fades..."), true);
                    }
                }
            }

            if (holdingGauntlet) {
                // Always make player fire resistant while holding the gauntlet
                player.setRemainingFireTicks(0);

                // Check for charging near lava when sneaking
                if (player.isCrouching() && !isCharged.getOrDefault(player.getUUID(), false)) {
                    // Check if player is near lava (within 3 blocks)
                    boolean nearLava = false;
                    BlockPos playerPos = player.blockPosition();

                    for (int x = -3; x <= 3 && !nearLava; x++) {
                        for (int y = -3; y <= 3 && !nearLava; y++) {
                            for (int z = -3; z <= 3 && !nearLava; z++) {
                                BlockPos checkPos = playerPos.offset(x, y, z);
                                if (level.getBlockState(checkPos).getMaterial() == Material.LAVA) {
                                    nearLava = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (nearLava) {
                        // Track charging progress
                        if (!level.isClientSide) {
                            // Increment a charging counter in player's persistent data
                            int chargingTicks = player.getPersistentData().getInt("GauntletChargingTicks");
                            chargingTicks++;
                            player.getPersistentData().putInt("GauntletChargingTicks", chargingTicks);

                            // Every second, show charging progress
                            if (chargingTicks % 20 == 0) {
                                int seconds = chargingTicks / 20;
                                if (seconds < 5) {
                                    player.displayClientMessage(Component.literal("§6Charging: " + seconds + "/5 seconds"), true);
                                } else {
                                    // Fully charged after 5 seconds
                                    player.displayClientMessage(Component.literal("§6The gauntlet is charged with nether energy!"), true);
                                    isCharged.put(player.getUUID(), true);
                                }
                            }
                        } else {
                            // Client-side charging particles
                            RandomSource random = level.getRandom();
                            if (level.getGameTime() % 5 == 0) {
                                for (int i = 0; i < 5; i++) {
                                    level.addParticle(
                                        ParticleTypes.LAVA,
                                        player.getX() + random.nextGaussian() * 0.5,
                                        player.getY() + random.nextGaussian() * 0.5,
                                        player.getZ() + random.nextGaussian() * 0.5,
                                        0, 0, 0
                                    );
                                }
                            }
                        }
                    } else {
                        // Reset charging progress when not near lava
                        player.getPersistentData().putInt("GauntletChargingTicks", 0);
                    }
                } else if (!player.isCrouching()) {
                    // Reset charging progress when not sneaking
                    player.getPersistentData().putInt("GauntletChargingTicks", 0);
                }

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

                    // Choose particle type based on state
                    ParticleOptions particleType = ParticleTypes.SMALL_FLAME;
                    if (isCharged.getOrDefault(player.getUUID(), false)) {
                        particleType = ParticleTypes.SOUL_FIRE_FLAME;
                    } else if (empowermentTicks.getOrDefault(player.getUUID(), 0) > 0) {
                        particleType = ParticleTypes.FLAME;
                    }

                    // Add ambient particles
                    for (int i = 0; i < 2; i++) {
                        double offsetX = random.nextGaussian() * 0.1;
                        double offsetY = random.nextGaussian() * 0.1;
                        double offsetZ = random.nextGaussian() * 0.1;

                        level.addParticle(
                            particleType,
                            handX, handY, handZ,
                            offsetX * PARTICLE_SPEED,
                            offsetY * PARTICLE_SPEED + 0.05,
                            offsetZ * PARTICLE_SPEED
                        );
                    }

                    // Extra particles for special states
                    if (isCharged.getOrDefault(player.getUUID(), false) && random.nextBoolean()) {
                        level.addParticle(
                            ParticleTypes.SOUL,
                            handX, handY, handZ,
                            random.nextGaussian() * 0.02,
                            0.1 + random.nextGaussian() * 0.02,
                            random.nextGaussian() * 0.02
                        );
                    }
                }

                // Detect when player is near lava block and offer empowerment
                if (!level.isClientSide && level.getGameTime() % 20 == 0) {  // Once per second
                    if (empowermentTicks.getOrDefault(player.getUUID(), 0) <= 0) {  // Not already empowered
                        BlockPos belowPos = player.blockPosition().below();

                        // If standing on netherrack near lava, offer empowerment
                        boolean onNetherrack = level.getBlockState(belowPos).is(Blocks.NETHERRACK);
                        boolean nearLava = false;

                        if (onNetherrack) {
                            // Search for lava in a 3x3x3 area
                            for (int x = -2; x <= 2 && !nearLava; x++) {
                                for (int y = -2; y <= 2 && !nearLava; y++) {
                                    for (int z = -2; z <= 2 && !nearLava; z++) {
                                        if (level.getBlockState(belowPos.offset(x, y, z)).getMaterial() == Material.LAVA) {
                                            nearLava = true;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (nearLava && level.getRandom().nextFloat() < 0.4f) {  // 40% chance
                                // Empower the gauntlet
                                empowermentTicks.put(player.getUUID(), EMPOWERMENT_DURATION);
                                player.displayClientMessage(Component.literal("§6The gauntlet draws power from the nearby lava!"), true);

                                // Play empowerment sound
                                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                        SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.0F, 0.8F);
                            }
                        }
                    }
                }

                // Check if player is standing on lava
                BlockPos pos = player.blockPosition().below();

                if (level.getBlockState(pos).getMaterial() == Material.LAVA) {
                    // Allow player to "surf" on lava
                    player.fallDistance = 0.0F;

                    // Faster surfing when empowered
                    double speedMultiplier = empowermentTicks.getOrDefault(player.getUUID(), 0) > 0 ? 1.3 : 1.1;
                    player.setDeltaMovement(player.getDeltaMovement().multiply(speedMultiplier, 0, speedMultiplier));

                    // Visual effect - create temporary obsidian under player
                    if (!level.isClientSide && level.getGameTime() % 10 == 0) {
                        BlockPos tempBlock = player.blockPosition().below();
                        if (level.getBlockState(tempBlock).getMaterial() == Material.LAVA) {
                            // Create obsidian or magma block based on empowerment
                            BlockState blockState = empowermentTicks.getOrDefault(player.getUUID(), 0) > 0 ?
                                                Blocks.MAGMA_BLOCK.defaultBlockState() :
                                                Blocks.OBSIDIAN.defaultBlockState();

                            level.setBlockAndUpdate(tempBlock, blockState);

                            // Schedule the block to return to lava
                            level.scheduleTick(tempBlock, blockState.getBlock(), 20 * LAVA_SURF_DURATION);
                        }
                    }

                    // Add lava surfing particles
                    if (level.isClientSide && level.getGameTime() % 5 == 0) {
                        RandomSource random = level.getRandom();
                        for (int i = 0; i < 5; i++) {
                            double offsetX = random.nextGaussian() * 0.2;
                            double offsetZ = random.nextGaussian() * 0.2;

                            level.addParticle(
                                empowermentTicks.getOrDefault(player.getUUID(), 0) > 0 ?
                                    ParticleTypes.FLAME : ParticleTypes.LAVA,
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
        }

        super.inventoryTick(stack, level, entity, slotId, isSelected);

        // Special code to handle summoned blazes despawning
        if (!level.isClientSide && entity.getType() == EntityType.BLAZE) {
            if (entity.getPersistentData().contains("NetherGauntletSummon")) {
                long despawnTime = entity.getPersistentData().getLong("DespawnTime");
                if (level.getGameTime() >= despawnTime) {
                    entity.discard(); // Remove the entity when it's time
                }
            }
        }
    }
}
