import '../lib/bootstrap/bootstrap-variables.scss';
import '../lib/bootstrap-sass-official/vendor/assets/stylesheets/bootstrap/_mixins.scss';
import '../lib/bootstrap-sass-official/vendor/assets/stylesheets/bootstrap/_component-animations.scss';
import '../lib/bootstrap-sass-official/vendor/assets/stylesheets/bootstrap/_tooltip.scss';
import '../scss/_variables.scss';
import '../scss/_typography.scss';
import '../scss/_global.scss';
import '../scss/_clm-modal.scss';
import '../scss/_mask.scss';

import '../lib/angular-ui-router-0.2.15.min';
import '../lib/bootstrap-sass-official/vendor/assets/javascripts/bootstrap-tooltip';
import '../utility/directives/utility.directives.module';
import '../utility/directives/load.wrapper.directive';
import '../utility/directives/form.mask.directive';
import '../utility/directives/submit.validation.directive';
import '../utility/services/utility.services.module';
import '../utility/services/unauthenticated.request.queue.service';
import '../utility/services/login.modal.controller';
import '../utility/services/login.modal.service';
import {messageTemplate, AngularUtils, AngularStateUtils} from '../util/Globals';
import '../util/AngularCommon';
import '../util/CommonServices';
import '../util/CLMAppLocation';
import '../util/CLMLocation';
import '../util/HttpInterceptors';
import '../lib/angular-1.5.8/angular-sanitize';
import '../lib/angular-vs-repeat/src/angular-vs-repeat';

// vulnerability.details
import '../audit-report/cip/vulnerability.details/vulnerability.details.module';
import '../audit-report/cip/vulnerability.details/vulnerability.details.modal.controller';
import '../audit-report/cip/vulnerability.details/vulnerability.details.service';

// cip-version-graph
import './cip.version.graph/cip.version.graph.module';
import '../version-graph/version-graph-index';

// cip-label-editor
import './cip.label.editor/cip-label-editor.css';
import './cip.label.editor/cip.label.editor.module';
import './cip.label.editor/current.label.data.service';
import './cip.label.editor/label.add.controller';
import './cip.label.editor/label.remove.controller';
import './cip.label.editor/labels.controller';
import './cip.label.editor/spinner.directive';
import './cip.label.editor/tip.directive';
import './cip.label.editor/cip.label.editor.directive';
import './cip.label.editor/label.modification.service';

// cip-license-editor
import '../lib/bootstrap/bootstrap-variables.scss';
import '../lib/bootstrap-sass-official/vendor/assets/stylesheets/bootstrap/_mixins.scss';
import '../lib/bootstrap-sass-official/vendor/assets/stylesheets/bootstrap/_dropdowns.scss';
import '../scss/_widgets.scss';
import './cip.license.editor/cip-license-editor.css';

import './cip.license.editor/cip.license.editor.module';
import './cip.license.editor/cip.license.editor.directive';
import './cip.license.editor/license.editor.controller';

// cip-policy-violations
import './cip.policy.violations/cip.policy.violations.module';
import './cip.policy.violations/add.waiver.controller';
import './cip.policy.violations/cip.policy.violations.directive';
import './cip.policy.violations/policy.violations.controller';
import './cip.policy.violations/view.waiver.controller';
import './cip.policy.violations/release.quarantine.controller';

import './cip.policy.violations/cip-policy-violations.css';

// cip.vulnerability.editor
import '../audit-report/cip/cip.vulnerability.editor/index';
import './cip.vulnerability.editor/cip.vulnerability.editor.scss';

// cip-claim-component
import '../lib/datepicker/datepicker.css';
import '../lib/datepicker/bootstrap-datepicker';
import './cip-claim-component.css';
import './cip-claim-component';

window.messageTemplate = messageTemplate;
window.AngularUtils = AngularUtils;
window.AngularStateUtils = AngularStateUtils;
