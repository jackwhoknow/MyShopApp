# MyShopApp API smoke test
# Prerequisite: backend running on http://localhost:8080 with profile "test"
# (independent database myshop_test, schema recreated on startup)

$ErrorActionPreference = 'Stop'
$Base = 'http://localhost:8080'
$script:Pass = 0
$script:Fail = 0
$script:Failures = @()

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token,
        $Body
    )

    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    $params = @{
        Uri             = "$Base$Path"
        Method          = $Method
        Headers         = $headers
        ContentType     = 'application/json; charset=utf-8'
        TimeoutSec      = 30
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $jsonText = if ($Body -is [string]) { $Body } else { $Body | ConvertTo-Json -Depth 10 -Compress }
        $params['Body'] = [System.Text.Encoding]::UTF8.GetBytes($jsonText)
    }

    try {
        $resp = Invoke-WebRequest @params
        $json = $null
        try { $json = $resp.Content | ConvertFrom-Json } catch { }
        return @{ Status = [int]$resp.StatusCode; Json = $json; Raw = $resp.Content }
    }
    catch {
        $status = 0
        $text = $_.ErrorDetails.Message
        if ($_.Exception.Response -ne $null) {
            $status = [int]$_.Exception.Response.StatusCode
            if (-not $text) {
                try {
                    $stream = $_.Exception.Response.GetResponseStream()
                    $reader = New-Object System.IO.StreamReader($stream)
                    $text = $reader.ReadToEnd()
                } catch { }
            }
        }
        $json = $null
        try { $json = $text | ConvertFrom-Json } catch { }
        return @{ Status = $status; Json = $json; Raw = $text }
    }
}

function Check {
    param([string]$Name, [bool]$Condition, [string]$Detail = '')
    if ($Condition) {
        Write-Host "  [PASS] $Name"
        $script:Pass++
    }
    else {
        Write-Host "  [FAIL] $Name  $Detail"
        $script:Fail++
        $script:Failures += "$Name $Detail"
    }
}

function Body($obj) { return $obj | ConvertTo-Json -Depth 10 -Compress }

Write-Host '=== MyShopApp API smoke test ==='
$stamp = Get-Date -Format 'HHmmss'
$merchantName = "sm_merchant_$stamp"
$buyerName = "sm_buyer_$stamp"
$password = 'smoke123'

# ---------- 0. health / auth ----------
Write-Host '-- auth & permission --'
$r = Invoke-Api -Method GET -Path '/api/orders'
Check 'anonymous request is rejected with 401' ($r.Status -eq 401) "status=$($r.Status)"

$r = Invoke-Api -Method POST -Path '/api/auth/register' -Body (Body @{
        username = $merchantName; password = $password; nickname = $merchantName; phone = '13800000001'; merchant = $true
    })
Check 'register merchant' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method POST -Path '/api/auth/register' -Body (Body @{
        username = $buyerName; password = $password; nickname = $buyerName; phone = '13800000002'; merchant = $false
    })
Check 'register buyer' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method POST -Path '/api/auth/register' -Body (Body @{
        username = $buyerName; password = $password; nickname = $buyerName; phone = '13800000002'; merchant = $false
    })
Check 'duplicate username rejected with 409' ($r.Status -eq 409) "status=$($r.Status)"

function Login([string]$name) {
    $resp = Invoke-Api -Method POST -Path '/api/auth/login' -Body (Body @{ username = $name; password = $password })
    if ($resp.Status -ne 200) { throw "login failed for $name : $($resp.Raw)" }
    return $resp.Json.data.token
}

$merchantToken = Login $merchantName
$buyerToken = Login $buyerName
$admin = Invoke-Api -Method POST -Path '/api/auth/login' -Body (Body @{ username = 'admin'; password = 'admin123' })
Check 'default admin can login' ($admin.Status -eq 200 -and $admin.Json.data.user.roles -contains 'ADMIN')
$adminToken = $admin.Json.data.token

$r = Invoke-Api -Method GET -Path '/api/admin/users' -Token $buyerToken
Check 'buyer cannot reach admin api (403)' ($r.Status -eq 403) "status=$($r.Status)"

# ---------- 1. category ----------
Write-Host '-- category --'
$r = Invoke-Api -Method POST -Path '/api/admin/categories' -Token $adminToken -Body (Body @{
        parentId = $null; name = "SmokeCategory$stamp"; sortOrder = 1; enabled = $true
    })
Check 'admin creates category' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status) raw=$($r.Raw)"
$categoryId = $r.Json.data.id

# ---------- 2. shop apply & audit ----------
Write-Host '-- shop --'
$r = Invoke-Api -Method POST -Path '/api/merchant/shops/apply' -Token $merchantToken -Body (Body @{
        name = "SmokeShop$stamp"; description = 'smoke test shop'
    })
Check 'merchant applies shop' ($r.Status -eq 200 -and $r.Json.data.status -eq 'PENDING') "status=$($r.Status) raw=$($r.Raw)"
$shopId = $r.Json.data.id

$r = Invoke-Api -Method GET -Path '/api/merchant/shops/mine' -Token $merchantToken
Check 'merchant reads own shop' ($r.Status -eq 200 -and $r.Json.data.id -eq $shopId)

$r = Invoke-Api -Method POST -Path "/api/admin/shops/$shopId/approve" -Token $adminToken
Check 'admin approves shop' ($r.Status -eq 200 -and $r.Json.data.status -eq 'ACTIVE') "status=$($r.Status) raw=$($r.Raw)"

# ---------- 3. product create & audit ----------
Write-Host '-- product --'
$productBody = @{
    categoryId  = $categoryId
    name        = "SmokeProduct$stamp"
    subtitle    = 'smoke subtitle'
    description = 'smoke description'
    mainImage   = 'https://example.com/p.png'
    specs       = @(@{ name = 'Color'; values = @('Red', 'Blue') })
    skus        = @(
        @{ price = 100; stock = 10; image = 'https://example.com/r.png'; specValues = @{ Color = 'Red' } },
        @{ price = 120; stock = 5; image = 'https://example.com/b.png'; specValues = @{ Color = 'Blue' } }
    )
}
$r = Invoke-Api -Method POST -Path '/api/merchant/products' -Token $merchantToken -Body (Body $productBody)
Check 'merchant creates product' ($r.Status -eq 200 -and $r.Json.data.status -eq 'PENDING_REVIEW') "status=$($r.Status) raw=$($r.Raw)"
$productId = $r.Json.data.id
$skuRed = $r.Json.data.skus[0].id
$skuBlue = $r.Json.data.skus[1].id

$r = Invoke-Api -Method GET -Path "/api/products/$productId"
Check 'pending product is hidden from public detail' ($r.Status -eq 404) "status=$($r.Status)"

$r = Invoke-Api -Method POST -Path "/api/admin/products/$productId/approve" -Token $adminToken
Check 'admin approves product' ($r.Status -eq 200 -and $r.Json.data.status -eq 'ON_SHELF') "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path "/api/products/$productId"
Check 'public can read on-shelf product' ($r.Status -eq 200 -and $r.Json.data.skus.Count -eq 2) "status=$($r.Status)"
Check 'initial red sku stock is 10' ($r.Json.data.skus[0].stock -eq 10) "stock=$($r.Json.data.skus[0].stock)"

# ---------- 4. address ----------
Write-Host '-- address --'
$r = Invoke-Api -Method POST -Path '/api/addresses' -Token $buyerToken -Body (Body @{
        receiverName = 'Buyer'; phone = '13800000002'
        province = 'Zhejiang'; city = 'Hangzhou'; district = 'Xihu'; detail = 'No.1 Smoke Road'
        defaultAddress = $true
    })
Check 'buyer creates address' ($r.Status -eq 200 -and $r.Json.data.defaultAddress -eq $true) "status=$($r.Status) raw=$($r.Raw)"
$addressId = $r.Json.data.id

# ---------- 5. cart & order & pay ----------
Write-Host '-- cart / order / pay --'
$r = Invoke-Api -Method POST -Path '/api/cart' -Token $buyerToken -Body (Body @{ skuId = $skuRed; quantity = 2 })
Check 'buyer adds 2 items to cart' ($r.Status -eq 200 -and $r.Json.data.quantity -eq 2) "status=$($r.Status) raw=$($r.Raw)"
$cartItemId = $r.Json.data.id

$r = Invoke-Api -Method PUT -Path "/api/cart/$cartItemId" -Token $buyerToken -Body (Body @{ quantity = 0 })
Check 'cart quantity 0 is rejected by validation' ($r.Status -eq 400) "status=$($r.Status)"

$r = Invoke-Api -Method POST -Path '/api/orders' -Token $buyerToken -Body (Body @{
        addressId = $addressId; remark = 'smoke order'; cartItemIds = @(); userCouponIds = @()
    })
Check 'buyer creates order' ($r.Status -eq 200 -and $r.Json.data.status -eq 'PENDING_PAYMENT') "status=$($r.Status) raw=$($r.Raw)"
$orderId = $r.Json.data.id
$subOrderId = $r.Json.data.subOrders[0].id
Check 'order total amount is 200' ([decimal]$r.Json.data.totalAmount -eq 200) "total=$($r.Json.data.totalAmount)"

$r = Invoke-Api -Method GET -Path "/api/products/$productId"
Check 'stock is deducted to 8 after order' ($r.Json.data.skus[0].stock -eq 8) "stock=$($r.Json.data.skus[0].stock)"

$r = Invoke-Api -Method GET -Path '/api/cart' -Token $buyerToken
Check 'cart is cleared after order' ($r.Json.data.Count -eq 0) "count=$($r.Json.data.Count)"

$r = Invoke-Api -Method POST -Path "/api/orders/$orderId/pay" -Token $buyerToken -Body (Body @{ channel = 'MOCK' })
Check 'buyer pays order' ($r.Status -eq 200 -and $r.Json.data.status -eq 'PAID') "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method POST -Path "/api/orders/$orderId/pay" -Token $buyerToken -Body (Body @{ channel = 'MOCK' })
Check 'second payment is rejected' ($r.Status -eq 409) "status=$($r.Status)"

# ---------- 6. ship & confirm ----------
Write-Host '-- fulfilment --'
$r = Invoke-Api -Method GET -Path '/api/merchant/orders?status=WAIT_SHIP&page=0&size=10' -Token $merchantToken
Check 'merchant sees wait-ship sub order' ($r.Status -eq 200 -and @($r.Json.data.list).Count -ge 1) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method POST -Path "/api/merchant/orders/$subOrderId/ship" -Token $merchantToken -Body (Body @{
        shipCompany = 'SF'; trackingNo = 'SF123456'
    })
Check 'merchant ships sub order' ($r.Status -eq 200 -and $r.Json.data.status -eq 'SHIPPED') "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path "/api/orders/$orderId/sub-orders/$subOrderId/logistics" -Token $buyerToken
Check 'buyer reads logistics' ($r.Status -eq 200) "status=$($r.Status)"

$r = Invoke-Api -Method POST -Path "/api/orders/$orderId/confirm" -Token $buyerToken
Check 'buyer confirms receipt' ($r.Status -eq 200 -and $r.Json.data.status -eq 'COMPLETED') "status=$($r.Status) raw=$($r.Raw)"

# ---------- 7. after sale: return & refund ----------
Write-Host '-- after sale --'
$r = Invoke-Api -Method POST -Path '/api/after-sales' -Token $buyerToken -Body (Body @{
        subOrderId = $subOrderId; reason = 'smoke return reason'
    })
Check 'buyer applies return refund' ($r.Status -eq 200 -and $r.Json.data.type -eq 'RETURN_REFUND') "status=$($r.Status) raw=$($r.Raw)"
$afterSaleId = $r.Json.data.id

$r = Invoke-Api -Method POST -Path "/api/merchant/after-sales/$afterSaleId/approve" -Token $merchantToken -Body (Body @{
        merchantRemark = 'return to Hangzhou warehouse'
    })
Check 'merchant approves return' ($r.Status -eq 200 -and $r.Json.data.status -eq 'RETURNING') "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method POST -Path "/api/after-sales/$afterSaleId/return-shipment" -Token $buyerToken -Body (Body @{
        returnCompany = 'YTO'; returnTrackingNo = 'YTO987654'
    })
Check 'buyer submits return shipment' ($r.Status -eq 200) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method POST -Path "/api/merchant/after-sales/$afterSaleId/confirm-receipt" -Token $merchantToken
Check 'merchant confirms return and refunds' ($r.Status -eq 200 -and $r.Json.data.status -eq 'REFUNDED') "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path "/api/products/$productId"
Check 'stock is restored to 10 after refund' ($r.Json.data.skus[0].stock -eq 10) "stock=$($r.Json.data.skus[0].stock)"

# ---------- 8. coupon flow ----------
Write-Host '-- coupon --'
$now = Get-Date
$r = Invoke-Api -Method POST -Path '/api/merchant/coupons' -Token $merchantToken -Body (Body @{
        title           = "SmokeCoupon$stamp"
        thresholdAmount = 50
        discountAmount  = 10
        totalCount      = 100
        startAt         = $now.AddDays(-1).ToString('yyyy-MM-ddTHH:mm:ss')
        endAt           = $now.AddDays(1).ToString('yyyy-MM-ddTHH:mm:ss')
    })
Check 'merchant creates coupon' ($r.Status -eq 200 -and $r.Json.data.state -eq 'RUNNING') "status=$($r.Status) raw=$($r.Raw)"
$couponId = $r.Json.data.id

$r = Invoke-Api -Method GET -Path '/api/coupons/available' -Token $buyerToken
$available = @($r.Json.data) | Where-Object { $_.id -eq $couponId }
Check 'coupon appears in claim center' ($r.Status -eq 200 -and @($available).Count -eq 1) "status=$($r.Status)"

$r = Invoke-Api -Method POST -Path "/api/coupons/$couponId/claim" -Token $buyerToken
Check 'buyer claims coupon' ($r.Status -eq 200 -and $r.Json.data.state -eq 'UNUSED') "status=$($r.Status) raw=$($r.Raw)"
$userCouponId = $r.Json.data.id

$r = Invoke-Api -Method POST -Path "/api/coupons/$couponId/claim" -Token $buyerToken
Check 'duplicate claim is rejected' ($r.Status -eq 409) "status=$($r.Status)"

$r = Invoke-Api -Method POST -Path '/api/cart' -Token $buyerToken -Body (Body @{ skuId = $skuRed; quantity = 1 })
$cartItemId2 = $r.Json.data.id

$r = Invoke-Api -Method POST -Path '/api/orders' -Token $buyerToken -Body (Body @{
        addressId = $addressId; remark = 'coupon order'; cartItemIds = @($cartItemId2); userCouponIds = @($userCouponId)
    })
Check 'buyer creates order with coupon' ($r.Status -eq 200) "status=$($r.Status) raw=$($r.Raw)"
$orderId2 = $r.Json.data.id
Check 'coupon discount applied (100 -> 90)' ([decimal]$r.Json.data.totalAmount -eq 90) "total=$($r.Json.data.totalAmount)"
Check 'sub order discount is 10' ([decimal]$r.Json.data.subOrders[0].discountAmount -eq 10) "discount=$($r.Json.data.subOrders[0].discountAmount)"

$r = Invoke-Api -Method GET -Path '/api/coupons/mine?state=USED' -Token $buyerToken
Check 'coupon shows as USED after order' (@($r.Json.data).Count -ge 1) "count=$(@($r.Json.data).Count)"

$r = Invoke-Api -Method POST -Path "/api/orders/$orderId2/cancel" -Token $buyerToken
Check 'buyer cancels pending order' ($r.Status -eq 200 -and $r.Json.data.status -eq 'CANCELLED') "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path '/api/coupons/mine?state=UNUSED' -Token $buyerToken
Check 'coupon is returned after cancel' (@($r.Json.data).Count -ge 1) "count=$(@($r.Json.data).Count)"

$r = Invoke-Api -Method GET -Path "/api/products/$productId"
Check 'stock is restored to 10 after cancel' ($r.Json.data.skus[0].stock -eq 10) "stock=$($r.Json.data.skus[0].stock)"

# ---------- 9. cross-user access ----------
Write-Host '-- authorization boundary --'
$r = Invoke-Api -Method GET -Path "/api/orders/$orderId" -Token $merchantToken
Check 'other user cannot read buyer order (404)' ($r.Status -eq 404) "status=$($r.Status)"

$r = Invoke-Api -Method GET -Path '/api/merchant/orders' -Token $buyerToken
Check 'buyer cannot call merchant api (403)' ($r.Status -eq 403) "status=$($r.Status)"

$r = Invoke-Api -Method POST -Path '/api/admin/shops/1/approve' -Token $merchantToken
Check 'merchant cannot call admin api (403)' ($r.Status -eq 403) "status=$($r.Status)"

# ---------- 10. read-only endpoints ----------
Write-Host '-- dashboards / notifications --'
$r = Invoke-Api -Method GET -Path '/api/admin/dashboard' -Token $adminToken
Check 'admin dashboard works' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path '/api/merchant/stats/overview' -Token $merchantToken
Check 'merchant stats works' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path '/api/merchant/inventory/summary' -Token $merchantToken
Check 'merchant inventory summary works' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path '/api/notifications?page=0&size=10' -Token $buyerToken
Check 'buyer notifications work' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status) raw=$($r.Raw)"

$r = Invoke-Api -Method GET -Path '/api/content/home'
Check 'home content is public' ($r.Status -eq 200 -and $r.Json.code -eq 0) "status=$($r.Status)"

Write-Host ''
Write-Host "=== Result: $script:Pass passed, $script:Fail failed ==="
if ($script:Fail -gt 0) {
    $script:Failures | ForEach-Object { Write-Host " - $_" }
    exit 1
}
exit 0
