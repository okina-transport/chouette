package mobi.chouette.exchange.gtfs.model.importer;

import lombok.Getter;
import lombok.Setter;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;

@Getter
@Setter
public class FactoryParameters {

    private String splitCharacter;
    private String linePrefixToRemove;
    private String commercialPointIdPrefixToRemove;
    private Boolean removeParentStations;

    public FactoryParameters() {}

    public FactoryParameters(GtfsImportParameters configuration) {
        this.splitCharacter = configuration.getSplitCharacter();
        this.linePrefixToRemove = configuration.getLinePrefixToRemove();
        this.commercialPointIdPrefixToRemove = configuration.getCommercialPointIdPrefixToRemove();
        this.removeParentStations = configuration.isRemoveParentStations();
    }

}
