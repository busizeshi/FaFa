package com.fafa.biz.testing;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.assertj.core.api.Assertions.*;

/** 独立原生 MySQL 库验证全量迁移、校验及同版本重启。 */
@EnabledIfEnvironmentVariable(named="FAFA_WP04_MIGRATION_URL",matches=".+")
class MediaMigrationTest {
    @Test void freshDatabaseAndRestartRespectMigrationHistory() {
        var flyway=Flyway.configure().dataSource(System.getenv("FAFA_WP04_MIGRATION_URL"),
            System.getenv("SPRING_DATASOURCE_USERNAME"),System.getenv("SPRING_DATASOURCE_PASSWORD"))
            .locations("classpath:db/migration").load();
        flyway.migrate();flyway.validate();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("007");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
