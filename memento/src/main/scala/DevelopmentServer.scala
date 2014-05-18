import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

object DevelopmentServer {
  def main(args: Array[String]) {
    val server = new Server(8081)
    val context = new WebAppContext("src/main/webapp", "/")

    context.setServer(server)
    server.setHandler(context)

    try {
      server.start()
      server.join()
    } catch {
      case e: Exception =>
        e.printStackTrace()
      System.exit(1)
    }
  }
}
