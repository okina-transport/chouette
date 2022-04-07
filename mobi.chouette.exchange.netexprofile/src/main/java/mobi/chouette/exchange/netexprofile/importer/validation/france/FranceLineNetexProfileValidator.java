package mobi.chouette.exchange.netexprofile.importer.validation.france;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.importer.util.DataLocationHelper;
import mobi.chouette.exchange.netexprofile.importer.util.IdVersion;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidatorFactory;
import mobi.chouette.exchange.netexprofile.util.NetexIdExtractorHelper;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.model.Codespace;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

import javax.xml.xpath.XPathExpressionException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j
public class FranceLineNetexProfileValidator extends AbstractFranceNetexProfileValidator implements NetexProfileValidator {

    public static final String NAME = "IDFMLineNetexProfileValidator";
    public static final String NETEX_FRANCE_RESEAU_PROFILE = "1.1:FR-NETEX_RESEAU-2.2";


    public FranceLineNetexProfileValidator() {
    }

    @Override
    public void validate(Context context) throws Exception {
        XPathCompiler xpath = (XPathCompiler) context.get(NETEX_XPATH_COMPILER);

        XdmNode dom = (XdmNode) context.get(NETEX_DATA_DOM);

        Set<Codespace> validCodespaces = (Set<Codespace>) context.get(NETEX_VALID_CODESPACES);
        ValidationData data = (ValidationData) context.get(VALIDATION_DATA);

        List<IdVersion> localIdList = NetexIdExtractorHelper.collectEntityIdentificators(context, xpath, dom, new HashSet<>(Arrays.asList("CompositeFrame", "GeneralFrame")));
        Set<IdVersion> localIds = new HashSet<>(localIdList);
        List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(context, xpath, dom, new HashSet<>(Arrays.asList("QuayRef", "TypeOfFrameRef")));

        for (IdVersion id : localIds) {
            data.getDataLocations().put(id.getId(), DataLocationHelper.findDataLocation(id));
        }

        verifyAcceptedCodespaces(context, xpath, dom, validCodespaces);
        verifyIdStructure(context, localIds, ID_STRUCTURE_REGEXP, validCodespaces);

        verifyNoDuplicatesAcrossLineFiles(context, localIdList, new HashSet<>());

        verifyUseOfVersionOnLocalElements(context, localIds);
        verifyUseOfVersionOnRefsToLocalElements(context, localIds, localRefs);
        verifyReferencesToCorrectEntityTypes(context, localRefs);

        XdmValue compositeFrames = selectNodeSet("/PublicationDelivery/dataObjects/CompositeFrame", xpath, dom);
        if (compositeFrames.size() > 0) {
            // Using composite frames
            for (XdmItem compositeFrame : compositeFrames) {
                validateCompositeFrame(context, xpath, (XdmNode) compositeFrame);
            }
        }
    }

    private void validateCompositeFrame(Context context, XPathCompiler xpath, XdmNode dom) throws XPathExpressionException, SaxonApiException {

        XdmValue generalFrames = selectNodeSet("frames/GeneralFrame", xpath, dom);
        for (XdmItem generalFrame : generalFrames) {
            validateGeneralFrame(context, xpath, (XdmNode) generalFrame, null);
        }
    }

    public static class DefaultValidatorFactory extends NetexProfileValidatorFactory {
        @Override
        protected NetexProfileValidator create(Context context) throws ClassNotFoundException {
            NetexProfileValidator instance = (NetexProfileValidator) context.get(FranceLineNetexProfileValidator.class.getName());
            if (instance == null) {
                instance = new FranceLineNetexProfileValidator();
                context.put(FranceLineNetexProfileValidator.class.getName(), instance);
            }
            return instance;
        }
    }

    static {
        NetexProfileValidatorFactory.factories.put(FranceLineNetexProfileValidator.class.getName(),
                new FranceLineNetexProfileValidator.DefaultValidatorFactory());
    }

    @Override
    public boolean isCommonFileValidator() {
        return false;
    }

    @Override
    public Collection<String> getSupportedProfiles() {
        return Arrays.asList(new String[] {NETEX_FRANCE_RESEAU_PROFILE});
    }
}
