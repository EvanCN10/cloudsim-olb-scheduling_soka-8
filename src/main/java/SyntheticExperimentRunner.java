import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicy;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.util.Log;
import ch.qos.logback.classic.Level;

import java.util.*;

/**
 * Runner untuk eksperimen penjadwalan Opportunistic Load Balancing (OLB)
 * menggunakan dataset sintetis (1.000 hingga 10.000 task, interval 1.000, 3 run per variasi).
 * Total: 30 simulation runs.
 */
public class SyntheticExperimentRunner {

    // Tarif sewa VM per detik sesuai spesifikasi project OlbSimulation
    private static final double COST_LARGE = 0.12;
    private static final double COST_MEDIUM = 0.05;
    private static final double COST_SMALL = 0.02;

    // Struktur data untuk menyimpan hasil tiap run
    public static class ExperimentResult {
        public final int taskCount;
        public final int run;
        public final long seed;
        public final long totalWorkload;
        public final double makespan;
        public final double totalCost;
        public final double resourceUtilization;
        public final double degreeOfImbalance;

        public ExperimentResult(int taskCount, int run, long seed, long totalWorkload,
                                double makespan, double totalCost,
                                double resourceUtilization, double degreeOfImbalance) {
            this.taskCount = taskCount;
            this.run = run;
            this.seed = seed;
            this.totalWorkload = totalWorkload;
            this.makespan = makespan;
            this.totalCost = totalCost;
            this.resourceUtilization = resourceUtilization;
            this.degreeOfImbalance = degreeOfImbalance;
        }
    }

    // Struktur data untuk menyimpan rata-rata per variasi task
    public static class TaskAverageResult {
        public final int taskCount;
        public final double avgMakespan;
        public final double avgCost;
        public final double avgUtilization;
        public final double avgImbalance;

        public TaskAverageResult(int taskCount, double avgMakespan, double avgCost,
                                 double avgUtilization, double avgImbalance) {
            this.taskCount = taskCount;
            this.avgMakespan = avgMakespan;
            this.avgCost = avgCost;
            this.avgUtilization = avgUtilization;
            this.avgImbalance = avgImbalance;
        }
    }

    public static void main(String[] args) {
        // Nonaktifkan log detail CloudSim Plus agar output terminal bersih dan eksekusi cepat
        Log.setLevel(Level.WARN);

        String line = "----------------------------------------------------------------------------------------";
        String doubleLine = "========================================================================================";

        // Tampilkan bukti pemetaan alokasi 10 VM ke 4 Host fisik (Graded / Non-Otomatis sesuai PENCERDASAN.MD Bab 4)
        displayInitialVmHostMapping();

        System.out.println(doubleLine);
        System.out.println("                 HASIL EKSPERIMEN OLB - DATASET SINTETIS");
        System.out.println(doubleLine);
        System.out.printf("%-8s %-6s %-15s %-14s %-12s %-15s %-12s\n",
                "Task", "Run", "Total MI", "Makespan", "Cost", "Utilization", "Imbalance");
        System.out.println(line);

        List<ExperimentResult> allResults = new ArrayList<>();
        int totalRuns = 0;
        int previousTask = -1;

        // Loop jumlah task: 1000 sampai 10000 dengan interval 1000
        for (int taskCount = 1000; taskCount <= 10000; taskCount += 1000) {
            if (previousTask != -1 && previousTask != taskCount) {
                System.out.println(line);
            }
            previousTask = taskCount;

            for (int run = 1; run <= 3; run++) {
                // Seed terkontrol & reproducible: taskCount + run (misal: 1001, 1002, 1003, dst)
                long seed = (long) taskCount + run;
                ExperimentResult result = runSingleSimulation(taskCount, run, seed);
                allResults.add(result);
                totalRuns++;

                System.out.printf("%-8d %-6d %-15d %-14.2f %-12.2f %-15.2f %-12.4f\n",
                        result.taskCount,
                        result.run,
                        result.totalWorkload,
                        result.makespan,
                        result.totalCost,
                        result.resourceUtilization,
                        result.degreeOfImbalance);
                System.out.flush();
            }
        }
        System.out.println(doubleLine);

        System.out.printf("\n%d RUNS COMPLETED\n\n", totalRuns);

        // Hitung dan Tampilkan Tabel 2: Rata-rata per Variasi Task
        List<TaskAverageResult> averages = calculateAverages(allResults);
        printAverageTable(averages);
    }

    /**
     * Menjalankan 1 run simulasi CloudSim dengan OLB dan dataset sintetis.
     */
    public static ExperimentResult runSingleSimulation(int taskCount, int run, long seed) {
        return runSingleSimulation(taskCount, run, seed, false);
    }

    /**
     * Menjalankan 1 run simulasi CloudSim dengan OLB dan dataset sintetis,
     * serta mencetak tabel pemetaan alokasi VM ke Host jika printMapping bernilai true.
     */
    public static ExperimentResult runSingleSimulation(int taskCount, int run, long seed, boolean printMapping) {
        // 1. Inisialisasi CloudSim instance
        CloudSim simulation = new CloudSim();

        // 2. Buat Host & Datacenter dengan GradedVmAllocationPolicy
        //    (VM LARGE/MEDIUM → Host High Performance, VM SMALL → Host Standard)
        List<Host> hostList = createHosts();
        Datacenter datacenter = new DatacenterSimple(simulation, hostList, new GradedVmAllocationPolicy());

        // 3. Buat Broker
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 4. Buat 10 VM Heterogen (sesuai konfigurasi existing OlbSimulation)
        List<Vm> vmList = createVms();
        broker.submitVmList(vmList);

        // 5. Generate Cloudlet sintetis dengan seed terkontrol
        List<Cloudlet> cloudletList = SyntheticDataset.generate(taskCount, seed);
        long totalWorkload = 0L;
        for (Cloudlet c : cloudletList) {
            totalWorkload += c.getLength();
        }

        // 6. Terapkan algoritma penjadwalan OLB
        applyOlbScheduling(vmList, cloudletList);
        broker.submitCloudletList(cloudletList);

        // 7. Jalankan simulasi
        simulation.start();

        // 8. Tampilkan bukti pemetaan alokasi VM ke Host (Graded / Manual Allocation)
        if (printMapping) {
            printVmAllocationMapping(vmList, hostList);
        }

        // 9. Hitung metrik evaluasi
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        return computeMetrics(taskCount, run, seed, totalWorkload, vmList, finishedCloudlets);
    }

    /**
     * Mencetak bukti tabel pemetaan alokasi VM ke Host fisik
     * sesuai spesifikasi GradedVmAllocationPolicy pada PENCERDASAN.MD (Bab 4.3 & Bab 9 Q1).
     */
    public static void printVmAllocationMapping(List<Vm> vmList, List<Host> hostList) {
        String doubleLine = "========================================================================================";
        String line = "----------------------------------------------------------------------------------------";

        System.out.println("\n" + doubleLine);
        System.out.println("       PEMETAAN ALOKASI VM KE HOST FISIK (GRADED VM ALLOCATION POLICY)");
        System.out.println("      (Sesuai PENCERDASAN.MD Bab 4.3: Terarah / Non-Otomatis First-Fit)");
        System.out.println(doubleLine);
        System.out.printf("%-8s %-10s %-22s %-12s %-20s %-15s\n",
                "VM ID", "Tipe VM", "Kapasitas vCPU/RAM", "Host", "Tipe / Grade Host", "Status Alokasi");
        System.out.println(line);

        for (Vm vm : vmList) {
            Host host = vm.getHost();
            String hostId = (host != null) ? "Host " + host.getId() : "Unassigned";
            String hostType = (host != null && !host.getPeList().isEmpty() && host.getPeList().get(0).getCapacity() >= 1500)
                    ? "High Performance" : "Standard";
            String vCpuRam = String.format("%d Core, %d GB RAM",
                    vm.getNumberOfPes(), (long) (vm.getRam().getCapacity() / 1024));

            String status = "Teralokasi OK";
            System.out.printf("%-8s %-10s %-22s %-12s %-20s %-15s\n",
                    "VM " + vm.getId(), vm.getDescription(), vCpuRam, hostId, hostType, status);
        }

        // Tampilkan Host yang tidak menampung VM (Standby Node sesuai Bab 4.4)
        Set<Long> usedHostIds = new HashSet<>();
        for (Vm vm : vmList) {
            if (vm.getHost() != null) {
                usedHostIds.add(vm.getHost().getId());
            }
        }
        for (Host host : hostList) {
            if (!usedHostIds.contains(host.getId())) {
                String hostType = (!host.getPeList().isEmpty() && host.getPeList().get(0).getCapacity() >= 1500)
                        ? "High Performance" : "Standard";
                System.out.printf("%-8s %-10s %-22s %-12s %-20s %-15s\n",
                        "-", "-", "-", "Host " + host.getId(), hostType, "Standby Node (0 VM)");
            }
        }

        System.out.println(doubleLine);
        System.out.println("Catatan Kebijakan Penempatan:");
        System.out.println("- Host 0 & 1 (High Performance): Menampung VM LARGE & MEDIUM");
        System.out.println("- Host 2 (Standard): Menampung VM SMALL");
        System.out.println("- Host 3 (Standard): Standby / Failover Node (Cadangan)");
        System.out.println(doubleLine + "\n");
    }

    /**
     * Menjalankan verifikasi alokasi awal 10 VM ke 4 Host fisik
     * menggunakan GradedVmAllocationPolicy dan mencetak tabel pemetaannya.
     */
    public static void displayInitialVmHostMapping() {
        CloudSim sim = new CloudSim();
        List<Host> hostList = createHosts();
        Datacenter datacenter = new DatacenterSimple(sim, hostList, new GradedVmAllocationPolicy());
        DatacenterBroker broker = new DatacenterBrokerSimple(sim);
        List<Vm> vmList = createVms();
        broker.submitVmList(vmList);
        sim.start();
        printVmAllocationMapping(vmList, hostList);
    }

    /**
     * Konfigurasi Host sesuai OlbSimulation:
     * - 2 Host High Performance: 4 Core @ 2.000 MIPS, RAM 16 GB, BW 10 Gbps
     * - 2 Host Standard: 4 Core @ 1.000 MIPS, RAM 8 GB, BW 10 Gbps
     */
    private static List<Host> createHosts() {
        List<Host> list = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < 4; p++) {
                peList.add(new PeSimple(2000));
            }
            list.add(new HostSimple(16384, 10000, 1000000, peList));
        }

        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < 4; p++) {
                peList.add(new PeSimple(1000));
            }
            list.add(new HostSimple(8192, 10000, 1000000, peList));
        }

        return list;
    }

    /**
     * Konfigurasi VM sesuai OlbSimulation:
     * - 2 Large: 2 Core @ 1.000 MIPS, RAM 4 GB
     * - 4 Medium: 1 Core @ 1.000 MIPS, RAM 2 GB
     * - 4 Small: 1 Core @ 500 MIPS, RAM 1 GB
     */
    private static List<Vm> createVms() {
        List<Vm> list = new ArrayList<>();
        int vmId = 0;

        for (int i = 0; i < 2; i++) {
            Vm vm = new VmSimple(vmId++, 1000, 2)
                    .setRam(4096)
                    .setBw(1000)
                    .setSize(10000);
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vm.setDescription("LARGE");
            list.add(vm);
        }

        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(vmId++, 1000, 1)
                    .setRam(2048)
                    .setBw(1000)
                    .setSize(10000);
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vm.setDescription("MEDIUM");
            list.add(vm);
        }

        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(vmId++, 500, 1)
                    .setRam(1024)
                    .setBw(1000)
                    .setSize(10000);
            vm.setCloudletScheduler(new CloudletSchedulerSpaceShared());
            vm.setDescription("SMALL");
            list.add(vm);
        }

        return list;
    }

    /**
     * Logika penjadwalan OLB (Opportunistic Load Balancing):
     * Memilih VM dengan ready time terendah.
     */
    private static void applyOlbScheduling(List<Vm> vmList, List<Cloudlet> cloudletList) {
        Map<Vm, Double> readyTime = new HashMap<>();
        for (Vm vm : vmList) {
            readyTime.put(vm, 0.0);
        }

        for (Cloudlet task : cloudletList) {
            Vm bestVm = vmList.stream()
                    .min(Comparator.comparingDouble(readyTime::get))
                    .orElseThrow();

            task.setVm(bestVm);

            double capacityMips = bestVm.getNumberOfPes() * bestVm.getMips();
            double duration = task.getLength() / capacityMips;

            readyTime.put(bestVm, readyTime.get(bestVm) + duration);
        }
    }

    /**
     * Perhitungan metrik evaluasi sesuai implementasi OlbSimulation.
     */
    private static ExperimentResult computeMetrics(int taskCount, int run, long seed, long totalWorkload,
                                                  List<Vm> vmList, List<Cloudlet> finishedCloudlets) {
        Map<Vm, Double> vmExecutionTimes = new HashMap<>();
        for (Vm vm : vmList) {
            vmExecutionTimes.put(vm, 0.0);
        }

        double totalCost = 0.0;
        for (Cloudlet c : finishedCloudlets) {
            Vm vm = c.getVm();
            double execTime = c.getActualCpuTime();
            vmExecutionTimes.put(vm, vmExecutionTimes.get(vm) + execTime);

            double rate = vm.getDescription().equals("LARGE") ? COST_LARGE :
                          vm.getDescription().equals("MEDIUM") ? COST_MEDIUM : COST_SMALL;
            totalCost += (execTime * rate);
        }

        double makespan = Collections.max(vmExecutionTimes.values());
        double minTime = Collections.min(vmExecutionTimes.values());
        double totalBusyTime = vmExecutionTimes.values().stream().mapToDouble(Double::doubleValue).sum();
        double avgTime = totalBusyTime / vmList.size();

        double resourceUtilization = (totalBusyTime / (vmList.size() * makespan)) * 100.0;
        double degreeOfImbalance = (makespan - minTime) / avgTime;

        return new ExperimentResult(taskCount, run, seed, totalWorkload, makespan, totalCost,
                resourceUtilization, degreeOfImbalance);
    }

    /**
     * Menghitung nilai rata-rata dari 3 run untuk setiap variasi taskCount.
     */
    private static List<TaskAverageResult> calculateAverages(List<ExperimentResult> results) {
        Map<Integer, List<ExperimentResult>> grouped = new LinkedHashMap<>();
        for (ExperimentResult r : results) {
            grouped.computeIfAbsent(r.taskCount, k -> new ArrayList<>()).add(r);
        }

        List<TaskAverageResult> avgList = new ArrayList<>();
        for (Map.Entry<Integer, List<ExperimentResult>> entry : grouped.entrySet()) {
            int tc = entry.getKey();
            List<ExperimentResult> runs = entry.getValue();

            double avgMakespan = runs.stream().mapToDouble(r -> r.makespan).average().orElse(0.0);
            double avgCost = runs.stream().mapToDouble(r -> r.totalCost).average().orElse(0.0);
            double avgUtil = runs.stream().mapToDouble(r -> r.resourceUtilization).average().orElse(0.0);
            double avgImb = runs.stream().mapToDouble(r -> r.degreeOfImbalance).average().orElse(0.0);

            avgList.add(new TaskAverageResult(tc, avgMakespan, avgCost, avgUtil, avgImb));
        }

        return avgList;
    }

    /**
     * Mencetak Tabel 1: Hasil Seluruh 30 Run.
     */
    private static void printDetailTable(List<ExperimentResult> results) {
        String line = "----------------------------------------------------------------------------------------";
        String doubleLine = "========================================================================================";

        System.out.println(doubleLine);
        System.out.println("                 HASIL EKSPERIMEN OLB - DATASET SINTETIS");
        System.out.println(doubleLine);
        System.out.printf("%-8s %-6s %-15s %-14s %-12s %-15s %-12s\n",
                "Task", "Run", "Total MI", "Makespan", "Cost", "Utilization", "Imbalance");
        System.out.println(line);

        int currentTask = -1;
        for (ExperimentResult r : results) {
            if (currentTask != -1 && currentTask != r.taskCount) {
                System.out.println(line);
            }
            currentTask = r.taskCount;

            System.out.printf("%-8d %-6d %-15d %-14.2f %-12.2f %-15.2f %-12.4f\n",
                    r.taskCount,
                    r.run,
                    r.totalWorkload,
                    r.makespan,
                    r.totalCost,
                    r.resourceUtilization,
                    r.degreeOfImbalance);
        }
        System.out.println(doubleLine);
    }

    /**
     * Mencetak Tabel 2: Rata-Rata Hasil Eksperimen.
     */
    private static void printAverageTable(List<TaskAverageResult> averages) {
        String line = "----------------------------------------------------------------------------------------";
        String doubleLine = "========================================================================================";

        System.out.println(doubleLine);
        System.out.println("                    RATA-RATA HASIL EKSPERIMEN");
        System.out.println(doubleLine);
        System.out.printf("%-10s %-18s %-14s %-19s %-15s\n",
                "Task", "Avg Makespan", "Avg Cost", "Avg Utilization", "Avg Imbalance");
        System.out.println(line);

        for (TaskAverageResult a : averages) {
            System.out.printf("%-10d %-18.2f %-14.2f %-19.2f %-15.4f\n",
                    a.taskCount,
                    a.avgMakespan,
                    a.avgCost,
                    a.avgUtilization,
                    a.avgImbalance);
        }
        System.out.println(doubleLine);
    }
}
