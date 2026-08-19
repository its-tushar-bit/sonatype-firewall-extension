/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

/*
 The "Go to Reports" Call To Action
 */
export default function ReportsCta({ id }) {
  const $state = useRouterState();

  const goToReports = () => {
    $state.go('violations');
  };

  return (
    <NxButton id={id} type="button" variant="secondary" onClick={goToReports}>
      Go To Reports
    </NxButton>
  );
}

ReportsCta.propTypes = {
  id: PropTypes.string.isRequired,
};
