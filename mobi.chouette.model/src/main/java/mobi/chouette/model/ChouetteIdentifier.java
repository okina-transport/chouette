package mobi.chouette.model;

import lombok.*;

import javax.validation.constraints.NotEmpty;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
public class ChouetteIdentifier {
    @NotEmpty
    private String dataset;

    @NotEmpty
    private String id;

}
