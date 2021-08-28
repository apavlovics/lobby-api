# Lobby API

This sample application exposes a server for a JSON-based Lobby API over WebSocket. Think of the lobby as a dynamic ordered collection of entities called tables. Lobby API users can subscribe to receive the current snapshot of tables and get notified once Lobby API admins add, update or remove them.

There are two implementations available: Akka-based one (see `akka` module) and Cats-based one (see `cats` module). Both implementations adhere to the same protocol, so from a client point of view they are equal.

## System Requirements

The application uses:

* Java 16
* Scala 2.13
* sbt 1.5

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

The server starts at http://localhost:9000/lobby_api by default. There is a simple WebSocket client in the *client* folder that you can use to manually test the server.

### Sample Messages

The following sample messages can be sent by both Lobby API users and admins.

To authenticate as a user:

```json
{
  "$type": "login",
  "username": "user",
  "password": "user"
}
```

To authenticate as an admin:

```json
{
  "$type": "login",
  "username": "admin",
  "password": "admin"
}
```

To ping the server:

```json
{
  "$type": "ping",
  "seq": 12345
}
```

To subscribe and receive the current snapshot of tables and notifications about any subsequent changes:

```json
{
  "$type": "subscribe_tables"
}
```

To unsubscribe and stop receiving notifications about table changes:

```json
{
  "$type": "unsubscribe_tables"
}
```

The following sample messages can be sent by Lobby API admins only.

To add a new table:

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

To update an existing table:

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

To remove an existing table:

```json
{
  "$type": "remove_table",
  "id": 2
}
```
