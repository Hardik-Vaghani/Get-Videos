package com.hardik.getvideos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.hardik.getvideos.common.FragmentSessionUtils
import com.hardik.getvideos.ui.VideoFragment

class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    val fragmentSessionUtils = FragmentSessionUtils.getInstance()
    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // load the default Fragment with data
        if (savedInstanceState == null) {
            switchToSplashScreenFragment()
        }
    }

    private fun switchToSplashScreenFragment() {
        Log.d(TAG, "switchToNotepadFragment: ")

        Handler(Looper.getMainLooper()).run {
            postDelayed({
                if (currentFragment !is VideoFragment) {
                    fragmentSessionUtils.switchFragment(
                        supportFragmentManager,
                        VideoFragment(),
                        false,
                    )
                }
            }, 0)
        }
    }
}