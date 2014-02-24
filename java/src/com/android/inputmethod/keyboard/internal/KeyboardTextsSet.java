/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.keyboard.internal;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

import com.android.inputmethod.annotations.UsedForTesting;
import com.android.inputmethod.latin.Constants;
import com.android.inputmethod.latin.utils.CollectionUtils;

import java.util.HashMap;
import java.util.Locale;

public final class KeyboardTextsSet {
    public static final String PREFIX_TEXT = "!text/";
    public static final String SWITCH_TO_ALPHA_KEY_LABEL = "label_to_alpha_key";

    private static final char BACKSLASH = Constants.CODE_BACKSLASH;
    private static final int MAX_STRING_REFERENCE_INDIRECTION = 10;

    private String[] mTextsTable;
    // Resource name to text map.
    private HashMap<String, String> mResourceNameToTextsMap = CollectionUtils.newHashMap();

    public void setLocale(final Locale locale) {
        final String language = locale.getLanguage();
        mTextsTable = KeyboardTextsTable.getTextsTable(language);
    }

    public void loadStringResources(final Context context) {
        final int referenceId = context.getApplicationInfo().labelRes;
        loadStringResourcesInternal(context, RESOURCE_NAMES, referenceId);
    }

    @UsedForTesting
    void loadStringResourcesInternal(final Context context, final String[] resourceNames,
            final int referenceId) {
        final Resources res = context.getResources();
        final String packageName = res.getResourcePackageName(referenceId);
        for (final String resName : resourceNames) {
            final int resId = res.getIdentifier(resName, "string", packageName);
            mResourceNameToTextsMap.put(resName, res.getString(resId));
        }
    }

    public String getText(final String name) {
        final String text = mResourceNameToTextsMap.get(name);
        return (text != null) ? text : KeyboardTextsTable.getText(name, mTextsTable);
    }

    private static int searchTextNameEnd(final String text, final int start) {
        final int size = text.length();
        for (int pos = start; pos < size; pos++) {
            final char c = text.charAt(pos);
            // Label name should be consisted of [a-zA-Z_0-9].
            if ((c >= 'a' && c <= 'z') || c == '_' || (c >= '0' && c <= '9')) {
                continue;
            }
            return pos;
        }
        return size;
    }

    public String resolveTextReference(final String rawText) {
        if (TextUtils.isEmpty(rawText)) {
            return null;
        }
        int level = 0;
        String text = rawText;
        StringBuilder sb;
        do {
            level++;
            if (level >= MAX_STRING_REFERENCE_INDIRECTION) {
                throw new RuntimeException("Too many " + PREFIX_TEXT + "name indirection: " + text);
            }

            final int prefixLen = PREFIX_TEXT.length();
            final int size = text.length();
            if (size < prefixLen) {
                break;
            }

            sb = null;
            for (int pos = 0; pos < size; pos++) {
                final char c = text.charAt(pos);
                if (text.startsWith(PREFIX_TEXT, pos)) {
                    if (sb == null) {
                        sb = new StringBuilder(text.substring(0, pos));
                    }
                    final int end = searchTextNameEnd(text, pos + prefixLen);
                    final String name = text.substring(pos + prefixLen, end);
                    sb.append(getText(name));
                    pos = end - 1;
                } else if (c == BACKSLASH) {
                    if (sb != null) {
                        // Append both escape character and escaped character.
                        sb.append(text.substring(pos, Math.min(pos + 2, size)));
                    }
                    pos++;
                } else if (sb != null) {
                    sb.append(c);
                }
            }

            if (sb != null) {
                text = sb.toString();
            }
        } while (sb != null);
        return TextUtils.isEmpty(text) ? null : text;
    }

    // These texts' name should be aligned with the @string/<name> in
    // values*/strings-action-keys.xml.
    private static final String[] RESOURCE_NAMES = {
        // Labels for action.
        "label_go_key",
        "label_send_key",
        "label_next_key",
        "label_done_key",
        "label_previous_key",
        // Other labels.
        "label_pause_key",
        "label_wait_key",
    };
}
