package com.prot.toex_app

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize osmdroid configuration
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = "com.prot.toex_app" // Replace with your package name or app name
        val basePath = File(cacheDir.absolutePath, "osmdroid")
        osmConfig.osmdroidBasePath = basePath
        val tileCache = File(osmConfig.osmdroidBasePath.absolutePath, "tile")
        osmConfig.osmdroidTileCache = tileCache
        // Optional: Set tile download threads, etc.
        // osmConfig.tileDownloadThreads = //...
        // osmConfig.tileFileSystemCacheMaxBytes = //...
    }
}