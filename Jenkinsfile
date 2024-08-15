/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _
import hudson.plugins.git.GitChangeSet
import hudson.scm.ChangeLogSet
import hudson.scm.ChangeLogSet.Entry

configureBranchJob()
make(
    // 2024-05-03: We are using c6i.2xlarge EC2 instances. I tried c6i.4xlarge with "--threads 8" and there was no difference.
    //agentLabel: 'iq-large'
    deployBranch: 'main',
    useEventSpy: false,
    javaVersion: 'OpenJDK 17',
    mavenVersion: 'Maven 3.9.x',
    mavenOptions: "-D skip-functional-test -D build.number=${env.BUILD_NUMBER} --threads 4",
    retentionPolicy: currentBuild.fullProjectName.contains('master-snapshot') ? RetentionPolicy.DEFAULT : RetentionPolicy.SHORT_TERM,
    prepare: {

    if (currentBuild.fullProjectName.toLowerCase().contains('insight/insight-brain/master-snapshot')) {
        String fixVersion = 'brain-next'
        List<String> newFixVersions = ['saas-next']
        echo "Replacing '${fixVersion}' with [${newFixVersions.join(', ')}]"
        List<String> issues = getIssuesByFixVersion('CLM', fixVersion)
        issues.addAll(getIssuesByFixVersion('SDEV', fixVersion))
        issues.addAll(getIssuesByFixVersion('INT', fixVersion))
        issues.addAll(getIssuesByFixVersion('NEXUS', fixVersion))
        issues.addAll(getIssuesByFixVersion('SBOM', fixVersion))
        replaceFixVersionForIssues(issues, fixVersion, newFixVersions)
      }
    },
    snapshotBuildAndTest: { Map<String, ?> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J ->
      echo "Using mavenVersion='${mavenCommon.get('mavenVersion')}'"

      /*
        Main is configured not to use the cache. We also skip tests on main because it runs the tests in parallel stages
        which requires copying the workspace. With cache enabled its actually faster to run the compilation and tests
        in a single stage.
       */
      String mavenOptions = mavenCommon.get('mavenOptions')

      if (isFastBuild()) {
        mavenOptions = addBuildCacheOptions(mavenOptions, true)
      }
      else {
        mavenOptions = addBuildCacheOptions(mavenOptions, false)
      }

      mavenOptions += " -DskipTests"

      mavenCommon.put('mavenOptions', mavenOptions)

      withSonatypeDockerRegistry() {
        withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/",
                 "TESTCONTAINERS_RYUK_DISABLED=true"]) {
          runAllTests(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
        }
      }
    },
    releaseBuild: { Map<String, ?> mavenCommon, String keystoreCredentialsId, boolean useInstall4J ->
      withSonatypeDockerRegistry() {
        withEnv(["TESTCONTAINERS_HUB_IMAGE_NAME_PREFIX=${sonatypeDockerRegistryId()}/"]) {
          buildAndSkipTests(mavenCommon, keystoreCredentialsId, false, useInstall4J)
        }
      }
    },
    releaseFromCommit: true,
    snapshotProjectName: 'insight/insight-brain/master-snapshot',
    githubProjectUrl: 'git@github.com:sonatype/insight-brain.git',
    runFeatureBranchPolicyEvaluations: true,
    iqPolicyEvaluation: { stage ->
        if (shouldRunPolicyEvaluation()) {
          nexusPolicyEvaluation iqStage: stage, iqApplication: 'insight-brain',
              iqScanPatterns: [[scanPattern: 'insight-brain-frontend/target/webpack-modules']],
              //Test files inside the maven modules are excluded from the scan
              iqModuleExcludes: [[moduleExclude: '**/test/**'], [moduleExclude: '**/test-classes/**/module.xml']],
              failBuildOnNetworkError: true,
              callflow: [
                enable: true,
                includes: [
                  'nexus-iq-server/target/insight-brain-service-*.jar'
                ],
                entrypointStrategy: [
                  $class: 'NamedStrategy',
                  name: 'JAVA_MAIN',
                  namespaces:['com.sonatype.insight']
                ],
                java: [
                  options: [
                    '-Xmx12G'
                  ]
                ]
              ]

          if (stage == 'release') {
            build(job: 'bnr/lifecycle-for-sonatype/generate-attribution-report',
                  parameters: [
                    string(name: 'applicationId', value: 'insight-brain'),
                    string(name: 'applicationName', value: 'Nexus Lifecycle'),
                    string(name: 'applicationVersion', value: params.version)
                  ]
              )
              copyArtifacts filter: "*insight-brain-${params.version}*.html",
                  projectName: 'bnr/lifecycle-for-sonatype/generate-attribution-report'
            }
          }
    },
    distFiles: [
      includes: [
        'nexus-iq-server/target/*.zip*',
        'nexus-iq-server/target/*.tar.gz*',
        'nexus-iq-cli/target/*.jar*',
        'nexus-iq-diagnostics/target/*.jar*',
        'nexus-mtiq-server/target/*.tar.gz*',
        "*insight-brain-${params.version}*.html"
      ],
      excludes: [
        '**/*-sources.jar*',
        '**/*-javadoc.jar*',
        '**/original-*'
      ]
    ],
    usePMD: true,
    useCheckstyle: true,
    releaseRetentionPolicy: RetentionPolicy.TEN_BUILDS,
    onSuccess: {
        postBuild()
    },
    onUnstable: {
        postBuild()
    }
)

void postBuild() {
  if (!isFastBuild()) {
    pushDockerImageIfDeployBranch()
  }
  pushMTIQDockerImage()
}

void configureBranchJob() {
  // Use the project name to determine the branch
  String projName = currentBuild.fullProjectName
  boolean mtiqImagePushEnabledByDefault = (projName.toLowerCase().contains('master') || projName.endsWith('_mtiq'))

  List params = [
      booleanParam(defaultValue: true,
          description: 'If checked will skip functional and UI Tests.',
          name: 'fastBuild'),
      booleanParam(defaultValue: mtiqImagePushEnabledByDefault,
          description: 'If checked will push the MTIQ Docker image to RSC for this branch',
          name: 'mtiqImagePushEnabled')
      ]

  // Jenkins unfortunately will overwrite any parameters defined at the folder level using this dynamic approach for
  // applitools. Therefore in order to support this workflow we need to mirror folder defined parameters here otherwise
  // they are erased completely from the release configuration.
  // See https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/release/
  if (projName.contains('insight/insight-brain/release')) {
    params += [
        booleanParam(name: 'hotfix',
            description: 'Check if this IQ Release is intended to be a hotfix e.g. urgent release to fix a severe bug' +
                '. (Used for internal metrics only.)'),
        stringParam(name: 'version', description: 'The version to release'),
        stringParam(name: 'nextVersion',
            description: 'The next SNAPSHOT version to use after the release. Optional as will be automatically be ' +
                'calculated if left blank.'),
        run(name:'snapshotBuild', filter: 'SUCCESSFUL', projectName: 'insight/insight-brain/master-snapshot',
            description: 'The snapshot build to release from.')
    ]
  }
  properties([
      copyArtifactPermission("/${projName}"),
      parameters(params)
  ])
}

void pushDockerImageIfDeployBranch() {
    if (!currentBuild.fullProjectName.contains("snapshot")) {
      echo 'Skipping build of docker image on release branch'
      return
    }
    def push = true
    // If the git repo branch name isn't main or the project name isn't snapshot, skip the image push.
    if (!isDeployBranch(env, 'main') || !currentBuild.fullProjectName.contains("snapshot")) {
        echo 'Skipping push of docker image for non-deploy branch or release'
        push = false
    }

    String iqVersion = getMavenProjectVersion('.')
    String imageVersion = "${iqVersion.split("-")[0]}-${env.BUILD_NUMBER}"
    echo "iqVersion:'${iqVersion}'"
    echo "buildnum: ${env.BUILD_NUMBER}"

    String imageName = 'iq/snapshot'
    String fullImage = "${sonatypeDockerRegistryId()}/${imageName}:${imageVersion}"

    dir("nexus-iq-server") {
        withSonatypeDockerRegistry() {
            String latest = "${sonatypeDockerRegistryId()}/${imageName}:latest"
            sh "docker buildx create --use"
            sh "docker buildx build --platform=linux/amd64,linux/arm64 --build-arg " +
                "SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} --build-arg " +
                "IQ_SERVER_VERSION=${iqVersion} " +
                (push ? " --push " : "") +
                " --tag ${latest} " +
                " --tag ${fullImage} ."
        }
    }

    if (push) {
        // Trigger downstream jobs for IQ
        String targetImage = "${sonatypeDockerRegistryId()}/iq/staging:${imageVersion}"
        build('job': 'ops/sonatype-lifecycle/docker-ops-nexus-iq-server/staging',
              parameters: [
                string(name: 'BASE_IMAGE', value: fullImage),
                string(name: 'TARGET_IMAGE', value: targetImage),
              ],
              propagate: false)

        build('job': 'ops/sonatype-lifecycle/ops-terraform-ecs-iq-server/staging',
              parameters: [
                string(name: 'environment', value: 'Staging'),
                string(name:'imageUrl', value: targetImage)
              ],
              propagate: false)
    }
}

void pushMTIQDockerImage() {
    // MTIQ image push rules:
    // - any snapshot build on the `main` branch
    //   - Note IQ on-prem release builds should not be processed
    //   - Note fast builds never run on `main`
    // - any branch ending with `_mtiq`
    // - any branch run manually with the parameter to push selected
    // Note: there is a cleanup policy on RSC to purge old MTIQ feature branches images.

    boolean isMainBuild = isDeployBranch(env, 'main')

    if (currentBuild.fullProjectName.contains("insight-brain/release")) {
        echo 'Skipping MTIQ docker image for IQ on-premise release'
        return
    }

    // Default version for the MTIQ image off the `main` branch is in the format 202307111354-1234-ABCDEFGH

    // First part of MTIQ version number (202307111354 in the example) is an unformatted date up to the minute
    def dateSection = new Date().format("yyyyMMddHHmm", TimeZone.getTimeZone('UTC'))

    // Second part of the MTIQ version number (1234 in the example) is the Jenkins snapshot build number
    def buildNumSection = env.BUILD_NUMBER

    // Third part of the MTIQ version number (ABCDEFGH in the example) is the Git short hash
    def gitShortHashSection = env.GIT_COMMIT.take(8)

    def imageVersion = "${dateSection}-${buildNumSection}-${gitShortHashSection}"

    // If we are on a feature branch (i.e. not `main`), then we use the branch name in the version number
    // as well as prefixing it with `branch-` to allow for easy identification
    if (!isMainBuild) {
      // get branch name, max 20 characters
      String branch = gitBranch(env).replaceAll(/[^\w.-]/, '_').take(30)
      imageVersion = "branch-${branch}-${env.BUILD_NUMBER}"
    }

    echo "MTIQ image version: ${imageVersion}"

    dir("nexus-mtiq-server") {
      withSonatypeDockerRegistry() {
        // Push for all `main` builds as well as any enabled branches by name or build parameter
        def pushMtiqImage = params.mtiqImagePushEnabled == null
            ? (isMainBuild || projName.endsWith('_mtiq')) : params.mtiqImagePushEnabled
        echo "pushMtiqImage: $pushMtiqImage"
        def pushOption = ""
        if (pushMtiqImage) {
          pushOption = " --push "
        }

        String fullImage = "${sonatypeDockerRegistryId()}/mtiq/server:${imageVersion}"
        sh "docker buildx create --use"
        sh "docker buildx build --platform=linux/amd64,linux/arm64 " +
            " --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} " +
            pushOption +
            " --tag ${fullImage} ."
      }
    }

    // Successful builds on the `main` branch trigger the MTIQ job to bump the image version in the K8S deployment
    def isSuccess = currentBuild.currentResult == 'SUCCESS'
    if (isMainBuild && isSuccess) {
      build('job': '/insight/MTIQ/bump-mtiq-version',
          parameters: [ string(name: 'DOCKER_IMAGE_VERSION', value: imageVersion), string(name: 'IQ_COMMIT', value:
              env.GIT_COMMIT) ],
          wait: false,
          propagate: false)
    }
}

void runAllTests(Map<String, ?> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J) {
  echo "fastBuild disabled - Running all tests"
  buildAndTest(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)

  if (isFastBuild()) {
    runSafely 'zip --symlinks -q -r m2.zip .zion/repository/*'
    runSafely 'find . \\( -type d \\( -name "test-classes" -o -name "classes" \\) -o -type f -name "pom.xml" -o -path "*/src/*" \\) ! -path "./insight-brain-frontend/target/*" ! -path "./insight-brain-functional-test-common/target/*" -print | zip -q -r iq-tests.zip -@'

    archiveArtifacts(artifacts: 'm2.zip, iq-tests.zip', fingerprint: false)
  }
  else {
    runSafely 'zip --symlinks -q -r workspace.zip . '
    archiveArtifacts(artifacts: 'workspace.zip', fingerprint: false)
  }

  parallel(getParallelTests())
}

private String addBuildCacheOptions(String mavenOptions, boolean enabled) {
  mavenOptions += " -Dmaven.build.cache.remote.enabled=${enabled}"
  mavenOptions += " -Dmaven.build.cache.remote.save.enabled=${enabled}"

  if (enabled) {
    mavenOptions += ' -Dmaven.build.cache.remote.url=https://repo.sonatype.com/repository/insight-brain-build-cache'
    mavenOptions += ' -Dmaven.build.cache.remote.server.id=insight-brain-build-cache'
  }

  return mavenOptions
}

Map<String, Closure> getParallelTests() {
  Map<String, Closure> testStages = [:]

  String[] zips = ['workspace.zip']
  if (isFastBuild()) {
    zips = ['m2.zip', 'iq-tests.zip']
  }

  if (!isFastBuild()) {
    testStages << createFunctionalTests('Java Functional Tests A', '.*/[A-B].*Test.class', zips)
    testStages << createFunctionalTests('Java Functional Tests B', '.*/[C-E].*Test.class', zips)
    testStages << createFunctionalTests('Java Functional Tests C', '.*/[F-M].*Test.class', zips)
    testStages << createFunctionalTests('Java Functional Tests D', '.*/[N-O].*Test.class', zips)
    testStages << createFunctionalTests('Java Functional Tests E', '.*/[P-R].*Test.class', zips)
    testStages << createFunctionalTests('Java Functional Tests F', '.*/[S-Z].*Test.class', zips)
    testStages << createMtiqFunctionalTests('MTIQ Functional Tests', '.*/.*Test.class', zips)
    testStages << createFrontendTests('Frontend Tests - Jasmine/Jest', zips)

    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 A', 'OpenJDK 17', '.*/[A-C].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 B', 'OpenJDK 17', '.*/[D-K].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 C', 'OpenJDK 17', '.*/[L-P].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 D', 'OpenJDK 17', '.*/[R-Z].*Test.class', zips)
    testStages << createMtiqUnitTests('MTIQ Unit and Integration Tests - OpenJDK 17', 'OpenJDK 17', zips)
  }
  else {
    // These tests make use of iq-tests.zip which does not include insight-brain-frontend (1.2GB)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 A', 'OpenJDK 17', '.*/[A].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 B', 'OpenJDK 17', '.*/[B].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 D-G', 'OpenJDK 17', '.*/[D-G].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 H-J', 'OpenJDK 17', '.*/[H-K].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 L-N', 'OpenJDK 17', '.*/[L-N].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 O-P', 'OpenJDK 17', '.*/[O-P].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 R + T', 'OpenJDK 17', '.*/[RT].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 S', 'OpenJDK 17', '.*/[S].*Test.class', zips)
    testStages << createUnitTests('Unit and Integration Tests - OpenJDK 17 C + U-Z', 'OpenJDK 17', '.*/[CU-Z].*Test.class', zips)
    testStages << createMtiqUnitTests('MTIQ Unit and Integration Tests - OpenJDK 17', 'OpenJDK 17', zips) //5:35
  }


  return testStages
}

Map<String, Closure> createFunctionalTests(
    String stageName,
    String regex,
    String... zipFiles
) {
  return createFunctionalTests(stageName, regex, false, zipFiles)
}

Map<String, Closure> createMtiqFunctionalTests(
    String stageName,
    String regex,
    String... zipFiles
) {
  return createFunctionalTests(stageName, regex, true, zipFiles)
}

Map<String, Closure> createFunctionalTests(
  String stageName,
  String regex,
  boolean mtiq,
  String... zipFiles
) {
  String mavenModule = 'insight-brain-java-functional-test'
  if (mtiq) {
    mavenModule = 'nexus-mtiq-functional-test'
  }

  return ["${stageName}": {
    node('iq-large') {
      stage(stageName) {
        try {
          copyRepo(zipFiles)
          String mavenOptions = "'-Dit.test=%regex[${regex}]'"
          mavenOptions += " -Dbrowser=chrome"
          mavenOptions += " -Ddocker.registry=${sonatypeDockerRegistryId()}"
          mavenOptions += " -DdetectTestEntityLeaks"
          mavenOptions += " -Dfailsafe.rerunFailingTestsCount=2"
          mavenOptions += " -Dfailsafe.failOnFlakeCount=5"
          mavenOptions += " --threads 4"
          Map<String, ?> testConfig = testConfig(mavenOptions, "${mavenModule}/pom.xml")
          // We just want to execute tests so directly invoke goals.
          mvn testConfig, 'failsafe:integration-test failsafe:verify'
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createFrontendTests(String stageName, String... zipFiles) {
  return ["${stageName}": {
    // 2024-05-03: We are using c6i.2xlarge EC2 instances. I tried c6i.4xlarge, there was a small difference (~2 mins),
    // but this stage is still faster then other parallel stages without using larger EC2 instances.
    node(InsightConstants.AGENT_LABEL){
      stage(stageName) {
        try {
          copyRepo(zipFiles)
          Map<String, ?> testConfig = testConfig(
              "-pl 'com.sonatype.insight.brain:insight-brain-frontend' " +
                  "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage --threads 4",
              null,  'OpenJDK 17')
          mvn testConfig, "com.github.eirslett:frontend-maven-plugin:yarn@jasmine " +
              "com.github.eirslett:frontend-maven-plugin:yarn@jest"
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createUnitTests(String stageName, String jdk, String regex, String... zipFiles) {
  return ["${stageName}": {
    node('iq-large'){
      stage(stageName) {
        try {
          copyRepo(zipFiles)
            Map<String, ?> testConfig = testConfig(
                // Note MTIQ & Frontend modules are excluded here as they run in their own stages
                addBuildCacheOptions("-pl '!nexus-mtiq-server' -pl '!insight-brain-frontend' " +
                "-Dtest=%regex[${regex}] -Dit.test=%regex[${regex}] " +
                "-Dskip-functional-test -DdetectTestEntityLeaks " +
                "-Dfailsafe.rerunFailingTestsCount=2 -Dfailsafe.failOnFlakeCount=5 " +
                "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage --threads 4", false),
                null, jdk)
          mvn testConfig, 'surefire:test failsafe:integration-test failsafe:verify'
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

Map<String, Closure> createMtiqUnitTests(String stageName, String jdk, String... zipFiles) {
  return ["${stageName}": {
    node('iq-large'){
      stage(stageName) {
        try {
          copyRepo(zipFiles)
          Map<String, ?> testConfig = testConfig(
              addBuildCacheOptions("-pl com.sonatype.insight.brain:nexus-mtiq-server -Dskip-functional-test -DdetectTestEntityLeaks " +
                    "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage " +
                    "-Dfailsafe.rerunFailingTestsCount=2 -Dfailsafe.failOnFlakeCount=5 --threads 4", false),
                null, jdk)
          mvn testConfig, 'surefire:test failsafe:integration-test failsafe:verify'
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]
}

void copyRepo(String... zipFiles) {
  for (String zipFile : zipFiles) {
    copyArtifacts(projectName: currentBuild.fullProjectName, filter: zipFile, selector: specific(currentBuild.id),
        flatten: false)
    runSafely "unzip -q -o ${zipFile}"
  }
}

Map<String, ?> testConfig(String mavenOptions, String pomFile = null, String javaVersion = 'OpenJDK 17') {
  return mavenCommon(javaVersion: javaVersion, mavenVersion: 'Maven 3.9.x', useEventSpy: false,
      pomFile: pomFile, mavenOptions: mavenOptions)
}

void captureResultsAndCleanup() {
  archiveArtifacts(artifacts: '**/target/*-reports/**', excludes: '**/TEST-*.xml, **/*-output.txt')
  collectTestResults(['**/target/*-reports/*.xml'])
  deleteDir()
}

boolean isFastBuild() {
  return !isDeployBranch(env, 'main') && params.fastBuild
}

boolean shouldRunPolicyEvaluation() {
  return !isFastBuild() || hasDependenciesChanged()
}

boolean hasDependenciesChanged() {
  return currentBuild.changeSets?.find() { ChangeLogSet<? extends Entry> changeSet ->
    changeSet.items.find() { GitChangeSet item ->
      item.getAffectedPaths().find() { String path ->
        if (!path.contains('/test/data/') &&
            (path.contains('pom.xml')
            || path.contains('package.json')
            || path.contains('yarn.lock'))) {
          return true
        }
      }
    }
  }
}
