package core.data.server;

import com.mojang.datafixers.util.Pair;
import com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSliceProvider;
import com.teamabnormals.blueprint.core.Blueprint;
import com.teamabnormals.blueprint.core.registry.BlueprintBiomes;
import com.teamabnormals.blueprint.core.util.BiomeUtil;
import com.teamabnormals.blueprint.core.util.modification.selection.ConditionedResourceSelector;
import com.teamabnormals.blueprint.core.util.modification.selection.selectors.NamesResourceSelector;
import core.BlueprintTest;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.CheckerboardColumnBiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;

import java.util.List;

public final class TestModdedBiomeSlicesProvider extends ModdedBiomeSliceProvider {

	public TestModdedBiomeSlicesProvider(DataGenerator dataGenerator) {
		super(dataGenerator, BlueprintTest.MOD_ID);
	}

	@Override
	protected void registerSlices() {
		Climate.Parameter zero = Climate.Parameter.point(0.0F);
		RegistryAccess.Frozen registryAccess = RegistryAccess.BUILTIN.get();
		var biomes = registryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
		this.registerSlice(
				"end_checkerboard",
				new ConditionedResourceSelector(new NamesResourceSelector("minecraft:the_end"), new ModLoadedCondition(Blueprint.MOD_ID)),
				5,
				new BiomeUtil.OverlayModdedBiomeProvider(
						List.of(
								Pair.of(
										biomes.getOrCreateTag(BiomeTags.HAS_END_CITY),
										new CheckerboardColumnBiomeSource(biomes.getOrCreateTag(BiomeTags.HAS_STRONGHOLD), 2)
								)
						)
				)
		);
		this.registerSlice("nether_test", 4, new BiomeUtil.MultiNoiseModdedBiomeProvider(new Climate.ParameterList<>(List.of(Pair.of(Climate.parameters(0, 0, 0, 0, 0, 0, 0), Biomes.FOREST)))), new ResourceLocation("the_nether"));
		this.registerSlice("overworld_crimson_forest_caves", 4, new BiomeUtil.MultiNoiseModdedBiomeProvider(new Climate.ParameterList<>(List.of(Pair.of(Climate.parameters(0, 0, 0, 0, 0, 0, 0), BlueprintBiomes.ORIGINAL_SOURCE_MARKER.getKey()), Pair.of(Climate.parameters(zero, zero, zero, zero, Climate.Parameter.span(0.3F, 1.0F), zero, 0.0F), Biomes.CRIMSON_FOREST)))), new ResourceLocation("overworld"));
	}

}
