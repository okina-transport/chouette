package mobi.chouette.model.admin;


import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "global_export_monitoring")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"id", "command", "referential", "status", "type", "created", "updated"})
public class GlobalExportMonitoring {

    @Id
    @SequenceGenerator(name = "GLOBAL_EXPORT_MONITORING_ID_SEQ", sequenceName = "GLOBAL_EXPORT_MONITORING_ID_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_EXPORT_MONITORING_ID_SEQ")
    protected Long id;

    @Column(name = "job_id", nullable = false, updatable = false)
    protected Long jobId;

    protected String command;

    protected String referential;

    @Enumerated(EnumType.STRING)
    protected JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type")
    protected ExportType type;

    @CreationTimestamp
    @Column(name = "created", updatable = false)
    protected Date created;

    @UpdateTimestamp
    @Column(name = "updated")
    protected Date updated;

    @ManyToOne
    @JoinColumn(name = "export_template_id", nullable = false)
    protected ExportTemplate exportTemplate;

}