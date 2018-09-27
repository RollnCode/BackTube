package com.rollncode.backtube.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.rollncode.backtube.R
import com.rollncode.backtube.R.id

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_close, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            if (item.itemId == id.menu_close) {
                controller.closeBackTube()
                true

            } else {
                super.onOptionsItemSelected(item)
            }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        controller.onActivityResult(requestCode)
    }
}