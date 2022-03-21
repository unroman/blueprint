package com.teamabnormals.blueprint.common.advancement.modification.modifiers;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.teamabnormals.blueprint.common.advancement.modification.AdvancementModifierSerializers;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

/**
 * An {@link AdvancementModifier} implementation that modifies the criteria and requirements of an advancement.
 * <p>This modifier can cause unexpected errors to occur later in-game when replacing certain parts due to how requirements work, so be sure you know what you're doing.</p>
 *
 * @author SmellyModder (Luke Tonon)
 */
public record CriteriaModifier(Mode mode, Optional<Map<String, Criterion>> criterionMap, Optional<String[][]> requirements) implements AdvancementModifier<CriteriaModifier> {
	public static final Field REQUIREMENTS_FIELD = ObfuscationReflectionHelper.findField(Advancement.Builder.class, "f_138337_");

	@Override
	public void modify(Advancement.Builder builder) {
		Map<String, Criterion> criteria = builder.getCriteria();
		try {
			String[][] requirementsArray = (String[][]) REQUIREMENTS_FIELD.get(builder);
			if (this.mode == Mode.MODIFY) {
				if (this.requirements.isPresent()) {
					String[][] configRequirements = this.requirements.get();
					requirementsArray = ArrayUtils.addAll(requirementsArray, configRequirements);
				}
				REQUIREMENTS_FIELD.set(builder, requirementsArray);
			} else {
				criteria.clear();
				REQUIREMENTS_FIELD.set(builder, this.requirements.orElseGet(() -> new String[][]{}));
			}
			this.criterionMap.ifPresent(criteria::putAll);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Serializer getSerializer() {
		return AdvancementModifierSerializers.CRITERIA;
	}

	public static final class Serializer implements AdvancementModifier.Serializer<CriteriaModifier> {
		@Override
		public JsonElement serialize(CriteriaModifier modifier, Void additional) throws JsonParseException {
			JsonObject jsonObject = new JsonObject();
			modifier.mode.serialize(jsonObject);
			modifier.criterionMap.ifPresent(map -> {
				JsonObject criteria = new JsonObject();
				map.forEach((key, criterion) -> criteria.add(key, criterion.serializeToJson()));
				jsonObject.add("criteria", criteria);
			});
			modifier.requirements.ifPresent(requirements -> {
				JsonArray requirementsArray = new JsonArray();
				for (String[] astring : requirements) {
					JsonArray jsonarray = new JsonArray();
					for (String s : astring) {
						jsonarray.add(s);
					}
					requirementsArray.add(jsonarray);
				}
				jsonObject.add("requirements", requirementsArray);
			});
			return jsonObject;
		}

		@Override
		public CriteriaModifier deserialize(JsonElement element, DeserializationContext additional) throws JsonParseException {
			JsonObject object = element.getAsJsonObject();
			Mode mode = Mode.deserialize(object);
			Optional<Map<String, Criterion>> criteria = GsonHelper.isValidNode(object, "criteria") ? Optional.of(Criterion.criteriaFromJson(GsonHelper.getAsJsonObject(object, "criteria"), additional)) : Optional.empty();
			if (criteria.isPresent() && criteria.get().isEmpty()) {
				throw new JsonParseException("Advancement criteria cannot be empty");
			}
			Optional<String[][]> requirements = Optional.empty();
			if (GsonHelper.isValidNode(object, "requirements")) {
				JsonArray jsonArray = GsonHelper.getAsJsonArray(object, "requirements", new JsonArray());
				String[][] requirementsArray = new String[jsonArray.size()][];

				for (int i = 0; i < jsonArray.size(); ++i) {
					JsonArray requirementsArray2 = GsonHelper.convertToJsonArray(jsonArray.get(i), "requirements[" + i + "]");
					requirementsArray[i] = new String[requirementsArray2.size()];

					for (int j = 0; j < requirementsArray2.size(); ++j) {
						requirementsArray[i][j] = GsonHelper.convertToString(requirementsArray2.get(j), "requirements[" + i + "][" + j + "]");
					}
				}

				Map<String, Criterion> map = criteria.orElse(Maps.newHashMap());
				if (requirementsArray.length == 0) {
					requirementsArray = new String[map.size()][];

					int i = 0;
					for (String key : map.keySet()) {
						requirementsArray[i++] = new String[]{key};
					}
				}

				for (String[] requirementArray : requirementsArray) {
					if (requirementArray.length == 0 && map.isEmpty()) {
						throw new JsonParseException("Requirement entry cannot be empty");
					}

					for (String criterion : requirementArray) {
						if (!map.containsKey(criterion)) {
							throw new JsonParseException("Unknown required criterion '" + criterion + "'");
						}
					}
				}

				for (String key : map.keySet()) {
					boolean required = false;

					for (String[] array : requirementsArray) {
						if (ArrayUtils.contains(array, key)) {
							required = true;
							break;
						}
					}

					if (!required) {
						throw new JsonParseException("Criterion '" + key + "' isn't a requirement for completion. This isn't supported behaviour, all criteria must be required.");
					}
				}

				requirements = Optional.of(requirementsArray);
			}
			return new CriteriaModifier(mode, criteria, requirements);
		}
	}
}
