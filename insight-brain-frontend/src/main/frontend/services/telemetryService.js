/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import angular from 'angular';
import { submitTelemetryData } from '../util/telemetryUtils';

export default angular.module('telemetryServiceModule', []).service('telemetryService', function telemetryService() {
  return {
    submitData: submitTelemetryData,
  };
});
