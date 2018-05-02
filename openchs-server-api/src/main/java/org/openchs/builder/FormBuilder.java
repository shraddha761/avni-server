package org.openchs.builder;

import org.openchs.application.Form;
import org.openchs.application.FormElementGroup;
import org.openchs.application.FormType;
import org.openchs.domain.Organisation;
import org.openchs.web.request.CHSRequest;
import org.openchs.web.request.application.FormElementGroupContract;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FormBuilder extends BaseBuilder<Form, FormBuilder> {
    public FormBuilder(Form existingForm) {
        super(existingForm, new Form());
    }

    public FormBuilder withType(String formType) {
        this.set(FormType.class.getSimpleName(), formType == null ? null : FormType.valueOf(formType), FormType.class);
        return this;
    }

    public FormBuilder withName(String name) {
        this.set("Name", name, String.class);
        return this;
    }

    private FormElementGroup getExistingFormElementGroup(String uuid) {
        return this.get().getFormElementGroups().stream()
                .filter((formElementGroup -> formElementGroup.getUuid().equals(uuid)))
                .findFirst()
                .orElse(null);
    }

    public FormBuilder withFormElementGroups(List<FormElementGroupContract> formElementGroupsContract) {
        Set<FormElementGroup> formElementGroups = formElementGroupsContract.stream()
                .map(formElementGroupContract -> new FormElementGroupBuilder(this.get(), getExistingFormElementGroup(formElementGroupContract.getUuid()))
                        .withName(formElementGroupContract.getName())
                        .withUUID(formElementGroupContract.getUuid())
                        .withDisplay(formElementGroupContract.getDisplay())
                        .withDisplayOrder(formElementGroupContract.getDisplayOrder())
                        .withFormElements(formElementGroupContract.getFormElements())
                        .build()).collect(Collectors.toSet());
        this.get().setFormElementGroups(formElementGroups);
        return this;
    }

    public FormBuilder addFormElementGroups(List<FormElementGroupContract> formElementGroupsContract) {
        Set<FormElementGroup> formElementGroups = formElementGroupsContract.stream()
                .map(formElementGroupContract -> new FormElementGroupBuilder(this.get(), getExistingFormElementGroup(formElementGroupContract.getUuid()))
                        .withName(formElementGroupContract.getName())
                        .withUUID(formElementGroupContract.getUuid())
                        .withDisplay(formElementGroupContract.getDisplay())
                        .withDisplayOrder(formElementGroupContract.getDisplayOrder())
                        .addFormElements(formElementGroupContract.getFormElements())
                        .build()).collect(Collectors.toSet());
        this.get().addFormElementGroups(formElementGroups);
        return this;
    }

    public FormBuilder withoutFormElements(Organisation organisation, List<FormElementGroupContract> formElementGroupContracts) {
        List<String> formElementGroupUUIDs = formElementGroupContracts.stream()
                .map(CHSRequest::getUuid).collect(Collectors.toList());
        Set<FormElementGroup> formElementGroups = formElementGroupContracts.stream().map(formElementGroupContract ->
                new FormElementGroupBuilder(this.get(), getExistingFormElementGroup(formElementGroupContract.getUuid()))
                        .withoutFormElements(organisation, formElementGroupContract.getFormElements())
                        .build()).collect(Collectors.toSet());
        Set<FormElementGroup> allFormElementGroups = this.get().getFormElementGroups().stream()
                .filter(feg -> !formElementGroupUUIDs.contains(feg.getUuid())).collect(Collectors.toSet());
        allFormElementGroups.addAll(formElementGroups);
        this.get().setFormElementGroups(allFormElementGroups);
        return this;
    }
}
