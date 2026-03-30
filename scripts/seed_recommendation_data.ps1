$ErrorActionPreference = 'Stop'

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
        [hashtable]$Headers = @{}
    )
    Invoke-RestMethod -Method Get -Uri $Url -Headers $Headers
}

function Register-User {
    param([string]$Username,[string]$Password,[string]$Nickname)
    try {
        Invoke-JsonPost -Url "$baseUrl/auth/register" -Body @{ username = $Username; password = $Password; nickname = $Nickname } | Out-Null
    } catch {
        $msg = $_.ErrorDetails.Message
        if (($msg -notmatch 'exists') -and ($msg -notmatch '已存在')) { throw }
    }
}

function Login-User {
    param([string]$Username,[string]$Password)
    $response = Invoke-JsonPost -Url "$baseUrl/auth/login" -Body @{ username = $Username; password = $Password }
    @{ Authorization = "Bearer $($response.data.token)" }
}

function Ensure-Tag {
    param([hashtable]$Headers,[string]$Name)
    try {
        Invoke-JsonPost -Url "$baseUrl/admin/taxonomy/tags" -Headers $Headers -Body @{ name = $Name } | Out-Null
    } catch {
        $msg = $_.ErrorDetails.Message
        if (($msg -notmatch 'exists') -and ($msg -notmatch '已存在')) { throw }
    }
}

function Get-TagMap {
    $response = Invoke-JsonGet -Url "$baseUrl/public/taxonomy/tags"
    $map = @{}
    foreach ($item in $response.data) { $map[$item.name] = [int]$item.id }
    $map
}

function Create-Article {
    param([hashtable]$Headers,[string]$Title,[string]$Summary,[string]$Body,[int]$CategoryId,[int[]]$TagIds)
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
    param([hashtable]$Headers,[int64]$ContentId)
    Invoke-JsonPost -Url "$baseUrl/admin/audit/$ContentId/approve" -Headers $Headers -Body @{} | Out-Null
}

function Add-Comment {
    param([hashtable]$Headers,[int64]$ContentId,[string]$Body,[Nullable[int64]]$ParentId = $null)
    $payload = @{ body = $Body }
    if ($null -ne $ParentId) { $payload.parentId = $ParentId }
    $response = Invoke-JsonPost -Url "$baseUrl/interaction/comment/$ContentId" -Headers $Headers -Body $payload
    [int64]$response.data
}

function Toggle-Like {
    param([hashtable]$Headers,[int64]$TargetId,[string]$TargetType)
    Invoke-JsonPost -Url "$baseUrl/interaction/like/toggle" -Headers $Headers -Body @{ targetId = $TargetId; targetType = $TargetType } | Out-Null
}

function Toggle-Favorite {
    param([hashtable]$Headers,[int64]$ContentId)
    Invoke-JsonPost -Url "$baseUrl/interaction/favorite/$ContentId/toggle" -Headers $Headers -Body @{} | Out-Null
}

$accounts = @(
    @{ username = 'author_chen'; password = 'User@123456'; nickname = '陈以衡'; role = '作者' },
    @{ username = 'author_luo'; password = 'User@123456'; nickname = '罗清越'; role = '作者' },
    @{ username = 'author_gu'; password = 'User@123456'; nickname = '顾明安'; role = '作者' },
    @{ username = 'reader_tan'; password = 'User@123456'; nickname = '谭书言'; role = '读者' },
    @{ username = 'reader_zhou'; password = 'User@123456'; nickname = '周沐禾'; role = '读者' },
    @{ username = 'reader_wu'; password = 'User@123456'; nickname = '吴知夏'; role = '读者' }
)

foreach ($account in $accounts) { Register-User -Username $account.username -Password $account.password -Nickname $account.nickname }

$adminHeaders = Login-User -Username 'admin_master' -Password 'Admin@123456'
$chenHeaders = Login-User -Username 'author_chen' -Password 'User@123456'
$luoHeaders = Login-User -Username 'author_luo' -Password 'User@123456'
$guHeaders = Login-User -Username 'author_gu' -Password 'User@123456'
$tanHeaders = Login-User -Username 'reader_tan' -Password 'User@123456'
$zhouHeaders = Login-User -Username 'reader_zhou' -Password 'User@123456'
$wuHeaders = Login-User -Username 'reader_wu' -Password 'User@123456'
$xuHeaders = Login-User -Username 'reader_xu' -Password 'User@123456'
$heHeaders = Login-User -Username 'reader_he' -Password 'User@123456'

foreach ($tagName in @('Docker','WebSocket','Elasticsearch','React','Python','DevOps','AI','Recommendation')) {
    Ensure-Tag -Headers $adminHeaders -Name $tagName
}

$tagMap = Get-TagMap

$bodyDocker = @"
<p>在个人知识分享与交流平台中，部署能力不仅决定系统是否能运行，更决定教师在答辩现场能否稳定看到完整功能。</p>
<p>本文从 Docker 容器编排、Redis 缓存接入与应用进程管理三个方面，总结我在知识社区项目中的工程化经验。</p>
<h2>一、为什么部署能力本身就是系统能力</h2>
<p>如果系统只能在开发环境中偶尔跑通，那么发布、审核、推荐、互动这些业务模块都无法被稳定展示。通过容器化，可以把数据库、缓存和应用服务固定在可重复执行的环境中。</p>
<h2>二、Redis 在知识社区中的价值</h2>
<p>推荐结果、热门内容、首页列表与统计数据都具有明显的读取高频特征。将热点数据放入 Redis，可以降低数据库读取压力，也为后续热榜与推荐模块打下基础。</p>
<h2>三、答辩场景下的实践建议</h2>
<p>建议将后端、前端、数据库和缓存的启动方式全部脚本化，确保在重启后可以快速恢复演示数据与系统状态，避免现场排障影响展示节奏。</p>
"@

$bodySocket = @"
<p>当用户提交文章、收到评论或获得最佳答案时，平台需要及时反馈结果。单纯依赖轮询会降低体验，因此实时通知能力具有明确价值。</p>
<p>本文结合审核流与问答模块，说明如何以 WebSocket 为核心建立实时消息链路。</p>
<h2>一、哪些场景适合实时推送</h2>
<p>审核通过、审核驳回、评论回复、关注作者发文、提问获得新回答，这些都属于用户感知强烈且对时效敏感的场景。</p>
<h2>二、为什么要和通知中心结合</h2>
<p>实时推送解决的是“第一时间知道”，通知中心解决的是“事后还能回看”。两者结合，才能兼顾交互体验与信息可追溯性。</p>
<h2>三、工程落地建议</h2>
<p>可以先保留站内通知作为稳定基础，再逐步引入 WebSocket 推送。这样既能满足毕设演示，又便于后续扩展。</p>
"@

$bodyFeed = @"
<p>知识社区首页并不是简单的内容列表，而是用户进行发现、筛选与互动的首要入口。它直接决定用户是否愿意继续阅读与参与讨论。</p>
<p>本文从信息层级、筛选入口和内容卡片三方面，分析首页为何需要更接近社区产品的浏览节奏。</p>
<h2>一、为什么首页不能像后台表格</h2>
<p>后台表格强调管理效率，而知识社区首页强调阅读效率。标题、摘要、作者、标签和互动数据需要形成清晰的视觉主次，而不是并列堆叠。</p>
<h2>二、前端协同的关键点</h2>
<p>无论采用 Vue 还是 React，关键都在于接口结构稳定、状态管理清晰，以及筛选条件变化后能快速更新内容列表。</p>
<h2>三、对推荐模块的帮助</h2>
<p>当首页信息流结构稳定后，推荐内容就可以自然融入主浏览链路，提升个性化内容的点击率与停留时间。</p>
"@

$bodySearch = @"
<p>个性化推荐的前提不是复杂算法，而是高质量内容标签、行为事件与检索能力的协同建设。</p>
<p>本文从 Elasticsearch 检索与 Python 数据建模两个角度，说明推荐功能为什么需要先把数据基础打牢。</p>
<h2>一、检索与推荐不是孤立模块</h2>
<p>搜索词、点击记录、点赞收藏、停留行为都可以反哺推荐系统，帮助平台建立更准确的用户兴趣画像。</p>
<h2>二、为什么引入 Elasticsearch</h2>
<p>当内容数量逐步上升后，简单数据库检索难以兼顾召回质量与查询速度。Elasticsearch 可以在分词、相关性排序和聚合分析上提供更强支持。</p>
<h2>三、Python 适合做什么</h2>
<p>推荐排序实验、标签权重计算和离线分析脚本，都可以先用 Python 快速验证，再逐步沉淀到正式服务中。</p>
"@

$bodyRec = @"
<p>在毕业设计中，推荐模块往往容易停留在公式层面。要让它真正具有说服力，必须让实验数据、用户行为和评估指标之间形成完整对应关系。</p>
<p>本文围绕标签向量、行为权重和排序得分，说明推荐实验设计的基本思路。</p>
<h2>一、用户兴趣画像如何得到</h2>
<p>可以基于浏览、点赞、收藏、评论和关注行为，对标签赋予不同权重，并按时间衰减构建兴趣画像。</p>
<h2>二、排序模型如何解释</h2>
<p>排序分值不必一开始就复杂，可以先采用线性加权模型，将相似度、热度、新鲜度和社交关系纳入统一框架，再逐步优化参数。</p>
<h2>三、答辩时应该展示什么</h2>
<p>建议展示不同用户因行为差异而得到的不同推荐结果，这比单纯展示一个公式更能体现系统价值。</p>
"@

$bodyAudit = @"
<p>任何知识社区都不能只关注发布效率，而忽视内容安全。内容审核并不是附加模块，而是平台长期运行的底线能力。</p>
<p>本文从自动拦截、人工审核和违规记录三个角度，分析知识社区的内容治理机制。</p>
<h2>一、敏感词拦截解决什么问题</h2>
<p>它可以在内容进入公开区之前先过滤明显违规信息，将风险尽量阻断在前置阶段。</p>
<h2>二、为什么仍然需要人工审核</h2>
<p>自动规则适合覆盖高风险场景，但对语义理解有限。人工审核能补足机器判断的边界，并为作者提供更明确的反馈。</p>
<h2>三、治理能力如何反哺平台信任</h2>
<p>当用户知道平台存在清晰的审核机制、通知机制和处罚机制时，内容生态会更稳定，优质作者也更愿意持续参与。</p>
"@

$articles = @(
    @{ headers = $chenHeaders; categoryId = 1; title = '基于 Docker 与 Redis 的知识社区部署与缓存实践'; summary = '围绕容器化部署、热点缓存与服务启动流程，梳理个人知识平台在本地与演示环境中的稳定运行方案。'; body = $bodyDocker; tags = @('Docker','Redis','DevOps') },
    @{ headers = $luoHeaders; categoryId = 1; title = '用 WebSocket 串联审核通知与问答提醒的实现思路'; summary = '从站内通知到实时提醒，分析知识社区在审核结果、评论回复和问答互动中的消息链路设计。'; body = $bodySocket; tags = @('WebSocket','SpringBoot','Redis') },
    @{ headers = $guHeaders; categoryId = 2; title = '面向知识社区首页的信息流重构与前端协同设计'; summary = '围绕首页内容流、筛选区和卡片结构，讨论 Vue 与 React 视角下的知识社区阅读体验优化。'; body = $bodyFeed; tags = @('Vue3','React','AI') },
    @{ headers = $chenHeaders; categoryId = 3; title = '基于 Elasticsearch 与 Python 的知识检索与推荐建模思路'; summary = '围绕搜索召回、标签画像和推荐排序，讨论知识社区如何为后续个性化推荐能力准备数据基础。'; body = $bodySearch; tags = @('Elasticsearch','Python','Recommendation') },
    @{ headers = $luoHeaders; categoryId = 3; title = '推荐系统实验数据应该如何组织：从标签向量到行为权重'; summary = '结合知识社区的用户行为事件，说明推荐实验阶段如何组织样本、特征和评估指标。'; body = $bodyRec; tags = @('Recommendation','Python','Redis') },
    @{ headers = $guHeaders; categoryId = 1; title = '从后端治理角度看个人知识平台的内容安全与敏感词拦截'; summary = '讨论常规敏感词拦截、人工审核与违规处理在知识社区中的协同关系，以及对平台长期治理的重要性。'; body = $bodyAudit; tags = @('Java','SpringBoot','DevOps') }
)

$created = @()
foreach ($article in $articles) {
    $tagIds = @()
    foreach ($tagName in $article.tags) {
        if (-not $tagMap.ContainsKey($tagName)) { throw "Tag not found: $tagName" }
        $tagIds += $tagMap[$tagName]
    }
    $id = Create-Article -Headers $article.headers -Title $article.title -Summary $article.summary -Body $article.body -CategoryId $article.categoryId -TagIds $tagIds
    Approve-Article -Headers $adminHeaders -ContentId $id
    $created += [pscustomobject]@{ id = $id; title = $article.title; tags = ($article.tags -join ', ') }
}

$articleByTitle = @{}
foreach ($item in $created) { $articleByTitle[$item.title] = $item.id }

$dockerId = $articleByTitle['基于 Docker 与 Redis 的知识社区部署与缓存实践']
$socketId = $articleByTitle['用 WebSocket 串联审核通知与问答提醒的实现思路']
$feedId = $articleByTitle['面向知识社区首页的信息流重构与前端协同设计']
$searchId = $articleByTitle['基于 Elasticsearch 与 Python 的知识检索与推荐建模思路']
$recId = $articleByTitle['推荐系统实验数据应该如何组织：从标签向量到行为权重']
$auditId = $articleByTitle['从后端治理角度看个人知识平台的内容安全与敏感词拦截']

$c1 = Add-Comment -Headers $tanHeaders -ContentId $dockerId -Body '这篇文章把部署、缓存和答辩演示之间的关系解释得很完整，尤其适合后期准备展示脚本时参考。'
Add-Comment -Headers $chenHeaders -ContentId $dockerId -Body '我后来也是把启动命令和演示数据脚本全部固定下来，现场展示会稳很多。' -ParentId $c1 | Out-Null
$c2 = Add-Comment -Headers $zhouHeaders -ContentId $feedId -Body '首页如果只做成管理表格，确实很难形成持续浏览的社区感。这里关于信息层级的分析很实用。'
$c3 = Add-Comment -Headers $wuHeaders -ContentId $searchId -Body '推荐系统先从检索和标签画像做起，这个思路很适合毕业设计阶段逐步推进。'
Add-Comment -Headers $luoHeaders -ContentId $socketId -Body '通知中心与实时推送分层设计这一点很关键，既能保证稳定，也方便后续扩展。' | Out-Null
Add-Comment -Headers $heHeaders -ContentId $recId -Body '把用户行为、标签权重和排序解释放在一起展示，会比只讲公式更有说服力。' | Out-Null

foreach ($id in @($dockerId,$socketId,$auditId)) {
    Toggle-Like -Headers $tanHeaders -TargetId $id -TargetType 'CONTENT'
    Toggle-Favorite -Headers $tanHeaders -ContentId $id
}
foreach ($id in @($feedId,$searchId)) {
    Toggle-Like -Headers $zhouHeaders -TargetId $id -TargetType 'CONTENT'
    Toggle-Favorite -Headers $zhouHeaders -ContentId $id
}
foreach ($id in @($searchId,$recId,$auditId)) {
    Toggle-Like -Headers $wuHeaders -TargetId $id -TargetType 'CONTENT'
    Toggle-Favorite -Headers $wuHeaders -ContentId $id
}
foreach ($id in @($dockerId,$recId)) { Toggle-Like -Headers $xuHeaders -TargetId $id -TargetType 'CONTENT' }
foreach ($id in @($feedId,$socketId)) { Toggle-Like -Headers $heHeaders -TargetId $id -TargetType 'CONTENT' }
Toggle-Like -Headers $chenHeaders -TargetId $c2 -TargetType 'COMMENT'
Toggle-Like -Headers $guHeaders -TargetId $c3 -TargetType 'COMMENT'

$result = [pscustomobject]@{ accounts = $accounts; articles = $created }
$result | ConvertTo-Json -Depth 5
