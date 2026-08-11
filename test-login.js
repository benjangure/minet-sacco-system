const http = require('http');

function testLogin(username, password, path = '/api/auth/login') {
  return new Promise((resolve, reject) => {
    const postData = JSON.stringify({ username, password });
    
    const options = {
      hostname: 'localhost',
      port: 9090,
      path: path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(postData)
      }
    };

    console.log(`\nTesting login: ${username} at ${path}`);
    console.log('Request:', postData);
    
    const req = http.request(options, (res) => {
      let data = '';
      
      console.log(`Status Code: ${res.statusCode}`);
      console.log('Headers:', JSON.stringify(res.headers, null, 2));
      
      res.on('data', (chunk) => {
        data += chunk;
      });
      
      res.on('end', () => {
        console.log('Response:', data);
        try {
          const parsed = JSON.parse(data);
          console.log('Parsed:', JSON.stringify(parsed, null, 2));
        } catch (e) {
          console.log('Could not parse as JSON');
        }
        resolve();
      });
    });

    req.on('error', (e) => {
      console.error(`Problem with request: ${e.message}`);
      reject(e);
    });

    req.write(postData);
    req.end();
  });
}

async function main() {
  console.log('Testing login credentials...\n');
  console.log('='.repeat(60));
  
  await testLogin('1191', '0a0b0c0D.', '/api/member-auth/login');
  console.log('\n' + '='.repeat(60));
  
  await testLogin('treasure', 'password', '/api/auth/login');
  console.log('\n' + '='.repeat(60));
  
  await testLogin('treasurer', 'password', '/api/auth/login');
  console.log('\n' + '='.repeat(60));
}

main().catch(console.error);
