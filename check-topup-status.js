/**
 * Check Top-Up Request Status in Database
 */

const http = require('http');

const BASE_URL = 'http://localhost:9090/api';

// Colors
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  cyan: '\x1b[36m',
  bright: '\x1b[1m'
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

// Login helper
async function login(username, password, isMember = false) {
  return new Promise((resolve, reject) => {
    const endpoint = isMember ? '/auth/member/login' : '/auth/login';
    const data = JSON.stringify({ username, password });
    
    const options = {
      hostname: 'localhost',
      port: 9090,
      path: `/api${endpoint}`,
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
        try {
          const parsed = JSON.parse(responseData);
          if (res.statusCode === 200 && parsed.token) {
            resolve(parsed.token);
          } else {
            reject(new Error(`Login failed: ${responseData}`));
          }
        } catch (e) {
          reject(e);
        }
      });
    });
    
    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

// Get pending top-up requests for treasurer
async function getPendingTopUps(token) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 9090,
      path: '/api/admin/topup-requests/pending',
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    };
    
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          resolve({ status: res.statusCode, data: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, data: data });
        }
      });
    });
    
    req.on('error', reject);
    req.end();
  });
}

// Get member's top-up requests
async function getMemberTopUps(token) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 9090,
      path: '/api/member/topup-requests',
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    };
    
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          resolve({ status: res.statusCode, data: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, data: data });
        }
      });
    });
    
    req.on('error', reject);
    req.end();
  });
}

// Main check
async function checkTopUpStatus() {
  try {
    console.log('\n' + '='.repeat(60));
    log('CHECKING TOP-UP REQUEST STATUS', colors.bright + colors.cyan);
    console.log('='.repeat(60) + '\n');
    
    // 1. Login as member
    log('1. Logging in as member 1203...', colors.cyan);
    const memberToken = await login('1203', '0a0b0c0D.', true);
    log('   ✓ Member login successful', colors.green);
    
    // 2. Check member's top-up requests
    log('\n2. Checking member\'s top-up requests...', colors.cyan);
    const memberResponse = await getMemberTopUps(memberToken);
    
    if (memberResponse.status === 200 && memberResponse.data.success) {
      const requests = memberResponse.data.data || [];
      log(`   ✓ Found ${requests.length} top-up request(s)`, colors.green);
      
      if (requests.length > 0) {
        requests.forEach((req, index) => {
          log(`\n   Request #${index + 1}:`, colors.yellow);
          log(`     ID: ${req.id}`);
          log(`     Loan: ${req.loanNumber}`);
          log(`     Amount: KES ${req.requestedAmount}`);
          log(`     Status: ${req.status}`, req.status === 'PENDING_GUARANTOR_APPROVAL' ? colors.yellow : colors.green);
          log(`     Guarantors Approved: ${req.guarantorApprovalCount || 0}`);
          log(`     All Guarantors Approved: ${req.allGuarantorsApproved ? 'YES' : 'NO'}`);
          
          if (req.guarantors && req.guarantors.length > 0) {
            log(`     Guarantors:`);
            req.guarantors.forEach(g => {
              log(`       - ${g.member?.memberNumber || 'Unknown'}: ${g.status}`);
            });
          }
        });
      }
    } else {
      log(`   ✗ Failed to fetch member requests: ${JSON.stringify(memberResponse.data)}`, colors.red);
    }
    
    // 3. Login as treasurer
    log('\n3. Logging in as treasurer (testing credentials)...', colors.cyan);
    try {
      const treasurerToken = await login('treasurer', 'test');
      log('   ✓ Treasurer login successful with treasurer/test', colors.green);
      
      // Check pending
      const treasurerResponse = await getPendingTopUps(treasurerToken);
      if (treasurerResponse.status === 200 && treasurerResponse.data.success) {
        const pending = treasurerResponse.data.data || [];
        log(`   ✓ Found ${pending.length} pending top-up(s) for treasurer review`, colors.green);
      }
    } catch (e1) {
      try {
        const treasurerToken = await login('treasure', 'password');
        log('   ✓ Treasurer login successful with treasure/password', colors.green);
        
        // Check pending
        const treasurerResponse = await getPendingTopUps(treasurerToken);
        if (treasurerResponse.status === 200 && treasurerResponse.data.success) {
          const pending = treasurerResponse.data.data || [];
          log(`   ✓ Found ${pending.length} pending top-up(s) for treasurer review`, colors.green);
        }
      } catch (e2) {
        log('   ✗ Failed to login as treasurer with both credential sets', colors.red);
      }
    }
    
    console.log('\n' + '='.repeat(60));
    log('STATUS CHECK COMPLETE', colors.bright + colors.green);
    console.log('='.repeat(60) + '\n');
    
  } catch (error) {
    log(`\n✗ Error: ${error.message}`, colors.red);
    console.error(error);
    process.exit(1);
  }
}

// Run the check
checkTopUpStatus().then(() => process.exit(0)).catch(err => {
  console.error(err);
  process.exit(1);
});
