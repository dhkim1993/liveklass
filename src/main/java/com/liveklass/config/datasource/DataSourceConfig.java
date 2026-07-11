package com.liveklass.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

@Configuration
public class DataSourceConfig {

	@Bean
	@ConfigurationProperties("app.datasource.master")
	public DataSourceProperties masterDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	public DataSource masterDataSource(
		@Qualifier("masterDataSourceProperties") DataSourceProperties properties
	) {
		return properties.initializeDataSourceBuilder()
			.type(HikariDataSource.class)
			.build();
	}

	@Bean
	@ConfigurationProperties("app.datasource.slave")
	public DataSourceProperties slaveDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	public DataSource slaveDataSource(
		@Qualifier("slaveDataSourceProperties") DataSourceProperties properties
	) {
		return properties.initializeDataSourceBuilder()
			.type(HikariDataSource.class)
			.build();
	}

	@Bean
	public DataSource routingDataSource(
		@Qualifier("masterDataSource") DataSource masterDataSource,
		@Qualifier("slaveDataSource") DataSource slaveDataSource
	) {
		RoutingDataSource routingDataSource = new RoutingDataSource();
		Map<Object, Object> dataSources = new HashMap<>();
		dataSources.put(DataSourceType.MASTER, masterDataSource);
		dataSources.put(DataSourceType.SLAVE, slaveDataSource);
		routingDataSource.setTargetDataSources(dataSources);
		routingDataSource.setDefaultTargetDataSource(masterDataSource);
		return routingDataSource;
	}

	@Primary
	@Bean
	public DataSource dataSource(
		@Qualifier("routingDataSource") DataSource routingDataSource
	) {
		return new LazyConnectionDataSourceProxy(routingDataSource);
	}
}
