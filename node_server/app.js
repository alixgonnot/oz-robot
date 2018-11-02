var WebSocketServer = require('websocket').server;
var http = require('http');
var tabConnections = [];

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
  tabConnections.push(connection);
  console.log("new client is now connected");

  // This is the most important callback for us, we'll handle
  // all messages from users here.
  connection.on('message', function(message) {
    if (message.type === 'utf8') {
      console.log(message.utf8Data);
      var content = JSON.parse(message.utf8Data)
      console.log(content.val);

      tabConnections.forEach(function(client){
        client.sendUTF(message.utf8Data);
      });

      // process WebSocket message
    }
  });

  connection.on('close', function(connection) {
    // close user connection
  });
});
