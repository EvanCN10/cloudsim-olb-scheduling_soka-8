# PENCERDASAN CLOUDSIM PLUS & ALGORITMA PENJADWALAN OLB
> **Dokumen Panduan Teknis, Logical Thinking, Bedah Kode, dan Eksekusi Demo**  
> **Workspace:** `C:\cloudsim-workspace`  
> **Pustaka:** CloudSim Plus Versi 7.3.0 | **Runtime:** Java JDK 24 (Target Bytecode 21)

---

## DAFTAR ISI
1. [Koreksi Sintaks & Dependensi (PENTING)](#1-koreksi-sintaks--dependensi-penting)
2. [Konsep Arsitektur 5 Komponen CloudSim Plus](#2-konsep-arsitektur-5-komponen-cloudsim-plus)
3. [Logical Thinking Algoritma OLB & 4 Metrik Evaluasi](#3-logical-thinking-algoritma-olb--4-metrik-evaluasi)
4. [Bedah Kode OlbSimulation.java Baris per Baris](#4-bedah-kode-olbsimulationjava-baris-per-baris)
5. [Kode Lengkap OlbSimulation.java (Siap Pakai)](#5-kode-lengkap-olbsimulationjava-siap-pakai)
6. [Panduan Terminal Maven & Variasi Jumlah Task](#6-panduan-terminal-maven--variasi-jumlah-task)
7. [Panduan Eksekusi 6 Permintaan Dosen saat Demo](#7-panduan-eksekusi-6-permintaan-dosen-saat-demo)

---

## 1. KOREKSI SINTAKS & DEPENDENSI (PENTING)

Pada modul panduan draf awal Anda, terdapat kesalahan deklarasi nama package (`import`). Hampir semua komponen ditulis dengan awalan `org.cloudsimplus.*`. 

### Fakta Teknis Library CloudSim Plus v7.3.0
CloudSim Plus adalah pengembangan dari CloudSim standar. Agar kompatibel dengan ekosistem Java, para pengembangnya mempertahankan namespace paket inti pada **`org.cloudbus.cloudsim.*`**, bukan `org.cloudsimplus.*`. Jika menggunakan import draf awal, kompilasi Java akan menghasilkan error `package does not exist`.

Berikut tabel pemetaan perbaikan yang wajib digunakan:

| Komponen | Import di Draf Awal (SALAH ❌) | Import Resmi di JAR 7.3.0 (BENAR ✅) |
| :--- | :--- | :--- |
| **Mesin Simulasi** | `org.cloudsimplus.core.CloudSimPlus` | `org.cloudbus.cloudsim.core.CloudSim` *(class: `CloudSim`)* |
| **Broker Penjadwal** | `org.cloudsimplus.brokers.DatacenterBrokerSimple` | `org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple` |
| **Interface Broker** | `org.cloudbus.cloudsim.brokers.DatacenterBroker` | `org.cloudbus.cloudsim.brokers.DatacenterBroker` |
| **Datacenter** | `org.cloudsimplus.datacenters.Datacenter` | `org.cloudbus.cloudsim.datacenters.Datacenter` |
| **Datacenter Simple** | `org.cloudsimplus.datacenters.DatacenterSimple` | `org.cloudbus.cloudsim.datacenters.DatacenterSimple` |
| **Host Fisik** | `org.cloudsimplus.hosts.Host` | `org.cloudbus.cloudsim.hosts.Host` |
| **Host Simple** | `org.cloudsimplus.hosts.HostSimple` | `org.cloudbus.cloudsim.hosts.HostSimple` |
| **Core CPU (Pe)** | `org.cloudsimplus.resources.Pe` | `org.cloudbus.cloudsim.resources.Pe` |
| **Pe Simple** | `org.cloudsimplus.resources.PeSimple` | `org.cloudbus.cloudsim.resources.PeSimple` |
| **Virtual Machine** | `org.cloudsimplus.vms.Vm` | `org.cloudbus.cloudsim.vms.Vm` |
| **Vm Simple** | `org.cloudsimplus.vms.VmSimple` | `org.cloudbus.cloudsim.vms.VmSimple` |
| **Task (Cloudlet)** | `org.cloudsimplus.cloudlets.Cloudlet` | `org.cloudbus.cloudsim.cloudlets.Cloudlet` |
| **Cloudlet Simple** | `org.cloudsimplus.cloudlets.CloudletSimple` | `org.cloudbus.cloudsim.cloudlets.CloudletSimple` |
| **Model Utilitas** | `org.cloudsimplus.utilizationmodels.UtilizationModelFull` | `org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull` |
| **Tabel Hasil** | `org.cloudsimplus.builders.tables.CloudletsTableBuilder` | `org.cloudsimplus.builders.tables.CloudletsTableBuilder` ✅ *(paket ini benar)* |

---

## 2. KONSEP ARSITEKTUR 5 KOMPONEN CLOUDSIM PLUS

Simulasi komputasi awan di CloudSim Plus berjalan secara diskrit (*Discrete-Event Simulation*) berbasis objek-objek berikut:

```text
[ CloudSim (Simulation Engine) ]
               │
               ▼
[ Datacenter ] ── menampung ──> [ Host (Server Fisik) ] ── terdiri dari ──> [ Pe (Core CPU) ]
               ▲
               │ alokasi resource
               ▼
[ DatacenterBroker ] ── menempatkan ──> [ Virtual Machine (VM) ]
         │
         │ memetakan (Algoritma Penjadwalan OLB)
         ▼
    [ Cloudlet ] (Beban Kerja / Task Komputasi)
```

### 1. `CloudSim` (Simulation Engine)
- **Fungsi:** Mengelola jam internal (*internal simulation clock*) dan antrean event.
- Simulasi komputasi awan tidak berjalan secara *real-time* detik demi detik jam dinding, melainkan melompati waktu dari satu event ke event berikutnya (misal: saat task selesai atau paket data tiba).

### 2. `Datacenter` & `Host` (Infrastruktur Fisik)
- **`Datacenter`**: Representasi fasilitas fisik penyedia layanan cloud. Bertindak sebagai kontainer bagi satu atau banyak server fisik (`Host`).
- **`Host`**: Server fisik individual. Setiap Host memiliki alokasi memori RAM (MB), Bandwidth jaringan (Mbps), media penyimpanan Storage (MB), dan daftar unit pemrosesan CPU (`List<Pe>`).
- **`Pe` (Processing Element)**: Representasi satu inti prosesor (*CPU Core*). Kecepatannya diukur dalam satuan **MIPS** (*Million Instructions Per Second*).
  - *Kapasitas Komputasi Host* = $\sum (\text{Jumlah Pe} \times \text{MIPS tiap Pe})$.

### 3. `Vm` (Virtual Machine)
- **Fungsi:** Mesin virtual tempat task dieksekusi. VM dialokasikan di atas Host fisik oleh Datacenter.
- Properti VM meliputi: ID, jumlah vCPU (`numberOfPes`), kecepatan komputasi per vCPU (`mips`), RAM, Bandwidth, dan ukuran disk.
- Syarat validasi: Total kebutuhan MIPS dan RAM seluruh VM yang aktif pada suatu Host tidak boleh melebihi kapasitas fisik Host tersebut.

### 4. `Cloudlet` (Task / Beban Komputasi)
- **Fungsi:** Representasi aplikasi atau pekerjaan komputasi yang meminta eksekusi ke CPU.
- Properti terpenting:
  - **`length`**: Beban komputasi total dalam satuan **MI** (*Million Instructions*).
  - **`numberOfPes`**: Jumlah core CPU yang dibutuhkan untuk memproses task tersebut secara paralel.
  - **`utilizationModel`**: Model konsumsi sumber daya (misal: `UtilizationModelFull` berarti task menggunakan 100% kapasitas vCPU yang dialokasikan hingga selesai).

### 5. `DatacenterBroker` (Pialang / Penjadwal)
- **Fungsi:** Bertindak atas nama pengguna (*cloud customer*). Broker bertanggung jawab:
  1. Mendaftarkan daftar VM (`submitVmList`).
  2. Mendaftarkan daftar Cloudlet (`submitCloudletList`).
  3. Menentukan Cloudlet mana dieksekusi oleh VM mana. Di sinilah logika algoritma penjadwalan (seperti OLB, FCFS, Round Robin) diterapkan.

---

## 3. LOGICAL THINKING ALGORITMA OLB & 4 METRIK EVALUASI

### A. Cara Kerja Algoritma Opportunistic Load Balancing (OLB)
OLB adalah algoritma penjadwalan heuristik sederhana dengan prinsip dasar: **"Tugaskan task berikutnya ke mesin yang paling cepat menganggur/bebas tanpa memedulikan ukuran task maupun kecepatan mesin."**

#### Logika Matematika:
1. Broker menyimpan tabel pencatatan waktu selesai mesin:
   $$\text{readyTime}_j = 0, \quad \forall j \in \text{Daftar VM}$$
2. Untuk setiap task $i$ yang masuk antrean berurutan:
   - Evaluasi seluruh VM dan cari indeks VM terpilih ($j^*$):
     $$j^* = \arg\min_{j} (\text{readyTime}_j)$$
   - Hitung durasi eksekusi task pada VM terpilih:
     $$\text{execTime} = \frac{\text{Length Cloudlet}_i}{\text{Total MIPS VM}_{j^*}} = \frac{\text{Length}_i}{\text{numberOfPes}_{j^*} \times \text{mips}_{j^*}}$$
   - Perbarui status waktu ketersediaan VM terpilih:
     $$\text{readyTime}_{j^*} = \text{readyTime}_{j^*} + \text{execTime}$$
   - Pasangkan task $i$ ke VM $j^*$.

#### Karakteristik OLB:
- **Kelebihan:** Kompleksitas komputasi sangat rendah ($\mathcal{O}(n \cdot m)$ dengan $n$ task dan $m$ VM). Menjaga agar semua VM tetap sibuk bekerja (utilisasi tinggi).
- **Kekurangan:** Mengabaikan heterogenitas. Task berukuran raksasa bisa saja terlempar ke VM lambat hanya karena VM tersebut saat itu sedang kosong, yang berakibat pada pembengkakan waktu penyelesaian total (*Makespan*) dan biaya sewa (*Total Cost*).

---

### B. Perhitungan 4 Metrik Evaluasi Proyek

1. **Makespan (Waktu Penyelesaian Total):**
   Waktu saat seluruh task pada sistem telah selesai diproses. Nilainya ditentukan oleh waktu eksekusi VM yang paling terakhir selesai bekerja:
   $$\text{Makespan} = \max_{j} (\text{Total Waktu Eksekusi VM}_j)$$
   *(Satuan: Detik)*

2. **Total Cost (Akumulasi Biaya Sewa VM):**
   Biaya total yang harus dibayar berdasarkan durasi aktif tiap VM dikalikan tarif per detik masing-masing tipe VM:
   $$\text{Total Cost} = \sum_{i \in \text{Finished Cloudlets}} (\text{execTime}_i \times \text{Tarif VM}_i)$$
   *(Satuan: Dollar AS / $)*

3. **Resource Utilization (Persentase Pemanfaatan Sumber Daya):**
   Rata-rata persentase waktu sibuk seluruh VM terhadap durasi Makespan:
   $$\text{Resource Utilization} = \left( \frac{\sum_{j=1}^{m} \text{Total Waktu Eksekusi VM}_j}{m \times \text{Makespan}} \right) \times 100\%$$
   *(Satuan: Persen / %)*

4. **Degree of Imbalance (DI - Derajat Ketidakseimbangan Beban):**
   Mengukur ketimpangan pembagian kerja antar mesin. Semakin kecil nilainya (mendekati 0), pembagian beban kerja antar VM semakin merata:
   $$\text{DI} = \frac{\text{Waktu VM Maksimal} - \text{Waktu VM Minimal}}{\text{Rata-rata Waktu Eksekusi Seluruh VM}}$$

---

## 4. BEDAH KODE OlbSimulation.java BARIS PER BARIS

### Struktur Method pada File:
- `main(...)`: Alur orkestrator simulasi.
- `createHosts()`: Inisialisasi 4 server fisik (24.000 MIPS).
- `createVms()`: Inisialisasi 10 mesin virtual (10.000 MIPS).
- `createCloudlets(int count)`: Pembangkitan dataset task sintetis.
- `applyOlbScheduling(...)`: Logika pemetaan task ke VM menggunakan formula OLB.
- `calculateProjectMetrics(...)`: Ekstraksi metrik Makespan, Cost, Utilisasi, dan DI.

### Bedah Detail Tiap Komponen:

#### 1. Inisialisasi Host Fisik (`createHosts`)
```java
// 2 Host High Performance: 4 Core @ 2.000 MIPS (Total 8.000 MIPS/host)
for (int i = 0; i < 2; i++) {
    List<Pe> peList = new ArrayList<>();
    for (int p = 0; p < 4; p++) peList.add(new PeSimple(2000));
    list.add(new HostSimple(16384, 10000, 1000000, peList)); // RAM 16GB, BW 10Gbps, Storage 1TB
}
// 2 Host Standard: 4 Core @ 1.000 MIPS (Total 4.000 MIPS/host)
for (int i = 0; i < 2; i++) {
    List<Pe> peList = new ArrayList<>();
    for (int p = 0; p < 4; p++) peList.add(new PeSimple(1000));
    list.add(new HostSimple(8192, 10000, 1000000, peList));  // RAM 8GB, BW 1Gbps, Storage 1TB
}
```
- Total MIPS Host Fisik: $(2 \times 4 \times 2000) + (2 \times 4 \times 1000) = 16.000 + 8.000 = \mathbf{24.000\text{ MIPS}}$.

#### 2. Inisialisasi Virtual Machine (`createVms`)
```java
// 2 Large: 2 Core @ 1.000 MIPS, RAM 4GB, Tarif $0.12/detik
for (int i = 0; i < 2; i++) {
    Vm vm = new VmSimple(vmId++, 1000, 2).setRam(4096).setBw(1000).setSize(10000);
    vm.setDescription("LARGE");
    list.add(vm);
}
// 4 Medium: 1 Core @ 1.000 MIPS, RAM 2GB, Tarif $0.05/detik
for (int i = 0; i < 4; i++) {
    Vm vm = new VmSimple(vmId++, 1000, 1).setRam(2048).setBw(1000).setSize(10000);
    vm.setDescription("MEDIUM");
    list.add(vm);
}
// 4 Small: 1 Core @ 500 MIPS, RAM 1GB, Tarif $0.02/detik
for (int i = 0; i < 4; i++) {
    Vm vm = new VmSimple(vmId++, 500, 1).setRam(1024).setBw(1000).setSize(10000);
    vm.setDescription("SMALL");
    list.add(vm);
}
```
- Total MIPS VM: $(2 \times 2 \times 1000) + (4 \times 1 \times 1000) + (4 \times 1 \times 500) = 4.000 + 4.000 + 2.000 = \mathbf{10.000\text{ MIPS}}$.
- Total MIPS VM (10.000) lebih kecil dari kapasitas Host (24.000), sehingga seluruh VM dijamin berhasil dialokasikan (*placed*) ke dalam Datacenter.

#### 3. Logika Penjadwalan OLB (`applyOlbScheduling`)
```java
private static void applyOlbScheduling(List<Vm> vmList, List<Cloudlet> cloudletList) {
    Map<Vm, Double> readyTime = new HashMap<>();
    for (Vm vm : vmList) readyTime.put(vm, 0.0);

    for (Cloudlet task : cloudletList) {
        // Cari VM yang memiliki nilai readyTime terkecil saat ini
        Vm bestVm = vmList.stream()
                .min(Comparator.comparingDouble(readyTime::get))
                .orElseThrow();

        // Tetapkan task ke VM tersebut
        task.setVm(bestVm);

        // Hitung durasi waktu pengerjaan task pada VM terpilih
        double capacityMips = bestVm.getNumberOfPes() * bestVm.getMips();
        double duration = task.getLength() / capacityMips;

        // Akumulasikan readyTime VM tersebut
        readyTime.put(bestVm, readyTime.get(bestVm) + duration);
    }
}
```

---

## 5. KODE LENGKAP OlbSimulation.java (SIAP PAKAI)

Buat file baru bernama **`OlbSimulation.java`** di dalam folder `C:\cloudsim-workspace\src\main\java\` dan masukkan kode berikut:

```java
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

import java.util.*;

public class OlbSimulation {
    // Tarif sewa VM per detik sesuai Draft Desain
    private static final double COST_LARGE = 0.12;
    private static final double COST_MEDIUM = 0.05;
    private static final double COST_SMALL = 0.02;

    public static void main(String[] args) {
        // 1. Inisialisasi Simulation Engine
        CloudSim simulation = new CloudSim();

        // 2. Inisialisasi Datacenter & 4 Host Heterogen (Total 24.000 MIPS)
        List<Host> hostList = createHosts();
        Datacenter datacenter = new DatacenterSimple(simulation, hostList);

        // 3. Broker Penjadwal
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        // 4. Inisialisasi 10 VM Heterogen (Total 10.000 MIPS)
        List<Vm> vmList = createVms();
        broker.submitVmList(vmList);

        // 5. Inisialisasi Cloudlet (Uji 100 Task Sintetis)
        int jumlahTask = 100;
        List<Cloudlet> cloudletList = createCloudlets(jumlahTask);

        // 6. PENERAPAN ALGORITMA OLB
        applyOlbScheduling(vmList, cloudletList);
        broker.submitCloudletList(cloudletList);

        // 7. Jalankan Simulasi
        simulation.start();

        // 8. Tampilkan Tabel Eksekusi & Hitung 4 Metrik Proposal
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        new CloudletsTableBuilder(finishedCloudlets).build();
        calculateProjectMetrics(vmList, finishedCloudlets);
    }

    private static List<Host> createHosts() {
        List<Host> list = new ArrayList<>();
        // 2 Host High Performance: 4 Core @ 2.000 MIPS, RAM 16 GB, BW 10 Gbps
        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < 4; p++) peList.add(new PeSimple(2000));
            list.add(new HostSimple(16384, 10000, 1000000, peList));
        }
        // 2 Host Standard: 4 Core @ 1.000 MIPS, RAM 8 GB, BW 1 Gbps
        for (int i = 0; i < 2; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < 4; p++) peList.add(new PeSimple(1000));
            list.add(new HostSimple(8192, 10000, 1000000, peList));
        }
        return list;
    }

    private static List<Vm> createVms() {
        List<Vm> list = new ArrayList<>();
        int vmId = 0;
        // 2 Large: 2 Core @ 1.000 MIPS, RAM 4 GB
        for (int i = 0; i < 2; i++) {
            Vm vm = new VmSimple(vmId++, 1000, 2).setRam(4096).setBw(1000).setSize(10000);
            vm.setDescription("LARGE");
            list.add(vm);
        }
        // 4 Medium: 1 Core @ 1.000 MIPS, RAM 2 GB
        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(vmId++, 1000, 1).setRam(2048).setBw(1000).setSize(10000);
            vm.setDescription("MEDIUM");
            list.add(vm);
        }
        // 4 Small: 1 Core @ 500 MIPS, RAM 1 GB
        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(vmId++, 500, 1).setRam(1024).setBw(1000).setSize(10000);
            vm.setDescription("SMALL");
            list.add(vm);
        }
        return list;
    }

    private static List<Cloudlet> createCloudlets(int count) {
        List<Cloudlet> list = new ArrayList<>();
        Random rand = new Random(42); // Seed 42 agar nilai acak konsisten/deterministik
        for (int i = 0; i < count; i++) {
            // Rentang panjang task sintetis: 1.000 - 100.000 MI
            long length = 1000 + rand.nextInt(99000);
            Cloudlet c = new CloudletSimple(length, 1);
            c.setFileSize(300 + rand.nextInt(9700));
            c.setOutputSize(300 + rand.nextInt(9700));
            c.setUtilizationModel(new UtilizationModelFull());
            list.add(c);
        }
        return list;
    }

    // Logika Pemetaan OLB: Pilih VM dengan readyTime terendah
    private static void applyOlbScheduling(List<Vm> vmList, List<Cloudlet> cloudletList) {
        Map<Vm, Double> readyTime = new HashMap<>();
        for (Vm vm : vmList) readyTime.put(vm, 0.0);

        for (Cloudlet task : cloudletList) {
            // Evaluasi VM dengan readyTime minimum
            Vm bestVm = vmList.stream()
                    .min(Comparator.comparingDouble(readyTime::get))
                    .orElseThrow();

            task.setVm(bestVm);

            // Perhitungan durasi eksekusi task di VM terpilih
            double capacityMips = bestVm.getNumberOfPes() * bestVm.getMips();
            double duration = task.getLength() / capacityMips;

            // Perbarui readyTime VM terpilih
            readyTime.put(bestVm, readyTime.get(bestVm) + duration);
        }
    }

    private static void calculateProjectMetrics(List<Vm> vmList, List<Cloudlet> finishedCloudlets) {
        Map<Vm, Double> vmExecutionTimes = new HashMap<>();
        for (Vm vm : vmList) vmExecutionTimes.put(vm, 0.0);

        double totalCost = 0.0;
        for (Cloudlet c : finishedCloudlets) {
            Vm vm = c.getVm();
            double execTime = c.getActualCpuTime();
            vmExecutionTimes.put(vm, vmExecutionTimes.get(vm) + execTime);

            // Hitung tarif sewa berdasarkan tipe VM
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

        System.out.println("\n================ HASIL METRIK PROJEK KELOMPOK 8 ================");
        System.out.printf("1. Makespan             : %.2f detik\n", makespan);
        System.out.printf("2. Total Cost           : $%.2f\n", totalCost);
        System.out.printf("3. Resource Utilization : %.2f %%\n", resourceUtilization);
        System.out.printf("4. Degree of Imbalance  : %.4f\n", degreeOfImbalance);
        System.out.println("=================================================================\n");
    }
}
```

---

## 6. PANDUAN TERMINAL MAVEN & VARIASI JUMLAH TASK

Pastikan terminal PowerShell Anda berada di direktori proyek:
```powershell
cd C:\cloudsim-workspace
```

### 1. Menjalankan Simulasi (Kompilasi Sekaligus Eksekusi):
```powershell
mvn compile exec:java "-Dexec.mainClass=OlbSimulation"
```

### 2. Cara Mengubah Jumlah Task (100, 200, atau 500 Task):
Buka file `OlbSimulation.java`, lalu cari baris pada method `main`:
```java
int jumlahTask = 100; // Ubah nilai ini menjadi 200 atau 500
```
Setelah mengubah dan menyimpan file (`Ctrl + S`), jalankan kembali perintah `mvn compile exec:java "-Dexec.mainClass=OlbSimulation"`.

---

## 7. PANDUAN EKSEKUSI 6 PERMINTAAN DOSEN SAAT DEMO

Ikuti urutan langkah berikut secara runtut saat pelaksanaan presentasi/demo di hadapan dosen:

### 1. Penjelasan Algoritma Menggunakan Slide
- **Slide Karakteristik:** Jelaskan bahwa OLB (*Opportunistic Load Balancing*) adalah algoritma penjadwalan berorientasi beban (*load-oriented*) yang bekerja secara *greedy* dengan kompleksitas waktu $\mathcal{O}(n \cdot m)$.
- **Slide Cara Kerja:** Jelaskan bahwa setiap task dialokasikan murni berdasarkan nilai `readyTime` minimum (mesin yang tercepat menganggur), tanpa mempertimbangkan ukuran task maupun biaya sewa mesin.
- **Slide Trade-off / Multi-objective:** Jelaskan bahwa OLB unggul dalam menjaga utilisasi mesin agar tidak ada yang idle, namun memiliki kelemahan berupa potensi Makespan panjang dan biaya sewa (*Total Cost*) yang tinggi karena alokasi yang tidak sensitif terhadap heterogenitas MIPS dan tarif.

### 2. Jalankan Simulasi di Hadapan Dosen
- Buka terminal di Antigravity IDE, jalankan:
  ```powershell
  mvn compile exec:java "-Dexec.mainClass=OlbSimulation"
  ```
- Sorot output tabel CloudSim Plus yang mencantumkan status `SUCCESS` seluruh Cloudlet beserta durasi eksekusinya.
- Tunjukkan ringkasan 4 metrik di bagian bawah konsol.

### 3. Tunjukkan Implementasi Algoritma di Kode
Buka file `OlbSimulation.java` dan sorot method `applyOlbScheduling`:
```java
Vm bestVm = vmList.stream()
        .min(Comparator.comparingDouble(readyTime::get))
        .orElseThrow();
```
Jelaskan ke dosen:
> *"Baris ini mengimplementasikan formula $j^* = \arg\min_j(\text{readyTime}_j)$, di mana sistem memilih VM yang saat itu memiliki akumulasi waktu penyelesaian paling kecil."*

### 4. Tunjukkan Kesesuaian Spesifikasi dengan Proposal
Sandingkan tabel proposal Anda dengan kode Java:
- **Datacenter & Host (`createHosts()`):** Tepat 4 Host fisik (2 Host High Performance 4 Core @ 2.000 MIPS + 2 Host Standard 4 Core @ 1.000 MIPS = 24.000 MIPS).
- **Virtual Machines (`createVms()`):** Tepat 10 VM (2 Large @ 2 Core 1.000 MIPS, 4 Medium @ 1 Core 1.000 MIPS, 4 Small @ 1 Core 500 MIPS = 10.000 MIPS).
- **Dataset Workload (`createCloudlets()`):** Rentang instruksi 1.000 hingga 100.000 MI dengan file I/O 300 KB–10 MB.
- **Metrik Evaluasi (`calculateProjectMetrics()`):** Menghitung tepat 4 metrik: Makespan, Total Cost, Resource Utilization, dan Degree of Imbalance.

### 5 & 6. Prosedur Perekaman & Pengumpulan Video
1. **Perekaman:** Pastikan aplikasi Zoom/OBS merekam tampilan layar penuh (slide + terminal IDE) beserta audio penjelasan Anda.
2. **Unggah YouTube:** Masuk ke YouTube Studio $\to$ Pilih **Upload Video** $\to$ Ubah opsi visibilitas menjadi **Unlisted** (tidak publik, hanya yang memegang link yang bisa menonton).
3. **Pengumpulan:** Salin tautan URL video Unlisted tersebut dan masukkan ke lembar pengumpulan yang ditentukan dosen.
