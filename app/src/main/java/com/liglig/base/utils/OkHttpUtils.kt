package com.liglig.base.utils

import android.graphics.Bitmap
import android.os.Handler
import android.text.TextUtils
import com.liglig.base.application.BaseApplication
import com.liglig.base.sp.SPUtils
import com.liglig.base.utils.LogUtils.e
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.Buffer
import okio.BufferedSink
import okio.Okio
import okio.source
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import kotlin.jvm.Throws

object OkHttpUtils {

    private val HEADER_NAME =
        arrayOf("Authorization", "language", "source")
    private var okHttpClient: OkHttpClient? = null
    private val handler = Handler()

    fun initOkHttp() {
        val builder = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(60, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
//        builder.addInterceptor(object :Interceptor {
//            override fun intercept(chain: Interceptor.Chain): Response {
//                val originalRequest = chain.request()
//                val authorised = originalRequest.newBuilder()
//                    .header(HEADER_NAME[0], SPUtils.getToken(BaseApplication.appContext)!!)
//                    .header(HEADER_NAME[1], SPUtils.getLanguage4Url(BaseApplication.appContext))
//                    .header(HEADER_NAME[2], "android")
//                    .build()
//                return chain.proceed(authorised)
//            }
//
//        })

        //处理https协议
        var sc: SSLContext?
        val tm = TrustAllManager()
        try {
            sc = SSLContext.getInstance("TLS")
            sc.init(null, arrayOf<TrustManager>(tm), SecureRandom())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            sc = null
        }
        okHttpClient = if (sc != null) {
            builder.sslSocketFactory(Tls12SocketFactory(sc.socketFactory), tm)
                .hostnameVerifier (HostnameVerifier { _, _ -> true })
                .build()
        } else {
            builder.hostnameVerifier (HostnameVerifier { _, _ -> true })
                .build()
        }
    }

    private class TrustAllManager : X509TrustManager {
        @Throws(CertificateException::class)
        override fun checkClientTrusted(
            chain: Array<X509Certificate?>?,
            authType: String?
        ) {
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(
            chain: Array<X509Certificate?>?,
            authType: String?
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate?>? {
            return arrayOfNulls(0)
        }
    }

    private class Tls12SocketFactory(val delegate: SSLSocketFactory) :
        SSLSocketFactory() {
        private val TLS_SUPPORT_VERSION =
            arrayOf("TLSv1", "TLSv1.1", "TLSv1.2")

        override fun getDefaultCipherSuites(): Array<String> {
            return delegate.defaultCipherSuites
        }

        override fun getSupportedCipherSuites(): Array<String> {
            return delegate.supportedCipherSuites
        }

        @Throws(IOException::class)
        override fun createSocket(
            s: Socket,
            host: String,
            port: Int,
            autoClose: Boolean
        ): Socket {
            return patch(delegate.createSocket(s, host, port, autoClose))
        }

        @Throws(IOException::class, UnknownHostException::class)
        override fun createSocket(host: String, port: Int): Socket {
            return patch(delegate.createSocket(host, port))
        }

        @Throws(IOException::class, UnknownHostException::class)
        override fun createSocket(
            host: String,
            port: Int,
            localHost: InetAddress,
            localPort: Int
        ): Socket {
            return patch(delegate.createSocket(host, port, localHost, localPort))
        }

        @Throws(IOException::class)
        override fun createSocket(host: InetAddress, port: Int): Socket {
            return patch(delegate.createSocket(host, port))
        }

        @Throws(IOException::class)
        override fun createSocket(
            address: InetAddress,
            port: Int,
            localAddress: InetAddress,
            localPort: Int
        ): Socket {
            return patch(delegate.createSocket(address, port, localAddress, localPort))
        }

        private fun patch(s: Socket): Socket {
            if (s is SSLSocket) {
                s.enabledProtocols = TLS_SUPPORT_VERSION
            }
            return s
        }

    }

    /**
     * 取消所有请求
     */
    fun cancel() {
        okHttpClient?.dispatcher?.cancelAll()
    }

    fun getStream(url: String, listener: OnBytesListener?) {
        val request = Request.Builder()
            .url(url)
            .build()
        val call = okHttpClient!!.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handler.post {
                    try {
                        if (listener != null) {
                            e(
                                """
                                    onFailure: $url
                                    ${e.message}
                                    """.trimIndent()
                            )
                            listener.onFailure(url, e.message)
                        }
                    } catch (e1: Exception) {
                        e("E: " + e1.message)
                    }
                }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body!!.bytes()
                handler.post {
                    listener?.onResponse(url, bytes)
                }
            }
        })
    }

    fun download(
        url: String,
        filePath: String,
        dataListener: OnDataListener?
    ) {
        val request = Request.Builder()
            .url(url)
            .build()
        okHttpClient!!.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFail(dataListener, url, e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val file = File(filePath)
                if (!file.parentFile.exists()) {
                    file.parentFile.mkdirs()
                }
                val `is` = response.body!!.byteStream()
                val os: OutputStream = FileOutputStream(file)
                val bs = ByteArray(1024)
                var len: Int
                while (`is`.read(bs).also { len = it } != -1) {
                    os.write(bs, 0, len)
                }
                `is`.close()
                os.close()
                onResp(dataListener, url, filePath, null)
            }
        })
    }

    /**
     * 下载文件
     */
    fun fileDownload(
        url: String, filePath: String?,
        progressListener: OnProgressListener?, dataListener: OnDataListener?
    ) {
        val request = Request.Builder()
            .url(url)
            .build()
        okHttpClient!!.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFail(dataListener, url, e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                val file = File(filePath)
                if (!file.parentFile.exists()) {
                    file.parentFile.mkdirs()
                }
                val `is` = response.body!!.byteStream()
                val length = response.body!!.contentLength()
                val os: OutputStream = FileOutputStream(file)
                val bs = ByteArray(1024 * 2)
                var len: Int
                var count: Long = 0
                while (`is`.read(bs).also { len = it } != -1) {
                    count += len.toLong()
                    os.write(bs, 0, len)
                    progressListener?.onProgress(0, (count * 100 / length).toInt())
                }
                `is`.close()
                os.close()
                onResp(dataListener, url, "suc", null)
            }
        })
    }

    /**
     * 同步get请求
     */
    @Throws(IOException::class)
    fun getJson(url: String?): String? {
        val request = Request.Builder()
            .url(url!!)
            .build()
        return okHttpClient!!.newCall(request).execute().body!!.string()
    }

    fun getJson(
        url: String,
        dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        getJsonKeyValue(url, dataListener, *kv as Array<out String>)
    }

    fun getJsonKeyValue(
        url: String,
        dataListener: OnDataListener?,
        vararg kv: String
    ) {
        if (TextUtils.isEmpty(url)) {
            e("getJsonKeyValue: 请求链接为空")
            return
        }
        if (kv.size % 2 != 0) { //键值对不匹配
            e("getJsonKeyValue: 请求参数不匹配")
            return
        }
        getJsonHeader(url + getKVJoint(kv as Array<String>), dataListener)
    }

    private fun getKVJoint(kv: Array<String>): String {
        if (kv.size == 0) {
            return ""
        }
        val sb = StringBuffer()
        for (i in 0 until kv.size / 2) {
            sb.append(if (i == 0) "?" else "&")
                .append(kv[i * 2])
                .append("=")
                .append(kv[i * 2 + 1])
        }
        return sb.toString()
    }

    /**
     * 携带多头部  -  GET
     */
    fun getJsonHeader(url: String, dataListener: OnDataListener?) {
        var url = url
        val builder = Request.Builder().url(url)
//        val builder = getRequestBuilderWithHeader().url(url)
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"))
        }
        okHttpClient!!.newCall(builder.build()).enqueue(OkHttpCallback(url, dataListener))
    }

    fun getRequestBuilderWithHeader(): Request.Builder {
        return Request.Builder()
            .addHeader(HEADER_NAME[0], SPUtils.getToken(BaseApplication.appContext)!!)
            .addHeader(HEADER_NAME[1], SPUtils.getLanguage4Url(BaseApplication.appContext))
            .addHeader(HEADER_NAME[2], "android")
    }

    fun postJson(url: String, json: String, dataListener: OnDataListener
    ) {
        //1.键值对时使用
//        FormBody body = new FormBody.Builder().add("InJson", json).build();

        //Body体时使用
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        postJsonSessionHeader(url, null, body, dataListener)
    }

    fun postJson(
        url: String,
        dataListener: OnDataListener,
        vararg kv: String?
    ) {
        postJsonTokenSession(url, null, null, dataListener, *kv)
    }

    fun postJsonToken(
        url: String,
        token: String?,
        dataListener: OnDataListener,
        vararg kv: String?
    ) {
        postJsonTokenSession(url, token, null, dataListener, *kv)
    }

    fun postJsonSession(
        url: String,
        session: String?,
        dataListener: OnDataListener,
        vararg kv: String?
    ) {
        postJsonTokenSession(url, null, session, dataListener, *kv)
    }

    fun postJsonTokenSession(
        url: String, token: String?, session: String?,
        dataListener: OnDataListener, vararg kv: String?
    ) {
        postJsonHeader2Session(url, token, null, session, dataListener, *kv as Array<out String>)
    }

    fun postJsonHeader2(
        url: String, header1: String?, header2: String?,
        dataListener: OnDataListener, vararg kv: String?
    ) {
        postJsonHeader2Session(url, header1, header2, null, dataListener, *kv as Array<out String>)
    }

    /**
     * 添加2Header
     */
    fun postJsonHeader2Session(
        url: String,
        header1: String?,
        header2: String?,
        session: String?,
        dataListener: OnDataListener,
        vararg kv: String
    ) {
        if (TextUtils.isEmpty(url)) {
            e("postJson: 请求链接为空")
            return
        }
        if (kv.size % 2 != 0) { //键值对不匹配
            e("postJson: 请求参数不匹配")
            return
        }
        postJsonSessionHeader(url, session, getKVBuild(kv as Array<String>), dataListener, header1, header2)
    }

    private fun getKVBuild(kv: Array<out String>): RequestBody? {
        if (kv.size == 0) {
            return null
        }
        val builder = FormBody.Builder()
        for (i in 0 until kv.size / 2) {
            builder.add(kv[i * 2], kv[i * 2 + 1])
            //            LogUtils.e( kv[i * 2] + ": " + kv[i * 2 + 1]);
        }
        return builder.build()
    }

    /**
     * 附带Session、携带多头部  -  POST
     */
    private fun postJsonSessionHeader(
        url: String, session: String?, body: RequestBody?,
        dataListener: OnDataListener, vararg header: String?
    ) {
        val builder = Request.Builder().url(url)
        if (header.isNotEmpty()) {
            for (i in header.indices) {
                if (!TextUtils.isEmpty(header[i])) {
                    header[i]?.let { builder.addHeader(HEADER_NAME[i], it) }
                }
            }
        }
        if (!TextUtils.isEmpty(session)) {
//            LogUtils.e( "session: " + session);
            if (session != null) {
                builder.addHeader("cookie", session)
            }
        }
        if (body != null) {
            builder.post(body)
        } else {
            builder.post(RequestBody.create(null, ""))
        }
        builder.addHeader("Connection", "close")
        okHttpClient!!.newCall(builder.build()).enqueue(OkHttpCallback(url, dataListener))
    }


    fun putJson(
        url: String,
        dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        putJsonToken(url, null, dataListener, *kv)
    }

    fun putJsonToken(
        url: String,
        token: String?,
        dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        putJsonHeader2Session(url, token, null, null, dataListener, *kv as Array<out String>)
    }

    /**
     * 添加2Header
     */
    fun putJsonHeader2Session(
        url: String,
        header1: String?,
        header2: String?,
        session: String?,
        dataListener: OnDataListener?,
        vararg kv: String
    ) {
        if (TextUtils.isEmpty(url)) {
            e("postJson: 请求链接为空")
            return
        }
        if (kv.size % 2 != 0) { //键值对不匹配
            e("postJson: 请求参数不匹配")
            return
        }
        putJsonSessionHeader(url, session, getKVBuild(kv as Array<String>), dataListener, header1!!, header2!!)
    }

    /**
     * 附带Session、携带多头部  -  PUT
     */
    private fun putJsonSessionHeader(
        url: String, session: String?, body: RequestBody?,
        dataListener: OnDataListener?, vararg header: String
    ) {
        /*Request.Builder builder = new Request.Builder().url(url);
        if (header.length > 0) {
            for (int i = 0; i < header.length; i++) {
                if (!TextUtils.isEmpty(header[i])) {
                    builder.addHeader(HEADER_NAME[i], header[i]);
                }
            }
        }*/
        val builder = Request.Builder().url(url)
//        val builder = getRequestBuilderWithHeader().url(url)
        if (!TextUtils.isEmpty(session)) {
//            LogUtils.e( "session: " + session);
            if (session != null) {
                builder.addHeader("cookie", session)
            }
        }
        if (body != null) {
            builder.put(body)
        } else {
            builder.put(RequestBody.create(null, ""))
        }
        builder.addHeader("Connection", "close")
        okHttpClient!!.newCall(builder.build()).enqueue(OkHttpCallback(url, dataListener))
    }


    fun deleteJsonToken(
        url: String,
        dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        deleteJsonHeader2Session(url, null, null, null, dataListener, *kv as Array<out String>)
    }


    fun deleteJsonToken(
        url: String,
        token: String?,
        dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        deleteJsonHeader2Session(url, token, null, null, dataListener, *kv as Array<out String>)
    }

    /**
     * 添加2Header
     */
    fun deleteJsonHeader2Session(
        url: String,
        header1: String?,
        header2: String?,
        session: String?,
        dataListener: OnDataListener?,
        vararg kv: String
    ) {
        if (TextUtils.isEmpty(url)) {
            e("postJson: 请求链接为空")
            return
        }
        if (kv.size % 2 != 0) { //键值对不匹配
            e("postJson: 请求参数不匹配")
            return
        }
        deleteJsonSessionHeader(
            url, session, getKVBuild(kv), dataListener,
            header1!!,
            header2!!
        )
    }

    /**
     * 附带Session、携带多头部  -  DELETE
     */
    private fun deleteJsonSessionHeader(
        url: String, session: String?, body: RequestBody?,
        dataListener: OnDataListener?, vararg header: String
    ) {
        val builder = Request.Builder().url(url)
        if (header.size > 0) {
            for (i in 0 until header.size) {
                if (!TextUtils.isEmpty(header[i])) {
                    builder.addHeader(HEADER_NAME[i], header[i])
                }
            }
        }
        if (!TextUtils.isEmpty(session)) {
//            LogUtils.e( "session: " + session);
            if (session != null) {
                builder.addHeader("cookie", session)
            }
        }
        if (body != null) {
            builder.delete(body)
        } else {
            builder.delete(RequestBody.create(null, ""))
        }
        builder.addHeader("Connection", "close")
        okHttpClient!!.newCall(builder.build()).enqueue(OkHttpCallback(url, dataListener))
    }


    /**
     * Socket长连接
     */
    fun socket(url: String?, socketListener: WebSocketListener?) {
        if (!TextUtils.isEmpty(url)) {
            val request = url?.let {
                Request.Builder()
                    .url(it)
                    .build()
            }
            if (socketListener != null && request != null) {
                okHttpClient!!.newWebSocket(request, socketListener)
            }
            //            okHttpClient.dispatcher().executorService().shutdown();//清除并关闭线程池
        }
    }


    /**
     * post提交Bitmap -- 不带进度条
     */
    fun postFORM(
        url: String?,
        token: String?,
        bitmap: Bitmap?,
        dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        if (bitmap == null) {
            return
        }
        postFORM(url!!, token!!, BitMapUtil.Bitmap2Bytes(bitmap)!!, dataListener, kv)
    }

    /**
     * post提交bytes -- 不带进度条
     */
    fun postFORM(
        url: String,
        token: String,
        bytes: ByteArray,
        dataListener: OnDataListener?,
        kv: Array<out String?>
    ) {
        val requestBody: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return MultipartBody.FORM
            }

            override fun contentLength(): Long {
                return bytes.size.toLong()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(bytes)
            }
        }
        createCall(url, token, requestBody, *kv as Array<out String>).enqueue(OkHttpCallback(url, dataListener))
    }

    /**
     * post上传File -- 不带参数 不带进度条
     */
    fun postFORM(
        url: String,
        token: String,
        file: File?,
        dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        if (file == null) {
            return
        }
        val requestBody = RequestBody.create(MultipartBody.FORM, file)
        createCall(url, token, requestBody, *kv as Array<out String>).enqueue(OkHttpCallback(url, dataListener))
    }

    /**
     * post上传Bitmap -- 带进度条
     */
    fun postFORM(
        url: String, token: String, index: Int, bitmap: Bitmap?,
        progressListener: OnProgressListener?, dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        if (bitmap == null) {
            return
        }
        val bytes: ByteArray? = BitMapUtil.Bitmap2Bytes(bitmap)
        if (bytes == null) {
            return
        }
        val requestBody: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return MultipartBody.FORM
            }

            override fun contentLength(): Long {
                return bytes.size.toLong()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val source = ByteArrayInputStream(bytes).source()
                var count = 0 //已上传长度
                var size: Int //每次循环上传长度
                val buffer = Buffer()
                while (true) {
                    size = source.read(buffer, 1024).toInt()
                    if (size < 0) {
                        break
                    } else {
                        sink.write(buffer, size.toLong())
                        count += size
                        if (progressListener != null) {
                            val rate = (count * 100.0 / contentLength()).toInt()
                            progressListener.onProgress(index, rate)
                        }
                    }
                }
            }
        }
        createCall(url, token, requestBody, *kv as Array<out String>).enqueue(OkHttpCallback(url, dataListener))
    }

    /**
     * post上传File -- 带进度条
     */
    fun postFORM(
        url: String, token: String, index: Int, file: File?,
        progressListener: OnProgressListener?, dataListener: OnDataListener?,
        vararg kv: String?
    ) {
        if (file == null) {
            return
        }
        val requestBody: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return MultipartBody.FORM
            }

            @Throws(IOException::class)
            override fun contentLength(): Long {
                return file.length()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val source = file.source()
                var count = 0 //已上传长度
                var size: Int //每次循环上传长度
                val buffer = Buffer()
                while (true) {
                    size = source.read(buffer, 1024).toInt()
                    if (size < 0) {
                        break
                    } else {
                        sink.write(buffer, size.toLong())
                        count += size
                        if (progressListener != null) {
                            val rate = (count * 100.0 / contentLength()).toInt()
                            progressListener.onProgress(index, rate)
                        }
                    }
                }
            }
        }
        createCall(url, token, requestBody, *kv as Array<out String>).enqueue(OkHttpCallback(url, dataListener))
    }

    /**
     * 创建Call对象 - 上传multipart
     */
    private fun createCall(
        url: String,
        token: String,
        requestBody: RequestBody,
        vararg kv: String
    ): Call {
        if (kv.size % 2 != 0) { //键值对不匹配
            throw RuntimeException("createCall: 请求参数不匹配")
        }
        val mb = MultipartBody.Builder().setType(MultipartBody.FORM)
        for (i in 0 until kv.size / 2) {
            mb.addFormDataPart(kv[i * 2], kv[i * 2 + 1])
        }
        val body = mb.addFormDataPart(
            "file",
            System.currentTimeMillis().toString() + ".jpg",
            requestBody
        )
            .build()
        val builder = Request.Builder().url(url)
        if (!TextUtils.isEmpty(token)) {
            builder.addHeader(HEADER_NAME[0], token)
        }
        return okHttpClient!!.newCall(builder.post(body).build())
    }

    /**
     * 结果回调
     */
    internal class OkHttpCallback(
        private val url: String,
        private val dataListener: OnDataListener?
    ) :
        Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFail(dataListener, url, e)
        }

        @Throws(IOException::class)
        override fun onResponse(
            call: Call,
            response: Response
        ) {
            onResp(
                dataListener,
                url,
                response.body!!.string(),
                response.request.headers.toString()
            )
        }

        private fun getSession(headers: Headers): String? {
            val s: String?
            s = try {
                val cookies = headers.values("Set-Cookie")
                val session = cookies[0]
                session.substring(0, session.indexOf(";"))
            } catch (e: Exception) {
                null
            }
            return s
        }

    }

    private fun onFail(
        dataListener: OnDataListener?,
        url: String,
        e: IOException
    ) {
        handler.post {
            try {
                if (dataListener != null) {
                    e(
                        """
                            onFailure: $url
                            ${e.message}
                            """.trimIndent()
                    )
                    dataListener.onFailure(url, e.message)
                }
            } catch (e1: Exception) {
                e("E: " + e1.message)
            }
        }
    }

    private fun onResp(
        dataListener: OnDataListener?,
        url: String,
        json: String,
        headers: String?
    ) {
        handler.post {
            try {
                if (dataListener != null) {
                    if (!TextUtils.isEmpty(json)) {
                        //                        LogUtils.e("url: " + url);
                        //                        LogUtils.e("json:" + json);
                        //                        LogUtils.e("headers:" + headers);
                        dataListener.onResponse(url, json, headers)
                    } else {
                        dataListener.onFailure(url, "Return json is EMPTY!!!")
                    }
                }
            } catch (e: Exception) {
                e(
                    """
                        E: ${e.message}
                        $json
                        """.trimIndent()
                )
            }
        }
    }


    interface OnDataListener {
        fun onResponse(
            url: String?,
            json: String?,
            session: String?
        )

        fun onFailure(url: String?, error: String?)
    }

    interface OnBytesListener {
        fun onResponse(url: String?, bytes: ByteArray?)
        fun onFailure(url: String?, error: String?)
    }

    /*public interface OnResponseListener {
        void onResponse(String url, ResponseBody body);
        void onFailure(String url, String error);
    }*/

    /*public interface OnResponseListener {
        void onResponse(String url, ResponseBody body);
        void onFailure(String url, String error);
    }*/
    interface OnProgressListener {
        fun onProgress(index: Int, rate: Int)
    }

}