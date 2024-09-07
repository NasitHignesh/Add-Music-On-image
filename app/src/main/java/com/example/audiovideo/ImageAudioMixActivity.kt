package com.example.audiovideo

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.audiovideo.databinding.ActivityImageAudioMixBinding
import java.io.File
import java.util.Random


class ImageAudioMixActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageAudioMixBinding
    private val PICK_IMAGE_REQUEST_CODE = 1001
    private val PICK_AUDIO_REQUEST_CODE = 1002
    private var imagePath: String? = null
    private var imageUri: Uri? = null
    private var audioPath: String? = null
    private var audioUri: Uri? = null
    private var mediaPlayer: MediaPlayer? = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageAudioMixBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.picImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*";
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }

        binding.picAudio.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            intent.type = "audio/*";
            startActivityForResult(intent, PICK_AUDIO_REQUEST_CODE)
        }

        binding.play.setOnClickListener {
            playAudio(audioPath!!)
        }

        binding.stop.setOnClickListener {
            stopAudio()
        }

        binding.combine.setOnClickListener {
            combineImageAndAudio(imagePath, audioPath)
        }
    }

    private fun combineImageAndAudio(imagePath: String?, audioPath: String?) {
        val randomInt = kotlin.random.Random.nextInt(1, 1010)
        val outputFileName = "merged_video$randomInt.mp4"
        val outputFilePath = getExternalFilesDir(Environment.getExternalStorageState())?.let {
            getConvertedFile(it.absolutePath, outputFileName)
        }

        Log.d("test", "outputFilePath : $outputFilePath")

        val cmds = arrayOf(
            "-y", "-loop",
            "1", "-r", "1",
            "-i", imagePath,
            "-i", audioPath,
            "-acodec", "aac",
            "-vcodec", "mpeg4",
            "-strict", "experimental",
            "-b:a","92k", "-shortest",
            "-f", "mp4",
            "-r", "2",
            outputFilePath?.path
        )

        val result = FFmpeg.execute(cmds)
        if (result == RETURN_CODE_SUCCESS) {
            binding.videoView.setVideoPath(outputFilePath?.path)
            binding.videoView.start()
            Log.d("test", "Merge completed successfully")
        } else {
            Log.e("test", "Merge failed. Result code: $result")
        }
    }



    fun createVideoFromImagesAndAudio(imagePath: String, audioPath: String, outputFilePath: String): Boolean {
        val ffmpeg = "ffmpeg" // Assuming ffmpeg is in your PATH

        val cmd = arrayOf(
            "-y", "-loop", "1", "-r", "1",
            "-i", imagePath, "-i", audioPath,
            "-acodec", "aac", "-vcodec", "mpeg4",
            "-strict", "experimental", "-b:a", "92k",
            "-shortest", "-f", "mp4", "-r", "2",
            outputFilePath
        )

        try {
            // Execute FFmpeg command
            val process = ProcessBuilder(*cmd)
                .directory(File(Environment.getExternalStorageDirectory().absolutePath))
                .redirectErrorStream(true)
                .start()

            // Wait for the process to finish
            process.waitFor()

            // Check if the process exited successfully
            return process.exitValue() == 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
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
            if (requestCode == PICK_IMAGE_REQUEST_CODE) {
                imageUri = data.data
                imagePath = getImageRealPathFromURI(this, imageUri!!)
                binding.imageView.setImageURI(imageUri)
                Log.d("test", "imageUri = $imageUri")
                Log.d("test", "imagePath = $imagePath")
            } else if (requestCode == PICK_AUDIO_REQUEST_CODE) {
                audioUri = data?.data!!
                audioPath = getAudioPath(audioUri!!)
                Log.d("test", "audioUri = $audioUri")
                Log.d("test", "audioPath = $audioPath")
            }
        }
    }

    private fun getImageRealPathFromURI(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            path = it.getString(columnIndex)
        }
        return path
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