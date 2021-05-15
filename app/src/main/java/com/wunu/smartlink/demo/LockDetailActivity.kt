package com.wunu.smartlink.demo

import android.os.Bundle
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleNotifyCallback
import com.wunu.smartlink.sdk.LockCmdManager
import com.clj.fastble.callback.BleWriteCallback
import com.wunu.smartlink.demo.util.WslLockUtil
import com.wunu.smartlink.sdk.ILockCmd
import com.wunu.smartlink.sdk.model.*
import kotlinx.android.synthetic.main.layout_lock.*
import java.util.*


class LockDetailActivity : AppCompatActivity() {

    companion object {
        val EXTRA_MAC_ADDRESS = "EXTRA_MAC_ADDRESS"
        val EXTRA_LOCK_NAME = "EXTRA_LOCK_NAME"
    }

    var macAddress: String? = null
    var lockName: String? = null
    var targetBleDevice: BleDevice? = null

    var lockCmdManager: LockCmdManager = LockCmdManager()

    var basecode: Int = 20947807
    var lockId = 1
    var lockManagerId = 1

    var lockModel: Int = 0
    var lockTaskId: Int = 0

    var pincode = -1
    var pincodeIndex = -1

    var rfCardId = -1
    var rfCardIndex = -1

    var fingerprintIndex = -1

    var isInitReady = false
    var isConnected = false
    var isLockLogin = false

    var isBindLock = false
    var isPincodeAdd = false
    var isRfCardAdd = false
    var isFingerprintAdd = false

    var isNbLock = false
    var isFpLock = false
    var isAlwaysOpen = false
    var isMuted = false

    var dialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_lock)

        macAddress = intent.getStringExtra(EXTRA_MAC_ADDRESS)
        lockName = intent.getStringExtra(EXTRA_LOCK_NAME)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = lockName

        lockModel = WslLockUtil.getLockModel(lockName)

        initLockUI()
        updateLockUI()

        val data = lockCmdManager.intSdk(
            this,
            "com.wunu.smartlink.demo",
            "d6e4aee6-5e99-4b9b-9d5f-4da02cdcd053"
        )
        Log.e("[data]", data.toString())

    }

    private fun initLockUI() {
        tv_dev_mac.text = getString(R.string.format_dev_mac, macAddress)
        tv_dev_type.text =
            getString(R.string.format_dev_type, WslLockUtil.getLockType(this, lockName!!))

        isNbLock = lockModel in 31..48 || lockModel in 81..88 || lockModel in 101..108
        isFpLock = lockModel in 71..88


        btn_bind_lock.setOnClickListener {
            lockTaskId = 11
            connectTo()
        }

        btn_unbind_lock.setOnClickListener {
            lockTaskId = 12
            connectTo()
        }

        btn_lock_state.setOnClickListener {
            lockTaskId = 21
            connectTo()
        }

        btn_lock_battery.setOnClickListener {
            lockTaskId = 22
            connectTo()
        }

        btn_nb_imei.setOnClickListener {
            lockTaskId = 31
            connectTo()
        }

        btn_lock_login.setOnClickListener {
            lockTaskId = 32
            connectTo()
        }

        btn_open_lock.setOnClickListener {
            lockTaskId = 41
            connectTo()
        }

        btn_sync_clock.setOnClickListener {
            lockTaskId = 42
            connectTo()
        }

        btn_add_pincode.setOnClickListener {
            lockTaskId = 51
            connectTo()
        }

        btn_del_pincode.setOnClickListener {
            lockTaskId = 52
            connectTo()
        }

        btn_add_rf_card.setOnClickListener {
            lockTaskId = 61
            connectTo()
        }

        btn_del_rf_card.setOnClickListener {
            lockTaskId = 62
            connectTo()
        }

        btn_add_fingerprint.setOnClickListener {
            lockTaskId = 71
            connectTo()
        }

        btn_del_fingerprint.setOnClickListener {
            lockTaskId = 72
            connectTo()
        }

        btn_change_admin_pincode.setOnClickListener {
            lockTaskId = 81
            connectTo()
        }

        btn_query_slot_state.setOnClickListener {
            lockTaskId = 91
            connectTo()
        }

        btn_query_lock_state.setOnClickListener {
            lockTaskId = 92
            connectTo()
        }

        btn_switch_lock_state.setOnClickListener {
            lockTaskId = 101
            connectTo()
        }

        btn_switch_mute_state.setOnClickListener {
            lockTaskId = 102
            connectTo()
        }

        btn_gen_offline_pincode.setOnClickListener {
            val timeStart = Date()
            val timeEnd = Date(timeStart.time + 300000)
            val data = lockCmdManager.genOfflinePincode(
                lockName!!,
                macAddress!!,
                basecode,
                0,
                timeStart,
                timeEnd
            )
            Log.e("[data]", data.toString())
            if (data.code == 200) {
                showMsg(
                    getString(
                        R.string.format_offline_pincode_succ,
                        (data.data as OfflinePincode).password
                    )
                )
            } else {
                showMsg(getString(R.string.format_offline_pincode_fail, data.data as String))
            }
        }
    }

    private fun updateLockUI() {
        btn_bind_lock.isEnabled = isInitReady && !isBindLock
        btn_unbind_lock.isEnabled = isBindLock

        btn_lock_state.isEnabled = isInitReady
        btn_lock_battery.isEnabled = isInitReady

        btn_nb_imei.isEnabled = isInitReady && isNbLock
        btn_lock_login.isEnabled = isBindLock && lockModel > 70

        btn_open_lock.isEnabled = isBindLock
        btn_sync_clock.isEnabled = isBindLock && (lockModel < 70 || isLockLogin)

        btn_add_pincode.isEnabled = isBindLock && !isPincodeAdd && (isLockLogin || lockModel < 70)
        btn_del_pincode.isEnabled = isBindLock && isPincodeAdd && (isLockLogin || lockModel < 70)

        btn_add_rf_card.isEnabled = isBindLock && !isRfCardAdd && (isLockLogin || lockModel < 70)
        btn_del_rf_card.isEnabled = isBindLock && isRfCardAdd && (isLockLogin || lockModel < 70)

        btn_add_fingerprint.isEnabled =
            isBindLock && lockModel > 70 && lockModel < 89 && !isFingerprintAdd && isLockLogin
        btn_del_fingerprint.isEnabled =
            isBindLock && lockModel > 70 && lockModel < 89 && isFingerprintAdd && isLockLogin

        btn_change_admin_pincode.isEnabled = isBindLock && lockModel > 70
        btn_gen_offline_pincode.isEnabled = isBindLock
        btn_switch_lock_state.text =
            getString(if (isAlwaysOpen) R.string.exit_always_open_mode else R.string.enter_always_open_mode)
        btn_switch_mute_state.text =
            getString(if (isMuted) R.string.exit_audio_muted_mode else R.string.enter_audio_muted_mode)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()

            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()

        connectTo()
    }

    override fun onStop() {
        super.onStop()
        if (targetBleDevice != null && BleManager.getInstance()
                .getConnectState(targetBleDevice!!) == BluetoothProfile.STATE_CONNECTED
        ) {
            BleManager.getInstance().disconnect(targetBleDevice)
        }
    }

    private fun connectTo() {
        if (isInitReady) {
            doWork()
            return
        }

        for (dev in BleManager.getInstance().allConnectedDevice.iterator()) {
            if (TextUtils.equals(dev.mac, macAddress)) {
                onLockConnected(dev)
                return
            }
        }

        BleManager.getInstance().connect(macAddress, object : BleGattCallback() {
            override fun onStartConnect() {
                // 开始连接
                supportActionBar?.title = lockName + " " + getString(R.string.progress_connecting)
            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                // 连接失败
                supportActionBar?.title = lockName + " " + getString(R.string.state_disconnected)
                if (dialog != null && dialog!!.isShowing)
                    dialog?.cancel()

                dialog = MaterialDialog(this@LockDetailActivity).show {
                    title(R.string.notice)
                    message(R.string.ble_connection_fail)
                    positiveButton(R.string.retry, null) {
                        it.dismiss()
                        connectTo()
                    }
                    negativeButton(R.string.exit, null) {
                        it.dismiss()
                        onBackPressed()
                    }
                }
            }

            override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
                // 连接成功，BleDevice即为所连接的BLE设备
                onLockConnected(bleDevice)
            }

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                bleDevice: BleDevice,
                gatt: BluetoothGatt,
                status: Int
            ) {
                isConnected = false
                isInitReady = false
                supportActionBar?.title = lockName + " " + getString(R.string.state_disconnected)
                if (!isActiveDisConnected) {
                    showMsg(getString(R.string.ble_connection_interrupted))
                }

            }
        })
    }

    private fun onLockConnected(bleDevice: BleDevice) {
        isConnected = true
        targetBleDevice = bleDevice
        supportActionBar?.title = lockName + " " + getString(R.string.state_connected)
        initConnection(bleDevice)
    }

    private fun showMsg(txt: String) {
        if (dialog != null && dialog!!.isShowing)
            dialog?.cancel()

        dialog = MaterialDialog(this@LockDetailActivity).show {
            title(R.string.notice)
            message(null, txt)
            positiveButton(R.string.ok, null) {
                it.dismiss()
            }
        }
    }

    fun initConnection(bleDevice: BleDevice) {
        BleManager.getInstance().notify(
            bleDevice,
            MyApp.SEVICE_UUID.toString(),
            MyApp.NOTIFY_UUID.toString(),
            object : BleNotifyCallback() {
                override fun onNotifySuccess() {
                    // 打开通知操作成功
                    isInitReady = true
                    supportActionBar?.title = lockName + " " + getString(R.string.state_ready)
                    onQueryBindState()
                }

                override fun onNotifyFailure(exception: BleException) {
                    // 打开通知操作失败
                }

                override fun onCharacteristicChanged(data: ByteArray) {
                    // 打开通知后，设备发过来的数据将在这里出现

                    val rsp = lockCmdManager.parseBytes(lockName!!, basecode, data)
                    when (rsp.cmd) {
                        "reportLockBattery" -> {
                            val lockBattery = rsp.data as LockBattery
                            if (lockBattery.battery < 10) {
                                showMsg(
                                    getString(
                                        R.string.format_lock_battery_low_warn,
                                        lockBattery.battery
                                    )
                                )
                            } else if (lockBattery.battery < 30) {
                                showMsg(
                                    getString(
                                        R.string.format_lock_battery_low_info,
                                        lockBattery.battery
                                    )
                                )
                            } else {
                                showMsg(getString(R.string.report_lock_battery))
                            }
                        }
                        "reportRfCardResult" -> {
                            if (lockModel > 70) {
                                val result = rsp.data as IndexUnlockResult
                                if (result.isValid) {
                                    showMsg(
                                        getString(
                                            R.string.format_rf_card_unlock_succ,
                                            result.index
                                        )
                                    )
                                } else {
                                    showMsg(getString(R.string.multi_rf_card_unlock_fail))
                                }
                            } else {
                                val result2 = rsp.data as RfCardUnlockResult
                                if (lockTaskId == 61) {
                                    if (dialog != null && dialog!!.isShowing)
                                        dialog?.cancel()

                                    dialog = MaterialDialog(this@LockDetailActivity).show {
                                        title(R.string.detect_new_rf_card)
                                        message(
                                            text = getString(
                                                R.string.format_add_rf_card_or_not,
                                                result2.cardId.toString(16)
                                            )
                                        )
                                        positiveButton(R.string.ok, null) {
                                            rfCardId = result2.cardId
                                            onAddRfCard()
                                            it.dismiss()
                                        }
                                    }
                                } else {
                                    if (result2.isValid) {
                                        showMsg(
                                            getString(
                                                R.string.format_rf_card_unlock_succ2,
                                                result2.cardId.toString(16)
                                            )
                                        )
                                    } else {
                                        showMsg(
                                            getString(
                                                R.string.multi_rf_card_unlock_fail2,
                                                result2.cardId.toString(16)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        "reportPincodeResult" -> {
                            if (lockModel > 70) {
                                val result = rsp.data as IndexUnlockResult
                                if (result.isValid) {
                                    showMsg(
                                        getString(
                                            R.string.format_pincode_unlock_succ,
                                            result.index
                                        )
                                    )
                                } else {
                                    showMsg(getString(R.string.multi_pincode_unlock_fail))
                                }
                            } else {
                                val result2 = rsp.data as PincodeUnlockResult
                                if (result2.isValid) {
                                    showMsg(
                                        getString(
                                            R.string.format_pincode_unlock_succ2,
                                            result2.pincode
                                        )
                                    )
                                } else {
                                    showMsg(
                                        getString(
                                            R.string.format_pincode_unlock_fail2,
                                            result2.pincode
                                        )
                                    )
                                }
                            }
                        }

                        "reportFingerprintResult" -> {
                            val result = rsp.data as IndexUnlockResult
                            if (result.isValid) {
                                showMsg(
                                    getString(
                                        R.string.format_fingerprint_unlock_succ,
                                        result.index
                                    )
                                )
                            } else {
                                showMsg(getString(R.string.multi_fingerprint_unlock_fail))
                            }
                        }

                        "queryLockState" -> {
                            if (rsp.code == 200) {
                                val lockState = rsp.data as LockState
                                isBindLock = lockState.bool
                                updateLockUI()
                                if (lockTaskId == 21) {
                                    showMsg(
                                        getString(
                                            R.string.format_lock_state,
                                            lockState.mac,
                                            lockState.bool
                                        )
                                    )
                                }
                            } else {
                                if (lockTaskId == 21) {
                                    showMsg(getString(R.string.cmd_rsp_fail))
                                }
                            }
                            lockTaskId = 0
                        }

                        "queryLockBattery" -> {
                            if (rsp.code == 200) {
                                val lockBattery = rsp.data as LockBattery
                                showMsg(
                                    getString(
                                        R.string.format_lock_battery,
                                        lockBattery.battery
                                    )
                                )
                            } else {
                                showMsg(getString(R.string.cmd_rsp_fail))
                            }
                            lockTaskId = 0
                        }

                        "queryNbImei" -> {
                            if (rsp.code == 200) {
                                val nbImei = rsp.data as NbImei
                                showMsg(getString(R.string.format_nb_imei, nbImei.imei))
                            } else {
                                showMsg(getString(R.string.cmd_rsp_fail))
                            }
                            lockTaskId = 0
                        }

                        "sendBindLock" -> {
                            if (rsp.code == 200) {
                                isBindLock = true
                                updateLockUI()
                                showMsg(getString(R.string.bind_lock_succ))
                            } else {
                                showMsg(getString(R.string.bind_lock_fail))
                            }
                            lockTaskId = 0
                        }

                        "sendUnbindLock" -> {
                            if (rsp.code == 200) {
                                isBindLock = false
                                updateLockUI()
                                showMsg(getString(R.string.unbind_lock_succ))
                            } else {
                                showMsg(getString(R.string.unbind_lock_fail))
                            }
                            lockTaskId = 0
                        }

                        "sendOpenLockP1" -> {
                            if (rsp.code == 200) {
                                if (lockTaskId == 32) { //登录
                                    writeBytes(
                                        targetBleDevice!!,
                                        lockCmdManager.login2(
                                            lockName!!,
                                            basecode,
                                            (rsp.data as RandomN).randomN
                                        )
                                    )
                                } else {  //开锁
                                    writeBytes(
                                        targetBleDevice!!,
                                        lockCmdManager.sendOpenLockP2(
                                            lockName!!,
                                            basecode,
                                            (rsp.data as RandomN).randomN
                                        )
                                    )
                                }
                            } else {
                                if (lockTaskId == 32) {
                                    showMsg(getString(R.string.lock_login_fail))
                                } else {
                                    showMsg(getString(R.string.open_lock_fail))
                                }
                                lockTaskId = 0
                            }
                        }

                        "login",
                        "sendOpenLockP2" -> {
                            if (rsp.code == 200) {
                                if (lockTaskId == 32) {
                                    showMsg(getString(R.string.lock_login_succ))
                                } else {
                                    showMsg(getString(R.string.open_lock__succ))
                                }
                                isLockLogin = true
                                updateLockUI()
                            } else {
                                if (lockTaskId == 32) {
                                    showMsg(getString(R.string.lock_login_fail))
                                } else {
                                    showMsg(getString(R.string.open_lock_fail))
                                }
                            }
                            lockTaskId = 0
                        }

                        "syncClock" -> {
                            val msgId = rsp.data as MsgId
                            if (rsp.code == 200) {
                                showMsg(getString(R.string.sync_clock_succ))
                            } else {
                                when (msgId.msgId) {
                                    ILockCmd.MSG_OPERATION_FAIL -> {
                                        showMsg(getString(R.string.sync_clock_fail))
                                    }
                                    ILockCmd.MSG_ERROR_LOGIN_STATE_OFF -> {
                                        showMsg(getString(R.string.error_login_state_off))
                                    }
                                    ILockCmd.MSG_ERROR_TIME_INVALID -> {
                                        showMsg(getString(R.string.error_time_invalid))
                                    }
                                }
                            }
                            lockTaskId = 0
                        }

                        "addPincode" -> {
                            val msgId = rsp.data as MsgId
                            if (rsp.code == 200) {
                                isPincodeAdd = true
                                updateLockUI()
                                showMsg(getString(R.string.add_pincode_succ))
                            } else {
                                when (msgId.msgId) {
                                    ILockCmd.MSG_OPERATION_FAIL -> {
                                        showMsg(getString(R.string.add_pincode_fail))
                                    }
                                    ILockCmd.MSG_ERROR_LOGIN_STATE_OFF -> {
                                        showMsg(getString(R.string.error_login_state_off))
                                    }
                                    ILockCmd.MSG_ERROR_TIME_INVALID -> {
                                        showMsg(getString(R.string.error_time_invalid))
                                    }
                                    ILockCmd.MSG_ERROR_INDEX_INVALID -> {
                                        showMsg(getString(R.string.error_index_invalid))
                                    }
                                }
                            }
                            lockTaskId = 0
                        }

                        "delPincode" -> {
                            if (rsp.code == 200) {
                                isPincodeAdd = false
                                updateLockUI()
                                showMsg(getString(R.string.del_pincode_succ))
                            } else {
                                showMsg(getString(R.string.del_pincode_fail))
                            }
                            lockTaskId = 0
                        }

                        "addRfCard" -> {
                            val msgId = rsp.data as MsgId
                            if (rsp.code == 200) {
                                isRfCardAdd = true
                                updateLockUI()
                                showMsg(getString(R.string.add_rf_card_succ))
                            } else {
                                when (msgId.msgId) {
                                    ILockCmd.MSG_OPERATION_FAIL -> {
                                        showMsg(getString(R.string.add_rf_card_fail))
                                    }
                                    ILockCmd.MSG_ERROR_LOGIN_STATE_OFF -> {
                                        showMsg(getString(R.string.error_login_state_off))
                                    }
                                    ILockCmd.MSG_ERROR_WAIT_TIMEOUT -> {
                                        showMsg(getString(R.string.error_wait_timeout))
                                    }
                                    ILockCmd.MSG_ERROR_OPERATION_CANCEL -> {
                                        showMsg(getString(R.string.error_user_cancel))
                                    }
                                    ILockCmd.MSG_ERROR_TIME_INVALID -> {
                                        showMsg(getString(R.string.error_time_invalid))
                                    }
                                    ILockCmd.MSG_ERROR_INDEX_INVALID -> {
                                        showMsg(getString(R.string.error_index_invalid))
                                    }
                                }
                            }
                            lockTaskId = 0
                        }

                        "delRfCard" -> {
                            if (rsp.code == 200) {
                                isRfCardAdd = false
                                updateLockUI()
                                showMsg(getString(R.string.del_rf_card_succ))
                            } else if (rsp.code == 100) {
                                val msgId = rsp.data as MsgId
                                if (msgId.msgId == ILockCmd.MSG_READY_TO_SWIPE_CARD) {
                                    showMsg(getString(R.string.ready_to_swipe_rf_card))
                                }
                            } else {
                                showMsg(getString(R.string.del_rf_card_fail))
                            }
                            lockTaskId = 0
                        }

                        "addFingerprint" -> {
                            val msgId = rsp.data as MsgId
                            if (rsp.code == 200) {
                                isFingerprintAdd = true
                                updateLockUI()
                                showMsg(getString(R.string.add_fingerprint_succ))
                            } else if (rsp.code == 100) {
                                when (msgId.msgId) {
                                    ILockCmd.MSG_READY_TO_PRESS_FINGER -> {
                                        showMsg(getString(R.string.ready_to_press_fingerprint))
                                    }
                                    ILockCmd.MSG_ACTION_PRESS_FINGER -> {
                                        showMsg(getString(R.string.action_to_press_fingerprint))
                                    }
                                    ILockCmd.MSG_ACTION_LEAVE_FINGER -> {
                                        showMsg(getString(R.string.action_to_leave_fingerprint))
                                    }
                                }
                            } else {
                                when (msgId.msgId) {
                                    ILockCmd.MSG_OPERATION_FAIL -> {
                                        showMsg(getString(R.string.add_fingerprint_fail))
                                    }
                                    ILockCmd.MSG_ERROR_LOGIN_STATE_OFF -> {
                                        showMsg(getString(R.string.error_login_state_off))
                                    }
                                    ILockCmd.MSG_ERROR_WAIT_TIMEOUT -> {
                                        showMsg(getString(R.string.error_wait_timeout))
                                    }
                                    ILockCmd.MSG_ERROR_OPERATION_CANCEL -> {
                                        showMsg(getString(R.string.error_user_cancel))
                                    }
                                    ILockCmd.MSG_ERROR_TIME_INVALID -> {
                                        showMsg(getString(R.string.error_time_invalid))
                                    }
                                    ILockCmd.MSG_ERROR_INDEX_INVALID -> {
                                        showMsg(getString(R.string.error_index_invalid))
                                    }
                                }
                            }
                            lockTaskId = 0
                        }

                        "delFingerprint" -> {
                            if (rsp.code == 200) {
                                isFingerprintAdd = false
                                updateLockUI()
                                showMsg(getString(R.string.del_fingerprint_succ))
                            } else {
                                showMsg(getString(R.string.del_fingerprint_fail))
                            }
                            lockTaskId = 0
                        }


                        "changeAdminPincode" -> {
                            if (rsp.code == 200) {
                                showMsg(getString(R.string.change_admin_pincode_succ))
                            } else {
                                showMsg(getString(R.string.change_admin_pincode_fail))
                            }
                            lockTaskId = 0
                        }

                        "queryLockSlotState" -> {
                            if (rsp.code == 200) {
                                val lockState = rsp.data as LockState
                                if (lockTaskId == 91) {
                                    showMsg(getString(if (lockState.bool) R.string.lock_slot_state_open else R.string.lock_slot_state_close))
                                }
                            } else {
                                if (lockTaskId == 91) {
                                    showMsg(getString(R.string.cmd_rsp_fail))
                                }
                            }
                            lockTaskId = 0
                        }

                        "queryLockUnlockState" -> {
                            if (rsp.code == 200) {
                                val lockState = rsp.data as LockState
                                if (lockTaskId == 92) {
                                    showMsg(getString(if (lockState.bool) R.string.lock_unlock_state_open else R.string.lock_unlock_state_close))
                                }
                            } else {
                                if (lockTaskId == 92) {
                                    showMsg(getString(R.string.cmd_rsp_fail))
                                }
                            }
                            lockTaskId = 0
                        }

                        "setLockUnlockState" -> {
                            when (rsp.code) {
                                200 -> {
                                    val lockState = rsp.data as LockState
                                    isAlwaysOpen = lockState.bool
                                    updateLockUI()
                                    showMsg(getString(if (lockState.bool) R.string.enter_always_open_mode else R.string.exit_always_open_mode))
                                }
                                300 -> {
                                    val msgId = rsp.data as MsgId
                                    when (msgId.msgId) {
                                        ILockCmd.MSG_OPERATION_FAIL -> {
                                            showMsg(getString(R.string.add_fingerprint_fail))
                                        }
                                        ILockCmd.MSG_ERROR_LOGIN_STATE_OFF -> {
                                            showMsg(getString(R.string.error_login_state_off))
                                        }
                                    }
                                }
                                else -> {
                                    showMsg(getString(R.string.cmd_rsp_fail))
                                }
                            }
                            lockTaskId = 0
                        }

                        "setLockMuteState" -> {
                            when (rsp.code) {
                                200 -> {
                                    val lockState = rsp.data as LockState
                                    isMuted = lockState.bool
                                    updateLockUI()
                                    showMsg(getString(if (lockState.bool) R.string.enter_audio_muted_mode else R.string.exit_audio_muted_mode))
                                }
                                300 -> {
                                    val msgId = rsp.data as MsgId
                                    when (msgId.msgId) {
                                        ILockCmd.MSG_OPERATION_FAIL -> {
                                            showMsg(getString(R.string.add_fingerprint_fail))
                                        }
                                        ILockCmd.MSG_ERROR_LOGIN_STATE_OFF -> {
                                            showMsg(getString(R.string.error_login_state_off))
                                        }
                                    }
                                }
                                else -> {
                                    showMsg(getString(R.string.cmd_rsp_fail))
                                }
                            }
                            lockTaskId = 0
                        }

                        else -> {

                        }
                    }
                }
            })
    }

    fun doWork() {
        Log.d("[doWork]", "lockTaskId = $lockTaskId")
        when (lockTaskId) {
            11 -> {
                onBindLock()
            }
            12 -> {
                onUnbindLock()
            }
            21 -> {
                onQueryBindState()
            }
            22 -> {
                onQueryBattery()
            }

            31 -> {
                onQueryNbImei()
            }
            32 -> {
                onLogin()
            }
            41 -> {
                onOpenLock()
            }
            42 -> {
                onSyncClock()
            }
            51 -> {
                onAddPincode()
            }
            52 -> {
                onDelPincode()
            }
            61 -> {
                if (lockModel > 70) {
                    onAddRfCard()
                } else {
                    showMsg(getString(R.string.ready_to_swipe_rf_card))
                }

            }
            62 -> {
                onDelRfCard()
            }
            71 -> {
                onAddFingerprint()
            }
            72 -> {
                onDelFingerprint()
            }
            81 -> {
                onChangeAdminPincode()
            }
            91 -> {
                onQueryLockSlotState()
            }
            92 -> {
                onQueryLockUnlockState()
            }
            101 -> {
                onChangeLockUnlockState()
            }
            102 -> {
                onChangeLockMutedState()
            }
            else -> {

            }
        }
    }

    private fun onChangeLockUnlockState() {
        writeBytes(targetBleDevice!!, lockCmdManager.setLockUnlockState(lockName!!, !isAlwaysOpen))
    }

    private fun onChangeLockMutedState() {
        writeBytes(targetBleDevice!!, lockCmdManager.setLockMuteState(lockName!!, !isMuted))
    }

    private fun onQueryLockSlotState() {
        writeBytes(targetBleDevice!!, lockCmdManager.queryLockSlotState(lockName!!))
    }

    private fun onQueryLockUnlockState() {
        writeBytes(targetBleDevice!!, lockCmdManager.queryLockUnlockState(lockName!!))
    }

    private fun onChangeAdminPincode() {
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.changeAdminPincode(lockName!!, macAddress!!, 12345678, 87654321)
        )
    }

    private fun onDelFingerprint() {
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.delFingerprint(lockName!!, basecode, fingerprintIndex)
        )
    }

    private fun onAddFingerprint() {
        fingerprintIndex = (Math.random() * 100).toInt()
        val timeStart = Date()
        val timeEnd = Date(timeStart.time + 300000)
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.addFingerprint(
                lockName!!,
                basecode,
                fingerprintIndex,
                timeStart,
                timeEnd
            )
        )
    }

    private fun onDelRfCard() {
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.delRfCard(lockName!!, basecode, rfCardId, rfCardIndex)
        )
    }

    private fun onAddRfCard() {
        rfCardIndex = (Math.random() * 100).toInt()
        val timeStart = Date()
        val timeEnd = Date(timeStart.time + 300000)
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.addRfCard(
                lockName!!,
                basecode,
                rfCardId,
                rfCardIndex,
                timeStart,
                timeEnd
            )
        )
    }

    private fun onDelPincode() {
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.delPincode(lockName!!, basecode, pincode, pincodeIndex)
        )
    }

    private fun onAddPincode() {
        if (dialog != null && dialog!!.isShowing)
            dialog?.cancel()

        dialog = MaterialDialog(this).show {
            input(inputType = InputType.TYPE_CLASS_NUMBER, maxLength = 6) { dialog, text ->
                pincode = text.toString().toInt()
                pincodeIndex = (Math.random() * 100).toInt()
                val timeStart = Date()
                val timeEnd = Date(timeStart.time + 300000)
                writeBytes(
                    targetBleDevice!!,
                    lockCmdManager.addPincode(
                        lockName!!,
                        basecode,
                        pincode,
                        pincodeIndex,
                        timeStart,
                        timeEnd
                    )
                )
            }
            positiveButton(R.string.ok)
        }
    }

    private fun onSyncClock() {
        writeBytes(targetBleDevice!!, lockCmdManager.syncClock(lockName!!, basecode, Date()))
    }

    private fun onOpenLock() {
        writeBytes(targetBleDevice!!, lockCmdManager.sendOpenLockP1(lockName!!, basecode))
    }

    private fun onLogin() {
        writeBytes(targetBleDevice!!, lockCmdManager.login1(lockName!!, basecode))
    }

    private fun onQueryNbImei() {
        writeBytes(targetBleDevice!!, lockCmdManager.queryNbImei(lockName!!))
    }

    private fun onQueryBattery() {
        writeBytes(targetBleDevice!!, lockCmdManager.queryLockBattery(lockName!!))
    }

    private fun onUnbindLock() {
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.sendUnbindLock(lockName!!, lockId, lockManagerId, basecode)
        )
    }

    private fun onBindLock() {
        writeBytes(
            targetBleDevice!!,
            lockCmdManager.sendBindLock(lockName!!, lockId, lockManagerId, basecode)
        )
    }

    private fun onQueryBindState() {
        writeBytes(targetBleDevice!!, lockCmdManager.queryLockState(lockName!!))
    }

    fun writeBytes(bleDevice: BleDevice, bytes: ByteArray) {
        BleManager.getInstance().write(
            bleDevice,
            MyApp.SEVICE_UUID.toString(),
            MyApp.WRITE_UUID.toString(),
            bytes,
            object : BleWriteCallback() {
                override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
                    // 发送数据到设备成功
                }

                override fun onWriteFailure(exception: BleException) {
                    // 发送数据到设备失败
                }
            })
    }
}