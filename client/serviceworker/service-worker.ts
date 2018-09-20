// Docs: https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API
// Read?: https://www.kollegorna.se/en/2017/06/service-worker-gotchas/
// Caching, see:
// https://developers.google.com/web/ilt/pwa/caching-files-with-service-worker
// https://github.com/mdn/sw-test/blob/gh-pages/sw.js


console.log("Service worker v0.0.3 loading [TyMSWVLDNG]");

declare var oninstall: any;
declare var onfetch: any;
declare var skipWaiting: any;

let longPollingReqNr = 0;
let longPollingPromise = null;

let totalReqNr = 0;
let numActive = 0;

oninstall = function(event) {
  console.log("Service worker installed [TyMSWINSTLD]");
  skipWaiting();
};

onfetch = function(event: any) {
  totalReqNr += 1;
  const reqNr = totalReqNr;
  const url = event.request.url;

  // Let all browser tabs share the same long polling request.
  if (url.indexOf('/-/pubsub/subscribe/') >= 0) {  // sync path with nginx [7BKXA02]
    if (longPollingPromise) {
      console.log(`sw lp ${longPollingReqNr} reuse, ${numActive} active: ${url} [TyMSWLPREUSE]`);
    }
    else {
      numActive += 1;
      longPollingReqNr += 1;
      console.log(`sw lp ${longPollingReqNr} req, req ${reqNr}, ${numActive} active: ${url} [TyMSWLPREQ]`);
      longPollingPromise = fetch(event.request).then(function() {
        console.log(`sw lp ${longPollingReqNr} rsp, req ${reqNr}, ${numActive} active: ${url} [TyMSWLPRSP]`);
      }).catch(function() {
        console.log(`sw lp ${longPollingReqNr} ERR, req ${reqNr}, ${numActive} active: ${url} [TyMSWLPERR]`);
      }).finally(function() {
        longPollingPromise = null;
        numActive -= 1;
      });
    }
    return longPollingPromise;
  }
  else {
    numActive += 1;
    console.log(`sw req ${reqNr}, ${numActive} active: ${url} [TyMSWREQ]`);
    return fetch(event.request).finally(function() {
      numActive -= 1;
      console.log(`sw rsp ${reqNr}, ${numActive} active: ${url} [TyMSWRSP]`);
    });
  }
};
