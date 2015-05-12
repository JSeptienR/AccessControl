package com.access.accesscontrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


public class AccessActivity extends FragmentActivity {
    public final String TAG = "Main";
    Bluetooth mBluetooth;

    TextView status;

    private Button mConnectButton;

    private RequestKeyTask mAuthTask = null;
    private final String LOGIN_URL= "http://172.17.10.245:8080/Bluetooth_Lock/GetKey?";

    private TextView mRequestText;
    private ImageView mLockView;

    private String VALIDATION_TAG = "key";
    String  user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access);


        Bundle extras = getIntent().getExtras();
        if(extras !=null) {
            user = extras.getString("user");
        } else {
            user = "";
        }

        status = (TextView) findViewById(R.id.status);
        mRequestText = (TextView) findViewById(R.id.requestText);
        mLockView = (ImageView) findViewById(R.id.lockImageView);

        mBluetooth = new Bluetooth(this, mHandler);

        mConnectButton = (Button) findViewById(R.id.buttonConnect);

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //mBluetooth.sendMessage("1");
                String lock = "3";
                mAuthTask = new RequestKeyTask(user, lock, LOGIN_URL);
                mAuthTask.execute((Void) null);
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
                    mRequestText.setText("Access Granted");
                    break;
                case Bluetooth.VALID:
                    Log.d(TAG, "VALID");
                    //mRequestText.setText("Access Granted");
                    accessGranted();
                    break;
                case Bluetooth.INVALID:
                    Log.d(TAG, "INVALID");
                    //mRequestText.setText("Invalid");
                    accessDenied();
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

    public void accessGranted() {
        mRequestText.setText("Access Granted");
        mLockView.setImageDrawable(getResources().getDrawable(R.drawable.lock_open));
    }

    public void accessDenied() {
        mRequestText.setText("Access Denied");
        mLockView.setImageDrawable(getResources().getDrawable(R.drawable.lock_closed));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0) {
            if(resultCode == Activity.RESULT_OK) {
                String pin=data.getStringExtra("pin");
                Log.d("RESULT_PIN", pin);

                String lock = "3";
                String url= "http://172.17.10.245:8080/Bluetooth_Lock/PinValidation?";
                new RequestKeyWithPinTask(user, lock, pin,  url).execute((Void) null);
            }
        }
    }

    private void showPinDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please Enter Pin");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pin = input.getText().toString();

                String lock = "3";
                String url= "http://172.17.10.245:8080/Bluetooth_Lock/PinValidation?";
                new RequestKeyWithPinTask(user, lock, pin,  url).execute((Void) null);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public class RequestKeyTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUrl;
        private final String mUser;
        private final String mLock;
        private int mKey;
        private int mSecurityLevel;


        RequestKeyTask(String user, String lock, String url) {

            mUser = user;
            mLock = lock;
            mUrl = url + "user=" + mUser+ "&" + "lock=" + mLock;
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
                //return false;
                return true;
            }
/*            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            //Create JSON Object from resulting String
            try {
                Log.d("JSON", "Creating JSON");
                JSONObject jsonObject = new JSONObject(response);
                
                mSecurityLevel = jsonObject.getInt("securityLevel");
                if(mSecurityLevel == 1)
                    mKey = jsonObject.getInt(VALIDATION_TAG);
                if(mSecurityLevel == 2)
                    mKey = 0;

                Log.d("JSON", "Created JSON");
                Log.d("JSON", Integer.toString(mKey));
                Log.d("JSON sec", Integer.toString(mSecurityLevel));


            } catch (JSONException e) {
                e.printStackTrace();
                return false;
                //return true;
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

                if(mKey != 0) {
                    mBluetooth.sendMessage(Integer.toString(mKey));
                } else {
                    //Intent intent = new Intent(AccessActivity.this,  PinActivity.class);
                    //startActivityForResult(intent, 0);

                    showPinDialog();
                }

            } else {
                //mRequestText.setText("Access Denied");
                accessDenied();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            //showProgress(false);
        }
    }


    //RequestPin

    public class RequestKeyWithPinTask extends AsyncTask<Void, Void, Boolean> {
        private final String mUrl;
        private final String mUser;
        private final String mLock;
        private int mKey;
        private int mSecurityLevel;


        RequestKeyWithPinTask(String user, String lock, String pin, String url) {

            mUser = user;
            mLock = lock;
            mUrl = url + "user=" + mUser+ "&" + "pin=" + pin + "&" + "lock=" + mLock ;
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
                return false;
                //return true;
            }
/*            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            //Create JSON Object from resulting String
            try {
                Log.d("JSON", "Creating JSON");
                JSONObject jsonObject = new JSONObject(response);
                mKey = jsonObject.getInt(VALIDATION_TAG);

                Log.d("JSON", "Created JSON");
                Log.d("JSON", Integer.toString(mKey));

            } catch (JSONException e) {
                e.printStackTrace();
                return false;
                //return true;
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
                mBluetooth.sendMessage(Integer.toString(mKey));
                //accessGranted();
            } else {
                //mRequestText.setText("Access Denied");
                accessDenied();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            //showProgress(false);
        }
    }
}
