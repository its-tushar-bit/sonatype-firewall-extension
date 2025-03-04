/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import ComponentLicenseDetailsPage from './ComponentLicenseDetailsPage';
import { pick } from 'ramda';
import { loadComponentAndLicenseDetails } from './componentLicenseDetailsActions';
import { setShowLicensesModal } from '../files/advancedLegalFileActions';

function mapStateToProps({ advancedLegal, componentLicenseDetails, router }) {
  const component = advancedLegal.component || {};
  const availableScopes = advancedLegal.availableScopes || {};
  const licenseLegalMetadata = advancedLegal.component.licenseLegalMetadata || [];
  return {
    loading: component.loading || availableScopes.loading,
    error: component.error || availableScopes.error,
    availableScopes,
    componentLicenseDetails,
    licenseLegalMetadata,
    ...pick(['component'], component),
    ...pick(
      ['hash', 'componentIdentifier', 'ownerType', 'ownerId', 'licenseIndex', 'stageTypeId', 'scanId'],
      router.currentParams
    ),
  };
}

const mapDispatchToProps = {
  loadComponentAndLicenseDetails,
  setShowLicensesModal,
};

const ComponentLicenseDetailsContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentLicenseDetailsPage);
export default ComponentLicenseDetailsContainer;
