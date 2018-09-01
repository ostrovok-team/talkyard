/*
 * Copyright (c) 2018 Kaj Magnus Lindberg
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

/// <reference path="../slim-bundle.d.ts" />
/// <reference path="../editor-bundle-already-loaded.d.ts" />
/// <reference path="oop-method.staff.ts" />


//------------------------------------------------------------------------------
   namespace debiki2.admin {
//------------------------------------------------------------------------------

const r = ReactDOMFactories;



export const ApiPanel = createFactory({
  displayName: 'ApiPanel',

  getInitialState: function() {
    return {
    };
  },

  componentDidMount: function() {
    Server.loadApiSecrets(secrets => {
      this.setState({ secrets });
    });
  },

  componentWillUnmount: function() {
    this.isGone = true;
  },

  render: function() {
    if (!this.state.secrets)
      return r.p({}, "Loading...");

    const store: Store = this.props.store;

    let elems = this.state.secrets.map((apiSecret: ApiSecret, index: number) => {
      return ApiSecretItem({ secretIndex: index });
    });

    if (!elems.length)
      elems = r.p({ className: 'esAdminSectionIntro e_NoApiSecrets' }, "No API secrets.");

    return (
      r.div({ className: 's_A_Api' },
        elems));
  }
});



const ApiSecretItem = createComponent({
  displayName: 'ApiSecretItem',

  getInitialState: function() {
    return {};
  },

  componentWillUnmount: function() {
    this.isGone = true;
  },

  render: function() {
    return r.p({}, "API Secret here, props: " + JSON.stringify(this.props));
  }
});



//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=r list
