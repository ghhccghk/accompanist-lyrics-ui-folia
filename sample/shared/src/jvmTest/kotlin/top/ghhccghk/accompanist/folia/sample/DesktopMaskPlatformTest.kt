package top.ghhccghk.accompanist.folia.sample

import kotlin.test.*

class DesktopMaskPlatformTest {
    @Test fun supportedHostResourcesUseNativeNamesAndCpuArchitecture() {
        for ((os, arch, path) in listOf(
            Triple("Windows 11","amd64","windows-x86_64/folia_mask_experiment.dll"),
            Triple("Linux","x86_64","linux-x86_64/libfolia_mask_experiment.so"),
            Triple("Linux","aarch64","linux-aarch64/libfolia_mask_experiment.so"),
            Triple("Mac OS X","x86_64","macos-x86_64/libfolia_mask_experiment.dylib"),
            Triple("Mac OS X","arm64","macos-aarch64/libfolia_mask_experiment.dylib")
        )) assertEquals("/native/$path",desktopMaskPlatform(os,arch)?.resourcePath)
        assertNull(desktopMaskPlatform("Windows 11","arm64"))
        assertNull(desktopMaskPlatform("Linux","x86"))
        assertNull(desktopMaskPlatform("Unknown","amd64"))
    }
}
