package ch.heigvd.iict.dma.labo1.service

import android.util.Log
import ch.heigvd.iict.dma.labo1.database.MessagesDatabase
import ch.heigvd.iict.dma.labo1.models.Message
import ch.heigvd.iict.dma.labo1.repositories.MessagesRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Calendar

class FirebaseService : FirebaseMessagingService() {

    private val repository by lazy {
        MessagesRepository(MessagesDatabase.getDatabase(this).messagesDao())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (message.data.isNotEmpty()) {
            Log.d("FCM_MessageData", "Message data payload: ${message.data}")

            val currentTime = Calendar.getInstance()

            val msg = Message(
                id = null,
                sentDate = Calendar.getInstance().apply { timeInMillis = message.sentTime },
                receptionDate = currentTime,
                message = message.data["message"],
                command = message.data["command"]
            )

            if(msg.command == "clear") repository.deleteAllMessage()
            else repository.insert(msg)

        } else {
            Log.d("FCM_MessageData", "No data payload received in this message.")
        }

    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_Token", token)
    }
}