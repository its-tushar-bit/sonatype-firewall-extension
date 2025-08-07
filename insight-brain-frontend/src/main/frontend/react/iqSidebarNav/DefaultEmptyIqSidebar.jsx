/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxGlobalSidebar2, useToggle } from '@sonatype/react-shared-components';
import { faArrowToLeft, faArrowToRight } from '@fortawesome/pro-regular-svg-icons';

import { isLeftNavigationOpen } from '../../util/preferenceStore';

function DefaultEmptyIqSidebar() {
  const [isOpen, toggleOpen] = useToggle(isLeftNavigationOpen());

  return (
    <NxGlobalSidebar2
      isOpen={isOpen}
      onToggleClick={toggleOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faArrowToRight}
      className="iq-lifecycle-sidebar"
    />
  );
}

DefaultEmptyIqSidebar.propTypes = {
  productEdition: PropTypes.string,
  releaseVersion: PropTypes.string,
  isShowVersionEnabled: PropTypes.bool,
};
export default DefaultEmptyIqSidebar;
