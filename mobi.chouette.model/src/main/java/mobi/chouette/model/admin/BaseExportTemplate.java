package mobi.chouette.model.admin;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseExportTemplate {

    @Id
    @SequenceGenerator(name = "EXPORT_TEMPLATE_ID_SEQ", sequenceName = "EXPORT_TEMPLATE_ID_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXPORT_TEMPLATE_ID_SEQ")
    protected Long id;
    protected String name;
    protected String description;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    protected ExportType type;

    @Column(name = "exported_filename")
    protected String exportedFileName;

    @Column(name = "line_id_prefix")
    protected String lineIdPrefix;

    @Column(name = "stop_id_prefix")
    protected String stopIdPrefix;

    @Column(name = "id_format")
    @Enumerated(EnumType.STRING)
    protected IdFormat idFormat;

    @Column(name = "id_suffix")
    protected String idSuffix;

    @Column(name = "commercial_point_id_prefix")
    protected String commercialPointIdPrefix;

    @Column(name = "commercial_point_export")
    protected Boolean commercialPointExport;

    @Column(name = "google_maps_compatibility")
    protected Boolean googleMapsCompatibility;

    @ElementCollection (fetch = FetchType.EAGER)
    @CollectionTable(name = "export_template_referential", joinColumns = {@JoinColumn(name = "export_template_id")})
    @Column(name = "referential")
    protected Set<String> referentials = new HashSet<>();

    @Column(name = "export_enabled")
    protected Boolean exportEnabled;

    @Column(name = "generate_attribution")
    @Enumerated(EnumType.STRING)
    protected AttributionsExportModes attributionsExportModes;

    @Column(name = "post_process")
    protected String postProcess;

    @Column(name = "export_job_id")
    protected Long exportJobId;

    @Column(name = "use_extended_gtfs_route_types")
    protected Boolean useExtendedGtfsRouteTypes;

    @Column(name = "agency_id")
    protected String agencyId;

    @Column(name = "agency_name")
    protected String agencyName;

    @Column(name = "agency_url")
    protected String agencyURL;

    @Column(name = "agency_timezone")
    protected String agencyTimezone;

    @ManyToMany
    @LazyCollection(LazyCollectionOption.FALSE)
    @JoinTable(name = "consumer_export_template", joinColumns = {@JoinColumn(name = "export_template_id", nullable = false, updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "consumer_id", updatable = false)})
    protected List<Consumer> consumers = new ArrayList<>();

    @OneToMany(mappedBy = "exportTemplate")
    protected Set<GlobalExportMonitoring> globalExportMonitorings;

    @Column(name = "agency_lang")
    protected String agencyLang;

}