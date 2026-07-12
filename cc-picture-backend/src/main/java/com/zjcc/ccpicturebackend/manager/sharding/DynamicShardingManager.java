package com.zjcc.ccpicturebackend.manager.sharding;

import com.baomidou.mybatisplus.extension.toolkit.SqlRunner;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.model.entity.Space;
import com.zjcc.ccpicturebackend.model.enums.SpaceLevelEnum;
import com.zjcc.ccpicturebackend.model.enums.SpaceTypeEnum;
import com.zjcc.ccpicturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.driver.jdbc.core.connection.ShardingSphereConnection;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.mode.manager.ContextManager;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义 ShardingSphere 分表管理器类
 * ShardingSphere 在分片逻辑初始化时默认获取的是配置的 actual-data-nodes 中的目标表名(固定值)
 * 框架自身不支持动态维护分表, 需要写一个分表管理器，自己来维护分表列表，并更新到 ShardingSphere 的 actual-data-nodes 配置
 * @author zjchenchang
 * @createDate 2026/7/12 15:08
 */
@Component
@Slf4j
public class DynamicShardingManager {
    /**
     * 1.将管理器注册为 Bean，通过 @PostConstruct 注解，在 Bean 加载后获取所有的分表并更新配置。
     * 2.编写获取分表列表的方法，从数据库中查询符合要求的空间列表，再补充上逻辑表，就得到了完整的分表列表。
     * 3.更新 ShardingSphere 的 actual-data-nodes 动态表名配置。获取到 ShardingSphere 的 ContextManager，找到配置文件中的那条规则进行更新即可
     */

    @Resource
    private DataSource dataSource;

    @Resource
    private SpaceService spaceService;

    private static final String LOGIC_TABLE_NAME = "picture";

    private static final String DATABASE_NAME = "logic_db"; // 配置文件中的数据库名称

    @PostConstruct
    public void initialize() {
        log.info("初始化动态分表配置...");
        updateShardingTableNodes();
    }

    /**
     * 获取所有动态表名，包括初始表 picture 和分表 picture_{spaceId}
     */
    private Set<String> fetchAllPictureTableNames() {
        // 仅对"团队空间 + 旗舰版"纳管分表，过滤条件必须与 createSpacePictureTable 保持一致；
        // 否则非旗舰团队空间会被列入路由目标，但其物理分表从未创建，路由过去会报表不存在
        Set<Long> spaceIds = spaceService.lambdaQuery()
                .eq(Space::getSpaceType, SpaceTypeEnum.TEAM.getValue())
                .eq(Space::getSpaceLevel, SpaceLevelEnum.FLAGSHIP.getValue())
                .list()
                .stream()
                .map(Space::getId)
                .collect(Collectors.toSet());
        Set<String> tableNames = spaceIds.stream()
                .map(spaceId -> LOGIC_TABLE_NAME + "_" + spaceId)
                .collect(Collectors.toSet());
        tableNames.add(LOGIC_TABLE_NAME); // 添加初始逻辑表
        return tableNames;
    }

    /**
     * 更新 ShardingSphere 的 actual-data-nodes 动态表名配置
     */
    private void updateShardingTableNodes() {
        Set<String> tableNames = fetchAllPictureTableNames();
        String newActualDataNodes = tableNames.stream()
                .map(tableName -> "cc_picture." + tableName) // 数据源名必须与 application.yml 的 datasource.names(cc_picture) 一致
                .collect(Collectors.joining(","));
        log.info("动态分表 actual-data-nodes 配置: {}", newActualDataNodes);

        ContextManager contextManager = getContextManager();
        ShardingSphereRuleMetaData ruleMetaData = contextManager.getMetaDataContexts()
                .getMetaData()
                .getDatabases()
                .get(DATABASE_NAME)
                .getRuleMetaData();

        Optional<ShardingRule> shardingRule = ruleMetaData.findSingleRule(ShardingRule.class);
        if (shardingRule.isPresent()) {
            ShardingRuleConfiguration ruleConfig = (ShardingRuleConfiguration) shardingRule.get().getConfiguration();
            List<ShardingTableRuleConfiguration> updatedRules = ruleConfig.getTables()
                    .stream()
                    .map(oldTableRule -> {
                        if (LOGIC_TABLE_NAME.equals(oldTableRule.getLogicTable())) {
                            ShardingTableRuleConfiguration newTableRuleConfig = new ShardingTableRuleConfiguration(LOGIC_TABLE_NAME, newActualDataNodes);
                            newTableRuleConfig.setDatabaseShardingStrategy(oldTableRule.getDatabaseShardingStrategy());
                            newTableRuleConfig.setTableShardingStrategy(oldTableRule.getTableShardingStrategy());
                            newTableRuleConfig.setKeyGenerateStrategy(oldTableRule.getKeyGenerateStrategy());
                            newTableRuleConfig.setAuditStrategy(oldTableRule.getAuditStrategy());
                            return newTableRuleConfig;
                        }
                        return oldTableRule;
                    })
                    .collect(Collectors.toList());
            ruleConfig.setTables(updatedRules);
            contextManager.alterRuleConfiguration(DATABASE_NAME, Collections.singleton(ruleConfig));
            contextManager.reloadDatabase(DATABASE_NAME);
            log.info("动态分表规则更新成功！");
        } else {
            log.error("未找到 ShardingSphere 的分片规则配置，动态分表更新失败。");
        }
    }

    /**
     * 动态创建分表
     */
    public void createSpacePictureTable(Space space) {
        // 仅为"团队空间 + 旗舰版"创建分表
        // 用 Objects.equals 做 null 安全比较：spaceType/spaceLevel 是 Integer，getValue() 是 int，
        // 直接 == 会自动拆箱，字段为 null 时抛 NPE
        if (Objects.equals(space.getSpaceType(), SpaceTypeEnum.TEAM.getValue())
                && Objects.equals(space.getSpaceLevel(), SpaceLevelEnum.FLAGSHIP.getValue())) {
            Long spaceId = space.getId();
            String tableName = "picture_" + spaceId;
            // 用 LIKE 复制逻辑表 picture 的结构(列/索引/引擎)，不复制数据；时序：必须先建表、后纳管
            String createTableSql = "CREATE TABLE " + tableName + " LIKE picture";
            try {
                // MyBatis-Plus 的 SqlRunner，走 ShardingSphere 接管的数据源；此时 picture_{spaceId} 尚未纳管，按单表 DDL 透传到底层
                SqlRunner.db().update(createTableSql);
                // 物理表建成，立即把新表纳入 ShardingSphere 路由认知
                updateShardingTableNodes();
            } catch (Exception e) {
                // 必须传入异常对象 e 才能输出堆栈；同时向上抛出，让"创建空间"的事务感知失败，避免"空间建了、分表没建"的不一致
                log.error("创建图片空间分表失败，空间 id = {}", space.getId(), e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建图片空间分表失败");
            }
        }
    }


    /**
     * 获取 ShardingSphere ContextManager
     */
    private ContextManager getContextManager() {
        try (ShardingSphereConnection connection = dataSource.getConnection().unwrap(ShardingSphereConnection.class)) {
            return connection.getContextManager();
        } catch (SQLException e) {
            throw new RuntimeException("获取 ShardingSphere ContextManager 失败", e);
        }
    }
}
