package com.wunu.smartlink.demo

import android.app.Application
import com.clj.fastble.BleManager
import java.util.*

class MyApp : Application() {

    companion object{
        val SEVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val WRITE_UUID: UUID = UUID.fromString("0000fff6-0000-1000-8000-00805f9b34fb")
        val NOTIFY_UUID: UUID = UUID.fromString("0000fff7-0000-1000-8000-00805f9b34fb")
    }

    override fun onCreate() {
        super.onCreate()

        BleManager.getInstance().init(this)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000)
            .setOperateTimeout(5000)
    }
}