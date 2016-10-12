package edu.ucr.ece.finalproject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "BT Data Comm";

    //Button   btnOn, btnOff;
    //TextView dataSensor1, dataSensor2, dataSensor3;
    ImageView imageView;
    Switch switch1, switch2;
    TextView OnorOff;

    Handler h;

    final int RECEIVE_MESSAGE = 1;		// Status  for Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service. DO NOT CHANGE!!! This is a Standard SerialPortService ID per
    // https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#createRfcommSocketToServiceRecord%28java.util.UUID%29
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Adafruit Bluefruit EZ-Link module (you must edit this line)
    private static String address = "98:76:B6:00:9D:73";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        boolean dark;
        boolean motion;
        switch1 = (Switch) findViewById(R.id.switch1);
        switch2 = (Switch) findViewById(R.id.switch2);
        OnorOff = (TextView) findViewById(R.id.OnorOff);
        imageView = (ImageView) findViewById(R.id.imageView);
        switch1.setChecked(false);
        switch2.setChecked(false);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();
        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    //Toast.makeText(getApplicationContext(), "The switch is ON", Toast.LENGTH_SHORT).show();
                    mConnectedThread.write("1");
                }
                if(!isChecked) {
                    mConnectedThread.write("0");
                    //Toast.makeText(getApplicationContext(),"The switch is OFF", Toast.LENGTH_SHORT).show();
                }

            }
        });
        switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    //Toast.makeText(getApplicationContext(), "The switch is ON", Toast.LENGTH_SHORT).show();
                    mConnectedThread.write("2");
                }
                if(!isChecked) {
                    mConnectedThread.write("3");
                    //
                    // Toast.makeText(getApplicationContext(),"The switch is OFF", Toast.LENGTH_SHORT).show();
                }

            }
        });
        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:									// if receive message
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);	// create string from bytes array
                        sb.append(strIncom);								// append string
                        int endOfLineIndex = sb.indexOf("#");				// determine the end-of-line
                        if (endOfLineIndex > 0) { 							// if end-of-line,
                            String sbprint1 = sb.substring(0,endOfLineIndex);
                            String off = "Off";
                            String on = "On";
                            if (sbprint1.matches(off)){
                                imageView.setImageResource(R.drawable.lightbulb8);
                                OnorOff.setText("1");
                            }
                            else if(sbprint1.matches(on)){
                                imageView.setImageResource(R.drawable.lightbulb9);
                                OnorOff.setText("2");
                            }
                            sb.delete(0, sb.length());                      // and clear
                            OnorOff.setText(sbprint1);

                        }
                        Log.d(TAG, "MsgString:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            };
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
        checkBTState();

//        btnOn.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                btnOn.setEnabled(true);
//                mConnectedThread.write("1");	// Send "1" via Bluetooth
//                //Toast.makeText(getBaseContext(), "Turn LED ON: Sending Char 1 ", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        btnOff.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                btnOff.setEnabled(true);
//                mConnectedThread.write("0");	// Send "0" via Bluetooth
//                //Toast.makeText(getBaseContext(), "Turn LED OFF: Sending Char 0 ", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord",
                        new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume(): Creating bluetooth socket ...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
            Log.d(TAG, "onResume(): Bluetooth socket created ...");
        } catch (IOException e) {
            errorExit("Fatal Error", "onResume(): Create bluetooth socket FAILED: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "Connecting to Bluetooth Device ...");
        try {
            btSocket.connect();
            Log.d(TAG, "Bluetooth Device Connected ...");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "onResume(): Unable to close socket when closing connection: " + e2.getMessage() + ".");
            }
        }

//        // Create a data stream so we can talk to server.
        Log.d(TAG, "onResume(): Creating data output stream ...");
//        try {
//            outStream = btSocket.getOutputStream();
//        } catch (IOException e) {
//            errorExit("Fatal Error", "onResume(): Creating data output stream FAILED: " + e.getMessage() + ".");
//        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "Inside onPause()...");

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "onPause(): FAILED to close socket." + e2.getMessage() + ".");
        }
    }
//    public void onPause() {
//        super.onPause();
//
//        Log.d(TAG, "Inside onPause()...");
//
//        if (outStream != null) {
//            try {
//                outStream.flush();
//            } catch (IOException e) {
//                errorExit("Fatal Error", "onPause(): FAILED to flush data output stream: " + e.getMessage() + ".");
//            }
//        }
//
//        try     {
//            btSocket.close();
//        } catch (IOException e2) {
//            errorExit("Fatal Error", "onPause(): FAILED to close socket." + e2.getMessage() + ".");
//        }
//    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not supported");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth is ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "Info: ConnectedThread: data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "Error: ConnectedThread: data to send: " + e.getMessage() + "...");
            }
        }
    }
//    private void sendData(String message) {
//        byte[] msgBuffer = message.getBytes();
//
//        Log.d(TAG, "Sending data: " + message + "...");
//
//        try {
//            outStream.write(msgBuffer);
//        } catch (IOException e) {
//            String msg = "onResume(): Exception occurred during write: " + e.getMessage();
//            if (address.equals("00:00:00:00:00:00"))
//                msg = msg + ".\n\nChange your server address from 00:00:00:00:00:00 to the correct address on line 35 in the java code";
//            msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
//
//            errorExit("Fatal Error", msg);
//        }
//    }
}