package org.avni.web.request.export;

import java.util.ArrayList;
import java.util.List;

public class ExportOutput extends ExportEntityType {
    private List<ExportEntityType> encounters = new ArrayList<>();
    private List<ExportNestedOutput> groups = new ArrayList<>();
    private List<ExportNestedOutput> programs = new ArrayList<>();

    public List<ExportEntityType> getEncounters() {
        return encounters;
    }

    public void setEncounters(List<ExportEntityType> encounters) {
        this.encounters = encounters;
    }

    public List<ExportNestedOutput> getGroups() {
        return groups;
    }

    public void setGroups(List<ExportNestedOutput> groups) {
        this.groups = groups;
    }

    public List<ExportNestedOutput> getPrograms() {
        return programs;
    }

    public void setPrograms(List<ExportNestedOutput> programs) {
        this.programs = programs;
    }

    private static class ExportNestedOutput extends ExportEntityType {
        private List<ExportEntityType> encounters = new ArrayList<>();

        public List<ExportEntityType> getEncounters() {
            return encounters;
        }

        public void setEncounters(List<ExportEntityType> encounters) {
            this.encounters = encounters;
        }
    }
}
