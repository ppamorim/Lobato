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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ppamorim.lobato.core.MaterialButton;
import org.ppamorim.lobato.core.R;
import org.ppamorim.lobato.utils.Utils;

public class MaterialFlatButton extends MaterialButton {

    public TextView mTextView;
    private Context mContext;

    private Paint mPaint = new Paint();

    public MaterialFlatButton(Context context) {
        this(context, null);
    }

    public MaterialFlatButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        if(mTextView != null) {
            setCustomFont(context, mTextView, attrs);
        }
        mPaint.setAntiAlias(true);
    }

    protected void setDefaultProperties(){
        rippleSize = 10;
        rippleSpeed = 10f;
        rippleFadeSpeed = 5f;
    }

    @Override
    protected void setAttributes(AttributeSet attrs) {

        TypedArray style = getContext().obtainStyledAttributes(attrs, R.styleable.lobato_colors);
        mBackgroundColor = style.getColor(R.styleable.lobato_colors_background_color, flatBackgroundColor);
        mTextColor = style.getColor(R.styleable.lobato_colors_text_color, flatTextColor);
        rippleColor = style.getColor(R.styleable.lobato_colors_ripple_color, accentColor);
        rippleSpeed = style.getInteger(R.styleable.lobato_colors_ripple_speed, (int) rippleSpeed);
        rippleSize = style.getInteger(R.styleable.lobato_colors_ripple_size, rippleSize);
        mIsUppercase = style.getBoolean(R.styleable.lobato_colors_uppercase, false);

        String text = null;
        int color = 0;

        int textResource = attrs.getAttributeResourceValue(ANDROID_XML ,"text", -1);
        int backgroundResource = attrs.getAttributeResourceValue(ANDROID_XML ,"background", -1);
        int xmlRes = attrs.getAttributeResourceValue(ANDROID_XML, "background", -1);
        if(textResource != -1){
            text = getResources().getString(textResource);
        }else{
            text = attrs.getAttributeValue(ANDROID_XML, "text");
        }

        setBackgroundResource(R.drawable.background_transparent);

        if(text != null){

            mTextView = new TextView(getContext());

            if(mIsUppercase) {
                mTextView.setText(text.toUpperCase());
            } else {
                mTextView.setText(text);
            }

            mTextView.setTextColor(mTextColor);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            mTextView.setLayoutParams(params);
            addView(mTextView);
        }

        int padding = Utils.dpToPx(minPadding, getResources());
        setPadding(padding, padding, padding, padding);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (x != -1) {

            mPaint.setColor(makePressColor());
            mPaint.setAlpha(alpha);

            if(alpha > 0) {
                alpha--;
            }

            canvas.drawCircle(x, y, radius, mPaint);

            if(radius > getHeight()/rippleSize) {
                radius += rippleSpeed;
            }

            if(radius >= getWidth() && onClickListener != null){
                onClickListener.onClick(this);
            }

        }
        invalidate();
    }
    /**
     * Make a dark color to ripple effect
     * @return
     */
    @Override
    public int makePressColor(){
        return Color.parseColor("#88DDDDDD");
    }

    public void setText(String text){
        mTextView.setText(text.toUpperCase());
    }

    public String getText(){
        return mTextView.getText().toString();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public TextView getTextView() {
        return mTextView;
    }

}
