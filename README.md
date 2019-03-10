# Evolution Gaming Lobby API

This sample application implements a server for a JSON-based lobby API over WebSocket.

## System Requirements

This application is tested to work with:

* Java 11
* Scala 2.12
* sbt 1.2.8

## Configuration

The default configuration is stored in the *src/main/resources/application.conf* file.

## Testing

To test this application, go to its root folder and execute:

    sbt test

## Running

To run this application, go to its root folder and execute:

    sbt run

The server starts at http://localhost:9000/ws_api. It accepts either "admin", "admin" or "user", "user" credentials. There is a simple WebSocket client in the *client* folder that you can use to manually test the server.
