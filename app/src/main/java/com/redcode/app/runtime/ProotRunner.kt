package com.redcode.app.runtime

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProotRunner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ProotRunner"
    }

    val nativeLibDir: File
        get() = File(context.applicationInfo.nativeLibraryDir)

    val prootBinary: File
        get() = File(nativeLibDir, "libproot.so")

    val prootLoader: File
        get() = File(nativeLibDir, "libproot-loader.so")

    val rootfsDir: File
        get() = File(context.filesDir, "ubuntu")

    val tmpDir: File
        get() = File(context.filesDir, "tmp").apply { if (!exists()) mkdirs() }

    suspend fun verifyProot(): Result<String> = withContext(Dispatchers.IO) {
        val proot = prootBinary
        val loader = prootLoader
        if (!proot.exists()) {
            return@withContext Result.failure(IllegalStateException("libproot.so not found at ${proot.absolutePath}"))
        }
        if (!loader.exists()) {
            return@withContext Result.failure(IllegalStateException("libproot-loader.so not found at ${loader.absolutePath}"))
        }

        val pb = ProcessBuilder(proot.absolutePath, "--version")
        val env = pb.environment()
        env["LD_LIBRARY_PATH"] = nativeLibDir.absolutePath
        env["PROOT_LOADER"] = loader.absolutePath
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath
        pb.redirectErrorStream(true)

        try {
            val process = pb.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(IllegalStateException("PRoot self-check failed with exit code $exitCode: $output"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeScript(
        script: String,
        extraEnv: Map<String, String> = emptyMap(),
        onLogLine: (String) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val proot = prootBinary.absolutePath
        val loader = prootLoader.absolutePath
        val rootfs = rootfsDir.absolutePath

        val cmd = mutableListOf(
            proot,
            "--link2symlink",
            "-0",
            "-r", rootfs,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "/storage/emulated/0:/mnt/sdcard",
            "-b", "/sdcard:/sdcard",
            "-w", "/root",
            "/bin/bash", "-c", script
        )

        val pb = ProcessBuilder(cmd)
        val env = pb.environment()
        env["LD_LIBRARY_PATH"] = nativeLibDir.absolutePath
        env["PROOT_LOADER"] = loader
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath
        env["HOME"] = "/root"
        env["USER"] = "root"
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        env["TMPDIR"] = "/tmp"
        env["LANG"] = "C.UTF-8"
        extraEnv.forEach { (k, v) -> env[k] = v }

        pb.redirectErrorStream(true)

        try {
            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line = reader.readLine()
            while (line != null) {
                onLogLine(line)
                line = reader.readLine()
            }
            val exitCode = process.waitFor()
            Log.d(TAG, "Process exited with code $exitCode")
            exitCode
        } catch (e: Exception) {
            val err = "Proot execution failed: ${e.message}"
            Log.e(TAG, err, e)
            onLogLine(err)
            -1
        }
    }

    fun startProcess(
        command: List<String>,
        extraEnv: Map<String, String> = emptyMap()
    ): Process {
        val proot = prootBinary.absolutePath
        val loader = prootLoader.absolutePath
        val rootfs = rootfsDir.absolutePath

        val cmd = mutableListOf(
            proot,
            "--link2symlink",
            "-0",
            "-r", rootfs,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "/storage/emulated/0:/mnt/sdcard",
            "-b", "/sdcard:/sdcard",
            "-w", "/root"
        )
        cmd.addAll(command)

        val pb = ProcessBuilder(cmd)
        val env = pb.environment()
        env["LD_LIBRARY_PATH"] = nativeLibDir.absolutePath
        env["PROOT_LOADER"] = loader
        env["PROOT_TMP_DIR"] = tmpDir.absolutePath
        env["HOME"] = "/root"
        env["USER"] = "root"
        env["PATH"] = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
        env["TMPDIR"] = "/tmp"
        env["LANG"] = "C.UTF-8"
        extraEnv.forEach { (k, v) -> env[k] = v }

        pb.redirectErrorStream(true)
        return pb.start()
    }
}
