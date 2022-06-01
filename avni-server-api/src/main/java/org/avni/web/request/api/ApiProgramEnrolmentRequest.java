package org.avni.web.request.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.avni.geo.Point;
import org.avni.web.api.ProgramEnrolmentFieldNames;

import java.util.LinkedHashMap;

import static org.avni.web.api.CommonFieldNames.VOIDED;
import static org.avni.web.api.ProgramEnrolmentFieldNames.*;

public class ApiProgramEnrolmentRequest {
    @JsonProperty("EXTERNAL_ID")
    private String externalId;

    @JsonProperty("Program")
    private String program;

    @JsonProperty(SUBJECT_ID)
    private String subjectUuid;

    @JsonProperty(ENROLMENT_LOCATION)
    private Point enrolmentLocation;

    @JsonProperty(EXIT_LOCATION)
    private Point exitLocation;

    @JsonProperty(ENROLMENT_DATETIME)
    private DateTime enrolmentDateTime;

    @JsonProperty(EXIT_DATETIME)
    private DateTime exitDateTime;

    @JsonProperty("observations")
    private LinkedHashMap<String, Object> observations;

    @JsonProperty(EXIT_OBS)
    private LinkedHashMap<String, Object> exitObservations;

    @JsonProperty(VOIDED)
    private boolean isVoided;

    public String getProgram() {
        return program;
    }

    public String getSubjectUuid() {
        return subjectUuid;
    }

    public Point getEnrolmentLocation() {
        return enrolmentLocation;
    }

    public Point getExitLocation() {
        return exitLocation;
    }

    public DateTime getEnrolmentDateTime() {
        return enrolmentDateTime;
    }

    public DateTime getExitDateTime() {
        return exitDateTime;
    }

    public LinkedHashMap<String, Object> getObservations() {
        return observations;
    }

    public LinkedHashMap<String, Object> getExitObservations() {
        return exitObservations;
    }

    public boolean isVoided() {
        return isVoided;
    }

    public String getExternalId() {
        return externalId;
    }
}
