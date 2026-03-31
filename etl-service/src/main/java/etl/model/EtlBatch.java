package etl.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtlBatch {
    @Builder.Default
    private List<BloodBank> banks = new ArrayList<>();

    @Builder.Default
    private List<Donor> donors = new ArrayList<>();
}
