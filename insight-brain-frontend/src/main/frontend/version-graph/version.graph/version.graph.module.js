/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import coordinatesService from './coordinates.service';
import propertiesService from './properties.service';
import informationPanelDirective from './information.panel.directive';
import cipController from './cip.controller';
import componentController from './component.controller';
import errorMessageFactory from './error.message.factory';
import statusController from './status.controller';
import stateService from './state.service';
import detailsController from './details.controller';
import namePartFilter from './name.part.filter';
import graphDirective from './graph.directive';
import licensesDirective from './licenses.directive';
import applicationsService from './applications.service';
import applicationController from './application.controller';
import agoLastDayFilter from './ago.last.day.filter';
import agoFilter from './ago.filter';

export default angular
  .module('version.graph', [])
  .service('Coordinates', coordinatesService)
  .service('Properties', propertiesService)
  .directive('informationPanel', informationPanelDirective)
  .controller('CIPController', cipController)
  .controller('ComponentController', componentController)
  .factory('ErrorMessage', errorMessageFactory)
  .controller('StatusController', statusController)
  .service('State', stateService)
  .controller('DetailsController', detailsController)
  .filter('namePart', namePartFilter)
  .directive('graph', graphDirective)
  .directive('licenses', licensesDirective)
  .service('Applications', applicationsService)
  .controller('ApplicationController', applicationController)
  .filter('agoLastDay', agoLastDayFilter)
  .filter('ago', agoFilter);
