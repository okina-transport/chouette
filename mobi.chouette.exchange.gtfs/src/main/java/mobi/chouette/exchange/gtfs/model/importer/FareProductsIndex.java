package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsFareProducts;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FareProductsIndex extends IndexImpl<GtfsFareProducts> implements GtfsConverter {

	public static final String FILENAME = "fare_products.txt";

	public static final String KEY = FIELDS.fare_product_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(FareProductsIndex.class.getName(), factory);
	}

	private final GtfsFareProducts bean = new GtfsFareProducts();
	private final String[] array = new String[FIELDS.values().length];

	public FareProductsIndex(String name, String id, boolean unique) throws IOException {
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
				|| fields.get(FIELDS.fare_product_id.name()) == null
				|| fields.get(FIELDS.fare_product_name.name()) == null
				|| fields.get(FIELDS.rider_category_id.name()) == null
				|| fields.get(FIELDS.amount.name()) == null
				|| fields.get(FIELDS.currency.name()) == null
		) {

			String name = "";
			if (fields.get(FIELDS.fare_media_id.name()) == null) {
				name = FIELDS.fare_media_id.name();
			} else if (fields.get(FIELDS.fare_product_id.name()) == null) {
				name = FIELDS.fare_product_id.name();
			} else if (fields.get(FIELDS.fare_product_name.name()) == null) {
				name = FIELDS.fare_product_name.name();
			} else if (fields.get(FIELDS.rider_category_id.name()) == null) {
				name = FIELDS.rider_category_id.name();
			} else if (fields.get(FIELDS.amount.name()) == null) {
				name = FIELDS.amount.name();
			} else if (fields.get(FIELDS.currency.name()) == null) {
				name = FIELDS.currency.name();
			}
			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsFareProducts build(GtfsIterator reader, Context context) {
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
		testExtraSpace(FIELDS.fare_product_name.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFareProductName(STRING_CONVERTER.from(context, FIELDS.fare_product_name, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_product_name.name()), FIELDS.fare_product_name.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.rider_category_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setRiderCategoryId(STRING_CONVERTER.from(context, FIELDS.rider_category_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.rider_category_id.name()), FIELDS.rider_category_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.fare_media_id.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setFareMediaId(STRING_CONVERTER.from(context, FIELDS.fare_media_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_media_id.name()), FIELDS.fare_media_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.amount.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setAmount(FLOAT_CONVERTER.from(context, FIELDS.amount, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.amount.name()), FIELDS.amount.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.currency.name(), value, bean);
		if (StringUtils.isNotBlank(value)) {
			try {
				bean.setCurrency(STRING_CONVERTER.from(context, FIELDS.currency, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.currency.name()), FIELDS.currency.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		return bean;
	}

	@Override
	public boolean validate(GtfsFareProducts bean, GtfsImporter dao) {
		boolean result = true;

		if (isPresent(bean.getFareProductId())) {
			if (dao.getFareProductsIndex().containsKey(bean.getFareProductId())) {
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
		bean.setFareProductName(null);
		bean.setFareMediaId(null);
		bean.setCurrency(null);
		bean.setRiderCategoryId(null);
		bean.setAmount(0);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new FareProductsIndex(name, KEY, false);
		}
	}

	public enum FIELDS {
		fare_product_id, fare_product_name, rider_category_id, fare_media_id, amount, currency
	}

}
