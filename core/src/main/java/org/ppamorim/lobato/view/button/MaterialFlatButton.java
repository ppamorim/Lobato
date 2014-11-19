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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.ppamorim.lobato.core.MaterialButton;
import org.ppamorim.lobato.core.R;
import org.ppamorim.lobato.utils.Utils;

public class MaterialFlatButton extends MaterialButton {

    private TextView mTextView;

    public MaterialFlatButton(Context context) {
        super(context);
    }

    public MaterialFlatButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomFont(context, attrs);
    }

    public MaterialFlatButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomFont(context, attrs);
    }

    @Override
    public TextView getTextView() {
        return mTextView;
    }

    protected void setDefaultProperties(){
        minHeight = 36;
        minWidth = 88;
        rippleSize = 3;
        // Min size
        setMinimumHeight(Utils.dpToPx(minHeight, getResources()));
        setMinimumWidth(Utils.dpToPx(minWidth, getResources()));
        setBackgroundResource(R.drawable.lobato_background_transparent);
    }

    @Override
    protected void setAttributes(AttributeSet attrs) {
// Set text button
        String text = null;
        int textResource = attrs.getAttributeResourceValue(ANDROID_XML ,"text", -1);
        if(textResource != -1){
            text = getResources().getString(textResource);
        }else{
            text = attrs.getAttributeValue(ANDROID_XML,"text");
        }
        if(text != null){
            mTextView = new TextView(getContext());
            mTextView.setText(text.toUpperCase());
            mTextView.setTextColor(backgroundColor);
            mTextView.setTypeface(null, Typeface.BOLD);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            mTextView.setLayoutParams(params);
            addView(mTextView);
        }
        int bacgroundColor = attrs.getAttributeResourceValue(ANDROID_XML,"background",-1);
        if(bacgroundColor != -1){
            setBackgroundColor(getResources().getColor(bacgroundColor));
        }else{
            // Color by hexadecimal
            String background = attrs.getAttributeValue(ANDROID_XML,"background");
            if(background != null) {
                setBackgroundColor(Color.parseColor(background));
            }
        }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (x != -1) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(makePressColor());
            canvas.drawCircle(x, y, radius, paint);
            if(radius > getHeight()/rippleSize)
                radius += rippleSpeed;
            if(radius >= getWidth()){
                x = -1;
                y = -1;
                radius = getHeight()/rippleSize;
                if(onClickListener != null)
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
    // Set color of background
    public void setBackgroundColor(int color){
        backgroundColor = color;
        if(isEnabled())
            beforeBackground = backgroundColor;
        mTextView.setTextColor(color);
    }

    public String getText(){
        return mTextView.getText().toString();
    }

}
