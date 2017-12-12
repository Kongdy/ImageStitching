package com.kongdy.imagestitchinglib.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * @author kongdy
 * @date 2017/12/9 16:26
 * @describe bitmap tools class
 **/
public class BitmapUtils {

    /**
     * 缩放matrix
     *
     * @param origin 原图
     * @return 偏移后的matrix
     */
    public static Matrix resetBitmapSize(Bitmap origin,Matrix matrix , int width, int height) {
        if (origin == null) {
            return null;
        }
        int orgWidth = origin.getWidth();
        int orgHeight = origin.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) width) / orgWidth;
        float scaleHeight = ((float) height) / orgHeight;
        matrix.postScale(scaleWidth, scaleHeight);
        return matrix;
    }

    /**
     * 位移matrix
     * @param matrix 矩阵
     * @param translateX x位移量
     * @param translateY y位移量
     * @return
     */
    public static Matrix translateBitmap(Matrix matrix,int translateX,int translateY)
    {
        matrix.postTranslate(translateX,translateY);
        return matrix;
    }
}
