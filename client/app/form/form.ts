/*
 * Copyright (c) 2016-2017 Kaj Magnus Lindberg
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
/// <reference path="../../typedefs/jquery/jquery.d.ts" />
/// <reference path="../plain-old-javascript.d.ts" />
/// <reference path="../prelude.ts" />
/// <reference path="../rules.ts" />
/// <reference path="../Server.ts" />
/// <reference path="../page-methods.ts" />
/// <reference path="../more-bundle-not-yet-loaded.ts" />

//------------------------------------------------------------------------------
   namespace debiki2.form {
//------------------------------------------------------------------------------


export function activateAnyCustomForm() {
  const forms = $bySelector('.dw-p-bd form');
  for (let i = 0; i < forms.length; ++i) {
    const form = <HTMLFormElement> forms[i];
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      event.stopPropagation();

      // let namesAndValues = $form.serializeArray();

      const data = new FormData(form);

      //const req = new XMLHttpRequest();
      //req.send(data);

      const doWhat = <HTMLInputElement> form.querySelector('input[name="doWhat"]');
      if (doWhat) {
        if (doWhat.value === 'CreateTopic') {
          die('unimpl [EdE2WKP05YU]');
          // Server.submitCustomFormAsNewTopic(data);
          // Instead: change submitCustomFormAsNewTopic() signature to: {
          //  newTopicTitle: string,
          //  newTopicBody: string,
          //  pageTypeId: string,
          //  categorySlug: string,
          // }  and use querySelector...value to get the values.
        }
        else if (doWhat.value === 'SignUp') {
          morebundle.loginIfNeeded(LoginReason.SignUp);
        }
        else {
          die(`Unknown input name=doWhat value: '${doWhat.value}' [EdE8402F4]`);
        }
      }
      else {
        Server.submitCustomFormAsJsonReply(data, function() {
           // This messes with stuff rendered by React, but works fine nevertheless.
           const thanks = form.querySelector('.FormThanks');
           const replacement = thanks || $h.parseHtml('<p class="esFormThanks">Thank you.</p>')[0];
           form.parentNode.insertBefore(replacement, form);
           form.remove();
        });
        const submitButton = form.querySelector('button[type=submit]');
        if (submitButton) {
          submitButton.textContent = "Submitting ...";
          submitButton.setAttribute('disabled', 'disabled');
        }
      }
    });
  }
}

//------------------------------------------------------------------------------
   }
//------------------------------------------------------------------------------
// vim: fdm=marker et ts=2 sw=2 tw=0 fo=r list
