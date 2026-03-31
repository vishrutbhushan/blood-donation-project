package etl.load.clickhouse;

import etl.constants.Constants;
import etl.constants.QueryConstants;
import etl.model.BloodBank;
import etl.model.Donor;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ClickhouseLoader {
    private final RestClient clickhouse;

    public ClickhouseLoader(RestClient.Builder builder) {
        this.clickhouse = builder.baseUrl(Constants.CLICKHOUSE_URL).build();
    }

    public void loadBanks(List<BloodBank> banks) {
        if (banks == null || banks.isEmpty()) {
            return;
        }
        for (BloodBank b : banks) {
            if (Constants.OP_DELETE.equalsIgnoreCase(str(b.getOp()))) {
                sql(String.format(QueryConstants.CLICKHOUSE_DELETE_BANK, esc(str(b.getBankId()))));
            } else {
                sql(String.format(
                    QueryConstants.CLICKHOUSE_UPSERT_BANK,
                    esc(str(b.getBankId())),
                    esc(str(b.getBankName())),
                    esc(str(b.getAddress())),
                    esc(str(b.getCity())),
                    esc(str(b.getState())),
                    esc(str(b.getPincode())),
                    esc(str(b.getPhone())),
                    num(b.getLat()),
                    num(b.getLon()),
                    esc(str(b.getUpdatedAt()))
                ));
            }
        }
    }

    public void loadDonors(List<Donor> donors) {
        if (donors == null || donors.isEmpty()) {
            return;
        }
        for (Donor d : donors) {
            if (Constants.OP_DELETE.equalsIgnoreCase(str(d.getOp()))) {
                sql(String.format(QueryConstants.CLICKHOUSE_DELETE_DONOR, esc(str(d.getDonorId()))));
            } else {
                sql(String.format(
                    QueryConstants.CLICKHOUSE_UPSERT_DONOR,
                    esc(str(d.getDonorId())),
                    esc(str(d.getName())),
                    esc(str(d.getBloodGroup())),
                    esc(str(d.getPhone())),
                    esc(str(d.getEmail())),
                    esc(str(d.getAddressCurrent())),
                    esc(str(d.getCityCurrent())),
                    esc(str(d.getStateCurrent())),
                    esc(str(d.getPincodeCurrent())),
                    num(d.getLat()),
                    num(d.getLon()),
                    esc(str(d.getBankId())),
                    esc(str(d.getLastDonatedOn())),
                    esc(str(d.getUpdatedAt()))
                ));
            }
        }
    }

    private void sql(String query) {
        clickhouse.post()
            .contentType(MediaType.TEXT_PLAIN)
            .body(query)
            .retrieve()
            .body(String.class);
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("'", "''");
    }

    private String num(Object n) {
        if (n == null || String.valueOf(n).isBlank()) {
            return "null";
        }
        return String.valueOf(n);
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
