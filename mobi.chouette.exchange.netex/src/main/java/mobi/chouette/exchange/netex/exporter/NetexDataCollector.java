package mobi.chouette.exchange.netex.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.exporter.DataCollector;
import mobi.chouette.model.Line;

import java.time.LocalDate;

@Log4j
public class NetexDataCollector extends DataCollector {
	public NetexDataCollector(ExportableData collection, Line line, LocalDate startDate, LocalDate endDate) {
		super(collection, line, startDate, endDate, false, false, false);
	}

	public boolean collect() {
       boolean res =  collect();
		if (line.getNetwork() == null) {
			log.error("line " + line.getObjectId() + " : missing network");
			return false;
		}
		if (line.getCompany() == null) {
			log.error("line " + line.getObjectId() + " : missing company");
			return false;
		}
		return res;
	}


}
