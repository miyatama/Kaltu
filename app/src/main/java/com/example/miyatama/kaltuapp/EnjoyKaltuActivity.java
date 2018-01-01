package com.example.miyatama.kaltuapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class EnjoyKaltuActivity extends AppCompatActivity {

    private static final String TAG = "CreateKaltuActivity";
    private AppCompatActivity activity;
    private static final boolean AUTO_HIDE = true;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    private Handler mHandler;
    private static final int HDL_SHOW_KALTU = 1000;
    private static final int HDL_HIDE_KALTU = 1001;

    private Button btnShutter;
    private AutoFitTextureView mTextureView;
    private CameraStateMachine mCamera;
    private ImageView mKaltImage;
    private ImageView mKaltCharImage;
    private Bitmap mKaltuBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enjoy_kaltu);

        activity = this;
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case HDL_SHOW_KALTU:
                        mKaltImage.setImageBitmap(mKaltuBitmap);
                        mKaltImage.setVisibility(View.VISIBLE);
                        mKaltCharImage.setVisibility(View.VISIBLE);
                        break;
                    case HDL_HIDE_KALTU:
                        mKaltImage.setVisibility(View.INVISIBLE);
                        mKaltCharImage.setVisibility(View.INVISIBLE);
                        break;
                }
                return false;
            }
        });
        btnShutter = findViewById(R.id.btn_shutter);
        btnShutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "btnShutter onClick");
                Message msgShowImage = Message.obtain();
                msgShowImage.what = HDL_SHOW_KALTU;
                mHandler.sendMessage(msgShowImage);

                Message msgHideImage = Message.obtain();
                msgHideImage.what = HDL_HIDE_KALTU;
                mHandler.sendMessageDelayed(msgHideImage, 2000);
            }
        });

        mTextureView = findViewById(R.id.textureView);
        mCamera = new CameraStateMachine();
        mKaltImage = findViewById(R.id.kaltImage);
        mKaltCharImage = findViewById(R.id.kaltCharImage);
        mKaltCharImage.setVisibility(View.INVISIBLE);

        try{
            File file = new File(activity.getFilesDir(), "Kaltu.png");
            FileInputStream fis = new FileInputStream(file);
            mKaltuBitmap = BitmapFactory.decodeStream(fis);
        } catch(FileNotFoundException e){
            Toast.makeText(activity, "Kaltu Not Found", Toast.LENGTH_SHORT).show();
            this.finish();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        mCamera.open(this, mTextureView, CameraCharacteristics.LENS_FACING_BACK);
        show();
    }

    @Override
    protected void onPause(){
        mCamera.close();
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if (mKaltuBitmap != null) {
            mKaltuBitmap.recycle();
            mKaltuBitmap = null;
        }
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }
}
