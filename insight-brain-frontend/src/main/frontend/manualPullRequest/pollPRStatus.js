/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getPullRequestStatusUrl } from 'MainRoot/util/CLMLocation';

const POLLING_FREQUENCY = 800;

const PR_STATUS = {
  PULL_REQUEST_CREATION_PENDING: 'PULL_REQUEST_CREATION_PENDING',
};

/**
 * Polls the status of a pull request creation process every {@link POLLING_FREQUENCY} milliseconds.
 * Continues polling until the status is no longer `PULL_REQUEST_CREATION_PENDING`.
 * The promise is rejected when the abort signal is triggered.
 *
 * @param {string} id - The id of the pull request creation process.
 * @param {AbortSignal} signal - Optional abort signal to cancel polling externally.
 * @returns {Promise<Object>} - Resolves with the final status of the pull request.
 */
export const pollPRStatus = async (id, signal) => {
  const poll = async () => {
    for (;;) {
      if (signal.aborted) {
        throw new Error('Polling aborted');
      }

      try {
        const url = getPullRequestStatusUrl(id);
        const response = await axios.get(url, { signal });
        if (response.data.status !== PR_STATUS.PULL_REQUEST_CREATION_PENDING) {
          return response.data;
        }
      } catch (error) {
        if (signal.aborted || axios.isCancel(error)) {
          throw new Error('Polling aborted');
        }
        console.error('Polling error:', error);
      }
      await new Promise((resolve) => setTimeout(resolve, POLLING_FREQUENCY));
    }
  };

  return poll();
};
