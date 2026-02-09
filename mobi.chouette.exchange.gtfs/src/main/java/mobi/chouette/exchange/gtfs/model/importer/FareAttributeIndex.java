package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsFareAttribute;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FareAttributeIndex extends IndexImpl<GtfsFareAttribute> implements GtfsConverter {

	public static final String FILENAME = "fare_attributes.txt";

	public static final String KEY = FIELDS.fare_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(FareAttributeById.class.getName(), factory);
	}

	private final GtfsFareAttribute bean = new GtfsFareAttribute();
	private final String[] array = new String[FIELDS.values().length];

	public FareAttributeIndex(String name, String id, boolean unique) throws IOException {
		super(name, id, unique);
	}

	public FareAttributeIndex(String name, String id, boolean unique, boolean ignoreRowsWithMissingKey) throws IOException {
		super(name, id, "", unique, ignoreRowsWithMissingKey);
	}

	@Override
	protected void checkRequiredFields(Map<String, Integer> fields) {
		Set<String> validFields = fields.keySet().stream().filter(fieldName -> Arrays.stream(FIELDS.values()).anyMatch(field -> field.name().equals(fieldName))).collect(Collectors.toSet());

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
		if (fields.get(FIELDS.fare_id.name()) == null || fields.get(FIELDS.price.name()) == null || fields.get(FIELDS.currency_type.name()) == null || fields.get(FIELDS.payment_method.name()) == null || fields.get(FIELDS.transfers.name()) == null) {// fields.get(FIELDS.agency_id.name()) == null

			String name = "";
			if (fields.get(FIELDS.fare_id.name()) == null)
				name = FIELDS.fare_id.name();
			else
				if (fields.get(FIELDS.price.name()) == null)
					name = FIELDS.price.name();
				else
					if (fields.get(FIELDS.currency_type.name()) == null)
						name = FIELDS.currency_type.name();
					else
						if (fields.get(FIELDS.payment_method.name()) == null)
							name = FIELDS.payment_method.name();
						else
							if (fields.get(FIELDS.transfers.name()) == null)
								name = FIELDS.transfers.name();
			//            else if (fields.get(FIELDS.agency_id.name()) == null)
			//                name = FIELDS.agency_id.name();

			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsFareAttribute build(GtfsIterator reader, Context context) {
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
		if (StringUtils.isNotEmpty(value)) {
			try {
				bean.setFareId(STRING_CONVERTER.from(context, FIELDS.fare_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.fare_id.name()), FIELDS.fare_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.price.name(), value, bean);
		if (StringUtils.isNotEmpty(value)) {
			try {
				bean.setPrice(FLOAT_CONVERTER.from(context, FIELDS.price, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.price.name()), FIELDS.price.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.currency_type.name(), value, bean);
		if (StringUtils.isNotEmpty(value)) {
			try {
				bean.setCurrencyType(STRING_CONVERTER.from(context, FIELDS.currency_type, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.currency_type.name()), FIELDS.currency_type.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.payment_method.name(), value, bean);
		if (StringUtils.isNotEmpty(value)) {
			try {
				bean.setPaymentMethod(PAYMENTMETHODTYPE_CONVERTER.from(context, FIELDS.payment_method, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.payment_method.name()), FIELDS.payment_method.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.transfers.name(), value, bean);
		if (StringUtils.isNotEmpty(value)) {
			try {
				bean.setTransfers(ATTRIBUTETRANSFERSTYPE_CONVERTER.from(context, FIELDS.transfers, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.transfers.name()), FIELDS.transfers.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.agency_id.name(), value, bean);
		if (StringUtils.isNotEmpty(value)) {
			try {
				bean.setAgencyId(STRING_CONVERTER.from(context, FIELDS.agency_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.agency_id.name()), FIELDS.agency_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.transfer_duration.name(), value, bean);
		if (StringUtils.isNotEmpty(value)) {
			try {
				bean.setTransferDuration(FLOAT_CONVERTER.from(context, FIELDS.transfer_duration, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.transfer_duration.name()), FIELDS.transfer_duration.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}
		return bean;
	}

	@Override
	public boolean validate(GtfsFareAttribute bean, GtfsImporter dao) {
		boolean result = true;

		if (isPresent(bean.getFareId()))
			if (dao.getFareAttributeById().containsKey(bean.getFareId())) {
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
		bean.setPrice(null);
		bean.setCurrencyType(null);
		bean.setPaymentMethod(null);
		bean.setTransfers(null);
		bean.setAgencyId(null);
		bean.setTransferDuration(null);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new FareAttributeById(name);
		}
	}

	public enum FIELDS {
		fare_id, price, currency_type, payment_method, transfers, agency_id, transfer_duration
	}

}
