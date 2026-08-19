/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { faCube } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';

export default function ArtifactNameDisplay({ artifactName }) {
  return (
    <Fragment>
      <NxFontAwesomeIcon icon={faCube} />
      <span>{artifactName}</span>
    </Fragment>
  );
}

ArtifactNameDisplay.propTypes = {
  artifactName: PropTypes.string.isRequired,
};
