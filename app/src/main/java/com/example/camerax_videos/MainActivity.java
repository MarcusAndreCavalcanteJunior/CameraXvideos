package com.example.camerax_videos;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ImageButton imgRec, imgFlash, imgFlip;
    ExecutorService service;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;

    private final ActivityResultLauncher<String> activityResultLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean o) {
                            if (ActivityCompat.checkSelfPermission(
                                    MainActivity.this,
                                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                                startCamera(cameraFacing);
                            }

                        }
                    }
            );




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imgRec = findViewById(R.id.img_rec);
        imgFlash = findViewById(R.id.img_flash);
        imgFlip = findViewById(R.id.img_flip);
        previewView = findViewById(R.id.preview);
        imgRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
                    activityResultLauncher.launch(Manifest.permission.CAMERA);
                } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
                    
                } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(
                        MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    captureVideo();
                }
            }
        });

        imgFlip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK){
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                }else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing);
            }
        });

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        }else {
            startCamera(cameraFacing);
        }

        service = Executors.newSingleThreadExecutor();
    }

    private void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> processCameraProvider = ProcessCameraProvider.getInstance(
                MainActivity.this);
        processCameraProvider.addListener(() ->{
            try{
                ProcessCameraProvider provider = processCameraProvider.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                Recorder recorder = new Recorder.Builder().setQualitySelector(
                        QualitySelector.from(Quality.HIGHEST)).build();
                videoCapture = VideoCapture.withOutput(recorder);

                provider.unbindAll();

                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(cameraFacing).build();

                Camera camera = provider.bindToLifecycle(MainActivity.this, cameraSelector, preview, videoCapture);

                imgFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        toggleFlash(camera);
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(MainActivity.this));
    }

    private void toggleFlash(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()){
            if (camera.getCameraInfo().getTorchState().getValue() == 0){
                    camera.getCameraControl().enableTorch(true);
                    imgFlash.setImageResource(R.drawable.flash_off);
            } else {
                camera.getCameraControl().enableTorch(false);
                imgFlash.setImageResource(R.drawable.flash);
            }
        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Não tem flash",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void captureVideo() {
        imgRec.setImageResource(R.drawable.stop);
        Recording recording1 = recording;
        if (recording1 != null){
            recording1.stop();
            recording = null;
            return;
        }
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED){
            return;
        }

        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options)
                .withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), videoRecordEvent -> {
                    if (videoRecordEvent instanceof VideoRecordEvent.Start){
                        imgRec.setEnabled(true);
                    } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                        if (((VideoRecordEvent.Finalize) videoRecordEvent).hasError()){
                            recording.close();
                            recording = null;
                            Toast.makeText(this, "Erro ao grvar", Toast.LENGTH_SHORT).show();
                        }else{
                            String msg = String.valueOf(((VideoRecordEvent.Finalize) videoRecordEvent).
                                    getOutputResults().getOutputUri());
                            Toast.makeText(this, "Video salvo em " + msg, Toast.LENGTH_SHORT).show();
                        }
                        imgRec.setImageResource(R.drawable.rec);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service.shutdown();
    }
}