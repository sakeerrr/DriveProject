# Drive Project with GCS Integration

A cloud-based file storage and sharing service, similar to Google Drive, built using Google Cloud Storage (GCS). This app supports user registration, login, secure file uploads and downloads, and file sharing with other registered users via email.

## Features

- **User Registration and Login**  
  Secure authentication system to register and log in users.

- **File Upload**  
  Users can upload files to their personal storage space using Google Cloud Storage.

- **File Download**  
  Users can download their uploaded files securely.

- **File Sharing**  
  Share files with other registered users by email address.

## Tech Stack

- **Backend:** Java SpringBoot / JavaScript 
- **Frontend:** HTML / CSS / Thymeleaf
- **Cloud Storage:** Google Cloud Storage (GCS)
- **Authentication:** Cookie-based sessions
- **Database:** MySQL

## Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/sakeerrr/DriveProject.git

2. **You should have a GCS credentials json key and cookies.txt file**

3. **Add all the mandatory credentials in the properties file to your running configuration**

4. **Start the SpringBoot project**


## Docker Compose Instructions
1. **Install Docker and Docker Compose**

2. **Create a .jar file of your project**
    ```bash
    ./mvnw clean package

3. **Create the Dockerfile and docker-compose.yaml file in the project directory and set them up. Create a .env file with 
the environmental variables**

4. **Create an image and a container**
    ```bash
    docker build -t drive-app
    docker-compose up --build
**NOTE**:
Temporarily stop your database server to prevent the servers from working on the same host :3306 !