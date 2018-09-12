/// <reference path="../test-types.ts"/>

import * as _ from 'lodash';
import assert = require('assert');
import server = require('../utils/server');
import utils = require('../utils/utils');
import make = require('../utils/make');
import pagesFor = require('../utils/pages-for');
import settings = require('../utils/settings');
import { buildSite } from '../utils/site-builder';
import logAndDie = require('../utils/log-and-die');
import c = require('../test-constants');

declare let browser: any;

let forum: LargeTestForum;

let everyonesBrowser;
let maria;
let mariasBrowser;
let mallory;
let mallorysBrowser;

let idAddress: IdAddress;
let siteId;
let forumTitle = "Change Password Test Forum";

let mariasPassword2 = "kittens-cute-sooo-cute-I-pat-cute-cute-347-each-day";


describe("user-profile-change-password.test.ts  TyT6HJ2RD1", () => {

  it("import a site, init people", () => {
    everyonesBrowser = _.assign(browser, pagesFor(browser));
    mariasBrowser = everyonesBrowser;
    mallorysBrowser = everyonesBrowser;

    forum = buildSite().addLargeForum({ title: forumTitle });
    maria = forum.members.maria;
    mallory = forum.members.mallory;

    idAddress = server.importSiteData(forum.siteData);
    siteId = idAddress.id;
  });

  it("Maria logs in", () => {
    mariasBrowser.go(idAddress.origin + '/');
    mariasBrowser.complex.loginWithPasswordViaTopbar(maria);
  });

  it("... goes to her profile page", () => {
    mariasBrowser.topbar.clickGoToProfile();
    mariasBrowser.userProfilePage.clickGoToPreferences();
    mariasBrowser.debugEachStep = true;
  });

  it("... and changes her password", () => {
    mariasBrowser.userProfilePage.preferences.clickChangePassword();
    mariasBrowser.changePasswordDialog.clickYesChange();
  });

  let mariasResetPasswordLink;

  it("She gets a password reset email", () => {
    mariasResetPasswordLink = server.waitAndGetResetPasswordLinkEmailedTo(
        siteId, maria.emailAddress, browser);
  });

  it("... clicks the reset-password link", () => {
    mariasBrowser.go(mariasResetPasswordLink);
  });

  it("... chooses a new password", () => {
    mariasBrowser.chooseNewPasswordPage.typeAndSaveNewPassword(mariasPassword2);
  });

  it("Maria logs out", () => {
    mariasBrowser.topbar.clickLogout();
  });

  it("Opens the login dialog, cannot login with the old password", () => {
    mariasBrowser.complex.loginWithPasswordViaTopbar(maria, { resultInError: true });
  });

  it("... but yes the new password works", () => {
    mariasBrowser.complex.loginWithPasswordViaTopbar(maria, mariasPassword2);
  });

  it("Mallory cannot login with Maria's old password", () => {
    mariasBrowser.topbar.clickLogout();
    mallorysBrowser.complex.loginWithPasswordViaTopbar(maria, { resultInError: true });
  });

  it("... not with her new", () => {
    mallorysBrowser.loginDialog.tryLogin(maria.username, mariasPassword2);
  });

  it("... yes though, with his own", () => {
    mallorysBrowser.loginDialog.tryLogin(maria.username, mariasPassword2);
    mallorysBrowser.waitUntilModalGone();
    mallorysBrowser.waitUntilLoadingOverlayGone();
  });

  it("... he gets logged in properly", () => {
    mallorysBrowser.topbar.waitForMyMenuVisible();
    mallorysBrowser.topbar.assertMyUsernameMatches(mallory.username);
  });

});

