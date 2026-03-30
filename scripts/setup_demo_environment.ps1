$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$h2Jar = Join-Path $env:USERPROFILE '.m2\repository\com\h2database\h2\2.2.224\h2-2.2.224.jar'
$dbUrl = 'jdbc:h2:file:./data/knowledge_platform;MODE=MySQL;DATABASE_TO_LOWER=TRUE;AUTO_SERVER=TRUE;NON_KEYWORDS=USER'
$sqlScript = Join-Path $PSScriptRoot 'reset_demo_data.sql'
$baseUrl = 'http://localhost:8080/api'

function Invoke-JsonPost {
    param(
        [string]$Url,
        [hashtable]$Body,
        [hashtable]$Headers = @{}
    )
    Invoke-RestMethod -Method Post -Uri $Url -Headers $Headers -ContentType 'application/json; charset=utf-8' -Body ($Body | ConvertTo-Json -Depth 10)
}

function Invoke-JsonGet {
    param(
        [string]$Url,
        [hashtable]$Headers = @{},
        [hashtable]$Query = @{}
    )
    if ($Query.Count -gt 0) {
        $pairs = $Query.GetEnumerator() | ForEach-Object {
            '{0}={1}' -f [System.Uri]::EscapeDataString([string]$_.Key), [System.Uri]::EscapeDataString([string]$_.Value)
        }
        $Url = '{0}?{1}' -f $Url, ($pairs -join '&')
    }
    Invoke-RestMethod -Method Get -Uri $Url -Headers $Headers
}

function Run-H2Script {
    param([string]$ScriptPath)
    Push-Location (Join-Path $root 'backend')
    try {
        & java -cp $h2Jar org.h2.tools.RunScript -url $dbUrl -user sa -script $ScriptPath | Out-Null
    } finally {
        Pop-Location
    }
}

function Run-H2Sql {
    param([string]$Sql)
    Push-Location (Join-Path $root 'backend')
    try {
        & java -cp $h2Jar org.h2.tools.Shell -url $dbUrl -user sa -sql $Sql | Out-Null
    } finally {
        Pop-Location
    }
}

function Register-User {
    param(
        [string]$Username,
        [string]$Password,
        [string]$Nickname
    )
    Invoke-JsonPost -Url "$baseUrl/auth/register" -Body @{
        username = $Username
        password = $Password
        nickname = $Nickname
    } | Out-Null
}

function Login-User {
    param(
        [string]$Username,
        [string]$Password
    )
    $response = Invoke-JsonPost -Url "$baseUrl/auth/login" -Body @{
        username = $Username
        password = $Password
    }
    @{ Authorization = "Bearer $($response.data.token)" }
}

function Create-Article {
    param(
        [hashtable]$Headers,
        [string]$Title,
        [string]$Summary,
        [string]$Body,
        [int]$CategoryId,
        [int[]]$TagIds
    )
    $response = Invoke-JsonPost -Url "$baseUrl/content" -Headers $Headers -Body @{
        type = 'ARTICLE'
        title = $Title
        summary = $Summary
        body = $Body
        categoryId = $CategoryId
        visibility = 'PUBLIC'
        tagIds = $TagIds
    }
    [int64]$response.data
}

function Approve-Article {
    param(
        [hashtable]$Headers,
        [int64]$ContentId
    )
    Invoke-JsonPost -Url "$baseUrl/admin/audit/$ContentId/approve" -Headers $Headers -Body @{} | Out-Null
}

function Add-Comment {
    param(
        [hashtable]$Headers,
        [int64]$ContentId,
        [string]$Body,
        [Nullable[int64]]$ParentId = $null
    )
    $payload = @{ body = $Body }
    if ($null -ne $ParentId) {
        $payload.parentId = $ParentId
    }
    $response = Invoke-JsonPost -Url "$baseUrl/interaction/comment/$ContentId" -Headers $Headers -Body $payload
    [int64]$response.data
}

function Toggle-Like {
    param(
        [hashtable]$Headers,
        [int64]$TargetId,
        [string]$TargetType
    )
    Invoke-JsonPost -Url "$baseUrl/interaction/like/toggle" -Headers $Headers -Body @{
        targetId = $TargetId
        targetType = $TargetType
    } | Out-Null
}

function Toggle-Favorite {
    param(
        [hashtable]$Headers,
        [int64]$ContentId
    )
    Invoke-JsonPost -Url "$baseUrl/interaction/favorite/$ContentId/toggle" -Headers $Headers -Body @{} | Out-Null
}

Run-H2Script -ScriptPath $sqlScript

$users = @(
    @{ username = 'admin_master'; password = 'Admin@123456'; nickname = 'System Admin' },
    @{ username = 'author_lin'; password = 'User@123456'; nickname = 'Lin Zhiyuan' },
    @{ username = 'author_qin'; password = 'User@123456'; nickname = 'Qin Ruoxi' },
    @{ username = 'author_song'; password = 'User@123456'; nickname = 'Song Mingzhe' },
    @{ username = 'reader_xu'; password = 'User@123456'; nickname = 'Xu Wensheng' },
    @{ username = 'reader_he'; password = 'User@123456'; nickname = 'He Qingjia' }
)

foreach ($user in $users) {
    Register-User -Username $user.username -Password $user.password -Nickname $user.nickname
}

Run-H2Sql -Sql @"
UPDATE user SET role = 'ADMIN', nickname = 'System Admin' WHERE username = 'admin_master';
UPDATE user SET nickname = 'Lin Zhiyuan' WHERE username = 'author_lin';
UPDATE user SET nickname = 'Qin Ruoxi' WHERE username = 'author_qin';
UPDATE user SET nickname = 'Song Mingzhe' WHERE username = 'author_song';
UPDATE user SET nickname = 'Xu Wensheng' WHERE username = 'reader_xu';
UPDATE user SET nickname = 'He Qingjia' WHERE username = 'reader_he';
"@

$adminHeaders = Login-User -Username 'admin_master' -Password 'Admin@123456'
$linHeaders = Login-User -Username 'author_lin' -Password 'User@123456'
$qinHeaders = Login-User -Username 'author_qin' -Password 'User@123456'
$songHeaders = Login-User -Username 'author_song' -Password 'User@123456'
$xuHeaders = Login-User -Username 'reader_xu' -Password 'User@123456'
$heHeaders = Login-User -Username 'reader_he' -Password 'User@123456'

$article1 = Create-Article -Headers $linHeaders `
    -Title 'Building a complete review, notification and points loop in a Spring Boot knowledge community' `
    -Summary 'This article explains how content review, author notification and incentive points can be connected into a stable product loop.' `
    -Body @"
<p>A knowledge community becomes credible only when publishing, moderation and feedback are connected as one consistent process.</p>
<p>In this implementation, a post first enters the review queue, then receives an audit result from the administrator, and finally triggers notification and points updates for the author.</p>
<h2>Why this loop matters</h2>
<p>Without review, content quality becomes unstable. Without notification, authors do not understand the platform response. Without points, the community lacks a durable incentive mechanism.</p>
<h2>Key implementation decisions</h2>
<p>We unified content statuses, stored audit logs for every moderation action, and added idempotent business keys for point rewards so repeated approvals would not create repeated rewards.</p>
<p>This structure gives authors a clear publishing path, gives administrators traceable operations, and gives the platform a reliable way to encourage valuable content creation.</p>
"@ `
    -CategoryId 1 -TagIds @(1, 5)

$article2 = Create-Article -Headers $qinHeaders `
    -Title 'Redesigning the homepage feed of a knowledge community for long-form reading and continuous interaction' `
    -Summary 'This article discusses homepage information hierarchy, filter layout and content card design for a healthier reading experience.' `
    -Body @"
<p>The homepage of a knowledge platform should not be only a list of records. It is the first layer of content discovery and therefore shapes how users browse, evaluate and return.</p>
<p>During this redesign, I focused on the hero section, filter placement, and the relationship between headline, summary and metadata in every content card.</p>
<h2>What the homepage must solve first</h2>
<p>Users need to quickly understand what kind of content the platform contains and how they can narrow the feed by category, tag and ranking rules.</p>
<h2>Why reading rhythm matters</h2>
<p>If author, time and metrics compete with the headline, the eye loses focus. A stronger title area with lighter metadata improves scanning efficiency and makes the feed feel calmer.</p>
<p>A good homepage is therefore not only a display layer. It is an interaction layer that continuously encourages reading, commenting and publishing.</p>
"@ `
    -CategoryId 2 -TagIds @(2)

$article3 = Create-Article -Headers $songHeaders `
    -Title 'How to organize a graduation project demo for a knowledge community platform from core flow to highlight modules' `
    -Summary 'This article explains how to present phase one, phase two and phase three capabilities in a clean and persuasive defense flow.' `
    -Body @"
<p>For a graduation project, implementation quality alone is not enough. The system also needs a presentation structure that allows teachers to understand the complete product loop quickly.</p>
<p>I split the platform into three phases. The first phase focuses on publishing, moderation, search and interaction. The second phase extends to recommendation, personal space and notifications. The third phase highlights analytics, growth and governance.</p>
<h2>What should be emphasized in the defense</h2>
<p>The most persuasive point is the complete loop: users publish content, administrators review it, approved content becomes visible, interactions are recorded, and the accumulated data feeds growth and reporting modules.</p>
<h2>Recommended demo order</h2>
<p>Start with a normal user creating content, switch to the administrator for moderation, then return to the front-end to show the approved result, community comments and the author growth impact.</p>
"@ `
    -CategoryId 1 -TagIds @(1, 3, 5)

foreach ($articleId in @($article1, $article2, $article3)) {
    Approve-Article -Headers $adminHeaders -ContentId $articleId
}

foreach ($articleId in @($article1, $article2, $article3)) {
    Invoke-JsonGet -Url "$baseUrl/content/$articleId" -Headers $xuHeaders | Out-Null
    Invoke-JsonGet -Url "$baseUrl/content/$articleId" -Headers $heHeaders | Out-Null
}

$commentA1 = Add-Comment -Headers $xuHeaders -ContentId $article1 -Body 'This post explains the moderation, notification and incentive loop very clearly. The audit log point is especially useful for a defense presentation.'
$commentA2 = Add-Comment -Headers $heHeaders -ContentId $article1 -Body 'I strongly agree with the idempotency decision. Without a unique business key, repeated approval can easily create repeated point rewards.'
Add-Comment -Headers $linHeaders -ContentId $article1 -Body 'That was exactly the reason for introducing a business key into the point log. It makes repeated reward bugs much easier to prevent.' -ParentId $commentA2 | Out-Null

$commentB1 = Add-Comment -Headers $linHeaders -ContentId $article2 -Body 'If the homepage structure is weak, later work on recommendation and search will also feel weak. This article makes that relationship very clear.'
Add-Comment -Headers $xuHeaders -ContentId $article2 -Body 'The decision to prioritize headline and summary while weakening metadata makes the feed much easier to scan.' -ParentId $commentB1 | Out-Null

$commentC1 = Add-Comment -Headers $heHeaders -ContentId $article3 -Body 'Splitting the platform into three phases is a very effective way to explain the scope and highlight modules during a graduation defense.'
Add-Comment -Headers $songHeaders -ContentId $article3 -Body 'Yes, and it also gives the demo a more natural sequence from publishing to moderation and then to growth analysis.' -ParentId $commentC1 | Out-Null

Toggle-Like -Headers $xuHeaders -TargetId $article1 -TargetType 'CONTENT'
Toggle-Like -Headers $heHeaders -TargetId $article1 -TargetType 'CONTENT'
Toggle-Favorite -Headers $xuHeaders -ContentId $article1
Toggle-Favorite -Headers $heHeaders -ContentId $article1

Toggle-Like -Headers $linHeaders -TargetId $article2 -TargetType 'CONTENT'
Toggle-Like -Headers $heHeaders -TargetId $article2 -TargetType 'CONTENT'
Toggle-Favorite -Headers $linHeaders -ContentId $article2

Toggle-Like -Headers $xuHeaders -TargetId $article3 -TargetType 'CONTENT'
Toggle-Like -Headers $qinHeaders -TargetId $article3 -TargetType 'CONTENT'
Toggle-Favorite -Headers $heHeaders -ContentId $article3

Toggle-Like -Headers $linHeaders -TargetId $commentA1 -TargetType 'COMMENT'
Toggle-Like -Headers $songHeaders -TargetId $commentC1 -TargetType 'COMMENT'

$summary = [pscustomobject]@{
    accounts = @(
        @{ role = 'ADMIN'; username = 'admin_master'; password = 'Admin@123456'; nickname = 'System Admin' },
        @{ role = 'USER'; username = 'author_lin'; password = 'User@123456'; nickname = 'Lin Zhiyuan' },
        @{ role = 'USER'; username = 'author_qin'; password = 'User@123456'; nickname = 'Qin Ruoxi' },
        @{ role = 'USER'; username = 'author_song'; password = 'User@123456'; nickname = 'Song Mingzhe' },
        @{ role = 'USER'; username = 'reader_xu'; password = 'User@123456'; nickname = 'Xu Wensheng' },
        @{ role = 'USER'; username = 'reader_he'; password = 'User@123456'; nickname = 'He Qingjia' }
    )
    articles = @(
        @{ id = $article1; title = 'Building a complete review, notification and points loop in a Spring Boot knowledge community' },
        @{ id = $article2; title = 'Redesigning the homepage feed of a knowledge community for long-form reading and continuous interaction' },
        @{ id = $article3; title = 'How to organize a graduation project demo for a knowledge community platform from core flow to highlight modules' }
    )
}

$summary | ConvertTo-Json -Depth 6
