package com.rollncode.youtube.types;

import android.support.annotation.NonNull;

import java.util.NoSuchElementException;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public enum PlayerAction {

    BASE("com.kkx.player.ACTION_", 0),
    HIDE_YOUTUBE("com.kkx.player.ACTION_HIDE_YOUTUBE", 1),
    SHOW_YOUTUBE("com.kkx.player.ACTION_SHOW_YOUTUBE", 2),
    START_YOUTUBE("com.kkx.player.ACTION_START_YOUTUBE", 3),
    STOP_YOUTUBE("com.kkx.player.ACTION_STOP_YOUTUBE", 4);

    private final String mName;
    private final int mCode;

    PlayerAction(String name, int code) {
        mName = name;
        mCode = code;
    }

    public String getName() {
        return mName;
    }

    @NonNull
    public static PlayerAction get(int code) {
        for (PlayerAction action : values()) {
            if (code == action.mCode) {
                return action;
            }
        }

        throw new NoSuchElementException();
    }

    @NonNull
    public static PlayerAction get(String name) {
        for (PlayerAction action : values()) {
            if (name.equals(action.mName)) {
                return action;
            }
        }

        throw new NoSuchElementException();
    }
}
