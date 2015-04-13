package com.access.accesscontrol;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class AccessActivity extends ActionBarActivity {
    public final String TAG = "Main";
    Bluetooth mBluetooth;

    TextView status;

    private Button mConnectButton;

    private RequestKeyTask mAuthTask = null;
    private final String ACCESS_REQUEST_URL= "http://172.17.10.244:8080/Bluetooth_Lock/LoginPhoneUser?";

    private EditText mRequestText;

    private String VALIDATION_TAG = "userOK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access);

        status = (TextView) findViewById(R.id.status);

        mBluetooth = new Bluetooth(this, mHandler);

        mConnectButton = (Button) findViewById(R.id.buttonConnect);

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetooth.sendMessage("1");
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_access, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void connectService(){
        try {
            status.setText("Connecting...");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()) {
                mBluetooth.start();
                mBluetooth.connectDevice("HC-06");
                Log.d(TAG, "Btservice started - listening");

                status.setText("Connected");
            } else {
                Log.w(TAG, "Btservice started - bluetooth is not enabled");
                status.setText("Bluetooth Not enabled");
            }
        } catch(Exception e){
            Log.e(TAG, "Unable to start bt ",e);
            status.setText("Unable to connect " +e);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        connectService();

    }

    @Override
    protected void onStop(){
        super.onStop();
        mBluetooth.stop();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Bluetooth.MESSAGE_STATE_CHANGE:
                    Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    break;
                case Bluetooth.MESSAGE_WRITE:
                    Log.d(TAG, "MESSAGE_WRITE ");
                    break;
                case Bluetooth.MESSAGE_READ:
                    Log.d(TAG, "MESSAGE_READ ");
                    break;
                case Bluetooth.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "MESSAGE_DEVICE_NAME "+msg);
                    break;
                case Bluetooth.MESSAGE_TOAST:
                    Log.d(TAG, "MESSAGE_TOAST "+msg);
                    break;
            }
        }
    };


    public class RequestKeyTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUrl;
        private final String mPassword;

        RequestKeyTask(String password, String url) {

            mPassword = password;
            mUrl = new StringBuilder().append(url).append("&").append("password="+ mPassword).toString();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            String response = null;

            try {
                // Simulate network access.
                //Thread.sleep(2000);

                HttpServiceHandler httpServiceHandler = new HttpServiceHandler();

                //Login request using using password: modify concatenation

                Log.d("Authentication", mUrl);
                response = httpServiceHandler.downloadUrl(mUrl);

            } catch (IOException e) {
                //return false;
                return true;
            }
/*            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            boolean validation = false;

            //Create JSON Object from resulting String
            try {
                Log.d("JSON", "Creating JSON");
                JSONObject jsonObject = new JSONObject(response);

                validation = jsonObject.getBoolean(VALIDATION_TAG);
                Log.d("JSON", "Created JSON");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // TODO: register the new account here.
            //return validation;
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            //showProgress(false);

            if (success) {
                finish();
                mRequestText.setText("Access Granted");

            } else {
                mRequestText.setText("Access Denied");
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            //showProgress(false);
        }
    }
}
