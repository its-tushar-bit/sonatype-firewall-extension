/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxGlobalSidebar, useToggle } from '@sonatype/react-shared-components';
import { faArrowToLeft, faBars } from '@fortawesome/pro-regular-svg-icons';

import IqSidebarNavFooter from './IqSidebarNavFooter';

import { isLeftNavigationOpen } from '../../util/preferenceStore';

function DefaultEmptyIqSidebar(props) {
  const [isOpen, toggleOpen] = useToggle(isLeftNavigationOpen());

  const { productEdition, releaseVersion, isShowVersionEnabled } = props;

  return (
    <NxGlobalSidebar
      isOpen={isOpen}
      onToggleClick={toggleOpen}
      toggleOpenIcon={faArrowToLeft}
      toggleCloseIcon={faBars}
      logoAltText={''}
      logoLink={'#'}
      logoImg={''}
    >
      {productEdition && releaseVersion && (
        <IqSidebarNavFooter releaseNumber={releaseVersion} isShowVersionEnabled={isShowVersionEnabled} />
      )}
    </NxGlobalSidebar>
  );
}

DefaultEmptyIqSidebar.propTypes = {
  productEdition: PropTypes.string,
  releaseVersion: PropTypes.string,
  isShowVersionEnabled: PropTypes.bool,
};
export default DefaultEmptyIqSidebar;
