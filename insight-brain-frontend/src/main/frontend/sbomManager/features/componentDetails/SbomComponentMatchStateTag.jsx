/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxFontAwesomeIcon, NxTag, NxTooltip } from '@sonatype/react-shared-components';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';

const matchStateMap = new Map([
  ['similar', 'Similar'],
  ['embedded', 'Embedded'],
]);

export default function SbomComponentMatchStateTag({ matchState, filename, ...props }) {
  if (isNilOrEmpty(matchState) || !matchStateMap.has(matchState)) {
    return null;
  }
  const matchStateTextLabel = matchStateMap.get(matchState);

  const tooltipText =
    matchState === 'embedded' ? (
      <span>
        Embedded component match: This component was identified as an OSS constituent inside an uber JAR ({filename}).
      </span>
    ) : (
      <span>
        Original Component Name: {filename}. <br />
        Similar component match: This component is similar to a known open <br />
        source component within your application based on its attributes.
      </span>
    );

  return (
    !isNilOrEmpty(matchStateTextLabel) && (
      <NxTag className="sbom-component-match-state-tag" aria-label={`Match State: ${matchStateTextLabel}`} {...props}>
        Match State: {matchStateTextLabel}{' '}
        <NxTooltip title={tooltipText}>
          <NxFontAwesomeIcon className={'sbom-match-state-info-icon'} icon={faInfoCircle} />
        </NxTooltip>
      </NxTag>
    )
  );
}

SbomComponentMatchStateTag.propTypes = {
  matchState: PropTypes.string,
  filename: PropTypes.string,
};
