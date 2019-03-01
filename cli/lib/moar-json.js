"use strict";

var _reallyRelaxedJson = require("really-relaxed-json");

/**
 * CLI tool to convert a stream of RJSON to standard JSON.
 */
var stdin = process.stdin;
var stdout = process.stdout;
var chunks = [];
stdin.resume();
stdin.setEncoding('utf8');
stdin.on('data', chunk => {
  chunks.push(chunk);
});
stdin.on('end', () => {
  var input = chunks.join();
  var object = JSON.parse((0, _reallyRelaxedJson.toJson)(input));
  var pretty = JSON.stringify(object);
  console.log(pretty);
});