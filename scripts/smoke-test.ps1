param(
    [string]$PaymentsUrl = 'http://localhost:8081',
    [string]$OrdersUrl = 'http://localhost:8082'
)

$ErrorActionPreference = 'Stop'

function Send-Json([string]$Uri, [hashtable]$Payload) {
    Invoke-RestMethod -Method Post -Uri $Uri -ContentType 'application/json' `
        -Body ($Payload | ConvertTo-Json -Compress) -TimeoutSec 10
}

function Assert-Equal($Actual, $Expected, [string]$Message) {
    if ($Actual -ne $Expected) {
        throw "${Message}: expected '$Expected', received '$Actual'"
    }
    Write-Host "PASS: $Message"
}

function Wait-Order([string]$OrderId, [string]$ExpectedStatus) {
    $deadline = [DateTime]::UtcNow.AddSeconds(60)
    do {
        $order = Invoke-RestMethod -Uri "$OrdersUrl/orders/$OrderId" -TimeoutSec 10
        if ($order.status -ne 'NEW') {
            Assert-Equal $order.status $ExpectedStatus "Order reaches $ExpectedStatus"
            return
        }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "Order $OrderId did not reach $ExpectedStatus within 60 seconds"
}

Assert-Equal (Invoke-RestMethod "$PaymentsUrl/actuator/health" -TimeoutSec 10).status 'UP' 'Payments is healthy'
Assert-Equal (Invoke-RestMethod "$OrdersUrl/actuator/health" -TimeoutSec 10).status 'UP' 'Orders is healthy'

$testUserId = [guid]::NewGuid().ToString()
$account = Send-Json "$PaymentsUrl/payments/account" @{ userId = $testUserId }
Assert-Equal ([decimal]$account.balance) 0 'New account has zero balance'

$toppedUp = Send-Json "$PaymentsUrl/payments/accounts/$testUserId/top-ups" @{ amount = 1000 }
Assert-Equal ([decimal]$toppedUp.balance) 1000 'Account top-up is persisted'

$paidOrder = Send-Json "$OrdersUrl/orders" @{
    userId = $testUserId; name = 'Docker smoke test'; amount = 250
    description = 'Successful payment through Kafka'
}
Wait-Order $paidOrder.id 'PAID'
$balance = Invoke-RestMethod "$PaymentsUrl/payments/balance?userId=$testUserId" -TimeoutSec 10
Assert-Equal ([decimal]$balance) 750 'Successful payment debits 250'

$declinedOrder = Send-Json "$OrdersUrl/orders" @{
    userId = $testUserId; name = 'Insufficient funds'; amount = 1000
    description = 'Declined payment through Kafka'
}
Wait-Order $declinedOrder.id 'CANCELED'
$balance = Invoke-RestMethod "$PaymentsUrl/payments/balance?userId=$testUserId" -TimeoutSec 10
Assert-Equal ([decimal]$balance) 750 'Declined payment preserves the balance'

$missingAccountOrder = Send-Json "$OrdersUrl/orders" @{
    userId = [guid]::NewGuid().ToString(); name = 'Missing account'; amount = 10
    description = 'User without a payment account'
}
Wait-Order $missingAccountOrder.id 'CANCELED'

Write-Host "Smoke test passed. Test user: $testUserId"
