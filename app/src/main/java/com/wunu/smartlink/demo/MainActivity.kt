package com.wunu.smartlink.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager

import com.afollestad.materialdialogs.MaterialDialog
import com.wunu.smartlink.demo.adapter.DeviceListAdapter
import com.clj.fastble.BleManager
import com.clj.fastble.scan.BleScanRuleConfig
import com.clj.fastble.data.BleDevice
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleScanState
import com.wunu.smartlink.sdk.utils.Hex
import com.yanzhenjie.permission.AndPermission
import com.yanzhenjie.permission.runtime.Permission
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    var adapter: DeviceListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setTitle(getString(R.string.lock_list))

        configureResultList()

        AndPermission.with(this)
            .runtime()
            .permission(Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_FINE_LOCATION)
            .onGranted {
                // Storage permission are allowed.
                initBleScan()

                startScan()
            }
            .onDenied {
                // Storage permission are not allowed.
                MaterialDialog(this).show {
                    title(R.string.notice)
                    message(R.string.ble_permission_not_granted)
                    positiveButton(R.string.ok, null) {
                        it.dismiss()
                    }
                }
            }
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        BleManager.getInstance().destroy();
    }

    private fun startScan() {
        Log.d("Main[startScan]", "enter")
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                // 开始扫描（主线程）
                supportActionBar?.setTitle(getString(R.string.searching))
                Log.d("Main[startScan]", "onScanStarted")
            }

            override fun onScanning(bleDevice: BleDevice) {
                // 扫描到一个符合扫描规则的BLE设备（主线程）
                adapter?.addScanResult(bleDevice)
                Log.d("Main[startScan]", "onScanning: ${bleDevice.name}, ad: ${Hex.encodeHex(bleDevice.getScanRecord()).substring(10, 14)}")
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
                // 扫描结束，列出所有扫描到的符合扫描规则的BLE设备（主线程）
                supportActionBar?.setTitle(getString(R.string.lock_list))
                Log.d("Main[startScan]", "onScanFinished")
            }
        })
    }

    private fun initBleScan() {
        Log.d("Main[initBleScan]", "enter")
        if (!envCheck()) return

        val scanRuleConfig = BleScanRuleConfig.Builder()
            .setServiceUuids(arrayOf(MyApp.SEVICE_UUID))      // 只扫描指定的服务的设备，可选
            .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒；小于等于0表示不限制扫描时间
            .build()
        BleManager.getInstance().initScanRule(scanRuleConfig)
    }

    private fun envCheck(): Boolean {
        if (!BleManager.getInstance().isSupportBle) {
            MaterialDialog(this).show {
                title(R.string.notice)
                message(R.string.ble_not_support)
                positiveButton(R.string.ok, null) {
                    it.dismiss()
                }
            }
            return false
        }

        if (!BleManager.getInstance().isBlueEnable) {
            MaterialDialog(this).show {
                title(R.string.notice)
                message(R.string.action_enable_ble)
                positiveButton(R.string.ok, null) {
                    BleManager.getInstance().enableBluetooth()
                    it.dismiss()
                }
            }
            return false
        }
        return true
    }

    private fun configureResultList() {
        scan_results?.setHasFixedSize(true)
        scan_results?.setItemAnimator(null)
        val recyclerLayoutManager = LinearLayoutManager(this)
        scan_results?.setLayoutManager(recyclerLayoutManager)
        adapter = DeviceListAdapter()
        scan_results?.setAdapter(adapter)
        scan_results?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter?.setOnAdapterItemClickListener { view ->
            val childAdapterPosition = scan_results?.getChildAdapterPosition(view)
            if (childAdapterPosition != null) {
                val itemAtPosition = adapter?.getItemAtPosition(childAdapterPosition)
                onAdapterItemClick(itemAtPosition!!)
            }
        }

        refreshLayout?.setOnRefreshListener {
            if (envCheck()) {
                if (BleManager.getInstance().scanSate == BleScanState.STATE_SCANNING) {
                    BleManager.getInstance().cancelScan()
                }
                adapter?.clearScanResults()
                startScan()
            }
            refreshLayout?.finishRefresh(2000)
        }
    }

    private fun onAdapterItemClick(device: BleDevice) {
        if(BleManager.getInstance().scanSate == BleScanState.STATE_SCANNING) {
            BleManager.getInstance().cancelScan()
        }

        val intent = Intent(this, LockDetailActivity::class.java)
        intent.putExtra(LockDetailActivity.EXTRA_MAC_ADDRESS, device.mac)
        intent.putExtra(LockDetailActivity.EXTRA_LOCK_NAME, device.name)
        startActivity(intent)
    }
}
