package com.kotecku.kittop.cpu;

public interface CpuInfoProvider {
    double[] getCpuLoadPerCore();
    double[] getCpuTemperaturePerCore();
    double getCpuLoadPercent();
    double getCpuTemperatureMax();
}
