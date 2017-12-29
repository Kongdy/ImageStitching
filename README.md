# ImageStitching
图像矩阵实现，图片拼接、缩放、旋转等工能
## 一、前文<br/>
&nbsp;&nbsp;之前有个朋友委托我实现一个图片拼接的组件，感觉挺有意思，于是周末花了些时间去研究了下，其实拼接这一步并不难，但是我在研究中发现了Matrix这个东西，非常好的东西。为此，我竟然拾起了多年没有动过的线性代数。<br/>
## 二、原理
&nbsp;&nbsp;要彻底搞懂matrix还是需要一定的线性代数上面的理解，不过对于基本使用，了解到矩阵乘法就足够了。<br/>
&nbsp;&nbsp;在android坐标系中，分为x、y和z三个轴，分别代表了长、宽、高三个维度。如下图所示<br/>
![android坐标系](https://user-gold-cdn.xitu.io/2017/12/21/160781ac13ec4e8b?w=527&h=238&f=jpeg&s=8064)
<br/>
&nbsp;&nbsp;在android中，使用三维坐标(x,y,z)组成一个行列式与一个三阶行列式进行矩阵乘法。<br/>
![矩阵乘法](https://user-gold-cdn.xitu.io/2017/12/21/160781ac106891a8?w=393&h=222&f=png&s=6619)
<br/>
&nbsp;&nbsp;图中显示的使用初始坐标组成的矩阵与单位矩阵进行矩阵乘法。矩阵乘法使用可以参考[矩阵乘法](https://en.wikipedia.org/wiki/Matrix_(mathematics))<br/>
&nbsp;&nbsp;Martix会把输入进来的矩阵带入到其内部的矩阵中进行计算，最终输出新的矩阵，来达到对图形形态的处理。<br/>
## 三、基本方法的使用
&nbsp;&nbsp;Matrix提供的基本方法有三种模式，<br/>
1. setXXX()方法，例如 setRotate(),setScale()
2. preXXX()方法，例如 preRotate(),preScale()
3.  postXXX()方法，例如 postRotate(),postScale()
<br/>
其中，setXXX()会先将矩阵重置为单位矩阵，然后再进行矩阵变幻<br/>
preXXX()和postXXX()方法会牵扯到矩阵的前乘和后乘，如果了解了矩阵乘法规则，就会明白矩阵前乘和后乘得出来的结果是不一样的，不过一般情况下都会选择使用post方法，后乘。<br/>
其中还有扩展方法比如：<br/>
1. mapRect(rect) / mapRect(desRect,orgRect)<br/>
	&nbsp;&nbsp;第一个方法即使用原始矩阵代入运算，会将返回的矩阵直接覆盖在传入的矩阵中<br/>
	&nbsp;&nbsp;第二个方法则是对于需要保存原始矩阵的情况下，会把原始矩阵的计算结果赋值到指定的矩阵中
2. setRectToRect(src,des,stf)<br/>
	&nbsp;&nbsp;这个方法相当于将原始矩阵填充到目标矩阵中，所以也就要求两个矩阵都是有值的。其中填充模式由第三个参数决定。

	```java
	    /**
         * 独立缩放X和Y，直到和src的rect和目标rect确切的匹配。这可能会改变原始rect的宽高比
         */
        FILL(0),
        /**
         * 在保持原有宽高比的情况下计算出一个合适的缩放比例，但也会确保原始rect合适的填入目标rect，
         * 最终会把开始的一个边与目标的开始边左边对齐
         */
        START(1),
        /**
         * 与START类似，不过最终结果会尽可能居中
         */
        CENTER(2),
        /**
         * 与START类似，不过最终结果会尽可能靠右边
         */
        END(3);
	```

3.  invert(inverse)<br/>
	&nbsp;&nbsp;反转矩阵，可以应用到类似倒影一类的实现中
4.  setPolyToPoly(src,srcIndex,dst,dstIndex,pointCount)
	&nbsp;&nbsp;这是一个比较神奇的方法。随着pointCount点数量，可以对原始矩阵进行平移、旋转、错切、翻页效果。功能非常强大。
<br/><br/>
此外，关于Matrix还有颜色变幻等效果，更多扩展用法后面会讲到。

## 四、实践到自定义view中
&nbsp;&nbsp;写一个自定义view，最重要的是要了解view的绘制过程。简单的绘制流程如下<br/>
![view绘制流程](https://user-gold-cdn.xitu.io/2017/12/21/160781ac13fca60e?w=683&h=780&f=png&s=12052)
<br/>
其中不带on的方法都为调度方法，不可被重写，这些方法里面会把前期一些必要的数据准备出来，带on前缀的方法都是实际进行处理的方法。<br/>
measure方法是测量控件大小的，layout是用来布局，根据measure测量的结果，把其中每个元素在其内部进行位置的计算。最后会执行的draw方法，draw也分为draw和onDraw，可以根据自己需求来改写对应的方法。<br/>
其中，onMeasure的方法如下所示:<br/>

```java
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
                        measureWidthSize), MeasureSpec.makeMeasureSpec(totalImageHeight,
                        measureHeightSize));
                break;
            // match_parent or accurate num
            case MeasureSpec.EXACTLY:
                setMeasuredDimension(MeasureSpec.makeMeasureSpec(maxImgWidth,
				measureHeightSize));
                break;
            case MeasureSpec.UNSPECIFIED:
                setMeasuredDimension(MeasureSpec.makeMeasureSpec(maxImgWidth,
                        measureWidthSize), MeasureSpec.makeMeasureSpec(totalImageHeight,
                        measureHeightSize));
                break;
        }
    }
```
<br/>
所有影响尺寸计算相关的方法都会放到这个measure里面进行计算，比如scale和rotate，都会影响size大小。所以在这里计算完成后，好在layout中进行正确的布局。<br/>
layout中的代码如下:<br/>

```java
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
            int translateTop = (int) (cursorTop + (imageData.orgRect.top -
            imageData.drawRect.top));
            int translateLeft = (int) (mid - imageData.drawRect.centerX());
            imageData.matrix.postTranslate(translateLeft, translateTop);

            imageData.matrix.mapRect(imageData.drawRect, imageData.orgRect);
            cursorTop = (int) imageData.drawRect.bottom;
        }
    }
```
两个方法中，要做到Matrix多效果叠加，切记要保留一个bitmap最原始的矩阵，然后再接下来的计算中需要用到当前尺寸的时候，使用Martix计算出临时的尺寸对其进行计算。<br/>
两个方法中，Bitmap被封装到一个ImageData类里面，进行对象化，这样可以更好的管理Bitmap的处理和数据记录。<br/>
ImageData如下:<br/>
```java
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
	        // ...
        }

        private float getPointDistance(MotionEvent e) {
	        // ...
        }

        private float getPointAngle(MotionEvent e) {
	        // ...
        }
    }
```
这里面跟本文无关的方法都隐藏了，随后会讲到.<br/>
那么我们来看看效果<br/>
![实现效果](https://user-gold-cdn.xitu.io/2017/12/21/160781ac0f425a10?w=332&h=588&f=gif&s=2493375)


# android触摸机制
首先，当用户点下屏幕的时候，Linux会将触摸包装成Event，然后InputReader会收到来自EventBus发送过来的Event，最后InputDispatcher分发给ViewRootImpl，ViewRootImpl再传递给DecorView，这最终才到达了我们的当前界面，接下来的传递如下图所示。

![android触摸传递](https://user-gold-cdn.xitu.io/2017/12/29/160a184bcacb09bb?w=994&h=740&f=png&s=27295)

<br/>图画的不好，水平有限，望见谅。

# 事件分发

<br/>那从这里我们就知道，我们要写的view，需要先从dispatchTouchEvent()里面分发触摸事件，然后再TouchEvent()里面进行事件的处理。以下是dispatchTouchEvent中的处理。

```java
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
```

<br/>这里使用getActionMask()是为了更好的处理多点触控。使用findTouchImg()方法，判断如果点落到图片区域就消费这次事件，但是，后续的触摸事件，父控件还是有可能拦截的，这次只是消费了这次按压触摸事件。如果是多点触控，就直接调用requestDisallowInterceptTouchEvent的方法，禁止父控件拦截子控件的后续事件，不过使用这个方法要记着后面释放。判断确实是多点触控之后，就直接在方法顶部执行Img的方法，避免下面不必要的判断。这里findTouchImg()方法主要是根据每个Img的DrawRect进行点的落位判定。方法如下

```java
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
```

# 触摸事件处理

这里我们主要实现两种效果，缩放和旋转。我们把Img的touch处理封装到了ImageData里面，代码如下:

```java
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
```

<br/>当多点触控的时候，记录下最先两个触摸点的距离和斜率角度，在随后发生滑动的时候，计算与之前触摸点距离和斜率角度发生的变化，再对Bitmap进行即时调整。计算距离和斜率角度的方法如下:

```java
private float getPointDistance(MotionEvent e) {
            if (e.getPointerCount() > 1) {
                final float touchX1 = e.getX(0);
                final float touchY1 = e.getY(0);
                final float touchX2 = e.getX(1);
                final float touchY2 = e.getY(1);
                return (float) Math.abs(Math.sqrt(Math.pow(touchX2 - touchX1, 2) +
                Math.pow(touchY2 - touchY1, 2)));
            }
            return 0;
        }

        private float getPointAngle(MotionEvent e) {
            if (e.getPointerCount() > 1) {
                final float touchX1 = e.getX(0);
                final float touchY1 = e.getY(0);
                final float touchX2 = e.getX(1);
                final float touchY2 = e.getY(1);
                return (float) (Math.atan2(touchY2 - touchY1, touchX2 - touchX1) * (180f
                / Math.PI));
            }
            return 0;
        }

```

<br/>计算两点距离很简单，中学的计算公式![两点之间求距离公式](https://user-gold-cdn.xitu.io/2017/12/29/160a184af9b40850?w=278&h=51&f=png&s=1712) 求两点相减的平方求根之后就是直线距离了。
<br/>求斜率也是借助中学的计算公式![斜率公式](https://user-gold-cdn.xitu.io/2017/12/29/160a184afb7f976c?w=291&h=47&f=png&s=1844) 算出来斜率，不过此时的斜率不能直接计算，要转换成角度。而转换成角度，只需要乘以(180÷π)即可。

<br/>那么我们求出角度和距离公式之后，只需要跟上一次记录的数据进行比对，即可改变数据。我们看看实现效果。

![触摸效果](https://user-gold-cdn.xitu.io/2017/12/29/160a184bede740dc?w=332&h=588&f=gif&s=3045168)

<br/>但是到这一步还没有完，我们还要加上吸附动画。

# 动画

我们先直接看看吸附动画的代码:

```java
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
```

吸附的运算原理在注释中已经详细距离说明了。这里就不再解释了。传参进来一个img的坐标，使用ValueAnimator对其属性进行改变，调用reDraw()方法即可完成一帧的动画。
<br/>最后再来看看添加完吸附动画之后的效果:

![带有吸附效果的触摸实现](https://user-gold-cdn.xitu.io/2017/12/29/160a184b014f5840?w=332&h=588&f=gif&s=2052400)

最后我们到这里基本的控制操作就完成了，还差最后一步，就是最终的图片拼接。

# 图片拼接

android的View给我们提供了getDrawingCache()方法来获得当前view的绘制界面，不过这个方法受很多因素影响，不能每次都可以调用成功，并且可能会发生不可预知的后续操作，开启DrawingCache会产生性能影响。所以我们自己创建一个Cavans，传给onDraw()方法，让其把当前最新的界面绘制到我们传给他的Cavans上面。代码如下:

```java
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
```

使用自己创建的Cavans还可以限定画布大小，达到裁剪的目的。onDraw()方法执行完成之后，界面绘制到了我们传递的Bitmap上面，就可以把Bitmap抛出给处理方法来实现显示或者存储等一系列操作。



<br/>
使用方法,跟目录gradle里面添加<br/>

```java
repositories {
			...
			maven { url 'https://jitpack.io' }
		}
```

<br/>
app.gradle中添加:<br/>

```java
compile 'com.github.Kongdy:ImageStitching:v1.0.0'
```

<br/>

本文代码:[https://github.com/Kongdy/ImageStitching](https://github.com/Kongdy/ImageStitching)<br/>
个人github地址:[https://github.com/Kongdy](https://github.com/Kongdy)<br/>
个人掘金主页:[https://juejin.im/user/595a64def265da6c2153545b](https://juejin.im/user/595a64def265da6c2153545b)<br/>
csdn主页:[http://blog.csdn.net/u014303003](http://blog.csdn.net/u014303003)<br/>