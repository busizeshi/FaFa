package com.fafa.infrastructure.config;

import com.fafa.domain.service.FoodScorePolicy;
import com.fafa.domain.service.FoodSafetyScoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 将外部配置转换为领域层可消费的不可变评分策略。 */
@Configuration
public class FoodScorePolicyConfiguration {

    @Bean
    public FoodScorePolicy foodScorePolicy(FaFaProperties properties) {
        FaFaProperties.Food.Score score = properties.getFood().getScore();
        return new FoodScorePolicy(score.getPolicyVersion(), score.getProteinSourcePartialScore(),
                score.getAdditiveFlagPenalty(), score.getCalciumPhosphorusMinRatio(),
                score.getCalciumPhosphorusMaxRatio());
    }

    @Bean
    public FoodSafetyScoreService foodSafetyScoreService(FoodScorePolicy policy) {
        return new FoodSafetyScoreService(policy);
    }
}
