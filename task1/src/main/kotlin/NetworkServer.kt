import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.Path
import kotlin.io.path.Path as KotlinPath
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class NetworkServer internal constructor(private val serverSocket: ServerSocket) : Closeable {
    constructor(port: Int = 80, timeout: Duration = 5000.milliseconds) : this(ServerSocket(port)) {
        serverSocket.soTimeout = timeout.toInt(DurationUnit.MILLISECONDS)
    }

    private lateinit var job: Job
    var isClosed = false
        private set

    private var activeConnectionsCount = AtomicInteger(0)

    suspend fun start() = coroutineScope {
        if (isClosed) return@coroutineScope

        job = launch {
            outer@ while (isActive) {
                println("Awaiting client connection on port ${serverSocket.localPort}")
                val deferredClient = async { serverSocket.acceptCancellable() }

                val clientConnection = deferredClient.await() ?: break@outer
                activeConnectionsCount.incrementAndGet()
                println("Client connected: ${clientConnection.inetAddress}:${clientConnection.port}")
                println("Active clients: $activeConnectionsCount")
                handleClient(clientConnection)
            }
            println("Exiting loop")
        }
    }

    private fun CoroutineScope.handleClient(client: Socket) = launch {
        BufferedReader(InputStreamReader(client.inputStream)).use { inputStream ->
            PrintWriter(OutputStreamWriter(client.outputStream)).use { outputStream ->
                try {
                    val path = readRequestFile(client, inputStream)
                    println("Request received for file $path from ${client.inetAddress.hostAddress}:${client.port}")
                    if (path.isRegularFile()) {
                        println("Sending response message to ${client.inetAddress.hostAddress}:${client.port}")
                        sendResponse(outputStream, path.readText())
                    } else {
                        println("File $path requested from ${client.inetAddress.hostAddress}:${client.port} not found")
                        sendErrorResponse(outputStream, "Not Found", 404)
                    }
                } catch (e: IOException) {
                    println("Exception occurred while reading request from ${client.inetAddress.hostAddress}:${client.port}: ${e.message}")
                    sendErrorResponse(outputStream, "Internal Server Error", 500)
                }
            }
        }

        delay(100)
        println("Closing connection with client ${client.inetAddress}:${client.port}")
        client.close()
        activeConnectionsCount.decrementAndGet()
    }

    private fun CoroutineScope.readRequestFile(client: Socket, inputStream: BufferedReader): Path {
        if (!isActive || client.isClosed) throw IOException("Connection closed")
        val clientMethodLine = inputStream.readLine() ?: throw IOException("No messages received")

        return clientMethodLine.parseMethodLine()
    }

    private fun sendErrorResponse(outputStream: PrintWriter, message: String, errorCode: Int) {
        outputStream.println(
            """
HTTP/1.1 $errorCode $message
            """.trimIndent()
        )
    }

    private fun sendResponse(outputStream: PrintWriter, content: String, contentType: String = "text/plain") {
        outputStream.println(
            """
HTTP/1.1 200 OK
Content-Type: $contentType

$content
            """.trimIndent()
        )
    }

    override fun close() {
        isClosed = true
        runBlocking {
            job.cancel()
            serverSocket.close()
        }
    }

    fun stop() = close()
}

private fun ServerSocket.acceptCancellable() = try {
    accept()
} catch (e: IOException) {
    null
}

private fun String.parseMethodLine(): Path {
    val trimmed = trimIndent()
    if (trimmed.isEmpty()) {
        throw IOException("Invalid empty request")
    }

    val split = trimmed.split("""\s+""".toRegex())
    if (!split.first().equals("GET", ignoreCase = true)) {
        throw IOException("Unknown method: ${split.first()}")
    }

    if (!split.last().startsWith("HTTP/", ignoreCase = true)) {
        throw IOException("Unknown scheme: ${split.last()}")
    }

    if (split.last().substringAfter("HTTP/") != "1.1") {
        throw IOException("Unknown HTTP version: ${split.last().substringAfter("HTTP/")}")
    }

    return KotlinPath(split.subList(1, split.lastIndex).joinToString(" "))
}

