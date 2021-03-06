/**
 * Copyright (c) 2016 Kaj Magnus Lindberg
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

package com.debiki.core

import EditedSettings._
import Prelude._


sealed abstract class ContribAgreement(protected val IntVal: Int) { def toInt: Int = IntVal }
object ContribAgreement {
  case object CcBy3And4 extends ContribAgreement(10)
  case object CcBySa3And4 extends ContribAgreement(40)
  case object CcByNcSa3And4 extends ContribAgreement(70)
  case object UseOnThisSiteOnly extends ContribAgreement(100)

  def fromInt(value: Int): Option[ContribAgreement] = Some(value match {
    case CcBy3And4.IntVal => CcBy3And4
    case CcBySa3And4.IntVal => CcBySa3And4
    case CcByNcSa3And4.IntVal => CcByNcSa3And4
    case UseOnThisSiteOnly.IntVal => UseOnThisSiteOnly
    case _ => return None
  })
}


sealed abstract class ContentLicense(protected val IntVal: Int) { def toInt: Int = IntVal }
object ContentLicense {
  case object CcBy4 extends ContentLicense(10)
  case object CcBySa4 extends ContentLicense(40)
  case object CcByNcSa4 extends ContentLicense(70)
  case object AllRightsReserved extends ContentLicense(100)

  def fromInt(value: Int): Option[ContentLicense] = Some(value match {
    case CcBy4.IntVal => CcBy4
    case CcBySa4.IntVal => CcBySa4
    case CcByNcSa4.IntVal => CcByNcSa4
    case AllRightsReserved.IntVal => AllRightsReserved
    case _ => return None
  })
}


/** Contains only settings that have been edited — they are Some(value), others are None.
  * Because only edited settings need to be saved to the database.
  */
case class EditedSettings(
  userMustBeAuthenticated: Option[Boolean],
  userMustBeApproved: Option[Boolean],
  inviteOnly: Option[Boolean],
  allowSignup: Option[Boolean],
  allowLocalSignup: Option[Boolean],
  allowGuestLogin: Option[Boolean],
  enableGoogleLogin: Option[Boolean],
  enableFacebookLogin: Option[Boolean],
  enableTwitterLogin: Option[Boolean],
  enableGitHubLogin: Option[Boolean],
  requireVerifiedEmail: Option[Boolean],
  emailDomainBlacklist: Option[String],
  emailDomainWhitelist: Option[String],
  mayComposeBeforeSignup: Option[Boolean],
  mayPostBeforeEmailVerified: Option[Boolean],
  doubleTypeEmailAddress: Option[Boolean],
  doubleTypePassword: Option[Boolean],
  minPasswordLength: Option[Int],
  begForEmailAddress: Option[Boolean],
  forumMainView: Option[String],
  forumTopicsSortButtons: Option[String],
  forumCategoryLinks: Option[String],
  forumTopicsLayout: Option[TopicListLayout],
  forumCategoriesLayout: Option[CategoriesLayout],
  showCategories: Option[Boolean],
  showTopicFilterButton: Option[Boolean],
  showTopicTypes: Option[Boolean],
  selectTopicType: Option[Boolean],
  showAuthorHow: Option[ShowAuthorHow],
  watchbarStartsOpen: Option[Boolean],
  numFirstPostsToReview: Option[Int],
  numFirstPostsToApprove: Option[Int],
  numFirstPostsToAllow: Option[Int],
  faviconUrl: Option[String],
  headStylesHtml: Option[String],
  headScriptsHtml: Option[String],
  endOfBodyHtml: Option[String],
  headerHtml: Option[String],
  footerHtml: Option[String],
  horizontalComments: Option[Boolean],
  socialLinksHtml: Option[String],
  logoUrlOrHtml: Option[String],
  orgDomain: Option[String],
  orgFullName: Option[String],
  orgShortName: Option[String],
  contribAgreement: Option[ContribAgreement],
  contentLicense: Option[ContentLicense],
  languageCode: Option[String],
  googleUniversalAnalyticsTrackingId: Option[String],
  enableChat: Option[Boolean],
  enableDirectMessages: Option[Boolean],
  showSubCommunities: Option[Boolean],
  showExperimental: Option[Boolean],
  allowEmbeddingFrom: Option[String],
  htmlTagCssClasses: Option[String],
  numFlagsToHidePost: Option[Int],
  cooldownMinutesAfterFlaggedHidden: Option[Int],
  numFlagsToBlockNewUser: Option[Int],
  numFlaggersToBlockNewUser: Option[Int],
  notifyModsIfUserBlocked: Option[Boolean],
  regularMemberFlagWeight: Option[Float],
  coreMemberFlagWeight: Option[Float]) {

  numFirstPostsToAllow foreach { num =>
    require(num >= 0 && num <= MaxNumFirstPosts, "EsE4GUK20")
  }

  numFirstPostsToApprove foreach { num =>
    require(num >= 0 && num <= MaxNumFirstPosts, "EsE6JK250")
    numFirstPostsToAllow foreach { numToAllow =>
      require(numToAllow >= num, "EsE2GHF8")
    }
  }

  numFirstPostsToReview foreach { num =>
    require(num >= 0 && num <= MaxNumFirstPosts, "EsE8WK2G3")
  }
}


object EditedSettings {

  // Sync with Typescript [6KG2W57]
  val MaxNumFirstPosts = 10

  val empty = EditedSettings(
    userMustBeAuthenticated = None,
    userMustBeApproved = None,
    inviteOnly = None,
    allowSignup = None,
    allowLocalSignup = None,
    allowGuestLogin = None,
    enableGoogleLogin = None,
    enableFacebookLogin = None,
    enableTwitterLogin = None,
    enableGitHubLogin = None,
    requireVerifiedEmail = None,
    emailDomainBlacklist = None,
    emailDomainWhitelist = None,
    mayComposeBeforeSignup = None,
    mayPostBeforeEmailVerified = None,
    doubleTypeEmailAddress = None,
    doubleTypePassword = None,
    minPasswordLength = None,
    begForEmailAddress = None,
    forumMainView = None,
    forumTopicsSortButtons = None,
    forumCategoryLinks = None,
    forumTopicsLayout = None,
    forumCategoriesLayout = None,
    showCategories = None,
    showTopicFilterButton = None,
    showTopicTypes = None,
    selectTopicType = None,
    showAuthorHow = None,
    watchbarStartsOpen = None,
    numFirstPostsToReview = None,
    numFirstPostsToApprove = None,
    numFirstPostsToAllow = None,
    faviconUrl = None,
    headStylesHtml = None,
    headScriptsHtml = None,
    endOfBodyHtml = None,
    headerHtml = None,
    footerHtml = None,
    horizontalComments = None,
    socialLinksHtml = None,
    logoUrlOrHtml = None,
    orgDomain = None,
    orgFullName = None,
    orgShortName = None,
    contribAgreement = None,
    contentLicense = None,
    languageCode = None,
    googleUniversalAnalyticsTrackingId = None,
    enableChat = None,
    enableDirectMessages = None,
    showSubCommunities = None,
    showExperimental = None,
    allowEmbeddingFrom = None,
    htmlTagCssClasses = None,
    numFlagsToHidePost = None,
    cooldownMinutesAfterFlaggedHidden = None,
    numFlagsToBlockNewUser = None,
    numFlaggersToBlockNewUser = None,
    notifyModsIfUserBlocked = None,
    regularMemberFlagWeight = None,
    coreMemberFlagWeight = None)

}


/** E.g. settingsToSave.title.isDefined means that the title should be updated.
  * And settingsToSave.title.get.isEmpty means that its value should be cleared,
  * so that the default will be used instead.
  * And if settingsToSave.title.get.isDefined, then the title will be set to
  * settingsToSave.title.get.get.
  */
case class SettingsToSave(
  userMustBeAuthenticated: Option[Option[Boolean]] = None,
  userMustBeApproved: Option[Option[Boolean]] = None,
  inviteOnly: Option[Option[Boolean]] = None,
  allowSignup: Option[Option[Boolean]] = None,
  allowLocalSignup: Option[Option[Boolean]] = None,
  allowGuestLogin: Option[Option[Boolean]] = None,
  enableGoogleLogin: Option[Option[Boolean]] = None,
  enableFacebookLogin: Option[Option[Boolean]] = None,
  enableTwitterLogin: Option[Option[Boolean]] = None,
  enableGitHubLogin: Option[Option[Boolean]] = None,
  requireVerifiedEmail: Option[Option[Boolean]] = None,
  emailDomainBlacklist: Option[Option[String]] = None,
  emailDomainWhitelist: Option[Option[String]] = None,
  mayComposeBeforeSignup: Option[Option[Boolean]] = None,
  mayPostBeforeEmailVerified: Option[Option[Boolean]] = None,
  doubleTypeEmailAddress: Option[Option[Boolean]] = None,
  doubleTypePassword: Option[Option[Boolean]] = None,
  minPasswordLength: Option[Option[Int]] = None,
  begForEmailAddress: Option[Option[Boolean]] = None,
  forumMainView: Option[Option[String]] = None,
  forumTopicsSortButtons: Option[Option[String]] = None,
  forumCategoryLinks: Option[Option[String]] = None,
  forumTopicsLayout: Option[Option[TopicListLayout]] = None,
  forumCategoriesLayout: Option[Option[CategoriesLayout]] = None,
  showCategories: Option[Option[Boolean]] = None,
  showTopicFilterButton: Option[Option[Boolean]] = None,
  showTopicTypes: Option[Option[Boolean]] = None,
  selectTopicType: Option[Option[Boolean]] = None,
  showAuthorHow: Option[Option[ShowAuthorHow]] = None,
  watchbarStartsOpen: Option[Option[Boolean]] = None,
  numFirstPostsToReview: Option[Option[Int]] = None,
  numFirstPostsToApprove: Option[Option[Int]] = None,
  numFirstPostsToAllow: Option[Option[Int]] = None,
  faviconUrl: Option[Option[String]] = None,
  headStylesHtml: Option[Option[String]] = None,
  headScriptsHtml: Option[Option[String]] = None,
  endOfBodyHtml: Option[Option[String]] = None,
  headerHtml: Option[Option[String]] = None,
  footerHtml: Option[Option[String]] = None,
  horizontalComments: Option[Option[Boolean]] = None,
  socialLinksHtml: Option[Option[String]] = None,
  logoUrlOrHtml: Option[Option[String]] = None,
  orgDomain: Option[Option[String]] = None,
  orgFullName: Option[Option[String]] = None,
  orgShortName: Option[Option[String]] = None,
  contribAgreement: Option[Option[ContribAgreement]] = None,
  contentLicense: Option[Option[ContentLicense]] = None,
  languageCode: Option[Option[String]] = None,
  googleUniversalAnalyticsTrackingId: Option[Option[String]] = None,
  enableChat: Option[Option[Boolean]] = None,
  enableDirectMessages: Option[Option[Boolean]] = None,
  showSubCommunities: Option[Option[Boolean]] = None,
  showExperimental: Option[Option[Boolean]] = None,
  allowEmbeddingFrom: Option[Option[String]] = None,
  htmlTagCssClasses: Option[Option[String]] = None,
  numFlagsToHidePost: Option[Option[Int]] = None,
  cooldownMinutesAfterFlaggedHidden: Option[Option[Int]] = None,
  numFlagsToBlockNewUser: Option[Option[Int]] = None,
  numFlaggersToBlockNewUser: Option[Option[Int]] = None,
  notifyModsIfUserBlocked: Option[Option[Boolean]] = None,
  regularMemberFlagWeight: Option[Option[Float]] = None,
  coreMemberFlagWeight: Option[Option[Float]] = None) {

  // The language code must be like en_US.
  require(languageCode.forall(_.forall(_.isAToZUnderscoreOnly)), "Weird lang code [TyE2WKBYF]")
  require(languageCode.forall(_.forall(_.length < 10)), "Too long language code [TyE2WKBP5]")

  if (contribAgreement.contains(Some(ContribAgreement.UseOnThisSiteOnly)) &&
      contentLicense.isDefined) {
    require(contentLicense.get.contains(ContentLicense.AllRightsReserved), "EsE8YKF2")
  }

  // For now, disable license-to-us-for-use-on-this-site-only, see [6UK2F4X] in admin-app.ts(?).
  if (contribAgreement.contains(Some(ContribAgreement.UseOnThisSiteOnly))) {
    unimplemented("UseOnThisSiteOnly not yet supported because of tricky legal stuff [EsE4YKW21]")
  }
}


