package com.liglig.base.utils

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.*
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import java.io.*
import kotlin.jvm.Throws


object BitMapUtil {

    const val K128 = 128 * 1024L * 8
    const val K256 = K128 * 2
    const val K512 = K128 * 4
    const val K640 = K128 * 5
    const val K768 = K128 * 6
    const val M1 = K128 * 8
    const val M5 = K128 * 8 * 5


    /**
     * 图片二次采样 -- Uri
     */
    fun decodeBitmap(`is`: InputStream?, inSampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inSampleSize = inSampleSize
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        return BitmapFactory.decodeStream(`is`, null, options)
    }

    fun decodeBitmap(bitmapFile: String?, inSampleSize: Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true //表示只解码图片的边缘信息，用来得到图片的宽高
        BitmapFactory.decodeFile(bitmapFile, options)
        options.inSampleSize = inSampleSize
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeFile(bitmapFile, options)
    }

    fun decodeBitmap(bitmapFile: String?): Bitmap? {
        return decodeBitmap(bitmapFile, K512)
    }

    fun decodeBitmap(bitmapFile: String?, limit: Long): Bitmap? {
        if (TextUtils.isEmpty(bitmapFile)) {
            return null
        }
        val file = File(bitmapFile)
        if (!file.exists() || !file.isFile()) {
            return null
        }
        val len: Long = file.length() * 8
        return if (len > limit) {
            decodeBitmap(
                bitmapFile,
                Math.ceil(len * 1.0f / limit.toDouble()).toInt()
            )
        } else BitmapFactory.decodeFile(bitmapFile)
    }

    fun decodeBitmap(bitmap: Bitmap, inSampleSize: Int): Bitmap? {
        var inSampleSize = inSampleSize
        val oldWidth = bitmap.width
        val oldHeight = bitmap.height
        if (inSampleSize < 2) {
            inSampleSize = 2
        }
        return ThumbnailUtils.extractThumbnail(
            bitmap,
            oldWidth / inSampleSize,
            oldHeight / inSampleSize
        )
    }

    fun decodeBitmap(bitmap: Bitmap): Bitmap? {
        return decodeBitmap(bitmap, K512)
    }

    fun decodeBitmap4Big(bitmap: Bitmap): Bitmap? {
        return decodeBitmap(bitmap, M1)
    }


    /**
     * 获取View截图  -- 为避免操作被回收的Bitmap 此处不再调用setDrawingCacheEnabled(false)
     */
    fun getDrawingCache(v: View): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            v.getMeasuredWidth(),
            v.getMeasuredHeight(),
            Bitmap.Config.ARGB_8888
        )
        v.draw(Canvas(bitmap))
        return bitmap
    }

    /**
     * 保存View为文件
     */
    fun saveView2File(v: View, bm: Bitmap?, filePath: String?): Boolean {
        val draw: Bitmap?
        draw = try {
            getDrawingCache(v)
        } catch (e: Exception) {
            null
        }
        return saveBitmap2File(draw ?: bm, filePath)
    }

    /**
     * 保存View为文件
     */
    fun saveView2File(v: View, filePath: String?): Boolean {
        val draw = getDrawingCache(v)
        return saveBitmap2File(draw, filePath)
    }

    /**
     * 保存Bitmap为文件
     */
    fun saveBitmap2File(bitmap: Bitmap?, filePath: String?): Boolean {
        if (bitmap == null) {
            return false
        }
        val file = File(filePath)
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs()
        }
        return try {
            val bos = BufferedOutputStream(FileOutputStream(file))
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
            bos.flush()
            bos.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 图片加入到相册并通知刷新
     */
    fun insertGallery(context: Context, filePath: String) {
        try {
            if (TextUtils.isEmpty(filePath)) {
                return
            }
            val name: String
            name = if (filePath.contains("/")) {
                filePath.substring(filePath.lastIndexOf("/") + 1)
            } else {
                filePath
            }
            MediaStore.Images.Media.insertImage(
                context.getContentResolver(),
                filePath, name, null
            )
            context.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.parse("file://$filePath")
                )
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    /**
     * 图片二次采样 -- 裁剪成方形
     */
    fun cropSquare(bitmap: Bitmap?, scaleWidth: Int, scaleHeight: Int): Bitmap? {
        val min = if (scaleWidth < scaleHeight) scaleWidth else scaleHeight
        return ThumbnailUtils.extractThumbnail(bitmap, min, min)
    }

    /**
     * 图片二次采样 -- 裁剪成方形
     */
    fun cropSquare(bitmap: Bitmap): Bitmap? {
        return cropSquare(bitmap, bitmap.width, bitmap.height)
    }

    /**
     * 图片转Bitmap
     */
    fun getBitmap(bitmapFile: String?): Bitmap? {
        return BitmapFactory.decodeFile(bitmapFile)
    }

    /**
     * 图片转Bitmap
     */
    fun getBitmap(bitmapFile: String?, inSampleSize: Int): Bitmap? {
        return BitmapFactory.decodeFile(bitmapFile, getBitmapOption(inSampleSize))
    }

    /**
     * 获取bitmap大小
     */
    fun getBitmapSize(bitmap: Bitmap): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {    //API 19
            bitmap.allocationByteCount
        } else bitmap.rowBytes * bitmap.height
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1){//API 12
            return bitmap.getByteCount();
        }*/
    }

    /**
     * 获取圆形图片-会自动裁剪多余部分
     */
    fun getCircleBitmap(bitmap: Bitmap): Bitmap? {
        val p = Paint()
        p.setAntiAlias(true)
        val w = Math.min(bitmap.width, bitmap.height)
        val tar = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888)
        val c = Canvas(tar)
        c.drawCircle((w / 2).toFloat(), (w / 2).toFloat(), (w / 2).toFloat(), p)
        p.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        c.drawBitmap(bitmap, 0.toFloat(), 0.toFloat(), p)
        return tar
    }

    /**
     * Bitmap转byte[]
     */
    @JvmStatic
    fun Bitmap2Bytes(bm: Bitmap?): ByteArray? {
        if (bm == null) {
            return null
        }
        val bas = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, bas)
        return bas.toByteArray()
    }

    /**
     * byte[]转Bitmap
     */
    fun Bytes2Bitmap(b: ByteArray): Bitmap? {
        return if (b.size == 0) {
            null
        } else BitmapFactory.decodeByteArray(b, 0, b.size)
    }

    fun Bitmap2IS(bm: Bitmap?): InputStream? {
        if (bm == null) {
            return null
        }
        val baos: ByteArrayOutputStream? = Bitmap2BAOS(bm)
        if (baos != null) {
            return ByteArrayInputStream(baos.toByteArray())
        }else{
            return null
        }
    }

    fun Bitmap2BAOS(bm: Bitmap?): ByteArrayOutputStream? {
        if (bm == null) {
            return null
        }
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos
    }

    fun decodeBitmap(bm: Bitmap, limit: Long): Bitmap? {
        val size = getBitmapSize(bm)
        return if (size > limit) {
            decodeBitmap(bm, Math.ceil(size * 1.0f / limit.toDouble()).toInt())
        } else {
            bm
        }
    }

    /**
     * Uri转Bitmap
     */
    fun Uri2Bitmap(context: Context, uri: Uri?): Bitmap? {
        return Uri2Bitmap(context, uri, K512)
    }

    fun Uri2Bitmap4Big(context: Context, uri: Uri?): Bitmap? {
        return Uri2Bitmap(context, uri, M1)
    }

    fun Uri2Bitmap(context: Context, uri: Uri?, limit: Long): Bitmap? {
        return try {
            val `is`: InputStream = context.getContentResolver().openInputStream(uri!!) ?: return null
            val len: Long = `is`.available() * 8L
            if (len > limit) {
                val inSampleSize = (len / limit).toInt() + 1
                return decodeBitmap(`is`, inSampleSize)
            }
            decodeBitmap(BitmapFactory.decodeStream(`is`), limit)
        } catch (e: Exception) {
            e.printStackTrace()
            val path = getPath(context, uri)
            val file = File(path)
            val len = limit / 8
            if (file.exists() && file.isFile() && file.length() > len) {
                getBitmap(path, (file.length() / len + 1) as Int)
            } else getBitmap(path)
        }
    }

    fun Uri2BitmapFLAC(context: Context, uri: Uri?): Bitmap? {
        return try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri!!))
        } catch (e: Exception) {
            e.printStackTrace()
            getBitmap(getPath(context, uri))
        }
    }

    /**
     * 异常情况下的备选方案
     */
    private fun getPath(context: Context, uri: Uri?): String {
        val projection =
            arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor = context.getContentResolver().query(uri!!, projection, null, null, null)!!
        val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(column_index)
    }

    private fun getBitmapOption(inSampleSize: Int): BitmapFactory.Options? {
        System.gc()
        val options = BitmapFactory.Options()
        options.inPurgeable = true
        options.inSampleSize = inSampleSize
        return options
    }


    /**
     * 对图片进行压缩处理
     */
    @Throws(IOException::class)
    fun compressImage(path: String?): Bitmap? {
        var `in` = BufferedInputStream(
            FileInputStream(
                File(path)
            )
        )
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(`in`, null, options)
        `in`.close()
        var i = 0
        var bitmap: Bitmap? = null
        while (true) {
            if (options.outWidth shr i <= 1000
                && options.outHeight shr i <= 1000
            ) {
                `in` = BufferedInputStream(
                    FileInputStream(File(path))
                )
                options.inSampleSize = Math.pow(2.0, i.toDouble()).toInt()
                options.inJustDecodeBounds = false
                bitmap = BitmapFactory.decodeStream(`in`, null, options)
                break
            }
            i += 1
        }
        return bitmap
    }

}