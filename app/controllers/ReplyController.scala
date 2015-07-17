/**
 * Copyright (C) 2012-2013 Kaj Magnus Lindberg (born 1979)
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

import actions.ApiActions.{PostJsonAction, StaffPostJsonAction}
import com.debiki.core._
import com.debiki.core.Prelude._
import controllers.Utils.OkSafeJson
import debiki._
import debiki.DebikiHttp._
import play.api._
import play.api.mvc.{Action => _, _}
import play.api.libs.json.Json
import requests._


/** Saves replies and [titles or summaries of trees]. Lazily creates pages for
  * embedded discussions — such pages aren't created until the very first comment
  * is posted.
  */
object ReplyController extends mvc.Controller {


  def handleReply = PostJsonAction(RateLimits.PostReply, maxLength = MaxPostSize) {
        request: JsonPostRequest =>
    val body = request.body
    val pageId = (body \ "pageId").as[PageId]
    val anyPageUrl = (body \ "pageUrl").asOpt[String]
    val replyToPostIds = (body \ "postIds").as[Set[PostId]]
    val text = (body \ "text").as[String].trim
    val wherePerhapsEmpty = (body \ "where").asOpt[String]
    val whereOpt = if (wherePerhapsEmpty == Some("")) None else wherePerhapsEmpty

    // Construct a request that concerns the specified page. Create the page
    // lazily if it's supposed to be a discussion embedded on a static HTML page.
    val pageReq = PageRequest.forPageThatExists(request, pageId = pageId) match {
      case Some(req) => req
      case None =>
        unimplemented("Creating embedded comments page [DwE5UYK4]") /*
        val page = tryCreateEmbeddedCommentsPage(request, pageId, anyPageUrl)
          .getOrElse(throwNotFound("Dw2XEG60", s"Page `$pageId' does not exist"))
        PageRequest.forPageThatExists(request, pageId = page.id) getOrDie "DwE77PJE0"
        */
    }

    if (text.isEmpty)
      throwBadReq("DwE85FK03", "Empty post")

    val postId = pageReq.dao.insertReply(text, pageId = pageId, replyToPostIds,
      authorId = pageReq.theUser.id, pageReq.theBrowserIdData)

    val json = ReactJson.postToJson2(postId = postId, pageId = pageId, pageReq.dao,
      includeUnapproved = true)
    OkSafeJson(json)
  }


  Read this, about how to continue:
    /*
    So this PR adds summaries above threads. One can summarize a thread e.g. like so:
      "Off-topic about ... blah blah." Then people see in 1 second that the thread is off-
      topic, and about ... and they can save time if they're not interested.
     Of course this won't work if the moderators are unable to write sensible summaries.
     It relies on mods being reasonably smart and responsible.
     One would only summarize threads very infrequently: if they are long and (?) off-topic
     and about a clearly identifiable subject. This actually often happens at Hacker News:
     the article is about X and then the top post and its 100 replies instead drifts off
     discussing Y. — This is fine I think, and I've thought that a summary and the possibility
     to collapse that off-topic thread would be nice.

    ? Add a dw2_posts column summarized_by_id, which tells which post summarizes
    the thread starting with the post at that row  (if any).

    And append the summary's author's name to the summary: "— KajMagnus" and use italic
    for the whole summary and allow no formatting or line breaks except for what
    StackExchange allows in the comments below the questions and answers.
    And at most ... 200 chars? summaries? Wrap in parenthesis?
    margin-top: -3px.

    Don't implement titles in this way? Instead, add certain wiki posts, for which no header
    line is shown. They are beautiful and can be used for nice looking mind maps :-)
    Everythng looks so clean and simple with no author and date line. However author and date
    is essential in discussions. Can be removed on wiki pages / wiki posts only?
    And wiki posts can be used as section titles + intros (with a short body of text after them).

    When summarizing a post or a wiki post, find the first paragraph of text and show
    that one only with no formatting? Then, a wiki post with a title, summarized, results
    in only the title (as plain text) being shown + "Click to read more", which looks nice.
     */


  /** Inserts a post just above another post. The new post will look like a title or
    * summary and one cannot reply to it. In this way, you can summarize long off-topic
    * threads or add titles to different parts of a mind map you might be building.
    */
  def addSummary = StaffPostJsonAction(maxLength = MaxPostSize) { request =>
    val pageId = (request.body \ "pageId").as[PageId]
    val postId = (request.body \ "postId").as[PostId]
    val text = (request.body \ "text").as[String].trim

    val summaryPostId = request.dao.insertSummary(pageId, postId, text,
      authorId = request.theUser.id, request.theBrowserIdData)

    // COULD add & use ReactJson.postsToJson(many-post-ids, ...)

    val summaryPost = ReactJson.postToJson2(postId = summaryPostId, pageId = pageId,
      request.dao, includeUnapproved = true)
    val postWithNewParent = ReactJson.postToJson2(postId = postId, pageId = pageId,
      request.dao, includeUnapproved = true)

    OkSafeJson(Json.obj(
      "summaryPost" -> summaryPost,
      "postWithNewParent" -> postWithNewParent))
  }


  /*
  private def tryCreateEmbeddedCommentsPage(
        request: DebikiRequest[_], pageId: PageId, anyPageUrl: Option[String]): Option[Page] = {

    if (anyPageUrl.isEmpty)
      throwBadReq("Cannot create embedded page: embedding page URL unknown")

    val site = request.dao.loadSite()
    val shallCreateEmbeddedTopic = EmbeddedTopicsController.isUrlFromEmbeddingUrl(
      anyPageUrl.get, site.embeddingSiteUrl)

    if (!shallCreateEmbeddedTopic)
      return None

    val topicPagePath = PagePath(
      request.siteId,
      folder = "/",
      pageId = Some(pageId),
      showId = true,
      pageSlug = "")

    val pageToCreate = Page.newPage(
      PageRole.EmbeddedComments,
      topicPagePath,
      PageParts(pageId),
      publishDirectly = true,
      author = SystemUser.User,
      url = anyPageUrl)

    val newPage = request.dao.createPage(pageToCreate)
    Some(newPage)
  }
    */

}
