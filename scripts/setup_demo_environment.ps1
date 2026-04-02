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
    -Title '在知识社区中构建审核、通知与积分联动闭环的实践' `
    -Summary '本文围绕内容审核、作者通知与积分激励三条链路，说明知识社区如何形成稳定可持续的产品闭环。' `
    -Body @"
<p>一个知识社区要想建立可信度，前提是发布、审核与反馈能够形成统一且连贯的处理流程。</p>
<p>在这套实现中，文章先进入待审核队列，再由管理员给出审核结果，最后触发作者通知与积分变更，形成完整业务闭环。</p>
<h2>为什么这个闭环重要</h2>
<p>没有审核，内容质量就容易失控；没有通知，作者无法理解平台反馈；没有积分激励，社区也很难形成长期稳定的创作动力。</p>
<h2>实现中的关键决策</h2>
<p>我统一了内容状态流转，记录每一次审核动作的日志，同时为积分奖励增加幂等业务键，避免重复审核导致重复发分。</p>
<p>这样一来，作者拥有清晰的发布路径，管理员拥有可追溯的操作记录，平台也拥有鼓励优质创作的可靠机制。</p>
"@ `
    -CategoryId 1 -TagIds @(1, 5)

$article2 = Create-Article -Headers $qinHeaders `
    -Title '知识社区首页信息流重构：面向深度阅读与持续互动的设计思路' `
    -Summary '本文围绕首页信息层级、筛选布局与内容卡片结构，讨论如何打造更适合知识社区的阅读体验。' `
    -Body @"
<p>知识平台的首页不应该只是内容记录的堆叠，它本质上是用户发现内容的第一入口，直接影响浏览、判断与回访意愿。</p>
<p>这次改版中，我重点关注了首屏视觉区、筛选入口的摆放方式，以及每张内容卡片中标题、摘要与元信息之间的关系。</p>
<h2>首页首先要解决什么问题</h2>
<p>用户需要在极短时间内理解平台主要有哪些内容，并能够通过分类、标签和排序规则快速缩小浏览范围。</p>
<h2>为什么阅读节奏很重要</h2>
<p>如果作者、发布时间和互动数据与标题抢占同等视觉权重，用户视线就会被打散。强化标题区、弱化次要元信息，能显著提升扫读效率。</p>
<p>因此，一个好的首页不只是展示层，更是持续引导用户阅读、评论与发布的互动入口。</p>
"@ `
    -CategoryId 2 -TagIds @(2)

$article3 = Create-Article -Headers $songHeaders `
    -Title '知识社区毕业设计演示如何组织：从核心流程到亮点模块的展示方法' `
    -Summary '本文说明如何将一期、二期和三期能力整理成清晰有说服力的答辩演示流程。' `
    -Body @"
<p>对于毕业设计来说，只有实现功能还不够，系统还需要一套能够让老师迅速理解完整产品闭环的展示结构。</p>
<p>我将平台划分为三个阶段：第一阶段聚焦发布、审核、搜索与互动；第二阶段扩展到推荐、个人空间与通知；第三阶段突出分析、增长与治理能力。</p>
<h2>答辩中应该重点强调什么</h2>
<p>最有说服力的点是完整业务闭环：用户发布内容，管理员审核，审核通过后内容对外可见，互动数据被持续记录，并进一步沉淀为增长与统计分析能力。</p>
<h2>推荐的演示顺序</h2>
<p>建议先展示普通用户发文，再切换管理员完成审核，最后回到前端页面展示审核结果、社区评论以及作者成长数据带来的变化。</p>
"@ `
    -CategoryId 1 -TagIds @(1, 3, 5)

foreach ($articleId in @($article1, $article2, $article3)) {
    Approve-Article -Headers $adminHeaders -ContentId $articleId
}

foreach ($articleId in @($article1, $article2, $article3)) {
    Invoke-JsonGet -Url "$baseUrl/content/$articleId" -Headers $xuHeaders | Out-Null
    Invoke-JsonGet -Url "$baseUrl/content/$articleId" -Headers $heHeaders | Out-Null
}

$commentA1 = Add-Comment -Headers $xuHeaders -ContentId $article1 -Body '这篇文章把审核、通知和激励闭环解释得很清楚，尤其是审核日志这一点很适合在答辩时展示。'
$commentA2 = Add-Comment -Headers $heHeaders -ContentId $article1 -Body '我很认同这里的幂等设计，没有唯一业务键的话，重复审核确实很容易造成重复积分。'
Add-Comment -Headers $linHeaders -ContentId $article1 -Body '这正是我在积分日志里引入业务键的原因，能大幅降低重复发奖励这类问题。' -ParentId $commentA2 | Out-Null

$commentB1 = Add-Comment -Headers $linHeaders -ContentId $article2 -Body '如果首页结构做得不清晰，后面的推荐和搜索模块也很难体现价值，这篇文章把这种关系讲得很明白。'
Add-Comment -Headers $xuHeaders -ContentId $article2 -Body '把标题和摘要放在更高优先级、弱化元信息之后，信息流确实更容易快速浏览。' -ParentId $commentB1 | Out-Null

$commentC1 = Add-Comment -Headers $heHeaders -ContentId $article3 -Body '把平台拆成三个阶段来讲，非常适合在毕业答辩里说明范围边界和模块亮点。'
Add-Comment -Headers $songHeaders -ContentId $article3 -Body '是的，而且这样也能让演示顺序从发布、审核自然过渡到互动与增长分析。' -ParentId $commentC1 | Out-Null

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
        @{ id = $article1; title = '在知识社区中构建审核、通知与积分联动闭环的实践' },
        @{ id = $article2; title = '知识社区首页信息流重构：面向深度阅读与持续互动的设计思路' },
        @{ id = $article3; title = '知识社区毕业设计演示如何组织：从核心流程到亮点模块的展示方法' }
    )
}

$summary | ConvertTo-Json -Depth 6
