import javax.servlet.ServletContext
import org.dbpedia.extraction.memento.MementoServlet
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new MementoServlet, "/*")
  }
}
