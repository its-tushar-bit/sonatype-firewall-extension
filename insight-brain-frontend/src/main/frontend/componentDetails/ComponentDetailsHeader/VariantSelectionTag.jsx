/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useId } from 'react';
import * as PropTypes from 'prop-types';

import { NxFontAwesomeIcon, NxTag, NxTooltip } from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';

const DESCRIPTION =
  'This dependency matched multiple Python package variants, so Lifecycle selected one representative ' +
  'package variant to reduce duplicate results.';

// Screen-reader-only: a persistent element so aria-describedby always resolves (the NxTooltip popup is only in
// the DOM while open, so it cannot be the describedby target).
const visuallyHidden = {
  position: 'absolute',
  width: '1px',
  height: '1px',
  padding: 0,
  margin: '-1px',
  overflow: 'hidden',
  clip: 'rect(0, 0, 0, 0)',
  whiteSpace: 'nowrap',
  border: 0,
};

/**
 * Pill shown on the component details page when a dependency matched multiple Python package variants and
 * Lifecycle selected one representative variant to report instead of every platform-specific variant.
 */
export default function VariantSelectionTag({ variantSelected, ...props }) {
  const descriptionId = useId();

  if (!variantSelected) {
    return null;
  }

  return (
    <>
      <NxTag
        className="variant-selection-tag"
        aria-label="Representative variant selected"
        aria-describedby={descriptionId}
        {...props}
      >
        Representative variant selected{' '}
        <NxTooltip title={DESCRIPTION}>
          <NxFontAwesomeIcon className="variant-selection-info-icon" icon={faInfoCircle} tabIndex={0} />
        </NxTooltip>
      </NxTag>
      {/* Outside NxTag: nesting it inside makes NxTag's NxOverflowTooltip duplicate this text as a second pill-wide popup. */}
      <span id={descriptionId} style={visuallyHidden}>
        {DESCRIPTION}
      </span>
    </>
  );
}

VariantSelectionTag.propTypes = {
  variantSelected: PropTypes.bool,
};
