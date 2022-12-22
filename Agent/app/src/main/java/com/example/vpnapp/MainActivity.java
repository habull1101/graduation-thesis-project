package com.example.vpnapp;

import static com.example.vpnapp.VpnServices.STATUS_CONNECTED;
import static com.example.vpnapp.VpnServices.STATUS_CONNECTING;
import static com.example.vpnapp.VpnServices.STATUS_DISCONNECTING;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Pair;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vpnapp.utils.AppInfoUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static Context context;

    ConstraintLayout btnStartStop;
    TextView tvStatusValue;
    Chronometer chronometerTimeConnect;
    TextView tvIntroSecurity;
    TextView tvIntroInsecurity;
    ListView lvListApp;

    TextView tvNumPackageValue;
    LinearLayout btnSave;
    LinearLayout btnCancel;
    BottomSheetBehavior bottomSheetBehavior;

    ListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set color for status bar and icon status bar
        getWindow().setStatusBarColor(getResources().getColor(R.color.blue_main_background));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getInsetsController().setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }

        setContentView(R.layout.activity_main);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setListPackageProtect();
        tvNumPackageValue.setText(PackageProtectManager.getInstance().numPackageProtect() + "/" +
                PackageProtectManager.getInstance().getListPackageProtect().size());
    }

    public void init() {
        context = getApplicationContext();
        setListPackageProtect();

        btnStartStop = findViewById(R.id.btn_startStop);
        tvStatusValue = findViewById(R.id.tv_status_value);
        tvIntroSecurity = findViewById(R.id.tv_intro_secure);
        tvIntroInsecurity = findViewById(R.id.tv_intro_insecure);
        lvListApp = findViewById(R.id.lv_list_app);
        chronometerTimeConnect = findViewById(R.id.chronometer_timeConnect);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);
        GifImageView iconExpand = findViewById(R.id.icon_expanded);
        tvNumPackageValue = findViewById(R.id.tv_value);
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet));

        // set adapter for listview
        adapter = new ListViewAdapter();
        lvListApp.setAdapter(adapter);

        statusConnectObserve();
        btnStartStop.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        chronometerTimeConnect.setOnChronometerTickListener(cArg -> {
            long time = SystemClock.elapsedRealtime() - cArg.getBase();
            int h = (int)(time /3600000);
            int m = (int)(time - h*3600000)/60000;
            int s = (int)(time - h*3600000- m*60000)/1000 ;
            String hh = h < 10 ? "0" + h: h + "";
            String mm = m < 10 ? "0" + m: m + "";
            String ss = s < 10 ? "0" + s: s + "";
            cArg.setText(hh + ":" + mm + ":" + ss);
        });

        tvNumPackageValue.setText(PackageProtectManager.getInstance().numPackageProtect() + "/" +
                PackageProtectManager.getInstance().getListPackageProtect().size());
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setDraggable(true);
                    iconExpand.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                    tvNumPackageValue.setText(PackageProtectManager.getInstance().numPackageProtect() + "/" +
                            PackageProtectManager.getInstance().getListPackageProtect().size());
                    saveListPackageProtect();
                } else {
                    bottomSheetBehavior.setDraggable(false);
                    iconExpand.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    public void startVpnServices() {
        VpnServices.statusConnect.postValue(STATUS_CONNECTING);

        Intent vpnIntent = VpnService.prepare(getApplication());
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, 0);
        else
            onActivityResult(0, RESULT_OK, null);
    }

    public void stopVpnServices() {
        VpnServices.statusConnect.postValue(STATUS_DISCONNECTING);

        Intent intent = new Intent("stopVpnServices");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            Intent intentTmp = new Intent(this, VpnServices.class);
            startService(intentTmp);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_startStop:
                if (VpnServices.statusConnect.getValue() == VpnServices.STATUS_DISCONNECTED) {
                    VpnServices.statusConnect.postValue(STATUS_CONNECTING);
                    startVpnServices();
                } else {
                    VpnServices.statusConnect.postValue(STATUS_DISCONNECTING);
                    stopVpnServices();
                }

                break;

            case R.id.btn_save:
                PackageProtectManager.getInstance().setListPackageProtect(adapter.getListPackageProtectTmp());
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                Toast.makeText(MainActivity.this, "Cập nhật danh sách ứng dụng thành công", Toast.LENGTH_SHORT).show();

                break;

            case R.id.btn_cancel:
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                break;
        }
    }

    public void statusConnectObserve() {
        VpnServices.statusConnect.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer statusConnect) {
                switch (statusConnect) {
                    case STATUS_CONNECTING:
                        tvStatusValue.setText(R.string.connecting);
                        break;

                    case STATUS_CONNECTED:
                        tvStatusValue.setText(R.string.connected);

                        chronometerTimeConnect.setBase(SystemClock.elapsedRealtime());
                        chronometerTimeConnect.start();

                        tvIntroSecurity.setVisibility(View.VISIBLE);
                        tvIntroInsecurity.setVisibility(View.GONE);

                        break;

                    case STATUS_DISCONNECTING:
                        tvStatusValue.setText(R.string.disconnecting);
                        break;

                    default:
                        tvStatusValue.setText(R.string.disconnected);

                        chronometerTimeConnect.stop();
                        chronometerTimeConnect.setBase(SystemClock.elapsedRealtime());
                        chronometerTimeConnect.setText("--:--:--");

                        tvIntroInsecurity.setVisibility(View.VISIBLE);
                        tvIntroSecurity.setVisibility(View.GONE);

                        Intent intent = new Intent("stopVpnServices");
                        LocalBroadcastManager.getInstance(MainActivity.context).sendBroadcast(intent);

                        break;
                }
            }
        });
    }

    public void setListPackageProtect() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        String resultString = prefs.getString("listPackageProtect", "");

        ArrayList<Pair<String, Boolean>> listPackageProtect = new ArrayList<>();
        ArrayList<Pair<String, Boolean>> listPackageProtectFromPrefs = getListPackageProtectFromPrefs();

        List<PackageInfo> tmp = getPackageManager().getInstalledPackages(PackageManager.GET_PERMISSIONS);
        for (PackageInfo i : tmp) {
            if (i.packageName.equals(getPackageName()) || AppInfoUtils.isSystemApp(this, i.packageName)) {
                continue;
            }

            String[] listPermission = i.requestedPermissions;
            if (listPermission != null) {
                for (int j = 0; j < listPermission.length; j++) {
                    if (listPermission[j].equals("android.permission.INTERNET")
                            && ((i.requestedPermissionsFlags[j] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0)) {
                        listPackageProtect.add(new Pair<>(i.packageName, true));
                        break;
                    }
                }
            }
        }

        if (listPackageProtect.size() != listPackageProtectFromPrefs.size()) {
            for (int i = 0; i < listPackageProtect.size(); i++) {
                Pair<String, Boolean> packageItem = listPackageProtect.get(i);

                for (Pair<String, Boolean> packageItem2: listPackageProtectFromPrefs) {
                    if (packageItem2.first.equals(packageItem.first)) {
                        listPackageProtect.set(i, packageItem2);
                        break;
                    }
                }
            }
        }

        if (resultString.isEmpty() || listPackageProtect.size() != listPackageProtectFromPrefs.size()) {
            sortListPackageProtect(listPackageProtect);
            PackageProtectManager.getInstance().setListPackageProtect(listPackageProtect);
        } else {
            sortListPackageProtect(listPackageProtectFromPrefs);
            PackageProtectManager.getInstance().setListPackageProtect(listPackageProtectFromPrefs);
        }
    }

    public void saveListPackageProtect() {
        JSONObject jsonObject = new JSONObject();

        ArrayList<Pair<String, Boolean>> listPackageProtect = PackageProtectManager.getInstance().getListPackageProtect();
        for (Pair<String, Boolean> packageItem: listPackageProtect) {
            try {
                jsonObject.put(packageItem.first, packageItem.second);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("listPackageProtect", jsonObject.toString());
        editor.commit();
    }

    public ArrayList<Pair<String, Boolean>> getListPackageProtectFromPrefs() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        String resultString = prefs.getString("listPackageProtect", "");

        JSONObject obj;
        ArrayList<Pair<String, Boolean>> result = new ArrayList<>();

        try {
            obj = new JSONObject(resultString);
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                result.add(new Pair<>(key, obj.getBoolean(key)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    public void sortListPackageProtect(ArrayList<Pair<String, Boolean>> list) {
        list.sort(new Comparator<Pair<String, Boolean>>() {
            @Override
            public int compare(Pair<String, Boolean> o1, Pair<String, Boolean> o2) {
                PackageManager pm = MainActivity.context.getPackageManager();
                try {
                    ApplicationInfo applicationInfo1 = pm.getApplicationInfo(o1.first, 0);
                    ApplicationInfo applicationInfo2 = pm.getApplicationInfo(o2.first, 0);

                    String name1 = pm.getApplicationLabel(applicationInfo1).toString();
                    String name2 = pm.getApplicationLabel(applicationInfo2).toString();

                    return name1.toLowerCase().compareTo(name2.toLowerCase());
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    return o1.first.toLowerCase().compareTo(o2.first.toLowerCase());
                }
            }
        });
    }
}