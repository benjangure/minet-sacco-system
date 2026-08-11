const axios = require('axios');

const API_BASE = 'http://localhost:9090/api';

// Treasurer credentials
const TREASURER_USERNAME = 'treasure';
const TREASURER_PASSWORD = 'password';

async function checkLoan367() {
    try {
        console.log('=== Checking Loan 367 Status ===\n');
        
        // 1. Login as treasurer
        console.log('1. Logging in as treasurer...');
        const loginResponse = await axios.post(`${API_BASE}/auth/login`, {
            username: TREASURER_USERNAME,
            password: TREASURER_PASSWORD
        });
        
        const token = loginResponse.data.token;
        console.log('✓ Logged in successfully\n');
        
        const headers = { 'Authorization': `Bearer ${token}` };
        
        // 2. Get loan details
        console.log('2. Fetching loan 367 details...');
        const loanResponse = await axios.get(`${API_BASE}/loans/367`, { headers });
        const loan = loanResponse.data.data;
        
        console.log('Loan Details:');
        console.log(`  - Loan Number: ${loan.loanNumber}`);
        console.log(`  - Amount: ${loan.amount}`);
        console.log(`  - Outstanding Balance: ${loan.outstandingBalance}`);
        console.log(`  - Total Top-up Amount: ${loan.totalTopupAmount || 'N/A'}`);
        console.log(`  - Top-up Count: ${loan.topupCount || 0}`);
        console.log(`  - Last Top-up Date: ${loan.lastTopupDate || 'N/A'}`);
        console.log(`  - Status: ${loan.status}\n`);
        
        // 3. Get top-up requests for this loan
        console.log('3. Fetching top-up requests for loan 367...');
        try {
            const topupRequestsResponse = await axios.get(`${API_BASE}/loans/367/topup-requests`, { headers });
            const topupRequests = topupRequestsResponse.data.data || topupRequestsResponse.data;
            
            console.log(`Found ${topupRequests.length} top-up request(s):`);
            topupRequests.forEach((req, index) => {
                console.log(`\n  Request ${index + 1}:`);
                console.log(`    - ID: ${req.id}`);
                console.log(`    - Requested Amount: ${req.requestedAmount}`);
                console.log(`    - Status: ${req.status}`);
                console.log(`    - Request Date: ${req.requestedDate || req.requestDate}`);
                console.log(`    - Disbursement Date: ${req.disbursementDate || 'N/A'}`);
                console.log(`    - Purpose: ${req.purpose}`);
                console.log(`    - Guarantors: ${req.guarantors ? req.guarantors.length : 0}`);
            });
        } catch (err) {
            console.log(`  Error fetching top-up requests: ${err.message}`);
        }
        
        // 4. Get top-up history for this loan
        console.log('\n4. Fetching top-up history for loan 367...');
        try {
            const historyResponse = await axios.get(`${API_BASE}/loans/367/topup-history`, { headers });
            const history = historyResponse.data.data || historyResponse.data;
            
            if (history.length === 0) {
                console.log('  ⚠️  NO TOP-UP HISTORY RECORDS FOUND!');
                console.log('  This is the problem - the history table is empty.');
            } else {
                console.log(`Found ${history.length} history record(s):`);
                history.forEach((h, index) => {
                    console.log(`\n  History ${index + 1}:`);
                    console.log(`    - Amount: ${h.amount}`);
                    console.log(`    - Date: ${h.topupDate}`);
                    console.log(`    - Outstanding Before: ${h.outstandingBeforeTopup}`);
                    console.log(`    - Outstanding After: ${h.outstandingAfterTopup}`);
                    console.log(`    - Principal Paid Before: ${h.principalPaidBeforeTopup}`);
                    console.log(`    - Processed By: ${h.processedBy}`);
                });
            }
        } catch (err) {
            console.log(`  Error fetching history: ${err.response?.data?.message || err.message}`);
        }
        
        console.log('\n=== End of Check ===');
        
    } catch (error) {
        console.error('Error:', error.response?.data || error.message);
    }
}

checkLoan367();
