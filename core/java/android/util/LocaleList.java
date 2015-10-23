/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.util;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.Size;

import com.android.internal.annotations.GuardedBy;

import java.util.HashSet;
import java.util.Locale;

// TODO: We don't except too many LocaleLists to exist at the same time, and
// we need access to the data at native level, so we should pass the data
// down to the native level, create a map of every list seen there, take a
// pointer back, and just keep that pointer in the Java-level object, so
// things could be copied very quickly.

/**
 * LocaleList is an immutable list of Locales, typically used to keep an
 * ordered user preferences for locales.
 */
public final class LocaleList {
    private final Locale[] mList;
    // This is a comma-separated list of the locales in the LocaleList created at construction time,
    // basically the result of running each locale's toLanguageTag() method and concatenating them
    // with commas in between.
    private final String mStringRepresentation;

    private static final Locale[] sEmptyList = new Locale[0];
    private static final LocaleList sEmptyLocaleList = new LocaleList();

    public Locale get(int location) {
        return location < mList.length ? mList[location] : null;
    }

    public Locale getPrimary() {
        return mList.length == 0 ? null : get(0);
    }

    public boolean isEmpty() {
        return mList.length == 0;
    }

    public int size() {
        return mList.length;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (!(other instanceof LocaleList))
            return false;
        final Locale[] otherList = ((LocaleList) other).mList;
        if (mList.length != otherList.length)
            return false;
        for (int i = 0; i < mList.length; ++i) {
            if (!mList[i].equals(otherList[i]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < mList.length; ++i) {
            result = 31 * result + mList[i].hashCode();
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < mList.length; ++i) {
            sb.append(mList[i]);
            if (i < mList.length - 1) {
                sb.append(',');
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @NonNull
    public String toLanguageTags() {
        return mStringRepresentation;
    }

    /**
     * It is almost always better to call {@link #getEmptyLocaleList()} instead which returns
     * a pre-constructed empty locale list.
     */
    public LocaleList() {
        mList = sEmptyList;
        mStringRepresentation = "";
    }

    /**
     * @throws NullPointerException if any of the input locales is <code>null</code>.
     * @throws IllegalArgumentException if any of the input locales repeat.
     */
    public LocaleList(@Nullable Locale locale) {
        if (locale == null) {
            mList = sEmptyList;
            mStringRepresentation = "";
        } else {
            mList = new Locale[1];
            mList[0] = (Locale) locale.clone();
            mStringRepresentation = locale.toLanguageTag();
        }
    }

    /**
     * @throws NullPointerException if any of the input locales is <code>null</code>.
     * @throws IllegalArgumentException if any of the input locales repeat.
     */
    public LocaleList(@Nullable Locale[] list) {
        if (list == null || list.length == 0) {
            mList = sEmptyList;
            mStringRepresentation = "";
        } else {
            final Locale[] localeList = new Locale[list.length];
            final HashSet<Locale> seenLocales = new HashSet<Locale>();
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.length; ++i) {
                final Locale l = list[i];
                if (l == null) {
                    throw new NullPointerException();
                } else if (seenLocales.contains(l)) {
                    throw new IllegalArgumentException();
                } else {
                    final Locale localeClone = (Locale) l.clone();
                    localeList[i] = localeClone;
                    sb.append(localeClone.toLanguageTag());
                    if (i < list.length - 1) {
                        sb.append(',');
                    }
                    seenLocales.add(localeClone);
                }
            }
            mList = localeList;
            mStringRepresentation = sb.toString();
        }
    }

    public static LocaleList getEmptyLocaleList() {
        return sEmptyLocaleList;
    }

    public static LocaleList forLanguageTags(@Nullable String list) {
        if (list == null || list.equals("")) {
            return getEmptyLocaleList();
        } else {
            final String[] tags = list.split(",");
            final Locale[] localeArray = new Locale[tags.length];
            for (int i = 0; i < localeArray.length; ++i) {
                localeArray[i] = Locale.forLanguageTag(tags[i]);
            }
            return new LocaleList(localeArray);
        }
    }

    private final static Object sLock = new Object();

    @GuardedBy("sLock")
    private static LocaleList sDefaultLocaleList;

    // TODO: fix this to return the default system locale list once we have that
    @NonNull @Size(min=1)
    public static LocaleList getDefault() {
        Locale defaultLocale = Locale.getDefault();
        synchronized (sLock) {
            if (sDefaultLocaleList == null || sDefaultLocaleList.size() != 1
                    || !defaultLocale.equals(sDefaultLocaleList.getPrimary())) {
                sDefaultLocaleList = new LocaleList(defaultLocale);
            }
        }
        return sDefaultLocaleList;
    }
}
