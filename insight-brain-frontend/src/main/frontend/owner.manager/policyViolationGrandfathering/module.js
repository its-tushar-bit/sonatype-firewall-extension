/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import commonServicesModule from '../../util/CommonServices';
import CLMContextLocationModule from '../../util/CLMContextLocation';
import PolicyViolationGrandfatheringEditor from './policyViolationGrandfatheringEditor';
import PolicyViolationGrandfatheringService from './policyViolationGrandfatheringService';

export default angular
  .module('policyViolationGrandfatheringModule', [CLMContextLocationModule.name, commonServicesModule.name])
  .service('policyViolationGrandfatheringService', PolicyViolationGrandfatheringService)
  .component('policyViolationGrandfatheringEditor', PolicyViolationGrandfatheringEditor);
