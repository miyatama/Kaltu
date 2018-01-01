package com.example.miyatama.kaltuapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.ImageReader;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CreateKaltuActivity extends AppCompatActivity {
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
    private Bitmap mCorrectRotateBitmap;
    private FaceDetector mFaceDetector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_kaltu);


        activity = this;
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case HDL_SHOW_KALTU:
                        try{
                            File file = new File(activity.getFilesDir(), "Kaltu.png");
                            FileInputStream fis = new FileInputStream(file);
                            Bitmap bm = BitmapFactory.decodeStream(fis);
                            mKaltImage.setImageBitmap(bm);
                            mKaltImage.setVisibility(View.VISIBLE);
                        } catch(FileNotFoundException e){
                            Toast.makeText(activity, "Kaltu Not Found", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case HDL_HIDE_KALTU:
                        mKaltImage.setVisibility(View.INVISIBLE);
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
               boolean takePictureResult = mCamera.takePicture(new ImageReader.OnImageAvailableListener() {
                   @Override
                   public void onImageAvailable(ImageReader imageReader) {
                       Log.d(TAG, "onImageAvailable");
                       Matrix matrix = new Matrix();
                       matrix.preScale(0.3f, -0.3f);
                       final Image image = imageReader.acquireLatestImage();
                       ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                       byte[] buff = new byte[buffer.remaining()];
                       buffer.get(buff);
                       Bitmap bitmap = BitmapFactory.decodeByteArray(buff, 0, buff.length);
                       image.close();

                       mCorrectRotateBitmap = Bitmap.createBitmap(bitmap, 0,0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);

                       Frame frame = new Frame.Builder()
                               .setBitmap(mCorrectRotateBitmap)
                               .build();
                       SparseArray detectedFaces = mFaceDetector.detect(frame);
                       if (detectedFaces != null && detectedFaces.size() > 0){
                           float maxSize = 0;
                           int idx = 0;
                           for (int i = 0 ; i < detectedFaces.size(); i++){
                               Face face = (Face)detectedFaces.valueAt(i);
                               if (maxSize < (face.getWidth() * face.getHeight())){
                                   maxSize = (face.getWidth() * face.getHeight());
                                   idx = i;
                               }
                           }
                           Face face = (Face)detectedFaces.valueAt(idx);
                           // 目の位置が満足に取れないので顔の高さからざっくり計算する。
                           int width = mCorrectRotateBitmap.getWidth();
                           int height = (int)Math.floor(face.getHeight() / 4.0f);
                           int left = 0;
                           int top = (int)Math.floor(face.getPosition().y) + (int)Math.floor(height * 1.7f);
                           if ((height + top ) > mCorrectRotateBitmap.getHeight()){
                               height = mCorrectRotateBitmap.getHeight() - top;
                           }
                           Bitmap kaltuBitmap = Bitmap.createBitmap(mCorrectRotateBitmap, left, top, width, height);
                           try{
                               File file = new File(activity.getFilesDir(), "Kaltu.png");
                               FileOutputStream outStream = new FileOutputStream(file);
                               kaltuBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                           } catch (IOException e ){
                               Toast.makeText(activity, "Save bitmap failure", Toast.LENGTH_SHORT).show();
                           }
                           kaltuBitmap.recycle();
                           kaltuBitmap = null;

                           Message msgShowImage = Message.obtain();
                           msgShowImage.what = HDL_SHOW_KALTU;
                           mHandler.sendMessage(msgShowImage);

                           Message msgHideImage = Message.obtain();
                           msgHideImage.what = HDL_HIDE_KALTU;
                           mHandler.sendMessageDelayed(msgHideImage, 5000);
                       } else {
                           Toast.makeText(activity, "face not exists", Toast.LENGTH_SHORT).show();
                       }

                       bitmap.recycle();
                       bitmap = null;
                   }
               });
               if (!takePictureResult ) {
                   Toast.makeText(activity, "take picture failure", Toast.LENGTH_SHORT).show();
               }
            }
        });

        mTextureView = findViewById(R.id.textureView);
        mCamera = new CameraStateMachine();
        mKaltImage = findViewById(R.id.kaltImage);

        // create facedetector
        // Classification: ALL_CLASSIFICATIONS or NO_CLASSIFICATIONS
        // Landmark: ALL_LANDMARKS or NO_LANDMARKS
        // mode: FAST_MODE or ACCURATE_MODE
        mFaceDetector = new FaceDetector.Builder(activity)
                .setProminentFaceOnly(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setTrackingEnabled(false)
                .build();
    }

    @Override
    protected void onResume(){
       super.onResume();
       mCamera.open(this, mTextureView, CameraCharacteristics.LENS_FACING_FRONT);
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
        if (mCorrectRotateBitmap != null) {
            mCorrectRotateBitmap.recycle();
            mCorrectRotateBitmap = null;
        }
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
    }

    @SuppressLint("InlinedApi")
    private void show() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }
}
