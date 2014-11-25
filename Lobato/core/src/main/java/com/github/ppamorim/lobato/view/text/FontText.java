/*
 * Copyright 2014 Leonardo Rossetto - Pedro Paulo de Amorim
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

package com.github.ppamorim.lobato.view.text;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.github.ppamorim.lobato.utils.Utils;

public class FontText extends TextView {

    public FontText(Context context) {
        super(context);
    }

    public FontText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomFont(context, attrs);
    }

    public FontText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomFont(context, attrs);
    }

    protected void setCustomFont(Context context, AttributeSet attrs) {
        Utils.applyFont(context, this, attrs);
    }

    public boolean setCustomFont(Context context, String font) {
        return Utils.applyFont(context, this, font);
    }

}