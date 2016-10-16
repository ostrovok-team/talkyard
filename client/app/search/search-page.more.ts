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
/// <reference path="../widgets.more.ts" />

//------------------------------------------------------------------------------
   module debiki2.search {
//------------------------------------------------------------------------------

var r = React.DOM;


export function routes() {
  return (
    Route({ path: '/-/search', component: SearchPageComponent },
      IndexRoute({ component: SearchPageContentComponent })));
}



var SearchPageComponent = React.createClass(<any> {
  displayName: 'SearchPageComponent',

  render: function() {
    return (
      r.div({},
        reactelements.TopBar({ backToSiteButtonTitle: "Back" }),
        r.div({ className: 'esLegal_home container', style: { marginTop: '20px' } },
          // href="/" will be wrong if coming from the forum and it's base path isn't /, but e.g.
          // /forum/. Ignore this minor problem, for now. [7KUFS25]
          r.a({ className: 'esLegal_home_link', href: '/' }, "Home",
            r.span({ className: 'esLegal_home_arw' }, ' →'))),
        r.div({},
          this.props.children)));
  }
});



var SearchPageContentComponent = React.createClass(<any> {
  displayName: 'SearchPageContentComponent',
  mixins: [debiki2.StoreListenerMixin],

  contextTypes: {
    router: React.PropTypes.object.isRequired
  },

  getInitialState: function() {
    return {
      searchResults: null,
      isSearching: false,
      store: debiki2.ReactStore.allData(),
    };
  },

  onChange: function() {
    this.setState({
      store: debiki2.ReactStore.allData(),
    });
  },

  componentWillMount: function() {
    let queryString = this.props.location.query;
    let queryParam = queryString.q || '';
    let query = parseSearchQueryUrlParam(queryParam);
    this.setState({ query: query });
    if (queryParam) {
      this.search(query);
    }
    if (this.props.location.query.advanced) {
      this.tagsLoaded = true;
      Server.loadTagsAndStats();
    }
  },

  componentWillUnmount: function() {
    this.isGone = true;
  },

  search: function(query?: SearchQuery) {
    query = query || this.state.query;
    this.setState({ isSearching: true });
    if (this.props.location.query.q !== query.rawQuery) {
      let queryStringObj = _.assign({}, this.props.location.query, { q: query.rawQuery });
      this.context.router.push({
        pathname: this.props.location.pathname,
        query: queryStringObj,
      });
    }
    Server.search(query.rawQuery, (results: SearchResults) => {
      if (this.isGone) return;
      this.setState({
        isSearching: false,
        searchResults: results,
        lastRawQuery: query.rawQuery,
      });
    });
  },

  onQueryTextEdited: function(event) {
    let query = parseSearchQueryInputText(event.target.value);
    this.setState({ query: query });
  },

  toggleAdvanced: function() {
    let queryStringObj = _.assign({}, this.props.location.query);
    if (queryStringObj.advanced) {
      delete queryStringObj.advanced;
    }
    else {
      if (!this.tagsLoaded) {
        this.tagsLoaded = true;
        Server.loadTagsAndStats();
      }
      queryStringObj.advanced = true;
    }
    this.context.router.push({
      pathname: this.props.location.pathname,
      query: queryStringObj,
    });
  },

  onTagsSelectionChange: function(labelsAndValues: any) {
    // We got null if the clear-all [x] button was pressed.
    labelsAndValues = labelsAndValues || [];
    let newTags = <string[]> _.map(labelsAndValues, 'value');
    let newQuery = updateTags(this.state.query, newTags);
    this.setState({ query: newQuery });
  },

  render: function() {
    let store: Store = this.state.store;
    let query: SearchQuery = this.state.query;
    let searchResults: SearchResults = this.state.searchResults;

    let isAdvancedOpen = !!this.props.location.query.advanced;
    let advancedSearch =
      Expandable({ header: "Advanced Search", onHeaderClick: this.toggleAdvanced,
          openButtonId: 'e_SP_AdvB', className: 's_SP_Adv', isOpen: isAdvancedOpen },
        !isAdvancedOpen ? null :
          AdvancedSearchPanel({ store: store, query: query,
            onTagsSelectionChange: this.onTagsSelectionChange }));

    let anyInfoText;
    let anyNothingFoundText;
    let resultsList;
    if (!searchResults) {
      if (this.state.isSearching) {
        anyInfoText = r.p({id: 'e_SP_IsSearching'}, "Searching...");
      }
    }
    else if (!searchResults.pagesAndHits.length) {
      anyNothingFoundText = r.p({ id: 'e_SP_NothingFound' }, "Nothing found.");
    }
    else {
      let pagesAndHits: PageAndHits[] = searchResults.pagesAndHits;
      resultsList = pagesAndHits.map(pageAndHits =>
          SearchResultListItem({ pageAndHits: pageAndHits, key: pageAndHits.pageId }));
    }

    let resultsForText = !this.state.lastRawQuery ? null :
      r.p({ className: 's_SP_SearchedFor' },
        "Results for ",
          r.b({}, r.samp({ id: 'e2eSERP_SearchedFor' }, `"${this.state.lastRawQuery}"`)));

    return (
      r.div({ className: 's_SP container' },
        r.form({},
          r.div({},
            "Search: ",
            r.input({ type: 'text', tabIndex: '1', placeholder: "Text to search for",
                value: query.rawQuery,
                className: 's_SP_QueryTI', onChange: this.onQueryTextEdited }),
            PrimaryButton({ value: "Search", className: 's_SP_SearchB',
                onClick: () => this.search() },
              "Search"),
          advancedSearch),
        anyInfoText,
        resultsForText,
        anyNothingFoundText,
        r.div({ className: 's_SP_SRs' },
          r.ol({},
            resultsList)))));
  }
});



function AdvancedSearchPanel(props: {
      store: Store, query: SearchQuery, onTagsSelectionChange: any }) {
  return (
    r.div({},
      r.div({ className: 'form-group' },
        r.label({ className: 'control-label' }, "With tags:"),
        rb.ReactSelect({ multi: true, value: props.query.tags,
          placeholder: "Select tags",
          options: makeTagLabelValues(props.store.tagsStuff),
          onChange: props.onTagsSelectionChange }))));
}



function makeTagLabelValues(tagsStuff: TagsStuff) {
  if (!tagsStuff || !tagsStuff.tagsAndStats)
    return [];
  return tagsStuff.tagsAndStats.map((tagAndStats: TagAndStats) => {
    return {
      label: tagAndStats.label,
      value: tagAndStats.label,
    };
  });
}



function SearchResultListItem(props: { pageAndHits: PageAndHits, key?: any }) {
  let pageAndHits: PageAndHits = props.pageAndHits;
  let hits = pageAndHits.hits.map(hit =>
      SearchResultHit({ hit: hit, pageId: pageAndHits.pageId, key: hit.postNr }));
  return (
    r.li({ className: 's_SR', key: props.key },
      r.h3({ className: 'esSERP_Hit_PageTitle' },
        r.a({ href: `/-${pageAndHits.pageId}` }, pageAndHits.pageTitle)),
      r.ol({}, hits)));
}



function SearchResultHit(props: { hit: any, pageId: PageId, key?: any }) {
  let hit: SearchHit = props.hit;
  let pageId = props.pageId;
  // Any html stuff was escaped here: [7YK24W].
  let safeHtml = hit.approvedTextWithHighligtsHtml.join(" <b>...</b> ");
  return (
    r.li({ className: 's_SR_Hit', key: props.key },
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


const TagsRegex = /(.* )?tags:([^ ]*) *(.*)/;

function parseSearchQueryInputText(text: string): SearchQuery {
  let tagNameMatchess = text.match(TagsRegex);
  let tags = tagNameMatchess && tagNameMatchess[2] ? tagNameMatchess[2].split(',') : [];
  return {
    rawQuery: text,
    tags: tags,
    // categories:
  };
}


function updateTags(oldQuery: SearchQuery, newTags: string[]): SearchQuery {
  let newRawQuery;
  let spaceAndTags = newTags.length ? ' tags:' + newTags.join(',') : '';
  let matches = oldQuery.rawQuery.match(TagsRegex);
  if (!matches) {
    newRawQuery = oldQuery.rawQuery.trim() + spaceAndTags;
  }
  else {
    newRawQuery = (matches[1] || '').trim() + spaceAndTags + ' ' + (matches[3] || '').trim();
  }
  return _.assign({}, oldQuery, { rawQuery: newRawQuery, tags: newTags });
}

//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=r list
