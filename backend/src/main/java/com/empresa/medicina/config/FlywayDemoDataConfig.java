package com.empresa.medicina.config;

import java.util.Arrays;
import java.util.stream.Stream;

import org.flywaydb.core.api.Location;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Añade los datos de demostración ({@code classpath:db/demo}) a las migraciones cuando
 * {@code app.demo-data=true}. Así los datos demo quedan identificados y separados del esquema.
 */
@Configuration(proxyBeanMethods = false)
class FlywayDemoDataConfig {

	static final String DEMO_LOCATION = "classpath:db/demo";

	@Bean
	FlywayConfigurationCustomizer demoDataLocation(AppProperties properties) {
		return configuration -> {
			if (!properties.demoData()) {
				return;
			}
			String[] locations = Stream
				.concat(Arrays.stream(configuration.getLocations()).map(Location::getDescriptor), Stream.of(DEMO_LOCATION))
				.distinct()
				.toArray(String[]::new);
			configuration.locations(locations);
		};
	}

}
