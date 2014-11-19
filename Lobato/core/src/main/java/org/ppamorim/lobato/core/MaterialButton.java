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

package org.ppamorim.lobato.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

import org.ppamorim.lobato.utils.Utils;

public abstract class MaterialButton extends CustomView {

    public int minWidth;
    public int minHeight;
    public int background;
    public float rippleSpeed = 10f;

    // ### RIPPLE EFFECT ###
    public int rippleSize = 3;
    public float x = -1, y = -1;
    public float radius = -1;

    public int mBackgroundColor;
    public int mRippleColor;
    public int mRippleSpeed;
    public int mRippleSize;

    public int backgroundColor = Color.parseColor("#1E88E5");

    public OnClickListener onClickListener;
    public MaterialButton(Context context) {
        super(context);
    }

    public MaterialButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaultProperties();
        setAttributes(attrs);
        beforeBackground = backgroundColor;
    }

    public MaterialButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    protected void setDefaultProperties() {

        setMinimumHeight(Utils.dpToPx(minHeight, getResources()));
        setMinimumWidth(Utils.dpToPx(minWidth, getResources()));

        setBackgroundResource(background);
        setBackgroundColor(backgroundColor);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled()) {
            isLastTouch = true;
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                radius = getHeight() / rippleSize;
                x = event.getX();
                y = event.getY();
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                radius = getHeight() / rippleSize;
                x = event.getX();
                y = event.getY();
                if (!((event.getX() <= getWidth() && event.getX() >= 0) && (event
                        .getY() <= getHeight() && event.getY() >= 0))) {
                    isLastTouch = false;
                    x = -1;
                    y = -1;
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if ((event.getX() <= getWidth() && event.getX() >= 0)
                        && (event.getY() <= getHeight() && event.getY() >= 0)) {
                    radius++;
                } else {
                    isLastTouch = false;
                    x = -1;
                    y = -1;
                }
            }
        }
        return true;
    }
    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
                                  Rect previouslyFocusedRect) {
        if (!gainFocus) {
            x = -1;
            y = -1;
        }
    }
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
// super.onInterceptTouchEvent(ev);
        return true;
    }
    public Bitmap makeCircle() {
        Bitmap output = Bitmap.createBitmap(
                getWidth() - Utils.dpToPx(6, getResources()), getHeight()
                        - Utils.dpToPx(7, getResources()), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawARGB(0, 0, 0, 0);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(makePressColor());
        canvas.drawCircle(x, y, radius, paint);
        if (radius > getHeight() / rippleSize)
            radius += rippleSpeed;
        if (radius >= getWidth()) {
            x = -1;
            y = -1;
            radius = getHeight() / rippleSize;
            if (onClickListener != null)
                onClickListener.onClick(this);
        }
        return output;
    }
    /**
     * Make a dark color to ripple effect
     *
     * @return makePressColor();
     */
    protected int makePressColor() {
        int r = (this.backgroundColor >> 16) & 0xFF;
        int g = (this.backgroundColor >> 8) & 0xFF;
        int b = (this.backgroundColor) & 0xFF;
        r = (r - 30 < 0) ? 0 : r - 30;
        g = (g - 30 < 0) ? 0 : g - 30;
        b = (b - 30 < 0) ? 0 : b - 30;
        return Color.rgb(r, g, b);
    }
    @Override
    public void setOnClickListener(OnClickListener l) {
        onClickListener = l;
    }
    // Set color of background
    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        if (isEnabled()) {
            beforeBackground = backgroundColor;
        }
        try {
            LayerDrawable layer = (LayerDrawable) getBackground();
            GradientDrawable shape = (GradientDrawable) layer
                    .findDrawableByLayerId(R.id.shape_background);
            shape.setColor(backgroundColor);
        } catch (Exception ex) {
// Without bacground
        }
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