const axios = require("axios");

function sleep(ms) {
    return new Promise((resolve) => {
        setTimeout(resolve, ms);
    });
}

(async function main() {
    const CONCURRENT_CALLS = 1000
    const createData = {
        "description": "Student Math Course Report",
        "headers": ["Student #", "Name", "Class", "Score"],
        "data": [
            ["s-008", "Sarah", "Class-A", "B"],
            ["s-009", "Thomas", "Class-A", "B-"],
            ["s-010", "Joseph", "Class-B", "A-"],
            ["s-011", "Charles", "Class-C", "A123123"]
        ],
        "submitter": "Mrs. York"
    }

    let requests = []
    for (let i = 0; i < CONCURRENT_CALLS; i++) {
        requests.push({
            url: 'http://localhost:8080/report/sync',
            data: createData
        })
    }

    const requestResult = await Promise.all(requests.map(async request => {
        try {
            let createResult = await axios.post(request.url, request.data);
            return createResult.data
        }
        catch (error) {
            console.log(error)
        }
        return null
    }))

    let listResult
    while (true) {
        try {
            listResult = await axios.get('http://localhost:8080/report');
            listResult = listResult.data.data

            let pdfPending = 0, pdfCompleted = 0, pdfFailed = 0
            let excelPending = 0, excelCompleted = 0, excelFailed = 0
            done = true
            listResult.forEach(result => {
                pdfPending += result.pdfReportStatus === "PENDING" ? 1 : 0
                excelPending += result.excelReportStatus === "PENDING" ? 1 : 0
                pdfCompleted += result.pdfReportStatus === "COMPLETED" ? 1 : 0
                excelCompleted += result.excelReportStatus === "COMPLETED" ? 1 : 0
                pdfFailed += result.pdfReportStatus === "FAILED" ? 1 : 0
                excelFailed += result.excelReportStatus === "FAILED" ? 1 : 0
            })

            console.log(`PDF: ${pdfPending} pending. ${pdfCompleted} completed. ${pdfFailed} failed`)
            console.log(`Excel: ${excelPending} pending. ${excelCompleted} completed. ${excelFailed} failed`)

            if (pdfPending === 0 && excelPending === 0) break;
        }
        catch (error) {
            console.log(error)
        }
        console.log("Function is pending. Sleep 3s...\n")
        await sleep(3000)
    }
})()
