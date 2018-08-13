/**
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


//------------------------------------------------------------------------------
   namespace debiki2.users {
//------------------------------------------------------------------------------

const r = ReactDOMFactories;


export const UserDrafts = createFactory({
  displayName: 'UserDrafts',

  getInitialState: function() {
    return { drafts: null, error: false };
  },

  componentDidMount: function() {
    const user: MemberInclDetails = this.props.user;
    this.listDrafts(user.id);
  },

  componentWillUnmount: function() {
    this.isGone = true;
  },

  componentWillReceiveProps: function(nextProps: any) {
    // Dupl code, also in view notfs. [7WUBKZ0]
    const me: Myself = this.props.store.me;
    const user: MemberInclDetails = this.props.user;
    const nextLoggedInUser: Myself = nextProps.store.me;
    const nextUser: MemberInclDetails = nextProps.user;
    if (me.id !== nextLoggedInUser.id ||
        user.id !== nextUser.id) {
      this.listDrafts(nextUser.id);
    }
  },

  listDrafts: function(userId: UserId) {
    // Dupl code, also in view notfs. [7WUBKZ0]
    const me: Myself = this.props.store.me;
    if (me.id !== userId && !isStaff(me)) {
      this.setState({
        error: "May not list an other user's drafts. [TyE5ARBK2]",
        drafts: null,
      });
      return;
    }
    Server.listDrafts(userId, (drafts: Draft[]) => {
      if (this.isGone) return;
      this.setState({ drafts });
    }, () => {
      // Clear state.notfs, in case we're no longer allowed to view the drafts.
      this.setState({ error: true, drafts: null });
    });
  },

  render: function() {
    // Dupl code, also in view notfs. [7WUBKZ0]
    if (this.state.error)
      return (
        r.p({ className: 'e_UP_Notfs_Err' },
          _.isString(this.state.error) ? this.state.error : "Error [EsE7YKW2]."));

    const drafts: Draft[] = this.state.drafts;

    if (!drafts)
      return r.p({}, t.Loading);

    const user: MemberInclDetails = this.props.user;
    const store: Store = this.props.store;
    const me: Myself = store.me;
    const isMe = user.id === me.id;

    const anyNoDraftsMessage = drafts.length ? null :
        r.p({ className: 'e_UP_Drfts_None' }, "No drafts");  // I18N

    const draftElems = drafts.map((draft: Draft) =>
        r.li({ key: draft.draftNr },
          Link({ to: linkToDraftSource(draft) },
            Draft({ draft, verbose: true }))));

    return (
      r.div({},
        r.p({}, isMe ? t.upp.NotfsToYouC : t.upp.NotfsToOtherC(user.username || user.fullName)),
        anyNoDraftsMessage,
        r.ol({ className: 'esNotfs' },
          draftElems)));
  }
});


function Draft(props: { draft: Draft, verbose: boolean }) {
  return r.p({}, JSON.stringify(props.draft));
}

//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=r list
