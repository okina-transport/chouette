package mobi.chouette.exchange.netexprofile.importer.validation.france;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidatorFactory;

import java.util.Arrays;
import java.util.Collection;

public class FranceCommonNetexProfileValidator extends AbstractFranceNetexProfileValidator implements NetexProfileValidator {

    public static final String NETEX_FRANCE_COMMON_PROFILE = "1.1:FR-NETEX_COMMUN-2.2";
    public static final String NETEX_FRANCE_PROFILE = "1.1:FR-NETEX-2.2";


    @Override
    public void validate(Context context) throws Exception {

    }


    public static class DefaultValidatorFactory extends NetexProfileValidatorFactory {
        @Override
        protected NetexProfileValidator create(Context context) throws ClassNotFoundException {
            NetexProfileValidator instance = (NetexProfileValidator) context.get(FranceCommonNetexProfileValidator.class.getName());
            if (instance == null) {
                instance = new FranceCommonNetexProfileValidator();

                context.put(FranceCommonNetexProfileValidator.class.getName(), instance);
            }
            return instance;
        }
    }

    static {
        NetexProfileValidatorFactory.factories.put(FranceCommonNetexProfileValidator.class.getName(),
                new FranceCommonNetexProfileValidator.DefaultValidatorFactory());
    }

    @Override
    public boolean isCommonFileValidator() {
        return false;
    }

    @Override
    public Collection<String> getSupportedProfiles() {
        return Arrays.asList(new String[] {NETEX_FRANCE_COMMON_PROFILE,NETEX_FRANCE_PROFILE});
    }
}
