/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.apis.animation;

// Need the following import to get access to the app resources, since this
// class is in a sub-package.
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import com.example.android.apis.R;

/*
 * 关键点：属性动画ValueAnimator的运用。
 */
public class BouncingBalls extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //布局文件就是一个线性布局，如果你运行过程序，肯定一眼便知这界面肯定是动态画出来的。
        setContentView(R.layout.bouncing_balls);
        LinearLayout container = (LinearLayout) findViewById(R.id.container);
        // 按F3看看MyAnimationView
        container.addView(new MyAnimationView(this));
    }

    /*
     * 运行程序之后，我们发现程序其实做了两件事，背景过渡动画，以及BouncingBalls
     */
    public class MyAnimationView extends View {

        private static final int RED = 0xffFF8080;
        private static final int BLUE = 0xff8080FF;
        private static final int CYAN = 0xff80ffff;
        private static final int GREEN = 0xff80ff80;

        public final ArrayList<ShapeHolder> balls = new ArrayList<ShapeHolder>();
        AnimatorSet animation = null;

        public MyAnimationView(Context context) {
            super(context);

            // Animate background color
            // Note that setting the background color will automatically invalidate the
            // view, so that the animated color, and the bouncing balls, get redisplayed on
            // every frame of the animation.
            //下面这一块代码就是操作背景颜色变化的，注意属性动画的使用，如果我没记错的话，api14之前需要使用http://nineoldandroids.com/
            //实际上这里用到了一点小技巧，ofInt方法传入的propertyName，如果大家对JavaBean比较熟悉的话，肯定马上能想到这里用反射机制操作backgroundColor的。
            //View当中实际上是没有backgroundColor这个属性的，但却有setBackGroundColor的方法，由于背景可能是图片，也可能是颜色（颜色最终也是drawable），没有这个属性当然也是容易理解的。
            //请大家注意，由于backgroundColor本质上是个方法，并不是一个属性，所以View并没有getBackGroundColor方法。在低版本api使用nineoldandroids的ValueAnimator时主要使用holder的方法，详见博客文章解析
            ValueAnimator colorAnim = ObjectAnimator.ofInt(this, "backgroundColor", RED, BLUE);
            colorAnim.setDuration(3000);
            colorAnim.setEvaluator(new ArgbEvaluator());
            colorAnim.setRepeatCount(ValueAnimator.INFINITE);
            colorAnim.setRepeatMode(ValueAnimator.REVERSE);
            colorAnim.start();
        }

        /*
         * 通过操作，我们发现每次调用下面的方法，会完成以下操作：
         * 生成一个ball，动画运动到屏幕底，形变，恢复，再动画运动到初始位置，淡出。
         */
        @Override
        public boolean onTouchEvent(MotionEvent event) {
        	//只处理按下，和移动手指触点的操作，常见的还有ACTION_CANCEL ACTION_UP，这里都不处理
            if (event.getAction() != MotionEvent.ACTION_DOWN &&
                    event.getAction() != MotionEvent.ACTION_MOVE) {
                return false;
            }
            //初始化一个ball，告诉他出现的位置
            ShapeHolder newBall = addBall(event.getX(), event.getY());

            // Bouncing animation with squash and stretch
            float startY = newBall.getY();
            //50f是小球的半径哈，这是屏幕底部的坐标
            float endY = getHeight() - 50f;
            float h = (float)getHeight();
            float eventY = event.getY();
            int duration = (int)(500 * ((h - eventY)/h));
            ValueAnimator bounceAnim = ObjectAnimator.ofFloat(newBall, "y", startY, endY);
            bounceAnim.setDuration(duration);
            bounceAnim.setInterpolator(new AccelerateInterpolator());
            
            //如果不做这个x方向的动画，你就会发现下面的width动画过程中重心跑偏了！
            ValueAnimator squashAnim1 = ObjectAnimator.ofFloat(newBall, "x", newBall.getX(),
                    newBall.getX() - 25f);
            squashAnim1.setDuration(duration/4);
            squashAnim1.setRepeatCount(1);
            squashAnim1.setRepeatMode(ValueAnimator.REVERSE);
            squashAnim1.setInterpolator(new DecelerateInterpolator());
            
            //横向拉伸的动画
            ValueAnimator squashAnim2 = ObjectAnimator.ofFloat(newBall, "width", newBall.getWidth(),
                    newBall.getWidth() + 50);
            squashAnim2.setDuration(duration/4);
            squashAnim2.setRepeatCount(1);
            squashAnim2.setRepeatMode(ValueAnimator.REVERSE);
            squashAnim2.setInterpolator(new DecelerateInterpolator());
            
            //降低重心
            ValueAnimator stretchAnim1 = ObjectAnimator.ofFloat(newBall, "y", endY,
                    endY + 25f);
            stretchAnim1.setDuration(duration/4);
            stretchAnim1.setRepeatCount(1);
            stretchAnim1.setInterpolator(new DecelerateInterpolator());
            stretchAnim1.setRepeatMode(ValueAnimator.REVERSE);
            
            //纵向压扁的动画
            ValueAnimator stretchAnim2 = ObjectAnimator.ofFloat(newBall, "height",
                    newBall.getHeight(), newBall.getHeight() - 25);
            stretchAnim2.setDuration(duration/4);
            stretchAnim2.setRepeatCount(1);
            stretchAnim2.setInterpolator(new DecelerateInterpolator());
            stretchAnim2.setRepeatMode(ValueAnimator.REVERSE);
            ValueAnimator bounceBackAnim = ObjectAnimator.ofFloat(newBall, "y", endY,
                    startY);
            bounceBackAnim.setDuration(duration);
            bounceBackAnim.setInterpolator(new DecelerateInterpolator());
            // Sequence the down/squash&stretch/up animations
            
            //这没啥好说的了，这代码跟读文章一样
            AnimatorSet bouncer = new AnimatorSet();
            bouncer.play(bounceAnim).before(squashAnim1);
            bouncer.play(squashAnim1).with(squashAnim2);
            bouncer.play(squashAnim1).with(stretchAnim1);
            bouncer.play(squashAnim1).with(stretchAnim2);
            bouncer.play(bounceBackAnim).after(stretchAnim2);

            //完事儿，淡出
            // Fading animation - remove the ball when the animation is done
            ValueAnimator fadeAnim = ObjectAnimator.ofFloat(newBall, "alpha", 1f, 0f);
            fadeAnim.setDuration(250);
            fadeAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    balls.remove(((ObjectAnimator)animation).getTarget());

                }
            });

            // Sequence the two animations to play one after the other
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(bouncer).before(fadeAnim);

            // Start the animation
            animatorSet.start();

            return true;
        }

        /**
         * 
         * @param x 初始化坐标x
         * @param y 初始化坐标y
         * @return
         */
        private ShapeHolder addBall(float x, float y) {
        	//这个跟布局文件里面创建的shape是一回事
            OvalShape circle = new OvalShape();
            circle.resize(50f, 50f);
            //封装进drawable
            ShapeDrawable drawable = new ShapeDrawable(circle);
            ShapeHolder shapeHolder = new ShapeHolder(drawable);
            shapeHolder.setX(x - 25f);
            shapeHolder.setY(y - 25f);
            int red = (int)(Math.random() * 255);
            int green = (int)(Math.random() * 255);
            int blue = (int)(Math.random() * 255);
            int color = 0xff000000 | red << 16 | green << 8 | blue;
            Paint paint = drawable.getPaint(); //new Paint(Paint.ANTI_ALIAS_FLAG);
            int darkColor = 0xff000000 | red/4 << 16 | green/4 << 8 | blue/4;
            RadialGradient gradient = new RadialGradient(37.5f, 12.5f,
                    50f, color, darkColor, Shader.TileMode.CLAMP);
            //加了个遮罩，仔细观察生成的球，右上角有高光反射点
            paint.setShader(gradient);
            shapeHolder.setPaint(paint);
            balls.add(shapeHolder);
            return shapeHolder;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            for (int i = 0; i < balls.size(); ++i) {
                ShapeHolder shapeHolder = balls.get(i);
                //因为事件触发后我们要绘制多个（balls是个list嘛）ball，所以如果不save、restore的话，相互之间的矩阵变换就会有影响。
                canvas.save();
                canvas.translate(shapeHolder.getX(), shapeHolder.getY());
                shapeHolder.getShape().draw(canvas);
                canvas.restore();
            }
        }
    }
}