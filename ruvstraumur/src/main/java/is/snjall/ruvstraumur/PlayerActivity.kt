/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package `is`.snjall.ruvstraumur

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.android.volley.toolbox.JsonObjectRequest
import `is`.snjall.ruvstraumur.databinding.ActivityPlayerBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URL

private const val TAG = "PlayerActivity"

/**
 * A fullscreen activity to play audio or video streams.
 */
class PlayerActivity : AppCompatActivity() {

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    private val playbackStateListener: Player.Listener = playbackStateListener()
    private var player: ExoPlayer? = null

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private var streamUrl = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun buildAndInitializePlayer() {
//        player = ExoPlayer.Builder(this)
//            .build()
        initializePlayer()
    }

    private fun initializePlayer() {
        // val trackSelector = DefaultTrackSelector(this).apply {
        //     setParameters(buildUponParameters().setMaxVideoSizeSd())
        // }

        player = ExoPlayer.Builder(this)
//            .setTrackSelector(trackSelector)
            .build()

        // TODO: Add ability to display an error message when the video player has a 403 error
//        player!!.addListener(object : Player.Listener {
//                    fun onPlayerError(error: ExoPlaybackException) {
//                        when(error.type){
//                            ExoPlaybackException.TYPE_REMOTE -> {
//                                Log.e("error", error.localizedMessage)
//                            }
//                            ExoPlaybackException.TYPE_RENDERER -> {
//                                Log.e("error", error.localizedMessage)
//                            }
//                            ExoPlaybackException.TYPE_SOURCE -> {
//                                Log.e("error", error.localizedMessage)
//                            }
//                            ExoPlaybackException.TYPE_UNEXPECTED -> {
//                                Log.e("error", error.localizedMessage)
//                            }
//                        }
//                    }
//                })
//        val ruvDataStream = async(Dispatchers.IO) {
//            // TODO: Capture errors and handle them here
//            JSONTokener(URL(getString(R.string.ruv_stream_json)).readText()).nextValue() as JSONObject
//        }
//        val ruvdata = ruvDataStream.await()
//        val ruvstream = ruvdata.getString("url")
//        Log.d("stream", ruvstream)
        .also { exoPlayer ->
            viewBinding.videoView.player = exoPlayer
            //exoPlayer.setMediaItem(MediaItem.fromUri(ruvstream))
            exoPlayer.setMediaItem(MediaItem.fromUri(getString(R.string.ruv_hls_stream)))
            exoPlayer.playWhenReady = playWhenReady
            exoPlayer.seekTo(currentItem, playbackPosition)
            exoPlayer.addListener(playbackStateListener)
            exoPlayer.prepare()
        }

    }

//    suspend fun getRuvStream(): JSONObject {
//        return JSONTokener(URL(getString(R.string.ruv_stream_json)).readText()).nextValue() as JSONObject;
//    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.release()
        }
        player = null
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBinding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

private fun playbackStateListener() = object : Player.Listener {
    override fun onPlaybackStateChanged(playbackState: Int) {
        val stateString: String = when (playbackState) {
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
            else -> "UNKNOWN_STATE             -"
        }
        Log.d(TAG, "changed state to $stateString")
    }
}