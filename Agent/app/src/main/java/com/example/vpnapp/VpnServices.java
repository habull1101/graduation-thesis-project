package com.example.vpnapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.vpnapp.packet.Packet;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VpnServices extends VpnService {
    static final int STATUS_CONNECTING = 1;
    static final int STATUS_CONNECTED = 2;
    static final int STATUS_DISCONNECTING = 3;
    static final int STATUS_DISCONNECTED = 4;

    private ParcelFileDescriptor tunnel = null;

    private final String vpnAddress = "10.0.0.2"; //Only IPv4 support for now
    private final String vpnRoute = "0.0.0.0"; //Intercept all

    private BlockingQueue<Packet> deviceToNetworkTCPQueue;
    private BlockingQueue<Packet> deviceToNetworkUDPQueue;
    private BlockingQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;

    public static MutableLiveData<Integer> statusConnect = new MutableLiveData<>(STATUS_DISCONNECTED);

    private BroadcastReceiver broadcastStopVpn = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("stopVpnServices".equals(intent.getAction())) {
                stopVpn();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();

        deviceToNetworkTCPQueue = new ArrayBlockingQueue<Packet>(1000);
        deviceToNetworkUDPQueue = new ArrayBlockingQueue<Packet>(1000);
        networkToDeviceQueue = new ArrayBlockingQueue<ByteBuffer>(1000);
        executorService = Executors.newFixedThreadPool(10);

        setupVPN();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(broadcastStopVpn, new IntentFilter("stopVpnServices"));

        executorService.submit(new HandleTcp(deviceToNetworkTCPQueue, networkToDeviceQueue, this));
        executorService.submit(new mainRunnable(tunnel.getFileDescriptor(), deviceToNetworkUDPQueue,
                deviceToNetworkTCPQueue, networkToDeviceQueue));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setupVPN() {
        try {
            if (tunnel == null) {
                Builder builder = new Builder();

                builder.addDisallowedApplication(getApplicationContext().getPackageName());
                for (Pair<String, Boolean> i: PackageProtectManager.getInstance().getListPackageProtect()) {
                    if (!i.second) {
                        builder.addDisallowedApplication(i.first);
                    }
                }

                //config vpn
                builder.setSession("MyVPNService")
                        .addAddress(vpnAddress,32)
                        .addRoute(vpnRoute,0);

                tunnel = builder.establish();
            }
        } catch (Exception e) {
            Log.i("msg", "can not initial vpn");
            statusConnect.postValue(STATUS_DISCONNECTED);

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void stopVpn() {
        try {
            if (tunnel != null) {
                tunnel.close();
                tunnel = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        stopForeground(true);
        stopSelf();
        statusConnect.postValue(STATUS_DISCONNECTED);
    }

    private class mainRunnable implements Runnable {
        private FileDescriptor vpnFileDescriptor;

        private BlockingQueue<Packet> deviceToNetworkUDPQueue;
        private BlockingQueue<Packet> deviceToNetworkTCPQueue;
        private BlockingQueue<ByteBuffer> networkToDeviceQueue;

        public mainRunnable(FileDescriptor vpnFileDescriptor,
                           BlockingQueue<Packet> deviceToNetworkUDPQueue,
                           BlockingQueue<Packet> deviceToNetworkTCPQueue,
                           BlockingQueue<ByteBuffer> networkToDeviceQueue) {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
        }

        class WriteVpnThread implements Runnable {
            FileChannel vpnOut;
            private BlockingQueue<ByteBuffer> networkToDeviceQueue;

            WriteVpnThread(FileChannel vpnOut, BlockingQueue<ByteBuffer> networkToDeviceQueue) {
                this.vpnOut = vpnOut;
                this.networkToDeviceQueue = networkToDeviceQueue;
            }

            @Override
            public void run() {
                while (true) {
                    try {
                        ByteBuffer buffer = networkToDeviceQueue.take();
                        buffer.flip();
                        vpnOut.write(buffer);
                    } catch (Exception e) {
                        Log.i("msg", "WriteVpnThread fail", e);
                    }
                }
            }
        }

        @Override
        public void run() {
            FileChannel vpnIn = new FileInputStream(vpnFileDescriptor).getChannel();
            FileChannel vpnOut = new FileOutputStream(vpnFileDescriptor).getChannel();

            Thread t = new Thread(new WriteVpnThread(vpnOut, networkToDeviceQueue));
            t.start();

            try {
                while (!Thread.interrupted()) {
                    ByteBuffer buffer = ByteBuffer.allocate(16384);
                    int readBytes = vpnIn.read(buffer);

                    if (readBytes > 0) {
                        buffer.flip();
                        Packet packet = new Packet(buffer);

                        if (packet.isUDP())
                            deviceToNetworkUDPQueue.offer(packet);
                        else if (packet.isTCP())
                            deviceToNetworkTCPQueue.offer(packet);
                    }
                    else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                Log.i("error", e.toString());
            }
        }
    }
}