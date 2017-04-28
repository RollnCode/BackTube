package com.rollncode.youtube.utility;

import android.content.IntentFilter;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.rollncode.youtube.types.LinkType;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class Utils {

    @NonNull
    public static String parse(String string) {
        final Uri uri = Uri.parse(string);
        if (uri.getAuthority().equals(LinkType.SHORT)) {
            return uri.getLastPathSegment();
        }
        return uri.getQueryParameter("v");
    }

    @NonNull
    public static IntentFilter newIntentFilter(@NonNull String... actions) {
        final IntentFilter filter = new IntentFilter();
        for (String action : actions) {
            filter.addAction(action);
        }
        return filter;
    }

}
