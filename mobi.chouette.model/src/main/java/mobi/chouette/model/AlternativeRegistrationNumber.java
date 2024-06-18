package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import javax.persistence.*;

@Entity
@Table(name = "alternative_registration_numbers")
@Cacheable
@NoArgsConstructor
public class AlternativeRegistrationNumber {

    @Id
    @Getter
    @Setter
    @Column(name = "original_registration_number", nullable = false)
    private String originalRegistrationNumber;

    @Getter
    @Setter
    @Column(name = "alternative_registration_number", nullable = false, unique = true)
    protected String alternativeRegistrationNumber;

}
