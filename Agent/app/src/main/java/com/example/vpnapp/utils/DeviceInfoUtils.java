package com.example.vpnapp.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class DeviceInfoUtils {

    public static ArrayList<String> listAllPackageInstalled(Context context) {
        ArrayList<String> result = new ArrayList<>();
        List<PackageInfo> tmp = context.getPackageManager().getInstalledPackages(PackageManager.GET_META_DATA);
        for (PackageInfo i: tmp) {
            if (!AppInfoUtils.isSystemApp(context, i.packageName)) {
                result.add(i.packageName);
            }
        }

        return result;
    }
}
