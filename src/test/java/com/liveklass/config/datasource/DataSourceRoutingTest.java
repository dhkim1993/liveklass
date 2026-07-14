package com.liveklass.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
	"app.datasource.master.url=jdbc:h2:mem:routing_master;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"app.datasource.master.username=sa",
	"app.datasource.master.password=",
	"app.datasource.master.driver-class-name=org.h2.Driver",
	"app.datasource.slave.url=jdbc:h2:mem:routing_slave;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"app.datasource.slave.username=sa",
	"app.datasource.slave.password=",
	"app.datasource.slave.driver-class-name=org.h2.Driver",
	"spring.jpa.hibernate.ddl-auto=none"
})
class DataSourceRoutingTest {

	private final JdbcTemplate masterJdbcTemplate;
	private final JdbcTemplate slaveJdbcTemplate;
	private final RoutingProbeService routingProbeService;

	@Autowired
	DataSourceRoutingTest(
		@Qualifier("masterDataSource") DataSource masterDataSource,
		@Qualifier("slaveDataSource") DataSource slaveDataSource,
		RoutingProbeService routingProbeService
	) {
		this.masterJdbcTemplate = new JdbcTemplate(masterDataSource);
		this.slaveJdbcTemplate = new JdbcTemplate(slaveDataSource);
		this.routingProbeService = routingProbeService;
	}

	@BeforeEach
	void setUp() {
		initializeMarker(masterJdbcTemplate, "MASTER");
		initializeMarker(slaveJdbcTemplate, "SLAVE");
	}

	@Test
	void readOnlyTransactionUsesSlaveDataSource() {
		String marker = routingProbeService.findMarkerInReadOnlyTransaction();

		assertThat(marker).isEqualTo("SLAVE");
	}

	@Test
	void writeTransactionUsesMasterDataSource() {
		String marker = routingProbeService.findMarkerInWriteTransaction();

		assertThat(marker).isEqualTo("MASTER");
	}

	private void initializeMarker(JdbcTemplate jdbcTemplate, String marker) {
		jdbcTemplate.execute("drop table if exists routing_marker");
		jdbcTemplate.execute("create table routing_marker (name varchar(20) not null)");
		jdbcTemplate.update("insert into routing_marker (name) values (?)", marker);
	}

	@TestConfiguration
	static class RoutingProbeConfig {

		@Bean
		RoutingProbeService routingProbeService(DataSource dataSource) {
			return new RoutingProbeService(dataSource);
		}
	}

	static class RoutingProbeService {

		private final JdbcTemplate jdbcTemplate;

		RoutingProbeService(DataSource dataSource) {
			this.jdbcTemplate = new JdbcTemplate(dataSource);
		}

		@Transactional(readOnly = true)
		public String findMarkerInReadOnlyTransaction() {
			return findMarker();
		}

		@Transactional
		public String findMarkerInWriteTransaction() {
			return findMarker();
		}

		private String findMarker() {
			return jdbcTemplate.queryForObject("select name from routing_marker", String.class);
		}
	}
}
