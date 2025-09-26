package com.datacenter.simulation;

import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.cloudlets.network.CloudletExecutionTask;
import org.cloudbus.cloudsim.cloudlets.network.CloudletReceiveTask;
import org.cloudbus.cloudsim.cloudlets.network.CloudletSendTask;
import org.cloudbus.cloudsim.cloudlets.network.NetworkCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.network.NetworkDatacenter;
import org.cloudbus.cloudsim.hosts.network.NetworkHost;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.ResourceProvisionerSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelDynamic;
import org.cloudbus.cloudsim.vms.network.NetworkVm;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Enhanced Data Center Network Simulation using Fat Tree topology
 * 
 * This simulation demonstrates:
 * - Fat tree network topology creation
 * - Network-aware VM placement
 * - Inter-VM communication patterns
 * - Performance metrics collection
 * - Network utilization analysis
 */
public class DataCenterFatTreeSimulation {
    
    // Simulation parameters
    // Fast mode toggle: enable by setting env SIM_FAST=true or system property -Dsim.fast=true
    private static final boolean FAST_MODE = "true".equalsIgnoreCase(System.getenv("SIM_FAST"))
        || "true".equalsIgnoreCase(System.getProperty("sim.fast"));

    // Reduced defaults for faster development / testing runs (can be further reduced in FAST_MODE)
    private static final int HOSTS = FAST_MODE ? 2 : 4;            // Number of hosts (must be <= k^3/4)
    private static final int VMS = FAST_MODE ? 2 : 4;              // Number of VMs
    private static final int CLOUDLETS = FAST_MODE ? 2 : 8;        // Number of cloudlets
    private static final int FAT_TREE_K = 4;                       // Fat tree parameter (must be even)
    
    // Host specifications (realistic data center specs)
    private static final long HOST_MIPS = 20000;   // 20 GIPS per core
    private static final int HOST_PES = 8;         // 8 cores per host
    private static final long HOST_RAM = 32768;    // 32 GB RAM
    private static final long HOST_STORAGE = 2000000; // 2 TB storage
    private static final long HOST_BW = 10000;     // 10 Gbps network
    
    // VM specifications
    private static final long VM_MIPS = 5000;      // 5 GIPS per core
    private static final int VM_PES = 2;           // 2 cores per VM
    private static final long VM_RAM = 4096;       // 4 GB RAM
    private static final long VM_STORAGE = 100000; // 100 GB storage
    private static final long VM_BW = 1000;        // 1 Gbps network
    
    // Cloudlet specifications
    // Shorter cloudlets for faster simulation runs during development
    private static final long CLOUDLET_LENGTH = FAST_MODE ? 200 : 1000;  // smaller workload in FAST_MODE
    private static final long CLOUDLET_FILE_SIZE = 1024; // 1 MB
    private static final long CLOUDLET_OUTPUT_SIZE = 1024; // 1 MB
    
    private CloudSim simulation;
    private DatacenterBroker broker;
    private List<NetworkHost> hostList;
    private List<NetworkVm> vmList;
    private List<NetworkCloudlet> cloudletList;
    // For FAST_MODE use simple cloudlets to ensure they complete reliably
    private List<Cloudlet> simpleCloudletList;
    private NetworkDatacenter datacenter;
    private FatTreeNetwork fatTreeNetwork;
    
    // Performance tracking
    private long simulationStartTime;
    private long simulationEndTime;

    public static void main(String[] args) {
        new DataCenterFatTreeSimulation();
    }

    public DataCenterFatTreeSimulation() {
        simulationStartTime = System.currentTimeMillis();
        
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║     DATA CENTER FAT TREE NETWORK SIMULATION         ║");
        System.out.println("║              CloudSim Plus Framework                ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            initializeSimulation();
            createInfrastructure();
            createWorkload();
            runSimulation();
            analyzeResults();
        } catch (Exception e) {
            System.err.println("Simulation failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            simulationEndTime = System.currentTimeMillis();
            System.out.printf("%nTotal simulation wall-clock time: %.2f seconds%n", 
                             (simulationEndTime - simulationStartTime) / 1000.0);
        }
    }
    
    /**
     * Initialize the CloudSim simulation environment
     */
    private void initializeSimulation() {
        System.out.println("Initializing CloudSim simulation environment...");
        simulation = new CloudSim();
        broker = new DatacenterBrokerSimple(simulation);
        // Only apply aggressive VM/datacenter timing tweaks when NOT in FAST_MODE.
        // FAST_MODE will use small cloudlets instead and shouldn't need these hacks.
        if (!FAST_MODE) {
            // Try to increase VM destruction delay to avoid VMs being removed while cloudlets are running
            // Use a large finite value instead of infinity to be safer across CloudSim versions
            tryInvoke(broker, "setVmDestructionDelay", new Class[]{double.class}, 100000.0);
            // Try to reduce the minimum time between events to avoid missed events during tight workloads
            tryInvoke(simulation, "setMinTimeBetweenEvents", new Class[]{double.class}, 0.001);
        }
        System.out.println("Simulation environment ready!");
        System.out.println();
    }
    
    /**
     * Create the data center infrastructure
     */
    private void createInfrastructure() {
        System.out.println("Building data center infrastructure...");
        
        // Create network hosts
        hostList = createNetworkHosts();
        
        // Create datacenter
        datacenter = createNetworkDatacenter();
        
        // Create and configure fat tree network
        fatTreeNetwork = new FatTreeNetwork(FAT_TREE_K);
        fatTreeNetwork.setDatacenter(datacenter);
        fatTreeNetwork.connectHosts(hostList);
        fatTreeNetwork.printTopologyStats();
        
        System.out.println("Infrastructure ready!");
        System.out.println();
    }
    
    /**
     * Create the workload (VMs and Cloudlets)
     */
    private void createWorkload() {
        System.out.println("Creating workload...");
        vmList = createNetworkVms();

        broker.submitVmList(vmList);

        if (FAST_MODE) {
            // Create lightweight CloudletSimple objects for reliable, fast runs
            simpleCloudletList = new java.util.ArrayList<>();
            System.out.printf("Creating %d simple cloudlets for FAST_MODE...%n", CLOUDLETS);
            for (int i = 0; i < CLOUDLETS; i++) {
                long length = CLOUDLET_LENGTH; // small in FAST_MODE
                CloudletSimple c = new CloudletSimple(length, VM_PES);
                c.setSizes(CLOUDLET_FILE_SIZE);
                c.setUtilizationModelCpu(new UtilizationModelDynamic(0.05, 0.15));
                simpleCloudletList.add(c);
            }
            broker.submitCloudletList(simpleCloudletList);
        } else {
            cloudletList = createNetworkCloudlets();

            // Bind network cloudlets to VMs deterministically to avoid mapping delays/issues
            if (vmList != null && !vmList.isEmpty()) {
                for (int i = 0; i < cloudletList.size(); i++) {
                    NetworkCloudlet c = cloudletList.get(i);
                    long vmId = vmList.get(i % vmList.size()).getId();
                    try {
                        java.lang.reflect.Method m = c.getClass().getMethod("setVmId", long.class);
                        m.invoke(c, vmId);
                    } catch (Exception ignored) {
                    }
                }
            }
            broker.submitCloudletList(cloudletList);
        }
        
        System.out.println("Workload created and submitted!");
        System.out.println();
    }
    
    /**
     * Run the simulation
     */
    private void runSimulation() {
        System.out.println("Starting simulation execution...");
        System.out.println("   (This may take a few moments...)");
        System.out.println();
        
        simulation.start();
        
        System.out.println("Simulation execution completed!");
        System.out.println();
    }

    /**
     * Create network hosts for the simulation
     */
    private List<NetworkHost> createNetworkHosts() {
        List<NetworkHost> hostList = new ArrayList<>();
        
        System.out.printf("Creating %d network hosts...%n", HOSTS);
        
        for (int i = 0; i < HOSTS; i++) {
            List<Pe> peList = new ArrayList<>();
            
            // Create processing elements (CPU cores)
            for (int j = 0; j < HOST_PES; j++) {
                peList.add(new PeSimple(HOST_MIPS, new PeProvisionerSimple()));
            }
            
            // Create host with network capabilities
            NetworkHost host = new NetworkHost(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
            host.setRamProvisioner(new ResourceProvisionerSimple())
                .setBwProvisioner(new ResourceProvisionerSimple())
                .setVmScheduler(new VmSchedulerTimeShared());
            
            hostList.add(host);
        }
        
        System.out.printf("Created %d network hosts (Total capacity: %.0f GIPS, %d GB RAM)%n", 
                         hostList.size(), 
                         (HOST_MIPS * HOST_PES * HOSTS) / 1000.0,
                         (HOST_RAM * HOSTS) / 1024);
        return hostList;
    }

    /**
     * Create network datacenter with cost model
     */
    private NetworkDatacenter createNetworkDatacenter() {
        NetworkDatacenter dc = new NetworkDatacenter(simulation, hostList, new VmAllocationPolicySimple());
        
        // Set realistic pricing (per second)
        dc.getCharacteristics()
          .setCostPerSecond(0.10)      // $0.10 per second of CPU usage
          .setCostPerMem(0.05)         // $0.05 per MB of RAM per second
          .setCostPerStorage(0.001)    // $0.001 per MB of storage per second
          .setCostPerBw(0.0);          // Free bandwidth (internal)
        
        System.out.printf("Created network datacenter with %d hosts%n", hostList.size());
        // Try to reduce the datacenter scheduling interval to avoid missed events (only for non-fast runs)
        if (!FAST_MODE) {
            tryInvoke(dc, "setSchedulingInterval", new Class[]{double.class}, 0.001);
        }
        return dc;
    }

    /**
     * Utility: attempt to invoke a method reflectively if it exists (silently ignore if not present).
     */
    private void tryInvoke(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        if (target == null) return;
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(methodName, paramTypes);
            m.setAccessible(true);
            m.invoke(target, args);
            System.out.printf("Invoked %s on %s%n", methodName, target.getClass().getSimpleName());
        } catch (NoSuchMethodException ignored) {
            // method not present in this CloudSim Plus version: ignore
        } catch (Exception e) {
            System.err.printf("Failed to invoke %s on %s: %s%n", methodName, target.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Create network VMs with diverse configurations
     */
    private List<NetworkVm> createNetworkVms() {
        List<NetworkVm> vmList = new ArrayList<>();
        
        System.out.printf("Creating %d network VMs...%n", VMS);
        
        for (int i = 0; i < VMS; i++) {
            // Create VMs with slightly different specs for diversity
            long vmMips = VM_MIPS + (i % 3) * 1000; // 5-7 GIPS variation
            long vmRam = VM_RAM + (i % 4) * 512;    // 4-5.5 GB variation
            
            NetworkVm vm = new NetworkVm(i, vmMips, VM_PES);
            vm.setRam(vmRam)
              .setBw(VM_BW)
              .setSize(VM_STORAGE)
              .setCloudletScheduler(new CloudletSchedulerTimeShared());
            
            vmList.add(vm);
        }
        
        System.out.printf("Created %d network VMs (Total capacity: %.0f GIPS)%n", 
                         vmList.size(), (VM_MIPS * VM_PES * VMS) / 1000.0);
        return vmList;
    }

    /**
     * Create network cloudlets with realistic communication patterns
     */
    private List<NetworkCloudlet> createNetworkCloudlets() {
        List<NetworkCloudlet> cloudletList = new ArrayList<>();
        
        System.out.printf("Creating %d network cloudlets...%n", CLOUDLETS);
        
        for (int i = 0; i < CLOUDLETS; i++) {
            // Variable cloudlet lengths to simulate different workload types.
            // Use a delta proportional to CLOUDLET_LENGTH so FAST_MODE stays small.
            long delta = Math.max(1, CLOUDLET_LENGTH / 2);
            long length = CLOUDLET_LENGTH + (i % 5) * delta;
            
            NetworkCloudlet cloudlet = new NetworkCloudlet(i, length, VM_PES);
         cloudlet.setMemory(1024)  // 1 GB memory requirement
             .setFileSize(CLOUDLET_FILE_SIZE)
             .setOutputSize(CLOUDLET_OUTPUT_SIZE)
             // Use lighter utilization during dev/testing so the simulator finishes quicker
             .setUtilizationModelCpu(new UtilizationModelDynamic(0.05, 0.15))   // 5-15% CPU
             .setUtilizationModelRam(new UtilizationModelDynamic(0.02, 0.08))   // 2-8% RAM  
             .setUtilizationModelBw(new UtilizationModelDynamic(0.02, 0.12));   // 2-12% BW
            
            // Add network communication tasks (skip in FAST_MODE to reduce event count)
            if (!FAST_MODE) {
                addRealisticNetworkTasks(cloudlet, i);
            }
            
            cloudletList.add(cloudlet);
        }
        
        System.out.printf("Created %d network cloudlets with communication patterns%n", 
                         cloudletList.size());
        return cloudletList;
    }

    /**
     * Add realistic network communication tasks to simulate data center workloads
     */
    private void addRealisticNetworkTasks(NetworkCloudlet cloudlet, int cloudletId) {
        // Initial computation phase
        CloudletExecutionTask initialTask = new CloudletExecutionTask(
            cloudlet.getTasks().size(), 
            CLOUDLET_LENGTH / 3  // 1/3 of total work
        );
        cloudlet.addTask(initialTask);
        
        // Communication phase (simulate microservices communication)
        if (cloudletId < CLOUDLETS - 1) {
            // Send data to next cloudlet (simulating service chain)
            long dataSize = 2048 + (cloudletId % 8) * 256; // 2-4 MB variable data
            
            CloudletSendTask sendTask = new CloudletSendTask(cloudlet.getTasks().size());
            sendTask.setMemory(dataSize);
            cloudlet.addTask(sendTask);
            
            // Receive response data
            CloudletReceiveTask receiveTask = new CloudletReceiveTask(
                cloudlet.getTasks().size(), 
                null  // Will be set by CloudSim
            );
            receiveTask.setMemory(dataSize / 2); // Response is typically smaller
            cloudlet.addTask(receiveTask);
        }
        
        // Final computation phase
        CloudletExecutionTask finalTask = new CloudletExecutionTask(
            cloudlet.getTasks().size(), 
            CLOUDLET_LENGTH / 2  // Remaining work
        );
        cloudlet.addTask(finalTask);
    }

    /**
     * Analyze and print comprehensive simulation results
     */
    private void analyzeResults() {
        System.out.println("SIMULATION RESULTS ANALYSIS");
        System.out.println("=".repeat(60));
        // Debug: print status of each created cloudlet and their assigned VM (if any)
        System.out.println("\n[DEBUG] Cloudlet statuses after simulation:");
        if (cloudletList != null) {
            for (NetworkCloudlet c : cloudletList) {
                String vmInfo = c.getVm() != null ? String.valueOf(c.getVm().getId()) : "<unassigned>";
                System.out.printf("  Cloudlet %d: status=%s, vm=%s, start=%.2f, finish=%.2f\n",
                        c.getId(), c.getStatus(), vmInfo, c.getExecStartTime(), c.getFinishTime());
            }
        }
        if (simpleCloudletList != null) {
            for (Cloudlet c : simpleCloudletList) {
                String vmInfo = c.getVm() != null ? String.valueOf(c.getVm().getId()) : "<unassigned>";
                System.out.printf("  SimpleCloudlet %d: status=%s, vm=%s, start=%.2f, finish=%.2f\n",
                        c.getId(), c.getStatus(), vmInfo, c.getExecStartTime(), c.getFinishTime());
            }
        }

        // Try to print broker lists sizes if available
        try {
        java.util.List<Cloudlet> created = broker.getCloudletCreatedList();
        java.util.List<Cloudlet> submitted = broker.getCloudletSubmittedList();
        java.util.List<Cloudlet> waiting = broker.getCloudletWaitingList();
        java.util.List<Cloudlet> finished = broker.getCloudletFinishedList();
        System.out.printf("[DEBUG] Broker lists -> created=%d, submitted=%d, waiting=%d, finished=%d\n",
            created.size(), submitted.size(), waiting.size(), finished.size());
        } catch (Throwable t) {
            // ignore if methods not present
        }

        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        
        printExecutionSummary(finishedCloudlets);
        printDetailedResults(finishedCloudlets);
        printPerformanceMetrics(finishedCloudlets);
        printNetworkAnalysis();
        printCostAnalysis(finishedCloudlets);
        printResourceUtilization();
    }
    
    /**
     * Print execution summary
     */
    private void printExecutionSummary(List<Cloudlet> cloudlets) {
        int successful = 0;
        int failed = 0;
        
        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {
                successful++;
            } else {
                failed++;
            }
        }
        
        System.out.println("\nEXECUTION SUMMARY");
        System.out.println("-".repeat(30));
        System.out.printf("Total Cloudlets:       %d%n", cloudlets.size());
        System.out.printf("Successful:           %d (%.1f%%)%n", 
                         successful, (successful * 100.0) / cloudlets.size());
        System.out.printf("Failed:               %d (%.1f%%)%n", 
                         failed, (failed * 100.0) / cloudlets.size());
    }
    
    /**
     * Print detailed cloudlet execution results
     */
    private void printDetailedResults(List<Cloudlet> cloudlets) {
        System.out.println("\nDETAILED EXECUTION RESULTS");
        System.out.println("-".repeat(80));
        System.out.printf("%-8s %-6s %-8s %-12s %-12s %-12s %-10s%n",
                "Cloudlet", "VM", "Host", "Start Time", "Finish Time", "Exec Time", "Status");
        System.out.println("-".repeat(80));
        
        for (Cloudlet cloudlet : cloudlets) {
            long hostId = cloudlet.getVm() != null && cloudlet.getVm().getHost() != null ? 
                         cloudlet.getVm().getHost().getId() : -1;
            long vmId = cloudlet.getVm() != null ? cloudlet.getVm().getId() : -1;
            
            System.out.printf("%-8d %-6d %-8d %-12.2f %-12.2f %-12.2f %-10s%n",
                    cloudlet.getId(),
                    vmId,
                    hostId,
                    cloudlet.getExecStartTime(),
                    cloudlet.getFinishTime(),
                    cloudlet.getActualCpuTime(),
                    cloudlet.getStatus());
        }
    }
    
    /**
     * Print performance metrics
     */
    private void printPerformanceMetrics(List<Cloudlet> cloudlets) {
        double totalExecutionTime = 0;
        double totalWaitingTime = 0;
        double maxFinishTime = 0;
        int successfulCloudlets = 0;
        
        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS) {
                totalExecutionTime += cloudlet.getActualCpuTime();
                totalWaitingTime += cloudlet.getWaitingTime();
                maxFinishTime = Math.max(maxFinishTime, cloudlet.getFinishTime());
                successfulCloudlets++;
            }
        }
        
        if (successfulCloudlets == 0) {
            System.out.println("\nPERFORMANCE METRICS");
            System.out.println("-".repeat(40));
            System.out.println("No successful cloudlets to analyze.");
            return;
        }
        
        System.out.println("\nPERFORMANCE METRICS");
        System.out.println("-".repeat(40));
        System.out.printf("Average Execution Time:    %.2f seconds%n", 
                         totalExecutionTime / successfulCloudlets);
        System.out.printf("Average Waiting Time:      %.2f seconds%n", 
                         totalWaitingTime / successfulCloudlets);
        System.out.printf("Total Makespan:           %.2f seconds%n", maxFinishTime);
        System.out.printf("Throughput:               %.2f cloudlets/second%n", 
                         successfulCloudlets / maxFinishTime);
        
        // Calculate CPU utilization
        double totalCpuTime = hostList.size() * HOST_PES * maxFinishTime;
        double actualCpuUsage = totalExecutionTime * VM_PES;
        System.out.printf("CPU Utilization:          %.2f%%%n", 
                         (actualCpuUsage / totalCpuTime) * 100);
    }
    
    /**
     * Print network analysis
     */
    private void printNetworkAnalysis() {
        System.out.println("\nNETWORK TOPOLOGY ANALYSIS");
        System.out.println("-".repeat(45));
        
        if (fatTreeNetwork == null) {
            System.out.println("Fat tree network not properly initialized.");
            return;
        }
        
        FatTreeNetwork.NetworkStats stats = fatTreeNetwork.getNetworkStats();
        
        System.out.printf("Network Parameter k:       %d%n", stats.k);
        System.out.printf("Network Switches:         %d (Core: %d, Agg: %d, Edge: %d)%n",
                         stats.coreSwitches + stats.aggregationSwitches + stats.edgeSwitches,
                         stats.coreSwitches, stats.aggregationSwitches, stats.edgeSwitches);
        System.out.printf("Connected Hosts:          %d / %d (%.1f%% utilization)%n",
                         stats.connectedHosts, (stats.k * stats.k * stats.k) / 4, stats.utilization);
        System.out.printf("Bisection Bandwidth:      %.1f Gbps%n", 
                         fatTreeNetwork.getBisectionBandwidth());
        System.out.printf("Max Equal-Cost Paths:     %d%n", 
                         fatTreeNetwork.getMaxEqualCostPaths());
        System.out.printf("Network Redundancy:       High (Multiple paths available)%n");
    }
    
    /**
     * Print cost analysis - Fixed version without non-existent methods
     */
    private void printCostAnalysis(List<Cloudlet> cloudlets) {
        double totalCost = 0;
        double totalProcessingCost = 0;
        double totalMemoryCost = 0;
        double totalStorageCost = 0;
        
        // Calculate costs manually since CloudSim Plus doesn't have direct cost methods
        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getStatus() == Cloudlet.Status.SUCCESS && cloudlet.getVm() != null) {
                // Calculate processing cost based on actual CPU time
                double processingCost = cloudlet.getActualCpuTime() * 
                                      datacenter.getCharacteristics().getCostPerSecond();
                
                // Calculate memory cost based on VM RAM and execution time
                double memoryCost = (cloudlet.getVm().getRam().getCapacity() / 1024.0) * 
                                  cloudlet.getActualCpuTime() * 
                                  datacenter.getCharacteristics().getCostPerMem();
                
                // Calculate storage cost based on VM storage and execution time
                double storageCost = (cloudlet.getVm().getStorage().getCapacity() / 1024.0) * 
                                   cloudlet.getActualCpuTime() * 
                                   datacenter.getCharacteristics().getCostPerStorage();
                
                totalProcessingCost += processingCost;
                totalMemoryCost += memoryCost;
                totalStorageCost += storageCost;
                totalCost += (processingCost + memoryCost + storageCost);
            }
        }
        
        System.out.println("\nCOST ANALYSIS");
        System.out.println("-".repeat(30));
        
        if (totalCost > 0) {
            System.out.printf("Total Cost:           $%.2f%n", totalCost);
            System.out.printf("|- Processing Cost:   $%.2f (%.1f%%)%n", 
                             totalProcessingCost, (totalProcessingCost / totalCost) * 100);
            System.out.printf("|- Memory Cost:       $%.2f (%.1f%%)%n", 
                             totalMemoryCost, (totalMemoryCost / totalCost) * 100);
            System.out.printf("|- Storage Cost:      $%.2f (%.1f%%)%n", 
                             totalStorageCost, (totalStorageCost / totalCost) * 100);
            System.out.printf("Cost per Cloudlet:    $%.4f%n", totalCost / cloudlets.size());
        } else {
            System.out.println("No cost data available (costs may be set to 0)");
        }
    }
    
    /**
     * Print resource utilization analysis
     */
    private void printResourceUtilization() {
        System.out.println("\nRESOURCE UTILIZATION");
        System.out.println("-".repeat(40));
        
        // Calculate VM to Host mapping
        Map<Long, Integer> hostVmCount = new HashMap<>();
        for (NetworkVm vm : vmList) {
            if (vm.getHost() != null) {
                long hostId = vm.getHost().getId();
                hostVmCount.put(hostId, hostVmCount.getOrDefault(hostId, 0) + 1);
            }
        }
        
        System.out.printf("Total Hosts:              %d%n", hostList.size());
        System.out.printf("Hosts with VMs:           %d%n", hostVmCount.size());
        System.out.printf("Total VMs:                %d%n", vmList.size());
        System.out.printf("Average VMs per Host:     %.1f%n", 
                         vmList.size() / (double) Math.max(hostVmCount.size(), 1));
        
        // Host utilization details
        System.out.println("\nHost Utilization Details:");
        for (int i = 0; i < Math.min(hostList.size(), 8); i++) { // Show first 8 hosts
            NetworkHost host = hostList.get(i);
            int vmCount = hostVmCount.getOrDefault(host.getId(), 0);
            double cpuUtilization = (vmCount * VM_PES * 100.0) / HOST_PES;
            double ramUtilization = (vmCount * VM_RAM * 100.0) / HOST_RAM;
            
            System.out.printf("Host %d: %d VMs, CPU: %.1f%%, RAM: %.1f%%%n", 
                             i, vmCount, cpuUtilization, ramUtilization);
        }
        if (hostList.size() > 8) {
            System.out.printf("... and %d more hosts%n", hostList.size() - 8);
        }
    }
    
    // Getters for testing and external access
    public CloudSim getSimulation() { return simulation; }
    public DatacenterBroker getBroker() { return broker; }
    public NetworkDatacenter getDatacenter() { return datacenter; }
    public FatTreeNetwork getFatTreeNetwork() { return fatTreeNetwork; }
    public List<NetworkHost> getHostList() { return hostList; }
    public List<NetworkVm> getVmList() { return vmList; }
    public List<NetworkCloudlet> getCloudletList() { return cloudletList; }
}