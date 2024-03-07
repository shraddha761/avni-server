package org.avni.server.importer.batch.model;

import org.avni.server.domain.OrganisationConfig;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BundleZip extends HashMap<String, byte[]> {
    public static final String STRING_FOLDER_PATH_SEPARATOR = "/";
    public static final char STRING_EXTENSION = '.';

    public BundleZip(Map<? extends String, ? extends byte[]> m) {
        super(m);
    }

    public byte[] getFile(String fileName) {
        String matchingKey = this.keySet().stream().filter(x -> x.endsWith(fileName)).findAny().orElse(null);
        return this.get(matchingKey);
    }

    public Map<String, String> getFileNameAndDataInFolder(String folder) {
        Map<String, String> map = new HashMap<>();
        //TODO refactor map put to be more readable
        this.entrySet().stream().filter(x -> x.getKey().contains(folder+STRING_FOLDER_PATH_SEPARATOR))
                .forEach(x -> map.put(x.getKey().substring(x.getKey().lastIndexOf(STRING_FOLDER_PATH_SEPARATOR)+1,
                                x.getKey().lastIndexOf(STRING_EXTENSION)),
                        new String(x.getValue(), StandardCharsets.UTF_8)));
        return map;
    }


    public List<String> getExtensionNames() {
        return this.keySet().stream().filter(bytes -> bytes.contains(String.format("%s/", OrganisationConfig.Extension.EXTENSION_DIR)))
                .map(key -> key.substring(key.indexOf(OrganisationConfig.Extension.EXTENSION_DIR))).collect(Collectors.toList());
    }
}
