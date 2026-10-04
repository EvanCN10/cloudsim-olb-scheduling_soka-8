import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class OlbSimulation {

    // Tarif sewa VM per detik sesuai spesifikasi
    private static final double COST_LARGE = 0.12;
    private static final double COST_MEDIUM = 0.05;
    private static final double COST_SMALL = 0.02;

    public static void main(String[] args) {
        CloudSim simulation = new CloudSim();

        // 1. Inisialisasi Datacenter & 4 Host (Total 24.000 MIPS)
        List<Host> hostList = createHosts();
        Datacenter datacenter = new DatacenterSimple(simulation, hostList);

        // 2. Broker Penjadwal
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 3. Inisialisasi 10 VM Heterogen (Total 10.000 MIPS)
        List<Vm> vmList = createVms();
        broker.submitVmList(vmList);

        // 4. Inisialisasi Cloudlet dari Dataset GoCJ (Default: 100 Task)
        int jumlahTask = 100;
        List<Cloudlet> cloudletList = createCloudlets(jumlahTask);

        // 5. Penjadwalan OLB
        applyOlbScheduling(vmList, cloudletList);
        broker.submitCloudletList(cloudletList);

        // 6. Jalankan Simulasi
        simulation.start();

        // 7. Tampilkan Hasil Eksekusi dan Metrik
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
        calculateProjectMetrics(vmList, finishedCloudlets);
    }

    private static List<Host> createHosts() {
        List<Host> list = new ArrayList<>();

        // 2 Host High Performance: 4 Core @ 2.000 MIPS, RAM 16 GB, BW 10 Gbps
        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < 4; p++) {
                peList.add(new PeSimple(2000));
            }
            list.add(new HostSimple(16384, 10000, 1000000, peList));
        }

        // 2 Host Standard: 4 Core @ 1.000 MIPS, RAM 8 GB, BW 10 Gbps
        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < 4; p++) {
                peList.add(new PeSimple(1000));
            }
            list.add(new HostSimple(8192, 10000, 1000000, peList));
        }

        return list;
    }

    private static List<Vm> createVms() {
        List<Vm> list = new ArrayList<>();
        int vmId = 0;

        // 2 Large: 2 Core @ 1.000 MIPS, RAM 4 GB
        for (int i = 0; i < 2; i++) {
            Vm vm = new VmSimple(vmId++, 1000, 2)
                    .setRam(4096)
                    .setBw(1000)
                    .setSize(10000);
            vm.setDescription("LARGE");
            list.add(vm);
        }

        // 4 Medium: 1 Core @ 1.000 MIPS, RAM 2 GB
        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(vmId++, 1000, 1)
                    .setRam(2048)
                    .setBw(1000)
                    .setSize(10000);
            vm.setDescription("MEDIUM");
            list.add(vm);
        }

        // 4 Small: 1 Core @ 500 MIPS, RAM 1 GB
        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(vmId++, 500, 1)
                    .setRam(1024)
                    .setBw(1000)
                    .setSize(10000);
            vm.setDescription("SMALL");
            list.add(vm);
        }

        return list;
    }

    private static List<Cloudlet> createCloudlets(int count) {
        List<Cloudlet> list = new ArrayList<>();
        String datasetPath = "src/main/resources/dataset/GoCJ_Dataset_1000.csv";
        Random rand = new Random(42);

        try (BufferedReader br = new BufferedReader(new FileReader(datasetPath))) {
            String line;
            while ((line = br.readLine()) != null && list.size() < count) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                long length = Long.parseLong(parts[0]);

                Cloudlet c = new CloudletSimple(length, 1);
                c.setFileSize(300 + rand.nextInt(9700));
                c.setOutputSize(300 + rand.nextInt(9700));
                c.setUtilizationModelCpu(new UtilizationModelFull());
                list.add(c);
            }
        } catch (IOException e) {
            System.err.println("Gagal membaca file dataset: " + e.getMessage());
        }

        return list;
    }

    private static void applyOlbScheduling(List<Vm> vmList, List<Cloudlet> cloudletList) {
        Map<Vm, Double> readyTime = new HashMap<>();
        for (Vm vm : vmList) {
            readyTime.put(vm, 0.0);
        }

        for (Cloudlet task : cloudletList) {
            // Pilih VM dengan readyTime terendah
            Vm bestVm = vmList.stream()
                    .min(Comparator.comparingDouble(readyTime::get))
                    .orElseThrow();

            task.setVm(bestVm);

            // Perhitungan durasi eksekusi task di VM terpilih
            double capacityMips = bestVm.getNumberOfPes() * bestVm.getMips();
            double duration = task.getLength() / capacityMips;

            // Update readyTime VM terpilih
            readyTime.put(bestVm, readyTime.get(bestVm) + duration);
        }
    }

    private static void calculateProjectMetrics(List<Vm> vmList, List<Cloudlet> finishedCloudlets) {
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

        System.out.println("\n---------------- Hasil Metrik Evaluasi ----------------");
        System.out.printf("1. Makespan             : %.2f detik\n", makespan);
        System.out.printf("2. Total Cost           : $%.2f\n", totalCost);
        System.out.printf("3. Resource Utilization : %.2f %%\n", resourceUtilization);
        System.out.printf("4. Degree of Imbalance  : %.4f\n", degreeOfImbalance);
        System.out.println("-------------------------------------------------------\n");
    }
}
