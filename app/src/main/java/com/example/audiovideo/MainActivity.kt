package com.example.audiovideo

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.audiovideo.databinding.ActivityMainBinding
import java.io.File
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val PICK_VIDEO_REQUEST = 1001
    private val PICK_AUDIO_REQUEST = 2002
    private var videoPath: String? = null
    private var audioPath: String? = null
    private var videoUri: Uri? = null
    private var audioUri: Uri? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mediaPlayer = MediaPlayer()

        binding.inputVideo.setOnClickListener {
            picRequest(PICK_VIDEO_REQUEST)
        }

        binding.inputAudio.setOnClickListener {
            picRequest(PICK_AUDIO_REQUEST)
        }

        binding.mergeButton.setOnClickListener {
            mergeAudioVideo(audioPath, videoPath)
        }

        binding.play.setOnClickListener {
            playAudio(audioPath!!)
        }

        binding.pause.setOnClickListener {
            stopAudio()
        }

    }

    private fun picRequest(requestCode: Int) {
        if (requestCode == PICK_VIDEO_REQUEST) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PICK_VIDEO_REQUEST)
            } else {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                intent.type = "video/*";
                startActivityForResult(intent, PICK_VIDEO_REQUEST)
            }
        } else if (requestCode == PICK_AUDIO_REQUEST) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PICK_AUDIO_REQUEST)
            } else {
                val intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                intent.type = "audio/*";
                startActivityForResult(intent, PICK_AUDIO_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PICK_VIDEO_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    intent.type = "video/*";
                    startActivityForResult(intent, PICK_VIDEO_REQUEST)
                } else {
                    Log.d("test", "please allow READ_EXTERNAL_STORAGE for video")
                }
                return
            }
            PICK_AUDIO_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                    intent.type = "audio/*";
                    startActivityForResult(intent, PICK_AUDIO_REQUEST)
                } else {
                    Log.d("test", "please allow READ_EXTERNAL_STORAGE for audio")
                }
                return
            }
            else -> {
                // Ignore all other requests
            }
        }
    }


    private fun mergeAudioVideo(audioPath: String?, videoPath: String?) {
        val randomInt = Random.nextInt(1, 10100)
        val outputFileName = "merge_video_file$randomInt.mp4"
        val mp4UriAfterTrim = getExternalFilesDir(Environment.getExternalStorageState())?.let {
            getConvertedFile(it.absolutePath, outputFileName)
        }

        val cmd = arrayOf(
            "-i", videoPath,
            "-i", audioPath,
            "-c:v", "copy",
            "-c:a", "aac",
            "-map", "0:v:0",
            "-map", "1:a:0",
            "-shortest", mp4UriAfterTrim?.path
        )

        val result = FFmpeg.execute(cmd)
        if (result == RETURN_CODE_SUCCESS) {
            // Play the merged video
            binding.videoView2.setVideoPath(mp4UriAfterTrim?.path)
            binding.videoView2.start()
            Log.d("test", "Task is done successfully")
        } else {
            Log.d("test", "result code = $result")
        }

    }

    private fun getConvertedFile(directoryPath: String, fileName: String): File {
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, fileName)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_VIDEO_REQUEST) {
                binding.videoView.setVideoURI(data.data)
                binding.videoView.start()
                videoUri = data?.data!!
                videoPath = getPath(videoUri)
                Log.d("test", "videoUri = $videoUri")
                Log.d("test", "videoPath = $videoPath")
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                audioUri = data?.data!!
                audioPath = getAudioPath(audioUri!!)
                Log.d("test", "audioUri = $audioUri")
                Log.d("test", "audioPath = $audioPath")
            }
        }
    }

    private fun getPath(uri: Uri?): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        val cursor: Cursor? = contentResolver.query(uri!!, projection, null, null, null)
        return if (cursor != null) {
            val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(column_index)
        } else null
    }

    private fun getAudioPath(uri: Uri): String? {
        val contentResolver = applicationContext.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val outputFile = File.createTempFile("temp_audio", ".mp3", cacheDir)
        outputFile.outputStream().use { output ->
            inputStream?.use { input ->
                input.copyTo(output)
            }
        }
        return outputFile.absolutePath
    }

    private fun playAudio(audioFilePath: String) {
        mediaPlayer?.apply {
            reset()
            setDataSource(audioFilePath)
            prepare()
            start()
        }
    }

    private fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
        }
    }

}