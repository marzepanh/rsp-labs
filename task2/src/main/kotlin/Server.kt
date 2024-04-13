import java.io.Closeable
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext

class Server internal constructor(private val datagramSocket: DatagramSocket) : Closeable {
    constructor(port: Int) : this(DatagramSocket(port))

    private lateinit var myJob: Job
    var isShutdown = false
        private set

    private val buffer = ByteArray(256)

    suspend fun start() {
        if (isShutdown) return
        println("Server is listening on port ${datagramSocket.localPort}")

        myJob = CoroutineScope(coroutineContext).launch {
            outer@ while (coroutineContext.isActive && !isShutdown) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    datagramSocket.receive(packet) ?: break
                } catch (e: IOException) {
                    break
                }
                val receivedMessage = String(packet.data, 0, packet.length)
                println("Received message from client ${packet.address.hostAddress}:${packet.port}: $receivedMessage")
                if ((1..10).random() < 4) {
                    println("Dropping client packet")
                    continue@outer
                }
                println("Sending response to client ${packet.address.hostAddress}:${packet.port}")
                datagramSocket.send(
                    DatagramPacket(
                        receivedMessage.uppercase().toByteArray(),
                        receivedMessage.length,
                        packet.address,
                        packet.port
                    )
                )
            }
        }
    }

    override fun close() {
        isShutdown = true
        runBlocking {
            myJob.cancel()
            datagramSocket.close()
        }
    }
}