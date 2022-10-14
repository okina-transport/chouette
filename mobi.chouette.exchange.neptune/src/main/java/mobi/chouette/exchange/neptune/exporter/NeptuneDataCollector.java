package mobi.chouette.exchange.neptune.exporter;

import lombok.extern.slf4j.Slf4j;;
import mobi.chouette.exchange.exporter.DataCollector;
import mobi.chouette.model.Line;

import java.time.LocalDate;

@Slf4j
public class NeptuneDataCollector extends DataCollector {
	public NeptuneDataCollector(mobi.chouette.exchange.exporter.ExportableData collection, Line line, LocalDate startDate, LocalDate endDate) {
		super(collection, line, startDate, endDate, false, false, true);
	}

	public boolean collect() {
		boolean res =  super.collect();
		if (line.getNetwork() == null) {
            log.error("line {} : missing network", line.getObjectId());
			return false;
		}
		if (line.getCompany() == null) {
            log.error("line {} : missing company", line.getObjectId());
			return false;
		}
		return res;
	}



}
