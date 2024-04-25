package com.github.mgcvale.springstudy.security;

import com.github.mgcvale.springstudy.database.DatabaseException;
import com.github.mgcvale.springstudy.database.DatabaseManager;
import com.github.mgcvale.springstudy.database.StaticDatabaseInstance;
import io.jsonwebtoken.Jwts;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;


@RestController
public class SecurityManager {
    private static final DatabaseManager dbm = StaticDatabaseInstance.getDatabaseManager();
    private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe



    @PostMapping("/users/add")
    public ResponseEntity<String> addUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("group") String group
    ) {
        try {
            // check if user already exists
            if (dbm.tableContains("users", "username", username))
                return ResponseEntity.status(HttpStatus.CONFLICT).body("USER_ALREADY_EXISTS_ERROR");

            //create user's directory, access token and database entry
            Runtime.getRuntime().exec("mkdir /usr/server/" + username);
            String accessToken = generateAccessToken();
            dbm.insertInto("users",
                    new String[]{"username", "password_hash", "user_group", "access_token"},
                    new Object[]{username, password, group, accessToken});
            return ResponseEntity.ok(accessToken);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DATABASE_ERROR");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User's directory already exists!");
        }
    }

    @PostMapping("/users/remove")
    public ResponseEntity<String> removeUser(
            @RequestParam("logintoken") String loginToken,
            @RequestParam("deletedir") boolean deleteDirectories,
            @RequestParam("user") String user) {
        try {
            String username = dbm.getEntryAt("users", "access_token", loginToken, "username", String.class);
            if(username == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_FOUND_ERROR");
            }
            String group = dbm.getEntryAt("users", "access_token", loginToken, "user_group", String.class);
            if(!username.equals(user))
                if(!"ADMIN".equals(group))
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("FORBIDDEN_ERROR");
                else
                    System.out.println("deletando pq e admin");
            else
                System.out.println("deletando pq e o mesmo usuario");

            dbm.deleteFromTable("users", "username=?", user);
            if(deleteDirectories)
                Runtime.getRuntime().exec("rm -rf /usr/server" + user);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("DATABASE_ERROR");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DIR_DELETE_ERROR");
        }
        return ResponseEntity.ok("USER_DELETED_SUCCESS");
    }

    @GetMapping("/users/getToken")
    public ResponseEntity<String> getToken(
            @RequestParam("user") String user,
            @RequestParam("password") String password
            ) {
        try {
            String correct_password = dbm.getEntryAt("users", "username", user,
                    "password_hash", String.class);
            if(correct_password == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("USER_NOT_FOUND_ERROR");
            if (password.equals(correct_password))
                return ResponseEntity.ok(dbm.getEntryAt("users", "username", user,
                        "access_token", String.class));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_AUTHENTICATION_FAILED_ERROR");
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("DATABASE_ERROR");
        }
    }

    private String generateAccessToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

}
