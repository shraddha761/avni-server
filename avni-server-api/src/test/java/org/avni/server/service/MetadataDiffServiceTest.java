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

    private final MetadataDiffService service = new MetadataDiffService();

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

        Set<String> missingInZip2 = service.findMissingFiles(filesInZip1, filesInZip2);
        Set<String> missingInZip1 = service.findMissingFiles(filesInZip2, filesInZip1);

        assertEquals(expectedMissingInZip2, missingInZip2);
        assertEquals(expectedMissingInZip1, missingInZip1);
    }

    @Test
    public void testFindDifferences() {
        Map<String, Map<String, Object>> map1 = new HashMap<>();
        Map<String, Map<String, Object>> map2 = new HashMap<>();

        // Example JSON objects
        Map<String, Object> obj1Map1 = new HashMap<>();
        obj1Map1.put("filename", "file1.txt");
        obj1Map1.put("field1", "value1");
        obj1Map1.put("field2", "value2");
        map1.put("uuid1", obj1Map1);

        Map<String, Object> obj2Map1 = new HashMap<>();
        obj2Map1.put("filename", "file2.txt");
        obj2Map1.put("field1", "value1");
        obj2Map1.put("field2", "valueDifferent");
        map1.put("uuid2", obj2Map1);

        Map<String, Object> obj1Map2 = new HashMap<>();
        obj1Map2.put("filename", "file2.txt");
        obj1Map2.put("field1", "value1");
        obj1Map2.put("field2", "value2");
        map2.put("uuid2", obj1Map2);

        Map<String, Object> obj2Map2 = new HashMap<>();
        obj2Map2.put("filename", "file3.txt");
        obj2Map2.put("field1", "value3");
        obj2Map2.put("field2", "value4");
        map2.put("uuid3", obj2Map2);

        Map<String, Object> expectedDifferences = new HashMap<>();
        Map<String, Object> diffForUuid2 = new HashMap<>();
        diffForUuid2.put("field2", "value2");
        diffForUuid2.put("filename", "file2.txt");
        expectedDifferences.put("uuid2", diffForUuid2);

        Map<String, Object> differences = service.findDifferences(map1, map2);
        assertEquals(expectedDifferences, differences);
    }
}