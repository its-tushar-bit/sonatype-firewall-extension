/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, {Fragment} from 'react';
import LoadWrapper from '../../../react/LoadWrapper';
import * as PropTypes from 'prop-types';
import ownerConstant from '../../../utility/services/owner.constant';

function CredentialsError({error, selectedOrganization, $state}) {

  if (!error) {
    return null;
  }

  function getSourceControlIdToUpdate() {
    if (!selectedOrganization || selectedOrganization.sourceControl.token.value === null) {
      return ownerConstant.ROOT_ORGANIZATION_ID;
    }
    else {
      return selectedOrganization.organization.id;
    }
  }

  const scmConfigurationHref = $state.href('management.edit.organization.edit-source-control',
      { organizationId: getSourceControlIdToUpdate()});

  return (
    <Fragment>
      {error.message}. You can update your login credentials <a href={scmConfigurationHref}>here</a>.
    </Fragment>
  );
}

CredentialsError.propTypes = {
  error: LoadWrapper.propTypes.error.isRequired,
  selectedOrganization: PropTypes.object,
  $state: PropTypes.object.isRequired
};

export default CredentialsError;
