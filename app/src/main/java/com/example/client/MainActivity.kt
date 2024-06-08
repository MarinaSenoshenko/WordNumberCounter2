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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.client.ui.theme.ClientTheme
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.*

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
    val wordCounts by remember { mutableStateOf(mutableStateListOf<Pair<String, Int>>()) }
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
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = appText, fontWeight = FontWeight.Bold)
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
            Text(text = "Прикрепить .txt file")
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (selectedFileUri != null) {
            Text(text = "Ссылка на выбранный файл: ${selectedFileUri.toString()}")
            Button(
                onClick = {
                    val file = File(context.filesDir, "temp.txt")
                    try {
                        context.contentResolver.openInputStream(selectedFileUri!!)?.use { inputStream ->
                            file.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        uploadFile(file) { response ->
                            wordCounts.clear()
                            wordCounts.addAll(response.map { it.key to it.value })
                        }
                    } catch (e: IOException) {
                        Log.e("FormFileError", "Ошибка создания файла: ${e.stackTrace}")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Узнать")
            }
            if (wordCounts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {Text(text = "Слово", style = MaterialTheme.typography.bodyLarge)
                        Text(text = "Количество", style = MaterialTheme.typography.bodyLarge)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(wordCounts) { (word, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(0.dp)
                                    .border(1.dp, Color.LightGray),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = word)
                                Text(text = count.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}

interface UploadService {
    @Multipart
    @POST("http://10.0.2.2:8080")
    fun uploadFile(@Part file: MultipartBody.Part): Call<Map<String, Int>> // Возвращаем Map<String, Int>
}

fun uploadFile(file: File, onSuccess: (Map<String, Int>) -> Unit) {
    val gson: Gson = GsonBuilder().create()
    val client = OkHttpClient()
    val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:8080")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .build()

    val service = retrofit.create(UploadService::class.java)

    val requestBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
    val bodyPart = MultipartBody.Part.createFormData("file", file.name, requestBody)

    service.uploadFile(bodyPart).enqueue(object : Callback<Map<String, Int>> {
        override fun onFailure(call: Call<Map<String, Int>>, t: Throwable) {
            Log.e("UploadFileError", "Ошибка загрузки файла: ${t.message}")
        }

        override fun onResponse(
            call: Call<Map<String, Int>>,
            response: Response<Map<String, Int>>
        ) {
            if (response.isSuccessful) {
                val responseBody = response.body()!!
                Log.i("UploadFileCorrectly", "Ответ от сервера: $responseBody")

                println("Слово | Количество")
                responseBody.forEach { (word, count) ->
                    println("$word | $count")
                }
                onSuccess(responseBody)
            } else {
                Log.e("UploadFileError", "Ошибка загрузки файла: ${response.errorBody()?.string()}")
            }
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