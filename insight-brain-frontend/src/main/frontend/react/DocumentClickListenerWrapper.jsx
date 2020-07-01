/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect } from 'react';

export default function DocumentClickListenerWrapper({onDocumentClick, children}) {
  useEffect(() => {
    document.addEventListener('click', onDocumentClick);
    return function cleanup() {
      document.removeEventListener('click', onDocumentClick);
    };
  });

  return children;
}
