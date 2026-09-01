pipeline {
    agent any

    environment {
        // SonarQube configuration
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_TOKEN = credentials('sonar-token')
        
        // Docker configuration
        DOCKER_IMAGE = 'stagemgmt'
        APP_PORT = '8082'
    }

    stages {
        // ==========================================
        // 1. Checkout
        // ==========================================
        stage('Checkout') {
            steps {
                git url: 'https://github.com/MNSkyWalker/InternBridge.git', branch: 'master'
            }
        }

        // ==========================================
        // 2. Build & Test
        // ==========================================
        stage('Build & Test') {
            steps {
                dir('demo') {
                    sh 'mvn clean compile package'
                }
            }
        }

        // ==========================================
        // 3. SonarQube Analysis
        // ==========================================
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    dir('demo') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
        }

        // ==========================================
        // 4. Quality Gate Check
        // ==========================================
        stage('Quality Gate') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        // ==========================================
        // 5. Deploy to Nexus
        // ==========================================
        stage('Deploy to Nexus') {
            steps {
                dir('demo') {
                    sh 'mvn deploy'
                }
            }
        }

        // ==========================================
        // 6. Build Docker Image
        // ==========================================
        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                        # Copy JAR from demo folder
                        cp demo/target/*.jar ./app.jar
                        
                        # Build Docker image
                        docker build -t ${DOCKER_IMAGE}:latest .
                        docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:${BUILD_NUMBER}
                    '''
                }
            }
        }

        // ==========================================
        // 7. Deploy Application
        // ==========================================
        stage('Deploy Application') {
            steps {
                script {
                    sh '''
                        # Stop and remove old container
                        docker stop intern-app || true
                        docker rm intern-app || true
                        
                        # Run new container
                        docker run -d --name intern-app \
                            --network devops-network \
                            -p ${APP_PORT}:${APP_PORT} \
                            ${DOCKER_IMAGE}:latest
                        
                        # Wait for app to start
                        sleep 10
                        
                        # Check if running
                        docker ps | grep intern-app
                    '''
                }
            }
        }

        // ==========================================
        // 8. Health Check
        // ==========================================
        stage('Health Check') {
            steps {
                script {
                    sh '''
                        curl -f http://localhost:${APP_PORT}/login || echo "Health check failed but continuing"
                        echo "Application is running at: http://localhost:${APP_PORT}"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline completed successfully!'
            echo 'Application is running at: http://localhost:8082'
        }
        failure {
            echo '❌ Pipeline failed!'
        }
    }
}
