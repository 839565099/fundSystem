package com.fund.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.entity.FundNews;
import com.fund.mapper.FundNewsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsDataInitializer implements CommandLineRunner {
    
    private final FundNewsMapper fundNewsMapper;
    
    @Override
    public void run(String... args) throws Exception {
        LambdaQueryWrapper<FundNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(FundNews::getContent).ne(FundNews::getContent, "");
        Long count = fundNewsMapper.selectCount(wrapper);
        if (count >= 10) {
            log.info("已有完整新闻数据 {} 条，跳过初始化", count);
            return;
        }
        
        log.info("当前完整新闻数据仅 {} 条，开始补充初始化...", count);
        List<FundNews> newsList = generateNewsData();
        
        for (FundNews news : newsList) {
            try {
                LambdaQueryWrapper<FundNews> existWrapper = new LambdaQueryWrapper<>();
                existWrapper.eq(FundNews::getTitle, news.getTitle());
                Long existCount = fundNewsMapper.selectCount(existWrapper);
                if (existCount == 0) {
                    fundNewsMapper.insert(news);
                    log.info("插入新闻: {}", news.getTitle());
                }
            } catch (Exception e) {
                log.warn("插入新闻失败: {}", news.getTitle(), e);
            }
        }
        
        log.info("新闻数据初始化完成");
    }
    
    private List<FundNews> generateNewsData() {
        List<FundNews> newsList = new ArrayList<>();
        
        newsList.add(createNews(
            "央行降准0.5个百分点，释放长期资金约1万亿元",
            "中国人民银行决定于2026年3月10日下调金融机构存款准备金率0.5个百分点（不含已执行5%存款准备金率的金融机构）。本次下调后，金融机构加权平均存款准备金率约为7.0%。\n\n此次降准为全面降准，体现了货币政策的稳健取向，旨在保持银行体系流动性合理充裕，降低金融机构资金成本，引导金融机构加大对实体经济的支持力度。\n\n业内专家表示，此次降准将释放长期资金约1万亿元，有效增加金融机构支持实体经济的稳定资金来源，降低银行资金成本每年约130亿元，通过金融机构传导可促进降低社会综合融资成本。\n\n受此消息影响，A股市场三大指数集体高开，上证指数开盘上涨1.2%，深证成指上涨1.5%，创业板指上涨1.8%。金融、地产、基建等板块表现亮眼。",
            "央行官网",
            "财经要闻",
            null,
            "BULLISH",
            new BigDecimal("0.85"),
            new BigDecimal("0.92"),
            5,
            LocalDateTime.now().minusDays(1)
        ));
        
        newsList.add(createNews(
            "融通人工智能指数基金净值创历史新高",
            "受益于人工智能板块持续走强，融通人工智能指数(LOF)A（161631）今日净值上涨3.2%，单位净值达到2.52元，创下成立以来历史新高。\n\n该基金主要投资于人工智能产业链相关上市公司，包括算力芯片、AI应用、云计算、大数据等细分领域。今年以来，随着ChatGPT、文心一言等大语言模型的快速发展，人工智能概念股表现强劲。\n\n基金经理何天翔表示，人工智能是新一轮科技革命的核心驱动力，长期投资价值显著。基金将坚持价值投资理念，精选具有核心竞争力的优质企业，为投资者创造长期稳健回报。\n\n数据显示，该基金今年以来收益率已超过40%，在同类指数基金中排名前列。截至最新披露，基金规模达到93.26亿元，持有人数持续增长。",
            "融通基金",
            "基金动态",
            "161631",
            "BULLISH",
            new BigDecimal("0.78"),
            new BigDecimal("0.88"),
            4,
            LocalDateTime.now().minusHours(3)
        ));
        
        newsList.add(createNews(
            "新能源板块调整，多只相关基金跌幅超2%",
            "受原材料价格上涨和补贴政策调整影响，今日新能源板块出现明显调整。光伏、风电、储能等细分领域领跌市场。\n\n数据显示，新能源ETF下跌2.8%，多只新能源主题基金跌幅超过2%。其中，某知名新能源基金净值下跌2.5%，创下近一个月最大单日跌幅。\n\n分析人士指出，新能源板块调整主要受三方面因素影响：一是上游硅料、锂矿等原材料价格持续上涨，压缩了下游企业利润空间；二是部分地方政府补贴政策收紧，影响行业短期需求；三是前期涨幅较大，部分资金获利了结。\n\n不过，长期来看，新能源作为国家战略性新兴产业，发展前景依然广阔。投资者应关注行业龙头企业的技术突破和成本控制能力，选择具有长期竞争力的优质基金。",
            "证券时报",
            "市场分析",
            null,
            "BEARISH",
            new BigDecimal("-0.65"),
            new BigDecimal("0.85"),
            4,
            LocalDateTime.now().minusHours(5)
        ));
        
        newsList.add(createNews(
            "证监会发布新规，优化基金产品注册机制",
            "为进一步规范基金行业发展，保护投资者合法权益，证监会今日发布《关于优化公募基金产品注册机制的通知》，对基金产品注册流程进行多项优化。\n\n新规主要内容包括：简化常规产品注册程序，将注册周期缩短至15个工作日；建立快速注册通道，对符合条件的产品实行5个工作日内完成注册；强化信息披露要求，提高基金产品透明度。\n\n证监会表示，此次改革旨在提高基金产品注册效率，为投资者提供更多优质投资选择。同时，通过加强信息披露和风险揭示，帮助投资者更好地了解产品特性，做出理性投资决策。\n\n基金行业人士普遍认为，新规将促进基金产品创新，提升行业整体竞争力。预计未来将有更多特色化、专业化的基金产品面世，满足不同投资者的需求。",
            "证监会",
            "政策法规",
            null,
            "NEUTRAL",
            new BigDecimal("0.15"),
            new BigDecimal("0.75"),
            3,
            LocalDateTime.now().minusHours(8)
        ));
        
        newsList.add(createNews(
            "医药板块反弹，创新药ETF涨幅居前",
            "经过前期调整，医药板块今日出现反弹。创新药、医疗器械等细分领域表现亮眼，多只相关ETF涨幅超过3%。\n\n具体来看，创新药ETF上涨3.5%，医药ETF上涨2.8%。个股方面，某创新药龙头企业涨停，带动板块情绪回暖。\n\n分析认为，医药板块反弹主要受三方面因素推动：一是估值已处于历史低位，具备安全边际；二是行业政策环境边际改善，集采影响逐步消化；三是部分企业业绩超预期，基本面支撑增强。\n\n基金经理建议，投资者可关注创新药、高端医疗器械等高景气细分领域，选择具有研发实力和产品管线的优质企业。同时要注意控制仓位，分散投资风险。",
            "上海证券报",
            "市场分析",
            null,
            "BULLISH",
            new BigDecimal("0.52"),
            new BigDecimal("0.82"),
            3,
            LocalDateTime.now().minusHours(12)
        ));
        
        newsList.add(createNews(
            "北向资金净流入超百亿元，外资持续加仓A股",
            "今日北向资金大幅净流入，截至收盘净流入金额达到120亿元，创下近一个月新高。\n\n分市场来看，沪股通净流入65亿元，深股通净流入55亿元。从行业流向看，食品饮料、医药生物、电子等板块获外资青睐，而银行、地产等板块出现净流出。\n\n分析人士指出，北向资金大幅流入显示外资对A股市场信心增强。随着中国经济持续复苏、人民币汇率企稳，外资有望持续流入A股市场。\n\n历史数据显示，北向资金的大幅流入往往对市场形成正面影响。投资者可关注外资重点布局的板块和个股，把握结构性机会。",
            "东方财富",
            "资金流向",
            null,
            "BULLISH",
            new BigDecimal("0.68"),
            new BigDecimal("0.90"),
            4,
            LocalDateTime.now().minusHours(15)
        ));
        
        newsList.add(createNews(
            "某明星基金经理离职，引发市场关注",
            "今日，某知名基金公司发布公告，称其明星基金经理因个人原因离职。该基金经理管理规模超过500亿元，任职期间业绩优异，曾获得多项行业大奖。\n\n公告显示，该基金经理管理的多只基金将由其他基金经理接手。基金公司表示，将妥善做好产品交接工作，保障投资者利益不受影响。\n\n市场人士分析，明星基金经理离职可能对相关基金产生短期影响，投资者需关注新任基金经理的投资风格和能力。同时，也要警惕部分投资者可能选择赎回，导致基金规模波动。\n\n对于投资者而言，选择基金时应关注基金公司的整体投研实力，而非过度依赖单一基金经理。分散投资、长期持有是应对基金经理变更的有效策略。",
            "中国基金报",
            "基金动态",
            null,
            "NEUTRAL",
            new BigDecimal("-0.25"),
            new BigDecimal("0.70"),
            3,
            LocalDateTime.now().minusDays(2)
        ));
        
        newsList.add(createNews(
            "债券市场波动加剧，债基收益承压",
            "受通胀预期上升和货币政策收紧预期影响，近期债券市场波动加剧，债券基金收益面临压力。\n\n数据显示，近一个月债券基金平均收益率为-0.5%，部分纯债基金甚至出现亏损。其中，某知名债基近一个月净值下跌1.2%，引发投资者关注。\n\n分析人士指出，债券市场调整主要受三方面因素影响：一是经济复苏预期增强，市场对货币政策收紧的担忧上升；二是通胀数据超预期，实际利率水平上升；三是前期债券涨幅较大，部分资金获利了结。\n\n对于债券基金投资者，建议关注基金的久期配置和信用风险控制能力。同时，可考虑配置部分货币基金或短债基金，降低组合波动。",
            "证券日报",
            "市场分析",
            null,
            "BEARISH",
            new BigDecimal("-0.58"),
            new BigDecimal("0.83"),
            4,
            LocalDateTime.now().minusDays(2)
        ));
        
        newsList.add(createNews(
            "ETF市场规模突破2万亿元，产品创新加速",
            "随着投资者对指数化投资认可度提升，ETF市场持续扩容。最新数据显示，国内ETF市场规模已突破2万亿元，产品数量超过800只。\n\n今年以来，ETF市场呈现三大特点：一是规模快速增长，新发ETF募集规模超过1000亿元；二是产品创新加速，涌现出多只特色ETF，如碳中和ETF、数字经济ETF等；三是投资者结构优化，机构投资者占比持续提升。\n\n业内人士表示，ETF作为低成本、高透明度的投资工具，正受到越来越多投资者的青睐。未来，随着产品丰富度和流动性提升，ETF市场有望继续保持快速发展。\n\n对于普通投资者，ETF提供了便捷的资产配置工具。建议根据自身风险偏好和投资目标，选择合适的ETF产品，实现分散投资。",
            "基金业协会",
            "行业动态",
            null,
            "BULLISH",
            new BigDecimal("0.42"),
            new BigDecimal("0.78"),
            3,
            LocalDateTime.now().minusDays(2)
        ));
        
        newsList.add(createNews(
            "公募基金分红总额超2000亿元，创历史新高",
            "2025年，公募基金分红总额达到2056亿元，创下历史新高。其中，权益类基金分红占比超过60%。\n\n数据显示，分红金额排名前列的基金多为规模较大的产品。某知名混合基金单次分红超过50亿元，成为年度分红王。\n\n分析人士指出，基金分红是基金经理兑现收益、回馈投资者的重要方式。高分红往往意味着基金前期业绩较好，但也可能影响后续投资运作。\n\n对于投资者而言，分红方式选择需要考虑个人情况。现金分红可获得即时收益，适合需要现金流的投资者；红利再投资可享受复利效应，适合长期投资者。",
            "中国证券报",
            "基金动态",
            null,
            "NEUTRAL",
            new BigDecimal("0.08"),
            new BigDecimal("0.72"),
            2,
            LocalDateTime.now().minusDays(3)
        ));
        
        newsList.add(createNews(
            "半导体板块大涨，国产替代概念受追捧",
            "受益于国产替代政策支持和行业景气度提升，半导体板块今日大涨。芯片设计、半导体设备、材料等细分领域表现突出。\n\n截至收盘，半导体指数上涨4.5%，多只半导体ETF涨幅超过5%。个股方面，某半导体设备公司涨停，带动板块情绪高涨。\n\n分析认为，半导体板块走强主要受三方面因素推动：一是国家持续加大对半导体产业的支持力度，国产替代进程加速；二是全球半导体需求旺盛，行业景气度持续；三是部分企业业绩超预期，基本面支撑增强。\n\n基金经理建议，投资者可关注具有核心技术优势和进口替代能力的龙头企业。同时要注意行业周期性，控制投资节奏。",
            "证券时报",
            "市场分析",
            null,
            "BULLISH",
            new BigDecimal("0.72"),
            new BigDecimal("0.86"),
            4,
            LocalDateTime.now().minusDays(3)
        ));
        
        newsList.add(createNews(
            "QDII基金表现亮眼，海外投资热度升温",
            "随着海外市场回暖和投资者全球化配置需求提升，QDII基金近期表现亮眼，资金流入明显增加。\n\n数据显示，近一个月QDII基金平均收益率为3.2%，远超国内股票基金。其中，投资美股、港股的QDII基金表现尤为突出。\n\n分析人士指出，QDII基金受青睐主要基于三方面原因：一是海外市场估值相对较低，投资价值凸显；二是分散单一市场风险，优化资产配置；三是人民币汇率波动，通过QDII投资可对冲汇率风险。\n\n不过，投资QDII基金也需注意风险，包括汇率风险、市场风险、政策风险等。投资者应根据自身情况，合理配置QDII基金比例。",
            "上海证券报",
            "基金动态",
            null,
            "BULLISH",
            new BigDecimal("0.55"),
            new BigDecimal("0.80"),
            3,
            LocalDateTime.now().minusDays(3)
        ));
        
        newsList.add(createNews(
            "基金销售渠道竞争加剧，费率优惠成常态",
            "随着基金销售市场竞争加剧，各大渠道纷纷推出费率优惠活动，基金申购费率持续下降。\n\n目前，多家互联网平台将基金申购费率降至一折，部分平台甚至推出零费率活动。银行渠道也开始跟进，对特定产品实行费率优惠。\n\n分析人士指出，费率竞争有利于降低投资者成本，但也可能导致渠道收入下降，影响服务质量。长期来看，渠道竞争将推动行业向专业化、服务化转型。\n\n对于投资者，选择基金时不应仅关注费率，更要考虑基金本身的投资价值和管理能力。同时，要警惕过度营销，理性投资。",
            "中国基金报",
            "行业动态",
            null,
            "NEUTRAL",
            new BigDecimal("0.05"),
            new BigDecimal("0.68"),
            2,
            LocalDateTime.now().minusDays(4)
        ));
        
        newsList.add(createNews(
            "REITs市场扩容，不动产投资新选择",
            "公募REITs市场持续扩容，产品数量和规模稳步增长，为投资者提供不动产投资新选择。\n\n数据显示，目前公募REITs产品数量超过50只，总规模突破2000亿元。底层资产涵盖仓储物流、产业园区、保障性租赁住房等多个领域。\n\n分析人士表示，REITs作为连接不动产和资本市场的桥梁，具有收益稳定、流动性好等优势。对于投资者而言，REITs提供了参与不动产投资的机会，且门槛相对较低。\n\n不过，投资REITs也需注意风险，包括资产运营风险、市场风险、政策风险等。投资者应充分了解产品特性，根据自身风险承受能力做出投资决策。",
            "证券日报",
            "行业动态",
            null,
            "NEUTRAL",
            new BigDecimal("0.18"),
            new BigDecimal("0.75"),
            2,
            LocalDateTime.now().minusDays(4)
        ));
        
        newsList.add(createNews(
            "量化基金规模突破万亿，投资策略持续创新",
            "随着量化投资理念普及，量化基金规模持续增长，目前已突破1万亿元大关。\n\n数据显示，量化基金数量超过200只，管理规模达到1.2万亿元。投资策略涵盖指数增强、市场中性、CTA、套利等多个领域。\n\n分析人士指出，量化基金受青睐主要基于三方面优势：一是投资纪律性强，避免情绪干扰；二是覆盖面广，能够发现更多投资机会；三是风险控制能力强，回撤相对较小。\n\n不过，量化投资也面临模型失效、过度竞争等风险。投资者应关注量化团队的技术实力和策略迭代能力，选择具有持续创新能力的量化基金。",
            "中国证券报",
            "基金动态",
            null,
            "NEUTRAL",
            new BigDecimal("0.22"),
            new BigDecimal("0.77"),
            2,
            LocalDateTime.now().minusDays(4)
        ));
        
        newsList.add(createNews(
            "养老目标基金规模增长，长期投资理念深入人心",
            "随着养老第三支柱建设推进，养老目标基金规模持续增长，长期投资理念逐渐深入人心。\n\n数据显示，养老目标基金规模已超过3000亿元，产品数量超过150只。其中，目标日期基金和目标风险基金各占半壁江山。\n\n分析人士表示，养老目标基金是个人养老金投资的重要工具，具有资产配置、风险控制、长期投资等优势。随着人口老龄化加剧，养老投资需求将持续增长。\n\n对于投资者，选择养老目标基金应根据自身年龄、风险偏好、退休目标等因素综合考虑。同时要坚持长期投资，避免频繁操作。",
            "基金业协会",
            "基金动态",
            null,
            "BULLISH",
            new BigDecimal("0.38"),
            new BigDecimal("0.79"),
            3,
            LocalDateTime.now().minusDays(5)
        ));
        
        newsList.add(createNews(
            "基金投顾业务发展迅速，服务模式不断创新",
            "随着投资者对专业投顾服务需求提升，基金投顾业务发展迅速，服务模式不断创新。\n\n数据显示，目前获得基金投顾试点资格的机构超过60家，服务资产规模超过5000亿元。服务模式从传统的资产配置建议，扩展到组合管理、风险控制、投资者教育等多个维度。\n\n分析人士指出，基金投顾业务的发展有助于解决投资者「选基难、配置难、持有难」的问题，提升投资体验。未来，随着人工智能等技术应用，投顾服务将更加智能化、个性化。\n\n对于投资者，选择投顾服务时应关注机构的投研实力、服务质量和收费标准。同时要明确自身投资目标和风险偏好，与投顾保持良好沟通。",
            "中国基金报",
            "行业动态",
            null,
            "NEUTRAL",
            new BigDecimal("0.12"),
            new BigDecimal("0.73"),
            2,
            LocalDateTime.now().minusDays(5)
        ));
        
        newsList.add(createNews(
            "绿色基金规模突破5000亿，ESG投资成新趋势",
            "随着可持续发展理念普及，绿色基金规模持续增长，ESG投资成为新趋势。\n\n数据显示，绿色基金规模已超过5000亿元，产品数量超过200只。投资领域涵盖新能源、节能环保、清洁技术等多个方向。\n\n分析人士表示，ESG投资不仅关注财务回报，还考虑环境、社会、治理等非财务因素，有助于识别长期价值。随着监管政策支持和投资者意识提升，ESG投资前景广阔。\n\n对于投资者，选择ESG基金应关注基金公司的ESG投研能力和产品实际运作情况。同时要认识到，ESG投资可能面临短期波动，需要长期持有。",
            "证券时报",
            "行业动态",
            null,
            "BULLISH",
            new BigDecimal("0.45"),
            new BigDecimal("0.81"),
            3,
            LocalDateTime.now().minusDays(5)
        ));
        
        return newsList;
    }
    
    private FundNews createNews(String title, String content, String source, String newsType,
                               String fundCode, String sentiment, BigDecimal sentimentScore,
                               BigDecimal sentimentConfidence, Integer impactLevel, LocalDateTime publishTime) {
        FundNews news = new FundNews();
        news.setTitle(title);
        news.setContent(content);
        news.setSource(source);
        news.setAuthor(null);
        news.setNewsType(newsType);
        news.setFundCode(fundCode);
        news.setSentiment(sentiment);
        news.setSentimentScore(sentimentScore);
        news.setSentimentConfidence(sentimentConfidence);
        news.setImpactLevel(impactLevel);
        news.setPublishTime(publishTime);
        news.setViewCount(0);
        news.setStatus(1);
        return news;
    }
}
