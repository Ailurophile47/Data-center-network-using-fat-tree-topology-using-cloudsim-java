package com.datacenter.simulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Performance analyzer for comparing different Fat Tree configurations
 * This utility helps evaluate the impact of different k values and workloads
 */
public class PerformanceAnalyzer {
    
    public static void main(String[] args) {
        System.out.println("=".repeat(55));
        System.out.println("            FAT TREE PERFORMANCE ANALYZER             ");
        System.out.println("          Comparing Different Configurations          ");
        System.out.println("=".repeat(55));
        System.out.println();
        
        // Run comparative analysis
        compareNetworkSizes();
        compareWorkloadScenarios();
        generateRecommendations();
    }
    
    /**
     * Compare different Fat Tree network sizes (k values)
     */
    private static void compareNetworkSizes() {
        System.out.println("NETWORK SIZE COMPARISON");
        System.out.println("=".repeat(60));
        
        int[] kValues = {2, 4, 6, 8};
        
        System.out.printf("%-8s %-12s %-15s %-15s %-15s%n", 
                         "k", "Max Hosts", "Core Switches", "Agg Switches", "Edge Switches");
        System.out.println("-".repeat(60));
        
        for (int k : kValues) {
            if (k % 2 == 0) { // Only even k values are valid
                int maxHosts = (k * k * k) / 4;
                int coreSwitches = (k / 2) * (k / 2);
                int aggSwitches = k * (k / 2);
                int edgeSwitches = k * (k / 2);
                
                System.out.printf("%-8d %-12d %-15d %-15d %-15d%n", 
                                 k, maxHosts, coreSwitches, aggSwitches, edgeSwitches);
            }
        }
        
        System.out.println();
        printScalabilityAnalysis();
    }
    
    /**
     * Print scalability analysis
     */
    private static void printScalabilityAnalysis() {
        System.out.println("SCALABILITY ANALYSIS");
        System.out.println("-".repeat(40));
        
        System.out.println("Key Observations:");
        System.out.println("• Network capacity grows as O(k³)");
        System.out.println("• Switch count grows as O(k²)");  
        System.out.println("• Bisection bandwidth grows as O(k²)");
        System.out.println("• Cost grows approximately as O(k²)");
        System.out.println();
        
        System.out.println("Recommended k values:");
        System.out.println("• k=4: Small data centers (16 hosts)");
        System.out.println("• k=6: Medium data centers (54 hosts)");
        System.out.println("• k=8: Large data centers (128 hosts)");
        System.out.println("• k=12+: Enterprise data centers (432+ hosts)");
        System.out.println();
    }
    
    /**
     * Compare different workload scenarios
     */
    private static void compareWorkloadScenarios() {
        System.out.println("WORKLOAD SCENARIO COMPARISON");
        System.out.println("=".repeat(60));
        
        WorkloadScenario[] scenarios = {
            new WorkloadScenario("Web Serving", 0.3, 0.2, 0.8, "Low CPU, High Network"),
            new WorkloadScenario("Batch Processing", 0.9, 0.6, 0.2, "High CPU, Low Network"), 
            new WorkloadScenario("Data Analytics", 0.7, 0.8, 0.6, "High CPU, High Memory"),
            new WorkloadScenario("Microservices", 0.4, 0.3, 0.7, "Moderate CPU, High Network"),
            new WorkloadScenario("Machine Learning", 0.8, 0.9, 0.4, "Very High CPU & Memory")
        };
        
        System.out.printf("%-18s %-8s %-8s %-8s %-25s%n", 
                         "Scenario", "CPU", "Memory", "Network", "Characteristics");
        System.out.println("-".repeat(75));
        
        for (WorkloadScenario scenario : scenarios) {
            System.out.printf("%-18s %-8.1f %-8.1f %-8.1f %-25s%n",
                             scenario.name, scenario.cpuUsage, scenario.memoryUsage, 
                             scenario.networkUsage, scenario.description);
        }
        
        System.out.println();
        printWorkloadRecommendations();
    }
    
    /**
     * Print workload-specific recommendations
     */
    private static void printWorkloadRecommendations() {
        System.out.println("WORKLOAD OPTIMIZATION TIPS");
        System.out.println("-".repeat(45));
        
        System.out.println("For Network-Intensive Workloads:");
        System.out.println("• Use larger k values for more bisection bandwidth");
        System.out.println("• Consider 10Gbps+ host connections");
        System.out.println("• Implement traffic-aware VM placement");
        System.out.println();
        
        System.out.println("For CPU-Intensive Workloads:");
        System.out.println("• Focus on host CPU capacity over network size");
        System.out.println("• Use fewer, more powerful hosts");
        System.out.println("• Consider CPU affinity for VMs");
        System.out.println();
        
        System.out.println("For Memory-Intensive Workloads:");
        System.out.println("• Increase host RAM capacity");
        System.out.println("• Implement memory-aware scheduling");
        System.out.println("• Consider NUMA topology");
        System.out.println();
    }
    
    /**
     * Generate configuration recommendations
     */
    private static void generateRecommendations() {
        System.out.println("CONFIGURATION RECOMMENDATIONS");
        System.out.println("=".repeat(60));
        
        printNetworkDesignGuidelines();
        printPerformanceTuningTips();
        printFutureEnhancements();
    }
    
    /**
     * Print network design guidelines
     */
    private static void printNetworkDesignGuidelines() {
        System.out.println("NETWORK DESIGN GUIDELINES");
        System.out.println("-".repeat(40));
        
        System.out.println("1. Choosing k value:");
        System.out.println("   • Start with k=4 for development/testing");
        System.out.println("   • Use k=6 or k=8 for production workloads");
        System.out.println("   • Ensure k is even and matches your scale needs");
        System.out.println();
        
        System.out.println("2. Switch specifications:");
        System.out.println("   • Use 10Gbps+ inter-switch links");
        System.out.println("   • Ensure sufficient port density");
        System.out.println("   • Consider switch buffer sizes for bursty traffic");
        System.out.println();
        
        System.out.println("3. Host connectivity:");
        System.out.println("   • 1-10Gbps per host depending on workload");
        System.out.println("   • Consider dual-homing for redundancy");
        System.out.println("   • Balance compute vs. network resources");
        System.out.println();
    }
    
    /**
     * Print performance tuning tips
     */
    private static void printPerformanceTuningTips() {
        System.out.println("PERFORMANCE TUNING TIPS");
        System.out.println("-".repeat(35));
        
        System.out.println("Simulation Optimization:");
        System.out.println("• Adjust CloudSim logging levels for faster execution");
        System.out.println("• Use appropriate time granularity for your use case");
        System.out.println("• Consider parallel execution for large simulations");
        System.out.println();
        
        System.out.println("Workload Modeling:");
        System.out.println("• Use realistic utilization patterns");
        System.out.println("• Model traffic locality (80/20 rule)");
        System.out.println("• Include background traffic for accuracy");
        System.out.println();
        
        System.out.println("Resource Allocation:");
        System.out.println("• Implement topology-aware VM placement");
        System.out.println("• Use load balancing across network paths");
        System.out.println("• Consider QoS requirements for different workloads");
        System.out.println();
    }
    
    /**
     * Print future enhancement suggestions
     */
    private static void printFutureEnhancements() {
        System.out.println("FUTURE ENHANCEMENTS");
        System.out.println("-".repeat(30));
        
        System.out.println("Network Features:");
        System.out.println("• SDN controller integration");
        System.out.println("• Dynamic routing protocols (ECMP, etc.)");
        System.out.println("• Network failure simulation");
        System.out.println("• Congestion control mechanisms");
        System.out.println();
        
        System.out.println("Workload Features:");
        System.out.println("• Container-based workloads");
        System.out.println("• Auto-scaling capabilities");
        System.out.println("• Live migration simulation");
        System.out.println("• Energy consumption modeling");
        System.out.println();
        
        System.out.println("Analysis Features:");
        System.out.println("• Real-time visualization");
        System.out.println("• Machine learning for optimization");
        System.out.println("• Comparative studies with other topologies");
        System.out.println("• Export results to standard formats");
        System.out.println();
    }
    
    /**
     * Calculate theoretical performance metrics
     */
    public static void calculateTheoreticalMetrics(int k) {
        System.out.printf("\nTHEORETICAL METRICS FOR k=%d%n", k);
        System.out.println("-".repeat(40));
        
        int maxHosts = (k * k * k) / 4;
        int totalSwitches = (k * k) / 4 + k * k; // core + agg + edge
        double bisectionBandwidth = Math.pow(k / 2.0, 2); // in units of switch bandwidth
        int maxPaths = (k / 2) * (k / 2);
        
        System.out.printf("Maximum Hosts:         %d%n", maxHosts);
        System.out.printf("Total Switches:        %d%n", totalSwitches);
        System.out.printf("Bisection BW (units):  %.0f%n", bisectionBandwidth);
        System.out.printf("Max Equal Paths:       %d%n", maxPaths);
        System.out.printf("Network Diameter:      %d hops%n", 6); // Always 6 for fat tree
        System.out.printf("Fault Tolerance:       High (multiple paths)%n");
    }
    
    /**
     * Helper class for workload scenarios
     */
    private static class WorkloadScenario {
        final String name;
        final double cpuUsage;
        final double memoryUsage;
        final double networkUsage;
        final String description;
        
        public WorkloadScenario(String name, double cpu, double memory, double network, String desc) {
            this.name = name;
            this.cpuUsage = cpu;
            this.memoryUsage = memory;
            this.networkUsage = network;
            this.description = desc;
        }
    }
}