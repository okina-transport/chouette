package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "stop_area_translations")
@NoArgsConstructor
@ToString(callSuper = true, exclude = "stopArea")
public class StopAreaTranslation extends Translation {

	@Getter
	@Setter
	@SequenceGenerator(name = "stop_area_translations_id_seq", sequenceName = "stop_area_translations_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "stop_area_translations_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	@Getter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stop_area_id")
	private StopArea stopArea;

	public void setStopArea(StopArea stopArea) {
		if (this.stopArea != null) {
			this.stopArea.getTranslations().remove(this);
		}
		this.stopArea = stopArea;
		if (stopArea != null) {
			stopArea.getTranslations().add(this);
		}
	}
}
