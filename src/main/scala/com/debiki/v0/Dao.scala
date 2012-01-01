// vim: fdm=marker et ts=2 sw=2 tw=80 fo=tcqwn list

package com.debiki.v0

import com.debiki.v0.Prelude._
import com.google.{common => guava}
import java.{util => ju}
import net.liftweb.common.{Logger, Box, Empty, Full, Failure}
import Dao._
import EmailNotfPrefs.EmailNotfPrefs

/** Debiki's Data Access Object service provider interface.
 */
abstract class DaoSpi {

  def createPage(where: PagePath, debate: Debate): Box[Debate]

  def close()

  def saveLogin(tenantId: String, loginReq: LoginRequest): LoginGrant

  def saveLogout(loginId: String, logoutIp: String)

  def savePageActions[T](tenantId: String, debateId: String, xs: List[T]
                            ): Box[List[T]]

  def loadPage(tenantId: String, debateId: String): Box[Debate]

  def loadTemplates(perhapsTmpls: List[PagePath]): List[Debate]

  def checkPagePath(pathToCheck: PagePath): Box[PagePath]

  def listPagePaths(
        withFolderPrefix: String,
        tenantId: String,
        include: List[PageStatus],
        sortBy: PageSortOrder,
        limit: Int,
        offset: Int
      ): Seq[(PagePath, PageDetails)]

  def loadUser(withLoginId: String, tenantId: String): Option[(Identity, User)]

  def loadPermsOnPage(reqInfo: RequestInfo): PermsOnPage

  def saveInboxSeeds(tenantId: String, seeds: Seq[InboxSeed])

  def loadInboxItems(tenantId: String, roleId: String): List[InboxItem]

  def configRole(tenantId: String, loginId: String, ctime: ju.Date,
                    roleId: String, emailNotfPrefs: EmailNotfPrefs)

  def configIdtySimple(tenantId: String, loginId: String, ctime: ju.Date,
                       emailAddr: String, emailNotfPrefs: EmailNotfPrefs)

  def createTenant(name: String): Tenant

  def loadTenants(tenantIds: Seq[String]): Seq[Tenant]

  def addTenantHost(tenantId: String, host: TenantHost)

  def lookupTenant(scheme: String, host: String): TenantLookup

  def checkRepoVersion(): Box[String]

  /** Used as salt when hashing e.g. email and IP, before the hash
   *  is included in HTML. */
  def secretSalt(): String

}


object Dao {
  case class LoginRequest(login: Login, identity: Identity) {
    require(login.id startsWith "?")
    require(identity.id startsWith "?")
    require(identity.id == login.identityId)
  }

  // COULD use RequesterInfo instead? (It includes role info too, is fine?)
  case class LoginGrant(login: Login, identity: Identity, user: User) {
    require(!login.id.contains('?'))
    require(!identity.id.contains('?'))
    require(!user.id.contains('?'))
    require(login.identityId == identity.id)
    require(identity.userId == user.id)
  }

}


/** Debiki's Data Access Object.
 *
 *  Delegates database requests to a DaoSpi implementation.
 */
abstract class Dao {

  protected def _spi: DaoSpi

  def createPage(where: PagePath, debate: Debate): Box[Debate] =
    _spi.createPage(where, debate)

  def close() = _spi.close

  /** Assigns ids to the login request, saves it, finds or creates a user
   * for the specified Identity, and returns everything with ids filled in.
   */
  def saveLogin(tenantId: String, loginReq: LoginRequest): LoginGrant =
    _spi.saveLogin(tenantId, loginReq)

  /** Updates the specified login with logout IP and timestamp.*/
  def saveLogout(loginId: String, logoutIp: String) =
    _spi.saveLogout(loginId, logoutIp)

  def savePageActions[T](tenantId: String, debateId: String, xs: List[T]
                            ): Box[List[T]] =
    _spi.savePageActions(tenantId, debateId, xs)

  def loadPage(tenantId: String, debateId: String): Box[Debate] =
    _spi.loadPage(tenantId, debateId)

  /** Looks up guids for each possible template.
   *
   *  Each perhaps-template is represented by a PagePath.
   *  The guids found are returned, but PagePaths that point to
   *  non-existing templates are filtered out.
   */
  def loadTemplates(perhapsTmpls: List[PagePath]): List[Debate] =
    _spi.loadTemplates(perhapsTmpls)

  def checkPagePath(pathToCheck: PagePath): Box[PagePath] =
    _spi.checkPagePath(pathToCheck)

  def listPagePaths(
        withFolderPrefix: String,
        tenantId: String,
        include: List[PageStatus],
        sortBy: PageSortOrder,
        limit: Int,
        offset: Int
      ): Seq[(PagePath, PageDetails)] =
    _spi.listPagePaths(withFolderPrefix, tenantId, include,
          sortBy, limit, offset)

  def loadUser(withLoginId: String, tenantId: String
                  ): Option[(Identity, User)] =
    _spi.loadUser(withLoginId, tenantId)

  def loadPermsOnPage(reqInfo: RequestInfo): PermsOnPage =
    _spi.loadPermsOnPage(reqInfo)

  def saveInboxSeeds(tenantId: String, seeds: Seq[InboxSeed]) =
    _spi.saveInboxSeeds(tenantId, seeds)

  def loadInboxItems(tenantId: String, roleId: String): List[InboxItem] =
    _spi.loadInboxItems(tenantId, roleId)

  def configRole(tenantId: String, loginId: String, ctime: ju.Date,
                 roleId: String, emailNotfPrefs: EmailNotfPrefs) =
    _spi.configRole(tenantId, loginId = loginId, ctime = ctime,
                    roleId = roleId, emailNotfPrefs = emailNotfPrefs)

  def configIdtySimple(tenantId: String, loginId: String, ctime: ju.Date,
                       emailAddr: String, emailNotfPrefs: EmailNotfPrefs) =
    _spi.configIdtySimple(tenantId, loginId = loginId, ctime = ctime,
                          emailAddr = emailAddr,
                          emailNotfPrefs = emailNotfPrefs)

  /** Creates a tenant, assigns it an id and and returns it. */
  def createTenant(name: String): Tenant =
    _spi.createTenant(name)

  def loadTenants(tenantIds: Seq[String]): Seq[Tenant] =
    _spi.loadTenants(tenantIds)

  def addTenantHost(tenantId: String, host: TenantHost) =
    _spi.addTenantHost(tenantId, host)

  def lookupTenant(scheme: String, host: String): TenantLookup =
    _spi.lookupTenant(scheme, host)

  def checkRepoVersion(): Box[String] = _spi.checkRepoVersion()

  def secretSalt(): String = _spi.secretSalt()
}


/** Caches pages in a ConcurrentMap.
 *
 *  Thread safe, if `impl' is thread safe.
 */
class CachingDao(spi: DaoSpi) extends Dao {

  protected val _spi = spi
  private case class Key(tenantId: String, debateId: String)

  private val _cache: ju.concurrent.ConcurrentMap[Key, Debate] =
      new guava.collect.MapMaker().
          softValues().
          maximumSize(100*1000).
          //expireAfterWrite(10. TimeUnits.MINUTES).
          makeComputingMap(new guava.base.Function[Key, Debate] {
            def apply(k: Key): Debate = {
              _spi.loadPage(k.tenantId, k.debateId) openOr null
            }
          })

  override def createPage(where: PagePath, debate: Debate): Box[Debate] = {
    for (debateWithIdsNoUsers <- _spi.createPage(where, debate)) yield {
      // ------------
      // Bug workaround: Load the page *inclusive Login:s and User:s*.
      // Otherwise only Actions will be included (in debateWithIdsNoUsers)
      // and then the page cannot be rendered (there'll be None.get errors).
      val debateWithIds =
          _spi.loadPage(where.tenantId, debateWithIdsNoUsers.guid).open_!
      // ------------
      val key = Key(where.tenantId, debateWithIds.guid)
      val duplicate = _cache.putIfAbsent(key, debateWithIds)
      errorIf(duplicate ne null, "Newly created page "+ safed(debate.guid) +
          " already present in mem cache [debiki_error_38WcK905]")
      debateWithIds
    }
  }

  override def savePageActions[T](tenantId: String, debateId: String,
                                  xs: List[T]): Box[List[T]] = {
    for (xsWithIds <- _spi.savePageActions(tenantId, debateId, xs)) yield {
      val key = Key(tenantId, debateId)
      var replaced = false
      while (!replaced) {
        val oldPage = _cache.get(key)
        // -- Should to: -----------
        // val newPage = oldPage ++ xsWithIds
        // newPage might == oldPage, if another thread just refreshed
        // the page from the database.
        // -- But bug: -------------
        // None$.get: newPage might not contain xs.loginId, nor the
        // related Identity and User. So later, when newPage
        // is rendered, there'll be a scala.None$.get, from inside
        // DebateHtml._layoutPosts.
        // -- So instead: ----------
        // This loads all Logins, Identity:s and Users referenced by the page:
        val newPage = _spi.loadPage(tenantId, debateId).open_!
        // -------- Unfortunately.--
        replaced = _cache.replace(key, oldPage, newPage)
      }
      xsWithIds  // TODO return newPage instead? Or possibly a pair.
    }
  }

  override def loadPage(tenantId: String, debateId: String): Box[Debate] = {
    try {
      Full(_cache.get(Key(tenantId, debateId)))
    } catch {
      case e: NullPointerException =>
        Empty
    }
  }

}

/** Always accesses the database, whenever you ask it do do something.
 *
 *  Useful when constructing test suites that should access the database.
 */
class NonCachingDao(protected val _spi: DaoSpi) extends Dao

