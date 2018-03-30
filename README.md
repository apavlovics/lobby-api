# Evolution Gaming Test Assignment

This sample application implements a server for a JSON-based protocol over WebSocket.

## System Requirements

This application is tested to work with:

* Java 1.8
* Scala 2.12.5
* sbt 0.13

## Configuration

The default configuration is stored in the *src/main/resources/application.conf* file.

## Testing

To test this application, go to its root folder and execute:

    sbt test

## Running

To run this application, go to its root folder and execute:

    sbt run

The server starts at http://localhost:9000/ws_api. It accepts the user "admin" with the password "admin" via HTTP basic authentication. There is a simple WebSocket client in the *client* folder that you can use to manually test the server. The client works in Chrome and Firefox.
