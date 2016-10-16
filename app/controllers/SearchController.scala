/**
 * Copyright (c) 2013, 2016 Kaj Magnus Lindberg
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
import debiki.{RateLimits, SiteTpi}
import ed.server.search._
import io.efdi.server.http._
import play.api._
import play.api.mvc.Result
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import Prelude._


/** Full text search, for a whole site, or for a site section, e.g. a single
  * forum (including all sub forums and topics), a single blog, or wiki.
  */
object SearchController extends mvc.Controller {

  private val SearchPhraseFieldName = "searchPhrase"


  /** 'q' not 'query', so urls becomes a tiny bit shorter, because people will sometimes
    * copy & paste search phrase urls in emails etc? Google uses 'q' not 'query' anyway.
    */
  def showSearchPage(q: Option[String]) = AsyncGetAction { request =>
    val htmlStr = views.html.templates.search(SiteTpi(request)).body
    Future.successful(Ok(htmlStr) as HTML)
  }


  def doSearch() = AsyncPostJsonAction(RateLimits.FullTextSearch, maxLength = 1000) {
        request: JsonPostRequest =>
    val rawQuery = (request.body \ "rawQuery").as[String]
    // searchQuery = parseRawSearchQuery(...)
    request.dao.fullTextSearch(rawQuery, None, request.user) map {
      searchResults: Seq[PageAndHits] =>
        import play.api.libs.json._
        CLEAN_UP; COULD // move to ... ReactJson? & rename it to Jsonifier?
        OkSafeJson(Json.obj(
          "pagesAndHits" -> searchResults.map((pageAndHits: PageAndHits) => {
            Json.obj(
              "pageId" -> pageAndHits.pageId,
              "pageTitle" -> pageAndHits.pageTitle,
              "hits" -> JsArray(pageAndHits.hitsByScoreDesc.map((hit: SearchHit) => Json.obj(
                "postId" -> hit.postId,
                "postNr" -> hit.postNr,
                "approvedRevisionNr" -> hit.approvedRevisionNr,
                "approvedTextWithHighligtsHtml" -> Json.arr(hit.approvedTextWithHighligtsHtml),
                "currentRevisionNr" -> hit.currentRevisionNr
              ))))
          })
        ))
    }
  }


  def searchWholeSiteFor(phrase: String) = AsyncGetAction { apiReq =>
    searchImpl(phrase, anyRootPageId = None, apiReq)
  }


  def searchWholeSite() = AsyncJsonOrFormDataPostAction(RateLimits.FullTextSearch,
        maxBytes = 200) { apiReq: ApiRequest[JsonOrFormDataBody] =>
    val searchPhrase = apiReq.body.getOrThrowBadReq(SearchPhraseFieldName)
    searchImpl(searchPhrase, anyRootPageId = None, apiReq)
  }


  def searchSiteSectionFor(phrase: String, pageId: String) = AsyncGetAction { apiReq =>
    debiki.RateLimiter.rateLimit(RateLimits.FullTextSearch, apiReq)
    searchImpl(phrase, anyRootPageId = Some(pageId), apiReq)
  }


  def searchSiteSection(pageId: String) = AsyncJsonOrFormDataPostAction(
        RateLimits.FullTextSearch, maxBytes = 200) { apiReq =>
    val searchPhrase = apiReq.body.getOrThrowBadReq(SearchPhraseFieldName)
    searchImpl(searchPhrase, anyRootPageId = Some(pageId), apiReq)
  }


  private def searchImpl(phrase: String, anyRootPageId: Option[String],
        apiReq:  DebikiRequest[_]): Future[Result] = {
    apiReq.dao.fullTextSearch(phrase, anyRootPageId, apiReq.user) map {
        searchResults: Seq[PageAndHits] =>
      val siteTpi = debiki.SiteTpi(apiReq)
      val htmlStr = views.html.templates.searchResults(
          siteTpi, anyRootPageId, phrase, searchResults).body
      Ok(htmlStr) as HTML
    }
  }


}

