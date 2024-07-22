package org.avni.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MetadataDiffService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataDiffService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> compareMetadataZips(MultipartFile zipFile1, MultipartFile zipFile2) throws IOException {

        Map<String, Object> result = new HashMap<>();
        File tempDir1 = null, tempDir2 = null;

        try {
            tempDir1 = extractZip(zipFile1);
            tempDir2 = extractZip(zipFile2);

            List<File> files1 = listJsonFiles(tempDir1);
            List<File> files2 = listJsonFiles(tempDir2);

            Set<String> fileNames1 = files1.stream().map(File::getName).collect(Collectors.toSet());
            Set<String> fileNames2 = files2.stream().map(File::getName).collect(Collectors.toSet());

            Set<String> commonFileNames = new HashSet<>(fileNames1);
            commonFileNames.retainAll(fileNames2);

            List<File> commonFiles1 = files1.stream()
                    .filter(file -> commonFileNames.contains(file.getName()))
                    .collect(Collectors.toList());

            List<File> commonFiles2 = files2.stream()
                    .filter(file -> commonFileNames.contains(file.getName()))
                    .collect(Collectors.toList());

            Map<String, Map<String, Object>> jsonMap1 = parseJsonFiles(commonFiles1);
            Map<String, Map<String, Object>> jsonMap2 = parseJsonFiles(commonFiles2);

            Map<String, Object> differences = findDifferences(jsonMap1, jsonMap2);

            result.put("differences", differences);
            result.put("Missing File Zip1", findMissingFiles(fileNames1, fileNames2));
            result.put("Missing File Zip2", findMissingFiles(fileNames2, fileNames1));

        } catch (IOException e) {
            e.printStackTrace();
            result.put("error", "Error comparing metadata ZIPs: " + e.getMessage());
        } finally {
            if (tempDir1 != null) {
                deleteDirectory(tempDir1);
            }
            if (tempDir2 != null) {
                deleteDirectory(tempDir2);
            }
        }
        return result;
    }

    private File extractZip(MultipartFile zipFile) throws IOException {
        File tempDir = Files.createTempDirectory("metadata-zip").toFile();

        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    File file = new File(tempDir, entry.getName());

                    File parentDir = file.getParentFile();
                    if (!parentDir.exists()) {
                        parentDir.mkdirs();
                    }

                    try (OutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipInputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found in ZIP: " + e.getMessage());
            throw e;
        }
        return tempDir;
    }

    private List<File> listJsonFiles(File directory) {
        List<File> jsonFiles = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".json")) {
                    jsonFiles.add(file);
                }
            }
        }
        return jsonFiles;
    }

    private Map<String, Map<String, Object>> parseJsonFiles(List<File> files) throws IOException {
        Map<String, Map<String, Object>> jsonMap = new HashMap<>();
        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }

                if (jsonContent.toString().trim().startsWith("[")) {
                    List<Map<String, Object>> jsonArray = objectMapper.readValue(jsonContent.toString(), new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> jsonObject : jsonArray) {
                        String uuid = (String) jsonObject.get("uuid");
                        if (uuid != null) {
                            jsonObject.put("filename", file.getName());
                            jsonMap.put(uuid, jsonObject);
                        }
                    }
                } else {
                    Map<String, Object> jsonObject = objectMapper.readValue(jsonContent.toString(), new TypeReference<Map<String, Object>>() {});
                    String uuid = (String) jsonObject.get("uuid");
                    if (uuid != null) {
                        jsonObject.put("filename", file.getName());
                        jsonMap.put(uuid, jsonObject);
                    }
                }
            }
        }
        return jsonMap;
    }

    protected Map<String, Object> findDifferences(Map<String, Map<String, Object>> map1, Map<String, Map<String, Object>> map2) {
        Map<String, Object> differences = new HashMap<>();

        Set<String> commonUuids = map1.keySet().stream()
                .filter(map2::containsKey)
                .collect(Collectors.toSet());

        for (String uuid : commonUuids) {
            Map<String, Object> obj1 = map1.get(uuid);
            Map<String, Object> obj2 = map2.get(uuid);
            Map<String, Object> diff = findDifferencesBetweenObjects(obj1, obj2);
            if (!diff.isEmpty()) {
                diff.put("filename", obj1.get("filename"));
                differences.put(uuid, diff);
            }
        }
        return differences;
    }

    private Map<String, Object> findDifferencesBetweenObjects(Map<String, Object> obj1, Map<String, Object> obj2) {
        Map<String, Object> differences = new HashMap<>();

        for (Map.Entry<String, Object> entry : obj1.entrySet()) {
            String key = entry.getKey();
            Object value1 = entry.getValue();
            Object value2 = obj2.get(key);

            if (!Objects.equals(value1, value2)) {
                differences.put(key, value2);
            }
        }
        return differences;
    }
    protected Set<String> findMissingFiles(Set<String> files1, Set<String> files2) {
        Set<String> missingFiles = new HashSet<>(files1);
        missingFiles.removeAll(files2);
        return missingFiles;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
