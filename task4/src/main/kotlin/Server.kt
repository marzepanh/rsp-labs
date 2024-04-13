import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class Server(val targetServer: String, targetPort: Int, port: Int = targetPort) : Closeable {
    init {
        require(targetPort >= 0)
        require(port >= 0)
    }
    private val serverSocket = ServerSocket(port)
    private val socket = Socket(targetServer, targetPort)

    suspend fun start() = coroutineScope {
        if (socket.isClosed) return@coroutineScope
        println("Waiting for a connection at port ${serverSocket.localPort}")
        val clientSocket = serverSocket.accept()
        println("Connection established with a client: ${clientSocket.inetAddress}:${clientSocket.port}")
        launch(Dispatchers.IO) {
            BufferedReader(InputStreamReader(clientSocket.inputStream)).use { clientReader ->
                PrintWriter(OutputStreamWriter(clientSocket.outputStream), true).use { clientWriter ->
                    BufferedReader(InputStreamReader(socket.inputStream)).use { remoteReader ->
                        PrintWriter(OutputStreamWriter(socket.outputStream), true).use { remoteWriter ->
                            val clientRequest = clientReader.readLine() ?: return@launch
                            println("Received request from the client: $clientRequest")
                            val cacheDirectory = File(
                                javaClass.classLoader.getResource("test")?.toURI() ?: return@launch
                            ).parentFile
                            val requestedFile = File(cacheDirectory, clientRequest.split("\\s+".toRegex())[1])
                            if (requestedFile.isFile) {
                                println("Requested file $requestedFile found in the cache")
                                println("Returning its content directly")
                                clientWriter.println(makeFileResponse(requestedFile.readText()))
                            } else {
                                println("Requested file $requestedFile not found in the cache")
                                println("Requesting it from the server $targetServer:${socket.port}")
                                remoteWriter.println(clientRequest)
                                val content = remoteReader.readLines().dropWhile { it.isNotBlank() }.joinToString("\n")
                                requestedFile.writeText(content)
                                println("File $requestedFile saved in the cache")
                                clientWriter.println(makeFileResponse(content))
                            }
                        }
                    }
                }
            }
        }
    }
    private fun makeFileResponse(content: String) = """
HTTP/1.1 200 OK

$content
""".trimIndent()

    override fun close() {
        if (!socket.isClosed) socket.close()
        if (!serverSocket.isClosed) serverSocket.close()
    }
}