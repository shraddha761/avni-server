package org.avni.server.importer.batch.csv.writer;

import org.avni.server.common.AbstractControllerIntegrationTest;
import org.avni.server.domain.AddressLevelType;
import org.avni.server.domain.factory.AddressLevelTypeBuilder;
import org.avni.server.service.builder.TestDataSetupService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Arrays;
import java.util.UUID;

@Sql(value = {"/tear-down.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = {"/tear-down.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class BulkLocationEditorIntegrationTest extends AbstractControllerIntegrationTest {
    @Autowired
    private TestDataSetupService testDataSetupService;

    @Test
    @Ignore
    public void shouldEdit() {
        TestDataSetupService.TestOrganisationData organisationData = testDataSetupService.setupOrganisation();
        AddressLevelType block = new AddressLevelTypeBuilder().name("Block").level(2d).withUuid(UUID.randomUUID()).build();
        AddressLevelType district = new AddressLevelTypeBuilder().name("District").level(3d).withUuid(UUID.randomUUID()).build();
        AddressLevelType state = new AddressLevelTypeBuilder().name("State").level(4d).withUuid(UUID.randomUUID()).build();
        testDataSetupService.saveLocationTypes(Arrays.asList(block, district, state));

        setUser(organisationData.getUser().getUsername());
    }
}