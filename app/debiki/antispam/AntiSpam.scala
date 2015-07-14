/**
 * Copyright (C) 2015 Kaj Magnus Lindberg
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

package debiki.antispam

import com.debiki.core._
import com.debiki.core.Prelude._
import java.{net => jn}
import play.{api => p}
import play.api.Play.current
import requests.DebikiRequest
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Success, Failure}



sealed abstract class SpamCheckResult { def isSpam: Boolean }
object SpamCheckResult {
  case object IsSpam extends SpamCheckResult { def isSpam = true }
  case object NotSpam extends SpamCheckResult { def isSpam = false }
}


object ApiKeyInvalidException extends QuickException
object CouldNotVerifyApiKeyException extends QuickException
object BadSpamCheckResponseException extends QuickException
object NoApiKeyException extends QuickException



/** Currently uses Akismet. Could break out Akismet requests to a separate class,
  * later on if needed, and perhaps support other spam check services too.
  *
  * Thread safe. (Well, that's the intention anyway) TODO verify that dispatch.Http is thread safe
  */
class AntiSpam {

  // also: http://blogspam.net/faq/
  // https://www.mollom.com/pricing
  // https://cleantalk.org/price

  // ... http://ipinfo.io/developers

  private val TimeoutMs = 5000
  private val UserAgent = "Debiki/0.00.00 | Built-In/0.00.00"
  private val ContentType = "application/x-www-form-urlencoded"
  private val Charset = "utf-8"

  private val isValidPromise: Promise[Boolean] = Promise()
  private var isValidFuture: Future[Boolean] = isValidPromise.future

  // Is thread safe?
  private val dispatchHttp = dispatch.Http configure { builder =>
    builder.setAllowPoolingConnection(true)
      .setConnectionTimeoutInMs(TimeoutMs)
      .setRequestTimeoutInMs(TimeoutMs)
      .setUserAgent(UserAgent)
  }

  private def encode(text: String) = jn.URLEncoder.encode(text, "UTF-8")

  // One key only, for now. Later on, one per site? + 1 global for non-commercial
  // low traffic newly created sites?
  private val anyApiKey: Option[String] = p.Play.configuration.getString("debiki.akismetApiKey")


  def start() {
    verifyApiKey()
  }


  private def verifyApiKey() {
    if (anyApiKey.isEmpty) {
      isValidPromise.failure(NoApiKeyException)
      return
    }
    val verifyRequest: dispatch.Req =
      dispatch.url("https://rest.akismet.com/1.1/verify-key").POST.setBody(
        "key=" + encode(anyApiKey.get) + "&blog=" + encode("http://localhost")) // apparently port number not okay, => invalid
        .setContentType(ContentType, Charset)
    dispatchHttp(verifyRequest.OK((response: dispatch.Res) => {   // TODO errors
      val body = response.getResponseBody
      val isValid = body.trim == "valid"
      p.Logger.info(s"Akismet key is valid: $isValid, response: '$body'")
      isValidPromise.success(isValid)
    }))

    /*
    val request: dispatch.Req = dispatch.url("http://api.hostip.info/country.php").GET
    val futureCountry: Future[String] = dispatchHttp(request.OK((response: dispatch.Res) => {
      response.getResponseBody
    }))
    val country = scala.concurrent.Await.ready(futureCountry, atMost = Duration(10, Seconds))
    p.Logger.info("zzgg Country: " + country)
    */

    // POST key=...&blog=urlencode(http//...)
    // https://rest.akismet.com/1.1/verify-key
    // User-Agent: "WordPress/3.8.1 | Akismet/2.5.9"; -- no,
    // Content-Type: application/x-www-form-urlencoded
  }


  /** Returns a future that eventually succeeds with isSpam: Boolean, or fails if we couldn't
    * find out if it's spam or not, e.g. because Akismet is offline or if
    * the API key has expired.
    */
  def checkIfIsSpam(apiKeyzzzz: String, debikiRequest: DebikiRequest[_], pageId: PageId,
        text: String): Future[Boolean] = {
    val payload = buildCheckIsSpamPayload(debikiRequest, pageId, text)
    val promise = Promise[Boolean]()
    isValidFuture onComplete {
      case Success(isValid) =>
        if (isValid) {
          doCheckIfIsSpam(apiKey = apiKey, payload = payload, promise)
        }
        else {
          promise.failure(ApiKeyInvalidException)
        }
      case Failure(whatIsThis) =>
        promise.failure(CouldNotVerifyApiKeyException)
    }
    promise.future
  }


  private def buildCheckIsSpamPayload(debikiRequest: DebikiRequest[_], pageId: PageId,
          text: String): String = {
    val body = new StringBuilder()
    val user = debikiRequest.theUser

    // Documentation: http://akismet.com/development/api/#comment-check

    // (required) The front page or home URL of the instance making the request.
    // For a blog or wiki this would be the front page. Note: Must be
    // a full URI, including http://.
    body.append("blog=" + encode(debikiRequest.origin))

    // (required) IP address of the comment submitter.
    body.append("&user_ip=" + encode(debikiRequest.ip))

    // (required) User agent string of the web browser submitting the comment - typically
    // the HTTP_USER_AGENT cgi variable. Not to be confused with the user agent
    // of your Akismet library.
    val browserUserAgent = debikiRequest.headers.get("User-Agent") getOrElse "Unknown"
    body.append("&user_agent=" + encode(browserUserAgent))

    // The content of the HTTP_REFERER header should be sent here.
    val browserReferrer = debikiRequest.headers.get("referer") getOrElse "Unknown"
    body.append("&referrer=" + encode(browserReferrer)) // should be 2 'r' yes

    // The permanent location of the entry the comment was submitted to.
    body.append("&permalink=" + encode(debikiRequest.origin + "/-" + pageId))

    // May be blank, comment, trackback, pingback, or a made up value like "registration".
    // It's important to send an appropriate value, and this is further explained here.
    body.append("&comment_type=forum-post") // that's what Discourse uses

    // Name submitted with the comment.
    val usernameAndName = user.username.map(_ + " ").getOrElse("") + user.displayName
    body.append("&comment_author=" + encode(usernameAndName))

    // Email address submitted with the comment.
    body.append("&comment_author_email=" + encode(user.email)) // TODO email inclusion configurable

    // The content that was submitted.
    body.append("&comment_content=" + encode(text))

    // URL submitted with comment.
    //comment_author_url (not supported)

    // The UTC timestamp of the creation of the comment, in ISO 8601 format. May be
    // omitted if the comment is sent to the API at the time it is created.
    //comment_date_gmt (omitted)

    // The UTC timestamp of the publication time for the post, page or thread
    // on which the comment was posted.
    //comment_post_modified_gmt  -- COULD include, need to load page

    // Indicates the language(s) in use on the blog or site, in ISO 639-1 format,
    // comma-separated. A site with articles in English and French might use "en, fr_ca".
    //blog_lang

    // The character encoding for the form values included in comment_* parameters,
    // such as "UTF-8" or "ISO-8859-1".
    body.append("&blog_charset=UTF-8")

    // The user role of the user who submitted the comment. This is an optional parameter.
    // If you set it to "administrator", Akismet will always return false.
    //user_role

    // This is an optional parameter. You can use it when submitting test queries to Akismet.
    body.append("&is_test" + (if (p.Play.isProd) false else true))

    body.toString()
  }


  private def doCheckIfIsSpam(apiKey: String, payload: String,
        promise: Promise[Boolean]) {
    val request: dispatch.Req = dispatch.url(
      s"https://$apiKey.rest.akismet.com/1.1/comment-check").POST.setBody(payload)
      .setContentType(ContentType, Charset)

    dispatchHttp(request.OK((response: dispatch.Res) => {   // TODO errors
      val body = response.getResponseBody
      body.trim match {
        case "true" =>
          p.Logger.debug(s"Akismet found spam: $payload")
          promise.success(true)
        case "false" =>
          p.Logger.debug(s"Akismet says not spam: $payload")
          promise.success(false)
        case badResponse =>
          p.Logger.error(s"Akismet error: Weird spam check response: '$badResponse'")
          promise.failure(BadSpamCheckResponseException)
      }
    }))
  }
}
