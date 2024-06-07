package com.example.client

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.client.ui.theme.ClientTheme
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
fun MainContent() {
    val appText = stringResource(id = R.string.app_text)
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedFileUri = result.data?.data
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = appText)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "text/plain"
                }
                launcher.launch(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Attach .txt file")
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (selectedFileUri != null) {
            Text(text = "Selected file: ${selectedFileUri.toString()}")

            Button(
                onClick = {
                    val file = File(context.filesDir, "temp.txt")
                    try {
                        context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                            file.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        uploadFile(file)
                    } catch (e: IOException) {
                        // Обработайте ошибку
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Upload File")
            }
        }
    }
}

interface UploadService {
    @Multipart
    @POST("http://10.0.2.2:8080")
    fun uploadFile(@Part("file") file: RequestBody): Call<Any>
}

fun uploadFile(file: File) {
    val gson: Gson = GsonBuilder().create()
    val client = OkHttpClient()
    val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .build()

    val service = retrofit.create(UploadService::class.java)

    val MEDIA_TYPE_TEXT = "text/plain".toMediaType()
    val requestBody = file.asRequestBody(MEDIA_TYPE_TEXT)

    service.uploadFile(requestBody).enqueue(object : Callback<Any> {
        override fun onFailure(call: Call<Any>, t: Throwable) {
            Log.e("UploadFileError", "Ошибка загрузки файла: ${t.message}")
        }

        override fun onResponse(call: Call<Any>, response: Response<Any>) {
            Log.i("UploadFileCorrectly", "Корректная загрузка файла: ${response.message()}")
            // TODO обработать вывод от сервера
        }
    })
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ClientTheme {
        MainContent()
    }
}