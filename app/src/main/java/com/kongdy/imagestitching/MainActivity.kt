package com.kongdy.imagestitching

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import com.kongdy.imagestitchinglib.view.ImageStitchingView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(),ImageStitchingView.OnGenerateBitmapListener {

    override fun onError(t: Throwable?) {
        Toast.makeText(this,t.toString(),Toast.LENGTH_LONG).show()
    }

    override fun onResourceReady(bitmap: Bitmap?) {
        var file = File(Environment.getExternalStorageDirectory(),"kongdyFile")
        if(!file.exists())
            file.mkdirs()

        var fileName = "result.png"
        var imgFie = File(file,fileName)
        if(!imgFie.exists())
            imgFie.createNewFile()

        var fos = FileOutputStream(imgFie)
        bitmap?.compress(Bitmap.CompressFormat.PNG,90,fos)
        fos.flush()
        fos.close()
        Toast.makeText(this,"已保存在:"+imgFie.absolutePath,Toast.LENGTH_LONG).show()
    }

    private var bitmapList = arrayListOf<Bitmap>()
    private var addCursor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bm1 = BitmapFactory.decodeResource(resources, R.mipmap.img_1)
        val bm2 = BitmapFactory.decodeResource(resources, R.mipmap.img_2)
        val bm3 = BitmapFactory.decodeResource(resources, R.mipmap.img_3)
        val bm4 = BitmapFactory.decodeResource(resources, R.mipmap.img_4)

        bitmapList.add(bm1)
        bitmapList.add(bm2)
        bitmapList.add(bm3)
        bitmapList.add(bm4)

        isv_test.onGenerateBitmapListener = this
    }

    fun fabClick(v: View) {
        when (v.id) {
            R.id.fab_rotate_1 -> {
                isv_test.rotateImage(50f, 0)
            }
            R.id.fab_add -> {
                addBitmap()
            }
            R.id.fab_zoom_in -> {
                isv_test.scaleImage(2f, 0)
            }
            R.id.fab_zoom_out -> {
                isv_test.scaleImage(0.5f, 0)
            }
            R.id.fab_generate -> {
                isv_test.generateBitmap()
            }
        }
    }

    private fun addBitmap() {
        isv_test.addImage(bitmapList[addCursor++ % bitmapList.size])
    }
}
