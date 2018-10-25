//ouverture de la websocket
var ws = new WebSocket("ws://localhost:1337");

//code executé à l'ouverture de la socket
ws.onopen = function() {
  alert("Socket is open");
};

//code executé à la reception d'un message
ws.onmessage = function (evt) {
  var received_msg = evt.data;
  alert("Message is received...");
};

//code executé à la fermeture de la socket
ws.onclose = function() {

  // websocket is closed.
  //alert("Connection is closed...");
};

//envoi d'un message : ws.send("Message to send");
