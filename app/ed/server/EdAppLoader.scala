package ed.server

import com.debiki.core._
import debiki.{EdHttp, Globals}
import ed.server.http.{PlainApiActions, SafeActions}
import ed.server.security.EdSecurity
import play.api._
import play.api.mvc.ControllerComponents
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import scala.concurrent.ExecutionContext


class EdAppLoader extends ApplicationLoader {

  def load(context: ApplicationLoader.Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new EdAppComponents(context).application
  }

}


class EdAppComponents(appLoaderContext: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(appLoaderContext)
  with HttpFiltersComponents {

  CLEAN_UP // dupl code [7UWKDAAQ0]
  val secure: Boolean =
    configuration.getBoolean("ed.secure").orElse(
      configuration.getBoolean("debiki.secure")) getOrElse {
      play.api.Logger.info("Config value 'ed.secure' missing; defaulting to true. [EdM3KEF2]")
      true
    }

  val http = new EdHttp(secure, isProd = appLoaderContext.environment.mode == play.api.Mode.Prod,
    ???, ???)
  val globals = new Globals(appLoaderContext, actorSystem, http)
  val security = new ed.server.security.EdSecurity(http, globals)
  val safeActions = new SafeActions(globals, http)
  val plainApiActions = new PlainApiActions(safeActions, globals, http, security)
  val context = new EdContext(http, globals, security, safeActions, plainApiActions, controllerComponents)

  globals.setEdContext(context)
  globals.startStuff()

  lazy val router: Router = Router.empty

}


class EdContext(
  val http: EdHttp,
  val globals: Globals,
  val security: EdSecurity,
  val safeActions: SafeActions,
  val plainApiActions: PlainApiActions,
  private val controllerComponents: ControllerComponents) {

  def executionContext: ExecutionContext = controllerComponents.executionContext

}
