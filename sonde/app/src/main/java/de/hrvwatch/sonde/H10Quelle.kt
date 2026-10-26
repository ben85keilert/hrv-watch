package de.hrvwatch.sonde

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import java.util.UUID

/**
 * Minimaler Leser für den Standard-Herzfrequenzdienst (0x180D, Messwert 0x2A37) mit
 * RR-Intervallen. Sucht das erste Gerät, das den Dienst bewirbt (Polar H10, H9, jeder
 * Standardgurt), verbindet, abonniert. Kein Bonding, kein PMD, Neuverbinden ohne Backoff.
 *
 * Rechte (BLUETOOTH_SCAN/CONNECT) fragt die Activity ab; hier wird nicht erneut geprüft.
 */
@SuppressLint("MissingPermission")
class H10Quelle(
    private val context: Context,
    private val log: (String) -> Unit,
    private val messwert: (hf: Int, rrMs: List<Double>) -> Unit,
) {
    companion object {
        val HR_DIENST: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HR_WERT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var gewollt = false

    fun verbinden() {
        if (adapter == null || !adapter.isEnabled) {
            log("Bluetooth ist aus")
            return
        }
        gewollt = true
        scanner = adapter.bluetoothLeScanner
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(HR_DIENST)).build()
        val einstellungen = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        log("suche Herzfrequenzdienst 0x180D …")
        scanner?.startScan(listOf(filter), einstellungen, scanRueckruf)
    }

    fun trennen() {
        gewollt = false
        try {
            scanner?.stopScan(scanRueckruf)
        } catch (_: Exception) {
        }
        gatt?.close()
        gatt = null
        log("H10 getrennt")
    }

    private val scanRueckruf = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: result.scanRecord?.deviceName ?: "unbenannt"
            log("gefunden: $name ${result.device.address}")
            scanner?.stopScan(this)
            gatt = result.device.connectGatt(context, false, gattRueckruf, BluetoothDevice.TRANSPORT_LE)
        }

        override fun onScanFailed(errorCode: Int) {
            log("Scan fehlgeschlagen: Code $errorCode")
        }
    }

    private val gattRueckruf = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    log("verbunden, suche Dienste")
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("Verbindung weg (Status $status)")
                    g.close()
                    gatt = null
                    if (gewollt) verbinden()
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val merkmal = g.getService(HR_DIENST)?.getCharacteristic(HR_WERT)
            if (merkmal == null) {
                log("kein 0x2A37 gefunden")
                return
            }
            g.setCharacteristicNotification(merkmal, true)
            val cccd = merkmal.getDescriptor(CCCD)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                g.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(cccd)
            }
            log("0x2A37 abonniert")
        }

        // Android 13+ ruft die Fassung mit value; ältere Versionen nur die alte.
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            merkmal: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            verarbeite(value)
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, merkmal: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                verarbeite(merkmal.value)
            }
        }
    }

    /**
     * Heart Rate Measurement (0x2A37): Flags, HF (8 oder 16 Bit), optional Energie (16 Bit),
     * dann RR-Intervalle in 1/1024 s, so viele wie ins Paket passen.
     */
    private fun verarbeite(b: ByteArray?) {
        if (b == null || b.isEmpty()) return
        val flags = b[0].toInt() and 0xFF
        var i = 1
        val hf: Int
        if ((flags and 0x01) != 0) {
            if (b.size < 3) return
            hf = (b[1].toInt() and 0xFF) or ((b[2].toInt() and 0xFF) shl 8)
            i = 3
        } else {
            if (b.size < 2) return
            hf = b[1].toInt() and 0xFF
            i = 2
        }
        if ((flags and 0x08) != 0) i += 2
        val rr = mutableListOf<Double>()
        if ((flags and 0x10) != 0) {
            while (i + 1 < b.size) {
                val roh = (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)
                rr.add(roh * 1000.0 / 1024.0)
                i += 2
            }
        }
        messwert(hf, rr)
    }
}
