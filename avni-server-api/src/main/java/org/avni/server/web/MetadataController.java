package org.avni.server.web;

import org.avni.server.domain.accessControl.PrivilegeType;
import org.avni.server.service.MetadataDiffService;
import org.avni.server.service.accessControl.AccessControlService;
import org.avni.server.web.util.ErrorBodyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/metadata")
public class MetadataController {
    private final Logger logger;
    private final MetadataDiffService metadataService;
    private final AccessControlService accessControlService;
    private final ErrorBodyBuilder errorBodyBuilder;

    @Autowired
    public MetadataController(MetadataDiffService metadataService,
                              AccessControlService accessControlService,
                              ErrorBodyBuilder errorBodyBuilder) {
        this.metadataService = metadataService;
        this.accessControlService = accessControlService;
        this.errorBodyBuilder = errorBodyBuilder;
        logger = LoggerFactory.getLogger(getClass());
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyAuthority('user')")
    public ResponseEntity<?> compareMetadataZips(@RequestParam("file1") MultipartFile file1,
                                                 @RequestParam("file2") MultipartFile file2) {
        accessControlService.checkPrivilege(PrivilegeType.UploadMetadataAndData);

        if (file1 == null || file1.isEmpty() || file2 == null || file2.isEmpty()) {
            String message = "Both files must be provided and non-empty.";
            logger.error(message);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", message);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        Map<String, Object> result;
        try {
            result = metadataService.compareMetadataZips(file1, file2);
        } catch (IOException e) {
            logger.error(String.format("IOException occurred while comparing metadata files: '%s' and '%s'",
                    file1.getOriginalFilename(), file2.getOriginalFilename()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBodyBuilder.getErrorBody(e));
        } catch (Exception e) {
            logger.error(String.format("Unexpected error occurred while comparing metadata files: '%s' and '%s'",
                    file1.getOriginalFilename(), file2.getOriginalFilename()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBodyBuilder.getErrorBody(e));
        }

        return ResponseEntity.ok(result);
    }
}
