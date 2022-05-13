package com.liglig.base.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import com.liglig.base.CacheConfigData
import com.liglig.base.application.BaseApplication
import java.io.*
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.security.MessageDigest
import java.util.*
import kotlin.jvm.Throws


object AppUtils {

    /**
     * 判断当前是否是debug模式
     * Determine whether it is currently in debug mode
     */
    fun isApkInDebug(context: Context): Boolean {
        return try {
            val info: ApplicationInfo = context.getApplicationInfo()
            info.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 当前应用是否处于后台
     * Whether the current application is in the background
     */
    fun isAppBackground():Boolean{
        return BaseApplication.appBackground
    }

    /**
     * 获取设备唯一标识符
     *
     * @param context
     * @return
     */
    fun getDeviceId(context: Context): String? {
        //读取保存的在sd卡中的唯一标识符
        var deviceId = readDeviceID(context)
        //用于生成最终的唯一标识符
        val s = StringBuffer()
        //判断是否已经生成过,
        if (deviceId != null && "" != deviceId) {
            return deviceId
        }
        try {
            //获取IMES(也就是常说的DeviceId)
            deviceId = getIMIEStatus(context)
            s.append(deviceId)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        try {
            //获取设备的MACAddress地址 去掉中间相隔的冒号
            deviceId = getLocalMac(context).replace(":", "")
            s.append(deviceId)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        //        }

        //如果以上搜没有获取相应的则自己生成相应的UUID作为相应设备唯一标识符
        if (s == null || s.length <= 0) {
            val uuid = UUID.randomUUID()
            deviceId = uuid.toString().replace("-", "")
            s.append(deviceId)
        }
        //为了统一格式对设备的唯一标识进行md5加密 最终生成32位字符串
        val md5 = getMD5(s.toString(), false)
        if (s.length > 0) {
            //持久化操作, 进行保存到SD卡中
            saveDeviceID(md5, context)
        }
        return s.toString()
    }


    /**
     * 读取固定的文件中的内容,这里就是读取sd卡中保存的设备唯一标识符
     *
     * @param context
     * @return
     */
    fun readDeviceID(context: Context): String? {
        val file = getDevicesDir(context)
        val buffer = StringBuffer()
        return try {
            val fis = FileInputStream(file)
            val isr = InputStreamReader(fis, "UTF-8")
            val `in`: Reader = BufferedReader(isr)
            var i: Int
            while (`in`.read().also { i = it } > -1) {
                buffer.append(i.toChar())
            }
            `in`.close()
            buffer.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取设备的DeviceId(IMES) 这里需要相应的权限<br></br>
     * 需要 READ_PHONE_STATE 权限
     *
     * @param context
     * @return
     */
    private fun getIMIEStatus(context: Context): String? {
        val tm = context
            .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.deviceId
    }


    /**
     * 获取设备MAC 地址 由于 6.0 以后 WifiManager 得到的 MacAddress得到都是 相同的没有意义的内容
     * 所以采用以下方法获取Mac地址
     * @param context
     * @return
     */
    fun getLocalMac(context: Context?): String {
//        WifiManager wifi = (WifiManager) context
//                .getSystemService(Context.WIFI_SERVICE);
//        WifiInfo info = wifi.getConnectionInfo();
//        return info.getMacAddress();
        var macAddress: String? = null
        val buf = StringBuffer()
        var networkInterface: NetworkInterface? = null
        try {
            networkInterface = NetworkInterface.getByName("eth1")
            if (networkInterface == null) {
                networkInterface = NetworkInterface.getByName("wlan0")
            }
            if (networkInterface == null) {
                return ""
            }
            val addr = networkInterface.hardwareAddress
            for (b in addr) {
                buf.append(String.format("%02X:", b))
            }
            if (buf.length > 0) {
                buf.deleteCharAt(buf.length - 1)
            }
            macAddress = buf.toString()
        } catch (e: SocketException) {
            e.printStackTrace()
            return ""
        }
        return macAddress
    }

    /**
     * 保存 内容到 SD卡中,  这里保存的就是 设备唯一标识符
     * @param str
     * @param context
     */
    fun saveDeviceID(str: String?, context: Context) {
        val file = getDevicesDir(context)
        try {
            val fos = FileOutputStream(file)
            val out: Writer = OutputStreamWriter(fos, "UTF-8")
            out.write(str)
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 对挺特定的 内容进行 md5 加密
     * @param message  加密明文
     * @param upperCase  加密以后的字符串是是大写还是小写  true 大写  false 小写
     * @return
     */
    fun getMD5(message: String, upperCase: Boolean): String {
        var md5str = ""
        try {
            val md = MessageDigest.getInstance("MD5")
            val input = message.toByteArray()
            val buff = md.digest(input)
            md5str = bytesToHex(buff, upperCase)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return md5str
    }


    fun bytesToHex(bytes: ByteArray, upperCase: Boolean): String {
        val md5str = StringBuffer()
        var digital: Int
        for (i in bytes.indices) {
            digital = bytes[i].toInt()
            if (digital < 0) {
                digital += 256
            }
            if (digital < 16) {
                md5str.append("0")
            }
            md5str.append(Integer.toHexString(digital))
        }
        return if (upperCase) {
            md5str.toString().toUpperCase()
        } else md5str.toString().toLowerCase()
    }

    /**
     * 统一处理设备唯一标识 保存的文件的地址
     * @param context
     * @return
     */
    private fun getDevicesDir(context: Context): File {
        var mCropFile: File? = null
        mCropFile = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val cropdir =
                File(Environment.getExternalStorageDirectory(), CacheConfigData.CACHE_IMAGE_DIR)
            if (!cropdir.exists()) {
                cropdir.mkdirs()
            }
            File(cropdir, CacheConfigData.DEVICES_FILE_NAME) // 用当前时间给取得的图片命名
        } else {
            val cropdir = File(context.filesDir, CacheConfigData.CACHE_IMAGE_DIR)
            if (!cropdir.exists()) {
                cropdir.mkdirs()
            }
            File(cropdir, CacheConfigData.DEVICES_FILE_NAME)
        }
        return mCropFile
    }


    fun getMac(context: Context): String? {
        var strMac: String? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.e("=====", "6.0以下")
            strMac = getLocalMacAddressFromWifiInfo(context)
            return strMac
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.e("=====", "6.0以上7.0以下")
            strMac = getMacAddress(context)
            return strMac
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.e("=====", "7.0以上")
            if (!TextUtils.isEmpty(getMacAddress())) {
                strMac = getMacAddress(context)
                return strMac
            }

//            else if (!TextUtils.isEmpty(getMachineHardwareAddress())) {
//                Log.e("=====", "7.0以上2");
//                Toast.makeText(context, "7.0以上2", Toast.LENGTH_SHORT).show();
//                strMac = getMachineHardwareAddress(context);
//                return strMac;
//            } else {
//                Log.e("=====", "7.0以上3");
//                Toast.makeText(context, "7.0以上3", Toast.LENGTH_SHORT).show();
//                strMac = getLocalMacAddressFromBusybox(context);
//                return strMac;
//            }
        }
        return "02:00:00:00:00:00"
    }


    /**
     * 根据wifi信息获取本地mac
     * @param context
     * @return
     */
    fun getLocalMacAddressFromWifiInfo(context: Context): String? {
        val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val winfo = wifi.connectionInfo
        return winfo.macAddress
    }


    /**
     * android 6.0及以上、7.0以下 获取mac地址
     *
     * @param context
     * @return
     */
    fun getMacAddress(context: Context): String? {

        // 如果是6.0以下，直接通过wifimanager获取
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val macAddress0 = getMacAddress0(context)
            if (!TextUtils.isEmpty(macAddress0)) {
                return macAddress0
            }
        }
        var str = ""
        var macSerial = ""
        try {
            val pp = Runtime.getRuntime().exec(
                "cat /sys/class/net/wlan0/address")
            val ir = InputStreamReader(pp.inputStream)
            val input = LineNumberReader(ir)
            while (null != str) {
                str = input.readLine()
                if (str != null) {
                    macSerial = str.trim { it <= ' ' } // 去空格
                    break
                }
            }
        } catch (ex: java.lang.Exception) {
            Log.e("----->" + "NetInfoManager", "getMacAddress:$ex")
        }
        if (macSerial == null || "" == macSerial) {
            try {
                return loadFileAsString("/sys/class/net/eth0/address")
                    .toUpperCase().substring(0, 17)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                Log.e("----->" + "NetInfoManager",
                    "getMacAddress:$e")
            }
        }
        return macSerial
    }

    private fun getMacAddress0(context: Context): String {
        if (isAccessWifiStateAuthorized(context)) {
            val wifiMgr = context
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            var wifiInfo: WifiInfo? = null
            try {
                wifiInfo = wifiMgr.connectionInfo
                return wifiInfo.macAddress
            } catch (e: java.lang.Exception) {
                Log.e("----->" + "NetInfoManager",
                    "getMacAddress0:$e")
            }
        }
        return ""
    }

    /**
     * Check whether accessing wifi state is permitted
     *
     * @param context
     * @return
     */
    private fun isAccessWifiStateAuthorized(context: Context): Boolean {
        return if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE")) {
            Log.e("----->" + "NetInfoManager", "isAccessWifiStateAuthorized:"
                    + "access wifi state is enabled")
            true
        } else false
    }

    @Throws(java.lang.Exception::class)
    private fun loadFileAsString(fileName: String): String {
        val reader = FileReader(fileName)
        val text = loadReaderAsString(reader)
        reader.close()
        return text
    }

    @Throws(java.lang.Exception::class)
    private fun loadReaderAsString(reader: Reader): String {
        val builder = StringBuilder()
        val buffer = CharArray(4096)
        var readLength = reader.read(buffer)
        while (readLength >= 0) {
            builder.append(buffer, 0, readLength)
            readLength = reader.read(buffer)
        }
        return builder.toString()
    }


    /**
     * 根据IP地址获取MAC地址
     *
     * @return
     */
    fun getMacAddress(): String? {
        var strMacAddr: String? = null
        try {
            // 获得IpD地址
            val ip = getLocalInetAddress()
            val b = NetworkInterface.getByInetAddress(ip)
                .hardwareAddress
            val buffer = StringBuffer()
            for (i in b.indices) {
                if (i != 0) {
                    buffer.append(':')
                }
                val str = Integer.toHexString((b[i].toString() + 0xFF.toString()).toInt())
                buffer.append(if (str.length == 1) "0$str" else str)
            }
            strMacAddr = buffer.toString().toUpperCase()
        } catch (e: java.lang.Exception) {
        }
        return strMacAddr
    }

    /**
     * 获取移动设备本地IP
     *
     * @return
     */
    private fun getLocalInetAddress(): InetAddress? {
        var ip: InetAddress? = null
        try {
            // 列举
            val en_netInterface = NetworkInterface
                .getNetworkInterfaces()
            while (en_netInterface.hasMoreElements()) { // 是否还有元素
                val ni = en_netInterface
                    .nextElement() as NetworkInterface // 得到下一个元素
                val en_ip = ni.inetAddresses // 得到一个ip地址的列举
                while (en_ip.hasMoreElements()) {
                    ip = en_ip.nextElement()
                    ip = if (!ip.isLoopbackAddress
                        && ip.hostAddress.indexOf(":") == -1) break else null
                }
                if (ip != null) {
                    break
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return ip
    }

    /**
     * 获取本地IP
     *
     * @return
     */
    private fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface
                .getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf
                    .inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress) {
                        return inetAddress.hostAddress.toString()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }

}