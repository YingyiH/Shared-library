// Named it "call" to call it by default

def call(dockerRepoName, path, imageName) {
    pipeline {
        //Starts defining a Jenkins pipeline and sets it to run on any available agent
        agent any 
        environment {
            PATH = "/var/lib/jenkins/.local/bin:$PATH"
        }
        // Add a boolean parameter to enable/disable the Delivery stage of the pipeline. 
        // This should go between the agent and stages keywords.
        parameters {
            booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
        }
        
        stages {

            stage('Environment Set up'){
                steps {
                    script {
                        sh "rm -rf venv"
                        // Python virtual environment set up
                        sh "python3 -m venv venv"
                        sh ". venv/bin/activate"
                        // Python package upgrade
                        sh "pip install --upgrade pip"
                        sh "pip install --upgrade flask"
                        // Python lint and Bandit installation
                        sh "pip install pylint"
                        sh "pip install bandit"
                    }
                }
            }

            stage('Lint') {
                steps {
                    script {
                        // Get in Python virtual environment
                        sh ". venv/bin/activate"
                        // Set up pylint minimum score
                        sh "pylint --fail-under=5 --disable import-error ./${path}/*.py"
                    }
                }
            }
            
            stage('Security Scan') {
                steps {
                    script{
                        // Get in Python virtual environment
                        sh ". venv/bin/activate"
                        // Runs Bandit to perform security analysis on the code.
                        sh "bandit -r ./${path}"
                    }
                }
            }
            
            stage('Package') {
                when {
                    expression {env.GIT_BRANCH == 'origin/main'}
                }
                steps {
                    // Inject credentials securely into the pipeline
                    withCredentials([string(credentialsId: 'DockerHub', variable: 'TOKEN')]) {
                        script {
                            // Locate path -> Docker login -> Build and push image for service
                            sh """
                                cd ./${path}/
                                docker login -u 'yingyi123' -p '$TOKEN' docker.io
                                docker build -t ${dockerRepoName}:latest --tag yingyi123/${dockerRepoName}:${imageName} .
                                docker push yingyi123/${dockerRepoName}:${imageName}
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
                        sh 'ssh -t -t doridori@35.235.112.242 -o StrictHostKeyChecking=no "cd ./BESTIE-Commerce-API/deployment && docker compose stop && docker compose rm -f && docker compose pull && docker compose up --build -d"'
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