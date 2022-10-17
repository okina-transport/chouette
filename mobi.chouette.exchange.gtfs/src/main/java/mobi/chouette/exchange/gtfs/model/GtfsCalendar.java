package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsCalendar extends GtfsObject implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	private String serviceId;

	@Getter
	@Setter
	private Boolean monday;

	@Getter
	@Setter
	private Boolean tuesday;

	@Getter
	@Setter
	private Boolean wednesday;

	@Getter
	@Setter
	private Boolean thursday;

	@Getter
	@Setter
	private Boolean friday;

	@Getter
	@Setter
	private Boolean saturday;

	@Getter
	@Setter
	private Boolean sunday;

	@Getter
	@Setter
	private LocalDate startDate;

	@Getter
	@Setter
	private LocalDate endDate;

	/*
	 * @Override public String toString() { return id + ":" +
	 * CalendarExporter.CONVERTER.to(new Context(),this); }
	 */
}
