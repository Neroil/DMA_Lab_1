package ch.heigvd.iict.dma.labo1.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import ch.heigvd.iict.dma.labo1.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URL
import java.net.HttpURLConnection
import kotlin.system.measureTimeMillis

class MeasuresRepository(private val scope : CoroutineScope,
                         private val dtd : String = "https://mobile.iict.ch/measures.dtd",
                         private val httpUrl : String = "http://mobile.iict.ch/api",
                         private val httpsUrl : String = "https://mobile.iict.ch/api") {

    private val _measures = MutableLiveData(mutableListOf<Measure>())
    val measures = _measures.map { mList -> mList.toList().map { el -> el.copy() } }

    private val _requestDuration = MutableLiveData(-1L)
    val requestDuration : LiveData<Long> get() = _requestDuration

    fun generateRandomMeasures(nbr: Int = 3) {
        addMeasures(Measure.getRandomMeasures(nbr))
    }

    fun resetRequestDuration() {
        _requestDuration.postValue(-1L)
    }

    fun addMeasure(measure: Measure) {
        addMeasures(listOf(measure))
    }

    fun addMeasures(measures: List<Measure>) {
        val l = _measures.value!!
        l.addAll(measures)
        _measures.postValue(l)
    }

    fun clearAllMeasures() {
        _measures.postValue(mutableListOf())
    }

    class Response (val _id: Number?, val _status: String?) {
        var id: Number? = _id
        var status: String? = _status
    }

    fun sendMeasureToServer(encryption : Encryption, compression : Compression, networkType : NetworkType, serialisation : Serialisation) {
        scope.launch(Dispatchers.Default) {

            val url = when (encryption) {
                Encryption.DISABLED -> httpUrl
                Encryption.SSL -> httpsUrl
            }

            val contentType = when (serialisation) {
                Serialisation.JSON -> "application/json"
                Serialisation.XML -> "application/xml"
                Serialisation.PROTOBUF -> "application/protobuf"
            }

            val gson = Gson()

            val elapsed = measureTimeMillis {
                //Log.e("SendViewModel", "Implement me !!! Send measures to $url")

                val body = when (serialisation) {
                    Serialisation.JSON -> gson.toJson(measures.value)
                    Serialisation.XML -> TODO()
                    Serialisation.PROTOBUF -> TODO()
                }

                val urlConnection = URL(url)
                val con = urlConnection.openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", contentType)
                con.setRequestProperty("X-Content-Encoding", compression.toString())
                con.setRequestProperty("User-Agent", "Larry_le_malicieux")

                if (networkType != NetworkType.RANDOM) {
                    con.setRequestProperty("X-Network-Type", networkType.toString())
                }
                Log.d("Req", con.toString())
                Log.d("Req", body)

                // Ajoute le body
                val os = con.outputStream
                os.write(body.toByteArray())
                os.close()

                // Récupère la réponse
                val json = con.inputStream.bufferedReader().use { it.readText() }
                Log.d("Req", json)
                //val type = object : TypeToken<Response>() {}.type
                //Gson().fromJson<Response>(json, type)
            }
            _requestDuration.postValue(elapsed)
        }
    }

}