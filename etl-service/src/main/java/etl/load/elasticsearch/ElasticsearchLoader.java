package etl.load.elasticsearch;

import etl.constants.Constants;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.util.JsonUtil;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ElasticsearchLoader {
    private final JsonUtil jsonUtil;
    private final RestClient elastic;

    public ElasticsearchLoader(JsonUtil jsonUtil, RestClient.Builder builder) {
        this.jsonUtil = jsonUtil;
        this.elastic = builder.baseUrl(Constants.ELASTIC_URL).build();
    }

    public void loadBanks(List<BloodBank> banks) {
        if (banks == null || banks.isEmpty()) {
            return;
        }
        for (BloodBank b : banks) {
            String id = str(b.getBankId());
            if (Constants.OP_DELETE.equalsIgnoreCase(str(b.getOp()))) {
                elastic.delete().uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_BANKS, id).retrieve().toBodilessEntity();
            } else {
                elastic.post()
                    .uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_BANKS, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonUtil.toJson(b))
                    .retrieve()
                    .toBodilessEntity();
            }
        }
    }

    public void loadDonors(List<Donor> donors) {
        if (donors == null || donors.isEmpty()) {
            return;
        }
        for (Donor d : donors) {
            String id = str(d.getDonorId());
            if (Constants.OP_DELETE.equalsIgnoreCase(str(d.getOp()))) {
                elastic.delete().uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_DONORS, id).retrieve().toBodilessEntity();
            } else {
                elastic.post()
                    .uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_DONORS, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonUtil.toJson(d))
                    .retrieve()
                    .toBodilessEntity();
            }
        }
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
