import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import kotlin.time.Duration


fun main(args: Array<String>) {
    running(arrayOf("10000", "10"))
}

fun running(args: Array<String>) {
    val port = args.getOrElse(0) { "${(1024..65535).random()}" }.toInt()
    if (port !in 0..65535) throw IllegalStateException("Invalid port number: $port. Port number should be between 0 and 65535")

    val pingIterations = args.getOrElse(1) { "10" }.toInt()
    if (pingIterations <= 0) throw IllegalStateException("Invalid number of ping iterations: $pingIterations. Number of iterations should be positive")

    val server = Server(port)
    val client = Client()

    runBlocking {
        launch {
            launch {
                withContext(Dispatchers.IO) {
                    server.start()
                }
            }

            launch {
                delay(100)
                var failures = 0
                val roundTripTimes = mutableListOf<Duration>()

                repeat(pingIterations) {
                    try {
                        roundTripTimes.add(client.ping("127.0.0.1", port))
                    } catch (_: SocketTimeoutException) {
                        failures++
                    }
                }

                client.close()
                server.close()

                println("\nPing statistics:")
                println("\tPackets: Sent = $pingIterations, Received = ${pingIterations - failures}, Lost = $failures (${(failures.toDouble() / pingIterations) * 100}% loss)")
                println("Approximate round trip times in milliseconds:")
                println("\tMinimum = ${roundTripTimes.minOrNull()?.inWholeMilliseconds ?: "N/A"} ms, Maximum = ${roundTripTimes.maxOrNull()?.inWholeMilliseconds ?: "N/A"} ms, Average = ${roundTripTimes.sumOf { it.inWholeMilliseconds } / roundTripTimes.size} ms")
            }
        }
    }
}