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

package com.github.ppamorim.lobato.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import com.github.ppamorim.lobato.utils.Utils;

public abstract class MaterialButton extends CustomView {

    public int minWidth;
    public int minHeight;
    public  int minPadding = 16;
    public int background;

    // ### RIPPLE EFFECT ###
    public int rippleSize = 3;
    public float x = -1, y = -1;
    public float radius = -1;

    public int rippleColor;
    public float rippleSpeed = 10f;
    public float rippleFadeSpeed = 5f;
    public int alpha = (int)rippleFadeSpeed * 25;
    public float alphaColor = 1.0f;
    public float fadeColorSpeed = 0.25f;

    public int mBackgroundColor;
    public int mTextColor;

    public boolean mIsUppercase;

    public int flatBackgroundColor = Color.parseColor("#EEEEEE");

    public int baseButtonColor = Color.WHITE;
    public int effectBaseButtonColor = 0;
    public int accentButtonColor = Color.parseColor("#2196F3");

    public int raisedBackgroundColor = Color.parseColor("#2196F3");
    public int raisedTextColor = Color.parseColor("#FFFFFF");

    public OnClickListener onClickListener;
    public MaterialButton(Context context) {
        super(context);
    }

    public MaterialButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaultProperties();
        setAttributes(attrs);
        beforeBackground = flatBackgroundColor;
    }

    public MaterialButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void setDefaultProperties() {

        setMinimumHeight(Utils.dpToPx(minHeight, getResources()));
        setMinimumWidth(Utils.dpToPx(minWidth, getResources()));

//        setBackgroundResource(background);
//        setBackgroundColor(flatBackgroundColor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (isEnabled()) {
            isLastTouch = true;
            alpha = (int)rippleFadeSpeed * 25;
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    setElementParams(event);
                    if (!verifyWidthAndHeight(event) &&
                            event.getAction() == MotionEvent.ACTION_MOVE ||
                            event.getAction() != MotionEvent.ACTION_UP) {
                        resetTouchValues();
                    } else {
                        radius++;
                    }
                    break;
            }
        }
        return true;
    }

    private void resetTouchValues() {
        isLastTouch = false;
        x = -1;
        y = -1;
        alphaColor = 1.0f;
    }

    private boolean verifyWidthAndHeight(MotionEvent event) {
        return ((event.getX() <= getWidth() && event.getX() >= 0) &&
                (event.getY() <= getHeight() && event.getY() >= 0));
    }

    private void setElementParams(MotionEvent event) {
        radius = getHeight() / rippleSize;
        x = event.getX();
        y = event.getY();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        if (!gainFocus) {
            x = -1;
            y = -1;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

    public Bitmap makeCircle(GradientDrawable canvasBackground, int background) {

        Bitmap output = Bitmap.createBitmap(
                getWidth() - Utils.dpToPx(6, getResources()), getHeight()
                        - Utils.dpToPx(7, getResources()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);

        Paint paint = new Paint();
        paint.setColor(rippleColor);
        paint.setAlpha(alpha);

        Paint paint2 = new Paint();
        paint2.setColor(Color.BLUE);

        if(alpha > 0) {
            alpha--;
        }

        if(alphaColor > 0.0) {
            canvasBackground.setColor(blendColors(effectBaseButtonColor, background, alphaColor));
            alphaColor = alphaColor - fadeColorSpeed;
        }

        canvas.drawCircle(x, y, radius, paint);

        if(radius > getHeight()/rippleSize) {
            radius += rippleSpeed;
        }

        if(radius >= getWidth() && onClickListener != null){
            onClickListener.onClick(this);
        }

        return output;
    }

    /**
     * Blend {@code color1} and {@code color2} using the given ratio.
     *
     * @param ratio of which to blend. 1.0 will return {@code color1}, 0.5 will give an even blend,
     *              0.0 will return {@code color2}.
     */
    private static int blendColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    /**
     * Make a dark color to ripple effect
     *
     * @return makePressColor();
     */
    protected int makePressColor() {
        int r = (this.flatBackgroundColor >> 16) & 0xFF;
        int g = (this.flatBackgroundColor >> 8) & 0xFF;
        int b = (this.flatBackgroundColor) & 0xFF;
        r = (r - 30 < 0) ? 0 : r - 30;
        g = (g - 30 < 0) ? 0 : g - 30;
        b = (b - 30 < 0) ? 0 : b - 30;
        return Color.rgb(r, g, b);
    }

    /**
     * Make a dark color to ripple effect
     *
     * @return makePressColor();
     */
    protected int makeTouchColor() {
        int r = (this.flatBackgroundColor >> 16) & 0xFF;
        int g = (this.flatBackgroundColor >> 8) & 0xFF;
        int b = (this.flatBackgroundColor) & 0xFF;
        r = (r - 50 < 0) ? 0 : r - 50;
        g = (g - 50 < 0) ? 0 : g - 50;
        b = (b - 50 < 0) ? 0 : b - 50;
        return Color.rgb(r, g, b);
    }

    protected int makeTouchColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color) & 0xFF;
        r = (r - 30 < 0) ? 0 : r - 30;
        g = (g - 30 < 0) ? 0 : g - 30;
        b = (b - 30 < 0) ? 0 : b - 30;
        return Color.rgb(r, g, b);
    }

    public static int opposeColor(int colorToInvert)
    {
        double y = (299 * Color.red(colorToInvert) + 587 * Color.green(colorToInvert) + 114 * Color.blue(colorToInvert)) / 1000;
        return y >= 128 ? Color.BLACK : Color.WHITE;
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        onClickListener = l;
    }
    // Set color of background
    public void setBackgroundColor(int color) {

//        this.flatBackgroundColor = color;
//        if (isEnabled()) {
//            beforeBackground = flatBackgroundColor;
//        }
//
//        try {
//            LayerDrawable layer = (LayerDrawable) getBackground();
//            GradientDrawable shape = (GradientDrawable) layer
//                    .findDrawableByLayerId(R.id.shape_background);
//            shape.setColor(flatBackgroundColor);
//        } catch (Exception ex) {
//        // Without bacground
//        }
    }

    public void setRippleSpeed(float rippleSpeed) {
        this.rippleSpeed = rippleSpeed;
    }
    public float getRippleSpeed() {
        return this.rippleSpeed;
    }

    abstract protected void setAttributes(AttributeSet attrs);
    abstract public TextView getTextView();

}