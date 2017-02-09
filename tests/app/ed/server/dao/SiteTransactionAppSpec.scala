/**
 * Copyright (C) 2017 Kaj Magnus Lindberg
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

package ed.server.dao

import com.debiki.core._
import debiki._
import debiki.dao.{DaoAppSuite, SiteDao}


class SiteTransactionAppSpec extends DaoAppSuite {


  "SiteTransaction can handle member stats" - {
    lazy val dao: SiteDao = Globals.siteDao(Site.FirstSiteId)

    lazy val forumId = dao.createForum(title = "Forum to delete", folder = "/",
      Who(SystemUserId, browserIdData)).pagePath.thePageId

    var admin: User = null
    var other: User = null
    var pageId: PageId = null
    var otherPageId: PageId = null
    var thirdPageId: PageId = null

    "prepare: create users" in {
      admin = createPasswordOwner(s"txt_adm", dao)
      other = createPasswordUser(s"txt_otr", dao)
    }

    "prepare: create pages" in {
      pageId = createPage(PageRole.Discussion, TextAndHtml.forTitle("Page Title XY 12 AB"),
        TextAndHtml.forBodyOrComment("Page body."), authorId = admin.id, browserIdData,
        dao, anyCategoryId = None)
      otherPageId = createPage(PageRole.Discussion, TextAndHtml.forTitle("Other Page Title"),
        TextAndHtml.forBodyOrComment("Other page body."), authorId = admin.id, browserIdData,
        dao, anyCategoryId = None)
      thirdPageId = createPage(PageRole.Discussion, TextAndHtml.forTitle("Third Page Title"),
        TextAndHtml.forBodyOrComment("Third page body."), authorId = admin.id, browserIdData,
        dao, anyCategoryId = None)
    }

    "load and save UserStats" in {
      dao.readWriteTransaction { transaction =>
        transaction.upsertUserStats(stats(admin.id, 100))
        transaction.loadUserStats(admin.id).get mustBe stats(admin.id, 100)
        transaction.loadUserStats(other.id) mustBe None

        transaction.upsertUserStats(stats(other.id, 200))
        transaction.loadUserStats(admin.id).get mustBe stats(admin.id, 100)
        transaction.loadUserStats(other.id).get mustBe stats(other.id, 200)

        // Overwrite, shouldn't overwrite the admin user.
        transaction.upsertUserStats(stats(other.id, 220))
        transaction.loadUserStats(admin.id).get mustBe stats(admin.id, 100)
        transaction.loadUserStats(other.id).get mustBe stats(other.id, 220)
      }

      def stats(userId: UserId, number: Int) = UserStats(
        userId = userId,
        lastSeenAt = When.fromMillis(number + 1),
        lastPostedAt = Some(When.fromMillis(number + 2)),
        lastEmailedAt = Some(When.fromMillis(number + 3)),
        emailBounceSum = number + 4,
        firstSeenAt = When.fromMillis(number + 5),
        firstNewTopicAt = Some(When.fromMillis(number + 6)),
        firstDiscourseReplyAt = Some(When.fromMillis(number + 7)),
        firstChatMessageAt = Some(When.fromMillis(number + 8)),
        topicsNewSince = When.fromMillis(number + 9),
        notfsNewSinceId = 20,
        numDaysVisited = 21,
        numMinutesReading = 22,
        numDiscourseRepliesRead = 23,
        numDiscourseRepliesPosted = 24,
        numDiscourseTopicsEntered = 25,
        numDiscourseTopicsRepliedIn = 26,
        numDiscourseTopicsCreated = 27,
        numChatMessagesRead = 30,
        numChatMessagesPosted = 31,
        numChatTopicsEntered = 32,
        numChatTopicsRepliedIn = 33,
        numChatTopicsCreated = 34,
        numLikesGiven = 40,
        numLikesReceived = 41)
    }

    "load and save MemberVisitStats" in {
      dao.readWriteTransaction { transaction =>
        transaction.upsertUserVisitStats(stats(admin.id, 10, 1000))
        transaction.loadUserVisitStats(admin.id) mustBe Seq(stats(admin.id, 10, 1000))
        transaction.loadUserVisitStats(other.id) mustBe Nil

        transaction.upsertUserVisitStats(stats(other.id, 20, 2000))
        transaction.loadUserVisitStats(admin.id) mustBe Seq(stats(admin.id, 10, 1000))
        transaction.loadUserVisitStats(other.id) mustBe Seq(stats(other.id, 20, 2000))

        // Overwrite, shouldn't overwrite the admin user.
        transaction.upsertUserVisitStats(stats(other.id, 20, 2100))
        transaction.loadUserVisitStats(admin.id) mustBe Seq(stats(admin.id, 10, 1000))
        transaction.loadUserVisitStats(other.id) mustBe Seq(stats(other.id, 20, 2100))

        // Add 40, so like: [40, 20]
        transaction.upsertUserVisitStats(stats(other.id, 40, 4000))
        transaction.loadUserVisitStats(admin.id) mustBe Seq(stats(admin.id, 10, 1000))
        transaction.loadUserVisitStats(other.id) mustBe Seq(
          stats(other.id, 40, 4000), stats(other.id, 20, 2100))

        // Add 30, so like: [40, 30, 20]
        transaction.upsertUserVisitStats(stats(other.id, 30, 3000))
        transaction.loadUserVisitStats(admin.id) mustBe Seq(stats(admin.id, 10, 1000))
        transaction.loadUserVisitStats(other.id) mustBe Seq(
          stats(other.id, 40, 4000), stats(other.id, 30, 3000), stats(other.id, 20, 2100))

        // Overwrite again, shouldn't overwrite 20 and 40.
        transaction.upsertUserVisitStats(stats(other.id, 30, 3333))
        transaction.loadUserVisitStats(admin.id) mustBe Seq(stats(admin.id, 10, 1000))
        transaction.loadUserVisitStats(other.id) mustBe Seq(
          stats(other.id, 40, 4000), stats(other.id, 30, 3333), stats(other.id, 20, 2100))
      }

      def stats(userId: UserId, days: Int, number: Int) = UserVisitStats(
        userId = userId,
        visitDate = WhenDay.fromDays(days),
        numMinutesReading = number + 1,
        numDiscourseRepliesRead = number + 3,
        numDiscourseTopicsEntered = number + 5,
        numChatMessagesRead = number + 8,
        numChatTopicsEntered = number + 10)
    }

    "load and save empty ReadingProgress" in {
      dao.readWriteTransaction { transaction =>
        val progress = ReadingProgress(
          firstVisitedAt = When.fromMillis(100),
          lastVisitedAt = When.fromMillis(101),
          lastReadAt = None,
          lastPostNrsReadRecentFirst = Vector.empty,
          lowPostNrsRead = Set.empty,
          secondsReading = 0)
        transaction.upsertReadProgress(admin.id, pageId, progress)

        transaction.loadReadProgress(admin.id, "wrong_page_id") mustBe None
        transaction.loadReadProgress(admin.id, pageId) mustBe progress
      }
    }

    "load and save ReadingProgress with low post nrs only" in {
      dao.readWriteTransaction { transaction =>
        val progress = ReadingProgress(
          firstVisitedAt = When.fromMillis(200),
          lastVisitedAt = When.fromMillis(201),
          lastReadAt = Some(When.fromMillis(202)),
          lastPostNrsReadRecentFirst = Vector.empty, // not yet implemented
          lowPostNrsRead = Set(1, 2, 3, 8),
          secondsReading = 203)
        transaction.upsertReadProgress(admin.id, otherPageId, progress)
        transaction.loadReadProgress(admin.id, otherPageId) mustBe progress
      }
    }

    var progressBefore: ReadingProgress = null

    "load and save ReadingProgress with high post nrs" in {
      dao.readWriteTransaction { transaction =>
        val progress = ReadingProgress(
          firstVisitedAt = When.fromMillis(300),
          lastVisitedAt = When.fromMillis(301),
          lastReadAt = Some(When.fromMillis(302)),
          lastPostNrsReadRecentFirst = Vector.empty, // not yet implemented
          lowPostNrsRead = Set(1, 10, 100, 200, 300, 400, 500, 512),
          secondsReading = 303)
        transaction.upsertReadProgress(admin.id, thirdPageId, progress)
        transaction.loadReadProgress(admin.id, thirdPageId) mustBe progress
        progressBefore = progress
      }
    }

    "overwrite ReadingProgress" in {
      dao.readWriteTransaction { transaction =>
        val progress = ReadingProgress(
          firstVisitedAt = When.fromMillis(400),
          lastVisitedAt = When.fromMillis(401),
          lastReadAt = Some(When.fromMillis(402)),
          lastPostNrsReadRecentFirst = Vector.empty, // not yet implemented
          lowPostNrsRead = Set(1, 2, 3, 4, 5, 6, 7, 8),
          secondsReading = 403)
        transaction.loadReadProgress(admin.id, thirdPageId) mustBe progressBefore
        transaction.upsertReadProgress(admin.id, thirdPageId, progress)
        transaction.loadReadProgress(admin.id, thirdPageId) mustBe progress
      }
    }
  }

}
