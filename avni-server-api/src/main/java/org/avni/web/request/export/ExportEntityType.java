package org.avni.web.request.export;

import java.util.ArrayList;
import java.util.List;

public class ExportEntityType {
    private String uuid;
    private List<String> fields = new ArrayList<>();
    private long maxCount;

    public long getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(long maxCount) {
        this.maxCount = maxCount;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }
}
