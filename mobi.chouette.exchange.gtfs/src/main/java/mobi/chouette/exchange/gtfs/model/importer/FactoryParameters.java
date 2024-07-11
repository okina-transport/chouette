package mobi.chouette.exchange.gtfs.model.importer;

public class FactoryParameters {

    private String splitCharacter;
    private String linePrefixToRemove;
    private String commercialPointIdPrefixToRemove;
    private Boolean isRemoveParentStations;

    public String getSplitCharacter() {
        return splitCharacter;
    }

    public void setSplitCharacter(String splitCharacter) {
        this.splitCharacter = splitCharacter;
    }

    public String getLinePrefixToRemove() {
        return linePrefixToRemove;
    }

    public void setLinePrefixToRemove(String linePrefixToRemove) {
        this.linePrefixToRemove = linePrefixToRemove;
    }

    public String getCommercialPointIdPrefixToRemove() {
        return commercialPointIdPrefixToRemove;
    }

    public void setCommercialPointIdPrefixToRemove(String commercialPointIdPrefixToRemove) {
        this.commercialPointIdPrefixToRemove = commercialPointIdPrefixToRemove;
    }

    public Boolean getRemoveParentStations() {
        return isRemoveParentStations;
    }

    public void setRemoveParentStations(Boolean removeParentStations) {
        isRemoveParentStations = removeParentStations;
    }
}
