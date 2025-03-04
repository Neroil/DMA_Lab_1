package ch.heigvd.iict.dma.labo1.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import ch.heigvd.iict.dma.labo1.models.*
import ch.heigvd.iict.dma.protobuf.MeasuresOuterClass
import com.google.gson.Gson
import com.google.protobuf.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jdom2.DocType
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import org.jdom2.output.XMLOutputter
import java.io.InputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream
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
                    Serialisation.JSON -> gson.toJson(measures.value).toByteArray()
                    Serialisation.XML -> toXML().toByteArray()
                    Serialisation.PROTOBUF -> {
                        val builder = MeasuresOuterClass.Measures.newBuilder()
                        measures.value!!.forEach { builder.addMeasures(it.toProtobuf()) }
                        builder.build().toByteArray()
                    }
                }

                val urlConnection = URL(url)
                val con = urlConnection.openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.setRequestProperty("Content-Type", contentType)
                Log.d("Req", "compression envoyée : ${compression.toString()}")
                con.setRequestProperty("X-Content-Encoding", compression.toString())
                con.setRequestProperty("User-Agent", "Larry_le_malicieux")

                if (networkType != NetworkType.RANDOM) {
                    con.setRequestProperty("X-Network-Type", networkType.toString())
                }

                val os = con.outputStream
                when (compression){
                    Compression.DISABLED -> {
                        os.write(body)
                        os.close()
                    }
                    Compression.DEFLATE -> {
                        val dos = DeflaterOutputStream(os, Deflater(Deflater.BEST_COMPRESSION,true))
                        dos.write(body)
                        dos.close()
                    }
                }

                // Récupère la réponse
                val inpst = when (con.getHeaderField("X-Content-Encoding")){
                    "DEFLATE" -> {
                        InflaterInputStream(con.inputStream)

                    }
                    else -> {
                        con.inputStream
                    }
                }

                val response = inpst.bufferedReader().use { it.readText() }

                Log.d("Req", "header content type : ${con.getHeaderField("Content-Type")}")
                when (con.getHeaderField("Content-Type")){
                    "application/json;charset=UTF-8" -> {
                        Log.d("Req", "response: $response")
                        val statusList = gson.fromJson(response, Array<Response>::class.java)
                        for (status in statusList){
                            updateMeasureStatus(Measure.Status.valueOf(status.status!!), status.id!!.toInt())
                        }
                    }
                    "application/xml;charset=UTF-8" -> {
                        try {
                            val builder = SAXBuilder()
                            // Crashes if not set to false
                            builder.setExpandEntities(false)

                            val responseDoc = builder.build(StringReader(response))
                            val measureElements = responseDoc.rootElement.getChildren("measure")

                            for (measureElement in measureElements) {
                                val id = measureElement.getAttributeValue("id").toInt()
                                val status = Measure.Status.valueOf(measureElement.getAttributeValue("status"))
                                updateMeasureStatus(status, id)
                            }
                        } catch (e: Exception) {
                            Log.e("XML", "Error parsing XML: ${e.message}", e)
                        }
                    }
                    "application/protobuf" -> {
                        val builder = MeasuresOuterClass.MeasuresAck.newBuilder()
                        builder.mergeFrom(response.toByteArray())
                        try{
                            for(ack in builder.build().measuresList){
                                updateMeasureStatus(Measure.Status.valueOf(ack.status.toString()), ack.id)
                            }
                        } catch (e: Exception) {
                            Log.e("Protobuf", "Error parsing Protobuf: ${e.message}", e)
                        }
                    }
                }

                //val type = object : TypeToken<Response>() {}.type
                //Gson().fromJson<Response>(json, type)
            }
            _requestDuration.postValue(elapsed)
        }
    }

    fun toXML(): String{
        try{
            val doc = Document()
            doc.setRootElement(Element("measures"))
            doc.setDocType(DocType("measures",dtd))
            measures.value?.forEach {
                //Measure
                val measure = Element("measure")
                measure.setAttribute("id", it.id.toString())
                measure.setAttribute("status", it.status.toString())
                //Type
                measure.addContent(Element("type").addContent(it.type.toString()))
                //Value
                measure.addContent(Element("value").addContent(it.value.toString()))
                //Date
                measure.addContent(Element("date").addContent(it.date.toString()))
                doc.rootElement.addContent(measure)
            }

            return XMLOutputter().outputString(doc)

        } catch (e: Exception){
            Log.e("XML", e.toString())
            return ""
        }
    }

    private fun updateMeasureStatus(status: Measure.Status, id: Int){
        val l = _measures.value!!
        for (measure in l) {
            if (measure.id == id) {
                measure.status = status
                _measures.postValue(l)
                break
            }
        }
    }


}