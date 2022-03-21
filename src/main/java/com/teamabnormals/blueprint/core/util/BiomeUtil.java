package com.teamabnormals.blueprint.core.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teamabnormals.blueprint.core.Blueprint;
import com.teamabnormals.blueprint.core.util.registry.BasicRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A utility class for biomes.
 *
 * @author bageldotjpg
 * @author SmellyModder (Luke Tonon)
 * @author ExpensiveKoala
 */
public final class BiomeUtil {
	private static final Set<ResourceKey<Biome>> CUSTOM_END_MUSIC_BIOMES = new HashSet<>();
	private static final BasicRegistry<Codec<? extends ModdedBiomeProvider>> MODDED_PROVIDERS = new BasicRegistry<>();

	static {
		MODDED_PROVIDERS.register(new ResourceLocation(Blueprint.MOD_ID, "original"), BiomeUtil.OriginalModdedBiomeProvider.CODEC);
		MODDED_PROVIDERS.register(new ResourceLocation(Blueprint.MOD_ID, "multi_noise"), BiomeUtil.MultiNoiseModdedBiomeProvider.CODEC);
		MODDED_PROVIDERS.register(new ResourceLocation(Blueprint.MOD_ID, "overlay"), BiomeUtil.OverlayModdedBiomeProvider.CODEC);
	}

	/**
	 * Registers a new {@link ModdedBiomeProvider} type that can be serialized and deserialized.
	 *
	 * @param name  A {@link ResourceLocation} name for the provider.
	 * @param codec A {@link Codec} to use for serializing and deserializing instances of the {@link ModdedBiomeProvider} type.
	 */
	public static synchronized void registerBiomeProvider(ResourceLocation name, Codec<? extends ModdedBiomeProvider> codec) {
		MODDED_PROVIDERS.register(name, codec);
	}

	/**
	 * Marks the {@link ResourceKey} belonging to a {@link Biome} to have it play its music in the end.
	 * <p>The music for biomes in the end is hardcoded, and this gets around that.</p>
	 * <p>This method is safe to call during parallel mod loading.</p>
	 *
	 * @param biomeName The {@link ResourceKey} belonging to a {@link Biome} to have it play its music in the end.
	 */
	public static synchronized void markEndBiomeCustomMusic(ResourceKey<Biome> biomeName) {
		CUSTOM_END_MUSIC_BIOMES.add(biomeName);
	}

	/**
	 * Checks if a {@link ResourceKey} belonging to a {@link Biome} should have the {@link Biome} plays its custom music in the end.
	 *
	 * @param biomeName The {@link ResourceKey} belonging to a {@link Biome} to check.
	 * @return If a {@link ResourceKey} belonging to a {@link Biome} should have the {@link Biome} plays its custom music in the end.
	 */
	public static boolean shouldPlayCustomEndMusic(ResourceKey<Biome> biomeName) {
		return CUSTOM_END_MUSIC_BIOMES.contains(biomeName);
	}

	/**
	 * Get the {@link Biome} id given a {@link Biome} {@link ResourceKey}.
	 *
	 * @param biome The {@link Biome} {@link ResourceKey} to get the id of.
	 * @return The id of the provided {@link Biome} {@link ResourceKey}.
	 */
	@SuppressWarnings("deprecation")
	public static int getId(@Nonnull ResourceKey<Biome> biome) {
		return BuiltinRegistries.BIOME.getId(BuiltinRegistries.BIOME.get(biome));
	}

	/**
	 * The interface used for selecting biomes in {@link com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSlice} instances.
	 * <p>Use {@link #CODEC} for serializing and deserializing instances of this class.</p>
	 *
	 * @author SmellyModder (Luke Tonon)
	 * @see com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSource
	 */
	public interface ModdedBiomeProvider {
		Codec<ModdedBiomeProvider> CODEC = BiomeUtil.MODDED_PROVIDERS.dispatchStable(ModdedBiomeProvider::codec, Function.identity());

		/**
		 * Gets a holder of a noise {@link Biome} at a position in a modded slice.
		 *
		 * @param x        The x pos, shifted by {@link net.minecraft.core.QuartPos#fromBlock(int)}.
		 * @param y        The y pos, shifted by {@link net.minecraft.core.QuartPos#fromBlock(int)}.
		 * @param z        The z pos, shifted by {@link net.minecraft.core.QuartPos#fromBlock(int)}.
		 * @param sampler  A {@link Climate.Sampler} instance to sample {@link net.minecraft.world.level.biome.Climate.TargetPoint} instances.
		 * @param original The original {@link BiomeSource} instance that this provider is modding.
		 * @param registry The biome {@link Registry} instance to use if needed.
		 * @return A noise {@link Biome} at a position in a modded slice.
		 */
		Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler, BiomeSource original, Registry<Biome> registry);

		/**
		 * Gets a set of the additional possible biomes that this provider may have.
		 *
		 * @param registry The biome {@link Registry} instance to use if needed.
		 * @return A set of the additional possible biomes that this provider may have.
		 * @see com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSource.WeightedBiomeSlices#combinePossibleBiomes(Set, Registry).
		 */
		Set<Holder<Biome>> getAdditionalPossibleBiomes(Registry<Biome> registry);

		/**
		 * Gets a {@link Codec} instance for serializing and deserializing this provider.
		 *
		 * @return A {@link Codec} instance for serializing and deserializing this provider.
		 */
		Codec<? extends ModdedBiomeProvider> codec();
	}

	/**
	 * A simple {@link ModdedBiomeProvider} implementation that uses the original biome source's {@link BiomeSource#getNoiseBiome(int, int, int, Climate.Sampler)} method.
	 *
	 * @author SmellyModder (Luke Tonon)
	 */
	public static record OriginalModdedBiomeProvider() implements ModdedBiomeProvider {
		public static final Codec<OriginalModdedBiomeProvider> CODEC = Codec.unit(new OriginalModdedBiomeProvider());

		@Override
		public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler, BiomeSource original, Registry<Biome> registry) {
			return original.getNoiseBiome(x, y, z, sampler);
		}

		@Override
		public Codec<? extends ModdedBiomeProvider> codec() {
			return CODEC;
		}

		@Override
		public Set<Holder<Biome>> getAdditionalPossibleBiomes(Registry<Biome> registry) {
			return new HashSet<>(0);
		}
	}

	/**
	 * A {@link ModdedBiomeProvider} implementation that uses a {@link Climate.ParameterList} instance for selecting its biomes.
	 *
	 * @author SmellyModder (Luke Tonon)
	 */
	public static record MultiNoiseModdedBiomeProvider(Climate.ParameterList<ResourceKey<Biome>> biomes) implements ModdedBiomeProvider {
		public static final Codec<MultiNoiseModdedBiomeProvider> CODEC = RecordCodecBuilder.create((instance) -> {
			return instance.group(
					ExtraCodecs.nonEmptyList(RecordCodecBuilder.<Pair<Climate.ParameterPoint, ResourceKey<Biome>>>create((pairInstance) -> {
						return pairInstance.group(Climate.ParameterPoint.CODEC.fieldOf("parameters").forGetter(Pair::getFirst), ResourceKey.codec(Registry.BIOME_REGISTRY).fieldOf("biome").forGetter(Pair::getSecond)).apply(pairInstance, Pair::of);
					}).listOf()).xmap(Climate.ParameterList::new, Climate.ParameterList::values).fieldOf("biomes").forGetter(sampler -> sampler.biomes)
			).apply(instance, MultiNoiseModdedBiomeProvider::new);
		});

		@Override
		public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler, BiomeSource original, Registry<Biome> registry) {
			return registry.getHolderOrThrow(this.biomes.findValue(sampler.sample(x, y, z)));
		}

		@Override
		public Codec<? extends ModdedBiomeProvider> codec() {
			return CODEC;
		}

		@Override
		public Set<Holder<Biome>> getAdditionalPossibleBiomes(Registry<Biome> registry) {
			return this.biomes.values().stream().map(pair -> registry.getHolderOrThrow(pair.getSecond())).collect(Collectors.toSet());
		}
	}

	/**
	 * A {@link ModdedBiomeProvider} implementation that maps out {@link BiomeSource} instances for overlaying specific biomes.
	 * <p>This is especially useful for sub-biomes.</p>
	 *
	 * @author SmellyModder (Luke Tonon)
	 */
	//TODO: Allow usage of biome tags for 'target_biomes'
	public static record OverlayModdedBiomeProvider(Map<ResourceLocation, BiomeSource> map) implements ModdedBiomeProvider {
		public static final Codec<OverlayModdedBiomeProvider> CODEC = RecordCodecBuilder.create(instance -> {
			return instance.group(
					//Using a list of pairs significantly saves file size
					Codec.mapPair(ResourceLocation.CODEC.listOf().fieldOf("target_biomes"), BiomeSource.CODEC.fieldOf("biome_source")).codec().listOf().xmap(list -> {
						ImmutableMap.Builder<ResourceLocation, BiomeSource> map = ImmutableMap.builder();
						for (var pair : list) {
							BiomeSource source = pair.getSecond();
							pair.getFirst().forEach(location -> map.put(location, source));
						}
						return (Map<ResourceLocation, BiomeSource>) map.build();
					}, map -> {
						ImmutableList.Builder<Pair<List<ResourceLocation>, BiomeSource>> list = new ImmutableList.Builder<>();
						Map<BiomeSource, List<ResourceLocation>> collected = new IdentityHashMap<>();
						map.forEach((location, source) -> collected.computeIfAbsent(source, __ -> new LinkedList<>()).add(location));
						collected.forEach((source, locations) -> list.add(Pair.of(locations, source)));
						return list.build();
					}).fieldOf("overlays").forGetter(provider -> provider.map)
			).apply(instance, OverlayModdedBiomeProvider::new);
		});

		@Override
		public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler, BiomeSource original, Registry<Biome> registry) {
			Holder<Biome> originalBiome = original.getNoiseBiome(x, y, z, sampler);
			BiomeSource source = this.map.get(originalBiome.unwrapKey().orElseThrow().location());
			if (source == null) return originalBiome;
			return source.getNoiseBiome(x, y, z, sampler);
		}

		@Override
		public Set<Holder<Biome>> getAdditionalPossibleBiomes(Registry<Biome> registry) {
			HashSet<Holder<Biome>> biomes = new HashSet<>();
			this.map.values().forEach(source -> biomes.addAll(source.possibleBiomes()));
			return biomes;
		}

		@Override
		public Codec<? extends ModdedBiomeProvider> codec() {
			return CODEC;
		}
	}
}
