package fr.nextu.kouache.kotlinapp

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import fr.nextu.kouache.kotlinapp.databinding.ActivityMain2Binding
import fr.nextu.kouache.kotlinapp.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


class MainActivity2 : AppCompatActivity() {
    val db: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }
    lateinit var jsonTextView: TextView
    lateinit var movies_recycler: RecyclerView
    private lateinit var binding: ActivityMain2Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        createNotificationChannel()
        movies_recycler = findViewById<RecyclerView>(R.id.movies_recylcer).apply {
            adapter = MovieAdapter(emptyList())
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity2)
        }

        binding.back.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showNotificationSettingsAlert() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("Notifications are disabled. Please enable them in Settings to continue receiving important updates.")
            .setPositiveButton("Settings") { dialog, which ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun areNotificationsEnabled(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled()
    }

    override fun onStart() {
        super.onStart()
        if (!areNotificationsEnabled()) {
            showNotificationSettingsAlert()
        }
        updateViewFromDB()
        requestMoviesList { jsonResponse ->
            parseMoviesFromJson(jsonResponse)
        }
    }
    override fun onStop() {
        super.onStop()
    }

    fun updateViewFromDB() {
        CoroutineScope(Dispatchers.IO).launch {
            val flow = db.movieDao().getFlowData()
            flow.collect { movies ->
                CoroutineScope(Dispatchers.Main).launch {
                    (movies_recycler.adapter as MovieAdapter).updateMovies(movies)
                    Log.d("Database Fetch", "Fetched ${movies.size} movies from DB")
                }
            }
        }
    }

    fun parseMoviesFromJson(json: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val gson = Gson()
            val type = object : TypeToken<Movies>() {}.type
            try {
                val moviesResponse = gson.fromJson<Movies>(json, type)
                val insertResults = db.movieDao().insertAll(*moviesResponse.movies.toTypedArray())

                CoroutineScope(Dispatchers.Main).launch {
                    if (insertResults.any { it == -1L }) {
                        Log.e("Database Insert", "Error inserting some movies")
                    } else {
                        Log.d("Database Insert", "All movies inserted successfully")
                        (movies_recycler.adapter as? MovieAdapter)?.updateMovies(moviesResponse.movies)
                    }
                }
            } catch (e: JsonSyntaxException) {
                Log.e("JSON Parsing Error", "Error parsing JSON", e)
            }
        }
    }
        companion object {
            const val CHANNEL_ID = "fr_nextu_guerton_pierreemmanuel_channel_notification"
            private const val NOTIFICATION_ID = 1

        }

        fun requestMoviesList(callback: (String) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                val client = OkHttpClient()
                val request: Request = Request.Builder()
                    .url("https://api.betaseries.com/movies/list")
                    .get()
                    .addHeader("X-BetaSeries-Key", getString(R.string.betaseries_api_key))
                    .build()

                try {
                    val response: Response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        notifyNewData(responseBody) // Pass the string directly
                        withContext(Dispatchers.Main) {
                            callback(responseBody) // Callback is now executed on the main thread
                        }
                    } else {
                        Log.e("HTTP Request", "Failed with code ${response.code}")
                        withContext(Dispatchers.Main) {
                            callback("Error: Failed with code ${response.code}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HTTP Request", "Exception during request", e)
                    withContext(Dispatchers.Main) {
                        callback("Error: ${e.message}")
                    }
                }
            }
        }

        private fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "movie update"
                val descriptionText = "A update notif when new movies come"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun notifyNewData(responseBody: String) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(this, MainActivity2::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val viewPendingIntent: PendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
            )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.movies_updated_title))
            .setContentText("Tap to view more details.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(responseBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_dialog_info, "View", viewPendingIntent)
            .setAutoCancel(true)

        when {
            ContextCompat.checkSelfPermission(this,POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
            }
            else -> {
                val requestPermissionLauncher =
                    this.registerForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted: Boolean ->
                        if (isGranted) {
                            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
                        }
                    }
                requestPermissionLauncher.launch(POST_NOTIFICATIONS)
            }
        }
    }



}

