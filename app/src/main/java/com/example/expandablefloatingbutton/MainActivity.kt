package com.example.expandablefloatingbutton

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        floatingButton.setOnChangeListener(object :ExpandedFloatingButton.OnChangeListener{
            override fun onMainActionSelected(): Boolean {
                val intent = Intent(this@MainActivity,SecondActivity::class.java)
                startActivityForResult(intent,1)
                overridePendingTransition(0,0)
                return false
            }

            override fun onToggleChanged(isOpen: Boolean) {

            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        floatingButton.close()
    }

    override fun onPause() {
//        overridePendingTransition(0,0)
        super.onPause()
    }
}
