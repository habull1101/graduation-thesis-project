package com.example.vpnapp;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class ListViewAdapter extends BaseAdapter {
    private ArrayList<Pair<String, Boolean>> listPackageProtect;
    private ArrayList<Pair<String, Boolean>> listPackageProtectTmp;

    public ListViewAdapter() {
        listPackageProtect = PackageProtectManager.getInstance().getListPackageProtect();
        listPackageProtectTmp = new ArrayList<>();
        listPackageProtectTmp.addAll(listPackageProtect);
    }

    public ArrayList<Pair<String, Boolean>> getListPackageProtectTmp() {
        return listPackageProtectTmp;
    }

    @Override
    public int getCount() {
        return listPackageProtect.size();
    }

    @Override
    public Object getItem(int position) {
        return listPackageProtect.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = View.inflate(parent.getContext(), R.layout.item_list_view, null);

        TextView nameApp = view.findViewById(R.id.tv_name_app);
        TextView namePackage = view.findViewById(R.id.tv_name_package);
        ShapeableImageView avtApp = view.findViewById(R.id.img_avt);
        SwitchCompat toggle = view.findViewById(R.id.toggle);

        String packageName = listPackageProtect.get(position).first;
        try {
            PackageManager pm = MainActivity.context.getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);

            nameApp.setText(pm.getApplicationLabel(applicationInfo));
            namePackage.setText(listPackageProtect.get(position).first);
            Drawable avt = pm.getApplicationIcon(packageName);
            avtApp.setImageDrawable(avt);
            toggle.setChecked(listPackageProtect.get(position).second);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (int i = 0; i < listPackageProtectTmp.size(); i++) {
                    Pair<String, Boolean> tmp = listPackageProtectTmp.get(i);
                    if (tmp.first.equals(packageName)) {
                        listPackageProtectTmp.set(i, new Pair<>(packageName, isChecked));
                    }
                }
            }
        });

        return view;
    }
}