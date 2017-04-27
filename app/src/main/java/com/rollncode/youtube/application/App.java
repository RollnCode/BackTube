package com.rollncode.youtube.application;

import android.app.Application;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AContext.init(this);
    }
}
