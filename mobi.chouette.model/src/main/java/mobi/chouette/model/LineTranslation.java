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
@Table(name = "line_translations")
@NoArgsConstructor
@ToString(callSuper = true, exclude = "line")
public class LineTranslation extends Translation {

	@Getter
	@Setter
	@SequenceGenerator(name = "line_translations_id_seq", sequenceName = "line_translations_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "line_translations_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	@Getter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "line_id")
	private Line line;

	public void setLine(Line line) {
		if (this.line != null) {
			this.line.getTranslations().remove(this);
		}
		this.line = line;
		if (line != null) {
			line.getTranslations().add(this);
		}
	}
}
