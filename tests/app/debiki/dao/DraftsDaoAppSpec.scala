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

package debiki.dao

import com.debiki.core._
import com.debiki.core.Prelude._
import org.scalatest._


class DraftsDaoAppSpec extends DaoAppSuite(disableScripts = true, disableBackgroundJobs = true) {
  var dao: SiteDao = _
  var owner: User = _
  var userOne: User = _
  var userTwo: User = _

  var categoryId: CategoryId = _

  var pageId: PageId = _
  var userOneDraft: Draft = _
  var userOneDraftTwoNewerForNewTopic: Draft = _
  var userOneDraftTwoEdited: Draft = _
  var userOneDraftThreeOlderDirectMessage: Draft = _
  var userOneDraftThreeDeleted: Draft = _

  val DraftOneText = "DraftOneText"
  val DraftTwoTitleOrig = "DraftTwoTitleOrig"
  val DraftTwoTitleEdited = "DraftTwoTitleEdited"
  val DraftTwoTextOrig = "DraftTwoTextOrig"
  val DraftTwoTextEdited = "DraftTwoTextEdited"
  val DraftThreeText = "DraftThreeText"

  "DraftsDao can" - {

    "prepare" in {
      globals.systemDao.getOrCreateFirstSite()
      dao = globals.siteDao(Site.FirstSiteId)
      owner = createPasswordOwner("5kwu8f40", dao)
      userOne = createPasswordUser("pp22xxnn", dao, trustLevel = TrustLevel.BasicMember)
      userTwo = createPasswordUser("jjyyzz55", dao, trustLevel = TrustLevel.BasicMember)
      pageId = createPage(PageRole.Discussion, dao.textAndHtmlMaker.forTitle("Reply Draft Page"),
        bodyTextAndHtml = dao.textAndHtmlMaker.forBodyOrComment("Text text."),
        authorId = SystemUserId, browserIdData, dao, anyCategoryId = None)

      categoryId =
          dao.createForum("Forum", s"/drafts-forum/", isForEmbCmts = false,
            Who(owner.id, browserIdData)).defaultCategoryId
    }

    "save a draft for a reply" in {
      val now = globals.now()
      val locator = DraftLocator(
        replyToPageId = Some(pageId),
        replyToPostNr = Some(PageParts.BodyNr),
        replyType = Some(PostType.Normal))

      userOneDraft = Draft(
        byUserId = userOne.id,
        draftNr = 1,
        forWhat = locator,
        createdAt = now,
        title = None,
        text = DraftOneText)

      dao.readWriteTransaction { tx =>
        tx.upsertDraft(userOneDraft)
      }
    }

    "list zero drafts for user two" in {
      dao.readOnlyTransaction { tx =>
        tx.listDraftsRecentlyEditedFirst(userTwo.id) mustBe Nil
      }
    }

    "list one draft for user one" in {
      dao.readOnlyTransaction { tx =>
        tx.listDraftsRecentlyEditedFirst(userOne.id) mustBe Vector(userOneDraft)
      }
    }

    "save another draft, for a new topic" in {
      val now = globals.now()
      val locator = DraftLocator(
        newTopicCategoryId = Some(categoryId))

      userOneDraftTwoNewerForNewTopic = Draft(
        byUserId = userOne.id,
        draftNr = 2,
        forWhat = locator,
        createdAt = now.plusMillis(1000),  // newer
        newTopicType = Some(PageRole.Discussion),
        title = Some("New topic title"),
        text = DraftTwoTextOrig)

      dao.readWriteTransaction { tx =>
        tx.upsertDraft(userOneDraftTwoNewerForNewTopic)
      }
    }

    "save yet another draft, for a direct message" in {
      val now = globals.now()
      val locator = DraftLocator(
        messageToUserId = Some(userTwo.id))

      userOneDraftThreeOlderDirectMessage = Draft(
        byUserId = userOne.id,
        draftNr = 3,
        forWhat = locator,
        createdAt = now.minusMillis(1000),  // older
        newTopicType = Some(PageRole.Discussion),
        title = Some("Direct message title"),
        text = DraftThreeText)

      dao.readWriteTransaction { tx =>
        tx.upsertDraft(userOneDraftThreeOlderDirectMessage)
      }
    }

    "list three drafts for user one, in correct order" in {
      dao.readOnlyTransaction { tx =>
        tx.listDraftsRecentlyEditedFirst(userOne.id) mustBe Vector(
          userOneDraftTwoNewerForNewTopic, userOneDraft, userOneDraftThreeOlderDirectMessage)
      }
    }

    "won't find non-existing drafts" in {
      dao.readOnlyTransaction { tx =>
        tx.loadDraftByNr(userOne.id, 123456) mustBe None
      }
    }

    "can load drafts by nr" in {
      dao.readOnlyTransaction { tx =>
        val d1 = userOneDraft
        val d2 = userOneDraftTwoNewerForNewTopic
        val d3 = userOneDraftThreeOlderDirectMessage
        tx.loadDraftByNr(userOne.id, d1.draftNr) mustBe Some(d1)
        tx.loadDraftByNr(userOne.id, d2.draftNr) mustBe Some(d2)
        tx.loadDraftByNr(userOne.id, d3.draftNr) mustBe Some(d3)
      }
    }

    "can load drafts by locator" in {
      dao.readOnlyTransaction { tx =>
        val d1 = userOneDraft
        val d2 = userOneDraftTwoNewerForNewTopic
        val d3 = userOneDraftThreeOlderDirectMessage
        tx.loadDraftByLocator(userOne.id, d1.forWhat) mustBe Some(d1)
        tx.loadDraftByLocator(userOne.id, d2.forWhat) mustBe Some(d2)
        tx.loadDraftByLocator(userOne.id, d3.forWhat) mustBe Some(d3)
      }
    }

    "soft delete a draft" in {
      userOneDraftThreeDeleted = userOneDraftThreeOlderDirectMessage.copy(deletedAt = Some(globals.now))
      dao.readWriteTransaction { tx =>
        tx.upsertDraft(userOneDraftThreeDeleted)
      }
    }

    "no longer incl in drafts list (but the others still are)" in {
      dao.readOnlyTransaction { tx =>
        tx.listDraftsRecentlyEditedFirst(userOne.id) mustBe Vector(
          userOneDraftTwoNewerForNewTopic, userOneDraft)
      }
    }

    "can still load the others, by id and loctor" in {
      dao.readOnlyTransaction { tx =>
        val d1 = userOneDraft
        val d2 = userOneDraftTwoNewerForNewTopic
        tx.loadDraftByNr(userOne.id, d1.draftNr) mustBe Some(d1)
        tx.loadDraftByNr(userOne.id, d2.draftNr) mustBe Some(d2)
        tx.loadDraftByLocator(userOne.id, d1.forWhat) mustBe Some(d1)
        tx.loadDraftByLocator(userOne.id, d2.forWhat) mustBe Some(d2)
      }
    }

    "load the deleted draft, it's now in deleted status" in {
      dao.readOnlyTransaction { tx =>
        val d3 = userOneDraftThreeOlderDirectMessage
        d3.draftNr mustBe userOneDraftThreeDeleted.draftNr
        tx.loadDraftByNr(userOne.id, d3.draftNr) mustBe Some(userOneDraftThreeDeleted)
        tx.loadDraftByLocator(userOne.id, d3.forWhat) mustBe Some(userOneDraftThreeDeleted)
      }
    }

    "hard delete a draft" in {
      dao.readWriteTransaction { tx =>
        tx.deleteDraft(userOne.id, userOneDraftThreeOlderDirectMessage.draftNr)
      }
    }

    "now cannot load it any more at all, gone" in {
      dao.readOnlyTransaction { tx =>
        val d3 = userOneDraftThreeOlderDirectMessage
        tx.loadDraftByNr(userOne.id, d3.draftNr) mustBe None
        tx.loadDraftByLocator(userOne.id, d3.forWhat) mustBe None
      }
    }

    "can edit a draft" in {
      userOneDraftTwoEdited = userOneDraftTwoNewerForNewTopic.copy(
        title = Some(DraftTwoTitleEdited),
        text = DraftTwoTextEdited)
      dao.readWriteTransaction { tx =>
        tx.upsertDraft(userOneDraftTwoEdited)
      }
    }

    "when reloading, the changes are there" in {
      dao.readOnlyTransaction { tx =>
        val d2 = userOneDraftTwoEdited
        tx.loadDraftByNr(userOne.id, d2.draftNr) mustBe Some(userOneDraftTwoEdited)
        tx.loadDraftByLocator(userOne.id, d2.forWhat) mustBe Some(userOneDraftTwoEdited)
      }
    }

    "the other draft wasn't changed" in {
      dao.readOnlyTransaction { tx =>
        val d1 = userOneDraft
        tx.loadDraftByNr(userOne.id, d1.draftNr) mustBe Some(d1)
        tx.loadDraftByLocator(userOne.id, d1.forWhat) mustBe Some(d1)
      }
    }

  }

}
