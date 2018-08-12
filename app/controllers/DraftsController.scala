/**
 * Copyright (c) 2018 Kaj Magnus Lindberg
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

import com.debiki.core._
import debiki._
import debiki.EdHttp._
import ed.server.{EdContext, EdController}
import ed.server.auth.Authz
import ed.server.http._
import javax.inject.Inject
import play.api._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc._
import scala.util.{Try, Failure, Success}


class DraftsController @Inject()(cc: ControllerComponents, edContext: EdContext)
  extends EdController(cc, edContext) {

  import context.security.{throwNoUnless, throwIndistinguishableNotFound}

  def upsertDraft: Action[JsValue] = PostJsonAction(RateLimits.DraftSomething, maxBytes = MaxPostSize) {
        request: JsonPostRequest =>
    import request.{body, dao, theRequester => requester}

    // Check authz  !

    val draftLocator = Try(DraftLocator(
      newTopicCategoryId = (body \ "newTopicCategoryId").asOpt[CategoryId],
      messageToUserId = (body \ "messageToUserId").asOpt[UserId],
      editPostId = (body \ "editPostId").asOpt[PostId],
      replyToPageId = (body \ "replyToPageId").asOpt[PageId],
      replyToPostNr = (body \ "replyToPostNr").asOpt[PostNr],
      replyType = (body \ "replyType").asOpt[Int].flatMap(PostType.fromInt))) match {
      case Failure(ex) => throwBadRequest("TyEBDDRFTDT", ex.getMessage)
      case Success(loc) => loc
    }

    val draft = Draft(
      byUserId = requester.id,
      draftNr = (body \ "draftNr").asOpt[DraftNr].getOrElse(NoDraftNr),
      forWhat = draftLocator,
      createdAt = (body \ "createdAtMs").asWhen,
      lastEditedAt = (body \ "lastEditedAt").asOptWhen,
      autoPostAt = (body \ "autoPostAt").asOptWhen,
      deletedAt = (body \ "deletedAt").asOptWhen,
      newTopicType = (body \ "").asOpt[Int].flatMap(PageRole.fromInt),
      title = (body \ "").asOpt[String],
      text = (body \ "").as[String])

    /*
    throwBadRequestIf(text.isEmpty, "EdE85FK03", "Empty post")
    throwForbiddenIf(requester.isGroup, "EdE4GKRSR1", "Groups may not reply")
    throwBadRequestIf(anyEmbeddingUrl.exists(_ contains '#'), "EdE0GK3P4",
        s"Don't include any URL #hash in the embedding page URL: ${anyEmbeddingUrl.get}")

    throwNoUnless(Authz.mayPostReply(
      request.theUserAndLevels, dao.getGroupIds(request.theUser),
      postType, pageMeta, replyToPosts, dao.getAnyPrivateGroupTalkMembers(pageMeta),
      inCategoriesRootLast = categoriesRootLast,
      permissions = dao.getPermsOnPages(categoriesRootLast)),
      "EdEZBXK3M2")
     */

    dao.readWriteTransaction { tx =>
      tx.upsertDraft(draft)
    }

    Ok
  }


  def loadDraft: Action[Unit] = GetAction { request: GetRequest =>
      import request.{body, dao, theRequester => requester}
    ???
    // Atuhz!!

    val draftLocator: DraftLocator = ???
    val anyDraft = dao.readOnlyTransaction { tx =>
      tx.loadDraftByLocator(requester.id, draftLocator)
    }

    OkSafeJson(Json.obj())
  }


  def listDrafts: Action[Unit] = GetAction { request: GetRequest =>
      import request.{body, dao, theRequester => requester}
    // Atuhz!!
    ???
    val drafts = dao.readOnlyTransaction { tx =>
      tx.listDraftsRecentlyEditedFirst(requester.id)
    }

    OkSafeJson(Json.arr())
  }


  def deleteDrafts: Action[JsValue] = PostJsonAction(RateLimits.DraftSomething, maxBytes = 1000) {
    request: JsonPostRequest =>
      import request.{body, dao, theRequester => requester}
      ???
    // Atuhz!!
  }

}
