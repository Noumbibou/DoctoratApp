package org.example.doctoratapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Copie les données H2 (fichier ./data/doctoratdb) vers MySQL.
 * Activer : --spring.profiles.active=dev,migrate-h2
 */
@Component
@Order(0)
@ConditionalOnProperty(name = "app.migration.h2-to-mysql.enabled", havingValue = "true")
public class H2ToMySqlMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(H2ToMySqlMigrationRunner.class);

    private static final List<String> TABLE_ORDER = List.of(
            "users",
            "doctorant",
            "directeur_these",
            "campagne_inscription",
            "dossier_inscription",
            "document",
            "publications",
            "formation_doctorale",
            "demande_soutenance",
            "membre_jury",
            "soutenance",
            "notification",
            "derogation",
            "audit_entry"
    );

    private final DataSource mysqlDataSource;

    @Value("${app.migration.h2.file:./data/doctoratdb}")
    private String h2FileBase;

    public H2ToMySqlMigrationRunner(DataSource mysqlDataSource) {
        this.mysqlDataSource = mysqlDataSource;
    }

    @Override
    public void run(String... args) {
        Path h2File = Path.of(h2FileBase + ".mv.db");
        if (!Files.exists(h2File)) {
            log.warn("Fichier H2 introuvable : {}", h2File.toAbsolutePath());
            log.warn("Aucune copie effectuée. MySQL sera alimentée par Hibernate + DemoDataInitializer.");
            return;
        }

        log.info("Migration H2 → MySQL depuis {}", h2File.toAbsolutePath());

        JdbcTemplate h2 = new JdbcTemplate(h2DataSource());
        JdbcTemplate mysql = new JdbcTemplate(mysqlDataSource);

        List<String> h2Tables = h2.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                        "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'",
                String.class
        );

        Set<String> ordered = new LinkedHashSet<>();
        for (String preferred : TABLE_ORDER) {
            h2Tables.stream()
                    .filter(t -> t.equalsIgnoreCase(preferred))
                    .findFirst()
                    .ifPresent(ordered::add);
        }
        h2Tables.stream()
                .filter(t -> TABLE_ORDER.stream().noneMatch(p -> p.equalsIgnoreCase(t)))
                .forEach(ordered::add);

        mysql.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : ordered) {
            migrateTable(h2, mysql, table);
        }
        mysql.execute("SET FOREIGN_KEY_CHECKS = 1");

        log.info("Migration H2 → MySQL terminée ({} table(s)).", ordered.size());
    }

    private DataSource h2DataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.h2.Driver");
        ds.setUrl("jdbc:h2:file:" + h2FileBase + ";AUTO_SERVER=TRUE");
        ds.setUsername("sa");
        ds.setPassword("");
        return ds;
    }

    private void migrateTable(JdbcTemplate h2, JdbcTemplate mysql, String table) {
        Integer count = h2.queryForObject("SELECT COUNT(*) FROM \"" + table + "\"", Integer.class);
        if (count == null || count == 0) {
            log.info("Table {} : vide, ignorée.", table);
            return;
        }

        List<String> columns = h2.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                String.class,
                table
        );

        String columnList = String.join(", ", columns);
        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());

        mysql.execute("DELETE FROM `" + table + "`");

        List<Object[]> rows = h2.query(
                "SELECT " + columnList + " FROM \"" + table + "\"",
                (rs, rowNum) -> {
                    Object[] row = new Object[columns.size()];
                    for (int i = 0; i < columns.size(); i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    return row;
                }
        );

        for (Object[] row : rows) {
            mysql.update(
                    "INSERT INTO `" + table + "` (" + columnList + ") VALUES (" + placeholders + ")",
                    row
            );
        }

        log.info("Table {} : {} ligne(s) copiée(s).", table, count);
    }
}
