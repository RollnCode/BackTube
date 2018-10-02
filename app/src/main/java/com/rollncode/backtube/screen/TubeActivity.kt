package com.rollncode.backtube.screen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.rollncode.backtube.logic.toLog
import java.nio.charset.Charset

class TubeActivity : AppCompatActivity() {

    companion object {
        fun newInstance(context: Context) =
                TubeScreenController.newInstance(context, TubeActivity::class.java)
    }

    private lateinit var controller: TubeScreenController

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        controller = TubeScreenController(this, b)

        val string ="𝐓𝐡𝐞 𝐁𝐞𝐬𝐭 𝐎𝐟 𝐃𝐞𝐩𝐞𝐜𝐡𝐞 𝐌𝐨𝐝𝐞 𝐕𝐨𝐥𝐮𝐦𝐞 𝟏 (𝐑𝐞𝐦𝐚𝐬𝐭𝐞𝐫𝐞𝐝)"
        val bytesUTF32 = string.toByteArray(Charset.forName("UTF-32"))
        val bytesUTF8 = string.toByteArray(Charset.forName("UTF-8"))

        toLog(String(bytesUTF32, Charset.forName("UTF-8")))
        toLog(String(bytesUTF8, Charset.forName("UTF-32")))

        '\uD835'
        'a'//97
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        controller.onActivityResult(requestCode)
    }
}