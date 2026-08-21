package com.dungeoncraft;

import com.dungeoncraft.block.DCPortalBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(DungeonCraft.MOD_ID)
public final class DungeonCraft {
    public static final String MOD_ID = "dungeoncraft";
    public static final Logger LOGGER = LogUtils.getLogger();

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
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(DC_PORTAL_ITEM);
        }
    }
}
