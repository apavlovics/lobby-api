# Evolution Gaming Lobby API

This sample application exposes a server for a JSON-based lobby API over WebSocket. There are two
implementations available: Akka-based one (see `akka` module) and Cats-based one (see `cats` module).
Both implementations adhere to the same protocol, so from a consumer point of view they are equal.

## System Requirements

The application is tested to work with:

* Java 11
* Scala 2.13
* sbt 1.3

## Configuration

The default configuration is stored in the *application.conf* files.

## Testing

To test the application, go to its root folder and execute:

    sbt test

## Running

To run Akka-based implementation, execute:

    sbt akka/run

To run Cats-based implementation, execute:

    sbt cats/run

The server starts at http://localhost:9000/lobby_api by default. It accepts either "admin", "admin" or
"user", "user" credentials. There is a simple WebSocket client in the *client* folder that you can use to
manually test the server.

### Sample Messages

The following sample messages can be sent from the client to the server while both are running:

```json
{
  "$type": "login",
  "username": "admin",
  "password": "admin"
}
```

```json
{
  "$type": "ping",
  "seq": 12345
}
```

```json
{
  "$type": "subscribe_tables"
}
```

```json
{
  "$type": "add_table",
  "after_id": 2,
  "table": {
    "name": "table - Foo Fighters",
    "participants": 4
  }
}
```

```json
{
  "$type": "update_table",
  "table": {
    "id": 1,
    "name": "table - Pink Floyd",
    "participants": 4
  }
}
```

```json
{
  "$type": "remove_table",
  "id": 2
}
```

```json
{
  "$type": "unsubscribe_tables"
}
```
