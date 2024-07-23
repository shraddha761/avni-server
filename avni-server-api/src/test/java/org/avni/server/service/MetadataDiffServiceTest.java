package org.avni.server.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

public class MetadataDiffServiceTest {

    private MetadataDiffService metadataDiffService;

    @Before
    public void setUp() {
        metadataDiffService = new MetadataDiffService();
    }

    @Test
    public void testCompareMetadataZips() throws IOException {

        MockMultipartFile zipFile1 = createMockZipFile("zipFile1.zip", "file1.json", "{\"uuid\":\"12345\", \"key1\":\"value1\"}");
        MockMultipartFile zipFile2 = createMockZipFile("zipFile2.zip", "file1.json", "{\"uuid\":\"12345\", \"key1\":\"value2\"}");

        Map<String, Object> result = metadataDiffService.compareMetadataZips(zipFile1, zipFile2);

        System.out.println("Result: " + result);

        assertTrue(result.containsKey("differences"));
        Map<?, ?> differences = (Map<?, ?>) result.get("differences");
        assertTrue("Expected at least one difference", !differences.isEmpty());

        assertEquals(1, differences.size());
        Map<?, ?> diff = (Map<?, ?>) differences.get("12345");
        assertEquals("value2", diff.get("key1"));
    }

    private MockMultipartFile createMockZipFile(String zipFileName, String jsonFileName, String jsonContent) throws IOException {
        File tempZipFile = Files.createTempFile("tempZipFile", ".zip").toFile();
        tempZipFile.deleteOnExit();

        try (java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(Files.newOutputStream(tempZipFile.toPath()))) {
            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(jsonFileName);
            zipOut.putNextEntry(zipEntry);
            zipOut.write(jsonContent.getBytes());
            zipOut.closeEntry();
        }

        byte[] zipContent = Files.readAllBytes(tempZipFile.toPath());
        return new MockMultipartFile("file", zipFileName, "application/zip", zipContent);
    }


    @Test
    public void testFindMissingFiles() {
        Set<String> filesInZip1 = new HashSet<>();
        Set<String> filesInZip2 = new HashSet<>();

        filesInZip1.add("file1.txt");
        filesInZip1.add("file2.txt");
        filesInZip1.add("file3.txt");

        filesInZip2.add("file2.txt");
        filesInZip2.add("file3.txt");
        filesInZip2.add("file4.txt");

        Set<String> expectedMissingInZip2 = new HashSet<>();
        expectedMissingInZip2.add("file1.txt");

        Set<String> expectedMissingInZip1 = new HashSet<>();
        expectedMissingInZip1.add("file4.txt");

        Set<String> missingInZip2 = metadataDiffService.findMissingFiles(filesInZip1, filesInZip2);
        Set<String> missingInZip1 = metadataDiffService.findMissingFiles(filesInZip2, filesInZip1);

        assertEquals(expectedMissingInZip2, missingInZip2);
        assertEquals(expectedMissingInZip1, missingInZip1);
    }


    @Test
    public void testFindDifferences() {
        Map<String, Object> obj1 = new HashMap<>();
        obj1.put("uuid", "123");
        obj1.put("name", "test1");
        obj1.put("filename", "specificFile.json");

        Map<String, Object> obj2 = new HashMap<>();
        obj2.put("uuid", "123");
        obj2.put("name", "test2");
        obj2.put("filename", "specificFile.json");

        Map<String, Map<String, Object>> map1 = new HashMap<>();
        map1.put("123", obj1);

        Map<String, Map<String, Object>> map2 = new HashMap<>();
        map2.put("123", obj2);

        Map<String, Map<String, Object>> differences = metadataDiffService.findDifferences(map1, map2);

        Map<String, Object> ruleDependencyDiff = new HashMap<>();
        ruleDependencyDiff.put("old_value", "test1");
        ruleDependencyDiff.put("new_value", "test2");

        Map<String, Object> modifiedDiff = new HashMap<>();
        modifiedDiff.put("name", ruleDependencyDiff);

        Map<String, Map<String, Object>> modifiedEntry = new HashMap<>();
        modifiedEntry.put("123", modifiedDiff);

        Map<String, Object> differencesEntry = new HashMap<>();
        differencesEntry.put("modified", modifiedEntry);

        Map<String, Object> expectedDifferences = new HashMap<>();
        expectedDifferences.put("Differences", differencesEntry);

        assertEquals(expectedDifferences, differences);
    }

    @Test
    public void testFindDifferencesWithAddedAndDeletedEntries() {
        Map<String, Object> obj1 = new HashMap<>();
        obj1.put("uuid", "123");
        obj1.put("name", "test1");
        obj1.put("filename", "specificFile.json");

        Map<String, Object> obj2 = new HashMap<>();
        obj2.put("uuid", "461");
        obj2.put("name", "test2");
        obj2.put("filename", "specificFile.json");

        Map<String, Map<String, Object>> map1 = new HashMap<>();
        map1.put("123", obj1);

        Map<String, Map<String, Object>> map2 = new HashMap<>();
        map2.put("461", obj2);

        Map<String, Object> deletedEntry = new HashMap<>();
        deletedEntry.put("name", "test1");
        deletedEntry.put("uuid", "123");

        Map<String, Object> addedEntry = new HashMap<>();
        addedEntry.put("name", "test2");
        addedEntry.put("uuid", "461");

        Map<String, Map<String, Object>> differencesObtain = metadataDiffService.findDifferences(map1, map2);

        Map<String, Object> differences = new HashMap<>();
        differences.put("deleted", Collections.singletonList(deletedEntry));
        differences.put("added", Collections.singletonList(addedEntry));

        Map<String, Object> expectedDifferences = new HashMap<>();
        expectedDifferences.put("Differences", differences);

        assertEquals(differencesObtain, expectedDifferences);
    }
}
