package it.unibo.sistemidigitali.app.kotlin

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import it.unibo.sistemidigitali.app.R
import kotlin.collections.get

class SignExplanationActivity : AppCompatActivity() {
    private val spiegazioni = mapOf<String, Pair<Int, Int>>(
        "stop" to Pair(R.string.stop, R.drawable.stop),  // Già fatto
        "precedenza" to Pair(R.string.precedenza, R.drawable.precedenza),
        "divieto_accesso" to Pair(R.string.divieto_accesso, R.drawable.divieto_accesso),
        "divieto_sosta" to Pair(R.string.divieto_sosta, R.drawable.divieto_sosta),
        "attraversamento" to Pair(R.string.attraversamento, R.drawable.attraversamento),
        "rotatoria" to Pair(R.string.rotatoria, R.drawable.rotatoria)
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_explanation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val label = intent.getStringExtra("label")

        findViewById<TextView>(R.id.sign).text = getString(spiegazioni[label]!!.first)
        findViewById<TextView>(R.id.sign).setCompoundDrawablesWithIntrinsicBounds(0,spiegazioni[label]?.second!!, 0, 0)
    }
}