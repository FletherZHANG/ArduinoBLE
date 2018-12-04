package com.example.fletherzhang.arduinoble;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Vibrator;
import android.content.Context;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;
import java.io.IOException;
import java.util.UUID;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.content.pm.ActivityInfo;


public class CarControl extends AppCompatActivity implements OnTouchListener {

    Button btn1, btn2, btn3, btn4, btn5, btnDis;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Vibrator vibrator;
    private boolean has_vibrator;
    private ImageView gamepad;
    private ImageView gamepad_mask;

    private int pad_1_up_colour;
    private int pad_1_down_colour;
    private int pad_1_left_colour;
    private int pad_1_right_colour;
    private int pad_2_up_colour;
    private int pad_2_down_colour;
    private int pad_2_left_colour;
    private int pad_2_right_colour;

    public static final int DPAD_1_BUTTON_UP_VIEW_INX = 1;
    public static final int DPAD_1_BUTTON_LEFT_VIEW_INX = 2;
    public static final int DPAD_1_BUTTON_RIGHT_VIEW_INX = 3;
    public static final int DPAD_1_BUTTON_DOWN_VIEW_INX = 4;

    public static final int DPAD_2_BUTTON_UP_VIEW_INX = 5;
    public static final int DPAD_2_BUTTON_LEFT_VIEW_INX = 6;
    public static final int DPAD_2_BUTTON_RIGHT_VIEW_INX = 7;
    public static final int DPAD_2_BUTTON_DOWN_VIEW_INX = 8;

    public static final int PAD_DOWN = 1;
    public static final int PAD_UP = 2;

    @Override
    protected void onResume() {
        if(getRequestedOrientation()!=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS);

        setContentView(R.layout.activity_car_control);


        new ConnectBT().execute();

        gamepad_mask = (ImageView) CarControl.this.findViewById(R.id.gamepad_mask);
        gamepad = (ImageView) CarControl.this.findViewById(R.id.gamepad);
        gamepad.setOnTouchListener(this);

        pad_1_up_colour = getResources().getColor(R.color.pad_1_up_colour);
        pad_1_down_colour = getResources().getColor(R.color.pad_1_down_colour);
        pad_1_left_colour = getResources().getColor(R.color.pad_1_left_colour);
        pad_1_right_colour = getResources().getColor(R.color.pad_1_right_colour);

        pad_2_up_colour = getResources().getColor(R.color.pad_2_up_colour);
        pad_2_down_colour = getResources().getColor(R.color.pad_2_down_colour);
        pad_2_left_colour = getResources().getColor(R.color.pad_2_left_colour);
        pad_2_right_colour = getResources().getColor(R.color.pad_2_right_colour);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        has_vibrator = vibrator.hasVibrator();
    }

    private void sendSignal ( String number ) {
        if ( btSocket != null ) {
            try {
                btSocket.getOutputStream().write(number.toString().getBytes());
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                btSocket.close();
            } catch(IOException e) {
                msg("Error");
            }
        }

        finish();
    }

    private void msg (String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int pointer_index = 0;
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            pointer_index = event.getActionIndex();
        }

        final int evX = (int) event.getX(pointer_index);
        final int evY = (int) event.getY(pointer_index);

        String up_or_down=""; // 1 = down, 2 = up

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
            // get the colour of the region touched from the gamepad mask and use this to figure out which pad was pressed
            int touchColor = getHotspotColor (R.id.gamepad_mask, evX, evY);
            int tolerance = 25;
            int pad_no=-1;
            if (closeMatch(pad_1_up_colour, touchColor, tolerance)) {
                pad_no = DPAD_1_BUTTON_UP_VIEW_INX;
            }
            if (closeMatch(pad_1_down_colour, touchColor, tolerance)) {
                pad_no = DPAD_1_BUTTON_DOWN_VIEW_INX;
            }
            if (closeMatch(pad_1_left_colour, touchColor, tolerance)) {
                pad_no = DPAD_1_BUTTON_LEFT_VIEW_INX;
            }
            if (closeMatch(pad_1_right_colour, touchColor, tolerance)) {
                pad_no = DPAD_1_BUTTON_RIGHT_VIEW_INX;
            }
            if (closeMatch(pad_2_up_colour, touchColor, tolerance)) {
                pad_no = DPAD_2_BUTTON_UP_VIEW_INX;
            }
            if (closeMatch(pad_2_down_colour, touchColor, tolerance)) {
                pad_no = DPAD_2_BUTTON_DOWN_VIEW_INX;
            }
            if (closeMatch(pad_2_left_colour, touchColor, tolerance)) {
                pad_no = DPAD_2_BUTTON_LEFT_VIEW_INX;
            }
            if (closeMatch(pad_2_right_colour, touchColor, tolerance)) {
                pad_no = DPAD_2_BUTTON_RIGHT_VIEW_INX;
            }

            if (pad_no > -1) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN ) {
                    up_or_down = "down";
                } else {
                    up_or_down = "up";
                }

                sendSignal(String.valueOf(pad_no)+up_or_down+"#");

                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                    if (has_vibrator) {
                        vibrator.vibrate(250);
                    }
                }
                return true;
            }
        }
        return false;
    }
    public int getHotspotColor (int hotspotId, int x, int y) {
        ImageView img = (ImageView) findViewById(hotspotId);
        img.setDrawingCacheEnabled(true);
        Bitmap hotspots = Bitmap.createBitmap(img.getDrawingCache());
        img.setDrawingCacheEnabled(false);
        if (x >= 0 && y >= 0 && x <= hotspots.getWidth() && y <= hotspots.getHeight()) {
            return hotspots.getPixel(x, y);
        } else {
            return 0;
        }
    }

    // see https://blahti.wordpress.com/2012/06/26/images-with-clickable-areas/
    public boolean closeMatch (int color1, int color2, int tolerance) {
        int red_diff = (int) Math.abs (Color.red(color1) - Color.red (color2));
        int green_diff = (int) Math.abs (Color.green(color1) - Color.green(color2));
        int blue_diff = (int) Math.abs (Color.blue(color1) - Color.blue(color2));
        if (red_diff > tolerance ) return false;
        if (green_diff > tolerance ) return false;
        if (blue_diff > tolerance ) return false;
        return true;
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
            progress = ProgressDialog.show(CarControl.this, "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected");
                isBtConnected = true;
            }

            progress.dismiss();
        }
    }


}
