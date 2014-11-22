/*
 * Copyright 2014 Pedro Paulo de Amorim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ppamorim.lobato.view.button;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import org.ppamorim.lobato.core.CustomView;
import org.ppamorim.lobato.core.R;
import org.ppamorim.lobato.utils.Utils;

public class MaterialCheckbox extends CustomView {

    private int mBackgroundColor;
    private int mAccentColor;
    private int mStrokeColor;
    private int mUncheckedColor;

    public int alpha = 255;

    private Context mContext;

    private Ball mBall;
    private Check checkView;

    private boolean mShowBall;
    private boolean press = false;
    private boolean isChecked = false;

    private Paint mPaint = new Paint();

    private Rect src = new Rect();
    private Rect dst = new Rect();

    private int radius = 0;
    private int rippleSpeed = 1;

    private OnCheckListener onCheckListener;

    public float x = -1, y = -1;

    public MaterialCheckbox(Context context) {
        this(context, null);
    }

    public MaterialCheckbox(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttributes(context, attrs);
    }

    protected void setAttributes(Context context, AttributeSet attrs) {
        mContext = context;
        TypedArray style = context.obtainStyledAttributes(attrs, R.styleable.lobato_colors);
        mAccentColor = style.getColor(R.styleable.lobato_colors_accent_color, accentColor);
        mStrokeColor = style.getColor(R.styleable.lobato_colors_stroke_color, strokeColor);
        mBackgroundColor = style.getColor(R.styleable.lobato_colors_background_color, backgroundColor);
        mUncheckedColor = style.getColor(R.styleable.lobato_colors_unchecked_color, backgroundColor);

        setBackgroundResource(R.drawable.background_checkbox);

        setMinimumHeight(Utils.dpToPx(48, getResources()));
        setMinimumWidth(Utils.dpToPx(48, getResources()));

        setBackgroundColor(mAccentColor);

        if (attrs.getAttributeBooleanValue(MATERIAL_DESIGN_XML, "check", false)) {

            post(new Runnable() {
                @Override
                public void run() {
                    setChecked(true);
                    setPressed(false);
                }
            });

        }

        checkView = new Check(getContext());

        RelativeLayout.LayoutParams checkboxParams = new LayoutParams(Utils.dpToPx(20,
                getResources()), Utils.dpToPx(20, getResources()));
        checkboxParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        checkView.setLayoutParams(checkboxParams);
        addView(checkView);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        System.out.println("event: " + event.getAction());

        if (isEnabled()) {

            isLastTouch = true;

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    mShowBall = true;
                    mBall.invalidate();
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:

                    mShowBall = false;
                    mBall.invalidate();


                    press = false;
                    if ((event.getX() <= getWidth() && event.getX() >= 0)
                            && (event.getY() <= getHeight() && event.getY() >= 0)) {


                        isLastTouch = false;
                        isChecked = !isChecked;

                        if (onCheckListener != null) {
                            onCheckListener.onCheck(isChecked);
                        }

                        if (isChecked) {
                            step = 0;
                        }

                        if (isChecked) {
                            System.out.println("changeBackground");
                            invalidate();
                            checkView.changeBackground();
                        }

                    }

                    break;
            }

        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (press) {

//            mPaint.setAntiAlias(true);
//            mPaint.setColor((isChecked) ? makePressColor() : Color
//                    .parseColor("#446D6D6D"));
            canvas.drawBitmap(makeCircle(),0,0,null);
        }
        canvas.drawBitmap(makeCircle(),getWidth(),getHeight(),null);
        invalidate();
    }

    /**
     * Make a dark color to press effect
     *
     * @return Color.argb()
     */
    protected int makePressColor(int color, int diff, int alpha) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        r = (r + diff < 0) ? 0 : r + diff;
        g = (g + diff < 0) ? 0 : g + diff;
        b = (b + diff < 0) ? 0 : b + diff;
        return Color.argb(alpha, r, g, b);
    }
    @Override
    public void setBackgroundColor(int color) {
        accentColor = color;
        if (isEnabled())
            beforeBackground = accentColor;
//        changeBackgroundColor(color);
    }

    public void setChecked(boolean check) {
        this.isChecked = check;
        setPressed(false);
//        changeBackgroundColor(getResources().getColor(
//                android.R.color.transparent));
        if (check) {
            step = 0;
        }
        if (check)
            checkView.changeBackground();
    }

    public boolean isChecked() {
        return isChecked;
    }

    int step = 0;


    class Check extends View {
        Bitmap sprite;
        public Check(Context context) {
            super(context);
            setBackgroundResource(R.drawable.background_checkbox_uncheck);
            sprite = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.sprite_check);
            mBall = new Ball(getContext());
            RelativeLayout.LayoutParams params = new LayoutParams(Utils.dpToPx(80,
                    getResources()), Utils.dpToPx(80, getResources()));
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            mBall.setLayoutParams(params);
            addView(mBall);
        }

        public void changeBackground() {

            if (isChecked) {
                setBackgroundResource(R.drawable.background_checkbox_check);
                LayerDrawable layer = (LayerDrawable) getBackground();
                GradientDrawable shape = (GradientDrawable) layer
                        .findDrawableByLayerId(R.id.shape_background);
                shape.setColor(accentColor);
            } else {
                setBackgroundResource(R.drawable.background_checkbox_uncheck);
                LayerDrawable layer = (LayerDrawable) getBackground();
                GradientDrawable shape = (GradientDrawable) layer
                        .findDrawableByLayerId(R.id.shape_background);
                shape.setColor(makePressColor(mUncheckedColor, 50, 80));
                shape.setStroke(Math.round(2 * getResources().getDisplayMetrics().density), mStrokeColor);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (isChecked) {
                if (step < 11) {
                    step++;
                }
            } else {
                if (step >= 0) {
                    step--;
                }
                if (step == -1) {
                    changeBackground();
                }
            }

            src.set(40 * step, 0, (40 * step) + 40, 40);
            dst.set(0, 0, this.getWidth() - 2, this.getHeight());

            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);

            if(radius < 50) {
                radius++;
                invalidate();
            }

            canvas.drawBitmap(sprite, src, dst, null);
            invalidate();

        }
    }
    public void setOncheckListener(OnCheckListener onCheckListener) {
        this.onCheckListener = onCheckListener;
    }
    public interface OnCheckListener {
        public void onCheck(boolean check);
    }

    public Bitmap makeCircle() {

        Bitmap output = Bitmap.createBitmap(
                getWidth() - Utils.dpToPx(6, getResources()), getHeight()
                        - Utils.dpToPx(7, getResources()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);

        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        canvas.drawCircle(x, y, 50, paint);

        radius += rippleSpeed;

        return output;
    }

    public class Ball extends View {

        private Paint paint;
        private int radius = 0;

        public Ball(Context context) {
            super(context);
            paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if(isChecked) {
                paint.setColor(makePressColor(mAccentColor, 90, 50));
            } else  {
                paint.setColor(makePressColor(mUncheckedColor, 50, 80));
            }

            if(mShowBall) {
                if (radius < 80) {
                    if(radius < 20) {
                        radius = radius + 8;
                    } else {
                        radius = radius + 4;
                    }
                    invalidate();
                }
            } else {
                if(radius > 0) {
                    if(radius < 20) {
                        radius = radius - 8;
                    } else {
                        radius = radius - 4;
                    }
                    invalidate();
                }
            }

            canvas.drawCircle(getWidth()/2, getHeight()/2, radius, paint);

        }

    }

}
