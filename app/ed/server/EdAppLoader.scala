package ed.server

import com.debiki.core._
import debiki.{EdHttp, Globals, RateLimiter, ReactJson}
import ed.server.http.{PlainApiActions, SafeActions}
import ed.server.security.EdSecurity
import play.api._
import play.api.http.FileMimeTypes
import play.api.mvc.ControllerComponents
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import scala.concurrent.ExecutionContext


class EdAppLoader extends ApplicationLoader {

  def load(context: ApplicationLoader.Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    val isProd = context.environment.mode == play.api.Mode.Prod
    Globals.setIsProdForever(isProd)

    new EdAppComponents(context).application
  }

}


class EdAppComponents(appLoaderContext: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(appLoaderContext)
  with HttpFiltersComponents {

  val globals = new Globals(appLoaderContext, executionContext, actorSystem)
  val security = new ed.server.security.EdSecurity(globals)
  val rateLimiter = new RateLimiter(globals, security)
  val safeActions = new SafeActions(globals, security)
  val plainApiActions = new PlainApiActions(safeActions, globals, security, rateLimiter)
  val context = new EdContext(
    globals, security, safeActions, plainApiActions, materializer, controllerComponents)

  globals.setEdContext(context)
  globals.startStuff()

  lazy val router: Router = Router.empty

}


class EdContext(
  val globals: Globals,
  val security: EdSecurity,
  val safeActions: SafeActions,
  val plainApiActions: PlainApiActions,
  val akkaStreamMaterializer: akka.stream.Materializer,
  private val controllerComponents: ControllerComponents) {

  implicit def executionContext: ExecutionContext = controllerComponents.executionContext
  def mimeTypes: FileMimeTypes = controllerComponents.fileMimeTypes

}
