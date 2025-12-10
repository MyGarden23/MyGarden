const functions = require("firebase-functions");
const { execFile } = require("child_process");

exports.send_friend_request_notification = functions.https.onCall((data, context) => {
  return new Promise((resolve, reject) => {
    execFile("python3", ["functions/main.py", JSON.stringify(data)], (err, stdout, stderr) => {
      if (err) {
        console.error(stderr);
        reject(err);
        return;
      }
      try {
        resolve(JSON.parse(stdout));
      } catch (e) {
        reject("Invalid JSON from Python: " + stdout);
      }
    });
  });
});