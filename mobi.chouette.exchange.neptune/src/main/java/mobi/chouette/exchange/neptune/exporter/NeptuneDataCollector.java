package mobi.chouette.exchange.neptune.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.exporter.DataCollector;
import mobi.chouette.model.Line;

import java.time.LocalDate;

@Log4j
public class NeptuneDataCollector extends DataCollector {
	public boolean collect(ExportableData collection, Line line, LocalDate startDate, LocalDate endDate) {
		boolean res =  collect(collection, line, startDate, endDate, false, false);
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
