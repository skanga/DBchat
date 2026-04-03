package com.skanga.mcp.demo;

import com.skanga.mcp.db.DatabaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataGeneratorTest {
    @Mock
    private DatabaseService databaseService;

    @Test
    void cleanupScenarioReturnsFalseWhenConnectionIsNull() throws Exception {
        when(databaseService.getConnection()).thenReturn(null);

        DemoDataGenerator generator = new DemoDataGenerator(databaseService, "h2");

        assertThat(generator.cleanupDemoScenario("retail")).isFalse();
    }

    @Test
    void scenarioActiveReturnsFalseWhenConnectionIsNull() throws Exception {
        when(databaseService.getConnection()).thenReturn(null);

        DemoDataGenerator generator = new DemoDataGenerator(databaseService, "h2");

        assertThat(generator.isDemoScenarioActive("retail")).isFalse();
    }

    @Test
    void getScenarioInfoReturnsNullForNullOrBlankScenarioName() {
        assertThat(DemoDataGenerator.getScenarioInfo(null)).isNull();
        assertThat(DemoDataGenerator.getScenarioInfo("")).isNull();
        assertThat(DemoDataGenerator.getScenarioInfo("   ")).isNull();
    }

    @Test
    void setupScenarioReturnsFalseForNullOrBlankScenarioName() {
        DemoDataGenerator generator = new DemoDataGenerator(databaseService, "h2");

        assertThat(generator.setupDemoScenario(null)).isFalse();
        assertThat(generator.setupDemoScenario("")).isFalse();
        assertThat(generator.setupDemoScenario("   ")).isFalse();
    }

    @Test
    void cleanupScenarioReturnsFalseForNullOrBlankScenarioName() {
        DemoDataGenerator generator = new DemoDataGenerator(databaseService, "h2");

        assertThat(generator.cleanupDemoScenario(null)).isFalse();
        assertThat(generator.cleanupDemoScenario("")).isFalse();
        assertThat(generator.cleanupDemoScenario("   ")).isFalse();
    }

    @Test
    void scenarioActiveReturnsFalseForNullOrBlankScenarioName() {
        DemoDataGenerator generator = new DemoDataGenerator(databaseService, "h2");

        assertThat(generator.isDemoScenarioActive(null)).isFalse();
        assertThat(generator.isDemoScenarioActive("")).isFalse();
        assertThat(generator.isDemoScenarioActive("   ")).isFalse();
    }
}
