# test-hybrid.ps1 - 混合检索测试脚本
# 使用方法: .\test-hybrid.ps1

$baseUrl = "http://localhost:8080"
$headers = @{
    "Content-Type" = "application/json"
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    混合检索系统测试脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 测试用例
$testQueries = @(
    @{name="发票问题"; query="发票怎么开"},
    @{name="月嫂价格"; query="月嫂多少钱"},
    @{name="育儿嫂要求"; query="育儿嫂需要什么条件"},
    @{name="钟点工收费"; query="钟点工怎么收费"},
    @{name="阿姨证书"; query="阿姨需要什么证书"},
    @{name="退款政策"; query="怎么退款"},
    @{name="保险问题"; query="保险怎么买"},
    @{name="自我介绍"; query="我叫张三"},
    @{name="找保姆"; query="我想找保姆"},
    @{name="服务列表"; query="你们有什么服务"}
)

# 1. 检查系统状态
Write-Host "1. 检查系统状态" -ForegroundColor Yellow
Write-Host "------------------------"

try {
    $bm25Status = Invoke-RestMethod -Uri "$baseUrl/debug/bm25/status" -Method Get
    Write-Host "✅ BM25状态: 已初始化=$(if($bm25Status.initialized){'是'}else{'否'}), 索引大小=$($bm25Status.indexSize)" -ForegroundColor Green
} catch {
    Write-Host "❌ 无法获取BM25状态: $_" -ForegroundColor Red
}
Write-Host ""

# 2. 基础检索测试
Write-Host "2. 基础检索测试" -ForegroundColor Yellow
Write-Host "------------------------"

foreach ($test in $testQueries) {
    Write-Host "测试: $($test.name) [$($test.query)]" -ForegroundColor Gray
    
    try {
        $result = Invoke-RestMethod -Uri "$baseUrl/test/hybrid/search?q=$([System.Web.HttpUtility]::UrlEncode($test.query))" -Method Get
        
        if ($result.success) {
            Write-Host "  ✅ 成功 - 耗时: $($result.time_ms)ms, 结果数: $($result.result_count)" -ForegroundColor Green
            
            # 显示前3个结果预览
            if ($result.results -and $result.results.Count -gt 0) {
                for ($i = 0; $i -lt [Math]::Min(3, $result.results.Count); $i++) {
                    $item = $result.results[$i]
                    $preview = if ($item.content.Length -gt 50) { $item.content.Substring(0,50) + "..." } else { $item.content }
                    Write-Host "    [$($i+1)] $preview" -ForegroundColor White
                }
            }
        } else {
            Write-Host "  ❌ 失败: $($result.error)" -ForegroundColor Red
        }
    } catch {
        Write-Host "  ❌ 请求失败: $_" -ForegroundColor Red
    }
    
    Start-Sleep -Milliseconds 500
}
Write-Host ""

# 3. 对比三种检索方式
Write-Host "3. 对比三种检索方式" -ForegroundColor Yellow
Write-Host "------------------------"

$compareQuery = "月嫂多少钱"
Write-Host "对比查询: $compareQuery" -ForegroundColor Gray

try {
    $compareResult = Invoke-RestMethod -Uri "$baseUrl/test/hybrid/compare?q=$([System.Web.HttpUtility]::UrlEncode($compareQuery))" -Method Get
    
    Write-Host ""
    Write-Host "  向量检索: 耗时 $($compareResult.vector_time)ms, 结果数 $($compareResult.vector_results.Count)" -ForegroundColor Cyan
    Write-Host "  BM25检索: 耗时 $($compareResult.bm25_time)ms, 结果数 $($compareResult.bm25_results.Count)" -ForegroundColor Cyan
    Write-Host "  混合检索: 耗时 $($compareResult.hybrid_time)ms, 结果数 $($compareResult.hybrid_results.Count)" -ForegroundColor Cyan
    
} catch {
    Write-Host "❌ 对比测试失败: $_" -ForegroundColor Red
}
Write-Host ""

# 4. 对比知识库和业务库
Write-Host "4. 对比知识库和业务库" -ForegroundColor Yellow
Write-Host "------------------------"

$storeQuery = "育儿嫂"
Write-Host "对比查询: $storeQuery" -ForegroundColor Gray

try {
    $storeResult = Invoke-RestMethod -Uri "$baseUrl/test/hybrid/compare-stores?q=$([System.Web.HttpUtility]::UrlEncode($storeQuery))" -Method Get
    
    Write-Host ""
    Write-Host "  知识库检索: 耗时 $($storeResult.knowledge_time)ms" -ForegroundColor Magenta
    foreach ($item in $storeResult.knowledge_results) {
        Write-Host "    [$($item.rank)] $($item.preview) [$($item.source)]" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "  业务库检索: 耗时 $($storeResult.business_time)ms" -ForegroundColor Magenta
    foreach ($item in $storeResult.business_results) {
        Write-Host "    [$($item.rank)] $($item.preview) [$($item.source)]" -ForegroundColor White
    }
    
    Write-Host ""
    Write-Host "  合并检索: 耗时 $($storeResult.merged_time)ms" -ForegroundColor Magenta
    foreach ($item in $storeResult.merged_results) {
        Write-Host "    [$($item.rank)] $($item.preview) [$($item.source)]" -ForegroundColor White
    }
    
} catch {
    Write-Host "❌ 对比测试失败: $_" -ForegroundColor Red
}
Write-Host ""

# 5. 批量测试
Write-Host "5. 批量测试结果统计" -ForegroundColor Yellow
Write-Host "------------------------"

try {
    $batchResult = Invoke-RestMethod -Uri "$baseUrl/test/hybrid/batch" -Method Get
    
    Write-Host "总测试用例数: $($batchResult.total_queries)" -ForegroundColor Cyan
    Write-Host ""
    
    $stats = @{}
    foreach ($query in $batchResult.results.PSObject.Properties) {
        $name = $query.Name
        $data = $query.Value
        
        Write-Host "  $name" -ForegroundColor Gray
        Write-Host "    知识库: $($data.knowledge_count) | 业务库: $($data.business_count) | 向量: $($data.vector_count) | BM25: $($data.bm25_count) | 混合: $($data.hybrid_count)" -ForegroundColor White
        
        # 统计
        if (-not $stats['knowledge']) { $stats['knowledge'] = 0 }
        if (-not $stats['business']) { $stats['business'] = 0 }
        if (-not $stats['vector']) { $stats['vector'] = 0 }
        if (-not $stats['bm25']) { $stats['bm25'] = 0 }
        if (-not $stats['hybrid']) { $stats['hybrid'] = 0 }
        
        $stats['knowledge'] += $data.knowledge_count
        $stats['business'] += $data.business_count
        $stats['vector'] += $data.vector_count
        $stats['bm25'] += $data.bm25_count
        $stats['hybrid'] += $data.hybrid_count
    }
    
    Write-Host ""
    Write-Host "  统计汇总:" -ForegroundColor Cyan
    Write-Host "    知识库总结果: $($stats['knowledge'])" -ForegroundColor White
    Write-Host "    业务库总结果: $($stats['business'])" -ForegroundColor White
    Write-Host "    向量检索总结果: $($stats['vector'])" -ForegroundColor White
    Write-Host "    BM25总结果: $($stats['bm25'])" -ForegroundColor White
    Write-Host "    混合检索总结果: $($stats['hybrid'])" -ForegroundColor White
    
} catch {
    Write-Host "❌ 批量测试失败: $_" -ForegroundColor Red
}
Write-Host ""

# 6. BM25诊断
Write-Host "6. BM25诊断" -ForegroundColor Yellow
Write-Host "------------------------"

$diagnoseQuery = "考核标准"
Write-Host "诊断查询: $diagnoseQuery" -ForegroundColor Gray

try {
    $diagnoseResult = Invoke-RestMethod -Uri "$baseUrl/test/hybrid/diagnose-bm25?q=$([System.Web.HttpUtility]::UrlEncode($diagnoseQuery))" -Method Get
    
    Write-Host "  BM25初始化: $(if($diagnoseResult.bm25_initialized){'✅'}else{'❌'})" -ForegroundColor Cyan
    Write-Host "  BM25索引大小: $($diagnoseResult.bm25_index_size)" -ForegroundColor Cyan
    
    if ($diagnoseResult.bm25_success) {
        Write-Host "  BM25检索成功: $($diagnoseResult.bm25_count) 条结果, 耗时 $($diagnoseResult.bm25_time_ms)ms" -ForegroundColor Green
        if ($diagnoseResult.bm25_previews) {
            foreach ($preview in $diagnoseResult.bm25_previews) {
                Write-Host "    - $preview..." -ForegroundColor White
            }
        }
    } else {
        Write-Host "  BM25检索失败: $($diagnoseResult.bm25_error)" -ForegroundColor Red
    }
    
    Write-Host "  知识库向量结果: $($diagnoseResult.knowledge_count)" -ForegroundColor White
    Write-Host "  业务库向量结果: $($diagnoseResult.business_count)" -ForegroundColor White
    
} catch {
    Write-Host "❌ BM25诊断失败: $_" -ForegroundColor Red
}
Write-Host ""

# 7. 测试完成
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "    测试完成！" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan