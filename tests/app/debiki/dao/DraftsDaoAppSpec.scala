/**
 * Copyright (c) 2015 Kaj Magnus Lindberg
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

package debiki.dao

import com.debiki.core._
import com.debiki.core.Prelude._
import org.scalatest._


class DraftsDaoAppSpec extends DaoAppSuite(disableScripts = true, disableBackgroundJobs = true) {
  var dao: SiteDao = _
  var userOne: User = _
  var userTwo: User = _

  var pageId: PageId = _
  var userOneDraft: Draft = _

  val ReplyDraftOneText = "ReplyDraftOneText"

  "DraftsDao can" - {

    "prepare" in {
      globals.systemDao.getOrCreateFirstSite()
      dao = globals.siteDao(Site.FirstSiteId)
      createPasswordOwner("5kwu8f40", dao)
      userOne = createPasswordUser("pp22xxnn", dao, trustLevel = TrustLevel.BasicMember)
      userTwo = createPasswordUser("jjyyzz55", dao, trustLevel = TrustLevel.BasicMember)
      pageId = createPage(PageRole.Discussion, dao.textAndHtmlMaker.forTitle("Reply Draft Page"),
        bodyTextAndHtml = dao.textAndHtmlMaker.forBodyOrComment("Text text."),
        authorId = SystemUserId, browserIdData, dao, anyCategoryId = None)
    }

    "save a draft for a reply" in {
      val now = globals.now()
      val locator = DraftLocator(
        replyToPageId = Some(pageId),
        replyToPostNr = Some(PageParts.BodyNr),
        replyType = Some(PostType.Normal))

      val draft = Draft(
        byUserId = userOne.id,
        draftNr = 1,
        forWhat = locator,
        createdAt = now,
        title = None,
        text = ReplyDraftOneText)

      dao.readWriteTransaction { tx =>
        tx.upsertDraft(draft)
      }
    }

    "list zero drafts for user two" in {
    }

    "list one draft for user one" in {
    }

    "load the draft" in {
    }

    "delete draft" in {
    }

    "load it, deleted status" in {
    }

  }

}
