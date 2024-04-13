import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

open class Client : Closeable {

    protected lateinit var socket: Socket
    open val port: Int = 25

    var isConnected = false
        protected set

    open fun connectToServer(server: String, timeout: Duration = 10.seconds) {
        isConnected = true
        socket = Socket(server, port)
        socket.soTimeout = timeout.inWholeMilliseconds.toInt()
        socket.expectResponse(220)
    }

    open fun <T> sendMessage(buildMessage: MailMessage.() -> T) {
        require(isConnected) { "You must establish a connection by calling connectToServer() first" }
        with(socket) {
            val message = MailMessage().apply { buildMessage() }
            expectResponse(250, "HELO alice")
            expectResponse(250, "MAIL FROM: <${message.from}>")
            expectResponse(250, "RCPT TO: <${message.to}>")
            expectResponse(354, "DATA")
            expectResponse(250, message.data)
            expectResponse(221, "QUIT")
        }
    }

    override fun close() {
        if (isConnected) {
            println("Closing the mail client socket")
            socket.close()
        }
    }
}

internal fun Socket.expectResponse(
    message: String? = null,
    expected: BufferedReader.() -> Boolean,
    errorMessage: () -> String
) {
    val reader = BufferedReader(InputStreamReader(inputStream))
    val writer = PrintWriter(OutputStreamWriter(outputStream), true)
    message?.let { msg -> writer.println(msg) }
    if (!expected(reader)) throw IllegalStateException("Unexpected server response! ${errorMessage()}")
}

internal fun Socket.expectResponse(expectedCode: Int, message: String? = null) {
    var errorMessage = "No response message received"
    expectResponse(message, expected = lam@{
        val line = readLine() ?: return@lam false
        println("Response: $line")
        errorMessage = "Expected $expectedCode response code, but received $line"
        line.startsWith((expectedCode.toString()))
    },
        errorMessage = { errorMessage }
    )
}

class MailMessage {
    var from = "1@gmail.com"
    var to = "2@gmail.com"
    var data = ""
}