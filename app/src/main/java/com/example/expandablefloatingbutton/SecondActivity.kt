package com.example.expandablefloatingbutton

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.second.*

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.second)
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        floatingButton.setOnChangeListener(object : ExpandedFloatingButton.OnChangeListener {
            override fun onMainActionSelected(): Boolean {
                return false
            }

            override fun onToggleChanged(isOpen: Boolean) {
                if (!isOpen) {
                    finish()
                }
            }
        })

        floatingButton.addActionItem(
            FloatingActionItem.Builder(R.id.action_first_floating_button, R.drawable.ic_help_outline_black_24dp)
                .setFloatingButtonSize(FloatingActionButton.SIZE_NORMAL)
                .setActionText("動態貼文")
                .setActionTextColor(Color.WHITE)
                .setActionTextBackgroundColor(Color.TRANSPARENT)
                .create()
        )

        floatingButton.addActionItem(
            FloatingActionItem.Builder(R.id.action_second_floating_button, R.drawable.ic_help_outline_black_24dp)
                .setFloatingButtonSize(FloatingActionButton.SIZE_NORMAL)
                .setActionText("動態貼文")
                .setActionTextColor(Color.WHITE)
                .setActionTextBackgroundColor(Color.TRANSPARENT)
                .create()
        )
        floatingButton.open()
    }

    override fun finish() {
        floatingButton.close()
        floatingButton.post {
            super.finish()
            overridePendingTransition(0, 0);
        }
    }

    override fun onBackPressed() {
        if (floatingButton.isOpen) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}