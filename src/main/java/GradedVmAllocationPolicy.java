import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Kebijakan alokasi VM ke Host berbasis grade/tingkat performa (Grade-Aware Placement).
 *
 * Aturan penempatan terarah sesuai PENCERDASAN.MD:
 * - VM LARGE  (2 Core @ 1.000 MIPS) → Host High Performance (>= 1.500 MIPS/core)
 * - VM MEDIUM (1 Core @ 1.000 MIPS) → Host High Performance (>= 1.500 MIPS/core)
 * - VM SMALL  (1 Core @ 500 MIPS)   → Host Standard (< 1.500 MIPS/core)
 *
 * Fallback: jika Host yang dituju penuh, cari Host lain yang masih cukup kapasitasnya.
 *
 * Matriks penempatan aktual (sesuai PENCERDASAN.MD Tabel 4.3):
 *   VM 0, VM 1 (LARGE)          → Host 0 (High Performance)
 *   VM 2, VM 3, VM 4, VM 5 (MEDIUM) → Host 1 (High Performance)
 *   VM 6, VM 7, VM 8, VM 9 (SMALL)  → Host 2 (Standard)
 *   Host 3 → Standby / idle node
 */
public class GradedVmAllocationPolicy extends VmAllocationPolicyAbstract {

    /** Ambang batas MIPS/core untuk membedakan host High Performance vs Standard */
    private static final double HIGH_PERFORMANCE_THRESHOLD_MIPS_PER_CORE = 1500.0;

    /**
     * Menentukan apakah sebuah Host tergolong High Performance.
     * Host dikategorikan HP jika kecepatan MIPS per core >= 1.500 MIPS.
     */
    private boolean isHighPerformanceHost(Host host) {
        if (host.getPeList().isEmpty()) return false;
        double mipsPerCore = host.getPeList().get(0).getCapacity();
        return mipsPerCore >= HIGH_PERFORMANCE_THRESHOLD_MIPS_PER_CORE;
    }

    /**
     * Menentukan apakah VM memerlukan host High Performance.
     * VM LARGE dan MEDIUM (MIPS >= 1.000) diarahkan ke Host HP.
     * VM SMALL (MIPS < 1.000) diarahkan ke Host Standard.
     */
    private boolean requiresHighPerformanceHost(Vm vm) {
        // LARGE = 2 core 1000 MIPS, MEDIUM = 1 core 1000 MIPS, SMALL = 1 core 500 MIPS
        return vm.getMips() >= 1000.0;
    }

    /**
     * Logika utama pencarian Host untuk VM yang diberikan.
     * 1. Cari Host sesuai grade yang cocok dan cukup kapasitasnya (preferred host).
     * 2. Jika tidak ada → fallback ke Host mana saja yang memiliki kapasitas cukup.
     */
    @Override
    protected Optional<Host> defaultFindHostForVm(Vm vm) {
        List<Host> hostList = getHostList();
        boolean preferHighPerformance = requiresHighPerformanceHost(vm);

        // Langkah 1: Cari Host yang sesuai grade dan memiliki kapasitas cukup
        Optional<Host> preferred = hostList.stream()
                .filter(host -> isHighPerformanceHost(host) == preferHighPerformance)
                .filter(host -> host.isSuitableForVm(vm))
                .min(Comparator.comparingDouble(host ->
                        host.getPeList().stream()
                                .mapToDouble(pe -> pe.getCapacity() * host.getFreePesNumber())
                                .sum()
                ));

        if (preferred.isPresent()) {
            return preferred;
        }

        // Langkah 2: Fallback - cari Host mana saja yang masih cukup kapasitasnya
        return hostList.stream()
                .filter(host -> host.isSuitableForVm(vm))
                .findFirst();
    }
}
