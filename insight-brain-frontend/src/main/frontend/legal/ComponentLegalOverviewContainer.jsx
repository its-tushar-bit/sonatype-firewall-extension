/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { connect } from 'react-redux';
import { pick } from 'ramda';
import ComponentLegalOverviewPage from './ComponentLegalOverviewPage';
import { loadAvailableScopes, loadComponent } from './advancedLegalActions';
import * as copyrightOverrideFormActions from './copyright/copyrightOverrideFormActions';
import { setShowLicenseFilesModal, setShowNoticesModal } from './files/advancedLegalFileActions';

function mapStateToProps({ advancedLegal, router, copyrightOverrides }) {
  let component = advancedLegal.component;
  let availableScopes = advancedLegal.availableScopes;
  return {
    loading: component.loading || availableScopes.loading,
    error: component.error || availableScopes.error,
    availableScopes: availableScopes,
    ...pick(['component', 'licenseLegalMetadata'], component),
    obligations: component.component ? component.component.licenseLegalData.obligations : null,
    showNoticesModal: component.component ? component.component.licenseLegalData.showNoticesModal : false,
    showLicenseFilesModal: component.component ? component.component.licenseLegalData.showLicenseFilesModal : false,
    noticeFiles: component.component ? component.component.licenseLegalData.noticeFiles : null,
    licenseFiles: component.component ? component.component.licenseLegalData.licenseFiles : null,
    ...pick(['hash', 'organizationId', 'applicationPublicId', 'stageTypeId'], router.currentParams),
    ...pick(['showEditCopyrightOverrideModal'], copyrightOverrides),
  };
}

const mapDispatchToProps = {
  loadComponent,
  loadAvailableScopes,
  setShowNoticesModal,
  setShowLicenseFilesModal,
  ...copyrightOverrideFormActions,
};

const ComponentLegalOverviewContainer = connect(mapStateToProps, mapDispatchToProps)(ComponentLegalOverviewPage);
export default ComponentLegalOverviewContainer;
