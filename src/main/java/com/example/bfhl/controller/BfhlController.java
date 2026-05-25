package com.example.bfhl.controller;

import com.example.bfhl.dto.BfhlRequest;
import com.example.bfhl.dto.BfhlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bfhl")
@CrossOrigin(origins = "*")
public class BfhlController {

    @Value("${user.fullname}")
    private String fullName;

    @Value("${user.dob}")
    private String dob;

    @Value("${user.email}")
    private String email;

    @Value("${user.rollnumber}")
    private String rollNumber;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getOperationCode() {
        return ResponseEntity.ok(Map.of("operation_code", 1));
    }

    @PostMapping
    public ResponseEntity<BfhlResponse> processData(@RequestBody BfhlRequest request) {
        BfhlResponse response = new BfhlResponse();
        try {
            // Build user_id: fullname_dob (e.g. aditya_yadav_23022005)
            String userId = fullName.toLowerCase().trim().replace(" ", "_") + "_" + dob.trim();
            response.setUser_id(userId);
            response.setEmail(email.trim());
            response.setRoll_number(rollNumber.trim());

            List<String> inputData = request.getData();
            if (inputData == null) {
                inputData = List.of();
            }

            List<String> numbers = new java.util.ArrayList<>();
            List<String> alphabets = new java.util.ArrayList<>();
            String highestLowercase = null;
            boolean isPrimeFound = false;

            for (String s : inputData) {
                if (s == null) continue;
                String trimmed = s.trim();
                if (trimmed.matches("^-?\\d+$")) {
                    numbers.add(trimmed);
                    try {
                        int num = Integer.parseInt(trimmed);
                        if (isPrime(num)) {
                            isPrimeFound = true;
                        }
                    } catch (NumberFormatException e) {
                        // ignore overflow or parse issues, not a simple prime
                    }
                } else if (trimmed.length() == 1 && Character.isLetter(trimmed.charAt(0))) {
                    alphabets.add(trimmed);
                    char ch = trimmed.charAt(0);
                    if (Character.isLowerCase(ch)) {
                        if (highestLowercase == null || ch > highestLowercase.charAt(0)) {
                            highestLowercase = String.valueOf(ch);
                        }
                    }
                }
            }

            response.setNumbers(numbers);
            response.setAlphabets(alphabets);

            List<String> highestLowercaseList = new java.util.ArrayList<>();
            if (highestLowercase != null) {
                highestLowercaseList.add(highestLowercase);
            }
            response.setHighest_lowercase_alphabet(highestLowercaseList);
            response.setIs_prime_found(isPrimeFound);

            // File parsing logic
            boolean fileValid = false;
            String mimeType = null;
            String fileSizeKb = null;
            String fileB64 = request.getFile_b64();

            if (fileB64 != null && !fileB64.trim().isEmpty()) {
                try {
                    String base64Data = fileB64.trim();
                    String detectedMimeType = null;

                    if (base64Data.startsWith("data:")) {
                        int commaIndex = base64Data.indexOf(",");
                        if (commaIndex != -1) {
                            String header = base64Data.substring(0, commaIndex);
                            if (header.contains(";base64")) {
                                detectedMimeType = header.substring(5, header.indexOf(";base64"));
                            }
                            base64Data = base64Data.substring(commaIndex + 1);
                        }
                    }

                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Data);
                    if (decodedBytes.length > 0) {
                        fileValid = true;
                        double sizeKb = decodedBytes.length / 1024.0;
                        fileSizeKb = String.valueOf((long) sizeKb);

                        if (detectedMimeType == null) {
                            detectedMimeType = detectMimeTypeFromBytes(decodedBytes);
                        }
                        mimeType = detectedMimeType != null ? detectedMimeType : "application/octet-stream";
                    }
                } catch (Exception e) {
                    fileValid = false;
                }
            }

            response.setFile_valid(fileValid);
            response.setFile_mime_type(mimeType);
            response.setFile_size_kb(fileSizeKb);
            response.setIs_success(true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setIs_success(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "is_success", false,
                "error", "Invalid or malformed request payload"
        ));
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n == 2 || n == 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }

    private String detectMimeTypeFromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        // PNG: 89 50 4E 47
        if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47) {
            return "image/png";
        }
        // JPEG: FF D8 FF
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        // PDF: 25 50 44 46 (%PDF)
        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
            return "application/pdf";
        }
        // GIF: 47 49 46 38 (GIF8)
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) {
            return "image/gif";
        }
        // ZIP: 50 4B 03 04 (PK..)
        if (bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04) {
            return "application/zip";
        }
        return null;
    }
}
