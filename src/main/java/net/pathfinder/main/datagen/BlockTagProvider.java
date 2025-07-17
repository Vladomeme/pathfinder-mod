package net.pathfinder.main.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.client.v1.ClientTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

/**
 * Generates block tag files required for finding suitable path positions.
 */
public class BlockTagProvider extends FabricTagProvider.BlockTagProvider {

    public static final TagKey<Block> PASSABLE = TagKey.of(RegistryKeys.BLOCK, Identifier.of("pathfinder", "passable"));
    public static final TagKey<Block> CARPETS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("pathfinder", "carpets"));
    public static final TagKey<Block> DANGEROUS = TagKey.of(RegistryKeys.BLOCK, Identifier.of("pathfinder", "dangerous"));

    public BlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(PASSABLE)
                .forceAddTag(BlockTags.WOOL_CARPETS)
                .forceAddTag(BlockTags.DOORS)
                .forceAddTag(BlockTags.TRAPDOORS)
                .forceAddTag(BlockTags.FENCE_GATES)
                .add(Blocks.MOSS_CARPET)
                .add(Blocks.SNOW)
                .add(Blocks.LADDER);

        getOrCreateTagBuilder(CARPETS)
                .forceAddTag(BlockTags.WOOL_CARPETS)
                .add(Blocks.MOSS_CARPET)
                .add(Blocks.SNOW);

        getOrCreateTagBuilder(DANGEROUS)
                .forceAddTag(BlockTags.CAMPFIRES)
                .forceAddTag(BlockTags.FIRE)
                .add(Blocks.LAVA);
    }

    public static void registerOnClient() {
        ClientTags.getOrCreateLocalTag(PASSABLE);
        ClientTags.getOrCreateLocalTag(CARPETS);
        ClientTags.getOrCreateLocalTag(DANGEROUS);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public static boolean isIn(TagKey<Block> tag, BlockState state) {
        return ClientTags.isInLocal(tag, state.getRegistryEntry().getKey().get());
    }
}
