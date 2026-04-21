package etl.service;

import etl.constants.Constants;
import etl.util.JsonUtil;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EtlStateStore {
    private final JsonUtil json;
    private Map<String, Object> state = new LinkedHashMap<>();

    public EtlStateStore(JsonUtil json) {
        this.json = json;
    }

    public void load() {
        Map<String, Object> loaded = json.readFileMap(statePath());
        if (loaded == null) {
            state = new LinkedHashMap<>();
            return;
        }
        state = loaded;
    }

    public long getEsLastSyncTs(String source) {
        Object esState = state.get(Constants.KEY_ES_SYNC);
        if (!(esState instanceof Map<?, ?> map)) {
            return Constants.INITIAL_PULL_START_TS;
        }
        Object sourceValue = map.get(source);
        if (!(sourceValue instanceof Map<?, ?> sourceMap)) {
            return Constants.INITIAL_PULL_START_TS;
        }
        Object value = sourceMap.get(Constants.KEY_LAST_SYNC);
        if (value == null) {
            return Constants.INITIAL_PULL_START_TS;
        }
        return Long.parseLong(String.valueOf(value));
    }

    public void setEsLastSyncTs(String source, long ts) {
        Map<String, Object> esState = new LinkedHashMap<>();
        Object existing = state.get(Constants.KEY_ES_SYNC);
        if (existing instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                esState.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        Map<String, Object> sourceState = new LinkedHashMap<>();
        sourceState.put(Constants.KEY_LAST_SYNC, String.valueOf(ts));
        esState.put(source, sourceState);
        state.put(Constants.KEY_ES_SYNC, esState);
    }

    public boolean isBulkDone() {
        Object value = state.get(Constants.KEY_BULK_DONE);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public void setBulkDone(boolean bulkDone) {
        state.put(Constants.KEY_BULK_DONE, bulkDone);
    }

    public void save() {
        json.writeFile(statePath(), state);
    }

    private String statePath() {
        return Constants.DATA_DIR + "/state.json";
    }
}