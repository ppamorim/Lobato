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

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class CustomView extends RelativeLayout {

    public final static String MATERIAL_DESIGN_XML = "http://schemas.android.com/apk/res-auto";
    public final static String ANDROID_XML = "http://schemas.android.com/apk/res/android";

    public final int disabledBackgroundColor = getResources().getColor(R.color.lobato_disabled_view);
    public int beforeBackground = getResources().getColor(R.color.lobato_reset_view);

    public boolean isLastTouch = false;

    public CustomView(Context context) {
        super(context);
    }

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public CustomView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setCustomFont(Context context, AttributeSet attrs) {
//        Utils.applyFont(context, this, attrs);
    }

    public boolean setCustomFont(Context context, String font) {
//        return Utils.applyFont(context, this, font);
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if(enabled) {
            setBackgroundColor(beforeBackground);
        } else {
            setBackgroundColor(disabledBackgroundColor);
        }
    }

}