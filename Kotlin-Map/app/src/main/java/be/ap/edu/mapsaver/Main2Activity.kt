package be.ap.edu.mapsaver

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_main2.*

class Main2Activity : AppCompatActivity() {

    internal var i: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity2)

        var naam: EditText = findViewById(R.id.naam)
        var aantal_loc: EditText = findViewById(R.id.aantal_loc)
        val button: Button = findViewById(R.id.button)

        Log.d("hi", "onCreate: hi")
        val extras = intent.extras ?: return
        // get data via the key
        val value1 = extras.getString("text1")
        val value2 = extras.getString("text2")
        if (value1 != null && value2 != null) {
            Log.d("value1", "onCreate: $value1")
            naam.setText(value1)
            aantal_loc.setText(value2)
        } else {
            return
        }

        button.setOnClickListener(View.OnClickListener {
            finish()
        })
    }

}
