package ch.heigvd.iict.dma.labo1.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ch.heigvd.iict.dma.labo1.models.*
import ch.heigvd.iict.dma.labo1.repositories.MeasuresRepository.Response
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

class GraphQLRepository(private val scope : CoroutineScope, private val httpsUrl : String = "https://mobile.iict.ch/graphql") {

    private val _working = MutableLiveData(false)
    val working : LiveData<Boolean> get() = _working

    private val _authors = MutableLiveData<List<Author>>(emptyList())
    val authors : LiveData<List<Author>> get() = _authors

    private val _books = MutableLiveData<List<Book>>(emptyList())
    val books : LiveData<List<Book>> get() = _books

    private val _requestDuration = MutableLiveData(-1L)
    val requestDuration : LiveData<Long> get() = _requestDuration

    fun resetRequestDuration() {
        _requestDuration.postValue(-1L)
    }

    private fun openConnection(query: String): HttpURLConnection {
        val urlConnection = URL(httpsUrl)
        val con = urlConnection.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.setRequestProperty("Content-Type", "application/json")
        con.setRequestProperty("User-Agent", "Larry_le_malicieux")
        val queryJson = "{\"query\":\"$query\"}"
        Log.d("GraphQL", "Query: $queryJson")
        con.outputStream.write(queryJson.toByteArray())
        con.outputStream.close()
        return con
    }

    fun loadAllAuthorsList() {

        scope.launch(Dispatchers.Default) {
            val elapsed = measureTimeMillis {

                val query = "{findAllAuthors{id, name}}"

                try{
                    // Effectue la requête GraphQL
                    val con = openConnection(query)
                    val result = con.inputStream.bufferedReader().use { it.readText() }
                    Log.d("GraphQL", "Result: $result")

                    // Parse le résultat JSON
                    val parsedResult = result.removePrefix("{\"data\":{\"findAllAuthors\":").removeSuffix("}}")
                    val authorsList = Gson().fromJson(parsedResult, Array<Author>::class.java)

                    // Met à jour la liste des auteurs
                    _authors.postValue(authorsList.toList())
                } catch (e: Exception){
                    Log.e("GraphQL", e.toString())
                    _authors.postValue(testAuthors)
                }
            }
            _requestDuration.postValue(elapsed)
        }
    }

    fun loadBooksFromAuthor(author: Author) {
        scope.launch(Dispatchers.Default) {

            //val query = "{findAllAuthors{id, name}}"

            val elapsed = measureTimeMillis {
                // TODO make the request to server
                // fill _books LiveData with list of book of the author

                //placeholder
                _books.postValue(testBooks)
            }
            _requestDuration.postValue(elapsed)
        }
    }

    companion object {
        //placeholder data - to remove
        private val testAuthors = listOf(Author(-1, "Test Author", emptyList()))
        private val testBooks = listOf(Book(-1, "Test Title", "01.01.2024", testAuthors))
    }

}