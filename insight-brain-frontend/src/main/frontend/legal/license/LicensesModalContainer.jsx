/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import LicensesModal from './LicensesModal';
import { loadLicenseModalInformation, saveLicenses, setShowLicensesModal } from '../files/advancedLegalFileActions';
import { pick } from 'ramda';

const mapDispatchToProps = {
  setShowLicensesModal,
  saveLicenses,
  loadLicenseModalInformation,
};

function mapStateToProps({ advancedLegal, router }) {
  const component = advancedLegal.component;
  const { applicationPublicId, organizationId, hash } = router.currentParams;
  return {
    availableScopes: advancedLegal.availableScopes,
    ownerId: applicationPublicId || organizationId,
    hash,
    ...pick(['component', 'licenseLegalMetadata'], component),
  };
}

const LicensesModalContainer = connect(mapStateToProps, mapDispatchToProps)(LicensesModal);
export default LicensesModalContainer;
