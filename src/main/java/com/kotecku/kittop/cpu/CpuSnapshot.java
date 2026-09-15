package com.kotecku.kittop.cpu;

public record CpuSnapshot(
        double[] cpuLoadPerCore,
        double[] cpuTemperaturePerCore,
        double cpuLoadPercent,
        double cpuTemperatureMax
) {
}
