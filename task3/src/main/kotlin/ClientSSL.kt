import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.time.Duration

class ClientSSL : Client() {

    private lateinit var socketSSL: SSLSocket
    override val port: Int = 587

    override fun connectToServer(server: String, timeout: Duration) {
        super.connectToServer(server, timeout)
        socket.expectResponse(250, "EHLO alice")
        socket.expectResponse(220, "STARTTLS")
        System.setProperty("javax.net.ssl.trustStore", "C:\\env\\JDKs\\openjdk-20.0.1\\lib\\security\\cacerts")
        val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        socketSSL = sslFactory.createSocket(socket, socket.inetAddress.hostAddress, socket.port, true) as SSLSocket
        socketSSL.useClientMode = true
        socketSSL.enableSessionCreation = true
        println("Start SSL handshake with the server")
        socketSSL.startHandshake()
        println("Secured connection with the server is established")
    }

    override fun <T> sendMessage(buildMessage: MailMessage.() -> T) {
        require(isConnected) { "It's required to call openConnection() before" }
        with(socketSSL) {
            val message = MailMessage().apply { buildMessage() }
            expectResponse(334, "AUTH LOGIN")
            println("Please, specify the username for the SMTP server:")
            expectResponse(334, readlnOrNull() ?: "anonymous")
            println("Please, specify the password for the SMTP server:")
            expectResponse(235, readlnOrNull() ?: "anonymous")
            expectResponse(250, "MAIL FROM: <${message.from}>")
            expectResponse(250, "RCPT TO: <${message.to}>")
            expectResponse(354, "DATA")
            expectResponse(250, message.data)
            expectResponse(221, "QUIT")
        }
    }

    override fun close() {
        socketSSL.close()
        super.close()
    }
}
