/*
 * Copyright (C) 2014, 2017 Kaj Magnus Lindberg
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

/// <reference path="../../typedefs/react/react.d.ts" />
/// <reference path="../../typedefs/lodash/lodash.d.ts" />
/// <reference path="../plain-old-javascript.d.ts" />
/// <reference path="../ReactStore.ts" />

//------------------------------------------------------------------------------
   namespace debiki2.page.PostsReadTracker {
//------------------------------------------------------------------------------

/*
Marks:  Bookmark. UnreadMark. ActionMark.


// topic time
// PAUSE_UNLESS_SCROLLED
  // MAX_TRACKING_TIME    const sinceScrolled = now - this._lastScrolled;
  if (sinceScrolled > PAUSE_UNLESS_SCROLLED) {
    return;
  }
  const diff = now - this._lastTick;
  this._lastFlush += diff;
  this._lastTick = now;
  // Don't track timings if we're not in focus
  if (!Discourse.get("hasFocus")) return;
*/

let d = { i: debiki.internal, u: debiki.v0.util };
let $: any = d.i.$;

export let debugIntervalHandler = null;

interface ReadState {
  postNr: number;
  mark?: number;
  charsRead?: number;
  hasBeenRead?: boolean;
  textLength?: number;
}


let readStatesByPostNr: { [postNr: number]: ReadState } = {};
let postNrsVisibleLastTick: { [postNr: number]: boolean } = {};
let pageId = debiki2.ReactStore.getPageId();
let postNrsJustRead = [];

// Most people read 200 words per minute with a reading comprehension of 60%.
// 0.1% read 1 000 wpm with a comprehension of 85%.
// A speed reading test article was 3692 chars, 597 words (www.readingsoft.com)
// and a speed of 200 wpm means 3692 / 597.0 * 200 / 60 = 20.6 chars per second.
// So 40 chars per second is fast, = 400 wpm, but on the other hand people frequently don't
// read text online so very carefully (?) so might make sense. Anyway better err on the side of
// assuming people read too fast, than too slow, because never-marking-a-post-as-read although
// the user did read it, seems more annoying, than marking-as-read posts when the user has read
// parts-of-it or most-of-it-but-not-all.
let charsReadPerSecond = 40;

// People usually (?) don't read everything in a long comment, so mark a comment as read after
// some seconds.
let maxCharsReadPerPost = charsReadPerSecond * 5.5;

let secondsBetweenTicks = 1;
let secondsSpentReading = 0;
let secondsLostPerNewPostInViewport = 0.4;
let maxConfusionSeconds = 0.8;
let localStorageKey = 'debikiPostNrsReadByPageId';

export function start() {
  debugIntervalHandler = setInterval(trackReadingActivity, secondsBetweenTicks * 1000);
}


export function getPostNrsAutoReadLongAgo(): number[] {
  if (!localStorage)
    return [];

  let postNrsReadByPageId = getFromLocalStorage(localStorageKey) || {};
  return postNrsReadByPageId[pageId] || [];
}


function trackReadingActivity() {
  // Don't remove posts read one tick ago until now, so they get time to fade away slowly.
  _.each(postNrsJustRead, postNr => {
    debiki2.ReactActions.markPostAsRead(postNr, false);
  });
  postNrsJustRead = [];

  if (!document.hasFocus()) {
    secondsSpentReading = -maxConfusionSeconds;
    return;
  }

  let store: Store = ReactStore.allData();
  let me: Myself = store.me;

  if (!me.isLoggedIn)
    return;

  let visibleUnreadPostsStats = [];
  let numVisibleUnreadChars = 0;
  let postsVisibleThisTick: { [postNr: number]: boolean } = {};

  // PERFORMANCE COULD optimize: check top level threads first, only check posts in
  // thread if parts of the thread is inside the viewport? isInViewport() takes
  // really long if there are > 200 comments (not good for mobile phones' battery?).

  let unreadPosts: Post[] = [];
  _.each(store.postsByNr, (post: Post) => {
    if (!me_hasRead(me, post)) {
      unreadPosts.push(post);
    }
  });

  _.each(unreadPosts, (post: Post) => {
    let postBody = $('#post-' + post.nr).children('.dw-p-bd');
    if (!postBody.length || !isInViewport(postBody))
      return;

    postsVisibleThisTick[post.nr] = true;
    let progress: ReadState = readStatesByPostNr[post.nr];

    if (!progress) {
      progress = { postNr: post.nr, charsRead: 0 };
      readStatesByPostNr[post.nr] = progress;
    }

    if (progress.hasBeenRead)
      return;

    if (!progress.textLength) {
      progress.textLength = postBody.text().replace(/\s/g, '').length;
    }

    visibleUnreadPostsStats.push(progress);
    numVisibleUnreadChars += progress.textLength;
  });

  let numPostsScrolledIntoViewport = 0;
  for (let i$ = 0, len$ = visibleUnreadPostsStats.length; i$ < len$; ++i$) {
    let stats: ReadState = visibleUnreadPostsStats[i$];
    if (!postNrsVisibleLastTick[stats.postNr]) {
      numPostsScrolledIntoViewport += 1;
    }
  }

  postNrsVisibleLastTick = postsVisibleThisTick;
  secondsSpentReading +=
      secondsBetweenTicks - numPostsScrolledIntoViewport * secondsLostPerNewPostInViewport;

  if (secondsBetweenTicks < secondsSpentReading) {
    secondsSpentReading = secondsBetweenTicks;
  }

  if (secondsSpentReading < -maxConfusionSeconds) {
    secondsSpentReading = -maxConfusionSeconds;
  }

  let charsLeftThisTick = Math.max(0, charsReadPerSecond * secondsSpentReading);

  for (let i$ = 0, len$ = visibleUnreadPostsStats.length; i$ < len$; ++i$) {
    let stats: ReadState = visibleUnreadPostsStats[i$];
    let charsToRead = Math.min(maxCharsReadPerPost, stats.textLength);
    let charsReadNow = Math.min(charsLeftThisTick, charsToRead - stats.charsRead);

    // Let's read all posts at the same time instead. We don't know which one the user is
    // reading anyway, and feels a bit annoying to see reading-progress advancing for *the wrong*
    // post. — Often the user scrolls into view only one post at a time? And then this approach
    // will give ok results I think. Also, both Discourse and Gitter.im advance reading-progress
    // for all posts on screen at once.
    // So, don't:  charsLeftThisTick -= charsReadNow;

    stats.charsRead += charsReadNow;
    if (stats.charsRead >= charsToRead) {
      stats.hasBeenRead = true;
      rememberHasBeenRead(stats.postNr);
    }

    let fractionRead = !charsToRead ? 1.0 : stats.charsRead / charsToRead;
    if (fractionRead) {
      fadeUnreadMark(stats.postNr, fractionRead);
    }
    if (fractionRead >= 1) {
      // Don't remove until next tick, so a fade-out animation gets time to run. [8LKW204R]
      postNrsJustRead.push(stats.postNr);
    }
  }
}


function rememberHasBeenRead(postNr: number) {
  if (!localStorage)
    return;

  let postNrsReadByPageId = getFromLocalStorage(localStorageKey) || {};
  let postNrsRead = postNrsReadByPageId[pageId] || [];
  postNrsReadByPageId[pageId] = postNrsRead;
  postNrsRead.push(postNr);
  putInLocalStorage(localStorageKey, postNrsReadByPageId);
}


/**
 * Customized is-in-viewport test to find out if a post, or at least
 * the start of it, is visible. Takes mobile phones into account: If the
 * post spans the whole viewport (from top to bottom) it's considered
 * visible.
 */
function isInViewport($postBody){
  let bounds = $postBody[0].getBoundingClientRect();
  // 100 px is 3-4 rows text. If that much is visible, feels OK to mark the post as read.
  let aBitDown = Math.min(bounds.bottom, bounds.top + 100);
  let windowHeight = debiki.window.height();
  let windowWidth = debiki.window.width();
  let inViewportY = bounds.top >= 0 && aBitDown <= windowHeight;
  let inViewportX = bounds.left >= 0 && bounds.right <= windowWidth;
  let spansViewportY = bounds.top <= 0 && bounds.bottom >= windowHeight;
  return (inViewportY || spansViewportY) && inViewportX;
}


function fadeUnreadMark(postNr, fractionRead) {
  // Map fractionRead to one of 10,30,50,70,90 %:
  let percent = Math.floor(fractionRead * 5) * 20 + 10;
  percent = Math.min(90, percent);
  let selector = postNr === BodyNr ? '.dw-ar-p-hd' : '#post-' + postNr;
  $(selector).find('.s_P_H_Unr').addClass('s_P_H_Unr-' + percent);  // [8LKW204R]
}


//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=r list
