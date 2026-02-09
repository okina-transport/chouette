package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsTimeframe;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TimeframeIndex extends IndexImpl<GtfsTimeframe> implements GtfsConverter {

	public static final String FILENAME = "timeframes.txt";

	public static final String KEY = FIELDS.timeframe_group_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(TimeframeIndex.class.getName(), factory);
	}

	private final GtfsTimeframe bean = new GtfsTimeframe();
	private final String[] array = new String[FIELDS.values().length];

	public TimeframeIndex(String name, String id, boolean unique) throws IOException {
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

		if (fields.get(FIELDS.timeframe_group_id.name()) == null
				|| fields.get(FIELDS.start_time.name()) == null
				|| fields.get(FIELDS.end_time.name()) == null
				|| fields.get(FIELDS.service_id.name()) == null
		) {

			String name = "";
			if (fields.get(FIELDS.timeframe_group_id.name()) == null) {
				name = FIELDS.timeframe_group_id.name();
			} else if (fields.get(FIELDS.start_time.name()) == null) {
				name = FIELDS.start_time.name();
			} else if (fields.get(FIELDS.end_time.name()) == null) {
				name = FIELDS.end_time.name();
			} else if (fields.get(FIELDS.service_id.name()) == null) {
				name = FIELDS.service_id.name();
			}
			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsTimeframe build(GtfsIterator reader, Context context) {
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
		testExtraSpace(FIELDS.timeframe_group_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setTimeframeGroupId(STRING_CONVERTER.from(context, FIELDS.timeframe_group_id, value, true));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.timeframe_group_id.name()), FIELDS.timeframe_group_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.start_time.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setStartTime(LOCALTIME_CONVERTER.from(context, FIELDS.start_time, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.start_time.name()), FIELDS.start_time.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.end_time.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setStartTime(LOCALTIME_CONVERTER.from(context, FIELDS.end_time, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.end_time.name()), FIELDS.end_time.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.service_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setServiceId(STRING_CONVERTER.from(context, FIELDS.service_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.service_id.name()), FIELDS.service_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}


		return bean;
	}

	@Override
	public boolean validate(GtfsTimeframe bean, GtfsImporter dao) {
		boolean result = true;

		if (isPresent(bean.getTimeframeGroupId())) {
			if (dao.getFareTimeframeIndex().containsKey(bean.getTimeframeGroupId())) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.timeframe_group_id.name()), FIELDS.timeframe_group_id.name(), GtfsException.ERROR.UNREFERENCED_ID, bean.getTimeframeGroupId(), bean.getTimeframeGroupId()));
				result = false;
			}
		}

		return result;
	}

	private void clearBean() {
		bean.setTimeframeGroupId(null);
		bean.setServiceId(null);
		bean.setStartTime(null);
		bean.setEndTime(null);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new TimeframeIndex(name, KEY, true);
		}
	}

	public enum FIELDS {
		timeframe_group_id, start_time, end_time, service_id
	}

}
