package com.icpstep.android.stepcounter;
/*====================================
    Author : Waheed Rahuman
======================================*/

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.ic4j.agent.Agent;
import org.ic4j.agent.AgentBuilder;
import org.ic4j.agent.ProxyBuilder;
import org.ic4j.agent.ReplicaTransport;
import org.ic4j.agent.http.ReplicaOkHttpTransport;
import org.ic4j.agent.identity.AnonymousIdentity;
import org.ic4j.agent.identity.Identity;
import org.ic4j.types.Principal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity implements SensorEventListener , View.OnClickListener {
    TextView tv_steps,tv_info;
    SensorManager sensorManager;
    Sensor sensor;
    boolean running = false;
    SharedPreferences.Editor editor = null;
    SharedPreferences pref = null;
    EditText principalHolder;
    String imageUploaded = null;
    String stepsCounter = null;
    String balanceTokens = null;
    MainActivity currentView = null;
    Drawable drawable = null;
    ImageView user_image=null;

    ReplicaTransport transport = null;
    Agent agent = null;
    HelloWorldProxy helloWorldProxy = null;

    // Permissions for accessing the storage
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int PICK_IMAGE_REQUEST = 9544;
    Uri selectedImage;
    String part_image;
    ImageView image;
    int TAKE_PHOTO_CODE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_steps = findViewById(R.id.tv_steps);

        user_image= (ImageView) findViewById(R.id.imageHolder);
        drawable = user_image.getDrawable();

        tv_info = findViewById(R.id.tv_info);

        String defaultPrincipal = getResources().getString(R.string.principal);
        principalHolder   = (EditText)findViewById(R.id.principal);
        principalHolder.setText(defaultPrincipal);

        pref = getDefaultSharedPreferences(getApplicationContext());
        editor = pref.edit();

        String steps = pref.getString("steps", "Move");
        tv_steps.setText(steps);

        restoreSteps("Move");


        Button redeemButton = (Button)findViewById(R.id.redeem);
        Button uploadButton = (Button)findViewById(R.id.upload);

        redeemButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);

        redeemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redeemMethod(v);
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadMethod(v);
            }
        });


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

         user_image.setImageDrawable(drawable);


         tv_info.setText("Status");


        if(imageUploaded != null)
        {
            tv_steps = findViewById(R.id.tv_steps);

            if(imageUploaded.equals("Large File"))
            {
                saveSteps();
                tv_info.setText(imageUploaded);
                Toast.makeText(this, imageUploaded, Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Photo is selected", Toast.LENGTH_SHORT).show();


            tv_info.setText("Uploading.....");

            final Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    tv_info.setText("Sending bytes....");
                }
            }, 1000);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms

                    String result = uploadImages(imageUploaded);
                    tv_info.setText("Image ID " + result);
                    imageUploaded = null;
                 }
            }, 3000);

            restoreSteps("0");
        }

        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Principal principal = Principal.fromString("aaaaa-aa");

            currentView = this;
            Toast.makeText(this, "SENSOR NOT FOUND", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        //if you unregister the hardware will stop detecting steps
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (running) {

            tv_steps.setText(String.valueOf(event.values[0]));

            String totalSteps =  pref.getString("totalSteps", "0");


            float _totalSteps   =  parseSteps(totalSteps);
            float _stepsCounter =  parseSteps(stepsCounter);

            int redeemSteps = getRedeem();


            if(_totalSteps < _stepsCounter) // Phone got rebooted
            {
                _totalSteps = _totalSteps + _stepsCounter + redeemSteps; //Walked Steps + Previous balance + redeem
            }
            else
                _totalSteps = parseSteps(String.valueOf(event.values[0]));

            // Save the redeem
            _stepsCounter =  _totalSteps - redeemSteps;

            editor.putString("steps", String.valueOf(_stepsCounter));
            editor.commit();

            editor.putString("totalSteps", String.valueOf(_totalSteps));
            editor.commit();

            stepsCounter = String.valueOf(event.values[0]);

            tv_steps.setText(String.valueOf(_stepsCounter));


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        Log.d("debug permission", String.valueOf(permission));
        Log.d("debug Package manager",String.valueOf(PackageManager.PERMISSION_GRANTED));
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void redeemMethod(View v)
    {

        String steps = pref.getString("steps", "0");
        float stepsCount = 0;
        Principal userPrincipal;
        EditText mEdit;

        mEdit = (EditText) findViewById(R.id.principal);


        try {
            String redeemPrincipal = mEdit.getText().toString();
            if (redeemPrincipal.length() >= 27) {
                userPrincipal = Principal.fromString(redeemPrincipal);
                Log.d("debug", userPrincipal.toString());
            }
        } catch (Exception e) {
        }
        Log.d("debug", "In redeem");
        stepsCount = parseSteps(steps);
        Log.d("debug line 276", String.valueOf(stepsCount));

        if (stepsCount < 1000) {
            Log.d("debug", "Low steps count");
            Toast.makeText(this, "Low steps count, you need at least 1000 steps to redeem !", Toast.LENGTH_SHORT).show();

        } else {

            final Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    tv_info.setText("Redeem Steps to Tokens");
                }
            }, 1000);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    redeemSteps();
                    tv_info.setText("Successfully redeemed!");

                    Log.d("debug stepsCounter", stepsCounter);
                    try{
                        float steps = parseSteps(stepsCounter);
                        steps = steps - 1000;
                        stepsCounter = String.valueOf(steps);
                        Log.d("debug try stepsCounter", stepsCounter);

                        editor.putString("steps", String.valueOf(stepsCounter));
                        editor.commit();

                        incrementRedeem();

                    }
                    catch(Exception e)
                    {
                        Log.d("debug catch stepsCounter", stepsCounter);
                        stepsCounter = "0";
                    }
                }
            }, 3000);

            final Handler handler2 = new Handler();
            handler2.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    restoreSteps("0");
                    tv_info.setText("Balance \uD83D\uDC63 Tokens:" + balanceTokens);
                }
            },10000);

            }

    }

    private int getRedeem()
    {
        pref = getDefaultSharedPreferences(getApplicationContext());
        editor = pref.edit();

        String redeem = pref.getString("redeem", "0");

        int _redeem = parseInteger(redeem);

        return _redeem;
    }

    private void saveSteps()
    {
        String steps = tv_steps.getText().toString();
        editor.putString("steps", steps);
        editor.commit();
    }
    private void restoreSteps(String nullValue)
    {
        pref = getDefaultSharedPreferences(getApplicationContext());
        editor = pref.edit();

        String steps = pref.getString("steps", nullValue);
        tv_steps.setText(steps);
        stepsCounter = steps;
    }

    private void createProxy() throws URISyntaxException {
        if(transport == null) {

            String url = getResources().getString(R.string.url);
            String canister = getResources().getString(R.string.canister);
            
            transport = ReplicaOkHttpTransport.create(url);

            agent = new AgentBuilder().transport(transport).build();

            Principal principal = Principal.fromString(canister);

            helloWorldProxy = ProxyBuilder.create(agent, principal).getProxy(HelloWorldProxy.class);
        }
    }
    private void incrementRedeem()
    {
        pref = getDefaultSharedPreferences(getApplicationContext());
        editor = pref.edit();

        String redeem = pref.getString("redeem", "0");

        int _redeem = parseInteger(redeem);

        _redeem = _redeem + 1000;

        editor.putString("redeem", String.valueOf(_redeem));
        editor.commit();
    }
    private String encodeImage(Bitmap bm)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG,100,baos);
        byte[] b = baos.toByteArray();
        String encImage = Base64.encodeToString(b, Base64.DEFAULT);

        return encImage;
    }


    private void redeemSteps()
    {
        EditText mEdit;
        mEdit = (EditText) findViewById(R.id.principal);

        boolean callHelloWorld = false;
        try {
            createProxy();
            if(callHelloWorld)
            {
                HelloCanister();
                setPrincipal(mEdit);
            }

            String redeemPrincipal  = redeemSteps2Token(mEdit);

            getBalance(redeemPrincipal);



        } catch (Throwable e) {

            Toast.makeText(this, "Error Calling Canister" + e.toString(), Toast.LENGTH_SHORT).show();
            Log.d("debug", e.toString());

        }
    }


    private void HelloCanister() throws ExecutionException, InterruptedException
    {
        String value = "Android";

        CompletableFuture<String> peekResponse = helloWorldProxy.peek();

        String peek = peekResponse.get();
        Log.d("debug",peek);

        CompletableFuture<String> greetResponse = helloWorldProxy.greet(value);

        String output = greetResponse.get();
        Log.d("debug",output);

        CompletableFuture<String> helloWorldResponse = helloWorldProxy.hello();

        String hello = helloWorldResponse.get();
        Log.d("debug Hello", hello);


        CompletableFuture<BigInteger> getInt = helloWorldProxy.get();

        BigInteger getNAT = getInt.get();
        Log.d("debug getInt Query ", String.valueOf(getNAT.intValue()));

        BigInteger initialValue = new BigInteger("100");
        CompletableFuture<Void> setInt = helloWorldProxy.set(initialValue);

        Log.d("debug setData", String.valueOf(100));


        String setNATResult = String.valueOf(setInt.get());
        Log.d("debug setData",setNATResult);

        CompletableFuture<Void> increment = helloWorldProxy.inc();
        //output = increment.get();
        // Log.d("debug increment",output);

        Log.d("debug increment by 1", String.valueOf(101));
        for (int i = 0; i <= 4; i++) {
            //increment = helloWorldProxy.inc();
            Log.d("debug looping", String.valueOf(i));
        }

        CompletableFuture<BigInteger> getIncrement = helloWorldProxy.get();

        BigInteger incremented = getIncrement.get();
        Log.d("debug get Current Value", String.valueOf(incremented.intValue()));

        //LOG.info(output);

    }
    private void setPrincipal(EditText mEdit) throws ExecutionException, InterruptedException
    {
        String redeemPrincipal = mEdit.getText().toString();
        CompletableFuture<String> setPrincipal = helloWorldProxy.setPrincipal(redeemPrincipal);
        String setPrincipalResult = setPrincipal.get();
        Log.d("debug setPrincipal", setPrincipalResult);
    }

    private void getBalance(String redeemPrincipal) throws ExecutionException, InterruptedException
    {
        CompletableFuture<BigInteger> getBalance = helloWorldProxy.balance(redeemPrincipal);
        BigInteger balanceSteps = getBalance.get();
        Log.d("debug balance ", String.valueOf(balanceSteps.toString()));

        balanceTokens = balanceSteps.toString();
    }
    private String redeemSteps2Token(EditText mEdit) throws ExecutionException, InterruptedException {
        String redeemPrincipal = mEdit.getText().toString();
        Log.d("debug Hello", redeemPrincipal);

        CompletableFuture<BigInteger> redeemSteps = helloWorldProxy.redeem(redeemPrincipal);
        BigInteger redeemStepResult = redeemSteps.get();
        Log.d("debug redeemSteps ", String.valueOf(redeemStepResult.toString()));

        return redeemPrincipal;
    }
    private String uploadImages(String encodedString)
    {

        try {

            createProxy();

            CompletableFuture<BigInteger> setImageResult = helloWorldProxy.setImage(encodedString);
            BigInteger imageID = setImageResult.get();

            BigInteger one = BigInteger.valueOf(1);
            imageID = imageID.subtract(one);

            Log.d("debug setImage ", String.valueOf(imageID.toString()));
            return String.valueOf(imageID.toString());
        }
        catch(Exception e)
        {
            Log.d("debug Imageset Error ", e.toString());

        }

        return "Error";

    }


    public void getImageMethod(View v)
    {


        tv_info.setText("Fetching Image..");
        Button p1_button = (Button)findViewById(R.id.getImage);

        Log.d("debug", p1_button.getText().toString());

        p1_button.setText("Fetching...");

        p1_button.post(new Runnable(){
            @Override
            public void run(){
                p1_button.setText(String.valueOf("Fetching \uD83D\uDCF7...."));
            }
        });
        Log.d("debug", p1_button.getText().toString());


        final Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms

                fetchImage();
                p1_button.setText("VIEW \uD83D\uDCF7");

            }
        }, 1000);


    }


    public void fetchImage()
    {
        try {

            createProxy();

            EditText imageEntered   = (EditText)findViewById(R.id.imageId);

            String _imageEntered = imageEntered.getText().toString();

            int imageEntered_ = parseInteger(_imageEntered);
            BigInteger imageId = BigInteger.valueOf(imageEntered_);

            CompletableFuture<String> setImageResult = helloWorldProxy.getImage(imageId);
            String imageID = setImageResult.get();
            Log.d("debug setImage ", String.valueOf(imageID.toString()));

            byte[] decodedString = Base64.decode(imageID, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            ImageView user_image= (ImageView) findViewById(R.id.imageHolder);
            user_image.setImageBitmap(decodedByte);

            tv_info.setText("Image loaded");


        }
        catch(Exception e)
        {
            Log.d("debug Imageset Error ", e.toString());

        }
    }
    public void uploadMethod(View v)
    {
        Log.d("debug", "In upload");
        verifyStoragePermissions(MainActivity.this);

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Open Gallery"), PICK_IMAGE_REQUEST);
    }

    public int parseInteger(String imageId)
    {
        int result = Integer.parseInt("0");

        try{
            result = Integer.parseInt(imageId);
        }
        catch(Exception e)
        {
            result = Integer.parseInt("0");
        }
        return result;
    }
    public float parseSteps(String steps)
    {
        float result = Float.parseFloat("0");

        try{
            result = Float.parseFloat(steps);
        }
        catch(Exception e)
        {
            result = Float.parseFloat("0");
        }
        return result;
    }


    // Method to get the absolute path of the selected image from its URI
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("debug RESULT_OK",String.valueOf(RESULT_OK));
        Log.d("debug resultCode",String.valueOf(resultCode));
        Log.d("debug PICK_IMAGE_REQUEST",String.valueOf(requestCode));



        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            final Uri imageUri = data.getData();

            if (imageUri != null) {
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byte[] byteArray = outputStream.toByteArray();

                //Use your Base64 String as you wish
                String encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
                Log.d("debug encodedImage",encodedString);

                Bitmap resized = Bitmap.createScaledBitmap(bitmap, 15, 15, true);

                resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byteArray = outputStream.toByteArray();

                //Use your Base64 String as you wish
                encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
                Log.d("debug small size",encodedString);

                int encodedStringLen = encodedString.length();

                Log.d("debug encoded String Length",String.valueOf(encodedStringLen));

                if(encodedStringLen < 140000)
                {
                    imageUploaded = encodedString;
                }
                else
                {
                    imageUploaded = "Large File";
                }

            }


        }
    }

    @Override
    public void onClick(View view) {

    }
}