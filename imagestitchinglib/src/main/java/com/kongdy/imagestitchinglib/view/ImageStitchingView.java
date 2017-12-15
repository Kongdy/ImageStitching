package com.kongdy.imagestitchinglib.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * @author kongdy
 * @date 2017/12/9 12:14
 * @describe image stitching view
 **/
public class ImageStitchingView extends View {

    private final static long DEFAULT_ANIMATION_TIME = 300L;
    private final static int BITMAP_GENERATE_RESULT = 0x000001;
    private final static int BITMAP_GENERATE_ERROR = 0x000002;
    private final static String BITMAP_ERROR = "bitmapError";
    /**
     * 正在执行动画
     */
    private final static int VIEW_MODE_RUN_ANIMATION = 0x000003;
    /**
     * 正在控制图片
     */
    private final static int VIEW_MODE_ON_CONTROL_IMG = 0x000004;
    /**
     * 空闲状态
     */
    private final static int VIEW_MODE_IDLE = 0x000005;

    private Paint stitchingPaint;
    private Paint framePaint;

    private Rect clipScope = new Rect();

    private List<ImageData> imgList = new ArrayList<>();
    private Bitmap outputBitmap;

    private OnGenerateBitmapListener onGenerateBitmapListener;
    // 触摸是否在对视图进行控制
    private int mViewMode = VIEW_MODE_IDLE;

    private int findIndex = -1;

    private Thread handleBitmapThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                outputBitmap = Bitmap.createBitmap(getMeasuredWidth(),
                        getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(outputBitmap);
                draw(canvas);
                generateBitmapHandler.sendEmptyMessage(BITMAP_GENERATE_RESULT);
            } catch (Exception e) {
                // 扔到主线程抛出
                Message message = new Message();
                message.what = BITMAP_GENERATE_ERROR;
                Bundle bundle = new Bundle();
                bundle.putSerializable(BITMAP_ERROR, e);
                message.setData(bundle);
                generateBitmapHandler.sendMessage(message);
            }
        }
    });

    private Handler generateBitmapHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case BITMAP_GENERATE_RESULT:
                    if (null != onGenerateBitmapListener)
                        onGenerateBitmapListener.onResourceReady(outputBitmap);
                    ImageStitchingView.this.setDrawingCacheEnabled(false);
                    break;
                case BITMAP_GENERATE_ERROR:
                    if (null != onGenerateBitmapListener) {
                        Bundle bundle = msg.getData();
                        Exception e = (Exception) bundle.getSerializable(BITMAP_ERROR);
                        onGenerateBitmapListener.onError(e);
                    }
                    ImageStitchingView.this.setDrawingCacheEnabled(false);
                    break;
            }
            return false;
        }
    });

    public ImageStitchingView(Context context) {
        this(context, null);
    }

    public ImageStitchingView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ImageStitchingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initProperty();
        applyAttr(attrs);
    }

    private void initProperty() {
        stitchingPaint = new Paint();
        framePaint = new Paint();

        framePaint.setStyle(Paint.Style.FILL_AND_STROKE);

        initPaintProperty(stitchingPaint);
        initPaintProperty(framePaint);

    }

    private void applyAttr(AttributeSet attrs) {
    }

    private void initPaintProperty(Paint paint) {
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setFilterBitmap(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // measure child img
        final int maxImgWidth = getMeasuredWidth();
        final int maxImgHeight = getMeasuredHeight();
        final int measureWidthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int measureHeightSize = MeasureSpec.getSize(heightMeasureSpec);
        int totalImageHeight = 0;
        // 缩放和旋转影响size的交给measure
        for (int i = 0; i < imgList.size(); i++) {
            ImageData imageData = imgList.get(i);
            final int imgOrgWidth = imageData.getImgWidth();
            final int imgOrgHeight = imageData.getImgHeight();
            int imgRotateWidth;
            int imgRotateHeight;
            if (imageData.scale > 0) {
                imageData.matrix.setScale(imageData.scale, imageData.scale);
            } else {
                final float sizeProportion = (float) imgOrgWidth / imgOrgHeight;
                if (imgOrgHeight > imgOrgWidth) {
                    if (measureHeightSize == MeasureSpec.EXACTLY &&
                            imgOrgHeight > maxImgHeight) {
                        imgRotateWidth = (int) (maxImgHeight * sizeProportion);
                        imgRotateHeight = maxImgHeight;
                    } else {
                        imgRotateWidth = imgOrgWidth;
                        imgRotateHeight = imgOrgHeight;
                    }
                } else {
                    if (imgOrgWidth > maxImgWidth) {
                        imgRotateHeight = (int) (maxImgWidth / sizeProportion);
                        imgRotateWidth = maxImgWidth;
                    } else {
                        imgRotateWidth = imgOrgWidth;
                        imgRotateHeight = imgOrgHeight;
                    }
                }

                // resize
                imageData.reSize(imgRotateWidth, imgRotateHeight);
            }

            // rotate
            imageData.matrix.mapRect(imageData.drawRect, imageData.orgRect);
            imageData.matrix.postRotate(imageData.rotateAngle, imageData.drawRect.centerX(),
                    imageData.drawRect.top + (imageData.drawRect.height() * 0.5f));

            imageData.matrix.mapRect(imageData.drawRect, imageData.orgRect);
            totalImageHeight += imageData.drawRect.height();
        }
        switch (measureHeightSize) {
            // wrap_content
            case MeasureSpec.AT_MOST:
                setMeasuredDimension(MeasureSpec.makeMeasureSpec(maxImgWidth,
                        measureWidthSize), MeasureSpec.makeMeasureSpec(totalImageHeight, measureHeightSize));
                break;
            // match_parent or accurate num
            case MeasureSpec.EXACTLY:
                setMeasuredDimension(MeasureSpec.makeMeasureSpec(maxImgWidth,
                        measureWidthSize), MeasureSpec.makeMeasureSpec(maxImgHeight, measureHeightSize));
                break;
            case MeasureSpec.UNSPECIFIED:
                setMeasuredDimension(MeasureSpec.makeMeasureSpec(maxImgWidth,
                        measureWidthSize), MeasureSpec.makeMeasureSpec(totalImageHeight, measureHeightSize));
                break;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // measure child layout
        int cursorTop = top;
        int mid = (right - left) >> 1;
        for (int i = 0; i < imgList.size(); i++) {
            final ImageData imageData = imgList.get(i);

            // fix layout translate
            imageData.matrix.mapRect(imageData.drawRect, imageData.orgRect);
            int translateTop = (int) (cursorTop + (imageData.orgRect.top - imageData.drawRect.top));
            int translateLeft = (int) (mid - imageData.drawRect.centerX());
            imageData.matrix.postTranslate(translateLeft, translateTop);

            imageData.matrix.mapRect(imageData.drawRect, imageData.orgRect);
            cursorTop = (int) imageData.drawRect.bottom;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.saveLayer(0, 0, getMeasuredWidth(), getMeasuredHeight(), stitchingPaint, Canvas.ALL_SAVE_FLAG);

        drawImg(canvas);

        drawFrame(canvas);

        canvas.restore();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        // 分发各个img的触摸事件
        if (mViewMode != VIEW_MODE_IDLE && findIndex >= 0) {
            imgList.get(findIndex).onTouchEvent(event);
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (mViewMode == VIEW_MODE_IDLE) {
                    findIndex = findTouchImg(event);
                    if (findIndex >= 0) {
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 判断落点是否在img中
                if (mViewMode == VIEW_MODE_IDLE) {
                    findIndex = findTouchImg(event);
                    if (findIndex >= 0) {
                        imgList.get(findIndex).onTouchEvent(event);
                        if (getParent() != null)
                            getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                }
                break;
        }
        return false;
    }

    /**
     * @return -1 is not find
     */
    private int findTouchImg(MotionEvent event) {
        final float touchX = event.getX();
        final float touchY = event.getY();
        for (int i = 0; i < imgList.size(); i++) {
            ImageData imageData = imgList.get(i);
            if (imageData.drawRect.contains(touchX, touchY)) {
                return i;
            }
        }
        return -1;
    }

    protected void requestDisallowInterceptTouchEvent(boolean flag) {
        this.mViewMode = flag ? VIEW_MODE_ON_CONTROL_IMG : VIEW_MODE_IDLE;
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawRect(clipScope, framePaint);
    }

    private void drawImg(Canvas canvas) {
        for (int i = 0; i < imgList.size(); i++) {
            final ImageData img = imgList.get(i);
            canvas.drawBitmap(img.getBitmap(), img.matrix, stitchingPaint);
        }
    }

    public void rotateImage(float angle, int pos) {
        if (!checkSafe(pos))
            return;
        mViewMode = VIEW_MODE_RUN_ANIMATION;
        final ImageData imageData = imgList.get(pos);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(imageData.rotateAngle, imageData.rotateAngle + angle);
        valueAnimator.setDuration(DEFAULT_ANIMATION_TIME);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                imageData.rotateAngle = (float) animation.getAnimatedValue();
                reDraw();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mViewMode = VIEW_MODE_IDLE;
            }
        });
        valueAnimator.start();
    }

    public void scaleImage(float scale, int pos) {
        if (!checkSafe(pos))
            return;
        mViewMode = VIEW_MODE_RUN_ANIMATION;
        final ImageData imageData = imgList.get(pos);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(imageData.scale, scale);
        valueAnimator.setDuration(DEFAULT_ANIMATION_TIME);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                imageData.scale = (float) animation.getAnimatedValue();
                reDraw();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mViewMode = VIEW_MODE_IDLE;
            }
        });
        valueAnimator.start();
    }

    private void runAngleAdsorbentAnim(int pos) {
        // force run animation
        if (pos >= imgList.size() || pos < 0)
            return;
        mViewMode = VIEW_MODE_RUN_ANIMATION;
        final ImageData imageData = imgList.get(pos);
        /*
          吸附运算方式:
          e.g:

          space = 100;
          left point = 100;
          right point = 200;

          x = 161;
          calc process:

          161+50 = 211
          211/100 = 2
          2x100=200

          x = 149
          calc process:

          149+50 = 199
          199/100 = 1
          1x100 = 100

          为了保证运算方式的结果，
          以int形式进行计算,运算
          结果出来之后再转换为rate
         */
        final int adsorbentAngle = 90;
        final int orgAngle = (int) imageData.rotateAngle;
        int toAngle = ((orgAngle + (adsorbentAngle / 2)) / adsorbentAngle) * adsorbentAngle;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(orgAngle, toAngle);
        valueAnimator.setDuration(DEFAULT_ANIMATION_TIME);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                imageData.rotateAngle = (float) animation.getAnimatedValue();
                reDraw();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mViewMode = VIEW_MODE_IDLE;
            }
        });
        valueAnimator.start();
    }

    private boolean checkSafe(int pos) {
        if (pos >= imgList.size() || pos < 0)
            return false;
        if (mViewMode != VIEW_MODE_IDLE)
            return false;
        return true;
    }

    public void addImage(Bitmap bitmap) {
        if (null == bitmap)
            return;
        ImageData imageData = new ImageData(bitmap);
        imageData.rotateAngle = (int) (10 * Math.random()) * 90;
        imgList.add(imageData);
        clearMatrixCache();
        post(new Runnable() {
            @Override
            public void run() {
                reDraw();
            }
        });
    }

    public void clearImage() {
        imgList.clear();
        reDraw();
    }

    private void clearMatrixCache() {
        for (ImageData id : imgList) {
            id.clearMatrixCache();
        }
    }

    private void reDraw() {
        requestLayout();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidate();
        }
    }

    public OnGenerateBitmapListener getOnGenerateBitmapListener() {
        return onGenerateBitmapListener;
    }

    public void setOnGenerateBitmapListener(OnGenerateBitmapListener onGenerateBitmapListener) {
        this.onGenerateBitmapListener = onGenerateBitmapListener;
    }

    public void generateBitmap() {
        generateBitmap(onGenerateBitmapListener);
    }

    public void generateBitmap(OnGenerateBitmapListener onGenerateBitmapListener) {
        this.onGenerateBitmapListener = onGenerateBitmapListener;
        handleBitmapThread.start();
    }

    public class ImageData {
        public ImageData(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.matrix = new Matrix();
            orgRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        }

        // 默认置0
        float scale = 0f;
        // 0点在3点钟方向，达到垂直居中的效果，需要置为-90度
        float rotateAngle = -90f;
        RectF drawRect = new RectF();
        RectF orgRect = new RectF();
        Bitmap bitmap;
        Matrix matrix;

        private float distanceStub = 0f;
        private float angleStub = 0f;

        public Bitmap getBitmap() {
            return bitmap;
        }

        public RectF getDrawRect() {
            return drawRect;
        }

        public int getImgWidth() {
            return bitmap.getWidth();
        }

        public int getImgHeight() {
            return bitmap.getHeight();
        }

        public void layout(int l, int t, int r, int b) {
            drawRect.set(l, t, r, b);
        }

        void reSize(int w, int h) {
            int orgWidth = bitmap.getWidth();
            int orgHeight = bitmap.getHeight();
            // 计算缩放比例
            float scaleWidth = ((float) w) / orgWidth;
            float scaleHeight = ((float) h) / orgHeight;
            scale = (scaleWidth + scaleHeight) * 0.5f;
            matrix.postScale(scale, scale);
        }

        void clearMatrixCache() {
            matrix.reset();
        }

        void setScale(float scale) {
            this.scale = scale;
        }

        float getScale() {
            return this.scale;
        }

        void setRotateAngle(float angle) {
            this.rotateAngle = angle;
        }

        float getRotateAngle() {
            return this.rotateAngle;
        }

        /**
         * imageData的触摸处理事件
         *
         * @param e 触摸事件
         */
        protected void onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    requestDisallowInterceptTouchEvent(true);
                    distanceStub = getPointDistance(e);
                    angleStub = getPointAngle(e);
                    break;
                case MotionEvent.ACTION_MOVE:
                    // confirm multi touch
                    if (e.getPointerCount() > 1) {
                        float tempDistance = getPointDistance(e);
                        float tempAngle = getPointAngle(e);
                        float tempScale = this.getScale();
                        float tempRotateAngle = this.getRotateAngle();

                        tempScale += (tempDistance / distanceStub) - 1;
                        tempRotateAngle += tempAngle - angleStub;

                        angleStub = tempAngle;
                        distanceStub = tempDistance;

                        this.setRotateAngle(tempRotateAngle);
                        this.setScale(tempScale);
                        reDraw();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    runAngleAdsorbentAnim(findIndex);
                    requestDisallowInterceptTouchEvent(false);
                    if (getParent() != null)
                        getParent().requestDisallowInterceptTouchEvent(false);
                    distanceStub = 0;
                    angleStub = 0;
                    findIndex = -1;
                    break;
            }
        }

        private float getPointDistance(MotionEvent e) {
            if (e.getPointerCount() > 1) {
                final float touchX1 = e.getX(0);
                final float touchY1 = e.getY(0);
                final float touchX2 = e.getX(1);
                final float touchY2 = e.getY(1);
                return (float) Math.abs(Math.sqrt(Math.pow(touchX2 - touchX1, 2) + Math.pow(touchY2 - touchY1, 2)));
            }
            return 0;
        }

        private float getPointAngle(MotionEvent e) {
            if (e.getPointerCount() > 1) {
                final float touchX1 = e.getX(0);
                final float touchY1 = e.getY(0);
                final float touchX2 = e.getX(1);
                final float touchY2 = e.getY(1);
                return (float) (Math.atan2(touchY2 - touchY1, touchX2 - touchX1) * (180f / Math.PI));
            }
            return 0;
        }
    }

    public interface OnGenerateBitmapListener {
        void onError(Throwable t);

        void onResourceReady(Bitmap bitmap);
    }
}
