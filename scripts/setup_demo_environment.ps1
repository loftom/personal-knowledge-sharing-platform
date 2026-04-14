param(
    [string]$BaseUrl = 'http://localhost:8080/api',
    [string]$MySqlContainer = 'knowledge-platform-mysql',
    [string]$MySqlDatabase = 'knowledge_platform',
    [string]$MySqlUser = 'root',
    [string]$MySqlPassword = 'root'
)

& (Join-Path $PSScriptRoot 'setup_demo_environment_mysql.ps1') `
    -BaseUrl $BaseUrl `
    -MySqlContainer $MySqlContainer `
    -MySqlDatabase $MySqlDatabase `
    -MySqlUser $MySqlUser `
    -MySqlPassword $MySqlPassword
return

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
    @{ username = 'admin_master'; password = 'Admin@123456'; nickname = '绯荤粺绠＄悊鍛? },
    @{ username = 'author_lin'; password = 'User@123456'; nickname = '鏋楃煡杩? },
    @{ username = 'author_qin'; password = 'User@123456'; nickname = '绉﹁嫢婧? },
    @{ username = 'author_song'; password = 'User@123456'; nickname = '瀹嬫槑鍝? },
    @{ username = 'reader_xu'; password = 'User@123456'; nickname = '寰愰椈绗? },
    @{ username = 'reader_he'; password = 'User@123456'; nickname = '浣曟竻鍢? }
)

foreach ($user in $users) {
    Register-User -Username $user.username -Password $user.password -Nickname $user.nickname
}

Run-H2Sql -Sql @"
UPDATE user SET role = 'ADMIN', nickname = '绯荤粺绠＄悊鍛? WHERE username = 'admin_master';
UPDATE user SET nickname = '鏋楃煡杩? WHERE username = 'author_lin';
UPDATE user SET nickname = '绉﹁嫢婧? WHERE username = 'author_qin';
UPDATE user SET nickname = '瀹嬫槑鍝? WHERE username = 'author_song';
UPDATE user SET nickname = '寰愰椈绗? WHERE username = 'reader_xu';
UPDATE user SET nickname = '浣曟竻鍢? WHERE username = 'reader_he';
"@

$adminHeaders = Login-User -Username 'admin_master' -Password 'Admin@123456'
$linHeaders = Login-User -Username 'author_lin' -Password 'User@123456'
$qinHeaders = Login-User -Username 'author_qin' -Password 'User@123456'
$songHeaders = Login-User -Username 'author_song' -Password 'User@123456'
$xuHeaders = Login-User -Username 'reader_xu' -Password 'User@123456'
$heHeaders = Login-User -Username 'reader_he' -Password 'User@123456'

$article1 = Create-Article -Headers $linHeaders `
    -Title '鍦ㄧ煡璇嗙ぞ鍖轰腑鏋勫缓瀹℃牳銆侀€氱煡涓庣Н鍒嗚仈鍔ㄩ棴鐜殑瀹炶返' `
    -Summary '鏈枃鍥寸粫鍐呭瀹℃牳銆佷綔鑰呴€氱煡涓庣Н鍒嗘縺鍔变笁鏉￠摼璺紝璇存槑鐭ヨ瘑绀惧尯濡備綍褰㈡垚绋冲畾鍙寔缁殑浜у搧闂幆銆? `
    -Body @"
<p>涓€涓煡璇嗙ぞ鍖鸿鎯冲缓绔嬪彲淇″害锛屽墠鎻愭槸鍙戝竷銆佸鏍镐笌鍙嶉鑳藉褰㈡垚缁熶竴涓旇繛璐殑澶勭悊娴佺▼銆?/p>
<p>鍦ㄨ繖濂楀疄鐜颁腑锛屾枃绔犲厛杩涘叆寰呭鏍搁槦鍒楋紝鍐嶇敱绠＄悊鍛樼粰鍑哄鏍哥粨鏋滐紝鏈€鍚庤Е鍙戜綔鑰呴€氱煡涓庣Н鍒嗗彉鏇达紝褰㈡垚瀹屾暣涓氬姟闂幆銆?/p>
<h2>涓轰粈涔堣繖涓棴鐜噸瑕?/h2>
<p>娌℃湁瀹℃牳锛屽唴瀹硅川閲忓氨瀹规槗澶辨帶锛涙病鏈夐€氱煡锛屼綔鑰呮棤娉曠悊瑙ｅ钩鍙板弽棣堬紱娌℃湁绉垎婵€鍔憋紝绀惧尯涔熷緢闅惧舰鎴愰暱鏈熺ǔ瀹氱殑鍒涗綔鍔ㄥ姏銆?/p>
<h2>瀹炵幇涓殑鍏抽敭鍐崇瓥</h2>
<p>鎴戠粺涓€浜嗗唴瀹圭姸鎬佹祦杞紝璁板綍姣忎竴娆″鏍稿姩浣滅殑鏃ュ織锛屽悓鏃朵负绉垎濂栧姳澧炲姞骞傜瓑涓氬姟閿紝閬垮厤閲嶅瀹℃牳瀵艰嚧閲嶅鍙戝垎銆?/p>
<p>杩欐牱涓€鏉ワ紝浣滆€呮嫢鏈夋竻鏅扮殑鍙戝竷璺緞锛岀鐞嗗憳鎷ユ湁鍙拷婧殑鎿嶄綔璁板綍锛屽钩鍙颁篃鎷ユ湁榧撳姳浼樿川鍒涗綔鐨勫彲闈犳満鍒躲€?/p>
"@ `
    -CategoryId 1 -TagIds @(1, 5)

$article2 = Create-Article -Headers $qinHeaders `
    -Title '鐭ヨ瘑绀惧尯棣栭〉淇℃伅娴侀噸鏋勶細闈㈠悜娣卞害闃呰涓庢寔缁簰鍔ㄧ殑璁捐鎬濊矾' `
    -Summary '鏈枃鍥寸粫棣栭〉淇℃伅灞傜骇銆佺瓫閫夊竷灞€涓庡唴瀹瑰崱鐗囩粨鏋勶紝璁ㄨ濡備綍鎵撻€犳洿閫傚悎鐭ヨ瘑绀惧尯鐨勯槄璇讳綋楠屻€? `
    -Body @"
<p>鐭ヨ瘑骞冲彴鐨勯椤典笉搴旇鍙槸鍐呭璁板綍鐨勫爢鍙狅紝瀹冩湰璐ㄤ笂鏄敤鎴峰彂鐜板唴瀹圭殑绗竴鍏ュ彛锛岀洿鎺ュ奖鍝嶆祻瑙堛€佸垽鏂笌鍥炶鎰忔効銆?/p>
<p>杩欐鏀圭増涓紝鎴戦噸鐐瑰叧娉ㄤ簡棣栧睆瑙嗚鍖恒€佺瓫閫夊叆鍙ｇ殑鎽嗘斁鏂瑰紡锛屼互鍙婃瘡寮犲唴瀹瑰崱鐗囦腑鏍囬銆佹憳瑕佷笌鍏冧俊鎭箣闂寸殑鍏崇郴銆?/p>
<h2>棣栭〉棣栧厛瑕佽В鍐充粈涔堥棶棰?/h2>
<p>鐢ㄦ埛闇€瑕佸湪鏋佺煭鏃堕棿鍐呯悊瑙ｅ钩鍙颁富瑕佹湁鍝簺鍐呭锛屽苟鑳藉閫氳繃鍒嗙被銆佹爣绛惧拰鎺掑簭瑙勫垯蹇€熺缉灏忔祻瑙堣寖鍥淬€?/p>
<h2>涓轰粈涔堥槄璇昏妭濂忓緢閲嶈</h2>
<p>濡傛灉浣滆€呫€佸彂甯冩椂闂村拰浜掑姩鏁版嵁涓庢爣棰樻姠鍗犲悓绛夎瑙夋潈閲嶏紝鐢ㄦ埛瑙嗙嚎灏变細琚墦鏁ｃ€傚己鍖栨爣棰樺尯銆佸急鍖栨瑕佸厓淇℃伅锛岃兘鏄捐憲鎻愬崌鎵鏁堢巼銆?/p>
<p>鍥犳锛屼竴涓ソ鐨勯椤典笉鍙槸灞曠ず灞傦紝鏇存槸鎸佺画寮曞鐢ㄦ埛闃呰銆佽瘎璁轰笌鍙戝竷鐨勪簰鍔ㄥ叆鍙ｃ€?/p>
"@ `
    -CategoryId 2 -TagIds @(2)

$article3 = Create-Article -Headers $songHeaders `
    -Title '鐭ヨ瘑绀惧尯姣曚笟璁捐婕旂ず濡備綍缁勭粐锛氫粠鏍稿績娴佺▼鍒颁寒鐐规ā鍧楃殑灞曠ず鏂规硶' `
    -Summary '鏈枃璇存槑濡備綍灏嗕竴鏈熴€佷簩鏈熷拰涓夋湡鑳藉姏鏁寸悊鎴愭竻鏅版湁璇存湇鍔涚殑绛旇京婕旂ず娴佺▼銆? `
    -Body @"
<p>瀵逛簬姣曚笟璁捐鏉ヨ锛屽彧鏈夊疄鐜板姛鑳借繕涓嶅锛岀郴缁熻繕闇€瑕佷竴濂楄兘澶熻鑰佸笀杩呴€熺悊瑙ｅ畬鏁翠骇鍝侀棴鐜殑灞曠ず缁撴瀯銆?/p>
<p>鎴戝皢骞冲彴鍒掑垎涓轰笁涓樁娈碉細绗竴闃舵鑱氱劍鍙戝竷銆佸鏍搞€佹悳绱笌浜掑姩锛涚浜岄樁娈垫墿灞曞埌鎺ㄨ崘銆佷釜浜虹┖闂翠笌閫氱煡锛涚涓夐樁娈电獊鍑哄垎鏋愩€佸闀夸笌娌荤悊鑳藉姏銆?/p>
<h2>绛旇京涓簲璇ラ噸鐐瑰己璋冧粈涔?/h2>
<p>鏈€鏈夎鏈嶅姏鐨勭偣鏄畬鏁翠笟鍔￠棴鐜細鐢ㄦ埛鍙戝竷鍐呭锛岀鐞嗗憳瀹℃牳锛屽鏍搁€氳繃鍚庡唴瀹瑰澶栧彲瑙侊紝浜掑姩鏁版嵁琚寔缁褰曪紝骞惰繘涓€姝ユ矇娣€涓哄闀夸笌缁熻鍒嗘瀽鑳藉姏銆?/p>
<h2>鎺ㄨ崘鐨勬紨绀洪『搴?/h2>
<p>寤鸿鍏堝睍绀烘櫘閫氱敤鎴峰彂鏂囷紝鍐嶅垏鎹㈢鐞嗗憳瀹屾垚瀹℃牳锛屾渶鍚庡洖鍒板墠绔〉闈㈠睍绀哄鏍哥粨鏋溿€佺ぞ鍖鸿瘎璁轰互鍙婁綔鑰呮垚闀挎暟鎹甫鏉ョ殑鍙樺寲銆?/p>
"@ `
    -CategoryId 1 -TagIds @(1, 3, 5)

foreach ($articleId in @($article1, $article2, $article3)) {
    Approve-Article -Headers $adminHeaders -ContentId $articleId
}

foreach ($articleId in @($article1, $article2, $article3)) {
    Invoke-JsonGet -Url "$baseUrl/content/$articleId" -Headers $xuHeaders | Out-Null
    Invoke-JsonGet -Url "$baseUrl/content/$articleId" -Headers $heHeaders | Out-Null
}

$commentA1 = Add-Comment -Headers $xuHeaders -ContentId $article1 -Body '杩欑瘒鏂囩珷鎶婂鏍搞€侀€氱煡鍜屾縺鍔遍棴鐜В閲婂緱寰堟竻妤氾紝灏ゅ叾鏄鏍告棩蹇楄繖涓€鐐瑰緢閫傚悎鍦ㄧ瓟杈╂椂灞曠ず銆?
$commentA2 = Add-Comment -Headers $heHeaders -ContentId $article1 -Body '鎴戝緢璁ゅ悓杩欓噷鐨勫箓绛夎璁★紝娌℃湁鍞竴涓氬姟閿殑璇濓紝閲嶅瀹℃牳纭疄寰堝鏄撻€犳垚閲嶅绉垎銆?
Add-Comment -Headers $linHeaders -ContentId $article1 -Body '杩欐鏄垜鍦ㄧН鍒嗘棩蹇楅噷寮曞叆涓氬姟閿殑鍘熷洜锛岃兘澶у箙闄嶄綆閲嶅鍙戝鍔辫繖绫婚棶棰樸€? -ParentId $commentA2 | Out-Null

$commentB1 = Add-Comment -Headers $linHeaders -ContentId $article2 -Body '濡傛灉棣栭〉缁撴瀯鍋氬緱涓嶆竻鏅帮紝鍚庨潰鐨勬帹鑽愬拰鎼滅储妯″潡涔熷緢闅句綋鐜颁环鍊硷紝杩欑瘒鏂囩珷鎶婅繖绉嶅叧绯昏寰楀緢鏄庣櫧銆?
Add-Comment -Headers $xuHeaders -ContentId $article2 -Body '鎶婃爣棰樺拰鎽樿鏀惧湪鏇撮珮浼樺厛绾с€佸急鍖栧厓淇℃伅涔嬪悗锛屼俊鎭祦纭疄鏇村鏄撳揩閫熸祻瑙堛€? -ParentId $commentB1 | Out-Null

$commentC1 = Add-Comment -Headers $heHeaders -ContentId $article3 -Body '鎶婂钩鍙版媶鎴愪笁涓樁娈垫潵璁诧紝闈炲父閫傚悎鍦ㄦ瘯涓氱瓟杈╅噷璇存槑鑼冨洿杈圭晫鍜屾ā鍧椾寒鐐广€?
Add-Comment -Headers $songHeaders -ContentId $article3 -Body '鏄殑锛岃€屼笖杩欐牱涔熻兘璁╂紨绀洪『搴忎粠鍙戝竷銆佸鏍歌嚜鐒惰繃娓″埌浜掑姩涓庡闀垮垎鏋愩€? -ParentId $commentC1 | Out-Null

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
        @{ role = 'ADMIN'; username = 'admin_master'; password = 'Admin@123456'; nickname = '绯荤粺绠＄悊鍛? },
        @{ role = 'USER'; username = 'author_lin'; password = 'User@123456'; nickname = '鏋楃煡杩? },
        @{ role = 'USER'; username = 'author_qin'; password = 'User@123456'; nickname = '绉﹁嫢婧? },
        @{ role = 'USER'; username = 'author_song'; password = 'User@123456'; nickname = '瀹嬫槑鍝? },
        @{ role = 'USER'; username = 'reader_xu'; password = 'User@123456'; nickname = '寰愰椈绗? },
        @{ role = 'USER'; username = 'reader_he'; password = 'User@123456'; nickname = '浣曟竻鍢? }
    )
    articles = @(
        @{ id = $article1; title = '鍦ㄧ煡璇嗙ぞ鍖轰腑鏋勫缓瀹℃牳銆侀€氱煡涓庣Н鍒嗚仈鍔ㄩ棴鐜殑瀹炶返' },
        @{ id = $article2; title = '鐭ヨ瘑绀惧尯棣栭〉淇℃伅娴侀噸鏋勶細闈㈠悜娣卞害闃呰涓庢寔缁簰鍔ㄧ殑璁捐鎬濊矾' },
        @{ id = $article3; title = '鐭ヨ瘑绀惧尯姣曚笟璁捐婕旂ず濡備綍缁勭粐锛氫粠鏍稿績娴佺▼鍒颁寒鐐规ā鍧楃殑灞曠ず鏂规硶' }
    )
}

$summary | ConvertTo-Json -Depth 6
