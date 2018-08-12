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


class DraftsController @Inject()(cc: ControllerComponents, edContext: EdContext)
  extends EdController(cc, edContext) {

  import context.security.{throwNoUnless, throwIndistinguishableNotFound}

  def saveDraft: Action[JsValue] = PostJsonAction(RateLimits.DraftSomething, maxBytes = MaxPostSize) {
        request: JsonPostRequest =>
    import request.{body, dao, theRequester => requester}

    // Check authz  !

    val draftLocator: DraftLocator = ???
    val draft: Draft = ???

    dao.readWriteTransaction { tx =>
      tx.upsertDraft(draft)
    }

    Ok

    /*
    val anyPageId = (body \ "pageId").asOpt[PageId]
    val anyAltPageId = (body \ "altPageId").asOpt[AltPageId]
    val anyEmbeddingUrl = (body \ "embeddingUrl").asOpt[String]
    val replyToPostNrs = (body \ "postNrs").as[Set[PostNr]]
    val text = (body \ "text").as[String].trim
    val postType = PostType.fromInt((body \ "postType").as[Int]) getOrElse throwBadReq(
      "DwE6KG4", "Bad post type")

    throwBadRequestIf(text.isEmpty, "EdE85FK03", "Empty post")
    throwForbiddenIf(requester.isGroup, "EdE4GKRSR1", "Groups may not reply")
    throwBadRequestIf(anyEmbeddingUrl.exists(_ contains '#'), "EdE0GK3P4",
        s"Don't include any URL #hash in the embedding page URL: ${anyEmbeddingUrl.get}")

    DISCUSSION_QUALITY; COULD // require that the user has spent a reasonable time reading
    // the topic, in comparison to # posts in the topic, before allowing hen to post a reply.

    var newPagePath: PagePath = null
    val pageId = anyPageId.orElse({
      (anyAltPageId orElse anyEmbeddingUrl).flatMap(request.dao.getRealPageId)
    }) getOrElse {
      // No page id. Maybe create a new embedded discussion?
      val embeddingUrl = anyEmbeddingUrl getOrElse {
        throwNotFound("EdE404NOEMBURL", "Page not found and no embedding url specified")
      }
      newPagePath = tryCreateEmbeddedCommentsPage(request, embeddingUrl, anyAltPageId)
      newPagePath.thePageId
    }

    val pageMeta = dao.getPageMeta(pageId) getOrElse throwIndistinguishableNotFound("EdE5FKW20")
    val replyToPosts = dao.loadPostsAllOrError(pageId, replyToPostNrs) getOrIfBad { missingPostNr =>
      throwNotFound(s"Post nr $missingPostNr not found", "EdEW3HPY08")
    }
    val categoriesRootLast = dao.loadAncestorCategoriesRootLast(pageMeta.categoryId)

    throwNoUnless(Authz.mayPostReply(
      request.theUserAndLevels, dao.getGroupIds(request.theUser),
      postType, pageMeta, replyToPosts, dao.getAnyPrivateGroupTalkMembers(pageMeta),
      inCategoriesRootLast = categoriesRootLast,
      permissions = dao.getPermsOnPages(categoriesRootLast)),
      "EdEZBXK3M2")

    REFACTOR; COULD // intstead: [5FLK02]
    // val authzContext = dao.getPageAuthzContext(requester, pageMeta)
    // throwNoUnless(Authz.mayPostReply(authzContext, postType, "EdEZBXK3M2")

    // For now, don't follow links in replies. COULD rel=follow if all authors + editors = trusted.
    val textAndHtml = dao.textAndHtmlMaker.forBodyOrComment(text, followLinks = false)
    val result = dao.insertReply(textAndHtml, pageId = pageId, replyToPostNrs,
      postType, request.who, request.spamRelatedStuff)

    var patchWithNewPageId: JsObject = result.storePatchJson
    if (newPagePath ne null) {
      patchWithNewPageId = patchWithNewPageId + ("newlyCreatedPageId" -> JsString(pageId))
    }
    OkSafeJson(patchWithNewPageId) */
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
