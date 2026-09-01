pipeline {
    agent any

    environment {
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_TOKEN = credentials('sonar-token')
        DOCKER_IMAGE = 'intern-app'
        APP_PORT = '8080'
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/MNSkyWalker/InternBridge.git', branch: 'master'
            }
        }

        stage('Build & Test') {
            steps {
                dir('demo') {
                    sh 'mvn clean compile package'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    dir('demo') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Deploy to Nexus') {
            steps {
                dir('demo') {
                    sh 'mvn deploy'
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                        cp demo/target/*.jar ./app.jar
                        docker build -t ${DOCKER_IMAGE}:latest .
                        docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:${BUILD_NUMBER}
                    '''
                }
            }
        }

        stage('Deploy Application') {
            steps {
                script {
                    sh '''
                        docker stop intern-app || true
                        docker rm intern-app || true
                        docker run -d --name intern-app -p ${APP_PORT}:${APP_PORT} ${DOCKER_IMAGE}:latest
                        sleep 10
                        docker ps | grep intern-app
                    '''
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    sh '''
                        curl -f http://localhost:${APP_PORT}/actuator/health || echo "Health check not available"
                        echo "Application running at: http://localhost:${APP_PORT}"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
            echo 'Application is running at: http://localhost:8080'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}
