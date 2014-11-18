package org.ppamorim.lobato.base;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.BoringLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextDirectionHeuristic;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.RemoteViews;
import android.widget.Scroller;
import android.widget.TextView;

import org.ppamorim.lobato.R;

import java.util.ArrayList;
import java.util.Locale;

@RemoteViews.RemoteView
public class VariableTextView extends View {

    static final String LOG_TAG = "VariableTextView";
    static final boolean DEBUG_EXTRACT = false;

    // Enum for the "typeface" XML parameter.
    // TODO: How can we get this from the XML instead of hardcoding it here?
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    // Bitfield for the "numeric" XML parameter.
    // TODO: How can we get this from the XML instead of hardcoding it here?
    private static final int SIGNED = 2;
    private static final int DECIMAL = 4;

    /**
     * Draw marquee text with fading edges as usual
     */
    private static final int MARQUEE_FADE_NORMAL = 0;
    /**
     * Draw marquee text as ellipsize end while inactive instead of with the fade.
     * (Useful for devices where the fade can be expensive if overdone)
     */
    private static final int MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS = 1;
    /**
     * Draw marquee text with fading edges because it is currently active/animating.
     */
    private static final int MARQUEE_FADE_SWITCH_SHOW_FADE = 2;

    private static final int LINES = 1;
    private static final int EMS = LINES;
    private static final int PIXELS = 2;

    private static final RectF TEMP_RECTF = new RectF();

    // XXX should be much larger
    private static final int VERY_WIDE = 1024*1024;
    private static final int ANIMATED_SCROLL_GAP = 250;

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final Spanned EMPTY_SPANNED = new SpannedString("");

    private static final int CHANGE_WATCHER_PRIORITY = 100;

    // New state used to change background based on whether this TextView is multiline.
    private static final int[] MULTILINE_STATE_SET = { R.attr.state_multiline };

    // System wide time for last cut or copy action.
    static long LAST_CUT_OR_COPY_TIME;

    private ColorStateList mTextColor;
    private ColorStateList mHintTextColor;
    private ColorStateList mLinkTextColor;
    @ViewDebug.ExportedProperty(category = "text")
    private int mCurTextColor;
    private int mCurHintTextColor;
    private boolean mFreezesText;
    private boolean mTemporaryDetach;
    private boolean mDispatchTemporaryDetach;

    private Editable.Factory mEditableFactory = Editable.Factory.getInstance();
    private Spannable.Factory mSpannableFactory = Spannable.Factory.getInstance();

    private float mShadowRadius, mShadowDx, mShadowDy;
    private int mShadowColor;

    private boolean mPreDrawRegistered;
    private boolean mPreDrawListenerDetached;

    // A flag to prevent repeated movements from escaping the enclosing text view. The idea here is
// that if a user is holding down a movement key to traverse text, we shouldn't also traverse
// the view hierarchy. On the other hand, if the user is using the movement key to traverse views
// (i.e. the first movement was to traverse out of this view, or this view was traversed into by
// the user holding the movement key down) then we shouldn't prevent the focus from changing.

    private boolean mPreventDefaultMovement;

    private TextUtils.TruncateAt mEllipsize;

    static class Drawables {

        final static int DRAWABLE_NONE = -1;
        final static int DRAWABLE_RIGHT = 0;
        final static int DRAWABLE_LEFT = 1;

        final Rect mCompoundRect = new Rect();

        Drawable mDrawableTop, mDrawableBottom, mDrawableLeft, mDrawableRight,
                mDrawableStart, mDrawableEnd, mDrawableError, mDrawableTemp;

        Drawable mDrawableLeftInitial, mDrawableRightInitial;
        boolean mIsRtlCompatibilityMode;
        boolean mOverride;

        int mDrawableSizeTop, mDrawableSizeBottom, mDrawableSizeLeft, mDrawableSizeRight,
                mDrawableSizeStart, mDrawableSizeEnd, mDrawableSizeError, mDrawableSizeTemp;
        int mDrawableWidthTop, mDrawableWidthBottom, mDrawableHeightLeft, mDrawableHeightRight,
                mDrawableHeightStart, mDrawableHeightEnd, mDrawableHeightError, mDrawableHeightTemp;
        int mDrawablePadding;

        int mDrawableSaved = DRAWABLE_NONE;

        public Drawables(Context context) {
            final int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
            mIsRtlCompatibilityMode = true;

//            mIsRtlCompatibilityMode = (targetSdkVersion < JELLY_BEAN_MR1 ||
//                    !context.getApplicationInfo().hasRtlSupport());
            mOverride = false;
        }

        public void resolveWithLayoutDirection(int layoutDirection) {
            // First reset "left" and "right" drawables to their initial values
            mDrawableLeft = mDrawableLeftInitial;
            mDrawableRight = mDrawableRightInitial;

            if (mIsRtlCompatibilityMode) {
                // Use "start" drawable as "left" drawable if the "left" drawable was not defined
                if (mDrawableStart != null && mDrawableLeft == null) {
                    mDrawableLeft = mDrawableStart;
                    mDrawableSizeLeft = mDrawableSizeStart;
                    mDrawableHeightLeft = mDrawableHeightStart;
                }
                // Use "end" drawable as "right" drawable if the "right" drawable was not defined
                if (mDrawableEnd != null && mDrawableRight == null) {
                    mDrawableRight = mDrawableEnd;
                    mDrawableSizeRight = mDrawableSizeEnd;
                    mDrawableHeightRight = mDrawableHeightEnd;
                }
            } else {
                // JB-MR1+ normal case: "start" / "end" drawables are overriding "left" / "right"
                // drawable if and only if they have been defined
                switch(layoutDirection) {
                    case LAYOUT_DIRECTION_RTL:
                        if (mOverride) {
                            mDrawableRight = mDrawableStart;
                            mDrawableSizeRight = mDrawableSizeStart;
                            mDrawableHeightRight = mDrawableHeightStart;
                            mDrawableLeft = mDrawableEnd;
                            mDrawableSizeLeft = mDrawableSizeEnd;
                            mDrawableHeightLeft = mDrawableHeightEnd;
                        }
                        break;
                    case LAYOUT_DIRECTION_LTR:
                    default:
                        if (mOverride) {
                            mDrawableLeft = mDrawableStart;
                            mDrawableSizeLeft = mDrawableSizeStart;
                            mDrawableHeightLeft = mDrawableHeightStart;
                            mDrawableRight = mDrawableEnd;
                            mDrawableSizeRight = mDrawableSizeEnd;
                            mDrawableHeightRight = mDrawableHeightEnd;
                        }
                        break;
                }
            }
            applyErrorDrawableIfNeeded(layoutDirection);
            updateDrawablesLayoutDirection(layoutDirection);
        }
        private void updateDrawablesLayoutDirection(int layoutDirection) {
            if (mDrawableLeft != null) {
                mDrawableLeft.setLayoutDirection(layoutDirection);
            }
            if (mDrawableRight != null) {
                mDrawableRight.setLayoutDirection(layoutDirection);
            }
            if (mDrawableTop != null) {
                mDrawableTop.setLayoutDirection(layoutDirection);
            }
            if (mDrawableBottom != null) {
                mDrawableBottom.setLayoutDirection(layoutDirection);
            }
        }
        public void setErrorDrawable(Drawable dr, TextView tv) {
            if (mDrawableError != dr && mDrawableError != null) {
                mDrawableError.setCallback(null);
            }
            mDrawableError = dr;

            final Rect compoundRect = mCompoundRect;
            int[] state = tv.getDrawableState();

            if (mDrawableError != null) {
                mDrawableError.setState(state);
                mDrawableError.copyBounds(compoundRect);
                mDrawableError.setCallback(tv);
                mDrawableSizeError = compoundRect.width();
                mDrawableHeightError = compoundRect.height();
            } else {
                mDrawableSizeError = mDrawableHeightError = 0;
            }
        }

        private void applyErrorDrawableIfNeeded(int layoutDirection) {
            // first restore the initial state if needed
            switch (mDrawableSaved) {
                case DRAWABLE_LEFT:
                    mDrawableLeft = mDrawableTemp;
                    mDrawableSizeLeft = mDrawableSizeTemp;
                    mDrawableHeightLeft = mDrawableHeightTemp;
                    break;
                case DRAWABLE_RIGHT:
                    mDrawableRight = mDrawableTemp;
                    mDrawableSizeRight = mDrawableSizeTemp;
                    mDrawableHeightRight = mDrawableHeightTemp;
                    break;
                case DRAWABLE_NONE:
                default:
            }
            // then, if needed, assign the Error drawable to the correct location
            if (mDrawableError != null) {
                switch(layoutDirection) {
                    case LAYOUT_DIRECTION_RTL:
                        mDrawableSaved = DRAWABLE_LEFT;

                        mDrawableTemp = mDrawableLeft;
                        mDrawableSizeTemp = mDrawableSizeLeft;
                        mDrawableHeightTemp = mDrawableHeightLeft;

                        mDrawableLeft = mDrawableError;
                        mDrawableSizeLeft = mDrawableSizeError;
                        mDrawableHeightLeft = mDrawableHeightError;
                        break;
                    case LAYOUT_DIRECTION_LTR:
                    default:
                        mDrawableSaved = DRAWABLE_RIGHT;

                        mDrawableTemp = mDrawableRight;
                        mDrawableSizeTemp = mDrawableSizeRight;
                        mDrawableHeightTemp = mDrawableHeightRight;

                        mDrawableRight = mDrawableError;
                        mDrawableSizeRight = mDrawableSizeError;
                        mDrawableHeightRight = mDrawableHeightError;
                        break;
                }
            }
        }
    }

    Drawables mDrawables;

    private CharWrapper mCharWrapper;

    private Marquee mMarquee;
    private boolean mRestartMarquee;

    private int mMarqueeRepeatLimit = 3;

    private int mLastLayoutDirection = -1;

    /**
     * On some devices the fading edges add a performance penalty if used
     * extensively in the same layout. This mode indicates how the marquee
     * is currently being shown, if applicable. (mEllipsize will == MARQUEE)
     */
    private int mMarqueeFadeMode = MARQUEE_FADE_NORMAL;
    /**
     * When mMarqueeFadeMode is not MARQUEE_FADE_NORMAL, this stores
     * the layout that should be used when the mode switches.
     */
    private Layout mSavedMarqueeModeLayout;

    @ViewDebug.ExportedProperty(category = "text")
    private CharSequence mText;
    private CharSequence mTransformed;
    private TextView.BufferType mBufferType = TextView.BufferType.NORMAL;

    private CharSequence mHint;
    private Layout mHintLayout;

    private MovementMethod mMovement;

    private TransformationMethod mTransformation;
    private boolean mAllowTransformationLengthChange;
    private ChangeWatcher mChangeWatcher;

    private ArrayList<TextWatcher> mListeners;

    // display attributes
    private final TextPaint mTextPaint;
    private boolean mUserSetTextScaleX;
    private Layout mLayout;

    private int mGravity = Gravity.TOP | Gravity.START;
    private boolean mHorizontallyScrolling;

    private int mAutoLinkMask;
    private boolean mLinksClickable = true;

    private float mSpacingMult = 1.0f;
    private float mSpacingAdd = 0.0f;

    private int mMaximum = Integer.MAX_VALUE;
    private int mMaxMode = LINES;
    private int mMinimum = 0;
    private int mMinMode = LINES;

    private int mOldMaximum = mMaximum;
    private int mOldMaxMode = mMaxMode;

    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxWidthMode = PIXELS;
    private int mMinWidth = 0;
    private int mMinWidthMode = PIXELS;

    private boolean mSingleLine;
    private int mDesiredHeightAtMeasure = -1;
    private boolean mIncludePad = true;
    private int mDeferScroll = -1;

    // tmp primitives, so we don't alloc them on each draw
    private Rect mTempRect;
    private long mLastScroll;
    private Scroller mScroller;

    private BoringLayout.Metrics mBoring, mHintBoring;
    private BoringLayout mSavedLayout, mSavedHintLayout;

    private TextDirectionHeuristic mTextDir;

    private InputFilter[] mFilters = NO_FILTERS;

    private volatile Locale mCurrentSpellCheckerLocaleCache;

    // It is possible to have a selection even when mEditor is null (programmatically set, like when
    // a link is pressed). These highlight-related fields do not go in mEditor.
    int mHighlightColor = 0x6633B5E5;
    private Path mHighlightPath;
    private final Paint mHighlightPaint;
    private boolean mHighlightPathBogus = true;

    // Although these fields are specific to editable text, they are not added to Editor because
    // they are defined by the TextView's style and are theme-dependent.
    int mCursorDrawableRes;
    // These four fields, could be moved to Editor, since we know their default values and we
    // could condition the creation of the Editor to a non standard value. This is however
    // brittle since the hardcoded values here (such as
    // com.android.internal.R.drawable.text_select_handle_left) would have to be updated if the
    // default style is modified.
    int mTextSelectHandleLeftRes;
    int mTextSelectHandleRightRes;
    int mTextSelectHandleRes;
    int mTextEditSuggestionItemLayout;

    /*
    * Kick-start the font cache for the zygote process (to pay the cost of
    * initializing freetype for our default font only once).
    */
    static {
        Paint p = new Paint();
        p.setAntiAlias(true);
// We don't care about the result, just the side-effect of measuring.
        p.measureText("H");
    }

    /**
     * Interface definition for a callback to be invoked when an action is
     * performed on the editor.
     */
    public interface OnEditorActionListener {
        /**
         * Called when an action is being performed.
         *
         * @param v The view that was clicked.
         * @param actionId Identifier of the action. This will be either the
         * identifier you supplied, or {@link EditorInfo#IME_NULL
         * EditorInfo.IME_NULL} if being called due to the enter key
         * being pressed.
         * @param event If triggered by an enter key, this is the event;
         * otherwise, this is null.
         * @return Return true if you have consumed the action, else false.
         */
        boolean onEditorAction(TextView v, int actionId, KeyEvent event);
    }

    public VariableTextView(Context context) {
        this(context, null);
    }

    public VariableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.textViewStyle);
    }

    public VariableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressWarnings("deprecation")
    public VariableTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mText = "";

        final Resources res = getResources();
        final CompatibilityInfo compat = res.getCompatibilityInfo();

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.density = res.getDisplayMetrics().density;
        mTextPaint.setCompatibilityScaling(compat.applicationScale);

        mHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mHighlightPaint.setCompatibilityScaling(compat.applicationScale);

        mMovement = getDefaultMovementMethod();

        mTransformation = null;

        int textColorHighlight = 0;
        ColorStateList textColor = null;
        ColorStateList textColorHint = null;
        ColorStateList textColorLink = null;

        int textSize = 15;
        String fontFamily = null;
        int typefaceIndex = -1;
        int styleIndex = -1;
        boolean allCaps = false;
        int shadowcolor = 0;
        float dx = 0, dy = 0, r = 0;
        boolean elegant = false;
        float letterSpacing = 0;
        String fontFeatureSettings = null;

        final Resources.Theme theme = context.getTheme();

        /*
        * Look the appearance up without checking first if it exists because
        * almost every TextView has one and it greatly simplifies the logic
        * to be able to parse the appearance first and then let specific tags
        * for this View override it.
        */

        TypedArray a = theme.obtainStyledAttributes(attrs,
                com.android.internal.R.styleable.TextViewAppearance, defStyleAttr, defStyleRes);
        TypedArray appearance = null;
        int ap = a.getResourceId(
                com.android.internal.R.styleable.TextViewAppearance_textAppearance, -1);
        a.recycle();
        if (ap != -1) {
            appearance = theme.obtainStyledAttributes(
                    ap, com.android.internal.R.styleable.TextAppearance);
        }
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                switch (attr) {
                    case com.android.internal.R.styleable.TextAppearance_textColorHighlight:
                        textColorHighlight = appearance.getColor(attr, textColorHighlight);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_textColor:
                        textColor = appearance.getColorStateList(attr);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_textColorHint:
                        textColorHint = appearance.getColorStateList(attr);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_textColorLink:
                        textColorLink = appearance.getColorStateList(attr);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_textSize:
                        textSize = appearance.getDimensionPixelSize(attr, textSize);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_typeface:
                        typefaceIndex = appearance.getInt(attr, -1);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_fontFamily:
                        fontFamily = appearance.getString(attr);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_textStyle:
                        styleIndex = appearance.getInt(attr, -1);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_textAllCaps:
                        allCaps = appearance.getBoolean(attr, false);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_shadowColor:
                        shadowcolor = appearance.getInt(attr, 0);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_shadowDx:
                        dx = appearance.getFloat(attr, 0);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_shadowDy:
                        dy = appearance.getFloat(attr, 0);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_shadowRadius:
                        r = appearance.getFloat(attr, 0);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_elegantTextHeight:
                        elegant = appearance.getBoolean(attr, false);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_letterSpacing:
                        letterSpacing = appearance.getFloat(attr, 0);
                        break;
                    case com.android.internal.R.styleable.TextAppearance_fontFeatureSettings:
                        fontFeatureSettings = appearance.getString(attr);
                        break;
                }
            }
            appearance.recycle();
        }

        CharSequence inputMethod = null;
        int numeric = 0;
        CharSequence digits = null;
        boolean phone = false;
        boolean autotext = false;
        int autocap = -1;
        int buffertype = 0;
        boolean selectallonfocus = false;
        Drawable drawableLeft = null, drawableTop = null, drawableRight = null,
                drawableBottom = null, drawableStart = null, drawableEnd = null;
        int drawablePadding = 0;
        int ellipsize = -1;
        boolean singleLine = false;
        int maxlength = -1;
        CharSequence text = "";
        CharSequence hint = null;
        boolean password = false;
        int inputType = EditorInfo.TYPE_NULL;
        a = theme.obtainStyledAttributes(
                attrs, com.android.internal.R.styleable.TextView, defStyleAttr, defStyleRes);
        int n = a.getIndexCount();

        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case com.android.internal.R.styleable.TextView_inputMethod:
                    inputMethod = a.getText(attr);
                    break;
                case com.android.internal.R.styleable.TextView_numeric:
                    numeric = a.getInt(attr, numeric);
                    break;
                case com.android.internal.R.styleable.TextView_digits:
                    digits = a.getText(attr);
                    break;
                case com.android.internal.R.styleable.TextView_phoneNumber:
                    phone = a.getBoolean(attr, phone);
                    break;
                case com.android.internal.R.styleable.TextView_autoText:
                    autotext = a.getBoolean(attr, autotext);
                    break;
                case com.android.internal.R.styleable.TextView_capitalize:
                    autocap = a.getInt(attr, autocap);
                    break;
                case com.android.internal.R.styleable.TextView_bufferType:
                    buffertype = a.getInt(attr, buffertype);
                    break;
                case com.android.internal.R.styleable.TextView_selectAllOnFocus:
                    selectallonfocus = a.getBoolean(attr, selectallonfocus);
                    break;
                case com.android.internal.R.styleable.TextView_autoLink:
                    mAutoLinkMask = a.getInt(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_linksClickable:
                    mLinksClickable = a.getBoolean(attr, true);
                    break;
                case com.android.internal.R.styleable.TextView_drawableLeft:
                    drawableLeft = a.getDrawable(attr);
                    break;
                case com.android.internal.R.styleable.TextView_drawableTop:
                    drawableTop = a.getDrawable(attr);
                    break;
                case com.android.internal.R.styleable.TextView_drawableRight:
                    drawableRight = a.getDrawable(attr);
                    break;
                case com.android.internal.R.styleable.TextView_drawableBottom:
                    drawableBottom = a.getDrawable(attr);
                    break;
                case com.android.internal.R.styleable.TextView_drawableStart:
                    drawableStart = a.getDrawable(attr);
                    break;
                case com.android.internal.R.styleable.TextView_drawableEnd:
                    drawableEnd = a.getDrawable(attr);
                    break;
                case com.android.internal.R.styleable.TextView_drawablePadding:
                    drawablePadding = a.getDimensionPixelSize(attr, drawablePadding);
                    break;
                case com.android.internal.R.styleable.TextView_maxLines:
                    setMaxLines(a.getInt(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_maxHeight:
                    setMaxHeight(a.getDimensionPixelSize(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_lines:
                    setLines(a.getInt(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_height:
                    setHeight(a.getDimensionPixelSize(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_minLines:
                    setMinLines(a.getInt(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_minHeight:
                    setMinHeight(a.getDimensionPixelSize(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_maxEms:
                    setMaxEms(a.getInt(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_maxWidth:
                    setMaxWidth(a.getDimensionPixelSize(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_ems:
                    setEms(a.getInt(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_width:
                    setWidth(a.getDimensionPixelSize(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_minEms:
                    setMinEms(a.getInt(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_minWidth:
                    setMinWidth(a.getDimensionPixelSize(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_gravity:
                    setGravity(a.getInt(attr, -1));
                    break;
                case com.android.internal.R.styleable.TextView_hint:
                    hint = a.getText(attr);
                    break;
                case com.android.internal.R.styleable.TextView_text:
                    text = a.getText(attr);
                    break;
                case com.android.internal.R.styleable.TextView_scrollHorizontally:
                    if (a.getBoolean(attr, false)) {
                        setHorizontallyScrolling(true);
                    }
                    break;
                case com.android.internal.R.styleable.TextView_singleLine:
                    singleLine = a.getBoolean(attr, singleLine);
                    break;
                case com.android.internal.R.styleable.TextView_ellipsize:
                    ellipsize = a.getInt(attr, ellipsize);
                    break;
                case com.android.internal.R.styleable.TextView_includeFontPadding:
                    if (!a.getBoolean(attr, true)) {
                        setIncludeFontPadding(false);
                    }
                    break;
                case com.android.internal.R.styleable.TextView_maxLength:
                    maxlength = a.getInt(attr, -1);
                    break;
                case com.android.internal.R.styleable.TextView_textScaleX:
                    setTextScaleX(a.getFloat(attr, 1.0f));
                    break;
                case com.android.internal.R.styleable.TextView_freezesText:
                    mFreezesText = a.getBoolean(attr, false);
                    break;
                case com.android.internal.R.styleable.TextView_shadowColor:
                    shadowcolor = a.getInt(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_shadowDx:
                    dx = a.getFloat(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_shadowDy:
                    dy = a.getFloat(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_shadowRadius:
                    r = a.getFloat(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_enabled:
                    setEnabled(a.getBoolean(attr, isEnabled()));
                    break;
                case com.android.internal.R.styleable.TextView_textColorHighlight:
                    textColorHighlight = a.getColor(attr, textColorHighlight);
                    break;
                case com.android.internal.R.styleable.TextView_textColor:
                    textColor = a.getColorStateList(attr);
                    break;
                case com.android.internal.R.styleable.TextView_textColorHint:
                    textColorHint = a.getColorStateList(attr);
                    break;
                case com.android.internal.R.styleable.TextView_textColorLink:
                    textColorLink = a.getColorStateList(attr);
                    break;
                case com.android.internal.R.styleable.TextView_textSize:
                    textSize = a.getDimensionPixelSize(attr, textSize);
                    break;
                case com.android.internal.R.styleable.TextView_typeface:
                    typefaceIndex = a.getInt(attr, typefaceIndex);
                    break;
                case com.android.internal.R.styleable.TextView_textStyle:
                    styleIndex = a.getInt(attr, styleIndex);
                    break;
                case com.android.internal.R.styleable.TextView_fontFamily:
                    fontFamily = a.getString(attr);
                    break;
                case com.android.internal.R.styleable.TextView_password:
                    password = a.getBoolean(attr, password);
                    break;
                case com.android.internal.R.styleable.TextView_lineSpacingExtra:
                    mSpacingAdd = a.getDimensionPixelSize(attr, (int) mSpacingAdd);
                    break;
                case com.android.internal.R.styleable.TextView_lineSpacingMultiplier:
                    mSpacingMult = a.getFloat(attr, mSpacingMult);
                    break;
                case com.android.internal.R.styleable.TextView_inputType:
                    inputType = a.getInt(attr, EditorInfo.TYPE_NULL);
                    break;
                case com.android.internal.R.styleable.TextView_textCursorDrawable:
                    mCursorDrawableRes = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_textSelectHandleLeft:
                    mTextSelectHandleLeftRes = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_textSelectHandleRight:
                    mTextSelectHandleRightRes = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_textSelectHandle:
                    mTextSelectHandleRes = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_textEditSuggestionItemLayout:
                    mTextEditSuggestionItemLayout = a.getResourceId(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_textAllCaps:
                    allCaps = a.getBoolean(attr, false);
                    break;
                case com.android.internal.R.styleable.TextView_elegantTextHeight:
                    elegant = a.getBoolean(attr, false);
                    break;
                case com.android.internal.R.styleable.TextView_letterSpacing:
                    letterSpacing = a.getFloat(attr, 0);
                    break;
                case com.android.internal.R.styleable.TextView_fontFeatureSettings:
                    fontFeatureSettings = a.getString(attr);
                    break;
            }
        }
        a.recycle();
        TextView.BufferType bufferType = TextView.BufferType.EDITABLE;
        final int variation =
                inputType & (EditorInfo.TYPE_MASK_CLASS | EditorInfo.TYPE_MASK_VARIATION);
        final boolean passwordInputType = variation
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        final boolean webPasswordInputType = variation
                == (EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD);
        final boolean numberPasswordInputType = variation
                == (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD);

        setCompoundDrawablesWithIntrinsicBounds(
                drawableLeft, drawableTop, drawableRight, drawableBottom);
        setRelativeDrawablesIfNeeded(drawableStart, drawableEnd);
        setCompoundDrawablePadding(drawablePadding);
// Same as setSingleLine(), but make sure the transformation method and the maximum number
// of lines of height are unchanged for multi-line TextViews.
        setInputTypeSingleLine(singleLine);
        applySingleLine(singleLine, singleLine, singleLine);
        if (singleLine && getKeyListener() == null && ellipsize < 0) {
            ellipsize = 3; // END
        }
        switch (ellipsize) {
            case 1:
                setEllipsize(TextUtils.TruncateAt.START);
                break;
            case 2:
                setEllipsize(TextUtils.TruncateAt.MIDDLE);
                break;
            case 3:
                setEllipsize(TextUtils.TruncateAt.END);
                break;
            case 4:
                if (ViewConfiguration.get(context).isFadingMarqueeEnabled()) {
                    setHorizontalFadingEdgeEnabled(true);
                    mMarqueeFadeMode = MARQUEE_FADE_NORMAL;
                } else {
                    setHorizontalFadingEdgeEnabled(false);
                    mMarqueeFadeMode = MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS;
                }
                setEllipsize(TextUtils.TruncateAt.MARQUEE);
                break;
        }
        setTextColor(textColor != null ? textColor : ColorStateList.valueOf(0xFF000000));
        setHintTextColor(textColorHint);
        setLinkTextColor(textColorLink);
        if (textColorHighlight != 0) {
            setHighlightColor(textColorHighlight);
        }
        setRawTextSize(textSize);
        setElegantTextHeight(elegant);
        setLetterSpacing(letterSpacing);
        setFontFeatureSettings(fontFeatureSettings);
        if (allCaps) {
            setTransformationMethod(new AllCapsTransformationMethod(getContext()));
        }
        if (password || passwordInputType || webPasswordInputType || numberPasswordInputType) {
            setTransformationMethod(PasswordTransformationMethod.getInstance());
            typefaceIndex = MONOSPACE;
        }
        setTypefaceFromAttrs(fontFamily, typefaceIndex, styleIndex);
        if (shadowcolor != 0) {
            setShadowLayer(r, dx, dy, shadowcolor);
        }
        if (maxlength >= 0) {
            setFilters(new InputFilter[] { new InputFilter.LengthFilter(maxlength) });
        } else {
            setFilters(NO_FILTERS);
        }
        setText(text, bufferType);
        if (hint != null) setHint(hint);


// If not explicitly specified this view is important for accessibility.
        if (getImportantForAccessibility() == IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    public final void setTransformationMethod(TransformationMethod method) {
        if (method == mTransformation) {
// Avoid the setText() below if the transformation is
// the same.
            return;
        }
        if (mTransformation != null) {
            if (mText instanceof Spannable) {
                ((Spannable) mText).removeSpan(mTransformation);
            }
        }
        mTransformation = method;
        if (method instanceof TransformationMethod2) {
            TransformationMethod2 method2 = (TransformationMethod2) method;
            mAllowTransformationLengthChange = !isTextSelectable() && !(mText instanceof Editable);
            method2.setLengthChangesAllowed(mAllowTransformationLengthChange);
        } else {
            mAllowTransformationLengthChange = false;
        }
        setText(mText);
        if (hasPasswordTransformationMethod()) {
            notifyViewAccessibilityStateChangedIfNeeded(
                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
        }
    }

    /**
     * Makes the TextView at least this many pixels tall.
     *
     * Setting this value overrides any other (minimum) number of lines setting.
     *
     * @attr ref android.R.styleable#TextView_minHeight
     */
    @RemotableViewMethod
    public void setMinHeight(int minHeight) {
        mMinimum = minHeight;
        mMinMode = PIXELS;
        requestLayout();
        invalidate();
    }

    /**
     * Sets the string value of the TextView. TextView <em>does not</em> accept
     * HTML-like formatting, which you can do with text strings in XML resource files.
     * To style your strings, attach android.text.style.* objects to a
     * {@link android.text.SpannableString SpannableString}, or see the
     * <a href="{@docRoot}guide/topics/resources/available-resources.html#stringresources">
     * Available Resource Types</a> documentation for an example of setting
     * formatted text in the XML resource file.
     *
     * @attr ref android.R.styleable#TextView_text
     */
    @RemotableViewMethod
    public final void setText(CharSequence text) {
        setText(text, mBufferType);
    }

    /**
     * Sets the text that this TextView is to display (see
     * {@link #setText(CharSequence)}) and also sets whether it is stored
     * in a styleable/spannable buffer and whether it is editable.
     *
     * @attr ref android.R.styleable#TextView_text
     * @attr ref android.R.styleable#TextView_bufferType
     */
    public void setText(CharSequence text, TextView.BufferType type) {
        setText(text, type, true, 0);
        if (mCharWrapper != null) {
            mCharWrapper.mChars = null;
        }
    }

    private void setText(CharSequence text, TextView.BufferType type,
                         boolean notifyBefore, int oldlen) {
        if (text == null) {
            text = "";
        }
// If suggestions are not enabled, remove the suggestion spans from the text
        if (!isSuggestionsEnabled()) {
            text = removeSuggestionSpans(text);
        }
        if (!mUserSetTextScaleX) mTextPaint.setTextScaleX(1.0f);
        if (text instanceof Spanned &&
                ((Spanned) text).getSpanStart(TextUtils.TruncateAt.MARQUEE) >= 0) {
            if (ViewConfiguration.get(mContext).isFadingMarqueeEnabled()) {
                setHorizontalFadingEdgeEnabled(true);
                mMarqueeFadeMode = MARQUEE_FADE_NORMAL;
            } else {
                setHorizontalFadingEdgeEnabled(false);
                mMarqueeFadeMode = MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS;
            }
            setEllipsize(TextUtils.TruncateAt.MARQUEE);
        }
        int n = mFilters.length;
        for (int i = 0; i < n; i++) {
            CharSequence out = mFilters[i].filter(text, 0, text.length(), EMPTY_SPANNED, 0, 0);
            if (out != null) {
                text = out;
            }
        }
        if (notifyBefore) {
            if (mText != null) {
                oldlen = mText.length();
                sendBeforeTextChanged(mText, 0, oldlen, text.length());
            } else {
                sendBeforeTextChanged("", 0, 0, text.length());
            }
        }
        boolean needEditableForNotification = false;
        if (mListeners != null && mListeners.size() != 0) {
            needEditableForNotification = true;
        }
        if (type == BufferType.EDITABLE || getKeyListener() != null ||
                needEditableForNotification) {
            createEditorIfNeeded();
            Editable t = mEditableFactory.newEditable(text);
            text = t;
            setFilters(t, mFilters);
            InputMethodManager imm = InputMethodManager.peekInstance();
            if (imm != null) imm.restartInput(this);
        } else if (type == BufferType.SPANNABLE || mMovement != null) {
            text = mSpannableFactory.newSpannable(text);
        } else if (!(text instanceof CharWrapper)) {
            text = TextUtils.stringOrSpannedString(text);
        }
        if (mAutoLinkMask != 0) {
            Spannable s2;
            if (type == BufferType.EDITABLE || text instanceof Spannable) {
                s2 = (Spannable) text;
            } else {
                s2 = mSpannableFactory.newSpannable(text);
            }
            if (Linkify.addLinks(s2, mAutoLinkMask)) {
                text = s2;
                type = (type == BufferType.EDITABLE) ? BufferType.EDITABLE : BufferType.SPANNABLE;
/*
* We must go ahead and set the text before changing the
* movement method, because setMovementMethod() may call
* setText() again to try to upgrade the buffer type.
*/
                mText = text;
// Do not change the movement method for text that support text selection as it
// would prevent an arbitrary cursor displacement.
                if (mLinksClickable && !textCanBeSelected()) {
                    setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        }
        mBufferType = type;
        mText = text;
        if (mTransformation == null) {
            mTransformed = text;
        } else {
            mTransformed = mTransformation.getTransformation(text, this);
        }
        final int textLength = text.length();
        if (text instanceof Spannable && !mAllowTransformationLengthChange) {
            Spannable sp = (Spannable) text;
// Remove any ChangeWatchers that might have come from other TextViews.
            final ChangeWatcher[] watchers = sp.getSpans(0, sp.length(), ChangeWatcher.class);
            final int count = watchers.length;
            for (int i = 0; i < count; i++) {
                sp.removeSpan(watchers[i]);
            }
            if (mChangeWatcher == null) mChangeWatcher = new ChangeWatcher();
            sp.setSpan(mChangeWatcher, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE |
                    (CHANGE_WATCHER_PRIORITY << Spanned.SPAN_PRIORITY_SHIFT));
            if (mEditor != null) mEditor.addSpanWatchers(sp);
            if (mTransformation != null) {
                sp.setSpan(mTransformation, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
            if (mMovement != null) {
                mMovement.initialize(this, (Spannable) text);
/*
* Initializing the movement method will have set the
* selection, so reset mSelectionMoved to keep that from
* interfering with the normal on-focus selection-setting.
*/
                if (mEditor != null) mEditor.mSelectionMoved = false;
            }
        }
        if (mLayout != null) {
            checkForRelayout();
        }
        sendOnTextChanged(text, 0, oldlen, textLength);
        onTextChanged(text, 0, oldlen, textLength);
        notifyViewAccessibilityStateChangedIfNeeded(AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT);
        if (needEditableForNotification) {
            sendAfterTextChanged((Editable) text);
        }
// SelectionModifierCursorController depends on textCanBeSelected, which depends on text
        if (mEditor != null) mEditor.prepareCursorControllers();
    }
    /**
     * Sets the TextView to display the specified slice of the specified
     * char array. You must promise that you will not change the contents
     * of the array except for right before another call to setText(),
     * since the TextView has no way to know that the text
     * has changed and that it needs to invalidate and re-layout.
     */
    public final void setText(char[] text, int start, int len) {
        int oldlen = 0;
        if (start < 0 || len < 0 || start + len > text.length) {
            throw new IndexOutOfBoundsException(start + ", " + len);
        }
/*
* We must do the before-notification here ourselves because if
* the old text is a CharWrapper we destroy it before calling
* into the normal path.
*/
        if (mText != null) {
            oldlen = mText.length();
            sendBeforeTextChanged(mText, 0, oldlen, len);
        } else {
            sendBeforeTextChanged("", 0, 0, len);
        }
        if (mCharWrapper == null) {
            mCharWrapper = new CharWrapper(text, start, len);
        } else {
            mCharWrapper.set(text, start, len);
        }
        setText(mCharWrapper, mBufferType, false, oldlen);
    }

    /**
     * It would be better to rely on the input type for everything. A password inputType should have
     * a password transformation. We should hence use isPasswordInputType instead of this method.
     *
     * We should:
     * - Call setInputType in setKeyListener instead of changing the input type directly (which
     * would install the correct transformation).
     * - Refuse the installation of a non-password transformation in setTransformation if the input
     * type is password.
     *
     * However, this is like this for legacy reasons and we cannot break existing apps. This method
     * is useful since it matches what the user can see (obfuscated text or not).
     *
     * @return true if the current transformation method is of the password type.
     */
    private boolean hasPasswordTransformationMethod() {
        return mTransformation instanceof PasswordTransformationMethod;
    }

    private void nullLayouts() {
        if (mLayout instanceof BoringLayout && mSavedLayout == null) {
            mSavedLayout = (BoringLayout) mLayout;
        }
        if (mHintLayout instanceof BoringLayout && mSavedHintLayout == null) {
            mSavedHintLayout = (BoringLayout) mHintLayout;
        }
        mSavedMarqueeModeLayout = mLayout = mHintLayout = null;
        mBoring = mHintBoring = null;

    }

    //TODO:bla!

    /**
     * Causes words in the text that are longer than the view is wide
     * to be ellipsized instead of broken in the middle. You may also
     * to constrain the text to a single line. Use <code>null</code>
     * to turn off ellipsizing.
     *
     * If {@link #setMaxLines} has been used to set two or more lines,
     * only {@link android.text.TextUtils.TruncateAt#END} and
     * {@link android.text.TextUtils.TruncateAt#MARQUEE} are supported
     * (other ellipsizing types will not do anything).
     *
     * @attr ref android.R.styleable#TextView_ellipsize
     */
    public void setEllipsize(TextUtils.TruncateAt where) {
// TruncateAt is an enum. != comparison is ok between these singleton objects.
        if (mEllipsize != where) {
            mEllipsize = where;
            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use 0 if you do not want a Drawable there.
     * The Drawables' bounds will be set to their intrinsic bounds.
     *
     * @param left Resource identifier of the left Drawable.
     * @param top Resource identifier of the top Drawable.
     * @param right Resource identifier of the right Drawable.
     * @param bottom Resource identifier of the bottom Drawable.
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    @RemotableViewMethod
    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
        final Context context = getContext();
        setCompoundDrawablesWithIntrinsicBounds(left != 0 ? context.getDrawable(left) : null,
                top != 0 ? context.getDrawable(top) : null,
                right != 0 ? context.getDrawable(right) : null,
                bottom != 0 ? context.getDrawable(bottom) : null);
    }

    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use {@code null} if you do not want a
     * Drawable there. The Drawables' bounds will be set to their intrinsic
     * bounds.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawablesRelative} or related methods.
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawablesWithIntrinsicBounds(@Nullable Drawable left,
                                                        @Nullable Drawable top, @Nullable Drawable right, @Nullable Drawable bottom) {
        if (left != null) {
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (right != null) {
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawables(left, top, right, bottom);
    }

    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use {@code null} if you do not want a
     * Drawable there. The Drawables must already have had
     * {@link Drawable#setBounds} called.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawables(@Nullable Drawable left, @Nullable Drawable top,
                                     @Nullable Drawable right, @Nullable Drawable bottom) {
        Drawables dr = mDrawables;
// We're switching to absolute, discard relative.
        if (dr != null) {
            if (dr.mDrawableStart != null) dr.mDrawableStart.setCallback(null);
            dr.mDrawableStart = null;
            if (dr.mDrawableEnd != null) dr.mDrawableEnd.setCallback(null);
            dr.mDrawableEnd = null;
            dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
            dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
        }
        final boolean drawables = left != null || top != null || right != null || bottom != null;
        if (!drawables) {
// Clearing drawables... can we free the data structure?
            if (dr != null) {
                if (dr.mDrawablePadding == 0) {
                    mDrawables = null;
                } else {
// We need to retain the last set padding, so just clear
// out all of the fields in the existing structure.
                    if (dr.mDrawableLeft != null) dr.mDrawableLeft.setCallback(null);
                    dr.mDrawableLeft = null;
                    if (dr.mDrawableTop != null) dr.mDrawableTop.setCallback(null);
                    dr.mDrawableTop = null;
                    if (dr.mDrawableRight != null) dr.mDrawableRight.setCallback(null);
                    dr.mDrawableRight = null;
                    if (dr.mDrawableBottom != null) dr.mDrawableBottom.setCallback(null);
                    dr.mDrawableBottom = null;
                    dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
                    dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
                    dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
                    dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
                }
            }
        } else {
            if (dr == null) {
                mDrawables = dr = new Drawables(getContext());
            }
            mDrawables.mOverride = false;
            if (dr.mDrawableLeft != left && dr.mDrawableLeft != null) {
                dr.mDrawableLeft.setCallback(null);
            }
            dr.mDrawableLeft = left;
            if (dr.mDrawableTop != top && dr.mDrawableTop != null) {
                dr.mDrawableTop.setCallback(null);
            }
            dr.mDrawableTop = top;
            if (dr.mDrawableRight != right && dr.mDrawableRight != null) {
                dr.mDrawableRight.setCallback(null);
            }
            dr.mDrawableRight = right;
            if (dr.mDrawableBottom != bottom && dr.mDrawableBottom != null) {
                dr.mDrawableBottom.setCallback(null);
            }
            dr.mDrawableBottom = bottom;
            final Rect compoundRect = dr.mCompoundRect;
            int[] state;
            state = getDrawableState();
            if (left != null) {
                left.setState(state);
                left.copyBounds(compoundRect);
                left.setCallback(this);
                dr.mDrawableSizeLeft = compoundRect.width();
                dr.mDrawableHeightLeft = compoundRect.height();
            } else {
                dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
            }
            if (right != null) {
                right.setState(state);
                right.copyBounds(compoundRect);
                right.setCallback(this);
                dr.mDrawableSizeRight = compoundRect.width();
                dr.mDrawableHeightRight = compoundRect.height();
            } else {
                dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
            }
            if (top != null) {
                top.setState(state);
                top.copyBounds(compoundRect);
                top.setCallback(this);
                dr.mDrawableSizeTop = compoundRect.height();
                dr.mDrawableWidthTop = compoundRect.width();
            } else {
                dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
            }
            if (bottom != null) {
                bottom.setState(state);
                bottom.copyBounds(compoundRect);
                bottom.setCallback(this);
                dr.mDrawableSizeBottom = compoundRect.height();
                dr.mDrawableWidthBottom = compoundRect.width();
            } else {
                dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
            }
        }
// Save initial left/right drawables
        if (dr != null) {
            dr.mDrawableLeftInitial = left;
            dr.mDrawableRightInitial = right;
        }
        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    /**
     * @hide
     */
    protected void resetResolvedDrawables() {
        super.resetResolvedDrawables();
        mLastLayoutDirection = -1;
    }

    /**
     * Sets the extent by which text should be stretched horizontally.
     *
     * @attr ref android.R.styleable#TextView_textScaleX
     */
    @RemotableViewMethod
    public void setTextScaleX(float size) {
        if (size != mTextPaint.getTextScaleX()) {
            mUserSetTextScaleX = true;
            mTextPaint.setTextScaleX(size);
            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Set whether the TextView includes extra top and bottom padding to make
     * room for accents that go above the normal ascent and descent.
     * The default is true.
     *
     * @see #getIncludeFontPadding()
     *
     * @attr ref android.R.styleable#TextView_includeFontPadding
     */
    public void setIncludeFontPadding(boolean includepad) {
        if (mIncludePad != includepad) {
            mIncludePad = includepad;
            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Sets whether the text should be allowed to be wider than the
     * View is. If false, it will be wrapped to the width of the View.
     *
     * @attr ref android.R.styleable#TextView_scrollHorizontally
     */
    public void setHorizontallyScrolling(boolean whether) {
        if (mHorizontallyScrolling != whether) {
            mHorizontallyScrolling = whether;
            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Makes the TextView at least this many lines tall.
     *
     * Setting this value overrides any other (minimum) height setting. A single line TextView will
     * set this value to 1.
     *
     * @see #getMinLines()
     *
     * @attr ref android.R.styleable#TextView_minLines
     */
    @RemotableViewMethod
    public void setMinLines(int minlines) {
        mMinimum = minlines;
        mMinMode = LINES;
        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView at most this many lines tall.
     *
     * Setting this value overrides any other (maximum) height setting.
     *
     * @attr ref android.R.styleable#TextView_maxLines
     */
    @RemotableViewMethod
    public void setMaxLines(int maxlines) {
        mMaximum = maxlines;
        mMaxMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * @return the maximum number of lines displayed in this TextView, or -1 if the maximum
     * height was set in pixels instead using {@link #setMaxHeight(int) or #setHeight(int)}.
     *
     * @see #setMaxLines(int)
     *
     * @attr ref android.R.styleable#TextView_maxLines
     */
    public int getMaxLines() {
        return mMaxMode == LINES ? mMaximum : -1;
    }

    /**
     * Makes the TextView at most this many pixels tall.  This option is mutually exclusive with the
     * {@link #setMaxLines(int)} method.
     *
     * Setting this value overrides any other (maximum) number of lines setting.
     *
     * @attr ref android.R.styleable#TextView_maxHeight
     */
    @RemotableViewMethod
    public void setMaxHeight(int maxHeight) {
        mMaximum = maxHeight;
        mMaxMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView exactly this many lines tall.
     *
     * Note that setting this value overrides any other (minimum / maximum) number of lines or
     * height setting. A single line TextView will set this value to 1.
     *
     * @attr ref android.R.styleable#TextView_lines
     */
    @RemotableViewMethod
    public void setLines(int lines) {
        mMaximum = mMinimum = lines;
        mMaxMode = mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView exactly this many pixels tall.
     * You could do the same thing by specifying this number in the
     * LayoutParams.
     *
     * Note that setting this value overrides any other (minimum / maximum) number of lines or
     * height setting.
     *
     * @attr ref android.R.styleable#TextView_height
     */
    @RemotableViewMethod
    public void setHeight(int pixels) {
        mMaximum = mMinimum = pixels;
        mMaxMode = mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView at least this many ems wide
     *
     * @attr ref android.R.styleable#TextView_minEms
     */
    @RemotableViewMethod
    public void setMinEms(int minems) {
        mMinWidth = minems;
        mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the minimum width of the TextView, expressed in ems or -1 if the minimum width
     * was set in pixels instead (using {@link #setMinWidth(int)} or {@link #setWidth(int)}).
     *
     * @see #setMinEms(int)
     * @see #setEms(int)
     *
     * @attr ref android.R.styleable#TextView_minEms
     */
    public int getMinEms() {
        return mMinWidthMode == EMS ? mMinWidth : -1;
    }

    /**
     * Makes the TextView at least this many pixels wide
     *
     * @attr ref android.R.styleable#TextView_minWidth
     */
    @RemotableViewMethod
    public void setMinWidth(int minpixels) {
        mMinWidth = minpixels;
        mMinWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the minimum width of the TextView, in pixels or -1 if the minimum width
     * was set in ems instead (using {@link #setMinEms(int)} or {@link #setEms(int)}).
     *
     * @see #setMinWidth(int)
     * @see #setWidth(int)
     *
     * @attr ref android.R.styleable#TextView_minWidth
     */
    public int getMinWidth() {
        return mMinWidthMode == PIXELS ? mMinWidth : -1;
    }

    /**
     * Makes the TextView at most this many ems wide
     *
     * @attr ref android.R.styleable#TextView_maxEms
     */
    @RemotableViewMethod
    public void setMaxEms(int maxems) {
        mMaxWidth = maxems;
        mMaxWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the maximum width of the TextView, expressed in ems or -1 if the maximum width
     * was set in pixels instead (using {@link #setMaxWidth(int)} or {@link #setWidth(int)}).
     *
     * @see #setMaxEms(int)
     * @see #setEms(int)
     *
     * @attr ref android.R.styleable#TextView_maxEms
     */
    public int getMaxEms() {
        return mMaxWidthMode == EMS ? mMaxWidth : -1;
    }

    /**
     * Makes the TextView at most this many pixels wide
     *
     * @attr ref android.R.styleable#TextView_maxWidth
     */
    @RemotableViewMethod
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;
        mMaxWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the maximum width of the TextView, in pixels or -1 if the maximum width
     * was set in ems instead (using {@link #setMaxEms(int)} or {@link #setEms(int)}).
     *
     * @see #setMaxWidth(int)
     * @see #setWidth(int)
     *
     * @attr ref android.R.styleable#TextView_maxWidth
     */
    public int getMaxWidth() {
        return mMaxWidthMode == PIXELS ? mMaxWidth : -1;
    }

    /**
     * Makes the TextView exactly this many ems wide
     *
     * @see #setMaxEms(int)
     * @see #setMinEms(int)
     * @see #getMinEms()
     * @see #getMaxEms()
     *
     * @attr ref android.R.styleable#TextView_ems
     */
    @RemotableViewMethod
    public void setEms(int ems) {
        mMaxWidth = mMinWidth = ems;
        mMaxWidthMode = mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView exactly this many pixels wide.
     * You could do the same thing by specifying this number in the
     * LayoutParams.
     *
     * @see #setMaxWidth(int)
     * @see #setMinWidth(int)
     * @see #getMinWidth()
     * @see #getMaxWidth()
     *
     * @attr ref android.R.styleable#TextView_width
     */
    @RemotableViewMethod
    public void setWidth(int pixels) {
        mMaxWidth = mMinWidth = pixels;
        mMaxWidthMode = mMinWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Sets line spacing for this TextView.  Each line will have its height
     * multiplied by <code>mult</code> and have <code>add</code> added to it.
     *
     * @attr ref android.R.styleable#TextView_lineSpacingExtra
     * @attr ref android.R.styleable#TextView_lineSpacingMultiplier
     */
    public void setLineSpacing(float add, float mult) {
        if (mSpacingAdd != add || mSpacingMult != mult) {
            mSpacingAdd = add;
            mSpacingMult = mult;

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Gets the line spacing multiplier
     *
     * @return the value by which each line's height is multiplied to get its actual height.
     *
     * @see #setLineSpacing(float, float)
     * @see #getLineSpacingExtra()
     *
     * @attr ref android.R.styleable#TextView_lineSpacingMultiplier
     */
    public float getLineSpacingMultiplier() {
        return mSpacingMult;
    }

    /**
     * Gets the line spacing extra space
     *
     * @return the extra space that is added to the height of each lines of this TextView.
     *
     * @see #setLineSpacing(float, float)
     * @see #getLineSpacingMultiplier()
     *
     * @attr ref android.R.styleable#TextView_lineSpacingExtra
     */
    public float getLineSpacingExtra() {
        return mSpacingAdd;
    }

    /**
     * Sets the horizontal alignment of the text and the
     * vertical gravity that will be used when there is extra space
     * in the TextView beyond what is required for the text itself.
     *
     * @see android.view.Gravity
     * @attr ref android.R.styleable#TextView_gravity
     */
    public void setGravity(int gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.START;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.TOP;
        }

        boolean newLayout = false;

        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) !=
                (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)) {
            newLayout = true;
        }

        if (gravity != mGravity) {
            invalidate();
        }

        mGravity = gravity;

        if (mLayout != null && newLayout) {
            // XXX this is heavy-handed because no actual content changes.
            int want = mLayout.getWidth();
            int hintWant = mHintLayout == null ? 0 : mHintLayout.getWidth();

            makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING,
                    mRight - mLeft - getCompoundPaddingLeft() -
                            getCompoundPaddingRight(), true);
        }
    }

    private class ChangeWatcher implements TextWatcher, SpanWatcher {

        private CharSequence mBeforeText;

        public void beforeTextChanged(CharSequence buffer, int start,
                                      int before, int after) {
            if (DEBUG_EXTRACT) Log.v(LOG_TAG, "beforeTextChanged start=" + start
                    + " before=" + before + " after=" + after + ": " + buffer);

            if (AccessibilityManager.getInstance(mContext).isEnabled()
                    && ((!isPasswordInputType(getInputType()) && !hasPasswordTransformationMethod())
                    || shouldSpeakPasswordsForAccessibility())) {
                mBeforeText = buffer.toString();
            }

            TextView.this.sendBeforeTextChanged(buffer, start, before, after);
        }

        public void onTextChanged(CharSequence buffer, int start, int before, int after) {
            if (DEBUG_EXTRACT) Log.v(LOG_TAG, "onTextChanged start=" + start
                    + " before=" + before + " after=" + after + ": " + buffer);
            TextView.this.handleTextChanged(buffer, start, before, after);

            if (AccessibilityManager.getInstance(mContext).isEnabled() &&
                    (isFocused() || isSelected() && isShown())) {
                sendAccessibilityEventTypeViewTextChanged(mBeforeText, start, before, after);
                mBeforeText = null;
            }
        }

        public void afterTextChanged(Editable buffer) {
            if (DEBUG_EXTRACT) Log.v(LOG_TAG, "afterTextChanged: " + buffer);
            TextView.this.sendAfterTextChanged(buffer);

            if (MetaKeyKeyListener.getMetaState(buffer, MetaKeyKeyListener.META_SELECTING) != 0) {
                MetaKeyKeyListener.stopSelecting(TextView.this, buffer);
            }
        }

        public void onSpanChanged(Spannable buf, Object what, int s, int e, int st, int en) {
            if (DEBUG_EXTRACT) Log.v(LOG_TAG, "onSpanChanged s=" + s + " e=" + e
                    + " st=" + st + " en=" + en + " what=" + what + ": " + buf);
            TextView.this.spanChange(buf, what, s, st, e, en);
        }

        public void onSpanAdded(Spannable buf, Object what, int s, int e) {
            if (DEBUG_EXTRACT) Log.v(LOG_TAG, "onSpanAdded s=" + s + " e=" + e
                    + " what=" + what + ": " + buf);
            TextView.this.spanChange(buf, what, -1, s, -1, e);
        }

        public void onSpanRemoved(Spannable buf, Object what, int s, int e) {
            if (DEBUG_EXTRACT) Log.v(LOG_TAG, "onSpanRemoved s=" + s + " e=" + e
                    + " what=" + what + ": " + buf);
            TextView.this.spanChange(buf, what, s, -1, e, -1);
        }
    }

}

