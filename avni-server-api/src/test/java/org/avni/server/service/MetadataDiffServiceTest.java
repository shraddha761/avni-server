package org.avni.server.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetadataDiffServiceTest {

    private MetadataDiffService metadataDiffService;

    @Before
    public void setUp() {
        metadataDiffService = new MetadataDiffService();
    }

    @Test
    public void testCompareMetadataZips() throws IOException {

        MockMultipartFile zipFile1 = createMockZipFile("zipFile1.zip", "file1.json", "{\"key1\":\"value1\"}");
        MockMultipartFile zipFile2 = createMockZipFile("zipFile2.zip", "file1.json", "{\"key1\":\"value2\"}");

        Map<String, Object> result = metadataDiffService.compareMetadataZips(zipFile1, zipFile2);

        assertTrue(result.containsKey("differences"));
        assertEquals(1, ((Map<?, ?>) result.get("differences")).size());
        assertTrue(result.containsKey("missingInZip1"));
        assertTrue(result.containsKey("missingInZip2"));
    }

    private MockMultipartFile createMockZipFile(String zipFileName, String jsonFileName, String jsonContent) throws IOException {
        File tempZipFile = Files.createTempFile("tempZipFile", ".zip").toFile();

        try (java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(Files.newOutputStream(tempZipFile.toPath()))) {
            java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(jsonFileName);
            zipOut.putNextEntry(zipEntry);
            zipOut.write(jsonContent.getBytes());
            zipOut.closeEntry();
        }

        byte[] zipContent = Files.readAllBytes(tempZipFile.toPath());
        MockMultipartFile mockMultipartFile = new MockMultipartFile("file", zipFileName, "application/zip", zipContent);

        FileSystemUtils.deleteRecursively(tempZipFile);
        return mockMultipartFile;
    }
}
