/**
 * Copyright (C) 2012 Kaj Magnus Lindberg (born 1979)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package debiki

import com.debiki.core._
import com.debiki.core.Prelude._
import ed.server.auth.MayMaybe
import ed.server.auth.MayMaybe.{NoMayNot, NoNotFound, Yes}
import ed.server.http.DebikiRequest
import java.{net => jn}
import play.api.libs.json.JsLookupResult
import play.{api => p}
import play.api.mvc._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Try



/**
 * HTTP utilities.
 */
class EdHttp(
  val secure: Boolean,
  isProd: Boolean,
  e2eTestPassword: Option[String],
  forbiddenPassword: Option[String]) {


  // ----- Limits

  // (The www.debiki.se homepage is 20 kb, and homepage.css 80 kb,
  // but it includes Twitter Bootstrap.)

  val MaxPostSize = 100 * 1000
  val MaxPostSizeForAuUsers = 30 * 1000
  val MaxPostSizeForUnauUsers = 10 * 1000
  val MaxDetailsSize =  20 * 1000


  // ----- Content type matchers

  // (Cannot use Play's, they depend on the codec.)

  abstract sealed class ContentType
  object ContentType {
    case object Json extends ContentType
    case object Html extends ContentType
  }

  // ----- Error handling

  private def R = Results

  def BadReqResult(errCode: String, message: String): Result =
    R.BadRequest("400 Bad Request\n"+ message +" [error "+ errCode +"]")

  // There's currently no WWW-Authenticate header
  // field in the response though!
  def UnauthorizedResult(errCode: String, message: String): Result =
    R.Unauthorized("401 Unauthorized\n"+ message +" [error "+ errCode +"]")

  def ForbiddenResult(errCode: String, message: String): Result =
    R.Forbidden("403 Forbidden\n"+ message +" [error "+ errCode +"]").withHeaders(
      "X-Error-Code" -> errCode)
    /* Doesn't work, the Som(reason) is ignored: (could fix later in Play 2.5 when Iterates = gone)
    Result(
      ResponseHeader(404, Map.empty, Some(s"Forbidden!!zz $errCode")),
      Enumerator(wString.transform(s"403 Forbidden bdy\n $message [$errCode]"))) */

  def NotImplementedResult(errorCode: String, message: String): Result =
    R.NotImplemented(s"501 Not Implemented\n$message [$errorCode]")

  def NotFoundResult(errCode: String, message: String): Result =
    R.NotFound("404 Not Found\n"+ message +" [error "+ errCode +"]")

  def ServiceUnavailableResult(errorCode: String, message: String): Result =
    R.ServiceUnavailable(s"503 Service Unavailable\n$message [$errorCode] [EsE5GK0Y2]")

  def MethodNotAllowedResult: Result =
    R.MethodNotAllowed("405 Method Not Allowed\nTry POST or GET instead please [DwE7KEF2]")

  def EntityTooLargeResult(errCode: String, message: String): Result =
    R.EntityTooLarge("413 Request Entity Too Large\n"+
       message +" [error "+ errCode +"]")

  def UnprocessableEntityResult(errCode: String, message: String): Result =
    R.UnprocessableEntity("422 Unprocessable Entity\n"+ message +" [error "+ errCode +"]")

  def InternalErrorResult(errCode: String, message: String): Result =
    R.InternalServerError(
      "500 Internal Server Error\n"+ message +" [error "+ errCode +"]")

  def InternalErrorResult2(message: String): Result =
    R.InternalServerError("500 Internal Server Error\n"+ message)

  /**
   * Thrown on error, caught in Global.onError, which returns the wrapped
   * result to the browser.
   */
  case class ResultException(result: Result) extends QuickException {
    override def toString = s"Status ${result.header.status}: $bodyToString"
    override def getMessage: String = toString

    def statusCode: Int = result.header.status

    def bodyToString: String = {
      play.api.Play.maybeApplication match {
        case None => "(Play app gone [EdM2WKG07])"
        case Some(app) =>
          implicit val materializer = play.api.Play.materializer(app)  // what is that [6KFW02G]
          val futureByteString = result.body.consumeData(materializer)
          val byteString = Await.result(futureByteString, Duration.fromNanos(1000*1000*1000))
          byteString.utf8String
      }
    }

    // ScalaTest prints the stack trace but not the exception message. However this is
    // a QuickException — it has no stack trace. Let's create a helpful fake stack trace
    // that shows the exception message, so one knows what happened.
    if (false) { // if (isTest) {
      val message = s"ResultException, status $statusCode [EsMRESEX]:\n$bodyToString"
      setStackTrace(Array(new StackTraceElement(message, "", "", 0)))
    }
  }

  def throwTemporaryRedirect(url: String) =
    throw ResultException(R.Redirect(url))

  /** Sets a Cache-Control max-age = 1 week, so that permanent redirects can be undone. [7KEW2Z]
    * Otherwise browsers might cache them forever.
    */
  def throwPermanentRedirect(url: String) =
    throw ResultException(R.Redirect(url).withHeaders(
      p.http.HeaderNames.CACHE_CONTROL -> ("public, max-age=" + 3600 * 24 * 7)))
    // Test that the above cache control headers work, before I redirect permanently,
    // otherwise browsers might cache the redirect *forever*, can never be undone.
    // So, right now, don't:
    //   p.http.Status.MOVED_PERMANENTLY

  def throwBadRequest(errCode: String, message: String = "") = throwBadReq(errCode, message)

  def throwBadRequestIf(condition: Boolean, errCode: String, message: String = "") =
    if (condition) throwBadRequest(errCode, message)

  def throwBadReq(errCode: String, message: String = "") =
    throw ResultException(BadReqResult(errCode, message))

  def throwUnprocessableEntity(errCode: String, message: String = "") =
    throw ResultException(UnprocessableEntityResult(errCode, message))

  def throwBadArgument(errCode: String, parameterName: String, problem: String = "") =
    throwBadReq(errCode, "Bad `"+ parameterName +"` value" + (
      if (problem.nonEmpty) ": " + problem else ""))

  def throwBadConfigFile(errCode: String, message: String) =
    throwNotFound(errCode, message)

  def throwParamMissing(errCode: String, paramName: String) =
    throwBadReq(errCode, "Parameter missing: "+ paramName)

  // There's currently no WWW-Authenticate header
  // field in the response though!
  def throwUnauthorized(errCode: String, message: String = "") =
    throw ResultException(UnauthorizedResult(errCode, message))

  def throwForbidden(errCode: String, message: String = "") =
    throw ResultException(ForbiddenResult(errCode, message))

  def throwForbiddenIf(test: Boolean, errorCode: String, message: => String): Unit =
    if (test) throwForbidden(errorCode, message)

  def throwForbiddenUnless(test: Boolean, errorCode: String, message: => String): Unit =
    if (!test) throwForbidden(errorCode, message)

  def throwNotImplemented(errorCode: String, message: String = "") =
    throw ResultException(NotImplementedResult(errorCode, message))

  def throwServiceUnavailable(errorCode: String, message: String = "") =
    throw ResultException(ServiceUnavailableResult(errorCode, message))

  def throwNotFound(errCode: String, message: String = "") =
    throw ResultException(NotFoundResult(errCode, message))

  /** Use this if page not found, or the page is private and we don't want strangers
    * to find out that it exists. [7C2KF24]
    */
  def throwIndistinguishableNotFound(devModeErrCode: String = ""): Nothing = {
    val suffix =
      if (!isProd && devModeErrCode.nonEmpty) s"-$devModeErrCode"
      else ""
    throwNotFound("EsE404" + suffix, "Page not found")
  }

  def throwEntityTooLargeIf(condition: Boolean, errCode: String, message: String) =
    if (condition) throwEntityTooLarge(errCode, message)

  def throwEntityTooLarge(errCode: String, message: String) =
    throw ResultException(EntityTooLargeResult(errCode, message))

  def throwTooManyRequests(message: String) =
    throw ResultException(R.TooManyRequest(message))

  /** Happens e.g. if the user attempts to upvote his/her own comment or
    * vote twice on another comment.
    */
  def throwConflict(errCode: String, message: String) =
    throw ResultException(R.Conflict(s"409 Conflict\n$message [error $errCode]"))

  def logAndThrowInternalError(errCode: String, message: String = "")
        (implicit logger: play.api.Logger) = {
    logger.error("Internal error: "+ message +" ["+ errCode +"]")
    throwInternalError(errCode, message)
  }

  def logAndThrowForbidden(errCode: String, message: String = "")
        (implicit logger: play.api.Logger) = {
    logger.warn("Forbidden: "+ message +" ["+ errCode +"]")
    throwForbidden(errCode, message)
  }

  def logAndThrowBadReq(errCode: String, message: String = "")
        (implicit logger: play.api.Logger) = {
    logger.warn("Bad request: "+ message +" ["+ errCode +"]")
    throwBadReq(errCode, message)
  }

  def throwInternalError(errCode: String, message: String = "") =
    throw ResultException(InternalErrorResult(errCode, message))



  def throwForbidden2: (String, String) => Nothing =
    throwForbidden

  def throwNoUnless(mayMaybe: MayMaybe, errorCode: String) {
    import MayMaybe._
    mayMaybe match {
      case Yes => // fine
      case NoNotFound(debugCode) => throwIndistinguishableNotFound(debugCode)
      case NoMayNot(code2, reason) => throwForbidden(s"$errorCode-$code2", reason)
    }
  }

  def throwNotImplementedIf(test: Boolean, errorCode: String, message: => String = "") {
    if (test) throwNotImplemented(errorCode, message)
  }

  def throwLoginAsSuperAdmin(request: Request[_]): Nothing =
    if (isAjax(request)) throwForbidden2("EsE54YK2", "Not super admin")
    else throwLoginAsSuperAdminTo(request.uri)

  def throwLoginAsSuperAdminTo(path: String): Nothing =
    ??? // throwLoginAsTo(LoginController.AsSuperadmin, path)


  def throwLoginAsAdmin(request: Request[_]): Nothing =
    if (isAjax(request)) throwForbidden2("EsE6GP21", "Not admin")
    else throwLoginAsAdminTo(request.uri)

  def throwLoginAsAdminTo(path: String): Nothing =
    ??? // throwLoginAsTo(LoginController.AsAdmin, path)


  def throwLoginAsStaff(request: Request[_]): Nothing =
    if (isAjax(request)) throwForbidden2("EsE4GP6D", "Not staff")
    else throwLoginAsStaffTo(request.uri)

  def throwLoginAsStaffTo(path: String): Nothing =
    ??? // throwLoginAsTo(LoginController.AsStaff, path)


  private def throwLoginAsTo(as: String, to: String): Nothing =
    ??? // throwTemporaryRedirect(routes.LoginController.showLoginPage(as = Some(as), to = Some(to)).url)



  // ----- Cookies

  def SecureCookie(name: String, value: String, maxAgeSeconds: Option[Int] = None,
        httpOnly: Boolean = false) =
    Cookie(name, value, maxAge = maxAgeSeconds, secure = secure, httpOnly = httpOnly)

  def DiscardingSecureCookie(name: String) =
    DiscardingCookie(name, secure = secure)

  def DiscardingSessionCookie = DiscardingSecureCookie("dwCoSid")

  // Two comments on the encoding of the cookie value:
  // 1. If the cookie contains various special characters
  // (whitespace, any of: "[]{]()=,"/\?@:;") it will be
  // sent as a Version 1 cookie (by javax.servlet.http.Cookie),
  // then it is surrounded with quotes.
  // the jQuery cookie plugin however expects an urlencoded value:
  // 2. urlEncode(value) results in these cookies being sent:
  //    Set-Cookie: dwCoUserEmail="kajmagnus79%40gmail.com";Path=/
  //    Set-Cookie: dwCoUserName="Kaj%20Magnus";Path=/
  // No encoding results in these cookies:
  //    Set-Cookie: dwCoUserEmail=kajmagnus79@gmail.com;Path=/
  //    Set-Cookie: dwCoUserName="Kaj Magnus";Path=/
  // So it seems a % encoded string is surrounded with double quotes, by
  // javax.servlet.http.Cookie? Why? Not needed!, '%' is safe.
  // So I've modified jquery-cookie.js to remove double quotes when
  // reading cookie values.
  def urlEncodeCookie(name: String, value: String, maxAgeSecs: Option[Int] = None) =
    Cookie(
      name = name,
      value = urlEncode(convertEvil(value)),  // see comment above
      maxAge = maxAgeSecs,
      path = "/",
      domain = None,
      secure = secure,
      httpOnly = false)

  def urlDecodeCookie(name: String, request: Request[_]): Option[String] =
    request.cookies.get(name).map(cookie => urlDecode(cookie.value))

  def urlEncode(in: String) = {
    // java.net.URLEncoder unfortunately converts ' ' to '+', so change '+' to
    // a percent encoded ' ', because the browsers seem to decode '+' to '+'
    // not ' '. And they should do so, i.e. decode '+' to '+', here is
    // more info on URL encoding and the silly space-to-plus conversion:
    //   <http://www.lunatech-research.com/archives/2009/02/03/
    //   what-every-web-developer-must-know-about-url-encoding>
    // see #HandlingURLscorrectlyinJava and also search for "plus".
    // Could also use Google API Client Library for Java, which has
    // a class  com.google.api.client.escape.PercentEscaper
    // <http://javadoc.google-api-java-client.googlecode.com/hg/1.0.10-alpha/
    //  com/google/api/client/escape/PercentEscaper.html>
    // that correctly encodes ' ' to '%20' not '+'.
    jn.URLEncoder.encode(in, "UTF-8").replaceAll("\\+", "%20")
  }

  def urlDecode(in : String) = jn.URLDecoder.decode(in, "UTF-8")

  /**
   * Converts dangerous characters (w.r.t. xss attacks) to "~".
   * Perhaps converts a few safe characters as well.
   * COULD simply URL encode instead?!
   */
  def convertEvil(value: String) =  // XSS test that implementation is ok
    value.replaceAll("""<>\(\)\[\]\{\}"!#\$\%\^\&\*\+=,;:/\?""", "~")
  //value.filter(c => """<>()[]{}"!#$%^&*+=,;:/?""".count(_ == c) == 0)
  // but these are okay:  `’'-@._
  // so email addresses are okay.



  // ----- Request "getters" and payload parsing helpers


  implicit class RichString2(value: String) {
    def toIntOrThrow(errorCode: String, errorMessage: String): Int =
      value.toIntOption getOrElse throwBadRequest(errorCode, errorMessage)

    def toFloatOrThrow(errorCode: String, errorMessage: String): Float =
      value.toFloatOption getOrElse throwBadRequest(errorCode, errorMessage)

    def toLongOrThrow(errorCode: String, errorMessage: String): Long =
      Try(value.toLong).toOption getOrElse throwBadRequest(errorCode, errorMessage)
  }


  implicit class RichJsLookupResult(val underlying: JsLookupResult) {
    def asOptStringTrimmed: Option[String] = underlying.asOpt[String].map(_.trim)

    def asOptStringNoneIfBlank: Option[String] = underlying.asOpt[String].map(_.trim) match {
      case Some("") => None
      case x => x
    }
  }


  implicit class GetOrThrowBadArgument[A](val underlying: Option[A]) {
    def getOrThrowBadArgument(errorCode: String, parameterName: String, message: => String = ""): A = {
      underlying getOrElse {
        throwBadArgument(errorCode, parameterName, message)
      }
    }
  }


  def parseIntOrThrowBadReq(text: String, errorCode: String = "DwE50BK7"): Int = {
    try {
      text.toInt
    }
    catch {
      case ex: NumberFormatException =>
        throwBadReq(s"Not an integer: ``$text''", errorCode)
    }
  }



  def isAjax(request: Request[_]) =
    request.headers.get("X-Requested-With") == Some("XMLHttpRequest")


  /** The real ip address of the client, unless a fakeIp url param or dwCoFakeIp cookie specified
    * In prod mode, an e2e test password cookie is required.
    *
    * (If 'fakeIp' is specified, actions.SafeActions.scala copies the value to
    * the dwCoFakeIp cookie.)
    */
  def realOrFakeIpOf(request: play.api.mvc.Request[_]): String = {
    val fakeIpQueryParam = request.queryString.get("fakeIp").flatMap(_.headOption)
    val fakeIp = fakeIpQueryParam.orElse(
      request.cookies.get("dwCoFakeIp").map(_.value))  getOrElse {
      return request.remoteAddress
    }

    if (isProd) {
      def where = fakeIpQueryParam.isDefined ? "in query param" | "in cookie"
      val password = getE2eTestPassword(request) getOrElse {
        throwForbidden(
          "DwE6KJf2", s"Fake ip specified $where, but no e2e test password — required in prod mode")
      }
      val correctPassword = e2eTestPassword getOrElse {
        throwForbidden(
          "DwE7KUF2", "Fake ips not allowed, because no e2e test password has been configured")
      }
      if (password != correctPassword) {
        throwForbidden(
          "DwE2YUF2", "Fake ip forbidden: Wrong e2e test password")
      }
    }

    // Dev or test mode, or correct password, so:
    fakeIp
  }


  def getE2eTestPassword(request: play.api.mvc.Request[_]): Option[String] =
    request.queryString.get("e2eTestPassword").flatMap(_.headOption).orElse(
      request.cookies.get("dwCoE2eTestPassword").map(_.value)).orElse( // dwXxx obsolete. esXxx now
      request.cookies.get("esCoE2eTestPassword").map(_.value))


  def hasOkE2eTestPassword(request: play.api.mvc.Request[_]): Boolean = {
    getE2eTestPassword(request) match {
      case None => false
      case Some(password) =>
        val correctPassword = e2eTestPassword getOrElse throwForbidden(
          "EsE5GUM2", "There's an e2e test password in the request, but not in any config file")
        if (password != correctPassword) {
          throwForbidden("EsE2FWK4", "The e2e test password in the request is wrong")
        }
        true
    }
  }


  def getForbiddenPassword(request: DebikiRequest[_]): Option[String] =
    request.queryString.get("forbiddenPassword").flatMap(_.headOption).orElse(
      request.cookies.get("esCoForbiddenPassword").map(_.value))


  def hasOkForbiddenPassword(request: DebikiRequest[_]): Boolean = {
    getForbiddenPassword(request) match {
      case None => false
      case Some(password) =>
        val correctPassword = forbiddenPassword getOrElse throwForbidden(
          "EsE48YC2", "There's a forbidden-password in the request, but not in any config file")
        if (password != correctPassword) {
          throwForbidden("EsE7UKF2", "The forbidden-password in the request is wrong")
        }
        true
    }
  }



}

