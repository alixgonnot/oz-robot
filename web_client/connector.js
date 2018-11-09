//ouverture de la websocket
var ws = new WebSocket("ws://localhost:8080");
var mode = 0;

//code executé à l'ouverture de la socket
ws.onopen = function() {
  var fieldNameElement = document.getElementById('server_status');
  fieldNameElement.innerHTML = "<h3 style=\"color:green\">ONLINE</h3>";
  var obj_cmd = new Object();
  obj_cmd.sender = 'web_client';
  obj_cmd.cat = 'info'
  obj_cmd.val  = 1;
  ws.send(JSON.stringify(obj_cmd));
};

//code executé à la reception d'un message
ws.onmessage = function (evt) {
  var received_msg = JSON.parse(evt.data);
  if (received_msg.sender === "robot" && received_msg.cat === "info" && received_msg.val === "1"){
    console.log("Oh ! it's you !");
    var robotStatusDiv = document.getElementById('robot_status');
    robotStatusDiv.innerHTML = "<h3 style=\"color:green\">ONLINE</h3>";
  }

  if (received_msg.sender === "robot" && received_msg.cat === "info" && received_msg.val === "2"){
    var robotStatusDiv = document.getElementById('robot_status');
    robotStatusDiv.innerHTML = "<h3 style=\"color:orange\">NEXT STEP</h3>";
  }
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
  obj_cmd.val  = cmd;
  ws.send(JSON.stringify(obj_cmd));
  var robotStatusDiv = document.getElementById('robot_status');
  robotStatusDiv.innerHTML = "";
}

function sendEval(cmd){
  var obj_cmd = new Object();
  obj_cmd.sender = 'web_client';
  obj_cmd.cat = 'evaluation'
  if (cmd == 5){
    obj_cmd.val = document.getElementById('eval_perso').value;
    document.getElementById('eval_perso').value = "";
  }
  else
    obj_cmd.val  = cmd;
  ws.send(JSON.stringify(obj_cmd));
  var robotStatusDiv = document.getElementById('robot_status');
  robotStatusDiv.innerHTML = "";
}

function sendCorrec(cmd){
  var obj_cmd = new Object();
  obj_cmd.sender = 'web_client';
  obj_cmd.cat = 'correction'
  obj_cmd.val  = cmd;
  ws.send(JSON.stringify(obj_cmd));
  var robotStatusDiv = document.getElementById('robot_status');
  robotStatusDiv.innerHTML = "";
}

function sendMode(md){
  if (mode == 0){
    var selector = document.getElementById('mode_selector');
    selector.disabled = true;
    mode = md;
    var obj_md = new Object();
    obj_md.sender = 'web_client';
    obj_md.cat = 'mode'
    obj_md.val  = md;
    ws.send(JSON.stringify(obj_md));
  }

}
