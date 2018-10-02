package com.rollncode.backtube.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class TubeActivity : AppCompatActivity() {

    companion object {
        fun newInstance(context: Context) =
                TubeScreenController.newInstance(context, TubeActivity::class.java)
    }

    private lateinit var controller: TubeScreenController

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        controller = TubeScreenController(this, b)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        controller.onActivityResult(requestCode)
    }
}