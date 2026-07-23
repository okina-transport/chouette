package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.NetexprofileExportParameters;
import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;
import org.rutebanken.netex.model.*;

import static mobi.chouette.common.Constant.CONFIGURATION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;

public class OrganisationFranceProducer extends NetexProducer implements NetexEntityProducer<Organisation_VersionStructure, Company> {

    private static final KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();

    @Override
    public Organisation_VersionStructure produce(Context context, Company company) {

        Organisation_VersionStructure organisation = null;
        NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

        if (OrganisationTypeEnum.Operator.equals(company.getOrganisationType())) {
            Operator operator = netexFactory.createOperator();

            if (isSet(company.getPublicPhone(), company.getPublicEmail(), company.getPublicUrl())) {
                ContactStructure contactStructure = netexFactory.createContactStructure();
                contactStructure.setPhone(company.getPublicPhone());
                contactStructure.setEmail(company.getPublicEmail());
                contactStructure.setUrl(company.getPublicUrl());
                operator.setCustomerServiceContactDetails(contactStructure);
            }
            organisation = operator;
        } else if (OrganisationTypeEnum.Authority.equals(company.getOrganisationType())) {
            organisation = netexFactory.createAuthority();
        } else {
            organisation = netexFactory.createGeneralOrganisation();
        }

        NetexProducerUtils.populateIdAndVersion(company, organisation);
        NetexProducerUtils.addAlternateIdentifier(organisation, company.getObjectId());

        if (company.getCode() != null) {
            PrivateCodeStructure privateCodeStructure = netexFactory.createPrivateCodeStructure().withValue(company.getCode());
            // TODO à revoir pour changement de profil
            organisation.setPublicCode(privateCodeStructure);
//			organisation.setPublicCode(privateCodeStructure.getValue());
        }
        organisation.setCompanyNumber(company.getRegistrationNumber());
        organisation.setName(ConversionUtil.getMultiLingualString(company.getName()));
        organisation.setLegalName(ConversionUtil.getMultiLingualString(company.getLegalName()));
        organisation.setShortName(ConversionUtil.getMultiLingualString(company.getShortName()));
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.List<mobi.chouette.model.Translation>> companyFieldValueTranslations =
                (java.util.Map<String, java.util.List<mobi.chouette.model.Translation>>) context.get(mobi.chouette.exchange.netexprofile.Constant.COMPANY_FIELD_VALUE_TRANSLATIONS);
        NetexProducerUtils.addAlternativeTexts(organisation,
                NetexProducerUtils.getTranslations(company.getTranslations(), companyFieldValueTranslations, "name", company.getName()), "name", "Name");
        NetexProducerUtils.addAlternativeTexts(organisation,
                NetexProducerUtils.getTranslations(company.getTranslations(), companyFieldValueTranslations, "phone", company.getPhone()), "phone", "Phone");
        NetexProducerUtils.addAlternativeTexts(organisation,
                NetexProducerUtils.getTranslations(company.getTranslations(), companyFieldValueTranslations, "url", company.getUrl()), "url", "Url");
        NetexProducerUtils.addAlternativeTexts(organisation,
                NetexProducerUtils.getTranslations(company.getTranslations(), companyFieldValueTranslations, "email", company.getEmail()), "email", "Email");
        organisation.setKeyList(keyListStructureProducer.produce(company.getKeyValues(), configuration.isExportExternalIds()));

        if (isSet(company.getPhone(), company.getUrl(), company.getEmail())) {
            ContactStructure contactStructure = netexFactory.createContactStructure();
            contactStructure.setPhone(company.getPhone());
            contactStructure.setUrl(company.getUrl());
            contactStructure.setEmail(company.getEmail());
            organisation.setContactDetails(contactStructure);
        }

        if (isSet(company.getOrganisationType())) {
            OrganisationTypeEnumeration organisationTypeEnumeration = NetexProducerUtils.getOrganisationTypeEnumeration(company.getOrganisationType());
            organisation.getOrganisationType().add(organisationTypeEnumeration);
        } else {
            organisation.getOrganisationType().add(OrganisationTypeEnumeration.OTHER);
        }

        if (isSet(company.getBranding())) {
            BrandingRefStructure brandingRefStructure = netexFactory.createBrandingRefStructure();
            NetexProducerUtils.populateReferenceIDFM(company.getBranding(), brandingRefStructure);
            organisation.setBrandingRef(brandingRefStructure);
        }

        return organisation;
    }
}

