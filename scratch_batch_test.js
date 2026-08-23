const http = require('http');

const queries = [
  "What is the minimum attendance?",
  "What is the hostel fee?",
  "Attendance minimum evlo?",
  "நூலகம் எப்போது திறக்கும்?",
  "What is the capital of France?"
];

function sendQuery(question) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify({ question });
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: '/api/chat',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        'Content-Length': Buffer.byteLength(data)
      }
    };

    const req = http.request(options, (res) => {
      let body = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => {
        resolve({
          statusCode: res.statusCode,
          data: JSON.parse(body)
        });
      });
    });

    req.on('error', (e) => {
      reject(e);
    });

    req.write(data);
    req.end();
  });
}

async function runTests() {
  for (let i = 0; i < queries.length; i++) {
    const q = queries[i];
    console.log(`========================================`);
    console.log(`TEST CASE ${i + 1}: "${q}"`);
    try {
      const res = await sendQuery(q);
      console.log(`STATUS: ${res.statusCode}`);
      console.log(`FOUND:  ${res.data.found}`);
      console.log(`SOURCE: ${res.data.source}`);
      console.log(`CONFIDENCE: ${res.data.confidence}`);
      console.log(`ANSWER:\n${res.data.answer}`);
    } catch (e) {
      console.error(`ERROR running test case ${i + 1}:`, e.message);
    }
  }
  console.log(`========================================`);
}

runTests();
