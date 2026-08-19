/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useRef } from 'react';

/**
 * track a status, and when the status changes from one state to another, execute the callback
 * example: when the request status goes from 'pending' to 'success' call callback
 *   useStateTransition(request.status, 'pending', 'success', doSomething);
 */
export const useStateTransition = (status, fromStatus, toStatus, callback) => {
  const lastStatusRef = useRef(null);
  useEffect(() => {
    if (lastStatusRef.current === fromStatus) {
      if (status === toStatus) {
        callback();
      }
    }
    lastStatusRef.current = status;
  }, [status, fromStatus, toStatus]);
};
