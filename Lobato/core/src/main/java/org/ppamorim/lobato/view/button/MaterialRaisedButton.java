package org.ppamorim.lobato.view.button;

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

import org.ppamorim.lobato.core.MaterialButton;
import org.ppamorim.lobato.core.R;
import org.ppamorim.lobato.utils.Utils;

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

        firstOffsetValue = Utils.dpToPx(6, mResources);
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
        mTextColor = style.getColor(R.styleable.lobato_colors_text_color, baseButtonColor);
        rippleColor = style.getColor(R.styleable.lobato_colors_ripple_color, makePressColor());
        rippleSpeed = style.getInteger(R.styleable.lobato_colors_ripple_speed, (int) rippleSpeed);
        rippleSize = style.getInteger(R.styleable.lobato_colors_ripple_size, rippleSize);
        mIsUppercase = style.getBoolean(R.styleable.lobato_colors_uppercase, false);

        String text = null;
        int color = 0;


        setBackgroundResource(R.drawable.background_raised);
        LayerDrawable layer = (LayerDrawable) getBackground();
        gradientDrawable = (GradientDrawable) layer.findDrawableByLayerId(R.id.shape_raised_background);

        //Set background Color
        // Color by resource
        int textResource = attrs.getAttributeResourceValue(ANDROID_XML ,"text", -1);
        int backgroundColor = attrs.getAttributeResourceValue(ANDROID_XML,"background",-1);

        if(backgroundColor != -1){
            gradientDrawable.setColor(getResources().getColor(backgroundColor));
        } else {
            // Color by hexadecimal
            String background = attrs.getAttributeValue(ANDROID_XML, "background");
            if (background != null) {
                gradientDrawable.setColor(Color.parseColor(background));
            } else {
                gradientDrawable.setColor(accentButtonColor);
            }
        }

        if(textResource != -1){
            text = getResources().getString(textResource);
        }else{
            text = attrs.getAttributeValue(ANDROID_XML, "text");
        }

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

    @Override
    protected void onDraw(Canvas canvas) {

        if (x != -1) {

            src.set(0, 0,
                    getWidth() - firstOffsetValue,
                    getHeight() - secondOffsetValue);

            dst.set(firstOffsetValue,
                    firstOffsetValue,
                    getWidth() - firstOffsetValue,
                    getHeight()- secondOffsetValue);

            canvas.drawBitmap(makeCircle(), src, dst, null);
            gradientDrawable.draw(mCanvasLayout);
        }
        invalidate();
    }

    @Override
    public TextView getTextView() {
        return mTextView;
    }
}
