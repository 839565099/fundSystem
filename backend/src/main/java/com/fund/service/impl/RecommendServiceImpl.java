package com.fund.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.dto.RecommendDTO;
import com.fund.entity.*;
import com.fund.mapper.*;
import com.fund.service.FundService;
import com.fund.service.RecommendService;
import com.fund.vo.RecommendFundVO;
import com.fund.vo.RecommendOverviewVO;
import com.fund.vo.RiskBasedRecommendVO;
import com.fund.vo.SimilarFundsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能推荐服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final FundMapper fundMapper;
    private final UserBehaviorMapper behaviorMapper;
    private final UserProfileMapper profileMapper;
    private final FundSimilarityMapper similarityMapper;
    private final UserFavoriteMapper favoriteMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FundService fundService;

    private static final String CACHE_PREFIX = "recommend:";
    private static final long CACHE_EXPIRE = 30; // 30分钟

    @Override
    public RecommendOverviewVO getRecommendOverview(Long userId) {
        RecommendOverviewVO overview = new RecommendOverviewVO();

        // 个性化推荐
        overview.setPersonalized(getPersonalizedRecommend(userId, 4));

        // 热门推荐
        overview.setHot(getHotRecommend(4));

        // 相似基金（基于最近收藏）
        if (userId != null) {
            LambdaQueryWrapper<UserFavorite> favWrapper = new LambdaQueryWrapper<>();
            favWrapper.eq(UserFavorite::getUserId, userId)
                    .orderByDesc(UserFavorite::getCreateTime)
                    .last("LIMIT 1");
            UserFavorite recentFav = favoriteMapper.selectOne(favWrapper);
            if (recentFav != null) {
                SimilarFundsVO similar = new SimilarFundsVO();
                similar.setFundCode(recentFav.getFundCode());
                Fund fund = fundService.getByFundCode(recentFav.getFundCode());
                similar.setFundName(fund != null ? fund.getFundName() : null);
                similar.setSimilarFunds(getSimilarFunds(recentFav.getFundCode(), 4));
                overview.setSimilar(similar);
            }
        }

        return overview;
    }

    @Override
    public List<RecommendFundVO> getPersonalizedRecommend(Long userId, Integer limit) {
        if (userId == null) {
            return getHotRecommend(limit);
        }

        // 尝试从缓存获取
        String cacheKey = CACHE_PREFIX + "personal:" + userId;
        List<RecommendFundVO> cached = (List<RecommendFundVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<RecommendFundVO> recommendations = new ArrayList<>();

        // 获取用户画像
        UserProfile profile = getUserProfile(userId);
        if (profile != null) {
            // 基于用户偏好推荐
            recommendations = recommendByProfile(profile, limit);
        }

        // 如果画像数据不足，结合协同过滤
        if (recommendations.size() < limit) {
            List<RecommendFundVO> cfRecommend = recommendByCollaborativeFiltering(userId, limit - recommendations.size());
            recommendations.addAll(cfRecommend);
        }

        // 去重并限制数量
        recommendations = recommendations.stream()
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());

        // 如果推荐数量仍不足，补充热门基金
        if (recommendations.size() < limit) {
            List<RecommendFundVO> hotFunds = getHotRecommend(limit - recommendations.size());
            recommendations.addAll(hotFunds);
        }

        // 设置推荐来源
        recommendations.forEach(r -> r.setSource("personal"));

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, recommendations, CACHE_EXPIRE, TimeUnit.MINUTES);

        return recommendations;
    }

    @Override
    public List<RecommendFundVO> getSimilarFunds(String fundCode, Integer limit) {
        // 尝试从缓存获取
        String cacheKey = CACHE_PREFIX + "similar:" + fundCode;
        List<RecommendFundVO> cached = (List<RecommendFundVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 查询相似度表
        LambdaQueryWrapper<FundSimilarity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(FundSimilarity::getFundCodeA, fundCode).or().eq(FundSimilarity::getFundCodeB, fundCode))
                .orderByDesc(FundSimilarity::getSimilarityScore)
                .last("LIMIT " + Math.min(limit, 50));

        List<FundSimilarity> similarities = similarityMapper.selectList(wrapper);

        List<RecommendFundVO> result = new ArrayList<>();
        for (FundSimilarity sim : similarities) {
            String otherCode = sim.getFundCodeA().equals(fundCode) ? sim.getFundCodeB() : sim.getFundCodeA();
            Fund fund = fundService.getByFundCode(otherCode);
            if (fund != null) {
                RecommendFundVO vo = convertToVO(fund);
                vo.setScore(sim.getSimilarityScore().multiply(new BigDecimal("100")));
                vo.setReason("与" + fundCode + "相似度高");
                vo.setSource("similar");
                result.add(vo);
            }
        }

        // 如果相似度数据不足，基于基金类型和风险等级推荐
        if (result.size() < limit) {
            Fund baseFund = fundService.getByFundCode(fundCode);
            if (baseFund != null) {
                List<RecommendFundVO> typeBased = recommendByTypeAndRisk(
                        baseFund.getFundType(),
                        baseFund.getRiskLevel(),
                        limit - result.size(),
                        Collections.singletonList(fundCode)
                );
                result.addAll(typeBased);
            }
        }

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE, TimeUnit.MINUTES);

        return result;
    }

    @Override
    public List<RecommendFundVO> getHotRecommend(Integer limit) {
        // 尝试从缓存获取
        String cacheKey = CACHE_PREFIX + "hot";
        List<RecommendFundVO> cached = (List<RecommendFundVO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        // 热门推荐策略：
        // 1. 近一年收益率高的基金
        // 2. 规模适中（10-100亿）
        // 3. 近期访问量高
        LambdaQueryWrapper<Fund> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Fund::getYearGrowth)
                .ge(Fund::getFundScale, new BigDecimal("10"))
                .le(Fund::getFundScale, new BigDecimal("100"))
                .orderByDesc(Fund::getYearGrowth)
                .last("LIMIT " + Math.min(limit * 2, 100));

        List<Fund> funds = fundMapper.selectList(wrapper);

        // 计算综合得分并排序
        List<RecommendFundVO> result = funds.stream()
                .map(this::convertToVO)
                .map(vo -> {
                    vo.setScore(calculateHotScore(vo));
                    vo.setReason("近期表现优秀，市场关注度高");
                    vo.setSource("hot");
                    return vo;
                })
                .sorted(Comparator.comparing(RecommendFundVO::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE, TimeUnit.MINUTES);

        return result;
    }

    @Override
    public List<RecommendFundVO> getRiskBasedRecommend(RecommendDTO dto, Integer limit) {
        Integer riskLevel = dto.getRiskLevel();
        if (riskLevel == null) {
            riskLevel = 2; // 默认稳健型
        }

        // 根据风险等级设置筛选条件
        BigDecimal minGrowth;
        BigDecimal maxGrowth;
        Integer minRisk;
        Integer maxRisk;
        String riskName;

        switch (riskLevel) {
            case 1: // 保守型
                minGrowth = new BigDecimal("-10");
                maxGrowth = new BigDecimal("20");
                minRisk = 1;
                maxRisk = 2;
                riskName = "保守型";
                break;
            case 2: // 稳健型
                minGrowth = new BigDecimal("-20");
                maxGrowth = new BigDecimal("50");
                minRisk = 2;
                maxRisk = 3;
                riskName = "稳健型";
                break;
            case 3: // 激进型
                minGrowth = new BigDecimal("-50");
                maxGrowth = new BigDecimal("200");
                minRisk = 3;
                maxRisk = 5;
                riskName = "激进型";
                break;
            default:
                minGrowth = new BigDecimal("-20");
                maxGrowth = new BigDecimal("50");
                minRisk = 2;
                maxRisk = 3;
                riskName = "稳健型";
        }

        LambdaQueryWrapper<Fund> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(Fund::getRiskLevel, minRisk, maxRisk)
                .ge(Fund::getYearGrowth, minGrowth)
                .le(Fund::getYearGrowth, maxGrowth);

        if (dto.getFundType() != null) {
            wrapper.eq(Fund::getFundType, dto.getFundType());
        }
        if (dto.getMinScale() != null) {
            wrapper.ge(Fund::getFundScale, dto.getMinScale());
        }
        if (dto.getMaxScale() != null) {
            wrapper.le(Fund::getFundScale, dto.getMaxScale());
        }

        wrapper.orderByDesc(Fund::getYearGrowth)
                .last("LIMIT " + Math.min(limit, 50));

        List<Fund> funds = fundMapper.selectList(wrapper);

        return funds.stream()
                .map(f -> {
                    RecommendFundVO vo = convertToVO(f);
                    vo.setReason("适合" + riskName + "投资者");
                    vo.setSource("risk");
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void recordBehavior(Long userId, String fundCode, String behaviorType, Integer dwellTime) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setFundCode(fundCode);
        behavior.setBehaviorType(behaviorType);
        behavior.setBehaviorScore(getBehaviorScore(behaviorType));
        behavior.setDwellTime(dwellTime != null ? dwellTime : 0);
        behavior.setCreateTime(LocalDateTime.now());
        behaviorMapper.insert(behavior);
    }

    @Override
    @Transactional
    public void updateUserProfile(Long userId) {
        // 获取用户行为数据
        LambdaQueryWrapper<UserBehavior> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBehavior::getUserId, userId)
                .ge(UserBehavior::getCreateTime, LocalDateTime.now().minusDays(30));

        List<UserBehavior> behaviors = behaviorMapper.selectList(wrapper);

        if (behaviors.isEmpty()) {
            return;
        }

        // 计算偏好
        Map<String, Integer> typeCount = new HashMap<>();
        int totalRiskLevel = 0;
        int riskCount = 0;

        for (UserBehavior behavior : behaviors) {
            Fund fund = fundService.getByFundCode(behavior.getFundCode());
            if (fund != null) {
                // 统计基金类型偏好
                String type = fund.getFundType();
                typeCount.merge(type, behavior.getBehaviorScore().intValue(), Integer::sum);

                // 统计风险偏好
                if (fund.getRiskLevel() != null) {
                    totalRiskLevel += fund.getRiskLevel();
                    riskCount++;
                }
            }
        }

        // 更新或创建用户画像
        UserProfile profile = getUserProfile(userId);
        boolean isNew = profile == null;
        if (isNew) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setTotalBehaviorScore(BigDecimal.ZERO);
        }

        // 设置风险偏好
        if (riskCount > 0) {
            int avgRisk = Math.round((float) totalRiskLevel / riskCount);
            profile.setRiskPreference(Math.min(3, Math.max(1, avgRisk)));
        }

        // 设置偏好类型（取前3个）
        List<String> topTypes = typeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        profile.setPreferredTypes(String.join(",", topTypes));

        profile.setLastActiveTime(LocalDateTime.now());

        // 更新行为总分
        BigDecimal totalScore = behaviors.stream()
                .map(UserBehavior::getBehaviorScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        profile.setTotalBehaviorScore(totalScore);

        if (isNew) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void calculateFundSimilarity() {
        log.info("开始计算基金相似度...");

        // 获取所有基金
        List<Fund> allFunds = fundMapper.selectList(null);

        // 计算两两相似度
        for (int i = 0; i < allFunds.size(); i++) {
            for (int j = i + 1; j < allFunds.size(); j++) {
                Fund fundA = allFunds.get(i);
                Fund fundB = allFunds.get(j);

                BigDecimal similarity = calculateSimilarity(fundA, fundB);
                if (similarity.compareTo(new BigDecimal("0.5")) > 0) {
                    // 只保存相似度 > 0.5 的记录
                    saveSimilarity(fundA.getFundCode(), fundB.getFundCode(), similarity);
                }
            }
        }

        log.info("基金相似度计算完成");
    }

    // ========== 私有方法 ==========

    private UserProfile getUserProfile(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        return profileMapper.selectOne(wrapper);
    }

    private List<RecommendFundVO> recommendByProfile(UserProfile profile, Integer limit) {
        LambdaQueryWrapper<Fund> wrapper = new LambdaQueryWrapper<>();

        // 按风险偏好筛选
        Integer riskPref = profile.getRiskPreference();
        if (riskPref != null) {
            wrapper.between(Fund::getRiskLevel, riskPref - 1, riskPref + 1);
        }

        // 按偏好类型筛选
        String preferredTypes = profile.getPreferredTypes();
        if (preferredTypes != null && !preferredTypes.isEmpty()) {
            List<String> types = Arrays.asList(preferredTypes.split(","));
            wrapper.in(Fund::getFundType, types);
        }

        wrapper.orderByDesc(Fund::getYearGrowth)
                .last("LIMIT " + Math.min(limit, 50));

        List<Fund> funds = fundMapper.selectList(wrapper);
        return funds.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private List<RecommendFundVO> recommendByCollaborativeFiltering(Long userId, Integer limit) {
        // 获取用户收藏的基金
        LambdaQueryWrapper<UserFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(UserFavorite::getUserId, userId);
        List<UserFavorite> favorites = favoriteMapper.selectList(favWrapper);

        if (favorites.isEmpty()) {
            return new ArrayList<>();
        }

        // 找到收藏了相同基金的其他用户
        Set<Long> similarUsers = new HashSet<>();
        for (UserFavorite fav : favorites) {
            LambdaQueryWrapper<UserFavorite> otherFavWrapper = new LambdaQueryWrapper<>();
            otherFavWrapper.eq(UserFavorite::getFundCode, fav.getFundCode())
                    .ne(UserFavorite::getUserId, userId);
            List<UserFavorite> otherFavs = favoriteMapper.selectList(otherFavWrapper);
            for (UserFavorite otherFav : otherFavs) {
                similarUsers.add(otherFav.getUserId());
            }
        }

        // 获取相似用户收藏但当前用户未收藏的基金
        Set<String> myFundCodes = favorites.stream()
                .map(UserFavorite::getFundCode)
                .collect(Collectors.toSet());

        Map<String, Integer> fundScores = new HashMap<>();
        for (Long similarUserId : similarUsers) {
            LambdaQueryWrapper<UserFavorite> userFavWrapper = new LambdaQueryWrapper<>();
            userFavWrapper.eq(UserFavorite::getUserId, similarUserId);
            List<UserFavorite> userFavs = favoriteMapper.selectList(userFavWrapper);
            for (UserFavorite userFav : userFavs) {
                if (!myFundCodes.contains(userFav.getFundCode())) {
                    fundScores.merge(userFav.getFundCode(), 1, Integer::sum);
                }
            }
        }

        // 按分数排序返回
        return fundScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> {
                    Fund fund = fundService.getByFundCode(entry.getKey());
                    if (fund != null) {
                        RecommendFundVO vo = convertToVO(fund);
                        vo.setReason("相似用户也关注了这只基金");
                        return vo;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<RecommendFundVO> recommendByTypeAndRisk(String fundType, Integer riskLevel, Integer limit, List<String> excludeCodes) {
        LambdaQueryWrapper<Fund> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Fund::getFundType, fundType)
                .eq(Fund::getRiskLevel, riskLevel)
                .notIn(Fund::getFundCode, excludeCodes)
                .orderByDesc(Fund::getYearGrowth)
                .last("LIMIT " + Math.min(limit, 50));

        List<Fund> funds = fundMapper.selectList(wrapper);
        return funds.stream()
                .map(f -> {
                    RecommendFundVO vo = convertToVO(f);
                    vo.setReason("同类型、同风险等级基金");
                    vo.setSource("similar");
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateSimilarity(Fund fundA, Fund fundB) {
        double similarity = 0;

        // 类型相同 +0.3
        if (Objects.equals(fundA.getFundType(), fundB.getFundType())) {
            similarity += 0.3;
        }

        // 风险等级相同 +0.2
        if (Objects.equals(fundA.getRiskLevel(), fundB.getRiskLevel())) {
            similarity += 0.2;
        }

        // 规模接近 +0.2
        if (fundA.getFundScale() != null && fundB.getFundScale() != null) {
            double scaleDiff = Math.abs(fundA.getFundScale().subtract(fundB.getFundScale()).doubleValue());
            if (scaleDiff < 10) {
                similarity += 0.2;
            } else if (scaleDiff < 50) {
                similarity += 0.1;
            }
        }

        // 收益率接近 +0.3
        if (fundA.getYearGrowth() != null && fundB.getYearGrowth() != null) {
            double growthDiff = Math.abs(fundA.getYearGrowth().subtract(fundB.getYearGrowth()).doubleValue());
            if (growthDiff < 5) {
                similarity += 0.3;
            } else if (growthDiff < 15) {
                similarity += 0.15;
            }
        }

        return new BigDecimal(similarity).setScale(4, RoundingMode.HALF_UP);
    }

    private void saveSimilarity(String fundCodeA, String fundCodeB, BigDecimal similarity) {
        // 检查是否已存在
        LambdaQueryWrapper<FundSimilarity> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(FundSimilarity::getFundCodeA, fundCodeA).eq(FundSimilarity::getFundCodeB, fundCodeB))
                .or(w -> w.eq(FundSimilarity::getFundCodeA, fundCodeB).eq(FundSimilarity::getFundCodeB, fundCodeA));

        FundSimilarity existing = similarityMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setSimilarityScore(similarity);
            existing.setCalcTime(LocalDateTime.now());
            similarityMapper.updateById(existing);
        } else {
            FundSimilarity newSim = new FundSimilarity();
            newSim.setFundCodeA(fundCodeA);
            newSim.setFundCodeB(fundCodeB);
            newSim.setSimilarityScore(similarity);
            newSim.setCalcTime(LocalDateTime.now());
            similarityMapper.insert(newSim);
        }
    }

    private BigDecimal calculateHotScore(RecommendFundVO vo) {
        double score = 50; // 基础分

        // 收益加分
        if (vo.getYearGrowth() != null) {
            if (vo.getYearGrowth().compareTo(new BigDecimal("50")) > 0) {
                score += 30;
            } else if (vo.getYearGrowth().compareTo(new BigDecimal("20")) > 0) {
                score += 20;
            } else if (vo.getYearGrowth().compareTo(BigDecimal.ZERO) > 0) {
                score += 10;
            }
        }

        // 规模加分（适中规模最好）
        if (vo.getFundScale() != null) {
            double scale = vo.getFundScale().doubleValue();
            if (scale >= 20 && scale <= 50) {
                score += 20;
            } else if (scale >= 10 && scale <= 100) {
                score += 10;
            }
        }

        return new BigDecimal(score);
    }

    private BigDecimal getBehaviorScore(String behaviorType) {
        switch (behaviorType) {
            case "view": return new BigDecimal("1");
            case "click": return new BigDecimal("1.5");
            case "compare": return new BigDecimal("2");
            case "favorite": return new BigDecimal("3");
            case "share": return new BigDecimal("4");
            default: return BigDecimal.ONE;
        }
    }

    private RecommendFundVO convertToVO(Fund fund) {
        RecommendFundVO vo = new RecommendFundVO();
        vo.setFundCode(fund.getFundCode());
        vo.setFundName(fund.getFundName());
        vo.setFundType(fund.getFundType());
        vo.setNav(fund.getNav());
        vo.setDayGrowth(fund.getDayGrowth());
        vo.setMonthGrowth(fund.getMonthGrowth());
        vo.setYearGrowth(fund.getYearGrowth());
        vo.setTotalGrowth(fund.getTotalGrowth());
        vo.setFundScale(fund.getFundScale());
        vo.setRiskLevel(fund.getRiskLevel());
        return vo;
    }
}
