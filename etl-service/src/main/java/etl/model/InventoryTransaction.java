package etl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransaction {
    private String source;
    private String transactionId;
    private String sourceEventId;
    private String bankId;
    private String donorId;
    private String bloodGroup;
    private String component;
    private String transactionType;
    private Integer unitsDelta;
    private Integer runningBalanceAfter;
    private String expiryDate;
    private String eventTimestamp;
    private String updatedAt;
    private String op;
}