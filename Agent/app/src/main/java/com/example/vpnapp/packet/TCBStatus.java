package com.example.vpnapp.packet;

import java.io.Serializable;

public enum TCBStatus implements Serializable {
    SYN_SENT,
    SYN_RECEIVED,
    ESTABLISHED,
    CLOSE_WAIT
}