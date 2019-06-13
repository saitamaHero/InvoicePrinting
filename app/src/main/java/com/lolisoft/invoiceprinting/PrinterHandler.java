package com.lolisoft.invoiceprinting;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.datecs.api.printer.Printer;
import com.datecs.api.printer.ProtocolAdapter;

import java.io.IOException;
import java.util.UUID;

public class PrinterHandler extends Handler implements Printer.ConnectionListener {
    public static final String CHARSET_ISO_8859_1 = "ISO-8859-1";
    public static final int FLAG_CONNECT = 1;
    public static final int FLAG_PRINTER_STATUS = 2;


    public static final int STATUS_CONNECTED = -1;
    public static final int STATUS_DISCONNECTED = 0;

    private Context mContext;
    private Handler activityHandler;
    private BluetoothSocket mBluetoothSocket;
    private Printer mPrinter;

    public PrinterHandler(Looper looper, Context context) {
        super(looper);
        this.mContext = context;

        if(this.mContext == null){
            throw new NullPointerException("The context can't be null");
        }

        activityHandler = new Handler(this.mContext.getMainLooper());

        if(looper.getThread() == activityHandler.getLooper().getThread()){
            throw new IllegalArgumentException("looper can't be different to the main looper");
        }
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        switch (msg.what)
        {
            case FLAG_CONNECT:
                if(connect((BluetoothDevice) msg.obj)){
                    sendPrinterStatus(STATUS_CONNECTED);
                }

                break;

        }
    }

    /**
     * Connect to the bluetooth device required
     * @param bluetoothDevice
     * @return true if bluetooth device is connected
     */
    private boolean connect(BluetoothDevice bluetoothDevice){
        if(bluetoothDevice == null){
            return false;
        }

        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            mBluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();

            ProtocolAdapter mProtocolAdapter = new ProtocolAdapter(mBluetoothSocket.getInputStream(), mBluetoothSocket.getOutputStream());

            if(mProtocolAdapter.isProtocolEnabled()) {
                ProtocolAdapter.Channel mPrinterChannel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);
                mPrinter = new Printer(mPrinterChannel.getInputStream(), mPrinterChannel.getOutputStream());
            }else{
                mPrinter =  new Printer(mProtocolAdapter.getRawInputStream(),mProtocolAdapter.getRawOutputStream());
            }

            mPrinter.setConnectionListener(this);

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public void onDisconnect() {
        sendPrinterStatus(STATUS_DISCONNECTED);
    }

    private void sendPrinterStatus(int connected){
        Message message = activityHandler.obtainMessage();
        message.what = FLAG_PRINTER_STATUS;
        message.arg1 = connected;
    }
}
