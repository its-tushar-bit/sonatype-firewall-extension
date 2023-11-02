/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import { NxTextLink } from '@sonatype/react-shared-components';
import ownerConstant from '../../../utility/services/owner.constant';
import { displayName } from '../utils/providers';

function CredentialsError(props) {
  const { inLoadWrapper, errorCode, hostUrlClicked, selectedOrganization, scmProvider, $state } = props;

  if (!errorCode) {
    return null;
  }

  function getSourceControlIdToUpdate() {
    if (!selectedOrganization || selectedOrganization.sourceControl.token.value === null) {
      return ownerConstant.ROOT_ORGANIZATION_ID;
    } else {
      return selectedOrganization.organization.id;
    }
  }

  const scmConfigurationHref = $state.href('management.edit.organization.edit-source-control', {
    organizationId: getSourceControlIdToUpdate(),
  });

  const ERROR_SHORT_DESC = {
    SCM_AUTHN_FAILURE: 'Authentication Error',
    SCM_AUTHZ_FAILURE: 'Authorization Error',
    SCM_UNKNOWN_HOST_FAILURE: 'Unknown Host Error',
  };
  const errorShortDescription = () => {
    const description = ERROR_SHORT_DESC[errorCode];
    return description ? description : '';
  };

  const ERROR_DETAIL_DESC = {
    SCM_AUTHN_FAILURE: 'IQ Server was unable to authenticate with',
  };
  const errorDetailDescription = () => {
    let description = ERROR_DETAIL_DESC[errorCode];
    description = description !== undefined ? description : 'IQ Server was unable to connect to';
    return (
      `${description} ${displayName(scmProvider)} using the credentials associated with the ` +
      `${selectedOrganization.organization.name} Organization.`
    );
  };

  const hostUrlBlock = () => {
    if (hostUrlClicked !== undefined) {
      return <NxTextLink onClick={hostUrlClicked}>different host URL</NxTextLink>;
    }
    return 'different host URL';
  };

  return (
    <Fragment>
      {inLoadWrapper ? (
        <span>Due to an {errorShortDescription()}, </span>
      ) : (
        <strong>{errorShortDescription()}. </strong>
      )}
      {errorDetailDescription()} You may try a {hostUrlBlock()} or manage your SCM configuration in the{' '}
      <NxTextLink href={scmConfigurationHref}>Orgs & Policies</NxTextLink> page.
    </Fragment>
  );
}

CredentialsError.propTypes = {
  inLoadWrapper: PropTypes.bool,
  errorCode: PropTypes.string.isRequired,
  selectedOrganization: PropTypes.object,
  scmProvider: PropTypes.string,
  hostUrlClicked: PropTypes.func,
  $state: PropTypes.object.isRequired,
};

export default CredentialsError;
