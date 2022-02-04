/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _

configureBranchJob()
make(
    useEventSpy: false,
    javaVersion: 'Java 8',
    mavenVersion: 'Maven 3.6.x',
    mavenOptions: '-D skipTests -D skip-functional-test',
    snapshotBuildAndTest: { Map<String, String> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J ->
      runAllTests(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
    },
    releaseBuildAndTest: { Map<String, String> mavenCommon, String keystoreCredId, boolean useInstall4J ->
      runAllTests(mavenCommon, keystoreCredId, false, useInstall4J)
    },
    runFeatureBranchPolicyEvaluations: true,
    iqPolicyEvaluation: { stage ->
        nexusPolicyEvaluation iqStage: stage, iqApplication: 'insight-brain',
          iqScanPatterns: [[scanPattern: 'insight-brain-frontend/target/webpack-modules']],
          failBuildOnNetworkError: true
    },
    distFiles: [
      includes: [
        'nexus-iq-server/target/*.zip*',
        'nexus-iq-server/target/*.tar.gz*',
        'nexus-iq-cli/target/*.jar*',
        'nexus-iq-diagnostics/target/*.jar*'
      ],
      excludes: [
        '**/*-sources.jar*',
        '**/*-javadoc.jar*',
        '**/*proguard*',
        '**/original-*'
      ]
    ],
    usePMD: true,
    useCheckstyle: true,
    releaseRetentionPolicy: RetentionPolicy.TEN_BUILDS,
    onSuccess: {
        pushDockerImageIfDeployBranch()
    },
    onUnstable: {
        pushDockerImageIfDeployBranch()
        createJiraIssueIfNeeded() 
    },
    onFailure: {
      createJiraIssueIfNeeded()
    }
)

/**
 * On unstable builds of the main branch, create a Jira ticket.
 */
def createJiraIssueIfNeeded() {
  // Only create the ticket on main branch failures.
  if (currentBuild.fullProjectName.contains('master') && getTestResults(currentBuild).failCount) {
    echo "The build is entering the unstable condition to create a jira ticket, " +
        "current build #: ${currentBuild.displayName} ${currentBuild.number}"

    List commits = getBuildRecentCommits(currentBuild)
    String commitMsg = ''
    if (commits.size() > 0) {
      commitMsg = commits[0].replace("\n", "\\n")
    }
    String desc = "This build had a failure. Please fix the failure and rebuild.\n" +
        "The latest commit is: ${commitMsg ?: 'unknown'}"
    Map issue = [fields: [project    : [key: 'CLM'],
                          labels     : ['master-build-failure'],
                          summary    : "Failure of Insight Brain main branch build ${currentBuild.displayName}",
                          description: desc,
                          issuetype  : [name: 'Bug']]]

    def newIssue = jiraNewIssue issue: issue, site: 'issues.sonatype.com'
    echo "New issue created: ${newIssue.data.key}"
  }
  else if (currentBuild.currentResult != 'SUCCESS') {
    echo "The build is unstable but this is not on the master branch - no jira ticket is created as a result."
  }
}

def configureBranchJob() {
    properties([copyArtifactPermission("/${currentBuild.fullProjectName}")])
}

def pushDockerImageIfDeployBranch() {
    //If the branch isn't master or the project name isn't snapshot, skip the image build and deploy.
    if (!isDeployBranch(env, 'master') || !currentBuild.fullProjectName.contains("snapshot")) {
        echo 'Skipping push of docker image for non-deploy branch or release'
        return
    }
    def version = getMavenProjectVersion('.')
    dir("nexus-iq-server") {
        withSonatypeDockerRegistry() {
            String shortImage = "iq/snapshot:${version.split("-")[0]}-${env.BUILD_NUMBER}"
            sh "docker build --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} --build-arg IQ_SERVER_VERSION=${version} --tag ${shortImage} ."
            String fullImage = "${sonatypeDockerRegistryId()}/${shortImage}"
            String latest = "${sonatypeDockerRegistryId()}/iq/snapshot:latest"
            runSafely "docker tag ${shortImage} ${fullImage}"
            runSafely "docker push ${fullImage}"
            // Also tag as latest
            runSafely "docker tag ${shortImage} ${latest}"
            runSafely "docker push ${latest}"
        }
    }
}

def runAllTests(Map<String, String> mavenCommon, String keystoreCredId, boolean deployToRepo, boolean useInstall4J) {
  buildAndTest(mavenCommon, keystoreCredId, deployToRepo, useInstall4J)
  // archive things for the parallel blocks which will copy
  // these artifacts to different agents for each parallel block
  runSafely 'zip --symlinks -q -r workspace.zip .'
  archiveArtifacts(artifacts: 'workspace.zip', fingerprint: false)
  parallel(parallelTests())
}

Map<String, Closure> parallelTests() {
  Map<String, Closure> blocks = [:]

  blocks << ['Geb Tests': {
    node(InsightConstants.AGENT_LABEL){
      stage("Geb Tests") {
        try {
          gebTests()
        }
        finally {
          captureResultsAndCleanup()
        }
      }
    }
  }]

  ['A': '.*/[A-H].*Test.class', 'B': '.*/[I-R].*Test.class', 'C': '.*/[S-Z].*Test.class'].each { String label, String regex ->
    def blockName = "Java Functional Tests - (Chrome) ${label}"
    blocks << ["${blockName}": {
      node(InsightConstants.AGENT_LABEL){
        stage(blockName) {
          try {
            withEnv(["APPLITOOLS_BATCH_ID=${env.GIT_COMMIT}"]) {
              functionalTests('chrome', regex)
            }
          }
          finally {
            captureResultsAndCleanup()
          }
        }
      }
    }]
  }

  ['A': '.*/[A-H].*Test.class', 'B': '.*/[I-P].*Test.class', 'C': '.*/[Q-Z].*Test.class'].each { String label, String regex ->
    ['Java 8','OpenJDK 11'].each {String jdk ->
        def blockName = "Unit and Integration Tests - ${jdk} ${label}"
        blocks << ["${blockName}": {
          node(InsightConstants.AGENT_LABEL){
            stage(blockName) {
              try {
                unitTests(regex, jdk)
              }
              finally {
                if (jdk == 'Java 8' && label == 'A') {
                  sonarAnalyze(env: env, sonarAnalysisPullRequestsOnly: currentBuild.projectName != 'master')
                }
                captureResultsAndCleanup()
              }
            }
          }
        }]
      }
  }

  return blocks
}

Map<String, String> testConfig(String mavenOptions, String pomFile = null, String javaVersion = 'Java 8') {
  return mavenCommon(javaVersion: javaVersion, mavenVersion: 'Maven 3.6.x', useEventSpy: false,
      pomFile: pomFile, mavenOptions: mavenOptions)
}

def copyRepo() {
  copyArtifacts(projectName: currentBuild.fullProjectName, filter: 'workspace.zip', selector: specific(currentBuild.id),
      flatten: false)
  runSafely 'unzip -q workspace.zip'
}

def gebTests() {
  copyRepo()
  String mavenOptions = "-Dgeb.env=ci -Drun-functional-tests=docker -Ddocker.registry=${sonatypeDockerRegistryId()}"
  Map<String, String> testConfig = testConfig(mavenOptions, 'insight-brain-functional-test/pom.xml')
  mvn testConfig, 'verify'
}

def functionalTests(String browser, String testRegex) {
  copyRepo()
  withCredentials([string(credentialsId: 'APPLITOOLS_KEY', variable: 'applitoolsKey')]) {
    String mavenOptions = "'-Dit.test=%regex[${testRegex}]'"
    mavenOptions += ' -Drun-functional-tests=docker'
    mavenOptions += " -Dbrowser=${browser}"
    mavenOptions += " -DapplitoolsKey=${applitoolsKey}"
    mavenOptions += " -Ddocker.registry=${sonatypeDockerRegistryId()}"
    Map<String, String> testConfig = testConfig(mavenOptions, 'insight-brain-java-functional-test/pom.xml')
    mvn testConfig, 'verify'
  }
}

def unitTests(String testRegex, String javaVersion = 'Java 8') {
  copyRepo()
  Map<String, String> testConfig = testConfig(
      "-Dtest=%regex[${testRegex}] -Dit.test=%regex[${testRegex}] -Dskip-functional-test " +
          "-Ddocker.registry=${sonatypeDockerRegistryId()} -Pbuildsupport-sonar-coverage",
      null, javaVersion)
  mvn testConfig, 'install'
}

def captureResultsAndCleanup() {
  archiveArtifacts(artifacts: '**/target/*-reports/**', excludes: '**/*.xml, **/*-output.txt')
  collectTestResults(['**/target/*-reports/*.xml'])
  deleteDir()
}
