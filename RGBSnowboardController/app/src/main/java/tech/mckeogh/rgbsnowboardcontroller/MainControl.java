package tech.mckeogh.rgbsnowboardcontroller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.view.ViewDebug;
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
import java.util.BitSet;
import java.io.IOException;
import java.util.BitSet;
import java.util.UUID;

public class MainControl extends AppCompatActivity{

    private SeekBar brightness;
    private Switch power;
    private Button disconnect;
    private RadioGroup modeGroup;
    private RadioButton modeButton;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BitSet command = new BitSet(8);
    int progresssaved;

    // Misc commands - need to actually check these
    byte defaultcommand = 4;
    byte poweroffcommand = 12;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS); //receive the address of the bluetooth device

        setContentView(R.layout.activity_main_control);

        brightness = (SeekBar) findViewById(R.id.seekBar1);
        brightness.setProgress(50); // Centre bar to match default brightness snowboard side
        brightness.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
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
                        int progress = brightness.getProgress(); // Awful, awful. I know a range of 0-100 doesn't need a long, but what am I to do?
                        progress = progress << 2; // Shift data along to make space for type
                        command = BitSet.valueOf(new long[] {progress});
                        command.set(0, true); // Binary type for brightness
                        command.set(1, false);
                        byte test = 0;
                        btSocket.getOutputStream().write(test); // Aaand back to an int for sending over bluetooth. God this is awful. I am so sorry
                    } catch (IOException e) {
                        msg("Error");
                    }
                }
            }
        });

        power = (Switch) findViewById(R.id.switch1);
        power.setChecked(true);
        power.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        brightness.setEnabled(true);
                        brightness.setProgress(progresssaved); // Half brightness
                        btSocket.getOutputStream().write(defaultcommand);
                    } catch (IOException e) {
                        msg("Error");
                    }
                }
                else {
                    try {
                        progresssaved = brightness.getProgress();
                        brightness.setEnabled(false);
                        brightness.setProgress(0);
                        btSocket.getOutputStream().write(poweroffcommand);
                    } catch (IOException e) {
                        msg("Error");
                    }
                }

            }
        });

        modeGroup = (RadioGroup) findViewById(R.id.modeGroup);
        modeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.colorDemo) {

                } else  if (checkedId == R.id.mReactive) {

                } else {

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

        new ConnectBT().execute(); //Call the class to connect
    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }
    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(MainControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
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
