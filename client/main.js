const outgoingMessageTypeSelect = $("#outgoing-message-type")
const outgoingMessageTextArea = $("#outgoing-message")
const incomingMessagesList = $("#incoming-messages")

function updateOutgoingMessage() {
  const outgoingMessageType = outgoingMessageTypeSelect.children("option:selected").val()
  let message
  switch(outgoingMessageType) {
    case "login":
      message =
`{
  "$type": "login",
  "username": "admin",
  "password": "admin"
}`
      break
    case "ping":
      message =
`{
  "$type": "ping",
  "seq": 12345
}`
      break
    case "subscribe_tables":
      message =
`{
  "$type": "subscribe_tables"
}`
      break
    case "unsubscribe_tables":
      message =
`{
  "$type": "unsubscribe_tables"
}`
      break
    case "add_table":
      message =
`{
  "$type": "add_table",
  "after_id": 2,
  "table": {
    "name": "table - Foo Fighters",
    "participants": 4
  }
}`
      break
    case "update_table":
      message =
`{
  "$type": "update_table",
  "table": {
    "id": 1,
    "name": "table - Pink Floyd",
    "participants": 4
  }
}`
      break
    case "remove_table":
      message =
`{
  "$type": "remove_table",
  "id": 2
}`
      break
    default:
      console.log(`Outgoing message type ${outgoingMessageType} is invalid`)
      break
  }
  outgoingMessageTextArea.val(message)
}
outgoingMessageTypeSelect.change(updateOutgoingMessage)
$(updateOutgoingMessage)

const webSocket = new WebSocket("ws://localhost:9000/lobby_api")
webSocket.onopen = () => {
  const sendButton = $("#send")
  sendButton.removeAttr("disabled")
  sendButton.click(() => {
    const outgoingMessageFormat = $("input[name=outgoing-message-format]:checked").val()
    switch (outgoingMessageFormat) {
      case "text":
        webSocket.send(outgoingMessageTextArea.val())
        break
      case "binary":
        webSocket.send(new Blob([outgoingMessageTextArea.val()]))
        break
      default:
        console.log(`Outgoing message format ${outgoingMessageFormat} is invalid`)
        break
    }
  })
  incomingMessagesList.prepend($("<li>Connected via WebSocket</li>"))
}
webSocket.onerror = error => {
  console.log("Issue with WebSocket connection", error)
}
webSocket.onmessage = event => {
  if (typeof event.data === "string") {
    incomingMessagesList.prepend($(`<li>${event.data}</li>`))
  } else {
    console.log(`Data ${event.data} is not supported`)
  }
}
