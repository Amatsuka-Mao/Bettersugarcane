package com.ran.bettersugarcane;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class bettersugarcane implements ModInitializer {
    private static final Random RANDOM = new Random();

    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) {
                return ActionResult.PASS;
            }

            ItemStack itemStack = player.getStackInHand(hand);
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();


            // 确保使用的是骨粉，并且目标方块是甘蔗
            if (block == Blocks.SUGAR_CANE && itemStack.isOf(Items.BONE_MEAL)) {
                ActionResult result = tryGrowSugarCane(world, pos);
                if (result == ActionResult.SUCCESS) {
                    // **消耗骨粉**
                    if (!player.isCreative()) {
                        itemStack.decrement(1);
                    }
                }
                return result;
            }
            return ActionResult.PASS;
        });

        // 监听甘蔗破坏
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient && state.getBlock() == Blocks.SUGAR_CANE) {
                handleFortuneEffect((ServerWorld) world, player, pos);
            }
            return true;
        });
    }

    /**
     * 让甘蔗随机生长（类似于普通作物催熟）
     */
    private ActionResult tryGrowSugarCane(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        int currentHeight = 1;
        BlockPos down = pos.down();
        if (serverWorld.getBlockState(down).isOf(Blocks.SUGAR_CANE)) {
            currentHeight = 2;
        }
        BlockPos down1 = down.down();
        if (serverWorld.getBlockState(down1).isOf(Blocks.SUGAR_CANE)) {
            currentHeight = 3;
        }

        // **生长逻辑**
        BlockPos newSugarCanePos = pos.up(1); // 确保新甘蔗放置在当前甘蔗的顶部

        if (currentHeight == 3) {
            // **添加粒子效果**
            return getActionResult(serverWorld, newSugarCanePos, currentHeight);
        }
        BlockState aboveBlockState = serverWorld.getBlockState(newSugarCanePos);
        if (aboveBlockState.isAir()) {
            if (RANDOM.nextFloat() < 0.75f) { // **75% 概率成长**
                serverWorld.setBlockState(newSugarCanePos, Blocks.SUGAR_CANE.getDefaultState());
                // **添加粒子效果**
                return getActionResult(serverWorld, newSugarCanePos,currentHeight);
            }
        }
        return ActionResult.PASS;
    }

    @NotNull
    private ActionResult getActionResult(ServerWorld serverWorld, BlockPos newSugarCanePos, int currentHeight) {
        if (currentHeight == 3) {
            return ActionResult.PASS;
        }
        for (int i = 0; i < 10; i++) {
            double offsetX = serverWorld.random.nextDouble();
            double offsetY = serverWorld.random.nextDouble() * 0.5 + 0.5;
            double offsetZ = serverWorld.random.nextDouble();
            serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, newSugarCanePos.getX() + offsetX, newSugarCanePos.getY() + offsetY, newSugarCanePos.getZ() + offsetZ, 1, 0, 0, 0, 0);
        }
        return ActionResult.SUCCESS;
    }

    private static void handleFortuneEffect(ServerWorld world, PlayerEntity player, BlockPos pos) {
        ItemStack heldItem = player.getMainHandStack();
        int fortuneLevel = getFortuneLevel(world, heldItem);
        System.out.println("时运附魔等级：" + fortuneLevel);
        Random random = new Random();

        // 计算掉落数量（基础掉落 1，时运附魔增加）
        int dropAmount = 1;
        if (fortuneLevel > 0) {
            dropAmount += random.nextInt(fortuneLevel) + 1; // 例如时运 III 可能最多掉 4 个
            // 掉落额外的甘蔗
            for (int i = 0; i < dropAmount; i++) {
                Block.dropStack(world, pos, new ItemStack(Items.SUGAR_CANE));
            }
        }


    }

    public static int getFortuneLevel(ServerWorld server, ItemStack stack) {
        RegistryEntry<Enchantment> fortuneEntry = getEnchantmentEntry(server);
        return EnchantmentHelper.getLevel(fortuneEntry, stack);
    }

    private static RegistryEntry<Enchantment> getEnchantmentEntry(ServerWorld server) {
        return server.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT) // 获取附魔注册表
                .entryOf(Enchantments.FORTUNE); // 获取 RegistryEntry
    }
}
