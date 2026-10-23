
package com.studysync;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseController {

    private final DataSource dataSource;

    public DatabaseController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping({"/api/db-check", "/api/db-test"})
    public Map<String, String> checkDatabase() throws Exception {
        try (
            Connection connection = dataSource.getConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT 1")
        ) {
            result.next();

            return Map.of(
                "application", "StudySync",
                "database", result.getInt(1) == 1 ? "CONNECTED" : "FAILED"
            );
        }
    }
}
