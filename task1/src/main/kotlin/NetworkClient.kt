import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

class NetworkClient internal constructor(private val socketConnection: Socket) : AutoCloseable {
    constructor(
        serverAddress: String = "127.0.0.1",
        serverPort: Int = 80,
        timeout: Duration = 1000.milliseconds
    ) : this(Socket(serverAddress, serverPort)) {
        socketConnection.soTimeout = timeout.toInt(DurationUnit.MILLISECONDS)
    }

    override fun close() {
        socketConnection.close()
    }

     fun retrieveFileContent(remoteFile: Path) {
        if (socketConnection.isClosed) return
        val inputStream = BufferedReader(InputStreamReader(socketConnection.inputStream))
        val outputStream = PrintWriter(OutputStreamWriter(socketConnection.outputStream), true)

        outputStream.println(buildRequest(socketConnection.inetAddress.hostAddress, remoteFile))

        val (statusCode, message) = inputStream.readLine().getResponseCode()
        if (statusCode != 200) {
            printErrorMessage("Failed to retrieve file from the server. Error code: $statusCode. Message: $message")
        } else {
            printDebugMessage("Content successfully retrieved from the server")

            inputStream.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) println(line)
                    else return@forEach
                }
            }
        }

        if (!socketConnection.isClosed) socketConnection.close()
        printDebugMessage("Connection with the server has been closed")
    }
}

private fun String.getResponseCode(): Pair<Int, String> {
    val splitted = split("\\s+".toRegex())
    return Pair(splitted[1].toInt(), splitted.subList(2, splitted.size).joinToString(" "))
}

private fun buildRequest(host: String, filepath: Path) = """
GET $filepath HTTP/1.1
Host: $host
Connection: Keep-Alive
""".trimIndent()

private fun printErrorMessage(message: String) {
    println("[ERROR] $message")
}

private fun printDebugMessage(message: String) {
    println("[DEBUG] $message")
}