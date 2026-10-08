import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicyAbstract;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.List;
import java.util.Optional;

/**
 * Kebijakan alokasi VM ke Host berbasis grade/spesifikasi (Grade-Aware Placement).
 *
 * Aturan Alokasi:
 * 1. VM LARGE  (2 Core @ 1000 MIPS) -> Host High Performance (Host 0 & 1, 2000 MIPS/core)
 * 2. VM MEDIUM (1 Core @ 1000 MIPS) -> Host High Performance (Host 0 & 1, 2000 MIPS/core)
 * 3. VM SMALL  (1 Core @  500 MIPS) -> Host Standard (Host 2 & 3, 1000 MIPS/core)
 */
public class GradedVmAllocationPolicy extends VmAllocationPolicyAbstract {

    // Threshold MIPS per core untuk membedakan kategori host:
    // >= 1500 MIPS/core = High Performance Host (Host 0 & 1: 2000 MIPS/core)
    // <  1500 MIPS/core = Standard Host (Host 2 & 3: 1000 MIPS/core)
    private static final double HIGH_PERF_THRESHOLD = 1500.0;

    @Override
    protected Optional<Host> defaultFindHostForVm(Vm vm) {
        List<Host> hostList = getHostList();
        String vmGrade = vm.getDescription();

        boolean preferHighPerf = "LARGE".equalsIgnoreCase(vmGrade) || "MEDIUM".equalsIgnoreCase(vmGrade);

        // 1. Cari host pada grade prioritas yang memenuhi seluruh syarat kapasitas (CPU, RAM, BW, Storage)
        Optional<Host> selectedHost = hostList.stream()
                .filter(h -> isHighPerformance(h) == preferHighPerf)
                .filter(h -> h.isSuitableForVm(vm))
                .findFirst();

        // 2. Fallback: jika host pada grade prioritas penuh, alokasikan ke host manapun yang masih sanggup
        if (selectedHost.isEmpty()) {
            selectedHost = hostList.stream()
                    .filter(h -> h.isSuitableForVm(vm))
                    .findFirst();
        }

        return selectedHost;
    }

    private boolean isHighPerformance(Host host) {
        return host.getMips() >= HIGH_PERF_THRESHOLD;
    }
}
