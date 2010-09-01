var sys = require("sys");
var http = require("http");
var HOST = "localhost"
//var HOST = "10.0.1.5"
var PORT = "8080"
var timeo=10000;
var conn_count = 0;
var MAXCONN = 5000;


function chatter() {
  //console.log('Starting client');
  var client = http.createClient(PORT, HOST);
  client.setTimeout(0);
  client.addListener('connect', function(){ conn_count++; });
  client.addListener('close', function(){ conn_count--; });
  
  msg = (new Date).toISOString()+": msg";
  var req = client.request('GET', '/subscribe', {'host': 'localhost'});
  req.end();
  req.on('response', function(r){ 
    r.on('data', function (chunk) {
      if (chunk.length > 1) console.log(chunk);
    });
  });
}

function logger() {
  console.log('connect count: ' + conn_count)
  console.log(sys.inspect(process.memoryUsage()));
  setTimeout(logger, timeo);
}

setTimeout(logger, 2000);
for (a = 0; a <= MAXCONN; a++) chatter();
