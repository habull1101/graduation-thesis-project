package com.example.vpnapp;

import android.util.Pair;

import java.util.ArrayList;

public class PackageProtectManager {
    private static final Object mLock = new Object();
    private static PackageProtectManager instance = null;

    private ArrayList<Pair<String, Boolean>> listPackageProtect = new ArrayList<>();

    public static PackageProtectManager getInstance() {
        synchronized (mLock) {
            if (instance == null) {
                instance = new PackageProtectManager();
            }
            return instance;
        }
    }

    public void setListPackageProtect(ArrayList<Pair<String, Boolean>> listPackageProtect) {
        this.listPackageProtect.clear();
        this.listPackageProtect.addAll(listPackageProtect);
    }

    public ArrayList<Pair<String, Boolean>> getListPackageProtect() {
        return listPackageProtect;
    }

    public int numPackageProtect() {
        int count = 0;

        for (Pair<String, Boolean> i: listPackageProtect) {
            if (i.second) {
                count++;
            }
        }

        return count;
    }
}
