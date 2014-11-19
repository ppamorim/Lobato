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

package org.ppamorim.lobato.base;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.HashMap;

public class VariableTextView extends TextView {

    private HashMap<String, Typeface> mHashTypeFace = new HashMap<String, Typeface>();

    public VariableTextView(Context context) {
        super(context);
    }

    public VariableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VariableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setTypeface(Typeface tf) {
        super.setTypeface(tf);
    }

    private void setHashTypeFace(HashMap<String, Typeface> hashTypeFace) {
        mHashTypeFace = hashTypeFace;
    }
}
