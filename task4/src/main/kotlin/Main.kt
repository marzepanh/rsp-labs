import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket

fun main(args: Array<String>) {
    running(arrayOf("google.com", "80", "37851", "true"))
}

fun running(args: Array<String>) {
    val remoteServerPort = args.getOrNull(1)?.toInt()
        ?: throw IllegalArgumentException("Please provide the remote server port as the second argument")
    if (remoteServerPort !in 0..65535) throw IllegalStateException("Invalid port number: $remoteServerPort. Expected a value between 0 and 65535.")
    val proxyServerPort = args.getOrNull(2)?.toInt()
        ?: throw IllegalArgumentException("Please provide the proxy server port as the third argument")
    if (proxyServerPort !in 0..65535) throw IllegalStateException("Invalid port number: $proxyServerPort. Expected a value between 0 and 65535.")
    runBlocking {
        launch {
            withContext(Dispatchers.IO) {
                Server(args.getOrNull(0) ?: throw IllegalArgumentException("Please provide the remote server address as the first argument"), remoteServerPort, proxyServerPort).start()
            }
        }
        launch {
            if (args.getOrNull(3)?.toBoolean() != true) return@launch
            delay(100)
            println("Modelling GET request of /index.html page")
            val socket = Socket("127.0.0.1", proxyServerPort)
            val writer = PrintWriter(OutputStreamWriter(socket.outputStream), true)
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            writer.println("GET /index.html")
            println("Got answer from the server")
            reader.readLines().joinToString("\n").also { println(it) }
        }
    }
}
