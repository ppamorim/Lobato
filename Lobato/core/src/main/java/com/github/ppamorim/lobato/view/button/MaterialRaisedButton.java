package com.github.ppamorim.lobato.view.button;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.ppamorim.lobato.core.MaterialButton;
import com.github.ppamorim.lobato.core.R;
import com.github.ppamorim.lobato.utils.Utils;

import java.util.regex.Pattern;

public class MaterialRaisedButton extends MaterialButton {

    private TextView mTextView;

    private Paint mPaint = new Paint();
    private Canvas mCanvasLayout = new Canvas();
    private GradientDrawable gradientDrawable;

    private Rect src;
    private Rect dst;

    private Resources mResources;

    private int firstOffsetValue;
    private int secondOffsetValue;

    public MaterialRaisedButton(Context context) {
        this(context, null);
    }

    public MaterialRaisedButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mResources = getResources();


        firstOffsetValue = Utils.dpToPx(5, mResources);
        secondOffsetValue = Utils.dpToPx(7, mResources);

        src = new Rect();
        dst = new Rect();

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
        mTextColor = style.getColor(R.styleable.lobato_colors_textColor, -1);
        rippleColor = style.getColor(R.styleable.lobato_colors_rippleColor, makePressColor());
        rippleSpeed = style.getInteger(R.styleable.lobato_colors_rippleSpeed, (int) rippleSpeed);
        fadeColorSpeed = style.getFloat(R.styleable.lobato_colors_fadeSpeed, fadeColorSpeed);
        rippleSize = style.getInteger(R.styleable.lobato_colors_rippleSize, rippleSize);
        mIsUppercase = style.getBoolean(R.styleable.lobato_colors_uppercase, false);
        effectBaseButtonColor = style.getColor(R.styleable.lobato_colors_pressColor, effectBaseButtonColor);

        String text;

        setBackgroundResource(R.drawable.background_raised);
        LayerDrawable layer = (LayerDrawable) getBackground();
        gradientDrawable = (GradientDrawable) layer.findDrawableByLayerId(R.id.shape_raised_background);


        Pattern colorPattern = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

        String attrBackground = attrs.getAttributeValue(ANDROID_XML, "background");
        if(attrBackground != null) {
            baseButtonColor = Color.parseColor(attrBackground);
        }

        if(colorPattern.matcher(convertIntColorToString(baseButtonColor)).matches()) {
            gradientDrawable.setColor(baseButtonColor);
        } else {
            gradientDrawable.setColor(Color.WHITE);
        }

        String attrTextColor = attrs.getAttributeValue(ANDROID_XML, "textColor");
        if(attrTextColor != null) {
            mTextColor = Color.parseColor(attrTextColor);
        }

        if(!colorPattern.matcher(convertIntColorToString(mTextColor)).matches()) {
            mTextColor = opposeColor(baseButtonColor);
        }

        if(effectBaseButtonColor == 0) {
            effectBaseButtonColor = makeTouchColor(baseButtonColor);
        }

        int textResource = attrs.getAttributeResourceValue(ANDROID_XML ,"text", -1);

        if(textResource != -1){
            text = getResources().getString(textResource);
        }else{
            text = attrs.getAttributeValue(ANDROID_XML, "text");
        }

        Utils.log("mTextColor: " + mTextColor);

        if(text != null){
            mTextView = new TextView(getContext());
            mTextView.setText(text);
            mTextView.setTextColor(mTextColor);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            params.setMargins(Utils.dpToPx(5, getResources()), Utils.dpToPx(5, getResources()), Utils.dpToPx(5, getResources()), Utils.dpToPx(5, getResources()));
            mTextView.setLayoutParams(params);
            addView(mTextView);

        }



        int padding = Utils.dpToPx(minPadding, getResources());
        setPadding(padding, padding, padding, padding);

    }

    private String convertIntColorToString(int value) {
        return String.format(String.format("#%06X", 0xFFFFFF & value));
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (x != -1) {

            src.set(0, 0,
                    getWidth() - firstOffsetValue,
                    getHeight() - secondOffsetValue);

            dst.set(firstOffsetValue,
                    firstOffsetValue,
                    getWidth() - secondOffsetValue,
                    getHeight()- secondOffsetValue);

            canvas.drawBitmap(makeCircle(gradientDrawable, baseButtonColor), src, dst, null);
            gradientDrawable.draw(mCanvasLayout);
        }
        invalidate();
    }

    @Override
    public TextView getTextView() {
        return mTextView;
    }
}
