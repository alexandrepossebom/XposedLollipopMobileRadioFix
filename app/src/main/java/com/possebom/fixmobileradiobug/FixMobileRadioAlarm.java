package com.possebom.fixmobileradiobug;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Created by alexandre on 22/05/15.
 */
public class FixMobileRadioAlarm implements IXposedHookLoadPackage {
    private long beforeAlarmSchedule;

    private static final String CLASS_NAME = "com.android.server.content.SyncManager$SyncHandler";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (!"android".equals(lpparam.packageName) || !"android".equals(lpparam.processName)) {
            return;
        }

        XposedBridge.log("Loaded app: " + lpparam.packageName);

        XposedHelpers.findAndHookMethod(CLASS_NAME, lpparam.classLoader,
                "manageSyncAlarmLocked", "long", "long", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                        beforeAlarmSchedule = XposedHelpers.getLongField(param.thisObject, "mAlarmScheduleTime");
                        XposedBridge.log("SyncManager before");
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("SyncManager after");
                        boolean shoudSet = XposedHelpers.getBooleanField(param.thisObject, "shoudSet");
                        boolean needAlarm = XposedHelpers.getBooleanField(param.thisObject, "needAlarm");

                        if (shoudSet && needAlarm) {
                            final boolean alarmIsActive = XposedHelpers.getBooleanField(param.thisObject, "alarmIsActive");
                            final long alarmTime = XposedHelpers.getLongField(param.thisObject, "alarmTime");

                            if (alarmIsActive && alarmTime <= beforeAlarmSchedule) {
                                final PendingIntent mSyncAlarmIntent = (PendingIntent) XposedHelpers.getObjectField(param.thisObject, "PendingIntent");
                                final AlarmManager mAlarmService = (AlarmManager) XposedHelpers.getObjectField(param.thisObject, "mAlarmService");
                                mAlarmService.cancel(mSyncAlarmIntent);
                                XposedHelpers.setObjectField(param.thisObject, "mAlarmScheduleTime", null);
                                XposedBridge.log("Alarm is canceled");
                            }
                        }

                    }
                });
    }
}
