/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';

let deferred;
let promise;

const initializePromise = () => {
  promise = new Promise((resolve, reject) => {
    deferred = { resolve, reject };
  });
};

initializePromise();

export function fetchUser(waitForLogin = true) {
  // NOTE: When waiting for login, the http promise might remain unresolved forever if login is cancelled.  A
  // successive attempt to login again should result in a new call to `fetchUser` to get a fresh promise.

  // waitForLogin is passed in as a request configuration here so that axios interceptors can look for it
  // when deciding whether to show the login modal
  axios.get(getSessionUrl(), { waitForLogin }).then(
    function ({ data }) {
      deferred.resolve(data);
    },
    function (error) {
      // 401 means the user is not logged in (and waitForLogin was false), in which case do nothing.
      // Only report other errors
      if (error.response.status !== 401) {
        deferred.reject(error.response);
      }
    }
  );
}

export function waitForLogin() {
  return promise;
}

export async function _resetForTest() {
  initializePromise();
}
