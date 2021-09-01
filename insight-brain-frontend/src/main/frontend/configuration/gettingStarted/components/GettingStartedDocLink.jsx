/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTextLink } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { submitData, LINK_CLICKED_ACTION } from '../gettingStartedTelemetryServiceHelper';

export default function GettingStartedDocLink({ href, text }) {
  return (
    <NxTextLink external href={href} onClick={() => submitData(LINK_CLICKED_ACTION, { href })}>
      {text}
    </NxTextLink>
  );
}

GettingStartedDocLink.propTypes = {
  href: PropTypes.string,
  text: PropTypes.string,
};
