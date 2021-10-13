package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;

@Log4j
public class OrganisationParser implements Parser, Constant {

	@Override
	public void parse(Context context) throws Exception {
		Referential referential = (Referential) context.get(REFERENTIAL);
		OrganisationsInFrame_RelStructure organisationsInFrameStruct = (OrganisationsInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);

		NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);

		for (JAXBElement<? extends DataManagedObjectStructure> organisationElement : organisationsInFrameStruct.getOrganisation_()) {
			DataManagedObjectStructure organisation = organisationElement.getValue();
			Organisation_VersionStructure organisationStruct = (Organisation_VersionStructure) organisation;
			boolean isOperator = false;
			String organisationId = organisation.getId();
			String generatedOrganisationId = null;

			if (organisationStruct instanceof Operator) {
				isOperator = true;
				generatedOrganisationId = NetexImportUtil.composeOperatorIdFromNetexId(parameters.getObjectIdPrefix(),organisationId);
			}else{
				generatedOrganisationId = NetexImportUtil.composeObjectIdFromNetexId("Authority",parameters.getObjectIdPrefix(),organisationId);
			}

			Company company = ObjectFactory.getCompany(referential, generatedOrganisationId);
			company.setObjectVersion(NetexParserUtils.getVersion(organisation));
			company.setName(ConversionUtil.getValue(organisationStruct.getName()));
			company.setLegalName(ConversionUtil.getValue(organisationStruct.getLegalName()));
			company.setRegistrationNumber(StringUtils.trimToNull(organisationStruct.getCompanyNumber()));

			if(organisationStruct.getPublicCode() != null) {
				company.setCode(organisationStruct.getPublicCode().getValue());

				// TODO à revoir pour changement de profil
//				company.setCode(organisationStruct.getPublicCode());
			}
			
			// Find type of organisation
			OrganisationTypeEnumeration organisationTypeEnumeration = null;
			if (CollectionUtils.isNotEmpty(organisationStruct.getOrganisationType())) {
				organisationTypeEnumeration = organisationStruct.getOrganisationType().get(0);
				OrganisationTypeEnum organisationType = NetexParserUtils.getOrganisationType(organisationTypeEnumeration);
				if (organisationType != null) {
					company.setOrganisationType(organisationType);
				}
			} else {
				company.setOrganisationType(isOperator ? OrganisationTypeEnum.Operator : OrganisationTypeEnum.Authority);
			}

			// Contact details
			if (organisationStruct.getContactDetails() != null) {
				company.setPhone(StringUtils.trimToNull(organisationStruct.getContactDetails().getPhone()));
				company.setUrl(StringUtils.trimToNull(organisationStruct.getContactDetails().getUrl()));
				company.setEmail(StringUtils.trimToNull(organisationStruct.getContactDetails().getEmail()));
			}

			// Customer service contact details
			if (organisationStruct instanceof Operator) {
				Operator operator = (Operator) organisationStruct;

				if (operator.getCustomerServiceContactDetails() != null) {
					ContactStructure customerServiceContactDetails = operator.getCustomerServiceContactDetails();
					company.setPublicPhone(StringUtils.trimToNull(customerServiceContactDetails.getPhone()));
					company.setPublicEmail(StringUtils.trimToNull(customerServiceContactDetails.getEmail()));
					company.setPublicUrl(StringUtils.trimToNull(customerServiceContactDetails.getUrl()));
				}
			}

			company.setFilled(true);

			if (organisationStruct.getBrandingRef() != null) {
				mobi.chouette.model.Branding branding = ObjectFactory.getBranding(referential, organisation.getBrandingRef().getRef());
				company.setBranding(branding);
			}
		}
	}

	static {
		ParserFactory.register(OrganisationParser.class.getName(), new ParserFactory() {
			private OrganisationParser instance = new OrganisationParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}

}
