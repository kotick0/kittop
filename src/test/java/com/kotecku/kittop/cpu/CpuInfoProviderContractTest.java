package com.kotecku.kittop.cpu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

abstract class CpuInfoProviderContractTest {

    protected static final int LOGICAL_CORES = Runtime.getRuntime().availableProcessors();

    protected final double MIN_TEMPERATURE = 0.0;
    protected final double MAX_TEMPERATURE = 150.0;

    protected final double MIN_LOAD = 0.0;
    protected final double MAX_LOAD = 100.0;

    protected abstract CpuInfoProvider provider();

    @Test
    void getCpuLoadPerCoreShouldNotBeNull() {
        assertThat(provider().getCpuLoadPerCore()).isNotNull();
    }

    @Test
    void getCpuLoadPerCoreShouldHaveSizeEqualToLogicalCores() {
        assertThat(provider().getCpuLoadPerCore()).hasSize(LOGICAL_CORES);
    }

    @Test
    void getCpuLoadPerCoreShouldReturnValuesBetween0And100() {
        double[] loadPerCore = provider().getCpuLoadPerCore();
        for (double load : loadPerCore) {
            assertThat(load).isNotNaN().isBetween(MIN_LOAD, MAX_LOAD);
        }
    }

    @Test
    void getCpuTemperaturePerCoreShouldNotBeNull() {
        assertThat(provider().getCpuTemperaturePerCore()).isNotNull();
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = "true")
    void getCpuTemperaturePerCoreShouldHaveSizeEqualToLogicalCores() {
        assertThat(provider().getCpuTemperaturePerCore()).hasSize(LOGICAL_CORES);
    }

    @Test
    void getCpuTemperaturePerCoreShouldReturnValuesBetween0And150() {
        double[] temperaturePerCore = provider().getCpuTemperaturePerCore();
        for (double temperature : temperaturePerCore) {
            assertThat(temperature).isNotNaN().isBetween(MIN_TEMPERATURE, MAX_TEMPERATURE);
        }
    }

    @Test
    void getCpuLoadPercentShouldNotBeNull() {
        assertThat(provider().getCpuLoadPercent()).isNotNull();
    }

    @Test
    void getCpuLoadPercentShouldReturnValueBetween0And100() {
        assertThat(provider().getCpuLoadPercent()).isNotNaN().isBetween(MIN_LOAD, MAX_LOAD);
    }

    @Test
    void getCpuTemperatureMaxShouldNotBeNull() {
        assertThat(provider().getCpuTemperatureMax()).isNotNull();
    }

    @Test
    void getCpuTemperatureMaxShouldReturnValueBetween0And150() {
        assertThat(provider().getCpuTemperatureMax()).isNotNaN().isBetween(MIN_TEMPERATURE, MAX_TEMPERATURE);
    }


}
