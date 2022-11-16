package mobi.chouette.exchange.mapmatching;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.JSONUtil;
import mobi.chouette.exchange.AbstractInputValidator;
import mobi.chouette.exchange.InputValidator;
import mobi.chouette.exchange.InputValidatorFactory;
import mobi.chouette.exchange.TestDescription;
import mobi.chouette.exchange.parameters.AbstractParameter;
import mobi.chouette.exchange.validation.parameters.ValidationParameters;


import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Log4j
public class MapmatchingInputValidator extends AbstractInputValidator {

    @Override
    public AbstractParameter toActionParameter(String abstractParameter) {
        try {
            return JSONUtil.fromJSON(abstractParameter, MapmatchingParameters.class);
        } catch (Exception e) {
            return null;
        }    }

    @Override
    public boolean checkParameters(String abstractParameterString, String validationParametersString) {
        try {
            MapmatchingParameters parameters = JSONUtil.fromJSON(abstractParameterString, MapmatchingParameters.class);

            ValidationParameters validationParameters = JSONUtil.fromJSON(validationParametersString,
                    ValidationParameters.class);

            return checkParameters(parameters, validationParameters);
        } catch (Exception ex) {
            log.error(ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean checkParameters(AbstractParameter abstractParameter, ValidationParameters validationParameters) {
        if (!(abstractParameter instanceof MapmatchingParameters)) {
            log.error("invalid parameters for map matching " + abstractParameter.getClass().getName());
            return false;
        }

        return true;    }

    @Override
    public boolean checkFilename(String fileName) {
        if (fileName != null) {
            log.error("input data not expected");
            return false;
        }

        return true;
    }

    @Override
    public boolean checkFile(String fileName, Path filePath, AbstractParameter abstractParameter) {
        return false;
    }

    @Override
    public List<TestDescription> getTestList() {
        return null;
    }

    public static class DefaultFactory extends InputValidatorFactory {
        @Override
        protected InputValidator create() throws IOException {
            InputValidator result = new MapmatchingInputValidator();
            return result;
        }
    }

    static {
        InputValidatorFactory.factories.put(MapmatchingInputValidator.class.getName(), new DefaultFactory());
    }
}
