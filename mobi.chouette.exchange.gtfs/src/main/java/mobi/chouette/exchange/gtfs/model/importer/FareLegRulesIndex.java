package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsFareLegRules;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FareLegRulesIndex extends IndexImpl<GtfsFareLegRules> implements GtfsConverter {

	public static final String FILENAME = "fare_leg_rules.txt";

	public static final String KEY = FIELDS.fare_product_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(FareLegRulesIndex.class.getName(), factory);
	}

	private final GtfsFareLegRules bean = new GtfsFareLegRules();
	private final String[] array = new String[FIELDS.values().length];

	public FareLegRulesIndex(String name, String id, boolean unique) throws IOException {
		super(name, id, unique);
	}

	@Override
	protected void checkRequiredFields(Map<String, Integer> fields) {
		Set<String> validFields = fields.keySet().stream().filter(fieldName -> Arrays.stream(FIELDS.values()).anyMatch(field -> field.name().equals(fieldName))).collect(Collectors.toSet());

		for (String fieldName : validFields) {
			if (fieldName != null) {
				if (!fieldName.equals(fieldName.trim())) {
					getErrors().add(new GtfsException(_path, 1, getIndex(fieldName), fieldName.trim(), GtfsException.ERROR.EXTRA_SPACE_IN_HEADER_FIELD, null, fieldName));
				}

				if (HTMLTagValidator.validate(fieldName.trim())) {
					getErrors().add(new GtfsException(_path, 1, getIndex(fieldName), fieldName.trim(), GtfsException.ERROR.HTML_TAG_IN_HEADER_FIELD, null, null));
				}

				boolean fieldNameIsExtra = true;
				for (FIELDS field : FIELDS.values()) {
					if (fieldName.trim().equals(field.name())) {
						fieldNameIsExtra = false;
						break;
					}
				}
				if (fieldNameIsExtra) {
					getErrors().add(new GtfsException(_path, 1, getIndex(fieldName), fieldName, GtfsException.ERROR.EXTRA_HEADER_FIELD, null, null));
				}
			}
		}

		if (fields.get(FIELDS.leg_group_id.name()) == null
				|| fields.get(FIELDS.network_id.name()) == null
				|| fields.get(FIELDS.fare_product_id.name()) == null
				|| fields.get(FIELDS.from_area_id.name()) == null
				|| fields.get(FIELDS.to_area_id.name()) == null
				|| fields.get(FIELDS.from_timeframe_group_id.name()) == null
				|| fields.get(FIELDS.to_timeframe_group_id.name()) == null
				|| fields.get(FIELDS.rule_priority.name()) == null
		) {

			String name = "";
			if (fields.get(FIELDS.leg_group_id.name()) == null) {
				name = FIELDS.leg_group_id.name();
			} else if (fields.get(FIELDS.network_id.name()) == null) {
				name = FIELDS.network_id.name();
			} else if (fields.get(FIELDS.fare_product_id.name()) == null) {
				name = FIELDS.fare_product_id.name();
			} else if (fields.get(FIELDS.from_area_id.name()) == null) {
				name = FIELDS.from_area_id.name();
			} else if (fields.get(FIELDS.to_area_id.name()) == null) {
				name = FIELDS.to_area_id.name();
			} else if (fields.get(FIELDS.from_timeframe_group_id.name()) == null) {
				name = FIELDS.from_timeframe_group_id.name();
			} else if (fields.get(FIELDS.to_timeframe_group_id.name()) == null) {
				name = FIELDS.to_timeframe_group_id.name();
			} else if (fields.get(FIELDS.rule_priority.name()) == null) {
				name = FIELDS.rule_priority.name();
			}
			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsFareLegRules build(GtfsIterator reader, Context context) {
		int i = 0;
		for (FIELDS field : FIELDS.values()) {
			array[i++] = getField(reader, field.name());
		}

		i = 0;
		String value = null;
		int id = (int) context.get(Context.ID);
		clearBean();
		bean.setId(id);
		bean.getErrors().clear();


		value = array[i++];
		testExtraSpace(FIELDS.leg_group_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setLegGroupId(STRING_CONVERTER.from(context, FIELDS.leg_group_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.leg_group_id.name()), FIELDS.leg_group_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.network_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setNetworkId(STRING_CONVERTER.from(context, FIELDS.network_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.network_id.name()), FIELDS.network_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.from_area_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFromAreaId(STRING_CONVERTER.from(context, FIELDS.from_area_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.from_area_id.name()), FIELDS.from_area_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.to_area_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setToAreaId(STRING_CONVERTER.from(context, FIELDS.to_area_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.to_area_id.name()), FIELDS.to_area_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.fare_product_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFareProductId(STRING_CONVERTER.from(context, FIELDS.fare_product_id, value, true));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_product_id.name()), FIELDS.fare_product_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.rule_priority.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setRulePriority(POSITIVE_INTEGER_CONVERTER.from(context, FIELDS.rule_priority, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.rule_priority.name()), FIELDS.rule_priority.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.from_timeframe_group_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFromTimeFrameGroupId(STRING_CONVERTER.from(context, FIELDS.from_timeframe_group_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.from_timeframe_group_id.name()), FIELDS.from_timeframe_group_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.to_timeframe_group_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setToTimeFrameGroupId(STRING_CONVERTER.from(context, FIELDS.to_timeframe_group_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.to_timeframe_group_id.name()), FIELDS.to_timeframe_group_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		return bean;
	}

	@Override
	public boolean validate(GtfsFareLegRules bean, GtfsImporter dao) {
		boolean result = true;

		if (isPresent(bean.getFareProductId())) {
			if (dao.getFareLegRulesIndex().containsKey(bean.getFareProductId())) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.fare_product_id.name()), FIELDS.fare_product_id.name(), GtfsException.ERROR.UNREFERENCED_ID, bean.getFareProductId(), bean.getFareProductId()));
				result = false;
			}
		}

		return result;
	}

	private void clearBean() {
		bean.setFareProductId(null);
		bean.setNetworkId(null);
		bean.setLegGroupId(null);
		bean.setFromTimeFrameGroupId(null);
		bean.setToTimeFrameGroupId(null);
		bean.setFromAreaId(null);
		bean.setToAreaId(null);
		bean.setRulePriority(0);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new FareLegRulesIndex(name, KEY, false);
		}
	}

	public enum FIELDS {
		leg_group_id, network_id, from_area_id, to_area_id, fare_product_id, rule_priority, from_timeframe_group_id, to_timeframe_group_id
	}

}
