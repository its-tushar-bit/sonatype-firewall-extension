import './cip.scss';

import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'bootstrap2-umd/js/bootstrap-tooltip';
import '../utility/directives/utility.directives.module';
import '../utility/directives/load.wrapper.directive';
import '../utility/directives/form.mask.directive';
import '../utility/directives/submit.validation.directive';
import '../utility/services/utility.services.module';
import '../utility/services/unauthenticated.request.queue.service';
import '../utility/services/login.modal.controller';
import '../utility/services/login.modal.service';
import '../util/Globals';
import '../util/AngularCommon';
import '../util/CommonServices';
import '../util/CLMContextLocation';
import '../util/CLMLocation';
import '../util/HttpInterceptors';
import 'angular-sanitize';
import 'angular-vs-repeat';

// vulnerability.details
import '../audit-report/cip/vulnerability.details/vulnerability.details.module';
import '../audit-report/cip/vulnerability.details/vulnerability.details.modal.controller';
import '../audit-report/cip/vulnerability.details/vulnerability.details.service';

// cip-version-graph
import './cip.version.graph/cip.version.graph.module';
import '../version-graph/version-graph-index';

// cip-label-editor
import './cip.label.editor/cip.label.editor.module';
import './cip.label.editor/current.label.data.service';
import './cip.label.editor/label.add.controller';
import './cip.label.editor/label.remove.controller';
import './cip.label.editor/labels.controller';
import './cip.label.editor/spinner.directive';
import './cip.label.editor/cip.label.editor.directive';
import './cip.label.editor/label.modification.service';

// cip-license-editor
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

// cip.vulnerability.editor
import '../audit-report/cip/cip.vulnerability.editor/index';
// cip-claim-component
import 'bootstrap-datepicker';
import './cip-claim-component';
