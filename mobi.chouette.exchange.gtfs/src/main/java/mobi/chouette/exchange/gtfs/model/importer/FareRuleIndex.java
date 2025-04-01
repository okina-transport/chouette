package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsFareRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FareRuleIndex extends IndexImpl<GtfsFareRule> implements GtfsConverter {

	public static final String FILENAME = "fare_rules.txt";

	public static final String KEY = FIELDS.fare_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(FareRuleById.class.getName(), factory);
	}

	private final GtfsFareRule bean = new GtfsFareRule();
	private final String[] array = new String[FIELDS.values().length];

	public FareRuleIndex(String name, String id, boolean unique) throws IOException {
		super(name, id, unique);
	}

	public FareRuleIndex(String name, String id, boolean unique, boolean ignoreRowsWithMissingKey) throws IOException {
		super(name, id, "", unique, ignoreRowsWithMissingKey);
	}

	@Override
	protected void checkRequiredFields(Map<String, Integer> fields) {
		Set<String> validFields = fields.keySet().stream().filter(fieldName -> Arrays.stream(FareAttributeIndex.FIELDS.values()).anyMatch(field -> field.name().equals(fieldName))).collect(Collectors.toSet());

		for (String fieldName : validFields) {
			if (fieldName != null) {
				if (!fieldName.equals(fieldName.trim())) {
					// extra spaces in end fields are tolerated : 1-GTFS-CSV-7 warning
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
					// extra fields are tolerated : 1-GTFS-Transfer-6 warning
					getErrors().add(new GtfsException(_path, 1, getIndex(fieldName), fieldName, GtfsException.ERROR.EXTRA_HEADER_FIELD, null, null));
				}
			}
		}

		// checks for ubiquitous header fields : 1-GTFS-Transfer-1 error
		if (fields.get(FIELDS.fare_id.name()) == null) {

			String name = "";
			if (fields.get(FIELDS.fare_id.name()) == null)
				name = FIELDS.fare_id.name();

			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsFareRule build(GtfsIterator reader, Context context) {
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
		testExtraSpace(FIELDS.fare_id.name(), value, bean);
		if (value != null || value.trim().isEmpty()) {
			try {
				bean.setFareId(STRING_CONVERTER.from(context, FIELDS.fare_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_id.name()), FIELDS.fare_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.route_id.name(), value, bean);
		if (value != null || value.trim().isEmpty()) {
			try {
				bean.setRouteId(STRING_CONVERTER.from(context, FIELDS.route_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.route_id.name()), FIELDS.route_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.origin_id.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setOriginId(INTEGER_CONVERTER.from(context, FIELDS.origin_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.origin_id.name()), FIELDS.origin_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.destination_id.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setDestinationId(INTEGER_CONVERTER.from(context, FIELDS.destination_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.destination_id.name()), FIELDS.destination_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.contains_id.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setContainsId(INTEGER_CONVERTER.from(context, FIELDS.contains_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.contains_id.name()), FIELDS.contains_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}
		return bean;
	}

	@Override
	public boolean validate(GtfsFareRule bean, GtfsImporter dao) {
		boolean result = true;

		if (isPresent(bean.getFareId()))
			if (dao.getFareRuleById().containsKey(bean.getFareId())) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.fare_id.name()), FIELDS.fare_id.name(), GtfsException.ERROR.UNREFERENCED_ID, bean.getFareId(), bean.getFareId()));
				result = false;
			}

		return result;
	}

	private void clearBean() {
		//bean.getErrors().clear();
		bean.setFareId(null);
		bean.setRouteId(null);
		bean.setOriginId(null);
		bean.setDestinationId(null);
		bean.setContainsId(null);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new FareRuleById(name);
		}
	}

	public enum FIELDS {
		fare_id, route_id, origin_id, destination_id, contains_id
	}

}
