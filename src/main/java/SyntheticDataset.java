import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull;

import java.util.*;

/**
 * Generator dataset sintetis untuk eksperimen penjadwalan CloudSim Plus.
 * Menghasilkan Cloudlet independen dengan random workload yang reproducible berdasarkan seed.
 */
public class SyntheticDataset {

    // Batasan panjang workload (dalam Million Instructions / MI)
    public static final long MIN_LENGTH = 1_000L;
    public static final long MAX_LENGTH = 100_000L;

    // Konfigurasi Cloudlet sesuai project existing (1 Core / PE per Cloudlet)
    public static final int DEFAULT_PES_NUMBER = 1;

    // Default random seed untuk reproduksibilitas
    public static final long DEFAULT_SEED = 2026L;

    /**
     * Menghasilkan sejumlah taskCount Cloudlet independen menggunakan default seed.
     *
     * @param taskCount jumlah Cloudlet yang diinginkan
     * @return List berisi Cloudlet
     */
    public static List<Cloudlet> generate(int taskCount) {
        return generate(taskCount, DEFAULT_SEED);
    }

    /**
     * Menghasilkan tepat taskCount Cloudlet independen dengan random workload
     * antara MIN_LENGTH (1,000 MI) dan MAX_LENGTH (100,000 MI).
     *
     * @param taskCount jumlah task/Cloudlet yang dihasilkan
     * @param seed      seed acak untuk reproduksibilitas eksperimen
     * @return List berisi tepat taskCount Cloudlet
     */
    public static List<Cloudlet> generate(int taskCount, long seed) {
        if (taskCount <= 0) {
            throw new IllegalArgumentException("taskCount harus lebih besar dari 0, diterima: " + taskCount);
        }

        List<Cloudlet> cloudlets = new ArrayList<>(taskCount);
        Random random = new Random(seed);
        long rangeBound = MAX_LENGTH - MIN_LENGTH + 1; // 99.001

        for (int i = 0; i < taskCount; i++) {
            // Random workload [1000, 100000] MI (inklusif)
            long length = MIN_LENGTH + random.nextLong(rangeBound);

            // Cloudlet ID unik (0, 1, 2, ..., taskCount - 1), PE = 1
            Cloudlet cloudlet = new CloudletSimple(i, length, DEFAULT_PES_NUMBER);
            cloudlet.setUtilizationModelCpu(new UtilizationModelFull());

            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }

    /**
     * Menampilkan ringkasan dataset sintetis:
     * - Jumlah task
     * - Min length
     * - Max length
     * - Average length
     * - Total workload
     * - Sample 10 task pertama
     *
     * @param cloudletList list cloudlet yang akan di-summary
     */
    public static void printDatasetSummary(List<Cloudlet> cloudletList) {
        if (cloudletList == null || cloudletList.isEmpty()) {
            System.out.println("Synthetic Dataset Summary: Dataset kosong atau null.");
            return;
        }

        int count = cloudletList.size();
        long minLength = Long.MAX_VALUE;
        long maxLength = Long.MIN_VALUE;
        long totalWorkload = 0L;

        for (Cloudlet c : cloudletList) {
            long len = c.getLength();
            if (len < minLength) {
                minLength = len;
            }
            if (len > maxLength) {
                maxLength = len;
            }
            totalWorkload += len;
        }

        double avgLength = (double) totalWorkload / count;

        System.out.println("Synthetic Dataset Summary");
        System.out.println("-------------------------");
        System.out.printf("Task count     : %d\n", count);
        System.out.printf("Min length     : %,d MI\n", minLength);
        System.out.printf("Max length     : %,d MI\n", maxLength);
        System.out.printf("Average length : %.2f MI\n", avgLength);
        System.out.printf("Total workload : %,d MI\n", totalWorkload);
        System.out.println();
        System.out.println("Sample (10 task pertama):");

        int sampleSize = Math.min(10, count);
        for (int i = 0; i < sampleSize; i++) {
            Cloudlet c = cloudletList.get(i);
            System.out.printf("Cloudlet %-4d -> %,d MI (PEs: %d)\n", c.getId(), c.getLength(), c.getNumberOfPes());
        }
        System.out.println("---------------------------------------------------------");
    }

    /**
     * Method main untuk memverifikasi dan menguji generator dataset sintetis:
     * 1. Pengujian ukuran 1000, 5000, dan 10000 Cloudlet
     * 2. Validasi keunikan ID
     * 3. Validasi batasan workload [1000, 100000] MI
     * 4. Validasi reproduksibilitas (seed yang sama menghasilkan dataset yang sama persis)
     */
    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("        PENGUJIAN GENERATOR DATASET SINTETIS            ");
        System.out.println("=========================================================\n");

        long seed = 2026L;
        int[] testCounts = {1000, 5000, 10000};

        for (int count : testCounts) {
            System.out.printf(">>> Menguji generate(%d, %d)...\n", count, seed);
            List<Cloudlet> dataset = generate(count, seed);

            // Validasi jumlah
            if (dataset.size() != count) {
                throw new AssertionError("FAIL: Jumlah Cloudlet tidak sesuai! Expected: " + count + ", Actual: " + dataset.size());
            }

            // Validasi keunikan ID dan batasan workload
            Set<Long> idSet = new HashSet<>();
            for (Cloudlet c : dataset) {
                if (!idSet.add(c.getId())) {
                    throw new AssertionError("FAIL: Ditemukan duplicate ID: " + c.getId());
                }
                if (c.getLength() < MIN_LENGTH || c.getLength() > MAX_LENGTH) {
                    throw new AssertionError("FAIL: Workload di luar batas [1000, 100000]: " + c.getLength());
                }
                if (c.getNumberOfPes() != DEFAULT_PES_NUMBER) {
                    throw new AssertionError("FAIL: PEs tidak sesuai konfigurasi existing: " + c.getNumberOfPes());
                }
            }

            System.out.printf("SUCCESS: %d Cloudlet berhasil dibuat dengan ID unik dan length valid [1.000 - 100.000 MI].\n\n", count);
            printDatasetSummary(dataset);
            System.out.println();
        }

        // Pengujian Reproduksibilitas (Determinisme Seed)
        System.out.println(">>> Menguji Reproduksibilitas (seed 2026)...");
        List<Cloudlet> run1 = generate(1000, seed);
        List<Cloudlet> run2 = generate(1000, seed);

        for (int i = 0; i < run1.size(); i++) {
            if (run1.get(i).getLength() != run2.get(i).getLength()) {
                throw new AssertionError("FAIL: Reproducibility gagal pada index " + i);
            }
            if (run1.get(i).getId() != run2.get(i).getId()) {
                throw new AssertionError("FAIL: ID mismatch pada index " + i);
            }
        }
        System.out.println("SUCCESS: Run 1 dan Run 2 menghasilkan dataset identik (100% reproducible).");
        System.out.println("\n=========================================================");
        System.out.println("           SEMUA PENGUJIAN BERHASIL DILALUI!             ");
        System.out.println("=========================================================");
    }
}
