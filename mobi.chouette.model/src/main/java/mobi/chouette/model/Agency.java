package mobi.chouette.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "agency")
@Cacheable
@NoArgsConstructor
public class Agency {
    @Id
    @SequenceGenerator(name = "agency_id_seq", sequenceName = "agency_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "agency_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter
    @Setter
    @Column(name = "agency_id")
    private String agencyId;

    @Getter
    @Setter
    @Column(name = "name")
    private String name;

    @Getter
    @Setter
    @Column(name = "url")
    private String url;

    @Getter
    @Setter
    @Column(name = "timezone")
    private String timezone;

    @Getter
    @Setter
    @Column(name = "lang")
    private String lang;

    @Getter
    @Setter
    @Column(name = "phone")
    private String phone;

    @Getter
    @Setter
    @Column(name = "fare_url")
    private String fareURL;

    @Getter
    @Setter
    @Column(name = "email")
    private String email;

}
