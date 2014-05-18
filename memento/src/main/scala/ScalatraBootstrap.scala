import javax.servlet.ServletContext
import org.dbpedia.extraction.memento.TimegateServlet
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new TimegateServlet, "/*")
  }
}
