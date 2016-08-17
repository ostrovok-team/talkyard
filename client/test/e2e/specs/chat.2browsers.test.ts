/// <reference path="../test-types.ts"/>
/// <reference path="../../../../modules/definitely-typed/lodash/lodash.d.ts"/>
/// <reference path="../../../../modules/definitely-typed/mocha/mocha.d.ts"/>

import * as _ from 'lodash';
import server = require('../utils/server');
import utils = require('../utils/utils');
import pages = require('../utils/pages');
import settings = require('../utils/settings');
import make = require('../utils/make');
import logAndDie = require('../utils/log-and-die');
var logUnusual = logAndDie.logUnusual, die = logAndDie.die, dieIf = logAndDie.dieIf;
var logMessage = logAndDie.logMessage;

declare var browser: any;
declare var browserA: any;
declare var browserB: any;

var everyone;
var owen;
var maria;


describe('chat', function() {


  it('create site with two members', function() {
    everyone = browser;
    owen = browserA;
    maria = browserB;

    var site: SiteData = make.emptySiteOwnedByOwen();
    site.meta.localHostname = 'chat-' + Date.now();

    site.members.push(make.memberMaria());

    var rootCategoryId = 1;

    var forumPage = make.page({
      id: 'fmp',
      role: <PageRole> 7,  // [commonjs] PageRole.Forum
      categoryId: rootCategoryId,
      authorId: -1,    // [commonjs] SystemUserId   [5KGEP02]
    });
    site.pages.push(forumPage);

    site.pagePaths.push({ folder: '/', pageId: forumPage.id, showId: false, slug: '' });

    site.posts.push(make.post({
      page: forumPage,
      nr: 0,
      approvedSource: "Forum Title",
      approvedHtmlSanitized: "Forum Title",
    }));

    site.posts.push(make.post({
      page: forumPage,
      nr: 1,
      approvedSource: "Forum intro text.",
      approvedHtmlSanitized: "<p>Forum intro text.</p>",
    }));

    var rootCategory = make.rootCategoryWithIdFor(rootCategoryId, forumPage);
    site.categories.push(rootCategory);

    var uncategorizedCategory = make.categoryWithIdFor(2, forumPage);
    uncategorizedCategory.parentId = rootCategory.id;
    uncategorizedCategory.name = "Uncatigorized";
    uncategorizedCategory.slug = "uncatigorized";
    uncategorizedCategory.description = '__uncategorized__';
    site.categories.push(uncategorizedCategory);


    var idAddress = server.importSiteData(site);
    browser.go(idAddress.siteIdOrigin);
    // browser.assertTextMatches('body', /login as admin to create something/);

    //browser.perhapsDebug();
  });

  it("Owen logs in, creates a chat topic", function() {
    browser.debug();
    owen.waitAndClick('#e2eCreateChatB');
    browser.debug();
    pages.loginDialog.loginWithPassword({ username: '', password: '' });
    browser.debug();
    owen.waitAndSetValue('.esEdtr_titleEtc_title', "Chat channel title");
    owen.setValue('textarea', "Chat channel purpose");
    owen.rememberCurrentUrl();
    browser.debug();
    owen.waitAndClick('.e2eSaveBtn');
    owen.waitForNewUrl();
  });

  it("Maria in browserB can create a chat topic", function() {
    maria.url(owen.url().value);
    browser.debug();
  });

  it("Done?", function() {
    //everyone.perhapsDebug();
  });
});

