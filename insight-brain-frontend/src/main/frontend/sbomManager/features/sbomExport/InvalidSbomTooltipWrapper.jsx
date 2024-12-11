/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTooltip } from '@sonatype/react-shared-components';
import React from 'react';

export default function InvalidSbomTooltipWrapper({ children, isValid, text }) {
  return isValid ? children : <NxTooltip title={text}>{children}</NxTooltip>;
}
