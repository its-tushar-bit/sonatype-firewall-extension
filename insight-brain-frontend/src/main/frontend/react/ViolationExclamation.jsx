/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

import { faExclamationCircle } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { allThreatLevelCategories } from '@sonatype/react-shared-components/util/threatLevels';

export default function ViolationExclamation({ threatLevelCategory }) {
  const iconClassName = classnames('iq-violation-exclamation', `iq-violation-exclamation--${threatLevelCategory}`);

  return <NxFontAwesomeIcon className={iconClassName} icon={faExclamationCircle} />;
}

ViolationExclamation.propTypes = {
  threatLevelCategory: PropTypes.oneOf([...allThreatLevelCategories, 'disabled']).isRequired,
};
