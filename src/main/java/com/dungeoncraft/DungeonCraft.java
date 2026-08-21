package com.dungeoncraft;

import com.dungeoncraft.block.DCPortalBlock;
import com.dungeoncraft.config.DungeonGenerationConfig;
import com.dungeoncraft.event.DungeonInteractionEvents;
import com.dungeoncraft.network.DungeonNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(DungeonCraft.MOD_ID)
public final class DungeonCraft {
    public static final String MOD_ID = "dungeoncraft";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceKey<Level> DUNGEON_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(MOD_ID, "dungeon"));

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    public static final DeferredBlock<DCPortalBlock> DC_PORTAL = BLOCKS.registerBlock(
            "dc_portal",
            DCPortalBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PODZOL)
                    .strength(2.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .lightLevel(state -> 15));

    public static final DeferredItem<BlockItem> DC_PORTAL_ITEM = ITEMS.registerSimpleBlockItem(DC_PORTAL);

    public DungeonCraft(IEventBus modBus, ModContainer container) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(this::addCreativeTabContents);
        modBus.addListener(DungeonNetwork::registerPayloads);
        container.registerConfig(ModConfig.Type.SERVER, DungeonGenerationConfig.SPEC);
        NeoForge.EVENT_BUS.register(DungeonInteractionEvents.class);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(DC_PORTAL_ITEM);
        }
    }
}
