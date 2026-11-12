package com.seven.gamehub.runtime

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

internal data class DeviceRuntimeDiagnostics(
    val primaryAbi: String,
    val supportsArm64: Boolean,
    val hasVulkan: Boolean,
    val vulkanLevel: Int?,
    val vulkanVersion: Int?,
    val totalRamMb: Long
) {
    val summary: String
        get() = buildString {
            append(if (supportsArm64) "ARM64 поддерживается" else "ARM64 не обнаружен")
            append(" • ")
            append(if (hasVulkan) "Vulkan обнаружен" else "Vulkan не обнаружен")
            append(" • ")
            append("$totalRamMb МБ RAM")
        }
}

internal object RuntimeDiagnostics {
    fun inspect(context: Context): DeviceRuntimeDiagnostics {
        val pm = context.packageManager
        val abis = Build.SUPPORTED_ABIS.toList()
        val arm64 = abis.any { it.equals("arm64-v8a", ignoreCase = true) }

        val features = runCatching { pm.systemAvailableFeatures.orEmpty().associateBy { it.name } }
            .getOrDefault(emptyMap())

        val vulkanLevel = features[PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL]?.version
        val vulkanVersion = features[PackageManager.FEATURE_VULKAN_HARDWARE_VERSION]?.version
        val hasVulkan = vulkanLevel != null || vulkanVersion != null

        val memory = ActivityManager.MemoryInfo()
        runCatching {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .getMemoryInfo(memory)
        }
        val totalRamMb = if (memory.totalMem > 0L) memory.totalMem / (1024L * 1024L) else 0L

        return DeviceRuntimeDiagnostics(
            primaryAbi = abis.firstOrNull() ?: "Не определена",
            supportsArm64 = arm64,
            hasVulkan = hasVulkan,
            vulkanLevel = vulkanLevel,
            vulkanVersion = vulkanVersion,
            totalRamMb = totalRamMb
        )
    }
}
