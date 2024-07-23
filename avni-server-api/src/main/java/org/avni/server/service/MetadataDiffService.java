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

            Set<String> filenames = new HashSet<>();
            filenames.addAll(jsonMap1.values().stream().map(file -> (String) file.get("filename")).collect(Collectors.toSet()));
            filenames.addAll(jsonMap2.values().stream().map(file -> (String) file.get("filename")).collect(Collectors.toSet()));

            for (String filename : filenames) {
                Map<String, Map<String, Object>> fileDifferences = findDifferences(
                        jsonMap1.entrySet().stream()
                                .filter(entry -> filename.equals(entry.getValue().get("filename")))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                        jsonMap2.entrySet().stream()
                                .filter(entry -> filename.equals(entry.getValue().get("filename")))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );

                if (!fileDifferences.isEmpty()) {
                    result.put(filename, fileDifferences);
                }
            }

            Set<String> set1 = findMissingFiles(fileNames1, fileNames2);
            if(!set1.isEmpty()) {
                result.put("Missing File Zip1", findMissingFiles(fileNames1, fileNames2));
            }
            Set<String> set2 = findMissingFiles(fileNames1, fileNames2);
            if(!set2.isEmpty()) {
                result.put("Missing File Zip1", findMissingFiles(fileNames2, fileNames1));
            }

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
                if (file.isDirectory()) {
                    jsonFiles.addAll(listJsonFiles(file)); // Recursively process subdirectories
                } else if (file.isFile() && file.getName().toLowerCase().endsWith(".json")) {
                    jsonFiles.add(file);
                }
            }
        }
        return jsonFiles;
    }

    private Map<String, Map<String, Object>> parseJsonFiles(List<File> files) throws IOException {
        Map<String, Map<String, Object>> jsonMap = new HashMap<>();

        for (File file : files) {
            String relativePath = file.getPath().substring(file.getParentFile().getParentFile().getPath().length() + 1);

            if (relativePath.startsWith("metadata-")) {
                int startIndex = relativePath.indexOf('/', "metadata-".length());
                     if (startIndex != -1) {
                         relativePath = relativePath.substring(startIndex + 1);
                     }
             }

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
                            jsonObject.put("filename", relativePath);
                            jsonMap.put(uuid, jsonObject);
                        }
                    }
                } else {
                    Map<String, Object> jsonObject = objectMapper.readValue(jsonContent.toString(), new TypeReference<Map<String, Object>>() {});
                    String uuid = (String) jsonObject.get("uuid");
                    if (uuid != null) {
                        jsonObject.put("filename", relativePath);
                        jsonMap.put(uuid, jsonObject);
                    }
                }
            }
        }
        return jsonMap;
    }


    protected Map<String, Map<String, Object>> findDifferences(Map<String, Map<String, Object>> map1, Map<String, Map<String, Object>> map2) {
        Map<String, Map<String, Object>> differences = new HashMap<>();

        Set<String> filenames = new HashSet<>();
        filenames.addAll(map1.values().stream().map(file -> (String) file.get("filename")).collect(Collectors.toSet()));
        filenames.addAll(map2.values().stream().map(file -> (String) file.get("filename")).collect(Collectors.toSet()));

        for (String filename : filenames) {
            Map<String, Object> fileDifferences = new HashMap<>();
            Map<String, Object> modified = new HashMap<>();
            List<Map<String, Object>> added = new ArrayList<>();
            List<Map<String, Object>> deleted = new ArrayList<>();

            Map<String, Map<String, Object>> tempMap1 = removeFilenameFromMap(map1, filename);
            Map<String, Map<String, Object>> tempMap2 = removeFilenameFromMap(map2, filename);

            List<String> uuidsInMap1 = new ArrayList<>(tempMap1.keySet());
            List<String> uuidsInMap2 = new ArrayList<>(tempMap2.keySet());

            Set<String> commonUuids = new HashSet<>(uuidsInMap1);
            commonUuids.retainAll(uuidsInMap2);

            for (String uuid : commonUuids) {
                Map<String, Object> obj1 = tempMap1.get(uuid);
                Map<String, Object> obj2 = tempMap2.get(uuid);
                Map<String, Object> diff = findDifferencesBetweenObjects(obj1, obj2);
                if (!diff.isEmpty()) {
                    modified.put(uuid, diff);
                }
            }

            for (String uuid : uuidsInMap2) {
                if (!uuidsInMap1.contains(uuid)) {
                    added.add(new HashMap<>(tempMap2.get(uuid)));
                }
            }

            for (String uuid : uuidsInMap1) {
                if (!uuidsInMap2.contains(uuid)) {
                    deleted.add(new HashMap<>(tempMap1.get(uuid)));
                }
            }

            if (!modified.isEmpty()) {
                fileDifferences.put("modified", modified);
            }
            if (!added.isEmpty()) {
                fileDifferences.put("added", added);
            }
            if (!deleted.isEmpty()) {
                fileDifferences.put("deleted", deleted);
            }

            if (!fileDifferences.isEmpty()) {
                differences.put("Differences", fileDifferences);
            }
        }
        return differences;
    }

    private Map<String, Map<String, Object>> removeFilenameFromMap(Map<String, Map<String, Object>> map, String filename) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            Map<String, Object> value = new HashMap<>(entry.getValue());
            if (filename.equals(value.get("filename"))) {
                value.remove("filename");
            }
            result.put(entry.getKey(), value);
        }
        return result;
    }

    private Map<String, Object> findDifferencesBetweenObjects(Map<String, Object> obj1, Map<String, Object> obj2) {
        Map<String, Object> differences = new HashMap<>();

        for (Map.Entry<String, Object> entry : obj1.entrySet()) {
            String key = entry.getKey();
            Object value1 = entry.getValue();
            Object value2 = obj2.get(key);

            if (!Objects.equals(value1, value2) && key != "id") {
                Map<String, Object> valueDetails = new HashMap<>();
                valueDetails.put("old_value", value1);
                valueDetails.put("new_value", value2);
                differences.put(key, valueDetails);
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
