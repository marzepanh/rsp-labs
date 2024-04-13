import kotlinx.coroutines.*
import java.util.*
import kotlin.io.path.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


fun main(args: Array<String>) {
    running(arrayOf("both", "8080", "10000"))
}

fun running(args: Array<String>) {
    if (args.isEmpty()) {
        println("Please provide either server, client, or both as the first argument")
        return
    }

    val modelType = args[0].uppercase(Locale.getDefault())

    runBlocking {
        when (modelType) {
            "SERVER" -> {
                val port = args.getOrElse(1) { "80" }.toInt()
                if (port !in 0..65535) {
                    println("Port $port is out of range. Please provide a value between 0 and 65535")
                    return@runBlocking
                }
                val terminationDelay = args.getOrNull(2)?.toLong()?.milliseconds

                launch {
                    runServer(port, terminationDelay)
                }
            }

            "CLIENT" -> {
                if (args.size < 4) {
                    println("Please provide server address, port, and requested file path")
                    return@runBlocking
                }

                val serverAddress = args[1]
                val serverPort = args[2].toInt()
                if (serverPort !in 0..65535) {
                    println("Port $serverPort is out of range. Please provide a value between 0 and 65535")
                    return@runBlocking
                }

                val requestedFilePath = args[3]

                launch {
                    val client = NetworkClient(serverAddress, serverPort)
                    client.retrieveFileContent(Path(requestedFilePath))
                }
            }

            "BOTH" -> {
                val serverPort = args.getOrElse(1) { "80" }.toInt()
                if (serverPort !in 0..65535) {
                    println("Port $serverPort is out of range. Please provide a value between 0 and 65535.")
                    return@runBlocking
                }

                val modelTime = args.getOrNull(2)?.toLong()?.milliseconds

                launch {
                    modelBoth(serverPort, modelTime)
                }
            }

            else -> println("Invalid connection type: $modelType")
        }
    }
}

fun CoroutineScope.runServer(port: Int, terminationDelay: Duration? = null): NetworkServer {
    val server = NetworkServer(port = port)
    launch {
        withContext(Dispatchers.IO) {
            server.start()
        }
    }
    launch {
        terminationDelay?.let { disableAfter ->
            delay(disableAfter)
            server.stop()
        }
    }
    return server
}

fun CoroutineScope.modelBoth(port: Int = 80, modelTime: Duration? = 10.seconds) {
    var isServerRunning = { false }
    launch {
        val server = runServer(port, terminationDelay = modelTime)
        isServerRunning = { !server.isClosed }
    }

    launch {
        while (!isServerRunning()) {
            delay(50)
        }

        while (isServerRunning()) {
            launch {
                try {
                    val client = NetworkClient("127.0.0.1", port)
                    delay((500L..2000L).random())
                    client.retrieveFileContent(randomPaths.random())
                } catch (_: Exception) {
                }
            }
            delay((100L..2000L).random())
        }
    }
}

private val randomPaths = arrayOf(
    Path("/bin/helpmeee"),
    Path("./src/main/resources/test.txt"),
    Path("./src/main/resources/test.html"),
)