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
@Table(name = "company_translations")
@NoArgsConstructor
@ToString(callSuper = true, exclude = "company")
public class CompanyTranslation extends Translation {

	@Getter
	@Setter
	@SequenceGenerator(name = "company_translations_id_seq", sequenceName = "company_translations_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_translations_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	@Getter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_id")
	private Company company;

	public void setCompany(Company company) {
		if (this.company != null) {
			this.company.getTranslations().remove(this);
		}
		this.company = company;
		if (company != null) {
			company.getTranslations().add(this);
		}
	}
}
