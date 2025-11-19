package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;

import org.rutebanken.netex.model.BrandingRefStructure;
import org.rutebanken.netex.model.ContactStructure;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.OrganisationTypeEnumeration;
import org.rutebanken.netex.model.Organisation_VersionStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;

public class OrganisationProducer extends NetexProducer implements NetexEntityProducer<Organisation_VersionStructure, Company> {

	private static KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();
	
	@Override
	public Organisation_VersionStructure produce(Context context, Company company) {
		
		Organisation_VersionStructure organisation = null;
		NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
		
		if(OrganisationTypeEnum.Operator.equals(company.getOrganisationType())) {
			Operator operator = netexFactory.createOperator();

			if (isSet(company.getPublicPhone(), company.getPublicEmail(), company.getPublicUrl())) {
				ContactStructure contactStructure = netexFactory.createContactStructure();
				contactStructure.setPhone(company.getPublicPhone());
				contactStructure.setEmail(company.getPublicEmail());
				contactStructure.setUrl(company.getPublicUrl());
				operator.setCustomerServiceContactDetails(contactStructure);
			}
			organisation = operator;
		} else if(OrganisationTypeEnum.Authority.equals(company.getOrganisationType())) {
			organisation = netexFactory.createAuthority();
		} else {
			organisation = netexFactory.createGeneralOrganisation();
		}

		NetexProducerUtils.populateId(company, organisation);

		if(company.getCode() != null) {
			PrivateCodeStructure privateCodeStructure = netexFactory.createPrivateCodeStructure().withValue(company.getCode());
			// TODO à revoir pour changement de profil
			organisation.setPublicCode(privateCodeStructure);
//			organisation.setPublicCode(privateCodeStructure.getValue());
		}
		organisation.setCompanyNumber(company.getRegistrationNumber());
		organisation.setName(ConversionUtil.getMultiLingualString(company.getName()));
		organisation.setLegalName(ConversionUtil.getMultiLingualString(company.getLegalName()));
		organisation.setShortName(ConversionUtil.getMultiLingualString(company.getShortName()));
		organisation.setKeyList(keyListStructureProducer.produce(company.getKeyValues(), configuration.isExportExternalIds()));

		if (isSet(company.getPhone(), company.getUrl())) {
			ContactStructure contactStructure = netexFactory.createContactStructure();
			contactStructure.setPhone(company.getPhone());
			contactStructure.setUrl(company.getUrl());
			organisation.setContactDetails(contactStructure);
		}

		if (isSet(company.getOrganisationType())) {
			OrganisationTypeEnumeration organisationTypeEnumeration = NetexProducerUtils.getOrganisationTypeEnumeration(company.getOrganisationType());
			organisation.getOrganisationType().add(organisationTypeEnumeration);
		} else {
			organisation.getOrganisationType().add(OrganisationTypeEnumeration.OTHER);
		}

		if (isSet(company.getBranding())){
			BrandingRefStructure brandingRefStructure = netexFactory.createBrandingRefStructure();
			NetexProducerUtils.populateReference(company.getBranding(), brandingRefStructure, true);
			organisation.setBrandingRef(brandingRefStructure);
		}

		return organisation;
	}
}
