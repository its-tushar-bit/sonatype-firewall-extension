/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useRef } from 'react';

export default function useConditionalAutoFocus(condition) {
  const ref = useRef(null);

  useEffect(() => {
    // This is needed because some times the ref takes some time to be attached to it's element
    setTimeout(() => {
      if (condition && ref?.current) {
        ref.current.focus();
      }
    }, 0);
  }, [condition, ref]);

  return ref;
}
