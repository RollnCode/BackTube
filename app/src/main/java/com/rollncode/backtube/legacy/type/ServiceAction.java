package com.rollncode.backtube.legacy.type;

import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Chekashov R.(email:roman_woland@mail.ru)
 * @since 27.04.17
 */
@StringDef({ServiceAction.HIDE, ServiceAction.SHOW, ServiceAction.START, ServiceAction.STOP})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceAction {

    String HIDE = "com.rollncode.backtube.ServiceAction.ACTION_0";
    String SHOW = "com.rollncode.backtube.ServiceAction.ACTION_1";
    String START = "com.rollncode.backtube.ServiceAction.ACTION_2";
    String STOP = "com.rollncode.backtube.ServiceAction.ACTION_3";
}