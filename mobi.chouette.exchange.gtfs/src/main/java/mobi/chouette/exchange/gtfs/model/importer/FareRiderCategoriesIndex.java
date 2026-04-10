package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsRiderCategories;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FareRiderCategoriesIndex extends IndexImpl<GtfsRiderCategories> implements GtfsConverter {

	public static final String FILENAME = "rider_categories.txt";

	public static final String KEY = FIELDS.rider_category_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(FareRiderCategoriesIndex.class.getName(), factory);
	}

	private final GtfsRiderCategories bean = new GtfsRiderCategories();
	private final String[] array = new String[FIELDS.values().length];

	public FareRiderCategoriesIndex(String name, String id, boolean unique) throws IOException {
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

		if (fields.get(FIELDS.rider_category_id.name()) == null
				|| fields.get(FIELDS.rider_category_name.name()) == null
				|| fields.get(FIELDS.is_default_fare_category.name()) == null
				|| fields.get(FIELDS.eligibility_url.name()) == null
		) {

			String name = "";
			if (fields.get(FIELDS.rider_category_id.name()) == null) {
				name = FIELDS.rider_category_id.name();
			} else if (fields.get(FIELDS.rider_category_name.name()) == null) {
				name = FIELDS.rider_category_name.name();
			} else if (fields.get(FIELDS.is_default_fare_category.name()) == null) {
				name = FIELDS.is_default_fare_category.name();
			} else if (fields.get(FIELDS.eligibility_url.name()) == null) {
				name = FIELDS.eligibility_url.name();
			}
			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsRiderCategories build(GtfsIterator reader, Context context) {
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
		testExtraSpace(FIELDS.rider_category_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setRiderCategoryId(STRING_CONVERTER.from(context, FIELDS.rider_category_id, value, true));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.rider_category_id.name()), FIELDS.rider_category_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.rider_category_name.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setRiderCategoryName(STRING_CONVERTER.from(context, FIELDS.rider_category_name, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.rider_category_name.name()), FIELDS.rider_category_name.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.is_default_fare_category.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setIsDefaultFareCategory(BOOLEAN_CONVERTER.from(context, FIELDS.is_default_fare_category, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.is_default_fare_category.name()), FIELDS.is_default_fare_category.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.eligibility_url.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setEligibilityUrl(STRING_CONVERTER.from(context, FIELDS.eligibility_url, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.eligibility_url.name()), FIELDS.eligibility_url.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}


		return bean;
	}

	@Override
	public boolean validate(GtfsRiderCategories bean, GtfsImporter dao) {
		boolean result = true;

		if (isPresent(bean.getRiderCategoryId())) {
			if (dao.getFareRiderCategoriesIndex().containsKey(bean.getRiderCategoryId())) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.rider_category_id.name()), FIELDS.rider_category_id.name(), GtfsException.ERROR.UNREFERENCED_ID, bean.getRiderCategoryId(), bean.getRiderCategoryId()));
				result = false;
			}
		}

		return result;
	}

	private void clearBean() {
		bean.setRiderCategoryId(null);
		bean.setIsDefaultFareCategory(null);
		bean.setRiderCategoryName(null);
		bean.setEligibilityUrl(null);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new FareRiderCategoriesIndex(name, KEY, true);
		}
	}

	public enum FIELDS {
		rider_category_id, rider_category_name, is_default_fare_category, eligibility_url
	}

}
