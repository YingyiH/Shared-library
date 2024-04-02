def call(path, imageName) {
    pipeline {
        agent any 
        
        // parameters {
        //     booleanParam(defaultValue: false, description: 'Deploy the App', name: 'DEPLOY')
        // }
        
        stages {
            stage('Environment Set up') {
                steps {
                    script {
                        sh '''
                            python3 -m venv venv
                            . venv/bin/activate
                            pip install --upgrade pip
                            pip install --upgrade flask
                            pip install pylint
                        '''
                    }
                }
            }

            stage('Lint') {
                steps {
                    script {
                        sh '. venv/bin/activate'
                        sh 'pylint --fail-under=5'
                    }
                }
            }
            
            stage('Security Scan') {
                steps {
                    script {
                        sh ". venv/bin/activate && bandit -r ./${path}"
                    }
                }
            }
            
            // stage('Package') {
            //     when {
            //         expression { env.GIT_BRANCH == 'origin/main' }
            //     }
            //     steps {
            //         withCredentials([string(credentialsId: 'Dockerhub', variable: 'TOKEN')]) {
            //             script {
            //                 sh "cd ${path} && docker login -u 'yingyi123' -p '$TOKEN' docker.io"
            //                 sh "docker build -t ${path}:latest --tag yingyi123/${path}:${imageName} ."
            //                 sh "docker push yingyi123/${path}:${imageName}"
            //             }
            //         }    
            //     }
            // }
            
            // stage('Deploy') {
            //     // when {
            //     //     expression { params.DEPLOY }
            //     // }
            //     steps {
            //         sshagent(credentials: ['ssh-key']) {
            //             sh '''
            //                 ssh -t -t doridori@34.106.187.98 -o StrictHostKeyChecking=no "cd /deployment &&
            //                 docker compose stop &&
            //                 docker compose rm -f &&
            //                 docker compose pull &&
            //                 docker compose up --build -d"
            //             '''
            //         }
            //     }
            // }

            // stage('Clean up') {
            //     steps {
            //         script {
            //             sh 'rm -rf venv'
            //         }
            //     }
            // }
        }
    }
}
