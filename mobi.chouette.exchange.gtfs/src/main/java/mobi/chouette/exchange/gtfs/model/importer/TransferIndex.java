package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsTransfer;

import java.io.IOException;
import java.time.temporal.ValueRange;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TransferIndex extends IndexImpl<GtfsTransfer> implements GtfsConverter {

	public static final String FILENAME = "transfers.txt";

	public static final String KEY = FIELDS.from_stop_id.name();

	static {
		IndexFactory factory = new DefaultImporterFactory();
		IndexFactory.factories.put(TransferByFromStop.class.getName(), factory);
	}

	private final GtfsTransfer bean = new GtfsTransfer();
	private final String[] array = new String[FIELDS.values().length];

	public TransferIndex(String name, String id, boolean unique) throws IOException {
		super(name, id, unique);
	}

	public TransferIndex(String name, String id, boolean unique, boolean ignoreRowsWithMissingKey) throws IOException {
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

		if (fields.get(FIELDS.transfer_type.name()) == null) {
			String name = "";
			if (fields.get(FIELDS.transfer_type.name()) == null)
				name = FIELDS.transfer_type.name();

			throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
		}
	}

	@Override
	protected GtfsTransfer build(GtfsIterator reader, Context context) {
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
		testExtraSpace(FIELDS.from_stop_id.name(), value, bean);
		if (value != null || value.trim().isEmpty()) {
			try {
				bean.setFromStopId(STRING_CONVERTER.from(context, FIELDS.from_stop_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.from_stop_id.name()), FIELDS.from_stop_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.to_stop_id.name(), value, bean);
		if (value != null || value.trim().isEmpty()) {
			try {
				bean.setToStopId(STRING_CONVERTER.from(context, FIELDS.to_stop_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.to_stop_id.name()), FIELDS.to_stop_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		// Route id
		value = array[i++];
		testExtraSpace(FIELDS.from_route_id.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setFromRouteId(STRING_CONVERTER.from(context, FIELDS.from_route_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.from_route_id.name()), FIELDS.from_route_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.to_route_id.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setToRouteId(STRING_CONVERTER.from(context, FIELDS.to_route_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.to_route_id.name()), FIELDS.to_route_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		// Trip id
		value = array[i++];
		testExtraSpace(FIELDS.from_trip_id.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setFromTripId(STRING_CONVERTER.from(context, FIELDS.from_trip_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.from_trip_id.name()), FIELDS.from_trip_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.to_trip_id.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setToTripId(STRING_CONVERTER.from(context, FIELDS.to_trip_id, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.to_trip_id.name()), FIELDS.to_trip_id.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}


		value = array[i++];
		testExtraSpace(FIELDS.transfer_type.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setTransferType(TRANSFERTYPE_CONVERTER.from(context, FIELDS.transfer_type, value, true));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, FIELDS.transfer_type.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		}

		value = array[i++];
		testExtraSpace(FIELDS.min_transfer_time.name(), value, bean);
		if (value != null && !value.trim().isEmpty()) {
			try {
				bean.setMinTransferTime(POSITIVE_INTEGER_CONVERTER.from(context, FIELDS.min_transfer_time, value, false));
			} catch (GtfsException ex) {
				if (withValidation)
					bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.min_transfer_time.name()), FIELDS.min_transfer_time.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
			}
		} else {
			bean.setMinTransferTime(null);
		}

		if (bean.getTransferType() == GtfsTransfer.TransfersTypeEnum.Minimal && bean.getMinTransferTime() == null) {
			if (withValidation)
				bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.transfer_type.name()), FIELDS.transfer_type.name(), GtfsException.ERROR.MISSING_TRANSFER_TIME, null, null));
		}


		if (bean.getTransferType() != null && ValueRange.of(1, 3).isValidIntValue(bean.getTransferType().getValue()) && (bean.getFromStopId() == null || bean.getToStopId() == null)) {
			if (bean.getFromStopId() == null) {
				bean.getErrors().add(new GtfsException(_path, id, FIELDS.from_stop_id.ordinal(), "fromStopId", GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, value));
			}
			if (bean.getToStopId() == null) {
				bean.getErrors().add(new GtfsException(_path, id, FIELDS.to_stop_id.ordinal(), "toStopId", GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, value));
			}
		}

		if (bean.getTransferType() != null && ValueRange.of(4, 5).isValidIntValue(bean.getTransferType().getValue()) && (bean.getFromTripId() == null || bean.getToTripId() == null)) {
			if (bean.getFromTripId() == null) {
				bean.getErrors().add(new GtfsException(_path, id, FIELDS.from_trip_id.ordinal(), "fromTripId", GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, value));
			}
			if (bean.getToTripId() == null) {
				bean.getErrors().add(new GtfsException(_path, id, FIELDS.to_trip_id.ordinal(), "toTripId", GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, value));
			}
		}
		return bean;
	}

	@Override
	public boolean validate(GtfsTransfer bean, GtfsImporter dao) {
		boolean result = true;

		String fromStopId = bean.getFromStopId();
		if (dao.getStopById().containsKey(fromStopId)) {
			bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
		} else {
			bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.from_stop_id.name()), FIELDS.from_stop_id.name(), GtfsException.ERROR.UNREFERENCED_ID, null, fromStopId));
			result = false;
		}

		String toStopId = bean.getToStopId();
		if (dao.getStopById().containsKey(toStopId)) {
			bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
		} else {
			bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.to_stop_id.name()), FIELDS.to_stop_id.name(), GtfsException.ERROR.UNREFERENCED_ID, null, toStopId));
			result = false;
		}

		String fromRouteId = bean.getFromRouteId();
		if (fromRouteId != null) {
			if (dao.getRouteById().containsKey(fromRouteId)) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.from_route_id.name()), FIELDS.from_route_id.name(), GtfsException.ERROR.UNREFERENCED_ID, null, fromRouteId));
				result = false;
			}
		}

		String toRouteId = bean.getToRouteId();
		if (toRouteId != null) {
			if (dao.getRouteById().containsKey(toRouteId)) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.to_route_id.name()), FIELDS.to_route_id.name(), GtfsException.ERROR.UNREFERENCED_ID, null, toRouteId));
				result = false;
			}
		}

		String fromTripId = bean.getFromTripId();
		if (fromTripId != null) {
			if (dao.getTripById().containsKey(fromTripId)) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.from_trip_id.name()), FIELDS.from_trip_id.name(), GtfsException.ERROR.UNREFERENCED_ID, null, fromTripId));
				result = false;
			}
		}

		String toTripId = bean.getToTripId();
		if (toTripId != null) {
			if (dao.getTripById().containsKey(toTripId)) {
				bean.getOkTests().add(GtfsException.ERROR.UNREFERENCED_ID);
			} else {
				bean.getErrors().add(new GtfsException(_path, bean.getId(), getIndex(FIELDS.to_trip_id.name()), FIELDS.to_trip_id.name(), GtfsException.ERROR.UNREFERENCED_ID, null, toTripId));
				result = false;
			}
		}

		return result;
	}

	private void clearBean() {
		//bean.getErrors().clear();
		bean.setId(null);
		bean.setFromStopId(null);
		bean.setMinTransferTime(null);
		bean.setToStopId(null);
		bean.setTransferType(null);
		bean.setFromRouteId(null);
		bean.setToRouteId(null);
		bean.setFromTripId(null);
		bean.setToTripId(null);
	}

	public static class DefaultImporterFactory extends IndexFactory {
		@SuppressWarnings("rawtypes")
		@Override
		protected Index create(String name) throws IOException {
			return new TransferByFromStop(name);
		}
	}

	public enum FIELDS {
		from_stop_id, to_stop_id, from_route_id, to_route_id, from_trip_id, to_trip_id, transfer_type, min_transfer_time
	}

}
