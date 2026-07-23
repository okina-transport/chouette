package mobi.chouette.exchange.gtfs.model.importer;

import mobi.chouette.common.HTMLTagValidator;
import mobi.chouette.exchange.gtfs.model.GtfsTranslation;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * translations.txt is not keyed by a single unique column: several rows share the same table_name
 * (one row per field_name/language/record_id combination), so this index is built non-unique on
 * table_name (always required) purely to allow full iteration over every row.
 */
public class TranslationIndex extends IndexImpl<GtfsTranslation> implements GtfsConverter {

    public static final String FILENAME = "translations.txt";

    public static final String KEY = FIELDS.table_name.name();

    static {
        IndexFactory factory = new DefaultImporterFactory();
        IndexFactory.factories.put(TranslationIndex.class.getName(), factory);
    }

    private final GtfsTranslation bean = new GtfsTranslation();
    private final String[] array = new String[FIELDS.values().length];

    public TranslationIndex(String name, String id, boolean unique) throws IOException {
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

        if (fields.get(FIELDS.table_name.name()) == null
                || fields.get(FIELDS.field_name.name()) == null
                || fields.get(FIELDS.language.name()) == null
                || fields.get(FIELDS.translation.name()) == null
        ) {

            String name = "";
            if (fields.get(FIELDS.table_name.name()) == null) {
                name = FIELDS.table_name.name();
            } else if (fields.get(FIELDS.field_name.name()) == null) {
                name = FIELDS.field_name.name();
            } else if (fields.get(FIELDS.language.name()) == null) {
                name = FIELDS.language.name();
            } else if (fields.get(FIELDS.translation.name()) == null) {
                name = FIELDS.translation.name();
            }
            throw new GtfsException(_path, 1, name, GtfsException.ERROR.MISSING_REQUIRED_FIELDS, null, null);
        }
    }

    @Override
    protected GtfsTranslation build(GtfsIterator reader, Context context) {
        int i = 0;
        for (FIELDS field : FIELDS.values()) {
            array[i++] = getField(reader, field.name());
        }

        i = 0;
        String value;
        int id = (int) context.get(Context.ID);
        bean.clear();
        bean.setId(id);
        bean.getErrors().clear();

        value = array[i++];
        testExtraSpace(FIELDS.table_name.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            try {
                bean.setTableName(STRING_CONVERTER.from(context, FIELDS.table_name, value, true));
            } catch (GtfsException ex) {
                if (withValidation)
                    bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.table_name.name()), FIELDS.table_name.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
            }
        }

        value = array[i++];
        testExtraSpace(FIELDS.field_name.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            try {
                bean.setFieldName(STRING_CONVERTER.from(context, FIELDS.field_name, value, true));
            } catch (GtfsException ex) {
                if (withValidation)
                    bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.field_name.name()), FIELDS.field_name.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
            }
        }

        value = array[i++];
        testExtraSpace(FIELDS.language.name(), value, bean);
        if (StringUtils.isNotBlank(value)) {
            try {
                bean.setLanguage(STRING_CONVERTER.from(context, FIELDS.language, value, true));
            } catch (GtfsException ex) {
                if (withValidation)
                    bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.language.name()), FIELDS.language.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
            }
        }

        value = array[i++];
        if (StringUtils.isNotBlank(value)) {
            try {
                bean.setTranslation(STRING_CONVERTER.from(context, FIELDS.translation, value, false));
            } catch (GtfsException ex) {
                if (withValidation)
                    bean.getErrors().add(new GtfsException(_path, id, getIndex(FIELDS.translation.name()), FIELDS.translation.name(), GtfsException.ERROR.INVALID_FORMAT, null, value));
            }
        }

        value = array[i++];
        if (StringUtils.isNotBlank(value)) {
            bean.setRecordId(STRING_CONVERTER.from(context, FIELDS.record_id, value, false));
        }

        value = array[i++];
        if (StringUtils.isNotBlank(value)) {
            bean.setRecordSubId(STRING_CONVERTER.from(context, FIELDS.record_sub_id, value, false));
        }

        value = array[i++];
        if (StringUtils.isNotBlank(value)) {
            bean.setFieldValue(STRING_CONVERTER.from(context, FIELDS.field_value, value, false));
        }

        return bean;
    }

    @Override
    public boolean validate(GtfsTranslation bean, GtfsImporter dao) {
        return true;
    }

    public enum FIELDS {
        table_name, field_name, language, translation, record_id, record_sub_id, field_value
    }

    public static class DefaultImporterFactory extends IndexFactory {
        @SuppressWarnings("rawtypes")
        @Override
        protected Index create(String name) throws IOException {
            return new TranslationIndex(name, KEY, false);
        }
    }

}
