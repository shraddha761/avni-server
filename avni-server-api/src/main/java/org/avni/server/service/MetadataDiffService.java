package org.avni.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MetadataDiffService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataDiffService.class);

    public Map<String, Object> compareMetadataZips(MultipartFile zipFile1, MultipartFile zipFile2) throws IOException {

        Map<String, Object> result = new HashMap<>();
        File tempDir1 = null, tempDir2 = null;

        try {
            // Extract files from ZIPs
            tempDir1 = extractZip(zipFile1);
            tempDir2 = extractZip(zipFile2);

            // List files from extracted directories
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

            result.put("differences", findDifferences(commonFiles1, commonFiles2));
            result.put("missingInZip1", findMissingFiles(fileNames1, fileNames2));
            result.put("missingInZip2", findMissingFiles(fileNames2, fileNames1));
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
        System.out.println("Created temporary directory: " + tempDir.getAbsolutePath());

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
                        System.out.println("Extracted: " + file.getAbsolutePath());
                    }
                }
                zipInputStream.closeEntry();
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found in ZIP: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.err.println("IO Error during ZIP extraction: " + e.getMessage());
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

    private Map<String, Object> findDifferences(List<File> files1, List<File> files2) throws IOException {
        Map<String, Object> differences = new HashMap<>();
        for (File file1 : files1) {
            for (File file2 : files2) {
                if (file1.getName().equals(file2.getName())) {
                    Map<String, Object> diff = compareJsonFiles(file1, file2);
                    if (!diff.isEmpty()) {
                        differences.put(file1.getName(), diff);
                    }
                    break;
                }
            }
        }
        return differences;
    }

    private Map<String, Object> compareJsonFiles(File file1, File file2) throws IOException {
        try (BufferedReader reader1 = new BufferedReader(new FileReader(file1));
             BufferedReader reader2 = new BufferedReader(new FileReader(file2))) {
            String line1, line2;
            while ((line1 = reader1.readLine()) != null && (line2 = reader2.readLine()) != null) {
                if (!line1.equals(line2)) {
                    Map<String, Object> diff = new HashMap<>();
                    diff.put("difference", "Files are not identical");
                    return diff;
                }
            }
        }
        return Collections.emptyMap();
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
