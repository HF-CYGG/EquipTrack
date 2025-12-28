
const axios = require('axios');

async function test() {
    try {
        // Login as Super Admin
        const loginUrl = 'http://localhost:3000/api/login';
        const loginPayload = {
            contact: 'furry', // From users.json
            password: '1330'  // From users.json
        };

        const loginRes = await axios.post(loginUrl, loginPayload);
        const token = loginRes.data.token;
        console.log("Login successful, token:", token.substring(0, 20) + "...");

        // Get approved borrow requests
        const approvedUrl = 'http://localhost:3000/api/borrow-requests/review?status=approved';
        const approvedRes = await axios.get(approvedUrl, {
            headers: {
                Authorization: `Bearer ${token}`
            }
        });

        console.log(`Approved requests count: ${approvedRes.data.length}`);
        if (approvedRes.data.length > 0) {
            console.log("Sample approved request:", JSON.stringify(approvedRes.data[0], null, 2));
        }

        // Get rejected borrow requests
        const rejectedUrl = 'http://localhost:3000/api/borrow-requests/review?status=rejected';
        const rejectedRes = await axios.get(rejectedUrl, {
            headers: {
                Authorization: `Bearer ${token}`
            }
        });

        console.log(`Rejected requests count: ${rejectedRes.data.length}`);
        if (rejectedRes.data.length > 0) {
            console.log("Sample rejected request:", JSON.stringify(rejectedRes.data[0], null, 2));
        }

    } catch (error) {
        if (error.response) {
             console.error(`Error: ${error.response.status} - ${JSON.stringify(error.response.data)}`);
        } else {
             console.error("Error:", error.message);
        }
    }
}

test();
