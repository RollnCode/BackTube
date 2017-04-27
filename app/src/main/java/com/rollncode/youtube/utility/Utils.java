package com.rollncode.youtube.utility;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.rollncode.youtube.service.PlayerService;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class Utils {

    public interface LinkType {
        public static final String NORMAL = "youtube.com";
        public static final String SHORT = "youtu.be";
    }

    public static String parse(Uri uri) {
        if (uri.getAuthority().equals(LinkType.SHORT)) {
            return uri.getLastPathSegment();
        }
        return uri.getQueryParameter("v");
    }

    public static String parse(String string) {
        return parse(Uri.parse(string));
    }

    public static void toLog(@Nullable String message) {
        Log.d(PlayerService.class.getSimpleName(), ">>> " + message);
    }

}
