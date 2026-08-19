/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const requests = [],
  promises = [],
  rejects = [];

/**
 * Adds a deferred request function and its corresponding reject handler to the queue.
 *
 * This is typically used to queue actions (e.g., API requests) that require authentication
 * or some other precondition. When the condition is met, `settleAll()` can be called
 * to execute all queued request functions. If the condition fails (e.g., login is canceled),
 * `rejectAll()` can be used to reject all queued promises.
 *
 * @param {Function} request - A function that returns a Promise when called. This should
 *                             encapsulate the actual request logic (e.g., an API call).
 * @param {Function} reject - A reject callback from the original Promise executor,
 *                            used to reject the promise if the requests are canceled.
 */
export const addRequest = (request, reject) => {
  requests.push(request);
  rejects.push(reject);
};

export const clearRequests = () => {
  requests.length = 0;
  promises.length = 0;
  rejects.length = 0;
};

export const getRequests = () => {
  return requests;
};

export const settleAll = () => {
  requests.forEach(function (request) {
    promises.push(request());
  });
  return Promise.allSettled(promises).then(() => {
    clearRequests();
  });
};

export const rejectAll = () => {
  rejects.forEach((reject) => reject('login cancelled'));
  clearRequests();
};
