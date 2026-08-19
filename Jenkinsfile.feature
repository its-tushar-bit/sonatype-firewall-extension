/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _

/**
 This feature branch Jenkinsfile is intended for validation rather than producing releasable artifacts. It distributes heavy tests
 across multiple agents to maximize parallelization.

 All of these optimizations can be toggled using Build with Parameters.

 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  1. INITIALIZE                                                                               │
 │     • Set env vars                                                                           │
 │     • Configure MAVEN_CMD                                                                    │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  2. PREPARE                                                                                 │
 │     • Install Node/Yarn BINARIES only                                                       │
 │     • install-node-and-yarn                                                                 │
 │     • yarn install runs phase-bound during the parallel Build                               │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  3. BUILD                                                                                    │
 │     • install -DskipTests                                                                    │
 │     • Uses -T 1C for parallel builds                                                      │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  4. STASH TEST ARTIFACTS                                                                     │
 │     • Stash SNAPSHOT JARs + ALL modules' test-classes + root jvm.options for distributed agents │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 │  5. TEST                                                               ║ PARALLEL ║          │
 ├─────────────────────────┬───────────────────────┬───────────────────────────────────────────┤
 │  Frontend Tests         │  Static Analysis      │  Policy Evaluation (conditional)          │
 │  • main agent • Jest    │  • agent: iq          │  • nexusPolicyEvaluation                  │
 ├─────────────────────────┴───────────────────────┴───────────────────────────────────────────┤
 │  MODULE PARTITIONS - each on its own iq-large agent (deps come from Build's install):         │
 │    • Fast Module Tests   -T 4  reactor: db/policy/common/event/tenancy/guide/*-db-pg/mtiq-fips │
 │    • Service Tests       insight-brain-service                                                 │
 │    • MTIQ Server Tests   nexus-mtiq-server                                                     │
 │    • Data Tests          insight-brain-data                                                    │
 │    • Component H2 Tests  variant-test-component-h2                                             │
 │    • Component PG Tests  variant-test-component-pg                                             │
 │    • Variant H2 + PG     variant-test-h2 + -pg (overlapped -T 2)                               │
 │    • Variant MTIQ + FIPS variant-test-mtiq + -fips (overlapped -T 2)                           │
 │    • Legacy A-E/F-Q/R-Z  variant-test-legacy (reuseForks=false, 3-way class-name split)        │
 ├───────────────────────────────────────────────────────────────────────────────────────────────┤
 │  Playwright Functional (-Psanity) + Playwright Regr 1/2/3 (-Pregression, opt-out)             │
 └──────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌───────────────────────────────────────────────────────────────────────────────────────────────┐
 │  6. COLLECT RESULTS                                                                           │
 │     • junit: Collect test reports (surefire, failsafe, jest)                                  │
 │     • archiveArtifacts: Test report files                                                     │
 └───────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌───────────────────────────────────────────────────────────────────────────────────────────────┐
 │  7. BUILD MTIQ IMAGE                                          ║ CONDITIONAL ║                 │
 │     When: mtiqImagePushEnabled OR *_mtiq job                                                  │
 │     • Build Docker image                                                                      │
 │     • Push to RSC registry                                                                    │
 └───────────────────────────────────────────────────────────────────────────────────────────────┘
 │
 ▼
 ┌───────────────────────────────────────────────────────────────────────────────────────────────┐
 │  POST ACTIONS                                                                                 │
 ├───────────────────────────────────────────────────────────────────────────────────────────────┤
 │  always:                                                                                      │
 │    • Echo "Pipeline completed"                                                                │
 └───────────────────────────────────────────────────────────────────────────────────────────────┘


 ╔═══════════════════════════════════════════════════════════════════════════════════════════════╗
 ║  LEGEND                                                                                       ║
 ╠═══════════════════════════════════════════════════════════════════════════════════════════════╣
 ║  -T 1C       = 100% of CPU cores (16 threads on 16-core)                                      ║
 ║  -T 2        = 2 Maven threads                                                                ║
 ║  FE          = insight-brain-frontend                                                         ║
 ║  MTIQ        = nexus-mtiq-server (Multi-Tenant IQ)                                            ║
 ║  agent: iq-large = Runs on separate distributed agent                                          ║
 ╚═══════════════════════════════════════════════════════════════════════════════════════════════╝
 */

// moby/buildkit v0.31.2. Pinned by digest because Firewall quarantines every moby/buildkit tag; a digest
// ref is not tag-quarantined.
buildkitImageDigest = 'sha256:2f5adac4ecd194d9f8c10b7b5d7bceb5186853db1b26e5abd3a657af0b7e26ec'

// IQ application the MTIQ container image is evaluated against. Separate from the 'insight-brain'
// application used by the source scan so the image's OS violations stay out of the source report.
mtiqImageIqApplication = 'docker-nexus-iq-server-mtiq'

// One named buildx builder per agent, shared by the scan build and the RSC push so they share a
// layer cache. buildx create without a name makes a new builder — and a cold cache — on every call,
// so with both mtiqImagePolicyEvaluationEnabled and mtiqImagePushEnabled set the push would
// otherwise rebuild from scratch what the scan just built.
mtiqBuildxBuilder = 'mtiq-buildx'

// Configure build parameters and job properties
configureBranchJob()

// Agent label for distributed test stages
// Instance type here: https://github.com/sonatype/bnr-jenkins-casc/blob/1ba07ca6a0576a8fa5ed0ff672a7b5c42587fdc2/clouds/zion.yaml#L242
def DISTRIBUTED_TEST_AGENT = 'iq-large'

pipeline {
  agent {
    label 'iq-large'
  }

  options {
    timestamps()
    buildDiscarder(logRotator(
        numToKeepStr: '10'
    ))
    // Increase timeout for Jakarta EE 11 migration branch - tests take longer due to framework changes
    timeout(time: 150, unit: 'MINUTES')
  }

  stages {
    stage('Initialize') {
      steps {
        script {

          echo "Pipeline Configuration:"
          echo "  Branch: ${env.BRANCH_NAME ?: gitBranch(env)}"
          echo "  Bundling enabled: ${isBundlingEnabled()}"

          env.BUILD_DIR = env.WORKSPACE

        }
      }
    }

    stage('Prepare: Install Node/Yarn') {
      steps {
        script {
          dir(env.BUILD_DIR) {
              // Install node/yarn BINARIES only (serial, ~40s, avoids any -T race on node install).
              // The node_modules install (yarn-install) is intentionally NOT run here: the frontend
              // module's own phase-bound yarn-install execution runs it during the parallel -T Build,
              // where it overlaps the backend compile that gates the Build (frontend is not the Build
              // critical path). This keeps ~110s of yarn install off the serial prefix on every build.
              mvn getFrontEndInstallConfig(),
                  'com.github.eirslett:frontend-maven-plugin:install-node-and-yarn@install-node-and-yarn'
          }
        }
      }
    }

    stage('Build') {
      steps {
        script {
          dir(env.BUILD_DIR) {
            withSonatypeDockerRegistry() {
              withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
                       "TESTCONTAINERS_RYUK_DISABLED=true"]) {
                // 'clean' is needed because feature-branch workspaces persist across force-pushes;
                // without it stale resources from prior tips (e.g. renamed migration files) remain in target/.
                mvn getMavenBuildConfig(), 'clean install'

                // Stash immediately after build (eliminates separate stage overhead)
                stashTestArtifacts()
              }
            }
          }
        }
      }
    }

    // Stash Test Artifacts stage removed - now integrated into Build stage above

    stage('Test') {
      parallel {
        // Frontend tests on main agent (benefits from cached node/yarn)
        stage('Frontend Tests') {
          steps {
            script {
              dir(env.BUILD_DIR) {
                // Frontend lint (relocated off the serial Build critical path) + Jest tests,
                // using cached node/yarn from the Prepare stage.
                mvn getFrontEndTestConfig(),
                    'com.github.eirslett:frontend-maven-plugin:yarn@ci-lint ' +
                    'com.github.eirslett:frontend-maven-plugin:yarn@jest'
              }
            }
          }
        }

        stage('Policy Evaluation') {
          when {
            expression { shouldRunPolicyEvaluation() }
          }
          steps {
            script {
              dir(env.BUILD_DIR) {
                def reachabilityConfig = [
                    javaAnalysis: [
                        enable: true,
                        includes: [
                            [pattern: 'nexus-iq-server/target/insight-brain-service-*.jar']
                        ],
                        entrypointStrategy: 'JAVA_MAIN',
                        namespaces: [
                            [namespace: 'com.sonatype.insight']
                        ]
                    ],
                    jsAnalysis: [
                        enable: true,
                        node: [
                            executable: "${env.WORKSPACE}/insight-brain-frontend/node/node"
                        ],
                        projectDirectory: 'insight-brain-frontend',
                        sourceFiles: [
                            [pattern: 'src/main/**']
                        ],
                        excludeFiles: [
                            [pattern: 'src/test/**']
                        ]
                    ]
                ]
                nexusPolicyEvaluation(
                    iqStage: 'develop',
                    iqApplication: 'insight-brain',
                    iqScanPatterns: [[scanPattern: 'insight-brain-frontend/target/webpack-modules']],
                    iqModuleExcludes: [[moduleExclude: '**/test/**'], [moduleExclude: '**/test-classes/**/module.xml']],
                    failBuildOnNetworkError: true,
                    reachability: reachabilityConfig,
                    unstableBuildOnScanningWarnings: false
                )
              }
            }
          }
        }

        // Static analysis on smaller distributed agent
        stage('Static Analysis') {
          agent { label 'iq' }
          steps {
            script {
              runDistributedStaticAnalysis()
            }
          }
          post {
            failure {
              script {
                // Ensure static analysis failures fail the entire build
                currentBuild.result = 'FAILURE'
                error('Static analysis (Spotless/License) failed - see logs for violations')
              }
            }
          }
        }

        // Backend tests are split by MODULE, not by class-name letter. Each variant-test module boots
        // its fixture once per module JVM (reuseForks per the module pom), so a class-name split across
        // agents would re-boot that fixture on every agent and mix modules with different fork configs.
        // Instead every heavy module gets its own distributed agent; the small independent leaf modules
        // are overlapped two-to-an-agent via a Maven -T reactor. The remaining quick JVM modules run in
        // one 'Fast Module Tests' reactor. Keep this set in sync with slowTestModules() (Fast reactor
        // excludes + Collect Results JaCoCo unstash). Module deps come from the Build stage's install
        // into the shared local repo, so no -am is needed.

        // Every quick JVM module in one -T 4 reactor (all modules NOT given a dedicated stage below,
        // and not frontend/playwright/legacy). This is where insight-brain-db/policy/common/event/
        // tenancy/guide + the PG DAO leaf modules (data-pg/db-pg/mtiq-db-pg) + mtiq-fips run.
        stage('Fast Module Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runFastModuleTests()
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // insight-brain-service surefire/failsafe: the largest single module, kept solo.
        stage('Service Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 120, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedModuleTests('insight-brain-service')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        stage('MTIQ Server Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedModuleTests('nexus-mtiq-server')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // Data is upstream of every test module; kept solo (short, and it cannot overlap a dependent
        // module without the reactor serialising it).
        stage('Data Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 120, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedModuleTests('insight-brain-data')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // Component H2 is the largest variant suite (~590 classes, single reused H2 fixture); solo.
        stage('Component H2 Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 120, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedModuleTests('insight-brain-variant-test-component-h2')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // Component PG is a single long-running Postgres-backed fork; keep it solo.
        stage('Component PG Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 120, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedModuleTests('insight-brain-variant-test-component-pg')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // Two independent single-fork leaf modules overlapped via -T 2. H2 uses an in-memory DB, PG a
        // Postgres container - separate fixtures in separate forked JVMs, so no cross-module collision.
        stage('Variant H2 + PG Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 120, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedModuleReactor(
                  ['insight-brain-variant-test-h2', 'insight-brain-variant-test-pg'], '2')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // Variant MTIQ + FIPS overlapped via -T 2 (FIPS is reuseForks=false: one class-JVM at a time).
        stage('Variant MTIQ + FIPS Tests') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 120, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedModuleReactor(
                  ['insight-brain-variant-test-mtiq', 'insight-brain-variant-test-fips'], '2')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
              script {
                String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
                stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
              }
            }
          }
        }

        // Legacy boots a full server per class (reuseForks=false) and is the most makespan-volatile
        // lane, so keep the balanced THREE-way class-range split rather than one slow stage.
        stage('Legacy Tests A-E') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runLegacyPartition('.*/[A-E].*Test.class')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
            }
          }
        }

        stage('Legacy Tests F-Q') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runLegacyPartition('.*/[F-Q].*Test.class')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
            }
          }
        }

        stage('Legacy Tests R-Z') {
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runLegacyPartition('.*/[R-Z].*Test.class')
            }
          }
          post {
            always {
              junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml', allowEmptyResults: true
            }
          }
        }

        stage('Playwright Functional Tests') {
          when {
            beforeAgent true
            expression { !params.skipPlaywrightTests }
          }
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 45, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedPlaywrightTests('sanity', '.*Test.class')
            }
          }
          post {
            always {
              script {
                capturePlaywrightStageResults()
              }
            }
          }
        }

        // Playwright Regression - split across 3 agents by test class name pattern
        // @Category(RegressionTest) method counts: A-L (234), M-P (257), Q-Z (222)
        stage('Playwright Regression 1 (A-L)') {
          when {
            beforeAgent true
            expression { playwrightRegressionEnabled() }
          }
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedPlaywrightTests('regression', '.*/[A-L].*Test.class')
            }
          }
          post {
            always {
              script {
                capturePlaywrightStageResults()
              }
            }
          }
        }

        stage('Playwright Regression 2 (M-P)') {
          when {
            beforeAgent true
            expression { playwrightRegressionEnabled() }
          }
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedPlaywrightTests('regression', '.*/[M-P].*Test.class')
            }
          }
          post {
            always {
              script {
                capturePlaywrightStageResults()
              }
            }
          }
        }

        stage('Playwright Regression 3 (Q-Z)') {
          when {
            beforeAgent true
            expression { playwrightRegressionEnabled() }
          }
          agent { label DISTRIBUTED_TEST_AGENT }
          options {
            timeout(time: 90, unit: 'MINUTES')
          }
          steps {
            script {
              runDistributedPlaywrightTests('regression', '.*/[Q-Z].*Test.class')
            }
          }
          post {
            always {
              script {
                capturePlaywrightStageResults()
              }
            }
          }
        }
      }
    }

    stage('Collect Results') {
      when {
        expression { return true } // Always run to collect results even if tests fail
      }
      steps {
        script {
          dir(env.BUILD_DIR) {
            // Unstash JaCoCo data from distributed test agents
            // Exec files go into build dir so report-aggregate can find them
            // XML files go into separate directories for recordCoverage to merge
            // One entry per distributed stage that stashes JaCoCo (jacoco-<sanitized stage name>).
            // Legacy stages are excluded from JaCoCo so they do not appear here.
            def distributedStages = [
                'Fast Module Tests',
                'Service Tests',
                'MTIQ Server Tests',
                'Data Tests',
                'Component H2 Tests',
                'Component PG Tests',
                'Variant H2 + PG Tests',
                'Variant MTIQ + FIPS Tests',
                'Playwright Functional Tests',
                'Playwright Regression 1 (A-L)',
                'Playwright Regression 2 (M-P)',
                'Playwright Regression 3 (Q-Z)'
            ]
            distributedStages.each { stageName ->
              String stashName = "jacoco-${stageName.replaceAll('[^a-zA-Z0-9]', '-')}"
              try {
                // Unstash exec files directly into build dir (report-aggregate needs them in place)
                unstash stashName
                // Also unstash into separate dir for XML aggregation (avoid overwrites)
                dir("jacoco-results/${stashName}") { unstash stashName }
              } catch (e) {
                echo "No JaCoCo coverage data from ${stageName}: ${e.message}"
              }
            }

            junit(
                testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, ' +
                    '**/target/karma-reports/*.xml, **/target/jest-reports/*.xml',
                allowEmptyResults: true,
                healthScaleFactor: 1.0
            )
            archiveArtifacts(
                artifacts: '**/target/*-reports/**',
                excludes: '**/TEST-*.xml, **/*-output.txt',
                allowEmptyArchive: true,
                fingerprint: false
            )
            // Archive JaCoCo coverage data files for offline analysis
            archiveArtifacts(
                artifacts: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml',
                allowEmptyArchive: true,
                fingerprint: false
            )
            // Single recordCoverage call merges XML reports from all distributed agents.
            // sourceCodeRetention NEVER: skip source-file painting. It resolves per-module sources
            // against the reactor root and finds 0 of ~3650 (painting failed for ALL files), wasting
            // ~180s on the critical path. Coverage metrics come from the parsed jacoco.xml regardless.
            recordCoverage(tools: [[parser: 'JACOCO', pattern: '**/target/site/jacoco/jacoco.xml']],
                sourceCodeRetention: 'NEVER')
          }
        }
      }
    }

    stage('Evaluate MTIQ Image Policy') {
      when {
        expression { shouldRunMtiqImagePolicyEvaluation() }
      }
      steps {
        script {
          dir(env.BUILD_DIR) {
            String scanImage = mtiqScanImageTag()
            try {
              buildMtiqScanImage(scanImage)
              evaluateMtiqImagePolicy(scanImage)
            }
            finally {
              removeMtiqScanImage(scanImage)
            }
          }
        }
      }
    }

    // Keep this stage after 'Evaluate MTIQ Image Policy'. The order is the gate: a Fail-action
    // violation fails that stage, and declarative pipeline then skips this one, so the image is
    // never pushed. Neither stage's when{} checks the build result, so reordering them — or moving
    // the evaluation into a parallel block — would silently allow a policy failure to publish.
    stage('Build MTIQ Image') {
      when {
        expression {
          return params.mtiqImagePushEnabled || currentBuild.fullProjectName.endsWith('_mtiq')
        }
      }
      steps {
        script {
          dir(env.BUILD_DIR) {
            def imageVersion = mtiqImageVersion()
            def shouldPush = params.mtiqImagePushEnabled || currentBuild.fullProjectName.endsWith('_mtiq')
            pushMTIQDockerImage(shouldPush, imageVersion)
            env.MTIQ_BUILD_SUCCEEDED = 'true'
          }
        }
      }
    }

    stage('Push to ECR Dev') {
      when {
        expression {
          return env.MTIQ_BUILD_SUCCEEDED == 'true' &&
            (params.mtiqImagePushEnabled || currentBuild.fullProjectName.endsWith('_mtiq'))
        }
      }
      steps {
        script {
          dir(env.BUILD_DIR) {
            try {
              def ecr = load 'jenkins/ecr-helpers.groovy'
              ecr.pushToEcrCached()  // branch auto-detected via gitBranch(env)
            } catch (Exception e) {
              echo "WARNING: ECR push failed (non-fatal): ${e.message}"
            }
          }
        }
      }
    }
  }

  post {
    always {
      echo 'Pipeline completed'
    }
  }
}

// =============================================================================
// Helper Functions
// =============================================================================

/** Whether the Playwright regression shards run. Unset (first build of a branch) means run. */
boolean playwrightRegressionEnabled() {
  return params.skipPlaywrightRegressionTests != true
}

void configureBranchJob() {
  String projName = currentBuild.fullProjectName
  boolean mtiqImagePushEnabledByDefault = (projName.toLowerCase().endsWith('/main') || projName.endsWith('_mtiq'))

  List params = [
      choice(
          name: 'buildMode',
          choices: ['FEATURE', 'MAIN'],
          description: 'Build mode: FEATURE uses build caching. ' +
              'MAIN runs all tests, no caching. ' +
              'Both modes run the Playwright functional/UI sanity suite; the Playwright regression ' +
              'suite is controlled separately by skipPlaywrightRegressionTests. ' +
              'Changing this requires a new build to take effect.'
      ),
      booleanParam(defaultValue: false,
          description: 'If checked will run policy evaluation with full reachability analysis (same as Main)',
          name: 'policyEvaluationEnabled'),
      booleanParam(defaultValue: false,
          description: 'If checked will build the MTIQ Docker image and evaluate it against IQ policy ' +
              '(same as Main). Independent of mtiqImagePushEnabled.',
          name: 'mtiqImagePolicyEvaluationEnabled'),
      booleanParam(defaultValue: mtiqImagePushEnabledByDefault,
          description: 'If checked will push the MTIQ Docker image to RSC for this branch (not available on Main)',
          name: 'mtiqImagePushEnabled'),
      booleanParam(defaultValue: false,
          description: 'If checked will run ReferencePolicyImportIntegrationTest for snapshot builds of feature ' +
              'branches',
          name: 'runRefPolicyImportIntTest'),
      booleanParam(defaultValue: false,
          description: 'If checked will create bundled artifacts (shade, jreleaser, MTIQ assembly). Required for ' +
              'releases and MTIQ image push.',
          name: 'bundlingEnabled'),

      booleanParam(name: 'skipPlaywrightTests', defaultValue: false,
          description: 'Skip: Playwright Functional Tests (insight-brain-playwright-test)'),
      booleanParam(name: 'skipPlaywrightRegressionTests', defaultValue: false,
          description: 'Skip: Playwright Regression Tests (-Pregression, split across 3 agents). ' +
              'Runs on every build unless checked.'),
      choice(name: 'playwrightTraceMode', choices: ['on-failure', 'always', 'off'],
          description: 'Playwright trace: on-failure (routine CI); always when debugging flakes; off to disable.')
  ]

  def propertyList = [copyArtifactPermission("/${projName}"), parameters(params)]

  propertyList.add(disableConcurrentBuilds(abortPrevious: true))

  properties(propertyList)
}

Map getMavenBuildConfig() {
  def opts = []

  // Base options
  opts << "--no-transfer-progress"
  opts << "-T 1C"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Skip options for faster builds
  opts << "-DskipTests"
  opts << "-Dspotless.apply.skip=true"
  opts << "-Dspotless.check.skip=true"
  opts << "-Dmaven.source.skip=true"
  opts << "-Dmaven.javadoc.skip=true"
  // Keep frontend lint off the serial critical Build path; it runs in the parallel Frontend
  // Tests stage instead (esbuild:build produces the same bundle without linting).
  opts << "-Dfrontend.build.script=esbuild:build"
  // Note: Do NOT skip install - forked test processes need artifacts in local repo
  //
  // Maven Build Cache Extension: evaluated (warm service-change build 149s -> 101s) and REJECTED
  // as too flaky - do NOT re-add .mvn/extensions.xml / cache config without new evidence. It never
  // helped cold builds (the CI case here), and its false-positive cache hits risk skipping test
  // execution. The kept wins (maven.source.skip, mtiq.package.skip) are compute-only and safe.

  // Bundling
  if (!isBundlingEnabled()) {
    opts << "-Dmtiq.package.skip=true"
  }

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for frontend install stage (node/yarn/npm dependencies)
Map getFrontEndInstallConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl insight-brain-frontend"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for frontend test stages (Jest)
Map getFrontEndTestConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl insight-brain-frontend"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for MTIQ Docker image build (insight-brain module with jdks profile)
Map getMtiqBuildConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl :insight-brain"
  opts << "-Pjdks"
  opts << "-Pjenkins"
  opts << "-DskipTests"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

// Get Maven config for MTIQ jreleaser assembly
Map getMtiqAssembleConfig() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl :nexus-mtiq-server"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return mavenCommon(
      javaVersion: 'OpenJDK 25',
      mavenVersion: 'Maven 3.9.x',
      useEventSpy: false,
      mavenOptions: opts.join(' ')
  )
}

/**
 * Execute Maven directly using withMaven, bypassing the shared library mvn helper.
 * This gives us control over JAVA_TOOL_OPTIONS to prevent the Jenkins Maven Event Spy
 * from being loaded into forked test JVMs.
 */
void mvnDirectForTests(String mavenOptions, String goals) {
  String localRepo = "${env.WORKSPACE}/.zion/repository"
  // This file has config for sonatype.repo.sonatype.app now
  String npmConfigFileId = 'rsc-ro-npmrc'

  configFileProvider([configFile(fileId: npmConfigFileId, variable: 'NPM_CONFIG_USERCONFIG')]) {
    withMaven(jdk: 'OpenJDK 25', maven: 'Maven 3.9.x', mavenSettingsConfig: 'private-settings.xml',
        publisherStrategy: 'EXPLICIT') {
      String mvnCmdLine = "mvn jacoco:prepare-agent ${goals} jacoco:report -Dmaven.repo.local='${localRepo}'"
      mvnCmdLine += " -DnpmRegistryURL="
      mvnCmdLine += " -DyarnDownloadRoot=https://sonatype.repo.sonatype.app/repository/yarn-binaries/"
      mvnCmdLine += " -DnodeDownloadRoot=https://sonatype.repo.sonatype.app/repository/nodejs-dist/"
      mvnCmdLine += " -DnpmDownloadRoot=https://sonatype.repo.sonatype.app/repository/npm-all/npm/-/"
      mvnCmdLine += " -DserverId=sonatype.repo.sonatype.app"
      mvnCmdLine += " ${mavenOptions}"

      sh mvnCmdLine
    }
  }
}

String mtiqImageVersion() {
  def dateSection = new Date().format("yyyyMMddHHmm", TimeZone.getTimeZone('UTC'))
  def buildNumSection = env.BUILD_NUMBER
  def gitShortHashSection = env.GIT_COMMIT.take(8)

  String branch = gitBranch(env).replaceAll(/[^\w.-]/, '_').take(30)
  return "branch-${branch}-${env.BUILD_NUMBER}"
}

/**
 * Create or re-select the shared buildx builder, pinned to the digest-referenced buildkit image
 * because Firewall quarantines every moby/buildkit tag.
 *
 * Idempotent: create fails if the builder already exists on this agent, in which case we select it.
 * Mirrors ensureMtiqBuildxBuilder() in Jenkinsfile.main so the scan and the push share a cache here
 * the same way they do there.
 */
void ensureMtiqBuildxBuilder() {
  sh """docker buildx create --name ${mtiqBuildxBuilder} --use \\
        --driver-opt image=${sonatypeDockerRegistryId()}/moby/buildkit@${buildkitImageDigest} \\
        || docker buildx use ${mtiqBuildxBuilder}"""
}

void pushMTIQDockerImage(boolean pushMtiqImage, String imageVersion) {
  if (currentBuild.fullProjectName.contains("insight-brain/release")) {
    echo 'Skipping MTIQ docker image for IQ on-premise release'
    return
  }

  echo "MTIQ image version: ${imageVersion}"
  String iqVersion = getMavenProjectVersion('.')
  echo "iqVersion:'${iqVersion}'"

  mvn getMtiqBuildConfig(), 'jdks:setup-jdks install'
  mvn getMtiqAssembleConfig(), 'jreleaser:assemble'

  dir("nexus-mtiq-server") {
    withSonatypeDockerRegistry() {
      echo "pushMtiqImage: $pushMtiqImage"
      def pushOption = pushMtiqImage ? " --push " : ""

      ensureMtiqBuildxBuilder()
      sh """docker buildx build --platform=linux/amd64,linux/arm64 \
            --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} \
            --build-arg IQ_SERVER_VERSION=${iqVersion} \
            ${pushOption} \
            --tag ${sonatypeDockerRegistryId()}/mtiq/server:${imageVersion} ."""
    }
  }
}

/** Local-only tag for the image handed to the container scan. Never pushed to any registry. */
String mtiqScanImageTag() {
  return "mtiq-server-scan:${env.BUILD_NUMBER}"
}

/**
 * Build the MTIQ image that the policy evaluation scans.
 *
 * The Dockerfile COPYs the jreleaser jlink assemblies for both architectures and interpolates
 * IQ_SERVER_VERSION into those paths, so the assemblies are built first.
 *
 * Single-platform (linux/amd64): buildx --load cannot load a multi-platform result into the classic
 * docker image store. The Ubuntu package set is the same on arm64, so the OS component list — and
 * therefore the violation set — is the same.
 *
 * @param scanImage local tag of the image to build
 */
void buildMtiqScanImage(String scanImage) {
  String iqVersion = getMavenProjectVersion('.')
  echo "Building MTIQ image for scanning: ${scanImage} (iqVersion='${iqVersion}')"

  mvn getMtiqBuildConfig(), 'jdks:setup-jdks install'
  mvn getMtiqAssembleConfig(), 'jreleaser:assemble'

  dir('nexus-mtiq-server') {
    withSonatypeDockerRegistry() {
      ensureMtiqBuildxBuilder()
      sh "docker buildx build --platform=linux/amd64 " +
          " --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} " +
          " --build-arg IQ_SERVER_VERSION=${iqVersion} " +
          " --load " +
          " --tag ${scanImage} ."
    }
  }
}

/**
 * Evaluate the built MTIQ image against IQ policy at IQ stage 'develop'.
 *
 * IQ retains one evaluation per application and stage. Feature branches report to 'develop', as the
 * source scan in the Policy Evaluation stage does, so they cannot overwrite the 'build' evaluation
 * that main records for this application.
 *
 * @param scanImage local tag of the image to evaluate
 */
void evaluateMtiqImagePolicy(String scanImage) {
  // Evaluate from an empty directory. Run from the workspace root and the scanner also picks up
  // the module jars in target/, mixing the source application's components into the image report.
  // dir() only sets the working directory for nested steps; it does not create the directory, and
  // nexusPolicyEvaluation fails if its basedir is absent.
  String scanDir = 'mtiq-container-scan'
  sh "mkdir -p ${scanDir}"
  dir(scanDir) {
    // nexusPolicyEvaluation blocks in "Waiting for policy evaluation to complete..." with no
    // timeout of its own, so a stalled IQ evaluation parks the stage indefinitely and holds the
    // agent. Convert the expiry into an ordinary failure so it travels the same path as a policy
    // violation, and rethrow everything else: a human abort has to keep taking effect immediately.
    try {
      timeout(time: 20, unit: 'MINUTES') {
        nexusPolicyEvaluation(
            iqApplication: mtiqImageIqApplication,
            iqStage: 'develop',
            iqScanPatterns: [[scanPattern: "container:${scanImage}"]],
            failBuildOnNetworkError: true,
            // CLM-44494: warn-level findings must not mark the build UNSTABLE. Matches the four
            // source-scan calls aligned in #16896. The consequence is that the IQ stage action is
            // the only gate on this image: Warn reports and publishes, Fail blocks everything.
            unstableBuildOnScanningWarnings: false)
      }
    }
    catch (org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
      if (isMtiqEvaluationTimeout(e)) {
        error 'MTIQ image policy evaluation did not complete within 20 minutes'
      }
      throw e
    }
  }
}

/**
 * Whether a pipeline interruption came from the evaluation timeout rather than from an abort.
 *
 * @param e the interruption thrown into the evaluation
 * @return true when the timeout expired
 */
boolean isMtiqEvaluationTimeout(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
  return e.causes.any {
    it instanceof org.jenkinsci.plugins.workflow.steps.TimeoutStepExecution.ExceededTimeout
  }
}

/**
 * Drop the local scan image from the agent's docker daemon. Tolerates the image being absent.
 *
 * @param scanImage local tag of the image to remove
 */
void removeMtiqScanImage(String scanImage) {
  sh "docker image rm -f ${scanImage} || true"
}

boolean isBundlingEnabled() {
  return params.bundlingEnabled
}

boolean shouldRunPolicyEvaluation() {
  return params.policyEvaluationEnabled
}

boolean shouldRunMtiqImagePolicyEvaluation() {
  return params.mtiqImagePolicyEvaluationEnabled
}

/**
 * Stash the minimum artifacts required to run tests on a remote agent.
 *
 * This creates a stash containing:
 * 1. SNAPSHOT JARs from local Maven repository (~260MB)
 * 2. Compiled test classes from target directories (~70MB)
 * 3. pom.xml files for Maven project structure
 *
 * Remote agents can unstash these and run tests without rebuilding.
 * External dependencies are resolved from Nexus (already cached on agents).
 */
void stashTestArtifacts() {
  def version = getMavenProjectVersion('.')
  def stashDir = "${env.BUILD_DIR}/.test-stash"

  echo "Stashing test artifacts for distributed testing..."
  echo "  Project version: ${version}"
  echo "  Build dir: ${env.BUILD_DIR}"
  echo "  Workspace: ${env.WORKSPACE}"

  // Create a minimal .m2 structure with only SNAPSHOT artifacts
  // Use parallel copies for speed
  sh """
    set -e
    rm -rf ${stashDir}
    mkdir -p ${stashDir}/m2-snapshot
    mkdir -p ${stashDir}/test-classes

    # Find the local Maven repository - check multiple possible locations
    SNAPSHOT_SRC=""
    for repo_path in "${env.WORKSPACE}/.zion/repository" "${env.WORKSPACE}/.m2/repository" "\$HOME/.m2/repository"; do
      if [ -d "\$repo_path/com/sonatype/insight/brain" ]; then
        SNAPSHOT_SRC="\$repo_path/com/sonatype/insight/brain"
        echo "  Found SNAPSHOT artifacts in: \$repo_path"
        break
      fi
    done

    if [ -z "\$SNAPSHOT_SRC" ]; then
      echo "ERROR: Could not find insight-brain artifacts in any Maven repository location"
      exit 1
    fi

    SNAPSHOT_DST="${stashDir}/m2-snapshot/com/sonatype/insight/brain"

    # Parallel copy of SNAPSHOT modules (run all copies in background, then wait)
    echo "Copying SNAPSHOT artifacts in parallel..."
    pids=""
    copied_count=0
    for module_dir in \$SNAPSHOT_SRC/*/; do
      module_name=\$(basename "\$module_dir")
      version_dir="\${module_dir}${version}"
      if [ -d "\$version_dir" ]; then
        mkdir -p "\$SNAPSHOT_DST/\$module_name"
        cp -r "\$version_dir" "\$SNAPSHOT_DST/\$module_name/" &
        pids="\$pids \$!"
        copied_count=\$((copied_count + 1))
      fi
    done
    # Wait for all SNAPSHOT copies to complete
    for pid in \$pids; do wait \$pid || exit 1; done
    echo "  Copied \$copied_count SNAPSHOT module(s)"

    if [ \$copied_count -eq 0 ]; then
      echo "ERROR: No SNAPSHOT artifacts found for version ${version}"
      exit 1
    fi

    # Parallel copy of classes/test-classes for EVERY module at any depth (not a hardcoded list) so
    # every distributed stage -- including the variant-test suites now nested under
    # insight-brain-testing/ -- has its compiled tests on the remote agent. mvnDirectForDistributedTests
    # runs surefire:test directly and does NOT recompile, so a module whose test-classes are missing
    # here silently runs zero tests. Frontend is excluded (its Jest tests run on the main agent).
    echo "Copying classes/test-classes in parallel..."
    pids=""
    # Write the dir list to a file and read it with `while read -r` so paths with spaces are not word-split
    # (matches the pom.xml copy below); a plain `for tc in \$(find ...)` would split on whitespace.
    find . -type d \\( -name test-classes -o -name classes \\) -path '*/target/*' \\
                  -not -path './insight-brain-frontend/*' -not -path '*/.test-stash/*' > "${stashDir}/.class-dirs.txt"
    while read -r tc; do
      module_target=\$(dirname "\$tc")
      mkdir -p "${stashDir}/test-classes/\$module_target"
      cp -r "\$tc" "${stashDir}/test-classes/\$module_target/" &
      pids="\$pids \$!"
    done < "${stashDir}/.class-dirs.txt"
    rm -f "${stashDir}/.class-dirs.txt"
    # Wait for all class copies to complete
    for pid in \$pids; do wait \$pid || exit 1; done
    echo "  Copied classes directories"

    # Note: Frontend module not stashed - frontend tests run on main agent with cached node/yarn

    # Copy pom.xml files (fast, no need to parallelize)
    find . -name 'pom.xml' -not -path '*/target/*' -not -path '*/.test-stash/*' | while read pom; do
      dir=\$(dirname "\$pom")
      mkdir -p "${stashDir}/test-classes/\$dir"
      cp "\$pom" "${stashDir}/test-classes/\$dir/"
    done

    # Root argfiles referenced by forked test JVMs (parent pom argLine: @<root>/jvm.options).
    # Without this, forks on remote agents fail to open jvm.options -> 0 tests.
    for f in jvm.options; do
      [ -f "\$f" ] && cp "\$f" "${stashDir}/test-classes/\$f"
    done

    # Report sizes
    echo ""
    echo "=== Stash sizes ==="
    du -sh ${stashDir}/m2-snapshot || echo "m2-snapshot: empty"
    du -sh ${stashDir}/test-classes || echo "test-classes: empty"
  """

  // Create single combined stash (faster than two separate stashes)
  dir(stashDir) {
    stash name: 'test-artifacts', includes: '**'
  }

  echo "Test artifacts stashed successfully."
}

/**
 * Unstash and set up test artifacts on a remote agent.
 *
 * This restores the stashed artifacts and configures the Maven local repository
 * to include the SNAPSHOT artifacts from the build agent.
 *
 * @return String The path to use for maven.repo.local
 */
String unstashTestArtifacts() {
  def localRepo = "${env.WORKSPACE}/.m2/repository"

  echo "Unstashing test artifacts on remote agent..."

  // Unstash the combined test artifacts
  unstash 'test-artifacts'

  // Move artifacts into place using parallel operations
  sh """
    set -e
    mkdir -p ${localRepo}

    # Start parallel restore operations
    pids=""

    # Merge the SNAPSHOT artifacts into the local repo (background)
    if [ -d "m2-snapshot/com" ]; then
      cp -r m2-snapshot/com ${localRepo}/ &
      pids="\$pids \$!"
    fi

    # Restore classes/test-classes to their original locations (background)
    if [ -d "test-classes" ]; then
      (
        cd test-classes
        find . -type d -name 'target' | while read target_dir; do
          module_dir=\$(dirname "\$target_dir")
          mkdir -p "${env.WORKSPACE}/\$module_dir"
          cp -r "\$target_dir" "${env.WORKSPACE}/\$module_dir/"
        done
        find . -name 'pom.xml' | while read pom; do
          dir=\$(dirname "\$pom")
          mkdir -p "${env.WORKSPACE}/\$dir"
          cp "\$pom" "${env.WORKSPACE}/\$dir/"
        done
        # Restore root argfiles (e.g. jvm.options) referenced by forked test JVMs
        for f in jvm.options; do
          [ -f "\$f" ] && cp "\$f" "${env.WORKSPACE}/\$f"
        done
      ) &
      pids="\$pids \$!"

      # Restore frontend module (node, node_modules, src)
      if [ -d "test-classes/insight-brain-frontend" ]; then
        mkdir -p "${env.WORKSPACE}/insight-brain-frontend"
        for dir in node node_modules src; do
          if [ -d "test-classes/insight-brain-frontend/\$dir" ]; then
            cp -r "test-classes/insight-brain-frontend/\$dir" "${env.WORKSPACE}/insight-brain-frontend/" &
            pids="\$pids \$!"
          fi
        done
      fi
    fi

    # Wait for all restore operations to complete
    for pid in \$pids; do wait \$pid || exit 1; done
    echo "  Restored all artifacts"

    # Cleanup temp directories
    rm -rf m2-snapshot test-classes

    echo ""
    echo "=== Local repo size ==="
    du -sh ${localRepo}
  """

  echo "Test artifacts restored successfully."
  return localRepo
}

/**
 * Run static analysis (Spotless, License Check) on a distributed agent.
 *
 * This runs on a fresh checkout - no unstash needed since static analysis
 * only requires source code, not compiled artifacts.
 */
void runDistributedStaticAnalysis() {
  echo "Running distributed static analysis..."
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  // Run Spotless check and License Check
  // These only need source code, not compiled artifacts
  mvnDirectForDistributedTests(buildStaticAnalysisMavenOptions(),
      'spotless:check license:check',
      localRepo)
}

/**
 * Build Maven options for distributed static analysis.
 */
String buildStaticAnalysisMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 2"
  opts << "-Ppre-check"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  // Disable build cache for static analysis
  opts << "-Dmaven.build.cache.enabled=false"
  opts << "-Dmaven.build.cache.remote.enabled=false"
  opts << "-Dmaven.build.cache.remote.save.enabled=false"

  return opts.join(' ')
}

/**
 * Run frontend tests (Jest) on a distributed agent.
 *
 * This function:
 * 1. Unstashes the build artifacts from the main build agent
 * 2. Runs Jest tests
 */
void runDistributedFrontendTests() {
  echo "Running distributed frontend tests..."
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  // Run Jest tests using Maven
  def opts = buildDistributedFrontendTestMavenOptions()
  mvnDirectForDistributedTests(opts,
      'com.github.eirslett:frontend-maven-plugin:yarn@jest',
      localRepo)
}

/**
 * Build Maven options for distributed frontend test execution.
 */
String buildDistributedFrontendTestMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl insight-brain-frontend"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  return opts.join(' ')
}

/** The test modules that each get their own parallel stage on a dedicated distributed agent. */
List slowTestModules() {
  // Everything NOT listed here -- and not frontend, playwright or legacy -- runs together in the
  // single 'Fast Module Tests' reactor. Keep in sync with buildFastModuleTestMavenOptions() and the
  // Collect Results stage list. stashTestArtifacts carries test-classes for ALL modules plus the
  // root jvm.options argfile, so every module listed here runs its real tests on a fresh agent.
  return [
      'insight-brain-service',
      'insight-brain-data',
      'nexus-mtiq-server',
      'insight-brain-variant-test-component-h2',
      'insight-brain-variant-test-component-pg',
      'insight-brain-variant-test-h2',
      'insight-brain-variant-test-pg',
      'insight-brain-variant-test-mtiq',
      'insight-brain-variant-test-fips'
  ]
}

/**
 * Run one test module on the current (distributed) agent: restore the build artifacts, then run the
 * module's surefire + failsafe tests. Each variant-test module boots its fixture once per module JVM
 * (reuseForks per the module pom), so running the whole module on one agent boots it exactly once.
 */
void runDistributedModuleTests(String module) {
  echo "Running distributed tests for ${module}..."
  def localRepo = unstashTestArtifacts()
  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {
      mvnDirectForDistributedTests(buildModuleTestMavenOptions(module),
          'surefire:test failsafe:integration-test failsafe:verify', localRepo)
    }
  }
}

/** Build Maven options for a single-module distributed test run. */
String buildModuleTestMavenOptions(String module) {
  def opts = []

  opts << "--no-transfer-progress"
  // No -T: a single module has no cross-module parallelism; test concurrency is the module's own
  // surefire/failsafe forkCount defined in its pom.
  opts << "-pl ':${module}'"
  opts << "-DdetectTestEntityLeaks"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Run several INDEPENDENT leaf test modules together on one distributed agent, overlapped via Maven
 * -T reactor parallelism. Each module keeps its own surefire/failsafe forkCount, so the agent's
 * concurrent JVMs = sum of the modules' fork counts; keep that x -Xmx under the agents' 16 GB (see
 * CLM-39214). If a listed module depends on another the reactor orders them and they run
 * sequentially instead of overlapping.
 */
void runDistributedModuleReactor(List<String> modules, String threads) {
  echo "Running distributed reactor for ${modules.join(', ')} (-T ${threads})..."
  def localRepo = unstashTestArtifacts()
  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {
      mvnDirectForDistributedTests(buildReactorTestMavenOptions(modules, threads),
          'surefire:test failsafe:integration-test failsafe:verify', localRepo)
    }
  }
}

/** Build Maven options for a multi-module (-pl ... -T ...) distributed reactor test run. */
String buildReactorTestMavenOptions(List<String> modules, String threads) {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T ${threads}"
  opts << "-pl '${modules.collect { ":${it}" }.join(',')}'"
  opts << "-DdetectTestEntityLeaks"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Run one class-name partition of the legacy module on the current (distributed) agent. Legacy boots
 * a full server per class (reuseForks=false), so it is split across several stages by a contiguous
 * class-name range via surefire/failsafe -Dtest / -Dit.test regex. Legacy is excluded from JaCoCo.
 */
void runLegacyPartition(String testRegex) {
  echo "Running legacy partition ${testRegex}..."
  def localRepo = unstashTestArtifacts()
  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {
      mvnDirectForDistributedTests(buildLegacyPartitionOptions(testRegex),
          'surefire:test failsafe:integration-test failsafe:verify', localRepo)
    }
  }
}

/** Build Maven options for one legacy class-name partition. */
String buildLegacyPartitionOptions(String testRegex) {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-pl ':insight-brain-variant-test-legacy'"
  opts << "-DdetectTestEntityLeaks"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  // Select this partition's classes; also drop ReferencePolicyImportIntegrationTest (imports the
  // live reference policy from external staging) unless explicitly requested via build param.
  String filter = "%regex[${testRegex}]"
  if (!params.runRefPolicyImportIntTest) {
    filter += ",!ReferencePolicyImportIntegrationTest"
  }
  opts << "-Dtest='${filter}'"
  opts << "-Dit.test='${filter}'"

  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Run the 'Fast Module Tests' reactor on the current (distributed) agent: every quick JVM module in
 * one -T 4 pass. The slow modules (slowTestModules), plus frontend, playwright and legacy, each run
 * in their own stage, so they are excluded here.
 */
void runFastModuleTests() {
  echo "Running fast module tests reactor..."
  def localRepo = unstashTestArtifacts()
  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {
      mvnDirectForDistributedTests(buildFastModuleTestMavenOptions(),
          'surefire:test failsafe:integration-test failsafe:verify', localRepo)
    }
  }
}

/** Build Maven options for the 'Fast Module Tests' reactor (every quick module in one -T 4 pass). */
String buildFastModuleTestMavenOptions() {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 4"
  // Exclude frontend and playwright (own stages), legacy (own split stages), plus every distributed
  // slow module. Everything else -- db/policy/common/event/tenancy/guide + the PG DAO leaf modules
  // (data-pg/db-pg/mtiq-db-pg) + mtiq-fips -- runs here.
  def excluded = ['insight-brain-frontend', 'insight-brain-playwright-test',
                  'insight-brain-variant-test-legacy'] + slowTestModules()
  opts << "-pl '${excluded.collect { "!:${it}" }.join(',')}'"
  opts << "-DdetectTestEntityLeaks"
  opts << "-D skip-functional-test"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  opts << "-Dsurefire.runOrder=alphabetical"
  opts << "-Dsurefire.rerunFailingTestsCount=2"
  opts << "-Dsurefire.failOnFlakeCount=5"
  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/**
 * Execute Maven for distributed tests with explicit local repo.
 */
void mvnDirectForDistributedTests(String mavenOptions, String goals, String localRepo) {
  // This file has config for sonatype.repo.sonatype.app now
  String npmConfigFileId = 'rsc-ro-npmrc'

  configFileProvider([configFile(fileId: npmConfigFileId, variable: 'NPM_CONFIG_USERCONFIG')]) {
    withMaven(jdk: 'OpenJDK 25', maven: 'Maven 3.9.x', mavenSettingsConfig: 'private-settings.xml',
        publisherStrategy: 'EXPLICIT') {
      String mvnCmdLine = "mvn jacoco:prepare-agent ${goals} jacoco:report -Dmaven.repo.local='${localRepo}'"
      mvnCmdLine += " -DnpmRegistryURL="
      mvnCmdLine += " -DyarnDownloadRoot=https://sonatype.repo.sonatype.app/repository/yarn-binaries/"
      mvnCmdLine += " -DnodeDownloadRoot=https://sonatype.repo.sonatype.app/repository/nodejs-dist/"
      mvnCmdLine += " -DnpmDownloadRoot=https://sonatype.repo.sonatype.app/repository/npm-all/npm/-/"
      mvnCmdLine += " -DserverId=sonatype.repo.sonatype.app"
      mvnCmdLine += " ${mavenOptions}"

      sh mvnCmdLine
    }
  }
}

/**
 * Run a Playwright UI suite (insight-brain-playwright-test) on a distributed agent. The 'sanity'
 * and 'regression' profiles select @Category(SanityTest) and @Category(RegressionTest)
 * respectively, narrowed to the test classes matching {@code testPattern}.
 */
void runDistributedPlaywrightTests(String profile, String testPattern) {
  echo "Running distributed Playwright ${profile} tests with pattern: ${testPattern}"
  echo "  Workspace: ${env.WORKSPACE}"

  // Restore stashed artifacts
  def localRepo = unstashTestArtifacts()

  withSonatypeDockerRegistry() {
    withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
             "TESTCONTAINERS_RYUK_DISABLED=true"]) {

      def opts = buildPlaywrightTestMavenOptions(profile, testPattern)
      mvnDirectForDistributedTests(opts, 'failsafe:integration-test failsafe:verify', localRepo)
    }
  }
}

/** Build Maven options for Playwright test execution under the given profile and pattern. */
String buildPlaywrightTestMavenOptions(String profile, String testPattern) {
  def opts = []

  opts << "--no-transfer-progress"
  opts << "-T 1"
  opts << "-pl 'com.sonatype.insight.brain:insight-brain-playwright-test'"
  opts << "-D build.number=${env.BUILD_NUMBER}"

  opts << "-P${profile}"
  opts << "-Dit.test='%regex[${testPattern}]'"
  opts << "-DdetectTestEntityLeaks"

  opts << "-Dfailsafe.runOrder=alphabetical"
  opts << "-Dfailsafe.rerunFailingTestsCount=2"
  opts << "-Dfailsafe.failOnFlakeCount=5"

  opts << "-Dplaywright.trace=${params.playwrightTraceMode ?: 'on-failure'}"

  // Docker registry
  opts << "-Ddocker.registry=${sonatypeDockerRegistryId()}"

  // Error handling
  opts << "-e"
  opts << "-C"
  opts << "-fae"
  opts << "-Dmaven.test.failure.ignore"

  return opts.join(' ')
}

/** Collect JUnit results, archive Playwright artifacts, and stash JaCoCo for a Playwright stage. */
void capturePlaywrightStageResults() {
  junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
  archiveArtifacts(artifacts: 'insight-brain-testing/insight-brain-playwright-test/target/playwright-screenshots/**',
                   allowEmptyArchive: true)
  archiveArtifacts(artifacts: 'insight-brain-testing/insight-brain-playwright-test/target/playwright-traces/**',
                   allowEmptyArchive: true)
  archiveArtifacts(artifacts: 'insight-brain-testing/insight-brain-playwright-test/target/playwright-diagnostics/**',
                   allowEmptyArchive: true)
  archiveArtifacts(artifacts: 'insight-brain-testing/insight-brain-playwright-test/target/*.hprof',
                   allowEmptyArchive: true)
  String stashName = "jacoco-${env.STAGE_NAME.replaceAll('[^a-zA-Z0-9]', '-')}"
  stash name: stashName, includes: '**/target/jacoco.exec, **/target/site/jacoco/jacoco.xml', allowEmpty: true
}
