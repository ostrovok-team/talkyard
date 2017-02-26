/*
 * Copyright (C) 2015 Kaj Magnus Lindberg
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

/// <reference path="../typedefs/react/react.d.ts" />
/// <reference path="plain-old-javascript.d.ts" />

// Don't <reference>, causes lots of TS errors.
declare const Bliss: any;
declare function $$(selector: string): Element[];

// Defined in client/third-party/smoothscroll-tiny.js.
declare function smoothScroll(elem: Element, x: number, y: number);

// Defined in client/third-party/get-set-cookie.js.
declare function getSetCookie(cookieName: string, value?: string, options?: any): string;

const React = window['React'];
const ReactDOM = window['ReactDOM'];
const ReactDOMServer = window['ReactDOMServer'];
const ReactCSSTransitionGroup = isServerSide() ? null :
  reactCreateFactory(React.addons.CSSTransitionGroup);
const ReactRouter = window['ReactRouter'];
const Router = reactCreateFactory(ReactRouter.Router);

// backw compat, later, do once per file instead (don't want a global 'r').
const r = React.DOM;

// Let this be a function, not a variable, so it can be used directly.
// (Otherwise there's a server side reactCreateFactory-not-yet-inited error)
function reactCreateFactory(x) {
  return React['createFactory'](x);
}


function isServerSide(): boolean {
  return !!window['ReactDOMServer'];
}


// Use this function to call getBoundingClientRect() and other stuff just before the next repaint,
// to avoid forced refresh of the layout.
function doNextFrameOrNow(something: () => void) {
  if (window.requestAnimationFrame) {
    window.requestAnimationFrame(something)
  }
  else {
    something();
  }
}


/**
 * Basic stuff needed by essentially all modules / files.
 */
//------------------------------------------------------------------------------
   namespace debiki2 {
//------------------------------------------------------------------------------

// E2e tests won't compile without this. Why not, React.js already included above? Oh well.
//declare var React;
// declare var ReactRouter;

export const Link = reactCreateFactory(ReactRouter.Link);


export function die(errorMessage: string) {
  const dialogs: any = debiki2['pagedialogs'];
  // I don't remember why I added setTimeout() but there was a good reason.
  setTimeout(() => {
    debiki2['Server'].logError(errorMessage);
  });
  if (dialogs && dialogs.showAndThrowClientSideError) {
    dialogs.showAndThrowClientSideError(errorMessage);
  }
  else {
    // Server side.
    throw new Error(errorMessage);
  }
}

export function dieIf(condition, errorMessage: string) {
  if (condition) {
    die(errorMessage);
  }
}


export function logError(errorMessage: string) {
  // Why setTimeout()? I don't remember, see above in die(errorMessage).
  setTimeout(() => {
    debiki2['Server'].logError(errorMessage);
  });
}


export function logErrorIf(condition, errorMessage: string) {
  if (condition) {
    logError(errorMessage);
  }
}


export function scrollToBottom(node) {
  dieIf(!node, 'DwE9FMW2');
  node.scrollTop = node.scrollHeight;
}


export function anyE2eTestPassword() {
  return (window.location.search.match(/e2eTestPassword=([^&#]+)/) || [])[1];
}

export function anyForbiddenPassword() {
  return (window.location.search.match(/forbiddenPassword=([^&#]+)/) || [])[1];
}


export const findDOMNode = isServerSide() ? null : window['ReactDOM'].findDOMNode;
dieIf(!isServerSide() && !findDOMNode, 'EsE6UMGY2');


export function hasErrorCode(request: HttpRequest, statusCode: string) {
  return request.responseText && request.responseText.indexOf(statusCode) !== -1;
}


export function toId(x: number | { id: number } | { uniqueId: number }): number {
  if (_.isNumber(x)) return <number> x;
  const nr = x['id'];
  if (_.isNumber(nr)) return <number> nr;
  return x['uniqueId'];
}


export function isBlank(x): boolean {
  return _.isEmpty(x) || !x.trim();
}


export function nonEmpty(x): boolean {
  return !_.isEmpty(x);
}


export function isDefined2(x): boolean {
  return !_.isUndefined(x);
}


// Ooops bad name, shouldn't include null
export function isDefined(x): boolean {  // rename to isNotNullOrUndefined(x)
  return !isNullOrUndefined(x);
}


export function isNullOrUndefined(x): boolean {
  return _.isNull(x) || _.isUndefined(x);
}


export function firstDefinedOf(x, y, z?) {
  return !_.isUndefined(x) ? x : (!_.isUndefined(y) ? y : z);
}


export function $bySelector(selector: string): NodeListOf<Element> {
  return document.querySelectorAll(selector);
}


export function $byId(elemId: string): HTMLElement {
  // @ifdef DEBUG
  dieIf(/#\., /.test(elemId), 'EdE2KWWE45');
  // @endif
  return document.getElementById(elemId);
}


export function $$byClass(className: string): HTMLCollectionOf<Element> {
  // @ifdef DEBUG
  // getElementsByClassName() works with one single class only.
  dieIf(/#\., /.test(className), 'EdE5JLKS02');
  // @endif
  return document.getElementsByClassName(className);
}


export const $h = {

  // classesString should be a space and/or comma separated class name string.
  addClasses: function(elem: Element, classesString: string) {
    // @ifdef DEBUG
    dieIf(/#\./.test(classesString), 'EdE6EF2T47');
    // @endif
    const classes = classesString.replace(/ *, */g, ',').replace(/ +/g, ',').split(',');
    elem.classList.add(...classes);
  },


  removeClasses: function(elem: Element, classesString: string) {
    // @ifdef DEBUG
    dieIf(/#\./.test(classesString), 'EdEKEW20P7');
    // @endif
    const classes = classesString.replace(/ *, */g, ',').replace(/ +/g, ',').split(',');
    elem.classList.remove(...classes);
  },



  parseHtml: function(htmlText: string): HTMLCollection {
    const doc = document.implementation.createHTMLDocument(''); // empty dummy title
    doc.body.innerHTML = htmlText;
    return doc.body.children;
  },


  wrapParseHtml: function(htmlText: string): Element {
    return $h.parseHtml('<div>' + htmlText + '</div>')[0];
  }

};

//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=r list
