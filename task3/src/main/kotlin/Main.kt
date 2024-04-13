fun main(args: Array<String>) {
    //running(arrayOf("smtp.gmail.com", "1@gmail.com", "2@gmail.com", "What's up"))
    running(arrayOf("ssl", "smtp.gmail.com", "1@gmail.com", "2@gmail.com", "What's up"))

}

fun running(args: Array<String>) {
    val isSslEnabled = args.getOrNull(0)?.equals("ssl", ignoreCase = true) == true
    val adjustedArgs = if (isSslEnabled) args.copyOfRange(1, args.lastIndex) else args

    val client = if (isSslEnabled) ClientSSL() else Client()
    client.use { mailClient ->
        mailClient.connectToServer(adjustedArgs.getOrNull(0) ?: throw IllegalArgumentException("Please provide SMTP server as the first argument"))
        mailClient.sendMessage {
            this.from = adjustedArgs.getOrNull(1) ?: throw IllegalArgumentException("Please provide email FROM as the second argument")
            this.to = adjustedArgs.getOrNull(2) ?: throw IllegalArgumentException("Please provide email TO as the third argument")
            this.data = adjustedArgs.drop(3).joinToString(" ")
        }
    }
}