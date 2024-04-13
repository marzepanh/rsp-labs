import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.time.temporal.ChronoField
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class Client internal constructor(private val datagramSocket: DatagramSocket) : Closeable {
    constructor() : this(DatagramSocket())

    private var requestCounter = -1
    private val buffer = ByteArray(256)

    fun ping(target: String, port: Int, timeout: Duration = 1.seconds): Duration {
        val timestampBefore = LocalDateTime.now()
        requestCounter++
        datagramSocket.soTimeout = timeout.inWholeMilliseconds.toInt()
        val messageBytes = "Sending ping request to $target: attempt $requestCounter at ${timestampBefore.get(ChronoField.HOUR_OF_DAY)}:${timestampBefore.get(ChronoField.MINUTE_OF_HOUR)}:${timestampBefore.get(ChronoField.SECOND_OF_MINUTE)}".toByteArray()
        println("Sent ping request to $target:$port")
        datagramSocket.send(DatagramPacket(messageBytes, messageBytes.size, InetAddress.getByName(target), port))

        val receivedPacket = DatagramPacket(buffer, buffer.size)
        try {
            datagramSocket.receive(receivedPacket)
        } catch (e: SocketTimeoutException) {
            println("$requestCounter Ping request to $target timed out")
            throw e
        }
        val timestampAfter = LocalDateTime.now()
        val receivedMessage = String(receivedPacket.data, 0, receivedPacket.length)
        val roundTripTime =
            (timestampAfter.getLong(ChronoField.MILLI_OF_SECOND) - timestampBefore.getLong(ChronoField.MILLI_OF_SECOND)).milliseconds
        println("$requestCounter|\t Response from $target: $receivedMessage|\t Round-trip time: ${roundTripTime.inWholeMilliseconds} ms")
        return roundTripTime
    }

    override fun close() {
        datagramSocket.close()
    }
}