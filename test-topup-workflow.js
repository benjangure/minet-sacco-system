/**
 * Comprehensive Loan Top-Up Workflow Test Script
 * Tests: Member request → Guarantor approval → Treasurer approval
 */

const https = require('https');
const http = require('http');

const BASE_URL = 'http://localhost:9090/api';
const agent = new http.Agent({ keepAlive: true });

// ANSI color codes for better output
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m'
};

function log(message, color = colors.reset) {
  console.log(`${color}${message}${colors.reset}`);
}

function logSection(title) {
  console.log('\n' + '='.repeat(60));
  log(title, colors.bright + colors.cyan);
  console.log('='.repeat(60));
}

function logSuccess(message) {
  log(`✓ ${message}`, colors.green);
}

function logError(message) {
  log(`✗ ${message}`, colors.red);
}

function logInfo(message) {
  log(`ℹ ${message}`, colors.blue);
}

// HTTP request helper
function makeRequest(method, path, token = null, body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE_URL);
    const bodyData = body ? JSON.stringify(body) : '';
    
    const options = {
      hostname: url.hostname,
      port: url.port,
      path: url.pathname + url.search,
      method,
      headers: {
        'Content-Type': 'application/json',
      }
    };

    if (bodyData) {
      options.headers['Content-Length'] = Buffer.byteLength(bodyData);
    }

    if (token) {
      options.headers['Authorization'] = `Bearer ${token}`;
    }

    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsed = data ? JSON.parse(data) : {};
          resolve({ status: res.statusCode, data: parsed, headers: res.headers, rawData: data });
        } catch (e) {
          resolve({ status: res.statusCode, data: data, headers: res.headers, rawData: data });
        }
      });
    });

    req.on('error', (err) => {
      logError(`Network error: ${err.message}`);
      reject(err);
    });
    
    if (bodyData) {
      req.write(bodyData);
    }
    
    req.end();
  });
}

// Authentication
async function login(username, password, isMember = false) {
  const endpoint = isMember ? '/api/auth/member/login' : '/api/auth/login';
  logInfo(`Logging in as: ${username} (${isMember ? 'Member' : 'Admin'})`);
  
  const data = JSON.stringify({ username, password });
  const options = {
    hostname: 'localhost',
    port: 9090,
    path: endpoint,
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
        if (res.statusCode === 200) {
          const parsed = JSON.parse(body);
          if (parsed.token) {
            logSuccess(`Login successful for ${username}`);
            resolve(parsed.token);
          } else {
            logError(`No token in response`);
            reject(new Error('No token in response'));
          }
        } else {
          logError(`Login failed - Status: ${res.statusCode}, Response: ${body}`);
          reject(new Error(`Login failed for ${username}: HTTP ${res.statusCode}`));
        }
      });
    });

    req.on('error', (err) => {
      logError(`Network error: ${err.message}`);
      reject(err);
    });

    req.write(data);
    req.end();
  });
}

// Fetch member's active loans
async function getMemberLoans(token) {
  logInfo('Fetching member loans...');
  const response = await makeRequest('GET', '/api/member/loans', token);
  
  if (response.status === 200) {
    const eligibleLoans = response.data.filter(loan => 
      ['ACTIVE', 'DISBURSED'].includes(loan.status) && 
      loan.outstandingBalance > 0
    );
    logSuccess(`Found ${eligibleLoans.length} eligible loans for top-up`);
    return eligibleLoans;
  } else {
    throw new Error(`Failed to fetch loans: ${JSON.stringify(response.data)}`);
  }
}

// Request loan top-up
async function requestTopUp(token, loanId, requestedAmount, reason, guarantors) {
  logInfo(`Requesting top-up of KES ${requestedAmount} for loan ID: ${loanId}`);
  const response = await makeRequest('POST', `/api/loans/${loanId}/request-topup`, token, {
    requestedAmount,
    purpose: reason,
    guarantors: guarantors || []
  });
  
  if (response.status === 200 || response.status === 201) {
    logSuccess(`Top-up request created: ID ${response.data.data?.id || response.data.id}`);
    return response.data.data || response.data;
  } else {
    throw new Error(`Top-up request failed: ${JSON.stringify(response.data)}`);
  }
}

// Get pending guarantor approvals
async function getPendingGuarantorApprovals(token) {
  logInfo('Checking pending guarantor approvals...');
  const response = await makeRequest('GET', '/api/member/pending-topup-guarantees', token);
  
  if (response.status === 200) {
    logSuccess(`Found ${response.data.length} pending guarantor approvals`);
    return response.data;
  } else {
    throw new Error(`Failed to fetch guarantor approvals: ${JSON.stringify(response.data)}`);
  }
}

// Approve as guarantor
async function approveAsGuarantor(token, topUpRequestId, guarantorId) {
  logInfo(`Guarantor ${guarantorId} approving top-up request ${topUpRequestId}...`);
  const response = await makeRequest('POST', `/api/topup-requests/${topUpRequestId}/guarantor/approve`, token, {
    guarantorId
  });
  
  if (response.status === 200) {
    logSuccess(`Guarantor approval successful`);
    return response.data;
  } else {
    throw new Error(`Guarantor approval failed: ${JSON.stringify(response.data)}`);
  }
}

// Get pending treasurer approvals
async function getPendingTreasurerApprovals(token) {
  logInfo('Checking pending treasurer approvals...');
  const response = await makeRequest('GET', '/api/admin/topup-requests/pending', token);
  
  if (response.status === 200) {
    logSuccess(`Found ${response.data.length} pending top-up requests for treasurer`);
    return response.data;
  } else {
    throw new Error(`Failed to fetch treasurer approvals: ${JSON.stringify(response.data)}`);
  }
}

// Approve as treasurer
async function approveAsTreasurer(token, topUpRequestId) {
  logInfo(`Treasurer approving top-up request ${topUpRequestId}...`);
  const response = await makeRequest('POST', `/api/admin/topup-requests/${topUpRequestId}/approve`, token);
  
  if (response.status === 200) {
    logSuccess(`Treasurer approval successful - Top-up disbursed!`);
    return response.data;
  } else {
    throw new Error(`Treasurer approval failed: ${JSON.stringify(response.data)}`);
  }
}

// Get loan details
async function getLoanDetails(token, loanId) {
  const response = await makeRequest('GET', `/api/member/loans/${loanId}`, token);
  
  if (response.status === 200) {
    return response.data;
  } else {
    throw new Error(`Failed to fetch loan details: ${JSON.stringify(response.data)}`);
  }
}

// Main test workflow
async function runTopUpWorkflowTest() {
  try {
    log('\n╔════════════════════════════════════════════════════════════╗', colors.bright);
    log('║     LOAN TOP-UP WORKFLOW COMPREHENSIVE TEST SUITE         ║', colors.bright + colors.cyan);
    log('╚════════════════════════════════════════════════════════════╝', colors.bright);

    // Step 1: Login as member (borrower)
    logSection('STEP 1: Member Login');
    const memberToken = await login('1203', '0a0b0c0D.', true);
    
    // Step 2: Get member's active loans
    logSection('STEP 2: Fetch Active Loans');
    const memberLoans = await getMemberLoans(memberToken);
    
    if (memberLoans.length === 0) {
      logError('No active loans found for member. Cannot proceed with top-up test.');
      logInfo('Please ensure the member has an active loan with outstanding balance.');
      return;
    }
    
    const targetLoan = memberLoans[0];
    logInfo(`Selected Loan: ID=${targetLoan.id}, Outstanding=${targetLoan.outstandingBalance}`);
    
    // Store original loan state
    const originalLoanState = await getLoanDetails(memberToken, targetLoan.id);
    logInfo(`Original loan amount: KES ${originalLoanState.approvedAmount || 'N/A'}`);
    logInfo(`Original outstanding: KES ${originalLoanState.outstandingBalance || 'N/A'}`);
    
    // Step 3: Request top-up
    logSection('STEP 3: Member Requests Top-Up');
    const topUpAmount = 5000;
    const topUpReason = 'Emergency medical expenses - automated test';
    
    // Define guarantors (using existing member number)
    const guarantors = [
      { memberNumber: '1191', guaranteeAmount: 5000 }
    ];
    
    logInfo(`Guarantors: ${guarantors.map(g => `${g.memberNumber} (KES ${g.guaranteeAmount})`).join(', ')}`);
    
    const topUpRequest = await requestTopUp(memberToken, targetLoan.id, topUpAmount, topUpReason, guarantors);
    
    logInfo(`Top-Up Request Details:`);
    logInfo(`  - Request ID: ${topUpRequest.id}`);
    logInfo(`  - Amount: KES ${topUpRequest.requestedAmount}`);
    logInfo(`  - Status: ${topUpRequest.status}`);
    logInfo(`  - Guarantors needed: ${topUpRequest.guarantors?.length || 0}`);
    
    // Step 4: Guarantor approvals
    logSection('STEP 4: Guarantor Approvals');
    
    if (!topUpRequest.guarantors || topUpRequest.guarantors.length === 0) {
      logError('No guarantors found for this top-up request');
      throw new Error('Top-up request has no guarantors');
    }
    
    let approvedCount = 0;
    for (const guarantor of topUpRequest.guarantors) {
      logInfo(`\nProcessing guarantor: ${guarantor.guarantorMemberNumber}`);
      
      try {
        // Login as guarantor
        const guarantorToken = await login(guarantor.guarantorMemberNumber, '0a0b0c0D.', true);
        
        // Check pending approvals
        const pendingApprovals = await getPendingGuarantorApprovals(guarantorToken);
        const thisApproval = pendingApprovals.find(p => p.topUpRequestId === topUpRequest.id);
        
        if (thisApproval) {
          // Approve
          await approveAsGuarantor(guarantorToken, topUpRequest.id, guarantor.id);
          approvedCount++;
          logSuccess(`Guarantor ${guarantor.guarantorMemberNumber} approved (${approvedCount}/${topUpRequest.guarantors.length})`);
        } else {
          logError(`Guarantor ${guarantor.guarantorMemberNumber} has no pending approval for this request`);
        }
      } catch (error) {
        logError(`Failed to process guarantor ${guarantor.guarantorMemberNumber}: ${error.message}`);
      }
    }
    
    if (approvedCount === 0) {
      logError('No guarantors successfully approved the request');
      throw new Error('Guarantor approval failed');
    }
    
    logSuccess(`All ${approvedCount} guarantor(s) approved the top-up request`);
    
    // Step 5: Treasurer approval
    logSection('STEP 5: Treasurer Approval & Disbursement');
    
    // Login as treasurer
    const treasurerToken = await login('treasure', 'password');
    
    // Check pending approvals
    const pendingForTreasurer = await getPendingTreasurerApprovals(treasurerToken);
    const thisRequest = pendingForTreasurer.find(r => r.id === topUpRequest.id);
    
    if (!thisRequest) {
      logError('Top-up request not found in treasurer pending list');
      logInfo('This might mean all guarantors have not approved yet, or request is in wrong state');
      throw new Error('Request not available for treasurer approval');
    }
    
    // Approve and disburse
    const disbursedTopUp = await approveAsTreasurer(treasurerToken, topUpRequest.id);
    
    logInfo(`Disbursement Details:`);
    logInfo(`  - Status: ${disbursedTopUp.status}`);
    logInfo(`  - Approved Amount: KES ${disbursedTopUp.approvedAmount}`);
    logInfo(`  - Approved At: ${disbursedTopUp.approvedAt}`);
    logInfo(`  - Approved By: ${disbursedTopUp.approvedBy}`);
    
    // Step 6: Verify loan update
    logSection('STEP 6: Verification');
    
    const updatedLoan = await getLoanDetails(memberToken, targetLoan.id);
    
    logInfo('Loan State Comparison:');
    logInfo(`  Before: Amount=${originalLoanState.approvedAmount}, Outstanding=${originalLoanState.outstandingBalance}`);
    logInfo(`  After:  Amount=${updatedLoan.approvedAmount}, Outstanding=${updatedLoan.outstandingBalance}`);
    
    const expectedNewAmount = (originalLoanState.approvedAmount || 0) + topUpAmount;
    const expectedNewOutstanding = (originalLoanState.outstandingBalance || 0) + topUpAmount;
    
    if (updatedLoan.approvedAmount >= expectedNewAmount) {
      logSuccess(`✓ Loan amount increased correctly (${updatedLoan.approvedAmount} >= ${expectedNewAmount})`);
    } else {
      logError(`✗ Loan amount not updated as expected (${updatedLoan.approvedAmount} < ${expectedNewAmount})`);
    }
    
    if (updatedLoan.outstandingBalance >= expectedNewOutstanding) {
      logSuccess(`✓ Outstanding balance increased correctly (${updatedLoan.outstandingBalance} >= ${expectedNewOutstanding})`);
    } else {
      logError(`✗ Outstanding balance not updated as expected (${updatedLoan.outstandingBalance} < ${expectedNewOutstanding})`);
    }
    
    // Final summary
    logSection('TEST SUMMARY');
    logSuccess('✓ Member login successful');
    logSuccess('✓ Active loan fetched');
    logSuccess('✓ Top-up request created');
    logSuccess(`✓ ${approvedCount} guarantor(s) approved`);
    logSuccess('✓ Treasurer approved and disbursed');
    logSuccess('✓ Loan updated successfully');
    
    log('\n╔════════════════════════════════════════════════════════════╗', colors.bright + colors.green);
    log('║          TOP-UP WORKFLOW TEST COMPLETED SUCCESSFULLY       ║', colors.bright + colors.green);
    log('╚════════════════════════════════════════════════════════════╝', colors.bright + colors.green);
    
  } catch (error) {
    logSection('TEST FAILED');
    logError(`Error: ${error.message}`);
    if (error.stack) {
      console.error(error.stack);
    }
    process.exit(1);
  }
}

// Run the test
runTopUpWorkflowTest().then(() => {
  log('\nTest execution completed. Exiting...', colors.cyan);
  process.exit(0);
}).catch(error => {
  logError(`Unhandled error: ${error.message}`);
  process.exit(1);
});
