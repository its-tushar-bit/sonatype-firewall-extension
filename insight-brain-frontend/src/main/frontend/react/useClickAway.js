/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect } from 'react';

const EVENT = 'mousedown';

export default function useClickAway(ref, callback) {
  useEffect(() => {
    const listener = (event) => {
      if (!ref || !ref.current || ref.current.contains(event.target)) {
        return;
      }
      callback(event);
    };

    document.addEventListener(EVENT, listener, true);

    return () => {
      document.removeEventListener(EVENT, listener, true);
    };
  }, [ref, callback]);
}
