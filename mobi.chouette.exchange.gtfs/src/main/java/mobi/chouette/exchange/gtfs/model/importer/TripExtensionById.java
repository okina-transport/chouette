package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsTripExtension;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;

public class TripExtensionById extends IndexImpl<GtfsTripExtension> implements GtfsConverter {

    public static final String FILENAME = "trips_extensions.txt";

    public static final String KEY = FIELDS.trip_id.name();

    static {
        IndexFactory factory = new DefaultImporterFactory();
        IndexFactory.factories.put(TripExtensionById.class.getName(), factory);
    }

    private final GtfsTripExtension bean = new GtfsTripExtension();
    private final String[] array = new String[FIELDS.values().length];

    public TripExtensionById(String name) throws IOException {
        super(name, KEY, true);
    }

    @Override
    protected void checkRequiredFields(Map<String, Integer> fields) {
        for (String fieldName : fields.keySet()) {
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

        if (fields.get(FIELDS.trip_id.name()) == null) {
            throw new GtfsException(_path, 1, FIELDS.trip_id.name(), GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
        }
    }

    @Override
    protected GtfsTripExtension build(GtfsIterator reader, Context context) {
        int i = 0;
        for (FIELDS field : FIELDS.values()) {
            array[i++] = getField(reader, field.name());
        }

        i = 0;
        String value;
        int id = (int) context.get(Context.ID);
        clearBean();
        bean.setId(id);

        value = array[i++];
        testExtraSpace(FIELDS.trip_id.name(), value, bean);
        bean.setTripId(STRING_CONVERTER.from(context, FIELDS.trip_id, value, true));

        value = array[i++];
        testExtraSpace(FIELDS.route_id.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setRouteId(STRING_CONVERTER.from(context, FIELDS.route_id, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.contract_company_id.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setContractCompanyId(STRING_CONVERTER.from(context, FIELDS.contract_company_id, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.exec_company_id.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setExecCompanyId(STRING_CONVERTER.from(context, FIELDS.exec_company_id, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.indic_reservation.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setIndicReservation(STRING_CONVERTER.from(context, FIELDS.indic_reservation, value, false));
        }

        return bean;
    }

    @Override
    public boolean validate(GtfsTripExtension bean, GtfsImporter dao) {
        return true;
    }

    private void clearBean() {
        bean.getErrors().clear();
        bean.setId(null);
        bean.setTripId(null);
        bean.setRouteId(null);
        bean.setContractCompanyId(null);
        bean.setExecCompanyId(null);
        bean.setIndicReservation(null);
    }

    public enum FIELDS {
        trip_id, route_id, contract_company_id, exec_company_id, indic_reservation
    }

    public static class DefaultImporterFactory extends IndexFactory {
        @SuppressWarnings("rawtypes")
        @Override
        protected Index create(String name) throws IOException {
            return new TripExtensionById(name);
        }
    }

}
