import java.net.InetAddress
import kotlin.time.Duration
import kotlin.time.measureTimedValue


fun main (args: Array<String>) {
    running(arrayOf("reddit.com"))
}

fun running(args: Array<String>) {
    val targetServerAddress = args.getOrNull(0) ?: throw IllegalArgumentException("Expected server address or name as the first argument")
    val roundTripTimes = mutableListOf<Duration>()
    repeat(4) {
        val (success, duration) = measureTimedValue {
            InetAddress.getByName(targetServerAddress).isReachable(1000)
        }
        if (success) {
            roundTripTimes += duration
            println("Response from ${ InetAddress.getByName(targetServerAddress).hostAddress}, time = ${duration.inWholeMilliseconds}ms")
        } else {
            println("Request timed out")
        }
    }
    val failures = 4 - roundTripTimes.size
    println("\nPing statistics for ${ InetAddress.getByName(targetServerAddress).hostAddress}:")
    println("\tPackets: Sent = $4, Received = ${4 - failures}, Lost = $failures (${(failures.toDouble() / 4) * 100}% loss)")
    println("Approximate round trip times in milliseconds:")
    val minMilliseconds = if (roundTripTimes.isEmpty()) 0 else roundTripTimes.min().inWholeMilliseconds
    val maxMilliseconds = if (roundTripTimes.isEmpty()) 0 else roundTripTimes.max().inWholeMilliseconds
    val averageMilliseconds = if (roundTripTimes.isEmpty()) 0 else roundTripTimes.sumOf { it.inWholeMilliseconds } / roundTripTimes.size
    println("\tMinimum = ${minMilliseconds}ms, Maximum = ${maxMilliseconds}ms, Average = ${averageMilliseconds}ms")
}