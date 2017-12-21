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

本文代码:https://github.com/Kongdy/ImageStitching[](https://github.com/Kongdy/ImageStitching)<br/>
个人github地址:https://github.com/Kongdy[](https://github.com/Kongdy)<br/>
个人掘金主页:https://juejin.im/user/595a64def265da6c2153545b[](https://juejin.im/user/595a64def265da6c2153545b)<br/>
csdn主页:http://blog.csdn.net/qq_24859309[](http://blog.csdn.net/qq_24859309)<br/>