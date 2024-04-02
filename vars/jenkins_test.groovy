// Directory is path to service, also docker repo name
def call(dir, imageName) {
    pipeline {
        agent any
        environment {
            PATH = "/var/lib/jenkins/.local/bin:$PATH"
        }
        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name:'DEPLOY')
        }
        stages {
            stage('Setup') {
                steps {
                    script {
                        sh """
                            python3 -m venv venv
                            . venv/bin/activate
                            pip install pylint
                            pip install --upgrade pip
                            pip install --upgrade flask
                            pip install bandit
                        """
                    }
                }
            }
            stage('Lint') {
                steps {
                    script {
                        sh """
                            . venv/bin/activate
                            pylint --fail-under=5 --disable import-error ./${dir}/*.py
                        """
                    }
                }
            }
            stage('Security') {
                steps {
                    script {
                        sh """
                            . venv/bin/activate
                            bandit -r ./${dir}
                        """
                    }
                }
            }
            stage('Package') {
                when {
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    withCredentials([string(credentialsId: 'Dockerhub', variable: 'TOKEN')]) {
                        script {
                            sh """
                            cd ${dir}
                            docker login -u 'tristan007' -p '$TOKEN' docker.io
                            docker build -t ${dir}:latest --tag tristan007/${dir}:${imageName} .
                            docker push tristan007/${dir}:${imageName}
                        """
                        }
                    }
                }
            }
            stage('Deploy') {
                when {
                    expression { params.DEPLOY }
                }
                steps {
                    sshagent(credentials : ['ssh-key']) {
                        // https://stackoverflow.com/questions/18522647/run-ssh-and-immediately-execute-command - Run commands using quotes
                        sh 'ssh -t -t tristandavis888@34.118.240.191 -o StrictHostKeyChecking=no "cd ./kafka/deployment && docker compose stop && docker compose rm -f && docker compose pull && docker compose up --build -d"'
                    }
                }
                post {
                    always {
                        script {
                            sh 'rm -rf venv'
                        }
                    }
                }
            }
        }
    }
}