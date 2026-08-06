package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.Translation;

public abstract class TranslationUpdater<T extends Translation> implements Updater<T> {

    public void update(Context context, T oldValue, T newValue) throws Exception {
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);
        if (oldValue.isDetached()) {
            oldValue.setObjectId(newValue.getObjectId());
            oldValue.setObjectVersion(newValue.getObjectVersion());
            oldValue.setCreationTime(newValue.getCreationTime());
            oldValue.setCreatorId(newValue.getCreatorId());
            oldValue.setFieldName(newValue.getFieldName());
            oldValue.setLanguage(newValue.getLanguage());
            oldValue.setTranslation(newValue.getTranslation());
            oldValue.setFieldValue(newValue.getFieldValue());
        } else {
            if (newValue.getObjectId() != null && !newValue.getObjectId().equals(oldValue.getObjectId())) {
                oldValue.setObjectId(newValue.getObjectId());
            }
            if (newValue.getObjectVersion() != null && !newValue.getObjectVersion().equals(oldValue.getObjectVersion())) {
                oldValue.setObjectVersion(newValue.getObjectVersion());
            }
            if (newValue.getCreationTime() != null && !newValue.getCreationTime().equals(oldValue.getCreationTime())) {
                oldValue.setCreationTime(newValue.getCreationTime());
            }
            if (newValue.getCreatorId() != null && !newValue.getCreatorId().equals(oldValue.getCreatorId())) {
                oldValue.setCreatorId(newValue.getCreatorId());
            }
            if (newValue.getFieldName() != null && !newValue.getFieldName().equals(oldValue.getFieldName())) {
                oldValue.setFieldName(newValue.getFieldName());
            }
            if (newValue.getLanguage() != null && !newValue.getLanguage().equals(oldValue.getLanguage())) {
                oldValue.setLanguage(newValue.getLanguage());
            }
            if (newValue.getTranslation() != null && !newValue.getTranslation().equals(oldValue.getTranslation())) {
                oldValue.setTranslation(newValue.getTranslation());
            }
            if (newValue.getFieldValue() != null && !newValue.getFieldValue().equals(oldValue.getFieldValue())) {
                oldValue.setFieldValue(newValue.getFieldValue());
            }
        }
    }

}
