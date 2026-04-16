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

    public long getLastSyncTs(String source) {
        Object sourceState = state.get(source);
        if (!(sourceState instanceof Map<?, ?> map)) {
            return Constants.INITIAL_PULL_START_TS;
        }
        Object value = map.get(Constants.KEY_LAST_SYNC);
        if (value == null) {
            return Constants.INITIAL_PULL_START_TS;
        }
        return Long.parseLong(String.valueOf(value));
    }

    public void setLastSyncTs(String source, long ts) {
        Map<String, Object> sourceState = new LinkedHashMap<>();
        sourceState.put(Constants.KEY_LAST_SYNC, String.valueOf(ts));
        state.put(source, sourceState);
    }

    public void save() {
        json.writeFile(statePath(), state);
    }

    private String statePath() {
        return Constants.DATA_DIR + "/state.json";
    }
}