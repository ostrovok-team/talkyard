/*
 * Copyright (c) 2014-2016 Kaj Magnus Lindberg
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

/// <reference path="../prelude.ts" />
/// <reference path="../utils/utils.ts" />
/// <reference path="../utils/react-utils.ts" />
/// <reference path="../help/help.ts" />
/// <reference path="../help/help.ts" />
/// <reference path="../rules.ts" />
/// <reference path="discussion.ts" />
/// <reference path="chat.ts" />
/// <reference path="scroll-buttons.ts" />

// Wrapping in a module causes an ArrayIndexOutOfBoundsException: null error, see:
//  http://stackoverflow.com/questions/26189940/java-8-nashorn-arrayindexoutofboundsexception
// The bug has supposedly been fixed in Java 8u40. Once I'm using that version,
// remove `var exports = {};` from app/debiki/ReactRenderer.scala.
//------------------------------------------------------------------------------
   namespace debiki2 {
//------------------------------------------------------------------------------

const r = ReactDOMFactories;


export const PageWithStateComponent = createReactClass(<any> {
  displayName: 'PageWithState',
  mixins: [debiki2.StoreListenerMixin],

  getInitialState: function() {
    return { store: ReactStore.allData() };
  },

  onChange: function() {
    this.setState({ store: ReactStore.allData() });
  },

  componentWillMount: function() {
    const store: Store = this.state.store;
    const newUrlPath = this.props.location.pathname;
    // The page can be in the store already, either because it's included in html from the
    // server, or because we were on the page, navigated away, and went back.
    let hasPageAlready = false;
    _.each(store.pagesById, (page: Page) => {
      if (page.pagePath.value === newUrlPath) {
        hasPageAlready = true;
        if (page.pageId === store.currentPageId) {
          // We just loaded the whole html page from the server, and are already trying to
          // render 'page'. Don't try to show that page again here.
        }
        else {
          // If navigating back to EmptyPageId, maybe there'll be no myData; then create empty data.
          const myData = store.me.myDataByPageId[page.pageId] || makeNoPageData();
          ReactActions.showNewPage(page, [], myData);
        }
      }
    });
    if (!hasPageAlready) {
      this.loadNewPage(newUrlPath);
    }
  },

  componentWillReceiveProps: function(nextProps) {
    const newUrlPath = nextProps.location.pathname;
    const isNewPath = this.props.location.pathname !== newUrlPath;
    this.loadNewPage(newUrlPath);
  },

  loadNewPage: function(newUrlPath) {
    // UX maybe dim & overlay-cover the current page, to prevent interactions, until request completes?
    // So the user e.g. won't click Reply and start typing, but then the page suddenly changes.
    Server.loadPageJson(newUrlPath, response => {
      if (response.problemCode) {
        // SHOULD look at the code and do sth "smart" instead.
        die(`${response.problemMessage} [${response.problemCode}]`);
        return;
      }

      // This is the React store for showing the page at the new url path.
      const newStore: Store = JSON.parse(response.reactStoreJsonString);
      const pageId = newStore.currentPageId;
      const page = newStore.pagesById[pageId];
      const newUsers = _.values(newStore.usersByIdBrief);
      const myPageData = response.me.myDataByPageId[pageId];

      // Maybe the page has moved to a different url? The server would still find it, via the old url,
      // but we should update the address bar to the new correct url.
      const pagePath = page.pagePath.value;
      const loc = this.props.location;
      if (pagePath !== loc.pathname) {
        this.props.history.replace(pagePath + loc.search + loc.hash);
      }

      // This'll trigger a this.onChange() event.
      ReactActions.showNewPage(page, newUsers, myPageData);
    });
  },

  render: function() {
    // Send router props to the page.
    return Page({ store: this.state.store, ...this.props });
  }
});


export const PageWithState = reactCreateFactory(<any> PageWithStateComponent);


const Page = createComponent({
  displayName: 'Page',

  getInitialState: function() {
    return {
      useWideLayout: this.isPageWide(),
    };
  },

  componentDidMount: function() {
    // A tiny bit dupl code though, perhaps break out... what? a mixin? [5KFEWR7]
    this.timerHandle = setInterval(this.checkSizeChangeLayout, 200);
  },

  componentWillUnmount: function() {
    this.isGone = true;
    clearInterval(this.timerHandle);
  },

  checkSizeChangeLayout: function() {
    // Dupl code [5KFEWR7]
    if (this.isGone) return;
    var isWide = this.isPageWide();
    if (isWide !== this.state.useWideLayout) {
      this.setState({ useWideLayout: isWide });
    }
  },

  isPageWide: function(): boolean {
    return store_getApproxPageWidth(this.props) > UseWidePageLayoutMinWidth;
  },

  render: function() {
    const store: Store = this.props.store;
    const page: Page = store.currentPage;
    const content = page_isChatChannel(page.pageRole)
        ? debiki2.page.ChatMessages({ store: store })
        : debiki2.page.TitleBodyComments({ store: store });
    const compactClass = this.state.useWideLayout ? '' : ' esPage-Compact';
    const pageTypeClass = ' s_PT-' + page.pageRole;
    const isChat = page_isChatChannel(page.pageRole);
    return rFragment({},
      isChat ? r.div({ id: 'theChatVspace' }) : null,
      r.div({ className: 'esPage' + compactClass + pageTypeClass },
        r.div({ className: 'container' },
          r.article({},
            content))));
  }
});


export function renderTitleBodyCommentsToString() {
  debiki2.avatar.resetAvatars();

  // Comment in the next line to skip React server side and debug in browser only.
  //return '<p class="dw-page" data-reactid=".123" data-react-checksum="123">react_skipped [BRWSRDBG]</p>'

  // Compare with [2FKB5P].
  const store: Store = debiki2.ReactStore.allData();
  const page: Page = store.currentPage;
  if (page.pageRole === PageRole.Forum) {
    const routes = debiki2.forum.buildForumRoutes();
    // In the future, when using the HTML5 history API to update the URL when navigating
    // inside the forum, we can use `store.pagePath` below. But for now:
    const path = page.pagePath.value + 'latest';
    return ReactDOMServer.renderToString(
        Router({ location: path }, routes));
  }
  else {
    return ReactDOMServer.renderToString(Page({ store }));
  }
}

//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 list
