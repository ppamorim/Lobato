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

    private Context mContext;

    private Check checkView;

    private boolean press = false;
    private boolean check = false;

    private Paint mPaint = new Paint();

    private Rect src = new Rect();
    private Rect dst = new Rect();

    private OnCheckListener onCheckListener;

    public MaterialCheckbox(Context context) {
        this(context, null);
    }

    public MaterialCheckbox(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialCheckbox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setAttributes(context, attrs);
    }

    protected void setAttributes(Context context, AttributeSet attrs) {
        mContext = context;
        TypedArray style = context.obtainStyledAttributes(attrs, R.styleable.lobato_colors);
        mAccentColor = style.getColor(R.styleable.lobato_colors_accent_color, accentColor);
        mStrokeColor = style.getColor(R.styleable.lobato_colors_stroke_color, strokeColor);
        mBackgroundColor = style.getColor(R.styleable.lobato_colors_background_color, backgroundColor);

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
                    changeBackgroundColor(getResources().getColor(
                            android.R.color.transparent));
                }
            });

        }

        checkView = new Check(getContext());

        RelativeLayout.LayoutParams params = new LayoutParams(Utils.dpToPx(20,
                getResources()), Utils.dpToPx(20, getResources()));
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        checkView.setLayoutParams(params);
        addView(checkView);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (isEnabled() && event != null) {

            isLastTouch = true;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                changeBackgroundColor((check) ? makePressColor() : Color
                        .parseColor("#446D6D6D"))
                ;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {

                changeBackgroundColor(getResources().getColor(
                        android.R.color.transparent));

                press = false;

                if ((event.getX() <= getWidth() && event.getX() >= 0)
                        && (event.getY() <= getHeight() && event.getY() >= 0)) {

                    isLastTouch = false;
                    check = !check;

                    if (onCheckListener != null) {
                        onCheckListener.onCheck(check);
                    }

                    if (check) {
                        step = 0;
                    }

                    if (check) {
                        checkView.changeBackground();
                    }

                }
            }
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (press) {

            mPaint.setAntiAlias(true);
            mPaint.setColor((check) ? makePressColor() : Color
                    .parseColor("#446D6D6D"));
            canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2,
                    mPaint);
        }
        invalidate();
    }

    private void changeBackgroundColor(int color) {
        ((GradientDrawable)((LayerDrawable) getBackground()).findDrawableByLayerId(R.id.shape_background)).setColor(color);
    }

    /**
     * Make a dark color to press effect
     *
     * @return Color.argb()
     */
    protected int makePressColor() {
        int r = (this.accentColor >> 16) & 0xFF;
        int g = (this.accentColor >> 8) & 0xFF;
        int b = (this.accentColor) & 0xFF;
        r = (r - 30 < 0) ? 0 : r - 30;
        g = (g - 30 < 0) ? 0 : g - 30;
        b = (b - 30 < 0) ? 0 : b - 30;
        return Color.argb(70, r, g, b);
    }
    @Override
    public void setBackgroundColor(int color) {
        accentColor = color;
        if (isEnabled())
            beforeBackground = accentColor;
//        changeBackgroundColor(color);
    }

    public void setChecked(boolean check) {
        this.check = check;
        setPressed(false);
        changeBackgroundColor(getResources().getColor(
                android.R.color.transparent));
        if (check) {
            step = 0;
        }
        if (check)
            checkView.changeBackground();
    }

    public boolean isCheck() {
        return check;
    }

    int step = 0;


    class Check extends View {
        Bitmap sprite;
        public Check(Context context) {
            super(context);
            setBackgroundResource(R.drawable.background_checkbox_uncheck);
            sprite = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.sprite_check);
        }

        public void changeBackground() {

            if (check) {
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
                shape.setColor(mBackgroundColor);
                shape.setStroke(Math.round(2 * getResources().getDisplayMetrics().density), mStrokeColor);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (check) {
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

}
