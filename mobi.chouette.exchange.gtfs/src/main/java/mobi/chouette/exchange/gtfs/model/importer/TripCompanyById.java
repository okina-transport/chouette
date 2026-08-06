package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsTripCompany;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;

public class TripCompanyById extends IndexImpl<GtfsTripCompany> implements GtfsConverter {

    public static final String FILENAME = "companies.txt";

    public static final String KEY = FIELDS.company_id.name();

    static {
        IndexFactory factory = new DefaultImporterFactory();
        IndexFactory.factories.put(TripCompanyById.class.getName(), factory);
    }

    private final GtfsTripCompany bean = new GtfsTripCompany();
    private final String[] array = new String[FIELDS.values().length];

    public TripCompanyById(String name) throws IOException {
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

        if (fields.get(FIELDS.company_id.name()) == null) {
            throw new GtfsException(_path, 1, FIELDS.company_id.name(), GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
        }
    }

    @Override
    protected GtfsTripCompany build(GtfsIterator reader, Context context) {
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
        testExtraSpace(FIELDS.company_id.name(), value, bean);
        bean.setCompanyId(STRING_CONVERTER.from(context, FIELDS.company_id, value, true));

        value = array[i++];
        testExtraSpace(FIELDS.company_name.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setCompanyName(STRING_CONVERTER.from(context, FIELDS.company_name, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.company_address.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setCompanyAddress(STRING_CONVERTER.from(context, FIELDS.company_address, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.company_zipcode.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setCompanyZipcode(STRING_CONVERTER.from(context, FIELDS.company_zipcode, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.company_city.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setCompanyCity(STRING_CONVERTER.from(context, FIELDS.company_city, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.company_phone.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setCompanyPhone(STRING_CONVERTER.from(context, FIELDS.company_phone, value, false));
        }

        value = array[i++];
        testExtraSpace(FIELDS.company_email.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            bean.setCompanyEmail(STRING_CONVERTER.from(context, FIELDS.company_email, value, false));
        }

        return bean;
    }

    @Override
    public boolean validate(GtfsTripCompany bean, GtfsImporter dao) {
        return true;
    }

    private void clearBean() {
        bean.getErrors().clear();
        bean.setId(null);
        bean.setCompanyId(null);
        bean.setCompanyName(null);
        bean.setCompanyAddress(null);
        bean.setCompanyZipcode(null);
        bean.setCompanyCity(null);
        bean.setCompanyPhone(null);
        bean.setCompanyEmail(null);
    }

    public enum FIELDS {
        company_id, company_name, company_address, company_zipcode, company_city, company_phone, company_email
    }

    public static class DefaultImporterFactory extends IndexFactory {
        @SuppressWarnings("rawtypes")
        @Override
        protected Index create(String name) throws IOException {
            return new TripCompanyById(name);
        }
    }

}
