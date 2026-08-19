/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import { NxTree } from '@sonatype/react-shared-components';

export default function LoadMoreSentinel({ onLoadMore, remainingCount }) {
  const sentinelRef = useRef(null);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel || remainingCount <= 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          onLoadMore();
        }
      },
      {
        threshold: 0,
        rootMargin: '400px',
      }
    );

    observer.observe(sentinel);

    return () => observer.disconnect();
  }, [onLoadMore, remainingCount]);

  if (remainingCount <= 0) return null;

  return (
    <NxTree.Item aria-hidden="true">
      <NxTree.ItemLabel>
        <span ref={sentinelRef} className="iq-original-bom-viewer__sentinel" />
      </NxTree.ItemLabel>
    </NxTree.Item>
  );
}

LoadMoreSentinel.propTypes = {
  onLoadMore: PropTypes.func.isRequired,
  remainingCount: PropTypes.number.isRequired,
};
