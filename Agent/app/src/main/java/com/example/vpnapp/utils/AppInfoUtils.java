package com.example.vpnapp.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.lang.reflect.Method;

public class AppInfoUtils {
    public static boolean isSystemApp(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        return isAppInSystemPartition(pm, packageName) || isSignedBySystem(pm, packageName) || isPrivilegedApp(pm, packageName);
    }

    private static boolean isAppInSystemPartition(PackageManager pm, String packageName) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return ((ai.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean isSignedBySystem(PackageManager pm, String packageName) {
        try {
            Signature[] systemSignatures;
            Signature[] appSignatures;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo sysPackageInfo = pm.getPackageInfo("android", PackageManager.GET_SIGNING_CERTIFICATES);
                systemSignatures = sysPackageInfo.signingInfo.getApkContentsSigners();

                PackageInfo appPackageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                appSignatures = appPackageInfo.signingInfo.getApkContentsSigners();

            } else {
                PackageInfo pi_sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
                systemSignatures = pi_sys.signatures;

                PackageInfo pi_app = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
                appSignatures = pi_app.signatures;
            }
            String systemHash = HashUtils.getMd5BytesAsString(systemSignatures[0].toByteArray());
            String appHash = HashUtils.getMd5BytesAsString(appSignatures[0].toByteArray());
            boolean check = systemHash.equalsIgnoreCase(appHash) && systemSignatures[0] != null && appSignatures[0] != null;
            return check;
        } catch (Exception e) {
//            Logger.logE(e);
            return false;
        }
    }

    private static boolean isPrivilegedApp(PackageManager pm, String packageName) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            Method method = ApplicationInfo.class.getDeclaredMethod("isPrivilegedApp");
            return (Boolean) method.invoke(ai);
        } catch (Exception e) {
//            Logger.logE(e);
            return false;
        }
    }
}
