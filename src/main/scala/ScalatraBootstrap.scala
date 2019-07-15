import javax.servlet.ServletContext
import org.codeoverflow.chatoverflow.ui.web.rest.config.ConfigController
import org.codeoverflow.chatoverflow.ui.web.rest.connector.ConnectorController
import org.codeoverflow.chatoverflow.ui.web.rest.plugin.PluginInstanceController
import org.codeoverflow.chatoverflow.ui.web.rest.types.TypeController
import org.codeoverflow.chatoverflow.ui.web.{CodeOverflowSwagger, OpenAPIServlet}
import org.scalatra._

/**
  * This class provides all runtime information for Scalatra. Servlets are mounted here.
  */
class ScalatraBootstrap extends LifeCycle {
  val apiVersion = "0.2"
  implicit val swagger: CodeOverflowSwagger = new CodeOverflowSwagger(apiVersion)

  override def init(context: ServletContext) {

    // Allow CORS
    context.initParameters("org.scalatra.cors.allowedOrigins") = "*"
    context.initParameters("org.scalatra.cors.allowCredentials") = "false"
    context.initParameters("org.scalatra.cors.allowedMethods") = "*"

    // Add all servlets and controller
    context.mount(new TypeController(), "/types/*", "types")
    context.mount(new ConfigController(), "/config/*", "config")
    context.mount(new PluginInstanceController(), "/instances/*", "instances")
    context.mount(new ConnectorController(), "/connectors/*", "connectors")
    context.mount(new OpenAPIServlet(), "/api-docs")
  }
}