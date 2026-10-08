# REKAPITULASI HASIL PENGUJIAN CLOUD TASK SCHEDULING (OLB)

> **Infrastruktur**: 4 Host fisik (2 High Performance, 2 Standard) & 10 VM Heterogen (2 Large, 4 Medium, 4 Small)
> **Kebijakan Alokasi**: `GradedVmAllocationPolicy` (VM-Host Grade Aware)
> **Dataset**: Google Cloud Jobs (`GoCJ_Dataset_1000.csv`)
> **Metode Pengujian**: Disajikan dalam 2 pendekatan pengujian komprehensif: **Trace-Driven (Sekuensial)** dan **Random Sampling (Acak)** dengan masing-masing 3 kali pengulangan (total 60 run data).

---

## BAGIAN 1: PENDEKATAN TRACE-DRIVEN (ALUR MASUK SEKUENSIAL)

### 1.1 Karakteristik Pendekatan
- **Metode**: Tugas dibaca secara berurutan persis dari baris awal trace dataset GoCJ (baris 1 s/d N).
- **Karakteristik**: Bersifat **100% Deterministik**. Karena dataset dan spesifikasi VM tidak berubah serta algoritma OLB berbasis formula pasti ($j^* = \arg\min(\text{readyTime})$), maka hasil Run 1, Run 2, dan Run 3 bernilai **identik** (standar deviasi = 0).
- **Tujuan Akademik**: Menjamin **reprodusibilitas (*reproducibility*)** pengujian agar dapat dibandingkan secara adil (*apple-to-apple*) dengan algoritma lain pada urutan kedatangan yang sama persis.

### 1.2 Tabel Rata-rata Trace-Driven (100 - 1.000 Tasks)

| Jumlah Task | Makespan Rata-rata (detik) | Total Cost Rata-rata ($) | Resource Utilization Rata-rata (%) | Degree of Imbalance Rata-rata |
| :---: | :---: | :---: | :---: | :---: |
| **100** | 17.078,41 | $5.541,19 | 48,09 % | 1,8122 |
| **200** | 68.599,70 | $23.585,50 | 46,64 % | 1,9292 |
| **300** | 180.452,95 | $56.043,04 | 42,38 % | 2,0365 |
| **400** | 365.392,41 | $108.063,04 | 39,48 % | 2,3621 |
| **500** | 604.126,77 | $175.949,00 | 38,48 % | 2,4258 |
| **600** | 999.698,09 | $284.846,28 | 35,89 % | 2,6162 |
| **700** | 1.219.718,22 | $383.690,51 | 39,50 % | 2,2913 |
| **800** | 1.555.496,54 | $485.351,93 | 39,68 % | 2,2928 |
| **900** | 1.817.028,95 | $573.980,85 | 41,33 % | 2,1033 |
| **1000** | 2.278.486,77 | $727.760,54 | 43,23 % | 1,9416 |

### 1.3 Data Detail 30 Run Trace-Driven

| No | Jumlah Task | Run Ke- | Makespan (detik) | Total Cost ($) | Resource Utilization (%) | Degree of Imbalance |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | 100 | Run 1 | 17.078,41 | $5.541,19 | 48,09 % | 1,8122 |
| 2 | 100 | Run 2 | 17.078,41 | $5.541,19 | 48,09 % | 1,8122 |
| 3 | 100 | Run 3 | 17.078,41 | $5.541,19 | 48,09 % | 1,8122 |
| 4 | 200 | Run 1 | 68.599,70 | $23.585,50 | 46,64 % | 1,9292 |
| 5 | 200 | Run 2 | 68.599,70 | $23.585,50 | 46,64 % | 1,9292 |
| 6 | 200 | Run 3 | 68.599,70 | $23.585,50 | 46,64 % | 1,9292 |
| 7 | 300 | Run 1 | 180.452,95 | $56.043,04 | 42,38 % | 2,0365 |
| 8 | 300 | Run 2 | 180.452,95 | $56.043,04 | 42,38 % | 2,0365 |
| 9 | 300 | Run 3 | 180.452,95 | $56.043,04 | 42,38 % | 2,0365 |
| 10 | 400 | Run 1 | 365.392,41 | $108.063,04 | 39,48 % | 2,3621 |
| 11 | 400 | Run 2 | 365.392,41 | $108.063,04 | 39,48 % | 2,3621 |
| 12 | 400 | Run 3 | 365.392,41 | $108.063,04 | 39,48 % | 2,3621 |
| 13 | 500 | Run 1 | 604.126,77 | $175.949,00 | 38,48 % | 2,4258 |
| 14 | 500 | Run 2 | 604.126,77 | $175.949,00 | 38,48 % | 2,4258 |
| 15 | 500 | Run 3 | 604.126,77 | $175.949,00 | 38,48 % | 2,4258 |
| 16 | 600 | Run 1 | 999.698,09 | $284.846,28 | 35,89 % | 2,6162 |
| 17 | 600 | Run 2 | 999.698,09 | $284.846,28 | 35,89 % | 2,6162 |
| 18 | 600 | Run 3 | 999.698,09 | $284.846,28 | 35,89 % | 2,6162 |
| 19 | 700 | Run 1 | 1.219.718,22 | $383.690,51 | 39,50 % | 2,2913 |
| 20 | 700 | Run 2 | 1.219.718,22 | $383.690,51 | 39,50 % | 2,2913 |
| 21 | 700 | Run 3 | 1.219.718,22 | $383.690,51 | 39,50 % | 2,2913 |
| 22 | 800 | Run 1 | 1.555.496,54 | $485.351,93 | 39,68 % | 2,2928 |
| 23 | 800 | Run 2 | 1.555.496,54 | $485.351,93 | 39,68 % | 2,2928 |
| 24 | 800 | Run 3 | 1.555.496,54 | $485.351,93 | 39,68 % | 2,2928 |
| 25 | 900 | Run 1 | 1.817.028,95 | $573.980,85 | 41,33 % | 2,1033 |
| 26 | 900 | Run 2 | 1.817.028,95 | $573.980,85 | 41,33 % | 2,1033 |
| 27 | 900 | Run 3 | 1.817.028,95 | $573.980,85 | 41,33 % | 2,1033 |
| 28 | 1000 | Run 1 | 2.278.486,77 | $727.760,54 | 43,23 % | 1,9416 |
| 29 | 1000 | Run 2 | 2.278.486,77 | $727.760,54 | 43,23 % | 1,9416 |
| 30 | 1000 | Run 3 | 2.278.486,77 | $727.760,54 | 43,23 % | 1,9416 |

---

## BAGIAN 2: PENDEKATAN RANDOM SAMPLING (PENGAMBILAN ACAK DARI 1.000 TASK)

### 2.1 Karakteristik Pendekatan
- **Metode**: Setiap pengulangan (Run 1, 2, 3), sistem mengambil sebanyak $N$ task secara **acak (*random sampling*)** dari total 1.000 baris dataset GoCJ. Khusus untuk 1.000 task, seluruh tugas terambil dengan **pengacakan urutan kedatangan (*random arrival order*)**.
- **Karakteristik**: Menghasilkan **variasi statistik realistis** antar run. Karena proporsi tugas Kategori 1 (ringan), Kategori 2 (menengah), dan Kategori 3 (berat) yang terambil di tiap run berbeda-beda, nilai Makespan, Cost, RU, dan DI mengalami fluktuasi alami.
- **Tujuan Akademik**: Menguji ketahanan (*robustness*) algoritma OLB terhadap variasi distribusi beban kerja yang dinamis.

### 2.2 Tabel Rata-rata Random Sampling (100 - 1.000 Tasks)

| Jumlah Task | Makespan Rata-rata (detik) | Total Cost Rata-rata ($) | Resource Utilization Rata-rata (%) | Degree of Imbalance Rata-rata |
| :---: | :---: | :---: | :---: | :---: |
| **100** | 17.231,00 | $5.568,02 | 47,98 % | 1,7711 |
| **200** | 67.679,36 | $23.269,36 | 46,94 % | 1,8816 |
| **300** | 185.005,43 | $57.579,89 | 42,16 % | 2,0292 |
| **400** | 366.223,81 | $107.963,49 | 39,17 % | 2,3408 |
| **500** | 598.977,20 | $174.936,13 | 38,16 % | 2,4077 |
| **600** | 1.012.650,74 | $288.834,82 | 35,39 % | 2,6258 |
| **700** | 1.240.509,13 | $389.269,37 | 39,50 % | 2,2937 |
| **800** | 1.551.434,63 | $482.001,94 | 39,35 % | 2,2939 |
| **900** | 1.812.110,49 | $573.609,57 | 41,59 % | 2,1233 |
| **1000** | 2.271.726,63 | $726.647,92 | 44,13 % | 1,9288 |

### 2.3 Data Detail 30 Run Random Sampling

| No | Jumlah Task | Run Ke- | Makespan (detik) | Total Cost ($) | Resource Utilization (%) | Degree of Imbalance |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | 100 | Run 1 | 17.292,72 | $5.568,08 | 47,42 % | 1,7624 |
| 2 | 100 | Run 2 | 17.441,88 | $5.675,12 | 49,27 % | 1,7378 |
| 3 | 100 | Run 3 | 16.958,40 | $5.460,86 | 47,25 % | 1,8132 |
| 4 | 200 | Run 1 | 65.676,55 | $22.471,67 | 47,09 % | 1,9373 |
| 5 | 200 | Run 2 | 66.873,71 | $23.024,92 | 47,57 % | 1,8404 |
| 6 | 200 | Run 3 | 70.487,82 | $24.311,49 | 46,16 % | 1,8672 |
| 7 | 300 | Run 1 | 187.878,44 | $58.196,61 | 41,16 % | 1,9639 |
| 8 | 300 | Run 2 | 186.096,52 | $57.891,68 | 43,30 % | 2,0779 |
| 9 | 300 | Run 3 | 181.041,32 | $56.651,39 | 42,02 % | 2,0459 |
| 10 | 400 | Run 1 | 372.614,13 | $110.407,80 | 40,57 % | 2,3760 |
| 11 | 400 | Run 2 | 369.877,35 | $108.594,53 | 38,66 % | 2,3242 |
| 12 | 400 | Run 3 | 356.179,96 | $104.888,14 | 38,28 % | 2,3221 |
| 13 | 500 | Run 1 | 609.045,01 | $176.997,79 | 38,09 % | 2,3735 |
| 14 | 500 | Run 2 | 595.680,27 | $174.701,08 | 38,92 % | 2,4454 |
| 15 | 500 | Run 3 | 592.206,33 | $173.109,54 | 37,47 % | 2,4041 |
| 16 | 600 | Run 1 | 1.029.060,62 | $293.869,41 | 36,06 % | 2,6494 |
| 17 | 600 | Run 2 | 1.020.262,99 | $291.989,64 | 35,08 % | 2,5320 |
| 18 | 600 | Run 3 | 988.628,62 | $280.645,42 | 35,02 % | 2,6959 |
| 19 | 700 | Run 1 | 1.247.261,97 | $391.191,64 | 39,97 % | 2,2725 |
| 20 | 700 | Run 2 | 1.250.056,09 | $392.975,09 | 38,79 % | 2,2457 |
| 21 | 700 | Run 3 | 1.224.209,33 | $383.641,39 | 39,75 % | 2,3629 |
| 22 | 800 | Run 1 | 1.549.863,18 | $481.422,43 | 41,17 % | 2,2945 |
| 23 | 800 | Run 2 | 1.532.588,34 | $474.738,89 | 38,51 % | 2,3157 |
| 24 | 800 | Run 3 | 1.571.852,36 | $489.844,50 | 38,37 % | 2,2715 |
| 25 | 900 | Run 1 | 1.849.481,76 | $584.504,51 | 42,74 % | 2,1682 |
| 26 | 900 | Run 2 | 1.785.073,44 | $565.877,82 | 41,88 % | 2,1100 |
| 27 | 900 | Run 3 | 1.801.776,28 | $570.446,37 | 40,16 % | 2,0916 |
| 28 | 1000 | Run 1 | 2.274.690,93 | $731.823,64 | 44,36 % | 1,8990 |
| 29 | 1000 | Run 2 | 2.278.534,85 | $724.033,99 | 44,47 % | 2,0083 |
| 30 | 1000 | Run 3 | 2.261.954,10 | $724.086,13 | 43,56 % | 1,8791 |

---

## BAGIAN 3: ANALISIS TREN & PERBANDINGAN KEDUA PENDEKATAN

### 3.1 Perbandingan Perilaku Metrik
1. **Makespan (Waktu Penyelesaian Total)**:
   - Pada pendekatan **Trace-Driven**, Makespan naik secara stabil dan prediktif dari 17.078,41 detik (100 task) hingga 2.278.486,77 detik (1.000 task).
   - Pada pendekatan **Random Sampling**, Makespan berfluktuasi wajar di sekitar nilai rata-rata (rentang variasi ±2% s/d ±5%). Run yang secara acak menarik lebih banyak tugas Kategori 3 (>500.000 MI) menghasilkan Makespan yang lebih panjang.

2. **Total Cost (Biaya Sewa VM)**:
   - Kedua pendekatan menunjukkan pertumbuhan biaya yang linier terhadap total beban instruksi komputasi (MI) yang dieksekusi oleh vCPU berbayar.
   - Biaya rata-rata meningkat dari kisaran ~$5.500 (100 task) hingga ~$727.000 (1.000 task).

3. **Resource Utilization (Tingkat Utilisasi Mesin)**:
   - Persentase utilisasi relatif stabil di kisaran 36% hingga 48% di kedua pendekatan.
   - Hal ini membuktikan bahwa algoritma OLB selalu berhasil membagikan antrean tugas ke seluruh 10 VM yang ada, namun tingkat penyelesaian akhirnya tetap tertahan oleh VM paling lambat.

4. **Degree of Imbalance (Derajat Ketimpangan Beban)**:
   - Nilai DI konsisten berada pada rentang tinggi (> 1.8 hingga > 2.6) di **kedua pendekatan**.
   - Fakta empiris ini sangat penting untuk laporan demo: membuktikan bahwa ketimpangan beban pada OLB **bukan disebabkan oleh kebetulan urutan dataset**, melainkan merupakan kelemahan mendasar dari algoritma OLB itu sendiri yang buta terhadap kapasitas mesin dan ukuran tugas (*resource & size-blind*).
