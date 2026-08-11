/**
 * Quick credential tester to find valid users
 */

const http = require('http');

const BASE_URL = 'http://localhost:9090/api';

async function testLogin(username, password) {
  return new Promise((resolve) => {
    const data = JSON.stringify({ username, password });
    
    const options = {
      hostname: 'localhost',
      port: 9090,
      path: '/api/auth/login',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': data.length
      }
    };

    const req = http.request(options, (res) => {
      let responseData = '';
      res.on('data', chunk => responseData += chunk);
      res.on('end', () => {
        resolve({ 
          username, 
          password, 
          status: res.statusCode, 
          success: res.statusCode === 200,
          data: responseData 
        });
      });
    });

    req.on('error', () => resolve({ username, password, status: 0, success: false }));
    req.write(data);
    req.end();
  });
}

async function findValidCredentials() {
  console.log('🔍 Testing credentials against http://localhost:9090/api/auth/login\n');
  
  const testCombinations = [
    // Common member numbers
    { username: 'EMP001', password: 'test' },
    { username: 'EMP001', password: 'password' },
    { username: 'EMP001', password: 'password123' },
    { username: 'EMP002', password: 'test' },
    { username: 'EMP002', password: 'password' },
    { username: '1191', password: 'test' },
    { username: '1191', password: 'password' },
    { username: '1191', password: '0a0b0c0D.' },
    
    // Treasurer/admin accounts
    { username: 'treasure', password: 'password' },
    { username: 'treasure', password: 'test' },
    { username: 'treasurer', password: 'password' },
    { username: 'admin', password: 'password' },
    { username: 'admin', password: 'admin' },
    { username: 'admin', password: 'admin123' },
  ];

  const validCredentials = [];

  for (const cred of testCombinations) {
    const result = await testLogin(cred.username, cred.password);
    
    if (result.success) {
      console.log(`✓ SUCCESS: ${cred.username} / ${cred.password}`);
      validCredentials.push(cred);
    } else {
      console.log(`✗ Failed:  ${cred.username} / ${cred.password} (Status: ${result.status})`);
    }
  }

  console.log('\n' + '='.repeat(60));
  console.log('VALID CREDENTIALS FOUND:');
  console.log('='.repeat(60));
  
  if (validCredentials.length > 0) {
    validCredentials.forEach(cred => {
      console.log(`  Username: ${cred.username}  |  Password: ${cred.password}`);
    });
  } else {
    console.log('  ❌ No valid credentials found!');
  }
}

findValidCredentials().then(() => process.exit(0)).catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
