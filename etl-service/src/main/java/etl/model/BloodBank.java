package etl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodBank {
    private String bankId;
    private String bankName;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private Double lat;
    private Double lon;
    private String updatedAt;
    private String op;
}
