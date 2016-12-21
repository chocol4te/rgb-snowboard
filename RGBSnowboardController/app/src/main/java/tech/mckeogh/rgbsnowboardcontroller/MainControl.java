package tech.mckeogh.rgbsnowboardcontroller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.BluetoothSocket;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.UUID;

public class MainControl extends AppCompatActivity{

    private SeekBar brightness;
    private Switch power;
    private Button disconnect;
    private RadioGroup modeGroup;
    private RadioButton modeButton;
    private Button emergency;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    int progresssaved;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        setContentView(R.layout.activity_main_control);

        brightness = (SeekBar) findViewById(R.id.seekBar1);
        brightness.setMax(63); // Limit maximum to fit inside 6 bits
        brightness.setProgress(32); // Centre bar to match default brightness board-side
        brightness.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            byte command;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                if (btSocket != null) {
                    try {
                        command = -128;
                        command += brightness.getProgress(); // Will give range of -128 to -65, or 10-000000 to 10-111111
                        btSocket.getOutputStream().write(command);
                    } catch (IOException e) {
                        msg("Error");
                    }
                }
            }
        });

        power = (Switch) findViewById(R.id.switch1);
        power.setEnabled(false); // Until I figure out how to make it work
        power.setChecked(true);
        power.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            byte poweroff = 0; // 00000000
            byte command;
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        brightness.setEnabled(true);
                        brightness.setProgress(progresssaved); // Half brightness
                        command = (byte) progresssaved; // progress can be between 0 and 63, 000000 and 111111
                        btSocket.getOutputStream().write(command);
                    } catch (IOException e) {
                        msg("Error");
                    }
                }
                else {
                    try {
                        progresssaved = brightness.getProgress();
                        brightness.setEnabled(false);
                        brightness.setProgress(0);
                        btSocket.getOutputStream().write(poweroff);
                    } catch (IOException e) {
                        msg("Error");
                    }
                }

            }
        });

        modeGroup = (RadioGroup) findViewById(R.id.modeGroup);
        modeGroup.check(R.id.colorDemo);
        modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            byte colordemocommand = 64; // 01000000
            byte mreactivecommand = 65; // 01000001
            byte areactivecommand = 66; // 01000010
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.colorDemo) {
                    try {
                        btSocket.getOutputStream().write(colordemocommand);
                    } catch (IOException e) {
                        msg("Error");
                    }
                } else  if (checkedId == R.id.mReactive) {
                    try {
                        btSocket.getOutputStream().write(mreactivecommand);
                    } catch (IOException e) {
                        msg("Error");
                    }
                } else {
                    try {
                        btSocket.getOutputStream().write(areactivecommand);
                    } catch (IOException e) {
                        msg("Error");
                    }
                }
            }
        });

        disconnect = (Button) findViewById(R.id.button);
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Disconnect();
            }
        });

        emergency = (Button) findViewById(R.id.EMERGENCY);
        emergency.setOnClickListener(new View.OnClickListener() {
            byte emergency = -1; // 11111111
            @Override
            public void onClick(View v) {
                try {
                    btSocket.getOutputStream().write(emergency);
                } catch (IOException e) {
                    msg("Error");
                }
            }
        });
        new ConnectBT().execute();
    }

    private void Disconnect()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish();

    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>
    {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(MainControl.this, "Connecting...", "Please wait!!!");
        }

        @Override
        protected Void doInBackground(Void... devices)
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }

    }
}
