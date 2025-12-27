package server;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

/**
 * 服务器状态监控工具
 * 获取 CPU、内存等信息
 */
public class ServerMonitor {

    public static class SystemStats {
        public double cpuLoad;
        public long memoryUsed; // in MB
    }

    public static SystemStats getStats() {
        SystemStats stats = new SystemStats();

        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        stats.memoryUsed = (totalMemory - freeMemory) / (1024 * 1024);

        // CPU 信息 (需要 com.sun.management 包，这里使用通用方法模拟或简化)
        // 注意：OperatingSystemMXBean 在不同 JDK 版本下表现不同，这里简单返回内存即可
        // 如果需要精确 CPU，通常需要引入 OSHI 库，这里为了作业简单，我们用模拟值或者JVM负载

        stats.cpuLoad = 0.0; // 真实 CPU 获取比较复杂，作业中可用内存变化率代替或留空
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                stats.cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad() * 100;
            }
        } catch (Exception e) {
            // 忽略不支持的系统
        }

        if (stats.cpuLoad < 0) stats.cpuLoad = 0;

        return stats;
    }
}