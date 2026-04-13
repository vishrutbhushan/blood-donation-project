package etl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donor {
    private String source;
    private String donorId;
    private String name;
    private String bloodGroup;
    private Integer age;
    private String phone;
    private String email;
    private String addressCurrent;
    private String cityCurrent;
    private String stateCurrent;
    private String pincodeCurrent;
    private Double lat;
    private Double lon;
    private String bankId;
    private String lastDonatedOn;
    private String lastDonatedBloodBank;
    private String updatedAt;
    private String op;
}
