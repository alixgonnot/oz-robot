var WebSocketServer = require('websocket').server;
var http = require('http');
var robot_socket = null;
var webclient_socket = null;

var server = http.createServer(function(request, response) {
  // process HTTP request. Since we're writing just WebSockets
  // server we don't have to implement anything.
});
server.listen(8080, function() {
  console.log("I AM ONLINE !!!!! ");
 });

// create the server
wsServer = new WebSocketServer({
  httpServer: server
});

// WebSocket server
wsServer.on('request', function(request) {
  var connection = request.accept(null, request.origin);
  console.log("new client is now connected");
  // This is the most important callback for us, we'll handle
  // all messages from users here.
  connection.on('message', function(message) {
    if (message.type === 'utf8') {
      console.log(message.utf8Data);
      var content = JSON.parse(message.utf8Data)
      console.log(content.val);

      if (content.sender == "robot"){
        robot_socket = connection;
        console.log("robot");
        if (webclient_socket != null){
          webclient_socket.sendUTF(message.utf8Data);
          console.log("message sent to webclient");
        }
      }
      else if (content.sender == "web_client"){
        webclient_socket = connection;
        console.log("webclient");
        if (robot_socket != null){
          robot_socket.sendUTF(message.utf8Data);
          console.log("message sent to robot");
        }
      }
    }
  });

  connection.on('close', function(connection) {
    // close user connection
  });
});
