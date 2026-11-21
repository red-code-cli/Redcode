package com.redcode.app.runtime

import android.content.Context
import android.system.Os
import android.util.Log
import com.redcode.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

sealed class InstallProgress {
    data class Downloading(val percent: Int, val speedText: String, val etaText: String, val downloadedBytes: Long, val totalBytes: Long) : InstallProgress()
    data class Extracting(val currentFile: String, val percent: Int) : InstallProgress()
    data class InstallingDependencies(val log: String) : InstallProgress()
    data class StartingServer(val log: String) : InstallProgress()
    data class Success(val message: String) : InstallProgress()
    data class Error(val error: String, val canRetry: Boolean = true) : InstallProgress()
}

@Singleton
class UbuntuInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prootRunner: ProotRunner
) {
    companion object {
        private const val TAG = "UbuntuInstaller"
        const val MARKER_FILE_NAME = ".extracted_success"
        val MIRROR_URLS = listOf(
            BuildConfig.UBUNTU_ROOTFS_URL,
            "http://mirrors.kernel.org/ubuntu-cdimage/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.4-base-arm64.tar.gz",
            "https://mirror.math.princeton.edu/pub/ubuntu-iso/CDs/ubuntu-base/releases/22.04/release/ubuntu-base-22.04.4-base-arm64.tar.gz"
        )
        val EXPECTED_SHA256 = BuildConfig.UBUNTU_ROOTFS_SHA256
    }

    val ubuntuDir: File
        get() = File(context.filesDir, "ubuntu")

    val isRootfsInstalled: Boolean
        get() {
            val marker = File(ubuntuDir, MARKER_FILE_NAME)
            val bashUsr = File(ubuntuDir, "usr/bin/bash")
            val bashBin = File(ubuntuDir, "bin/bash")
            val valid = marker.exists() && (bashUsr.exists() || bashBin.exists())
            if (!valid && ubuntuDir.exists() && ubuntuDir.list()?.isNotEmpty() == true) {
                Log.w(TAG, "Ubuntu rootfs incomplete or corrupted, cleaning up...")
                ubuntuDir.deleteRecursively()
            }
            return valid
        }

    fun inspectRootfs(): List<String> {
        val info = mutableListOf<String>()
        info.add("Rootfs dir: ${ubuntuDir.absolutePath} (exists=${ubuntuDir.exists()})")
        if (!ubuntuDir.exists()) return info

        val binFile = File(ubuntuDir, "bin")
        val isBinSymlink = Files.isSymbolicLink(binFile.toPath())
        info.add("/bin: exists=${binFile.exists()}, isDirectory=${binFile.isDirectory}, isSymlink=$isBinSymlink")

        val usrBinBash = File(ubuntuDir, "usr/bin/bash")
        info.add("/usr/bin/bash: exists=${usrBinBash.exists()}, canExecute=${usrBinBash.canExecute()}, length=${usrBinBash.length()}")

        val binBash = File(ubuntuDir, "bin/bash")
        info.add("/bin/bash: exists=${binBash.exists()}, canExecute=${binBash.canExecute()}")

        val resolv = File(ubuntuDir, "etc/resolv.conf")
        info.add("/etc/resolv.conf: exists=${resolv.exists()}, size=${resolv.length()}")

        val marker = File(ubuntuDir, MARKER_FILE_NAME)
        info.add("$MARKER_FILE_NAME: exists=${marker.exists()}")

        val topDirs = ubuntuDir.listFiles()?.map { it.name } ?: emptyList()
        info.add("Top-level entries: $topDirs")
        return info
    }

    fun installUbuntu(): Flow<InstallProgress> = flow {
        val rootfsDir = ubuntuDir
        if (isRootfsInstalled) {
            emit(InstallProgress.Success("Ubuntu rootfs already installed"))
            return@flow
        }

        if (!rootfsDir.exists()) {
            rootfsDir.mkdirs()
        }

        val cacheArchive = File(context.cacheDir, "ubuntu-rootfs.tar.gz")

        // 1. Download RootFS with resume & mirrors
        var downloadSuccess = false
        var lastError = ""

        if (cacheArchive.exists() && verifySha256(cacheArchive, EXPECTED_SHA256)) {
            Log.d(TAG, "Cached rootfs archive is valid!")
            downloadSuccess = true
        } else {
            for (mirrorUrl in MIRROR_URLS) {
                try {
                    Log.d(TAG, "Attempting download from mirror: $mirrorUrl")
                    downloadFileWithResume(mirrorUrl, cacheArchive) { downloaded, total, speed, eta ->
                        val percent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                        // Progress callback
                    }
                    if (verifySha256(cacheArchive, EXPECTED_SHA256)) {
                        downloadSuccess = true
                        break
                    } else {
                        Log.w(TAG, "SHA256 mismatch for mirror $mirrorUrl")
                        cacheArchive.delete()
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: "Download failed"
                    Log.e(TAG, "Mirror failed: $mirrorUrl, err: $lastError")
                }
            }
        }

        if (!downloadSuccess) {
            emit(InstallProgress.Error("Failed to download rootfs: $lastError"))
            return@flow
        }

        // 2. Extract RootFS with true POSIX symlinks and hardlinks
        emit(InstallProgress.Extracting("Extracting Ubuntu rootfs...", 0))
        try {
            extractTarGz(cacheArchive, rootfsDir) { fileName, percent ->
                // emit extraction progress
            }
        } catch (e: Exception) {
            emit(InstallProgress.Error("Extraction failed: ${e.message}"))
            return@flow
        }

        // 3. Post-extract setup: DNS + Mount Points
        setupResolvConf(rootfsDir)
        setupMountPoints(rootfsDir)

        // 4. Write success marker file
        try {
            File(rootfsDir, MARKER_FILE_NAME).writeText("OK:${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write marker file", e)
        }

        emit(InstallProgress.Success("Ubuntu runtime extracted and configured!"))
    }.flowOn(Dispatchers.IO)

    private fun setupResolvConf(rootfsDir: File) {
        try {
            val etcDir = File(rootfsDir, "etc")
            if (!etcDir.exists()) etcDir.mkdirs()
            val resolvConf = File(etcDir, "resolv.conf")
            resolvConf.writeText("nameserver 8.8.8.8\nnameserver 1.1.1.1\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write resolv.conf", e)
        }
    }

    private fun setupMountPoints(rootfsDir: File) {
        try {
            val mntSdcard = File(rootfsDir, "mnt/sdcard")
            if (!mntSdcard.exists()) mntSdcard.mkdirs()
            val sdcard = File(rootfsDir, "sdcard")
            if (!sdcard.exists()) sdcard.mkdirs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create mount dirs", e)
        }
    }

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val calculated = digest.digest().joinToString("") { "%02x".format(it) }
            return calculated.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            return false
        }
    }

    private fun downloadFileWithResume(
        urlString: String,
        targetFile: File,
        onProgress: (downloaded: Long, total: Long, speed: String, eta: String) -> Unit
    ) {
        val existingLength = if (targetFile.exists()) targetFile.length() else 0L
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000

        if (existingLength > 0) {
            connection.setRequestProperty("Range", "bytes=$existingLength-")
        }

        connection.connect()
        val responseCode = connection.responseCode
        val isRangeSupported = responseCode == HttpURLConnection.HTTP_PARTIAL
        val totalLength = if (isRangeSupported) {
            existingLength + connection.contentLengthLong
        } else {
            connection.contentLengthLong
        }

        val raf = RandomAccessFile(targetFile, "rw")
        if (isRangeSupported) {
            raf.seek(existingLength)
        } else {
            raf.setLength(0)
            raf.seek(0)
        }

        val inputStream = connection.inputStream
        val buffer = ByteArray(16384)
        var bytesRead: Int
        var downloaded = if (isRangeSupported) existingLength else 0L
        var lastTime = System.currentTimeMillis()
        var lastDownloaded = downloaded

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                raf.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                val now = System.currentTimeMillis()
                if (now - lastTime >= 500) {
                    val bytesDiff = downloaded - lastDownloaded
                    val timeDiffSec = (now - lastTime) / 1000.0
                    val speedBytesSec = if (timeDiffSec > 0) (bytesDiff / timeDiffSec) else 0.0
                    val speedMbSec = speedBytesSec / (1024 * 1024)
                    val speedStr = "%.1f MB/s".format(speedMbSec)

                    val remainingBytes = totalLength - downloaded
                    val etaSec = if (speedBytesSec > 0) (remainingBytes / speedBytesSec).toInt() else 0
                    val etaStr = if (etaSec > 60) "${etaSec / 60}m ${etaSec % 60}s" else "${etaSec}s"

                    onProgress(downloaded, totalLength, speedStr, etaStr)
                    lastTime = now
                    lastDownloaded = downloaded
                }
            }
        } finally {
            inputStream.close()
            raf.close()
            connection.disconnect()
        }
    }

    private fun extractTarGz(
        tarGzFile: File,
        targetDir: File,
        onProgress: (fileName: String, percent: Int) -> Unit
    ) {
        val approxTotal = 15000 // typical files in ubuntu-base
        var count = 0
        FileInputStream(tarGzFile).use { fis ->
            GzipCompressorInputStream(fis).use { gzis ->
                TarArchiveInputStream(gzis).use { tais ->
                    var entry = tais.nextTarEntry
                    while (entry != null) {
                        val outFile = File(targetDir, entry.name)

                        if (entry.isDirectory) {
                            if (!outFile.exists()) outFile.mkdirs()
                        } else if (entry.isSymbolicLink) {
                            try {
                                if (outFile.exists() || Files.isSymbolicLink(outFile.toPath())) {
                                    outFile.delete()
                                }
                                outFile.parentFile?.mkdirs()
                                try {
                                    Os.symlink(entry.linkName, outFile.absolutePath)
                                } catch (_: Throwable) {
                                    Files.createSymbolicLink(outFile.toPath(), Paths.get(entry.linkName))
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Symlink error for ${entry.name} -> ${entry.linkName}: ${e.message}")
                            }
                        } else if (entry.isLink) {
                            try {
                                if (outFile.exists() || Files.isSymbolicLink(outFile.toPath())) {
                                    outFile.delete()
                                }
                                outFile.parentFile?.mkdirs()
                                val targetFile = File(targetDir, entry.linkName)
                                try {
                                    Os.link(targetFile.absolutePath, outFile.absolutePath)
                                } catch (_: Throwable) {
                                    Files.createLink(outFile.toPath(), targetFile.toPath())
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Hardlink error for ${entry.name} -> ${entry.linkName}: ${e.message}")
                            }
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos ->
                                tais.copyTo(fos)
                            }
                            // Set permissions
                            val mode = entry.mode and 0xFFF
                            if (mode != 0) {
                                try {
                                    Os.chmod(outFile.absolutePath, mode)
                                } catch (_: Throwable) {
                                    if (mode and 0x49 != 0) {
                                        outFile.setExecutable(true, false)
                                    }
                                }
                            }
                        }
                        count++
                        if (count % 250 == 0) {
                            val pct = (count * 100 / approxTotal).coerceAtMost(99)
                            onProgress(entry.name, pct)
                        }
                        entry = tais.nextTarEntry
                    }
                }
            }
        }
    }
}
