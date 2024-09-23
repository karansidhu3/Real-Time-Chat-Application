# Real-Time Chat Application

This is a real-time chat application built using both **TCP** and **UDP** protocols. It allows users to register, manage a buddy list, send messages, and update their statuses. The application is designed to communicate directly between clients for real-time messaging and status updates.

[Link to Video Demo](https://drive.google.com/file/d/1htMtEBektKSCJs5Y-cYGsuKZL9G3HqtD/view?usp=sharing)

## Features
- **User Registration**: Users can register with the server.
- **Buddy List Management**: Add or delete buddies.
- **Status Updates**: Update your status and check the status of your buddies.
- **Messaging**: Send and receive messages in real-time using TCP.
- **Status Checking**: Use UDP for quick status updates and requests.

## Technologies Used
- **TCP**: For user registration, buddy list updates, and messaging.
- **UDP**: For status updates and buddy status requests.
- **Multithreading**: To handle multiple tasks (like TCP/UDP handling) concurrently.

## How It Works
1. **Server**:
   - The server handles TCP connections for user registration, managing buddy lists, and message exchange.
   - It also uses UDP to handle user status updates and buddy status checks.
  
2. **Client**:
   - Users can register, login, and update their status.
   - After logging in, users can manage their buddy lists and send messages to buddies.
   - The client uses a TCP connection for messaging and UDP for status updates.

### Protocol Details
- **TCP (Transmission Control Protocol)**:
  - Reliable connection-based communication.
  - Used for user registration, buddy list updates, and messaging.

- **UDP (User Datagram Protocol)**:
  - Connectionless communication for faster status updates.
  - Used to quickly check buddy statuses and send updates.
