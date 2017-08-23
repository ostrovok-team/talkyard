package ed.server

import debiki.{DebikiHttp, Globals}
import play.api._
import play.api.ApplicationLoader.Context
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.routing.Router
import play.filters.HttpFiltersComponents


class AppLoader extends ApplicationLoader {

  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    new AppComponents(context).application
  }

}


class AppComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents {

  val globals = new Globals(context, actorSystem)

  lazy val router: Router = Router.empty

}


class EdController(cc: ControllerComponents, val globals: Globals)
  extends AbstractController(cc)
  with DebikiHttp {


}
