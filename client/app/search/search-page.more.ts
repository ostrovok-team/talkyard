/**
 * Copyright (C) 2016 Kaj Magnus Lindberg
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
/// <reference path="../slim-bundle.d.ts" />

//------------------------------------------------------------------------------
   module debiki2.search {
//------------------------------------------------------------------------------

var r = React.DOM;


export function routes() {
  return (
    Route({ path: '/-/search2', component: SearchPageComponent },
      IndexRoute({ component: DefaultComponent })));
}



var SearchPageComponent = React.createClass(<any> {
  render: function() {
    return (
      r.div({},
        reactelements.TopBar({
          backToSiteButtonTitle: "Back",
          extraMargin: true,
        }),
        r.div({ className: 'esLegal_home container', style: { marginTop: '20px' } },
          // href="/" will be wrong if coming from the forum and it's base path isn't /, but e.g.
          // /forum/. Ignore this minor problem, for now. [7KUFS25]
          r.a({ className: 'esLegal_home_link', href: '/' }, "Home",
            r.span({ className: 'esLegal_home_arw' }, ' →'))),
        r.div({},
          this.props.children)));
  }
});



var DefaultComponent = React.createClass(<any> {
  contextTypes: {
    router: React.PropTypes.object.isRequired
  },

  getInitialState: function() {
    return {
      searchResults: null,
      isSearching: false,
    };
  },

  componentWillMount: function() {
    var queryString = this.props.location.query;
    if (queryString.q) {
      let query = parseSearchQueryUrlParam(queryString.q);
      this.setState({ query: query });
      this.search(query);
    }
  },

  componentWillUnmount: function() {
    this.isGone = true;
  },

  search: function(query?: SearchQuery) {
    this.setState({ isSearching: true });
    Server.search(query || this.state.query, (results: SearchResults) => {
      if (this.isGone) return;
      this.setState({
        searchResults: results,
        isSearching: false,
      });
    });
  },

  onQueryTextEdited: function(event) {
    let query = parseSearchQueryInputText(event.target.value);
    this.setState({ query: query });
  },

  render: function() {
    let query: SearchQuery = this.state.query;
    let searchResults: SearchResults = this.state.searchResults;

    let anyInfoText;
    let resultsList;
    if (!searchResults) {
      if (this.state.isSearching) {
        anyInfoText = r.p({id: 'e_SP_IsSearching'}, "Searching...");
      }
    }
    else if (!searchResults.pagesAndHits.length) {
      anyInfoText = r.p({ id: 'e_SP_NothingFound' }, "Nothing found.");
    }
    else {
      let pagesAndHits: PageAndHits[] = searchResults.pagesAndHits;
      resultsList = pagesAndHits.map(pageAndHits =>
          SearchResultListItem({ pageAndHits: pageAndHits, key: pageAndHits.pageId }));
    }

    return (
      r.div({ className: 's_SP container' },
        r.form({},
          r.input({ type: 'text', tabIndex: '1', placeholder: "Text to search for",
            value: query.fullTextQuery, // for now
            className: 's_SP_QueryTI', onChange: this.onQueryTextEdited }),
          PrimaryButton({ value: "Search", className: 's_SP_SearchB',
              onClick: () => this.search() },
            "Search"),
        anyInfoText,
        r.div({ className: 's_SP_SRs' },
          r.ol({},
            resultsList)))));
  }
});



function SearchResultListItem(props) {
  let pageAndHits: PageAndHits = props.pageAndHits;
  let hits = pageAndHits.hits.map(hit =>
      SearchResultHit({ hit: hit, pageId: pageAndHits.pageId, key: hit.postNr }));
  return (
    r.li({},
      r.h3({ className: 'esSERP_Hit_PageTitle' },
        r.a({ href: `/-${pageAndHits.pageId}`, tabindex: 2 }, pageAndHits.pageTitle)),
      r.ol({}, hits)));
}



function SearchResultHit(props) {
  let hit: SearchHit = props.hit;
  let pageId = props.pageId;
  // Any html stuff was escaped here: [7YK24W].
  let safeHtml = hit.approvedTextWithHighligtsHtml.join(" <b>...</b> ");
  return (
    r.li({},
      r.span({ className: 'esSERP_Hit_In' },
        "In ", r.a({ href: `/-${pageId}#post-${hit.postNr}`, className: 'esSERP_Hit_In_Where' },
          foundWhere(hit)), ':'),
      r.p({ className: 'esSERP_Hit_Text',
          dangerouslySetInnerHTML: { __html: safeHtml }})));
}



function foundWhere(hit: SearchHit): string {
  switch (hit.postNr) {
    case TitleNr: return "the title";
    case BodyNr: return "the page text";
    default: return "a comment";
  }
}



function parseSearchQueryUrlParam(urlParamValue: string): SearchQuery {
  // We use '+' instead of '%20' for spaces in the url query string, so it becomes more readable.
  let textWithSpacesNotPlus = urlParamValue.replace(/\+/, ' ');
  let textDecoded = decodeURIComponent(textWithSpacesNotPlus);
  return parseSearchQueryInputText(textDecoded);
}



function parseSearchQueryInputText(text: string): SearchQuery {
  return {
    fullTextQuery: text, // for now
    // tags:
    // categories:
  };
}

//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=r list
