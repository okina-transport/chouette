package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.FareMediaType;
import mobi.chouette.exchange.gtfs.model.GtfsFareMedia;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FareMediaIndex extends IndexImpl<GtfsFareMedia> implements GtfsConverter {

	public static final String FILENAME = "fare_media.txt";

	public static final String KEY = FIELDS.fare_media_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(FareMediaIndex.class.getName(), factory);
	}

	private final GtfsFareMedia bean = new GtfsFareMedia();
	private final String[] array = new String[FIELDS.values().length];

	public FareMediaIndex(String name, String id, boolean unique) throws IOException {
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

		if (fields.get(FIELDS.fare_media_id.name()) == null
				|| fields.get(FIELDS.fare_media_name.name()) == null
				|| fields.get(FIELDS.fare_media_type.name()) == null
		) {

			String name = "";
			if (fields.get(FIELDS.fare_media_id.name()) == null) {
				name = FIELDS.fare_media_id.name();
			} else if (fields.get(FIELDS.fare_media_name.name()) == null) {
				name = FIELDS.fare_media_name.name();
			} else if (fields.get(FIELDS.fare_media_type.name()) == null) {
				name = FIELDS.fare_media_type.name();
			}
			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsFareMedia build(GtfsIterator reader, Context context) {
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
		testExtraSpace(FIELDS.fare_media_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFareMediaId(STRING_CONVERTER.from(context, FIELDS.fare_media_id, value, true));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_media_id.name()), FIELDS.fare_media_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.fare_media_name.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFareMediaName(STRING_CONVERTER.from(context, FIELDS.fare_media_name, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_media_name.name()), FIELDS.fare_media_name.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.fare_media_type.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFareMediaType(FareMediaType.values()[POSITIVE_INTEGER_CONVERTER.from(context, FIELDS.fare_media_type, value, true)]);
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_media_type.name()), FIELDS.fare_media_type.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		return bean;
	}

	@Override
	public boolean validate(GtfsFareMedia bean, GtfsImporter dao) {
		boolean result = true;

		if (isPresent(bean.getFareMediaId())) {
			if (dao.getFareMediaIndex().containsKey(bean.getFareMediaId())) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.fare_media_id.name()), FIELDS.fare_media_id.name(), GtfsException.ERROR.UNREFERENCED_ID, bean.getFareMediaId(), bean.getFareMediaId()));
				result = false;
			}
		}

		return result;
	}

	private void clearBean() {
		bean.setFareMediaType(null);
		bean.setFareMediaName(null);
		bean.setFareMediaId(null);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new FareMediaIndex(name, KEY, true);
		}
	}

	public enum FIELDS {
		fare_media_id, fare_media_name, fare_media_type
	}

}
