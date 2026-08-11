const http = require('http');

async function testLogin() {
  const data = JSON.stringify({
    username: 'treasurer',
    password: 'password'
  });

  const options = {
    hostname: 'localhost',
    port: 9090,
    path: '/api/auth/login',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(data)
    }
  };

  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        console.log('Status:', res.statusCode);
        console.log('Response:', body);
        resolve({ status: res.statusCode, body });
      });
    });

    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

testLogin().then(() => process.exit(0)).catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
