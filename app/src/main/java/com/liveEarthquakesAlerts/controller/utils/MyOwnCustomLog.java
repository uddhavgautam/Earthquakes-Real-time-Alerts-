package com.liveEarthquakesAlerts.controller.utils;

import android.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by uddhav on 4/6/17.
 */

public class MyOwnCustomLog {
    public static List<Method> getStaticMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<Method>();
        for (Method method : clazz.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                methods.add(method);
            }
        }
        return Collections.unmodifiableList(methods);
    }

    public void addLog(String simpleName, String methodName, StackTraceElement[] stackTrace) {
        //add the log line
        Log.i(simpleName, "##############");
        int count = 0;
        for (StackTraceElement stackTraceElement : stackTrace) {
            count++;
            if (count == 3) {
                Log.i(simpleName, "\"" + methodName + "\" " + count + " " + methodName);

            } else
                Log.i(simpleName, "\"" + methodName + "\" " + count + " " + stackTraceElement.getMethodName());
        }
        ; //end of log line
    }
}
