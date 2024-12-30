package it.unibo.sistemidigitali.app.kotlin

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import it.unibo.sistemidigitali.app.R
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import it.unibo.sistemidigitali.app.kotlin.ui.CropAreaView
import java.io.IOException


class CropPhotoActivity : AppCompatActivity() {
    private lateinit var cropArea: CropAreaView
    private lateinit var imageView: ImageView
    private lateinit var imageUri: Uri
    private lateinit var classifyButton: ImageButton
    private lateinit var imageLabeler: ImageLabeler
    private var adjusted=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_crop_photo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createImageLabeler()
        cropArea=findViewById<CropAreaView>(R.id.cropArea)
        imageView = findViewById<ImageView>(R.id.imageView)
        classifyButton=findViewById<ImageButton>(R.id.classify)

        imageUri=intent.getParcelableExtra<Uri>("imageUri")!!
        imageView.setImageURI(imageUri)

        imageView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->


            // La View è stata ridimensionata o spostata
            val newWidth = right - left
            val newHeight = bottom - top



            //layoutParams.height = imageView.height-cropArea.radius.toInt()*2
            if (!adjusted){
                var layoutParams = cropArea.layoutParams
                layoutParams.width=newWidth
                layoutParams.height=newHeight
                cropArea.post{            cropArea.layoutParams=layoutParams
                }
            }
            else{
                var layoutParams = cropArea.layoutParams
                layoutParams.width=newWidth+cropArea.radius.toInt()*2
                layoutParams.height=newHeight+cropArea.radius.toInt()*2
                cropArea.post{            cropArea.layoutParams=layoutParams
                }
            }




        }
        imageView.post{
            adjusted=true
            var layoutParams=imageView.layoutParams
            layoutParams.width = imageView.width-cropArea.radius.toInt()*2
            imageView.layoutParams=layoutParams


        }


        classifyButton.setOnClickListener{
            classifyBufferImage(cropImage()!!)
        }


    }


    private fun cropImage(): Bitmap? {
        val drawable = imageView.drawable

        val bitmap = drawable.toBitmap()

        val boundaryPoints=cropArea.boundaryPoints
        val imageViewWidth = imageView.width
        val imageViewHeight = imageView.height
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val scaleX = bitmapWidth.toFloat() / imageViewWidth.toFloat()
        val scaleY = bitmapHeight.toFloat() / imageViewHeight.toFloat()

        val left = (boundaryPoints["lt"]!!.x-cropArea.radius)*scaleX
        val top = (boundaryPoints["lt"]!!.y-cropArea.radius) *scaleY
        val right = (boundaryPoints["rb"]!!.x-cropArea.radius)*scaleX
        val bottom = (boundaryPoints["rb"]!!.y-cropArea.radius)*scaleY
        val width = right - left
        val height = bottom - top
        return Bitmap.createBitmap(bitmap, left.toInt(), top.toInt(), width.toInt(), height.toInt())
    }


    private fun createImageLabeler() {
        try {
            Log.i(TAG, "Using Custom Image Label Detector Processor")
            val localClassifier =
                LocalModel.Builder().setAssetFilePath("custom_models/sign_classifier.tflite")
                    .build()
            val customImageLabelerOptions =
                CustomImageLabelerOptions.Builder(localClassifier).build()
            imageLabeler = ImageLabeling.getClient(customImageLabelerOptions)


        } catch (e: Exception) {
            Log.e(TAG, "Can not create image labeling processor", e)

        }


    }


    private fun classifyBufferImage(image: Bitmap) {
        Log.i(TAG, "Classifying image buffer {$image}")
        imageLabeler.process(InputImage.fromBitmap(image, 0))
            .addOnSuccessListener { result: List<ImageLabel> -> this.onClassificationSuccess(result) }
            .addOnFailureListener { e: Exception ->
                Log.w(TAG, "Image classification failed: $e")
            }

    }
    private fun onClassificationSuccess(labels: List<ImageLabel>) {
        val i = Intent(this, SignExplanationActivity::class.java)
        i.putExtra("label",labels.first().text)

        startActivity(i)
    }


    override fun onResume() {
        super.onResume()
        createImageLabeler()


    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        try {
            imageLabeler.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close ImageLabelerClient: $e"
            )
        }
    }
    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        try {
            imageLabeler.close()
        } catch (e: IOException) {
            Log.e(
                TAG,
                "Exception thrown while trying to close ImageLabelerClient: $e"
            )
        }
    }

    companion object {
        private const val TAG = "CropPhotoActivity"
    }

}