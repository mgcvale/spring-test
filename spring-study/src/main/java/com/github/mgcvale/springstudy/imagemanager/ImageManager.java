package com.github.mgcvale.springstudy.imagemanager;

import com.github.mgcvale.springstudy.database.DatabaseManager;
import com.github.mgcvale.springstudy.database.StaticDatabaseInstance;
import org.apache.coyote.Response;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

@RestController
public class ImageManager {

    private final DatabaseManager dbm = StaticDatabaseInstance.getDatabaseManager();
    private static String uploadDir = "/usr/server";

    @PostMapping("/upload")
    public ResponseEntity<String> upload(
            @RequestParam("img") MultipartFile imgFile,
            @RequestParam("loginToken") String token) {
        try {
            String user = dbm.getEntryAt("users", "access_token", token,
                    "username", String.class);
            if (user == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_FOUND_ERROR");

            uploadFile(imgFile, user);
            return ResponseEntity.ok("UPLOAD_SUCCESS");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("INTERNAL_UPLOAD_ERROR");
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DATABASE_ERROR");
        }
    }

    private void uploadFile(MultipartFile imgFile, String user) throws IOException {
        String filename = imgFile.getOriginalFilename();
        Path filePath = Paths.get(uploadDir + "/" + user, filename);
        Files.write(filePath, imgFile.getBytes());
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @RequestParam("imgName") String filename,
            @RequestParam("loginToken") String token) {
        try {
            String user = dbm.getEntryAt("users", "access_token", token,
                    "username", String.class);
            if (user == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);

            Path filePath = Paths.get(uploadDir + "/" + user).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if(!resource.exists()) {
                throw new Exception();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            return ResponseEntity.ok().headers(headers).body(resource);

        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
