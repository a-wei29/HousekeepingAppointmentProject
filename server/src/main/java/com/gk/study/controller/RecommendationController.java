package com.gk.study.controller;

import com.gk.study.common.APIResponse;
import com.gk.study.common.ResponeCode;
import com.gk.study.entity.Thing;
import com.gk.study.enums.HousekeepingServiceCategory;
import com.gk.study.service.MixedRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    @Autowired
    private MixedRecommendationService recommendationService;

    @Operation(
            summary = "获取个性化推荐列表",
            description = "根据用户ID获取系统为用户生成的个性化家政服务推荐列表。",
            responses = {
                    @ApiResponse(responseCode = "200", description = "获取成功"),
                    @ApiResponse(responseCode = "400", description = "参数无效")
            }
    )
    @GetMapping("/list")
    public ResponseEntity<APIResponse<?>> list(
            @Parameter(description = "用户ID", required = true)
            @RequestParam Long userId
    ) {
        logger.info("Fetching personalized recommendations for user ID: {}", userId);

        if (userId == null || userId <= 0) {
            return ResponseEntity.ok(
                    new APIResponse<>(ResponeCode.FAIL, "无效的用户ID")
            );
        }

        // 调用推荐服务
        List<Thing> recommendations = recommendationService.recommendForUser(userId);

        // 为每条推荐结果补充分类名称
        recommendations.forEach(thing -> {
            if (thing.getClassificationId() != null) {
                HousekeepingServiceCategory cat =
                        HousekeepingServiceCategory.getByCode(thing.getClassificationId().intValue());
                if (cat != null) {
                    thing.setClassificationName(cat.getDescription());
                }
            }
        });

        return ResponseEntity.ok(
                new APIResponse<>(ResponeCode.SUCCESS, "获取成功", recommendations)
        );
    }
}