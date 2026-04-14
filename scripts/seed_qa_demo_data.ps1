param(
    [string]$BaseUrl = 'http://localhost:8080/api'
)

$ErrorActionPreference = 'Stop'

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

function Login-User {
    param(
        [string]$Username,
        [string]$Password
    )
    $response = Invoke-JsonPost -Url "$BaseUrl/auth/login" -Body @{
        username = $Username
        password = $Password
    }
    @{ Authorization = "Bearer $($response.data.token)" }
}

function Get-TagMap {
    $response = Invoke-JsonGet -Url "$BaseUrl/public/taxonomy/tags"
    $map = @{}
    foreach ($item in $response.data) {
        $map[$item.name] = [int]$item.id
    }
    return $map
}

function Ensure-Tag {
    param(
        [hashtable]$Headers,
        [string]$Name
    )
    try {
        Invoke-JsonPost -Url "$BaseUrl/admin/taxonomy/tags" -Headers $Headers -Body @{ name = $Name } | Out-Null
    } catch {
        $msg = $_.ErrorDetails.Message
        if (($msg -notmatch 'exists') -and ($msg -notmatch '已存在')) {
            throw
        }
    }
}

function Get-ExistingQuestionTitles {
    $response = Invoke-JsonGet -Url "$BaseUrl/qa/questions"
    $titles = @{}
    foreach ($item in ($response.data | Where-Object { $_ })) {
        $titles[[string]$item.title] = $true
    }
    return $titles
}

function Create-Question {
    param(
        [hashtable]$Headers,
        [string]$Title,
        [string]$Summary,
        [string]$Body,
        [int]$CategoryId,
        [string[]]$TagNames,
        [hashtable]$TagMap
    )
    $tagIds = foreach ($tagName in $TagNames) {
        if (-not $TagMap.ContainsKey($tagName)) {
            throw "Tag not found: $tagName"
        }
        $TagMap[$tagName]
    }

    $response = Invoke-JsonPost -Url "$BaseUrl/content" -Headers $Headers -Body @{
        type = 'QUESTION'
        title = $Title
        summary = $Summary
        body = $Body
        categoryId = $CategoryId
        visibility = 'PUBLIC'
        tagIds = $tagIds
    }
    return [int64]$response.data
}

function Approve-Content {
    param(
        [hashtable]$Headers,
        [int64]$ContentId
    )
    Invoke-JsonPost -Url "$BaseUrl/admin/audit/$ContentId/approve" -Headers $Headers -Body @{} | Out-Null
}

function Add-Answer {
    param(
        [hashtable]$Headers,
        [int64]$QuestionId,
        [string]$Body
    )
    $response = Invoke-JsonPost -Url "$BaseUrl/qa/$QuestionId/answer" -Headers $Headers -Body @{ body = $Body }
    return [int64]$response.data
}

function Pick-BestAnswer {
    param(
        [hashtable]$Headers,
        [int64]$QuestionId,
        [int64]$AnswerId
    )
    Invoke-JsonPost -Url "$BaseUrl/qa/$QuestionId/best" -Headers $Headers -Body @{ answerId = $AnswerId } | Out-Null
}

$adminHeaders = Login-User -Username 'admin_master' -Password 'Admin@123456'
$linHeaders = Login-User -Username 'author_lin' -Password 'User@123456'
$qinHeaders = Login-User -Username 'author_qin' -Password 'User@123456'
$songHeaders = Login-User -Username 'author_song' -Password 'User@123456'
$xuHeaders = Login-User -Username 'reader_xu' -Password 'User@123456'
$heHeaders = Login-User -Username 'reader_he' -Password 'User@123456'

foreach ($tagName in @('Docker', 'Redis', 'SpringBoot', 'Vue3', 'Java')) {
    Ensure-Tag -Headers $adminHeaders -Name $tagName
}

$tagMap = Get-TagMap
$existingTitles = Get-ExistingQuestionTitles
$createdQuestions = @()
$skippedExistingTitles = @()

$questions = @(
    @{
        headers = $linHeaders
        categoryId = 1
        title = 'Docker 启动后前端页面空白，应该优先排查哪些环节？'
        summary = '想梳理一份适合毕业设计演示前自检的排查顺序，避免每次都是临时抓日志。'
        body = @"
<p>我现在把前后端、MySQL 和 Redis 都放进了 Docker Compose 里统一启动，但有时容器看起来已经运行，浏览器里却还是首页空白或接口不可用。</p>
<p>为了在答辩前把排查流程固定下来，我想知道如果再次遇到这类问题，最应该优先检查哪些环节，才能尽快定位是前端构建、Nginx 代理、后端启动，还是数据库依赖没有就绪。</p>
<h2>我目前会看的信息</h2>
<p>包括容器状态、前端控制台、后端日志以及接口返回码，但顺序还不够稳定，经常会漏掉真正关键的一步。</p>
"@
        tags = @('Docker', 'Redis', 'SpringBoot')
        answers = @(
            @{ headers = $xuHeaders; body = '我建议先按“容器状态 -> 后端健康接口 -> 前端静态资源 -> Nginx 代理 -> 数据库依赖”这个顺序检查。先确认 `docker compose ps` 里服务是否都在运行，再看后端健康接口能否返回 200，这样能最快区分是前端壳子问题还是后端根本没起来。' },
            @{ headers = $heHeaders; body = '如果是答辩场景，最好把关键检查命令固定成脚本，例如先看容器日志尾部，再访问后端登录接口和前端首页。这样就不会每次靠手工判断，排查效率会稳定很多。'; best = $true }
        )
    },
    @{
        headers = $qinHeaders
        categoryId = 2
        title = '知识社区首页卡片信息太多时，前端应该如何控制阅读节奏？'
        summary = '首页既要展示标题、摘要、作者、时间、互动数据，又不能让页面显得拥挤，想听听更适合知识社区的做法。'
        body = @"
<p>我在做个人知识分享平台首页时，发现卡片里如果把分类、标签、作者、时间、阅读、点赞、收藏全部平铺出来，用户很难一眼抓住重点。</p>
<p>但如果信息删得太狠，又担心用户无法快速判断内容质量。有没有更适合知识社区的信息层级组织方式，可以兼顾浏览效率和信息完整性？</p>
<h2>目前的想法</h2>
<p>优先突出标题和摘要，把作者、时间和互动数据收成次级信息，但还不确定这样是否足够支撑用户做点击决策。</p>
"@
        tags = @('Vue3', 'Java')
        answers = @(
            @{ headers = $songHeaders; body = '首页卡片最重要的是先让用户看到“值不值得点进去”，所以标题、摘要必须占主导。作者和时间保留，但互动数据建议弱化成右侧或底部统计，不要和标题竞争视觉权重。' },
            @{ headers = $xuHeaders; body = '如果你已经有分类筛选和标签筛选，其实卡片内就不必再重复强调太多标签。让卡片承担“吸引点击”的任务，把“辅助筛选”的任务交给列表上方控件，会更清晰。'; best = $true }
        )
    },
    @{
        headers = $songHeaders
        categoryId = 1
        title = '毕业设计答辩时，如何更自然地演示“审核驳回”和“内容下架”的区别？'
        summary = '系统里两个功能都做了，但讲解时容易被老师听成同一件事，想整理一套更有区分度的演示方法。'
        body = @"
<p>我现在的平台已经把“驳回”和“下架”做成了两个真正不同的流程：驳回只针对待审核内容，下架只针对已发布内容。</p>
<p>不过在实际演示时，如果只是口头讲状态变化，老师可能仍然觉得它们很像。我想知道怎样设计演示顺序，才能让这两个动作的业务差异更直观。</p>
<h2>已有基础</h2>
<p>前端页面已经区分了按钮，后端也限制了不同状态下的操作范围，现在更需要的是演示和讲解上的组织方法。</p>
"@
        tags = @('SpringBoot', 'Docker', 'Java')
        answers = @(
            @{ headers = $qinHeaders; body = '可以先用一篇新提交内容演示“待审核 -> 驳回”，让老师看到它从未公开出现；再用一篇已经发布并能在首页看到的内容演示“已发布 -> 下架”，这样两者的区别会非常清楚。' },
            @{ headers = $heHeaders; body = '最好把讲解重点落在“作用阶段不同”：驳回发生在上线前，下架发生在上线后。再配合通知记录和内容是否对外可见这两个现象，老师通常会很容易理解。'; best = $true }
        )
    }
)

foreach ($question in $questions) {
    if ($existingTitles.ContainsKey($question.title)) {
        $skippedExistingTitles += $question.title
        continue
    }

    $questionId = Create-Question -Headers $question.headers `
        -Title $question.title `
        -Summary $question.summary `
        -Body $question.body `
        -CategoryId $question.categoryId `
        -TagNames $question.tags `
        -TagMap $tagMap

    Approve-Content -Headers $adminHeaders -ContentId $questionId

    $bestAnswerId = $null
    foreach ($answer in $question.answers) {
        $answerId = Add-Answer -Headers $answer.headers -QuestionId $questionId -Body $answer.body
        if ($answer.best) {
            $bestAnswerId = $answerId
        }
    }
    if ($null -ne $bestAnswerId) {
        Pick-BestAnswer -Headers $question.headers -QuestionId $questionId -AnswerId $bestAnswerId
    }

    $createdQuestions += [pscustomobject]@{
        id = $questionId
        title = $question.title
        answerCount = $question.answers.Count
    }
}

[pscustomobject]@{
    createdQuestions = $createdQuestions
    skippedExistingTitles = $skippedExistingTitles
} | ConvertTo-Json -Depth 6



