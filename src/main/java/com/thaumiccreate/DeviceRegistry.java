package com.thaumiccreate;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

public final class DeviceRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK, ThaumicCreate.ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, ThaumicCreate.ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, ThaumicCreate.ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(net.minecraft.core.registries.Registries.FLUID, ThaumicCreate.ID);
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES, ThaumicCreate.ID);
    public static final Supplier<EssentiaEngineBlock> ENGINE = BLOCKS.register("essentia_engine", EssentiaEngineBlock::new);
    public static final Supplier<Item> ENGINE_ITEM = ITEMS.register("essentia_engine", () -> new BlockItem(ENGINE.get(), new Item.Properties()));
    public static final Supplier<BlockEntityType<EssentiaEngineBlockEntity>> ENGINE_ENTITY = ENTITIES.register("essentia_engine", () -> BlockEntityType.Builder.of(EssentiaEngineBlockEntity::new, ENGINE.get()).build(null));
    public static final Supplier<EssentiaTankBlock> TANK = BLOCKS.register("essentia_tank", EssentiaTankBlock::new);
    public static final Supplier<EssentiaAdapterBlock> ADAPTER = BLOCKS.register("essentia_adapter", EssentiaAdapterBlock::new);
    public static final Supplier<Item> TANK_ITEM = ITEMS.register("essentia_tank", () -> new BlockItem(TANK.get(), new Item.Properties()));
    public static final Supplier<Item> ADAPTER_ITEM = ITEMS.register("essentia_adapter", () -> new BlockItem(ADAPTER.get(), new Item.Properties()));
    public static final Supplier<BlockEntityType<EssentiaTankBlockEntity>> TANK_ENTITY = ENTITIES.register("essentia_tank", () -> BlockEntityType.Builder.of(EssentiaTankBlockEntity::new, TANK.get()).build(null));
    public static final Supplier<BlockEntityType<EssentiaAdapterBlockEntity>> ADAPTER_ENTITY = ENTITIES.register("essentia_adapter", () -> BlockEntityType.Builder.of(EssentiaAdapterBlockEntity::new, ADAPTER.get()).build(null));
    public static final Supplier<FluidType> ESSENTIA_TYPE = TYPES.register("essentia", () -> new FluidType(
        FluidType.Properties.create().descriptionId("fluid_type.thaumic_create.essentia").canSwim(false).canDrown(false).canHydrate(false).canConvertToSource(false)) {
        @Override public net.minecraft.world.level.block.state.BlockState getBlockForFluidState(net.minecraft.world.level.BlockAndTintGetter level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.material.FluidState state) { return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(); }
        @Override public net.minecraft.network.chat.Component getDescription(net.neoforged.neoforge.fluids.FluidStack stack) {
            String aspect = EssentiaFluid.aspect(stack);
            return net.minecraft.network.chat.Component.translatable("fluid_type.thaumic_create.essentia").append(aspect == null ? "" : ": " + aspect);
        }
    });
    public static final Supplier<Fluid> ESSENTIA = FLUIDS.register("essentia", () -> new BaseFlowingFluid.Source(new BaseFlowingFluid.Properties(ESSENTIA_TYPE, () -> DeviceRegistry.ESSENTIA.get(), () -> DeviceRegistry.ESSENTIA.get())));
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TYPES.register(bus); FLUIDS.register(bus);
        bus.addListener(DeviceRegistry::capabilities);
        bus.addListener(DeviceRegistry::creative);
    }
    private static void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            event.accept(ENGINE_ITEM.get()); event.accept(TANK_ITEM.get()); event.accept(ADAPTER_ITEM.get());
        }
    }
    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, TANK_ENTITY.get(), (be, side) -> {
            var controller = be.getControllerBE();
            return controller == null ? null : controller.getTankInventory();
        });
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, ADAPTER_ENTITY.get(),
            (be, side) -> side == be.front() ? be.pipe : null);
    }
}
