// Named it "call" to call it by default

def call(path, imageName) {
    pipeline {
        //Starts defining a Jenkins pipeline and sets it to run on any available agent
        agent any 

        // Add a boolean parameter to enable/disable the Delivery stage of the pipeline. 
        // This should go between the agent and stages keywords.
        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
        }
        stages {

            stage('Environment Set up'){
                steps {
                    script {
                        // Python virtual environment set up
                        sh '''
                            python3 -m venv venv
                            . venv/bin/activate
                        '''
                        // Python package upgrade
                        sh '''
                            pip install --upgrade pip
                            pip install --upgrade flask
                        '''
                        // Python lint installation
                        sh 'pip install pylint'
                    }
                }
            }

            stage('Lint') {
                steps {
                    script {
                        // Get in Python virtual environment
                        sh '. venv/bin/activate'
                        // Set up pylint minimum score
                        sh 'pylint --fail-under=5'
                    }
                }
            }
            
            stage('Security Scan') {
                steps {
                    script{
                        // Get in Python virtual environment
                        sh '. venv/bin/activate'
                        // Runs Bandit to perform security analysis on the code.
                        sh 'bandit -r ./${path}'
                    }
                }
            }
            
            stage('Package') {
                when {
                    
                    expression { env.GIT_BRANCH == 'origin/main' }
                }
                steps {
                    // Inject credentials securely into the pipeline
                    withCredentials([string(credentialsId: 'Dockerhub', variable: 'TOKEN')]) {
                        script {
                            sh 'cd ${path}'
                            sh "docker login -u 'yingyi123' -p '$TOKEN' docker.io"
                            // Build and push image for service
                            sh '''
                                docker build -t ${path}:latest --tag yingyi123/${path}:${imageName} .
                                docker push yingyi/${path}:${imageName}
                            '''
                        }
                    }    
                }
            }
            
            stage('Deploy') {
                when {
                    // The Deliver stage will now only run if the DEPLOY parameter is set to true
                    expression { params.DEPLOY }
                }
                steps {
                    script {
                        // Starts an SSH agent, allowing SSH commands to be executed securely within the pipeline 
                        // using the specified SSH key credentials.
                        sshagent(credentials : ['ssh-key']) {
                            // Executes a series of Docker commands on a remote server via SSH. It pulls, and then 
                            // rebuilds the Docker containers specified in the 'docker-compose.yml' file located in 
                            // the '/deployment' directory.
                            sh '''
                                sh 'ssh -t -t yhe@34.106.187.98 -o StrictHostKeyChecking=no "cd /deployment"'
                                sh 'ssh -t -t yhe@34.106.187.98 -o StrictHostKeyChecking=no "docker compose stop"'
                                sh 'ssh -t -t yhe@34.106.187.98 -o StrictHostKeyChecking=no "docker compose rm -f"'
                                sh 'ssh -t -t yhe@34.106.187.98 -o StrictHostKeyChecking=no "docker compose pull"'
                                sh 'ssh -t -t yhe@34.106.187.98 -o StrictHostKeyChecking=no "docker compose up --build -d"'
                            '''
                        }
                    }
                }
            }

            stage ('Clean up') {
                steps {
                    when {
                        always()
                    }
                    script {
                        // Clean up Python virtual environment
                        sh 'rm -rf venv'
                    }
                }
            }
        }
    }
}
