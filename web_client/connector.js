//ouverture de la websocket
var ws = new WebSocket("ws://localhost:1337");
var mode = 0;

//code executé à l'ouverture de la socket
ws.onopen = function() {
  var fieldNameElement = document.getElementById('server_status');
  fieldNameElement.innerHTML = "<h3 style=\"color:green\">ONLINE</h3>";
};

//code executé à la reception d'un message
ws.onmessage = function (evt) {
  var received_msg = evt.data;
  alert("Message is received...");
};

//code executé à la fermeture de la socket
ws.onclose = function() {
  // websocket is closed.
  var fieldNameElement = document.getElementById('server_status');
  fieldNameElement.innerHTML = "<h3 style=\"color:red\">OFFLINE</h3>";
};

function sendCommand(cmd){
  var obj_cmd = new Object();
  obj_cmd.sender = 'web_client';
  obj_cmd.cat = 'command'
  obj_cmd.msg  = cmd;
  ws.send(JSON.stringify(obj_cmd));
}

function sendMode(md){
  if (mode == 0){
    var selector = document.getElementById('mode_selector');
    selector.disabled = true;
    mode = md;
    var obj_md = new Object();
    obj_md.sender = 'web_client';
    obj_md.cat = 'mode'
    obj_md.msg  = md;
    ws.send(JSON.stringify(obj_md));
  }

}
