/**
 * Copyright (c) 2017 Kaj Magnus Lindberg
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

package controllers

import org.apache.commons.codec.{binary => acb}
import com.debiki.core._
import com.debiki.core.Prelude._
import debiki._
import debiki.EdHttp._
import ed.server.spam.SpamChecker
import debiki.dao.SiteDao
import ed.server.{EdContext, EdController}
import ed.server.http._
import javax.inject.Inject
import org.scalactic.{Bad, Good}
import play.api._
import play.api.mvc._
import play.api.libs.json.{JsBoolean, JsValue, Json}
import LoginWithPasswordController._
import org.owasp.encoder.Encode



/** Logs in users via username and password.
  */
class SingleSignOnController @Inject()(cc: ControllerComponents, edContext: EdContext)
  extends EdController(cc, edContext) {

  import context.globals
  import context.security.createSessionIdAndXsrfToken


  def redirectToRemoteServer = GetAction { request =>
    val url = "" // request.siteSettings.singleSignOnUrl
    // acb.Base64.encodeBase64URLSafeString(mdSha1.digest(text.getBytes("UTF-8")))
    Redirect(url)
  }


  def loginMaybeCreateUser(userData: String, signature: String) =
        GetActionRateLimited(RateLimits.Login) { request =>
    ??? /*

    val base64QueryStringUrlEncoded: String = userData
    val base64QueryString: String = debiki.EdHttp.urlDecode(base64QueryStringUrlEncoded)
    val queryStringBytes: Array[Byte] = acb.Base64.decodeBase64(base64QueryString)
    val queryString = new String(queryStringBytes, "UTF-8")

    val nameValuePairs = queryString.split("&")

    val body = ???
    val nonce = ???
    val externalId: String = ???
    val emailAddress: String = ???
    val isEmailAddressVerified: Boolean = ???
    val username: String = ???
    val fullName: Option[String] = ???
    val aboutUser: Option[String] = ???
    val anyReturnToUrl: Option[String] = ???
    val isAdmin: Option[Boolean] = ???
    val isModerator: Option[Boolean] = ???
    val addToGroupIds: Seq[GroupId] = Nil
    val addToGroupNames: Seq[String] = Nil
    val removeFromGroupIds: Seq[GroupId] = Nil
    val removeFromGroupNames: Seq[String] = Nil

    // For now. Security review before allowing non-verified addresses.
    throwForbiddenIf(!isEmailAddressVerified, "EdESSO2PKDRRF", "Email not verified")

    val dao = daoFor(request.request)
    val siteSettings = dao.getWholeSiteSettings()

    // First, lookup by external id, if provided.
    val anyUser = dao.loadUserByExternalId(externalId) orElse {
      if (!isEmailAddressVerified) None else {
        dao.loadUserByEmail(emailAddress)
      }
    }

    anyUser match {
      case Some(user) =>
        //if (.... stuff ... isDefined) {
        // save isAdmin, isModerator, groups, avatar url, about-user-text
        //}
      case None =>
        // Create new user & identity.
    }

    // OLD below

    // Some dupl code. [2FKD05]
    if (!siteSettings.requireVerifiedEmail && emailAddress.isEmpty) {
      // Fine. If needn't verify email, then people can specify non-existing addresses,
      // so then we might as well accept no-email-at-all.
    }
    else if (emailAddress.isEmpty) {
      throwUnprocessableEntity("EdESSO1GUR0", "Email address missing")
    }
    else if (!isValidNonLocalEmailAddress(emailAddress))
      throwUnprocessableEntity("EdESSO80KFP2", "Bad email address")

    if (ed.server.security.ReservedNames.isUsernameReserved(username))
      throwForbidden("EdESSO5PKW01", s"Username is reserved: '$username'; choose another username")

    globals.spamChecker.detectRegistrationSpam(request, name = username, email = emailAddress) map {
        isSpamReason =>
      SpamChecker.throwForbiddenIfSpam(isSpamReason, "EdE7KVF2_")

      // Password strength tested in createPasswordUserCheckPasswordStrong() below.

      val userData =
        NewPasswordUserData.create(name = fullName, email = emailAddress, username = username,
            password = password, createdAt = globals.now(),
            isAdmin = isAdmin.getOrElse(false), isOwner = false) match {
          case Good(data) => data
          case Bad(errorMessage) =>
            throwUnprocessableEntity("DwE805T4", s"$errorMessage, please try again.")
        }

      val loginCookies: List[Cookie] = try {
        val newMember = dao.createPasswordUserCheckPasswordStrong(userData)
        if (newMember.email.nonEmpty) {
          sendEmailAddressVerificationEmail(newMember, anyReturnToUrl, request.host, request.dao)
        }
        if (newMember.email.nonEmpty && !siteSettings.mayPostBeforeEmailVerified) {
          TESTS_MISSING // no e2e tests for this
          // Apparently the staff wants to know that all email addresses actually work.
          // (But if no address specifeid — then, just below, we'll log the user in directly.)
          Nil
        }
        else {
          dieIf(newMember.email.isEmpty && siteSettings.requireVerifiedEmail, "EdE2GKF06")
          dao.pubSub.userIsActive(request.siteId, newMember, request.theBrowserIdData)
          val (_, _, sidAndXsrfCookies) = createSessionIdAndXsrfToken(dao.siteId, newMember.id)
          sidAndXsrfCookies
        }
      }
      catch {
        case DbDao.DuplicateUsername =>
          throwForbidden(
            "DwE65EF0", "Username already taken, please try again with another username")
        case DbDao.DuplicateUserEmail =>
          // Send account reminder email. But don't otherwise indicate that the account exists,
          // so no email addresses are leaked.
          sendYouAlreadyHaveAnAccountWithThatAddressEmail(
            dao, emailAddress, siteHostname = request.host, siteId = request.siteId)
          Nil
      }

      OkSafeJson(Json.obj(
        "userCreatedAndLoggedIn" -> JsBoolean(loginCookies.nonEmpty),
        "emailVerifiedAndLoggedIn" -> JsBoolean(false)))
          .withCookies(loginCookies: _*)
    } */
  }

}

