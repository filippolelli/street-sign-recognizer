package it.unibo.sistemidigitali.app.kotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.annotation.KeepName
import it.unibo.sistemidigitali.app.R
import java.io.File
import java.util.concurrent.TimeUnit


@KeepName
class TakePhotoActivity : AppCompatActivity() {

    private lateinit var openGalleryLauncher: ActivityResultLauncher<Intent>

    private lateinit var previewView: PreviewView
    private lateinit var cameraPreview: Preview
    private lateinit var camera: Camera
    private lateinit var imageCapture: ImageCapture

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private val REQUEST_CAMERA_PERMISSION = 123
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_photo)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                this@TakePhotoActivity.REQUEST_CAMERA_PERMISSION
            )
        }
        //camerax async initialization
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            cameraXConfig(cameraProvider)
        }, ContextCompat.getMainExecutor(this))


        openGalleryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    launchCropPhotoActivity(result.data?.data!!)

                }
            }

        findViewById<View>(R.id.take_photo_button).setOnClickListener {
            takePhoto()
        }
        findViewById<View>(R.id.select_image_button).setOnClickListener {
            launchGalleryIntent()
        }



    }


    private fun cameraXConfig(cameraProvider: ProcessCameraProvider) {
        previewView=findViewById<PreviewView>(R.id.previewView)

        cameraPreview= Preview.Builder()
            .build()
        imageCapture = ImageCapture.Builder().build()

        var cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        cameraPreview.surfaceProvider = previewView.getSurfaceProvider()

        camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner, //camera same lifecycle as activity
            cameraSelector,
            cameraPreview,
            imageCapture
        )


        //setup zoom and tap to focus
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = camera.cameraInfo.zoomState.value?.zoomRatio?.times(detector.scaleFactor)
                camera.cameraControl.setZoomRatio(scale!!)
                return true
            }
        }
        scaleGestureDetector = ScaleGestureDetector(this, listener)
        previewView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)

            if (event.action == MotionEvent.ACTION_DOWN) {
                previewView.performClick()
                val factory = previewView.getMeteringPointFactory()
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                    .setAutoCancelDuration(5, TimeUnit.SECONDS)
                    .build()
                camera.cameraControl.startFocusAndMetering(action)
            }

            return@setOnTouchListener true
        }

        val torchButton: ImageButton =findViewById<ImageButton>(R.id.toggle_torch)
        torchButton.setOnClickListener{
            if (camera.cameraInfo.torchState.value == TorchState.ON) {
                camera.cameraControl.enableTorch(false)
                torchButton.setImageResource(R.drawable.torch_off)

            }

            else {
                camera.cameraControl.enableTorch(true)
                torchButton.setImageResource(R.drawable.torch_on)

            }
        }

    }

    private fun takePhoto() {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(this.filesDir, "temp.jpg")).build()
        imageCapture.takePicture(outputFileOptions,
            ContextCompat.getMainExecutor(this), // Defines where the callbacks are run
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    launchCropPhotoActivity(outputFileResults.savedUri!!)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG,exception.toString())
                }
            }
        )
    }
    private fun launchGalleryIntent() {

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        openGalleryLauncher.launch(intent)
    }

    private fun launchCropPhotoActivity(imageUri: Uri){

        val i = Intent(this, CropPhotoActivity::class.java);
        i.putExtra("imageUri",imageUri);
        startActivity(i);
    }



    companion object {
        private const val TAG = "TakePhotoActivity"
    }
}
