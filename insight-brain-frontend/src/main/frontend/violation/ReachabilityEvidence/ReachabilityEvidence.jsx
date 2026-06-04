/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxAccordion, NxLoadWrapper, NxP } from '@sonatype/react-shared-components';
import EvidencePath from './EvidencePath';
import { actions } from './reachabilityEvidenceSlice';
import { selectReachabilityEvidence } from './reachabilityEvidenceSelectors';
import './_reachabilityEvidence.scss';

/**
 * Reachability Evidence component that displays compressed call paths
 * from entry point to vulnerable method when evidence is available.
 *
 * Evidence is eagerly loaded as part of the violation page data load.
 * Only renders when evidence has paths to show, or when a load error occurred.
 */
export default function ReachabilityEvidence({ reachabilityStatus }) {
  const dispatch = useDispatch();
  const { isOpen, evidence, loading, loadError } = useSelector(selectReachabilityEvidence);

  // Reset Redux state on unmount (component remounts on violation change via key prop)
  React.useEffect(() => {
    return () => dispatch(actions.reset());
  }, [dispatch]);

  // Don't render for non-reachable violations
  if (reachabilityStatus !== 'REACHABLE') {
    return null;
  }

  // Don't render until we know there's something to show:
  // - Show if there's a load error (non-404 server failure)
  // - Show if evidence loaded with paths
  // - Hide if still loading, or if evidence is empty (404 sentinel or kill-switch)
  if (!loadError && (!evidence || !evidence.paths || evidence.paths.length === 0)) {
    return null;
  }

  function handleToggle() {
    dispatch(actions.toggleAccordion());
  }

  return (
    <NxAccordion open={isOpen} onToggle={handleToggle}>
      <NxAccordion.Header>
        <NxAccordion.Title>Reachability Evidence</NxAccordion.Title>
      </NxAccordion.Header>
      <NxLoadWrapper loading={loading} error={loadError}>
        {evidence && (
          <>
            <NxP>
              Showing {(evidence.paths || []).length} {(evidence.paths || []).length === 1 ? 'path' : 'paths'}
              {evidence.truncated && ' (additional paths not shown)'}
            </NxP>
            <ul className="iq-reachability-evidence">
              {(evidence.paths || []).map((path, index) => (
                <EvidencePath key={index} path={path} index={index + 1} />
              ))}
            </ul>
          </>
        )}
      </NxLoadWrapper>
    </NxAccordion>
  );
}

ReachabilityEvidence.propTypes = {
  reachabilityStatus: PropTypes.string,
};
