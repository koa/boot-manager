#!/usr/bin/env groovy

properties([
  disableConcurrentBuilds(),
  buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '1', numToKeepStr: '3')),
  parameters([
  	choice(defaultValue: "build", choices: ["build", "release", "update-dependencies"].join("\n"), description: '', name: 'build')
  ]),
  pipelineTriggers([cron('H 23 * * *')])
])

node {
   checkout scm
   checkout([$class: 'GitSCM',
       extensions: [[$class: 'CleanCheckout'],[$class: 'LocalBranch', localBranch: "master"]]])
   def mvnHome
   stage('Preparation') {
      mvnHome = tool 'Maven 3.5.2'
   }
   configFileProvider(
        [configFile(fileId: 'MyGlobalSettings', variable: 'MAVEN_SETTINGS')]) {
	   stage('Update Dependencies'){
	        sh "'${mvnHome}/bin/mvn' -U versions:use-next-releases versions:use-releases"
	        sh "'${mvnHome}/bin/mvn' scm:checkin -Dmessage='resolved versions'"
	   }
	   stage('Build') {
	     if(params.build=='release'){
	       sh "'${mvnHome}/bin/mvn' -Dresume=false release:prepare release:perform"     
	     }else if (params.build != 'update-dependencies'){
	       sh "'${mvnHome}/bin/mvn' clean deploy -DperformRelease=true"
	     }
   }
   }
   stage('Results') {
      archive 'target/*.jar'
   }
}
