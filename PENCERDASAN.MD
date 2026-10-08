# PENCERDASAN TEKNIS CLOUDSIM PLUS & ALGORITMA PENJADWALAN OLB

Dokumen ini merupakan acuan teknis utama (*single source of truth*) untuk implementasi simulasi cloud computing menggunakan CloudSim Plus 7.3.0 dengan algoritma penjadwalan Opportunistic Load Balancing (OLB), kebijakan alokasi terarah `GradedVmAllocationPolicy`, dan dataset Google Cloud Jobs (GoCJ).

---

## 1. GAMBARAN UMUM SISTEM

### 1.1 Tujuan Proyek
Simulasi ini bertujuan untuk mengevaluasi kinerja algoritma penjadwalan tugas Opportunistic Load Balancing (OLB) pada lingkungan komputasi awan yang heterogen (kapasitas server fisik dan mesin virtual bervariasi). Evaluasi dilakukan dengan mengukur empat metrik utama:
1. **Makespan** (waktu penyelesaian seluruh rangkaian tugas)
2. **Total Cost** (biaya sewa VM finansial)
3. **Resource Utilization** (efisiensi pemanfaatan kapasitas mesin)
4. **Degree of Imbalance** (derajat ketimpangan pembagian beban kerja antar VM)

### 1.2 Teknologi & Pustaka
- **Bahasa Pemrograman**: Java (JDK 21 / 24)
- **Build & Dependency Management**: Apache Maven
- **Simulation Framework**: CloudSim Plus 7.3.0
- **Namespace Library**: `org.cloudbus.cloudsim.*` (bukan `org.cloudsimplus.*`)
- **Titik Masuk Utama**: `src/main/java/OlbSimulation.java`
- **Kebijakan Alokasi VM**: `src/main/java/GradedVmAllocationPolicy.java`

### 1.3 Arsitektur Dasar CloudSim Plus
CloudSim Plus bekerja berbasis *Discrete-Event Simulation*. Terdapat 5 entitas utama yang saling berinteraksi secara hierarkis:

1. **CloudSim**: Mesin simulasi inti yang mengelola jam internal (*simulation clock*) dan antrean event. Waktu simulasi bergerak maju berdasarkan eksekusi event, bukan detik riil jam dinding.
2. **Datacenter**: Penyedia infrastruktur fisik (*cloud provider*) yang menaungi server-server fisik (`Host`).
3. **Host**: Server fisik mandiri yang memiliki spesifikasi komputasi: Processing Elements (`Pe` / core CPU), RAM, Bandwidth, dan Storage.
4. **Virtual Machine (VM)**: Mesin virtual yang dialokasikan di dalam Host fisik. VM merupakan unit eksekusi langsung bagi Cloudlet.
5. **DatacenterBroker**: Pialang yang bertindak atas nama pengguna (*cloud customer*). Broker mengelola daftar VM, mendaftarkan tugas (`Cloudlet`), dan menjalankan algoritma penjadwalan (pemetaan Cloudlet ke VM).
6. **Cloudlet**: Beban kerja komputasi (tugas/task) yang memiliki ukuran panjang instruksi tertentu dalam satuan MI (*Million Instructions*).

---

## 2. STRUKTUR PROYEK & PANDUAN MAVEN

### 2.1 Struktur Direktori Proyek
```text
cloudsim-workspace/
├── .gitignore
├── pom.xml
├── PENCERDASAN.MD
└── src/
    └── main/
        ├── java/
        │   ├── GradedVmAllocationPolicy.java
        │   └── OlbSimulation.java
        └── resources/
            └── dataset/
                └── GoCJ_Dataset_1000.csv
```

### 2.2 Penjelasan Komponen File
- **`pom.xml`**: Berkas konfigurasi Maven yang mendefinisikan dependensi `cloudsim-plus` (versi 7.3.0) dan plugin `exec-maven-plugin` untuk mengeksekusi simulasi melalui terminal.
- **`GradedVmAllocationPolicy.java`**: Kelas kebijakan penempatan VM ke Host fisik berbasis kesesuaian spesifikasi (*Grade-Aware Placement*).
- **`OlbSimulation.java`**: Berkas utama simulasi yang menginisialisasi CloudSim, Datacenter, Broker, VM, pembacaan CSV dataset, algoritma OLB, pencetakan pemetaan VM-Host, dan perhitungan metrik.
- **`GoCJ_Dataset_1000.csv`**: Berkas dataset beban kerja yang berisi 1.000 baris tugas dari trace Google Cloud Jobs.
- **`target/`**: Direktori hasil kompilasi bytecode Java (`.class` dan `.jar`). Dihasilkan otomatis oleh Maven dan tidak boleh di-commit ke Git.

### 2.3 Panduan Version Control (Git & GitHub)
Untuk membagikan repositori proyek ini ke rekan tim melalui GitHub:
1. **File/Folder yang wajib di-push**:
   - `pom.xml`
   - `src/` (seluruh berkas kode Java dan dataset CSV di dalamnya)
   - `.gitignore`
   - `PENCERDASAN.MD` (atau `README.md`)
2. **File/Folder yang dilarang di-push**:
   - Folder `target/`
   - Folder konfigurasi lokal IDE seperti `.idea/`, `.vscode/`, atau file `*.iml`.
3. **Konfigurasi standar `.gitignore`**:
   ```gitignore
   target/
   *.class
   .idea/
   *.iml
   .vscode/
   .settings/
   .project/
   .classpath
   ```

### 2.4 Panduan Perintah Maven di Terminal
Pastikan terminal berada pada root direktori:
```powershell
cd C:\cloudsim-workspace
```

1. **Kompilasi dan Jalankan Simulasi**:
   ```powershell
   mvn compile exec:java "-Dexec.mainClass=OlbSimulation"
   ```
2. **Kapan Perlu `mvn clean`?**:
   - `mvn clean` bertugas menghapus folder `target/` hasil kompilasi terdahulu.
   - **Tidak perlu** menjalankan `mvn clean` setiap kali hanya mengubah parameter `jumlahTask` pada file `OlbSimulation.java`.
   - Cukup jalankan `mvn compile exec:java "-Dexec.mainClass=OlbSimulation"`. Maven secara otomatis mendeteksi perubahan file `.java` dan melakukan kompilasi ulang inkremental.
   - Perintah `mvn clean` hanya diperlukan jika terjadi error kompilasi yang aneh, corrupt cache bytecode, atau perubahan dependensi di `pom.xml`.

---

## 3. SPESIFIKASI INFRASTRUKTUR (HOST & VM)

Sistem dimodelkan menggunakan arsitektur heterogen untuk mencerminkan kondisi riil infrastruktur komputasi awan bertingkat (multi-tier).

### 3.1 Spesifikasi Server Fisik (Host)
Terdapat 4 Host fisik yang dinaungi oleh Datacenter:

| ID Host | Tipe / Grade Host | Core CPU (Pe) | Kapasitas per Core | Total MIPS Host | RAM | Bandwidth (BW) | Storage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Host 0 | High Performance | 4 Core | 2.000 MIPS | 8.000 MIPS | 16.384 MB (16 GB) | 10.000 Mbps | 1.000.000 MB |
| Host 1 | High Performance | 4 Core | 2.000 MIPS | 8.000 MIPS | 16.384 MB (16 GB) | 10.000 Mbps | 1.000.000 MB |
| Host 2 | Standard | 4 Core | 1.000 MIPS | 4.000 MIPS | 8.192 MB (8 GB) | 10.000 Mbps | 1.000.000 MB |
| Host 3 | Standard | 4 Core | 1.000 MIPS | 4.000 MIPS | 8.192 MB (8 GB) | 10.000 Mbps | 1.000.000 MB |
| **Total**| **4 Host** | **16 Core** | - | **24.000 MIPS** | **49.152 MB (48 GB)** | - | - |

**Korelasi dengan Revisi Laporan (Spesifikasi Regional / Grade)**:
Jika dosen menanyakan spesifikasi server Jakarta vs Surabaya:
- **Datacenter High-Grade (contoh: Region Jakarta)**: Merepresentasikan Host 0 dan Host 1 dengan spesifikasi tinggi (4 Core @ 2.000 MIPS, RAM 16 GB) untuk tugas-tugas intensif.
- **Datacenter Standard-Grade (contoh: Region Surabaya)**: Merepresentasikan Host 2 dan Host 3 dengan spesifikasi standar (4 Core @ 1.000 MIPS, RAM 8 GB) untuk efisiensi biaya.

### 3.2 Spesifikasi Mesin Virtual (VM)
Terdapat 10 unit VM heterogen yang disewa untuk melayani eksekusi tugas:

| VM ID | Tipe / Grade | vCPU (Core) | MIPS per Core | Total MIPS VM | RAM | Bandwidth | Storage | Tarif Sewa per Detik |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| VM 0 | LARGE | 2 Core | 1.000 MIPS | 2.000 MIPS | 4.096 MB (4 GB) | 1.000 Mbps | 10.000 MB | $0.12 |
| VM 1 | LARGE | 2 Core | 1.000 MIPS | 2.000 MIPS | 4.096 MB (4 GB) | 1.000 Mbps | 10.000 MB | $0.12 |
| VM 2 | MEDIUM | 1 Core | 1.000 MIPS | 1.000 MIPS | 2.048 MB (2 GB) | 1.000 Mbps | 10.000 MB | $0.05 |
| VM 3 | MEDIUM | 1 Core | 1.000 MIPS | 1.000 MIPS | 2.048 MB (2 GB) | 1.000 Mbps | 10.000 MB | $0.05 |
| VM 4 | MEDIUM | 1 Core | 1.000 MIPS | 1.000 MIPS | 2.048 MB (2 GB) | 1.000 Mbps | 10.000 MB | $0.05 |
| VM 5 | MEDIUM | 1 Core | 1.000 MIPS | 1.000 MIPS | 2.048 MB (2 GB) | 1.000 Mbps | 10.000 MB | $0.05 |
| VM 6 | SMALL | 1 Core | 500 MIPS | 500 MIPS | 1.024 MB (1 GB) | 1.000 Mbps | 10.000 MB | $0.02 |
| VM 7 | SMALL | 1 Core | 500 MIPS | 500 MIPS | 1.024 MB (1 GB) | 1.000 Mbps | 10.000 MB | $0.02 |
| VM 8 | SMALL | 1 Core | 500 MIPS | 500 MIPS | 1.024 MB (1 GB) | 1.000 Mbps | 10.000 MB | $0.02 |
| VM 9 | SMALL | 1 Core | 500 MIPS | 500 MIPS | 1.024 MB (1 GB) | 1.000 Mbps | 10.000 MB | $0.02 |
| **Total**| **10 VM** | **12 Core** | - | **10.000 MIPS** | **20.480 MB (20 GB)**| - | - | - |

---

## 4. SISTEM ASSIGN VM KE HOST (GRADED VM ALLOCATION POLICY)

Bagian ini menjawab secara tegas dan ilmiah pertanyaan dosen: **"Bagaimana sistem mengalokasikan 10 VM ke dalam 4 Host fisik sesuai spesifikasi terbaik?"**

### 4.1 Mengapa Perlu Kebijakan Khusus (`GradedVmAllocationPolicy`)?
Pada CloudSim standar, alokasi VM menggunakan `VmAllocationPolicySimple` yang bersifat *First-Fit* buta (hanya mengisi host dari indeks 0 tanpa peduli kecocokan grade).
Untuk memberikan **arah yang jelas dan terdesain secara formal**, kita menerapkan kelas kebijakan mandiri: `GradedVmAllocationPolicy.java`.

### 4.2 Aturan Kebijakan Alokasi Berbasis Grade
Kebijakan ini mengklasifikasikan Host fisik berdasarkan performa per core:
- **High Performance Host**: Kecepatan $\ge 1.500$ MIPS/core (Host 0 & Host 1 @ 2.000 MIPS/core).
- **Standard Host**: Kecepatan $< 1.500$ MIPS/core (Host 2 & Host 3 @ 1.000 MIPS/core).

Aturan pemetaan terarah:
1. **VM LARGE (2 Core @ 1.000 MIPS)**:
   - Ditempatkan ke **Host High Performance** (Host 0 & Host 1) karena membutuhkan densitas daya pemrosesan tinggi.
2. **VM MEDIUM (1 Core @ 1.000 MIPS)**:
   - Ditempatkan ke **Host High Performance** (Host 0 & Host 1) untuk memaksimalkan throughput eksekusi tugas-tugas mid-grade.
3. **VM SMALL (1 Core @ 500 MIPS)**:
   - Ditempatkan ke **Host Standard** (Host 2 & Host 3) untuk efisiensi alokasi sumber daya.
4. **Fallback Mechanism**:
   - Jika Host pada grade yang dituju sudah penuh, sistem secara otomatis mencari Host lain yang masih memiliki kapasitas yang cukup (`isSuitableForVm(vm)`).

### 4.3 Matriks Penempatan Aktual (10 VM ke 4 Host)
Berikut hasil penempatan aktual yang tereksekusi di dalam simulasi:

| VM ID | Tipe VM | Kebutuhan vCPU / RAM | Host Terpilih | Tipe / Grade Host | Status Kapasitas Host Setelah Alokasi |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **VM 0** | LARGE | 2 Core, 4 GB | **Host 0** | High Performance | Sisa Host 0: 2 Core, 12 GB RAM |
| **VM 1** | LARGE | 2 Core, 4 GB | **Host 0** | High Performance | Sisa Host 0: 0 Core, 8 GB RAM (Host 0 Penuh) |
| **VM 2** | MEDIUM | 1 Core, 2 GB | **Host 1** | High Performance | Sisa Host 1: 3 Core, 14 GB RAM |
| **VM 3** | MEDIUM | 1 Core, 2 GB | **Host 1** | High Performance | Sisa Host 1: 2 Core, 12 GB RAM |
| **VM 4** | MEDIUM | 1 Core, 2 GB | **Host 1** | High Performance | Sisa Host 1: 1 Core, 10 GB RAM |
| **VM 5** | MEDIUM | 1 Core, 2 GB | **Host 1** | High Performance | Sisa Host 1: 0 Core, 8 GB RAM (Host 1 Penuh) |
| **VM 6** | SMALL | 1 Core, 1 GB | **Host 2** | Standard | Sisa Host 2: 3 Core, 7 GB RAM |
| **VM 7** | SMALL | 1 Core, 1 GB | **Host 2** | Standard | Sisa Host 2: 2 Core, 6 GB RAM |
| **VM 8** | SMALL | 1 Core, 1 GB | **Host 2** | Standard | Sisa Host 2: 1 Core, 5 GB RAM |
| **VM 9** | SMALL | 1 Core, 1 GB | **Host 2** | Standard | Sisa Host 2: 0 Core, 4 GB RAM (Host 2 Penuh) |
| - | - | - | **Host 3** | Standard | **Idle / Standby Node** (0 VM terpasang) |

### 4.4 Peran Host 3 (Standby Node)
Host 3 berstatus kosong (0 VM). Dalam arsitektur cloud computing industri (*enterprise cloud infrastructure*), menyisakan 1 Host sebagai *hot-standby / failover node* adalah praktik standar untuk:
- Memastikan ketersediaan tinggi (*High Availability*) jika salah satu Host fisik mengalami kegagalan (*hardware failure*).
- Menyediakan ruang cadangan untuk migrasi mesin virtual (*VM Migration*) saat terjadi lonjakan beban komputasi tak terduga.

---

## 5. DATASET BEBAN KERJA (GOOGLE CLOUD JOBS - GoCJ)

### 5.1 Karakteristik File Dataset
Dataset yang digunakan adalah `GoCJ_Dataset_1000.csv` pada folder:
`src/main/resources/dataset/GoCJ_Dataset_1000.csv`

Dataset ini merupakan trace beban kerja komputasi terstandarisasi yang diturunkan dari aktivitas komputasi nyata di Google Cloud. Berkas terdiri dari 1.000 baris data tanpa baris header.

### 5.2 Struktur Kolom CSV & Penjelasannya
Setiap baris CSV memiliki 3 kolom:
```text
<Kolom 1>,<Kolom 2>,<Kolom 3>
```
Contoh 5 baris pertama:
```text
83000,1,1
95000,2,2
91000,3,3
81000,2,3
27500,1,4
```

1. **Kolom 1 - Task Length (MI - Million Instructions)**:
   - Menunjukkan beban komputasi total instruksi yang harus diselesaikan oleh vCPU.
   - Diambil oleh kode program menggunakan: `Long.parseLong(parts[0])` sebagai parameter panjang `Cloudlet`.
2. **Kolom 2 - Job Category / Grade (1, 2, atau 3)**:
   - Menunjukkan tingkatan beban komputasi tugas:
     - **Grade 1 (Low Grade / Ringan)**: Tugas berdurasi cepat (< 80.000 MI).
     - **Grade 2 (Mid Grade / Menengah)**: Tugas komputasi reguler (80.000 - 200.000 MI).
     - **Grade 3 (High Grade / Berat)**: Tugas komputasi berat (*compute-intensive*) (> 200.000 MI, hingga 712.500 MI).
3. **Kolom 3 - Job ID**:
   - Menunjukkan identitas pekerjaan induk (*job lineage*) asal tugas tersebut di lingkungan Google Cloud.

### 5.3 Klasifikasi Ukuran Dataset Pengujian
Pengujian dilakukan berdasarkan tiga tingkatan volume data:
- **Small Dataset**: 100 Task
- **Medium Dataset**: 200 s/d 500 Task
- **Large Dataset**: 1.000 Task (keseluruhan dataset)

### 5.4 Metode Pembacaan Dataset di Program
Data dibaca secara **sekuensial (berurutan dari baris 1)** hingga mencapai batas `count` yang ditentukan pada variabel `jumlahTask`. Data **tidak diacak (non-random)** demi menjamin reprodusibilitas hasil simulasi.

---

## 6. ALGORITMA PENJADWALAN OPPORTUNISTIC LOAD BALANCING (OLB)

### 6.1 Prinsip Dasar OLB
Opportunistic Load Balancing (OLB) adalah algoritma penjadwalan beban kerja dengan prinsip:
> *"Tugaskan setiap tugas yang datang ke Mesin Virtual yang memiliki waktu ketersediaan (ready time) paling awal, tanpa mempertimbangkan ukuran panjang tugas maupun kapasitas kecepatan mesin virtual."*

### 6.2 Alur Kerja Logika OLB
1. Inisialisasi waktu siap untuk setiap VM: `readyTime(vm) = 0.0`.
2. Untuk setiap tugas `task` dalam antrean:
   a. Pilih VM dengan nilai `readyTime` minimum: `bestVm = argmin(readyTime(vm))`.
   b. Pasangkan tugas ke VM terpilih: `task.setVm(bestVm)`.
   c. Hitung durasi estimasi: `duration = task.getLength() / (bestVm.getNumberOfPes() * bestVm.getMips())`.
   d. Perbarui waktu siap VM: `readyTime(bestVm) += duration`.
3. Serahkan daftar tugas ke broker untuk dieksekusi mesin simulasi.

### 6.3 Analisis Karakteristik OLB
- **Keunggulan**: Kompleksitas komputasi sangat ringan O(n * m), utilisasi kuantitas tugas antar VM sangat rata.
- **Kelemahan pada Lingkungan Heterogen**: OLB bersifat acuh terhadap ukuran tugas (*size-blind*). Tugas Kategori 3 berukuran 712.500 MI dapat terlempar ke VM Small (500 MIPS) hanya karena VM tersebut sedang kosong 0 detik. Tugas tersebut butuh waktu 1.425 detik di VM Small (padahal di VM Large hanya 356 detik). Fenomena ini menjadi penyebab utama tingginya Makespan dan Degree of Imbalance.

---

## 7. RUMUS & INTERPRETASI 4 METRIK EVALUASI

### 7.1 Makespan
- **Definisi**: Total waktu yang dibutuhkan sistem hingga seluruh tugas selesai dieksekusi.
- **Formula**:
  $$\text{Makespan} = \max_{j \in \text{vmList}} (\text{ExecutionTime}_j)$$
- **Satuan**: Detik.

### 7.2 Total Cost (Biaya Total)
- **Definisi**: Total biaya finansial sewa mesin virtual selama waktu proses aktif CPU tugas berlangsung.
- **Formula**:
  $$\text{Total Cost} = \sum_{i=1}^{n} (\text{ActualCpuTime}_i \times \text{Tarif}(\text{Vm}_i))$$
- **Tarif Per Detik**: Large = $0.12, Medium = $0.05, Small = $0.02.
- **Satuan**: Dolar AS ($).

### 7.3 Resource Utilization (Tingkat Utilisasi Sumber Daya)
- **Definisi**: Persentase rasio antara total waktu kerja produktif seluruh VM terhadap total kapasitas waktu sepanjang Makespan.
- **Formula**:
  $$\text{Resource Utilization} = \left( \frac{\sum_{j=1}^{m} \text{ExecutionTime}_j}{m \times \text{Makespan}} \right) \times 100\%$$
- **Satuan**: Persen (%).

### 7.4 Degree of Imbalance (DI - Derajat Ketidakseimbangan)
- **Definisi**: Tingkat ketimpangan pembagian durasi eksekusi antar mesin virtual.
- **Formula**:
  $$\text{Degree of Imbalance} = \frac{\max(\text{ExecutionTime}) - \min(\text{ExecutionTime})}{\text{Average}(\text{ExecutionTime})}$$
- **Satuan**: Nilai rasio numerik.

---

## 8. PANDUAN EKSEKUSI & TEST CASES

### 8.1 Cara Mengubah Parameter Jumlah Task
Buka file `src/main/java/OlbSimulation.java` pada baris:
```java
int jumlahTask = 100; // Ubah menjadi 200, 500, atau 1000
```
Simpan file, lalu jalankan di terminal:
```powershell
mvn compile exec:java "-Dexec.mainClass=OlbSimulation"
```

### 8.2 Daftar Test Case Evaluasi
| Test Case | Jumlah Task | Kategori Dataset | Tujuan Analisis |
| :--- | :--- | :--- | :--- |
| TC-01 | 100 Task | Small | Baseline pengujian awal dan verifikasi alokasi 10 VM |
| TC-02 | 200 Task | Medium | Pengamatan perilaku awal saat antrean tugas bertambah |
| TC-03 | 500 Task | Medium | Evaluasi stabilitas sistem pada beban kerja menengah |
| TC-04 | 1000 Task | Large | Evaluasi kapasitas maksimum dari seluruh baris dataset GoCJ |

---

## 9. TANYA JAWAB KRITIS SAAT DEMO (FAQ)

### Q1: Bagaimana mekanisme 10 VM dialokasikan ke 4 Host fisik?
**Jawaban**:
Sistem menggunakan kebijakan alokasi terarah `GradedVmAllocationPolicy`:
- VM LARGE (2 Core) dan VM MEDIUM (1 Core) dialokasikan ke Host High Performance (Host 0 dan Host 1 @ 2.000 MIPS/core). Host 0 menampung 2 VM LARGE, Host 1 menampung 4 VM MEDIUM.
- VM SMALL (1 Core @ 500 MIPS) dialokasikan ke Host Standard (Host 2 @ 1.000 MIPS/core) menampung 4 VM SMALL.
- Host 3 dipertahankan sebagai node cadangan (*standby/redundancy node*).
- Setiap kali simulasi dijalankan, tabel pemetaan ini dicetak langsung di konsol terminal.

### Q2: Mengapa alokasi VM sempat mengalami error "No suitable host found"?
**Jawaban**:
Error tersebut disebabkan oleh konfigurasi awal Bandwidth Host (1.000 Mbps) yang sama dengan permintaan Bandwidth satu VM (1.000 Mbps), sehingga satu Host langsung jenuh setelah menerima 1 VM. Masalah terselesaikan secara tuntas setelah Bandwidth Host dinaikkan menjadi 10.000 Mbps (10 Gbps).

### Q3: Mengapa nilai Degree of Imbalance (DI) relatif tinggi (> 1.8)?
**Jawaban**:
Tingginya nilai DI adalah karakteristik inheren algoritma OLB pada lingkungan heterogen, karena OLB menugaskan tugas hanya berdasarkan ketersediaan waktu (`readyTime`) tanpa mempertimbangkan apakah tugas tersebut Kategori 3 (berat) dan tanpa memeriksa apakah VM tujuan berkategori Small (lambat).
