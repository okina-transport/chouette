package mobi.chouette.model.admin;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="export_template")
@NoArgsConstructor
public class ExportTemplate extends BaseExportTemplate {


}
