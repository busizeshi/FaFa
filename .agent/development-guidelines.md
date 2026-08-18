# FaFa 项目开发规范

## 总体原则

1. **简洁优于复杂**：不过度编码，不过度设计
2. **实用优于完美**：优先实现核心功能，避免过早优化
3. **可读优于简短**：代码要清晰易懂，必要时添加注释
4. **实现后必审查**：每次功能完成后进行代码审查

---

## Java 开发规范

### DDD 设计原则

#### 1. 分层架构
```
├── interfaces/         # 用户接口层（Controller、DTO）
├── application/        # 应用服务层（ApplicationService、事件处理）
├── domain/            # 领域层（Entity、ValueObject、DomainService、Repository接口）
└── infrastructure/    # 基础设施层（Repository实现、外部服务适配器）
```

**规则**：
- **依赖方向**：外层依赖内层，domain 层不依赖任何外层
- **领域层独立**：业务逻辑全部在 domain 层，不依赖框架
- **Repository 接口**：在 domain 层定义，在 infrastructure 层实现

#### 2. 领域模型设计

**实体（Entity）**：
```java
// ✅ 好的实践
@Entity
public class Pet {
    @Id
    private PetId id;
    private String name;
    private PetType type;
    private OwnerId ownerId;
    
    // 领域行为：将业务逻辑封装在实体内
    public void adopt(OwnerId newOwnerId) {
        if (this.ownerId != null) {
            throw new PetAlreadyAdoptedException();
        }
        this.ownerId = newOwnerId;
        // 发布领域事件
        DomainEventPublisher.publish(new PetAdoptedEvent(this.id, newOwnerId));
    }
}

// ❌ 避免的做法：贫血模型
@Entity
public class Pet {
    private Long id;
    private String name;
    // 只有getter/setter，没有业务逻辑
}
```

**值对象（Value Object）**：
```java
// ✅ 不可变值对象
public record PetId(String value) {
    public PetId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PetId 不能为空");
        }
    }
}

// ✅ 复杂值对象
public record Address(String province, String city, String detail) {
    public Address {
        Objects.requireNonNull(province, "省份不能为空");
        Objects.requireNonNull(city, "城市不能为空");
    }
    
    public String getFullAddress() {
        return province + city + detail;
    }
}
```

**聚合根（Aggregate Root）**：
```java
// ✅ 聚合根控制边界内所有访问
@Entity
public class Order {  // 聚合根
    @Id
    private OrderId id;
    
    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> items;  // 聚合内实体
    
    private OrderStatus status;
    
    // 通过聚合根操作内部实体
    public void addItem(Product product, int quantity) {
        if (status != OrderStatus.DRAFT) {
            throw new OrderNotEditableException();
        }
        items.add(new OrderItem(product, quantity));
    }
    
    // 保证聚合内一致性
    public void submit() {
        if (items.isEmpty()) {
            throw new EmptyOrderException();
        }
        this.status = OrderStatus.SUBMITTED;
    }
}
```

**领域服务（Domain Service）**：
```java
// ✅ 当业务逻辑涉及多个实体时使用领域服务
@Service
public class PetAdoptionService {
    
    public void processPetAdoption(Pet pet, User user, AdoptionApplication application) {
        // 跨实体的业务规则
        if (!user.isQualifiedForAdoption()) {
            throw new UserNotQualifiedException();
        }
        
        if (!pet.isAvailableForAdoption()) {
            throw new PetNotAvailableException();
        }
        
        pet.adopt(user.getId());
        application.approve();
    }
}
```

#### 3. Repository 模式

```java
// ✅ 在 domain 层定义接口
package com.fafa.domain.pet;

public interface PetRepository {
    Pet findById(PetId id);
    List<Pet> findByOwner(OwnerId ownerId);
    void save(Pet pet);
    void delete(PetId id);
}

// ✅ 在 infrastructure 层实现
package com.fafa.infrastructure.persistence;

@Repository
public class JpaPetRepository implements PetRepository {
    
    @Autowired
    private PetJpaRepository jpaRepository;
    
    @Override
    public Pet findById(PetId id) {
        return jpaRepository.findById(id.value())
            .map(this::toDomain)
            .orElseThrow(() -> new PetNotFoundException(id));
    }
    
    // DO 转换为领域对象
    private Pet toDomain(PetDO petDO) {
        return new Pet(
            new PetId(petDO.getId()),
            petDO.getName(),
            PetType.valueOf(petDO.getType())
        );
    }
}
```

#### 4. 应用服务层

```java
// ✅ 应用服务协调领域对象，不包含业务逻辑
@Service
@Transactional
public class PetApplicationService {
    
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetAdoptionService adoptionService;
    
    public void adoptPet(AdoptPetCommand command) {
        // 1. 加载领域对象
        Pet pet = petRepository.findById(new PetId(command.petId()));
        User user = userRepository.findById(new UserId(command.userId()));
        
        // 2. 执行领域逻辑
        adoptionService.processPetAdoption(pet, user, application);
        
        // 3. 持久化
        petRepository.save(pet);
        
        // 4. 日志记录
        log.info("宠物领养成功: petId={}, userId={}", command.petId(), command.userId());
    }
}
```

### 企业级开发规范

#### 1. 命名规范

```java
// ✅ 类名：大驼峰，名词
public class PetService { }
public class UserController { }

// ✅ 方法名：小驼峰，动词开头
public void createPet() { }
public Pet findPetById(String id) { }
public boolean isPetAvailable() { }

// ✅ 常量：全大写下划线
public static final int MAX_PET_COUNT = 10;
public static final String DEFAULT_PET_NAME = "未命名";

// ✅ 包命名：小写，按层级组织
com.fafa.domain.pet
com.fafa.application.service
com.fafa.infrastructure.persistence
```

#### 2. 异常处理

```java
// ✅ 自定义业务异常
public class PetNotFoundException extends BusinessException {
    public PetNotFoundException(PetId id) {
        super("PET_NOT_FOUND", "宠物不存在: " + id.value());
    }
}

// ✅ 统一异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity
            .badRequest()
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("SYSTEM_ERROR", "系统异常，请稍后重试"));
    }
}

// ✅ 方法中的异常处理
public Pet findPetById(String id) {
    try {
        return petRepository.findById(new PetId(id));
    } catch (DataAccessException ex) {
        log.error("查询宠物失败: id={}", id, ex);
        throw new SystemException("数据库查询失败", ex);
    }
}
```

#### 3. 日志规范

```java
// ✅ 必须记录日志的场景
@Service
public class PetService {
    
    private static final Logger log = LoggerFactory.getLogger(PetService.class);
    
    public void createPet(CreatePetCommand command) {
        // 1. 关键业务操作 - INFO 级别
        log.info("创建宠物: name={}, type={}, ownerId={}", 
            command.name(), command.type(), command.ownerId());
        
        try {
            Pet pet = new Pet(command.name(), command.type());
            petRepository.save(pet);
            
            // 2. 操作成功 - INFO 级别
            log.info("宠物创建成功: petId=", pet.getId());
            
        } catch (BusinessException ex) {
            // 3. 业务异常 - WARN 级别
            log.warn("宠物创建失败: {}", ex.getMessage());
            throw ex;
            
        } catch (Exception ex) {
            // 4. 系统异常 - ERROR 级别，包含堆栈
            log.error("宠物创建系统异常: command={}", command, ex);
            throw new SystemException("宠物创建失败", ex);
        }
    }
    
    // 5. 外部调用 - 记录请求和响应
    public PetInfo queryPetFromThirdParty(String externalId) {
        log.info("调用第三方接口查询宠物: externalId={}", externalId);
        
        PetInfo info = thirdPartyClient.getPet(externalId);
        
        log.info("第三方接口返回: externalId={}, found={}", 
            externalId, info != null);
        
        return info;
    }
}

// ❌ 避免的做法
log.info("进入方法");  // 无意义日志
log.debug(pet.toString());  // 生产环境看不到
System.out.println("debug");  // 使用标准输出
```

**日志级别使用**：
- **ERROR**：系统异常、数据不一致、外部服务失败
- **WARN**：业务异常、配置缺失、降级处理
- **INFO**：关键业务操作、状态变更、外部调用
- **DEBUG**：详细执行流程（仅开发环境）

#### 4. 注释规范

```java
// ✅ 类注释：说明职责和使用场景
/**
 * 宠物领养应用服务
 * 
 * 负责协调宠物领养流程，包括：
 * - 领养申请提交
 * - 领养资格审核
 * - 领养关系建立
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Service
public class PetAdoptionApplicationService {
}

// ✅ 方法注释：复杂业务逻辑必须说明
/**
 * 处理宠物领养申请
 * 
 * 业务规则：
 * 1. 用户必须通过实名认证
 * 2. 宠物必须处于可领养状态
 * 3. 用户当前领养数量不能超过 3 只
 * 
 * @param command 领养命令
 * @throws UserNotQualifiedException 用户不符合领养条件
 * @throws PetNotAvailableException 宠物不可领养
 */
public void processAdoption(AdoptPetCommand command) {
    // 实现
}

// ✅ 关键逻辑注释
public BigDecimal calculateDiscount(Order order) {
    // 会员折扣：金卡 8 折，银卡 9 折
    BigDecimal memberDiscount = getMemberDiscount(order.getUserLevel());
    
    // 活动折扣：满 100 减 20
    BigDecimal promotionDiscount = getPromotionDiscount(order.getTotalAmount());
    
    // 折扣不叠加，取最优
    return memberDiscount.max(promotionDiscount);
}

// ✅ TODO 和 FIXME
// TODO: 需要添加缓存以提升性能
// FIXME: 并发情况下可能存在库存超卖问题

// ❌ 避免无意义注释
// 获取用户
User user = getUser();  // 代码已经很清晰

// 循环处理
for (Pet pet : pets) {  // 废话
}
```

**注释原则**：
- **必须注释**：复杂算法、业务规则、非显而易见的设计决策
- **建议注释**：公共 API、配置参数含义、特殊处理逻辑
- **不需注释**：简单的 CRUD、代码已经自解释的逻辑

#### 5. 代码审查清单

**功能完成后必须自查**：

- [ ] **DDD 设计**
  - [ ] 领域逻辑是否在 domain 层？
  - [ ] 是否使用了贫血模型（只有 getter/setter）？
  - [ ] Repository 接口是否在 domain 层定义？
  - [ ] 聚合边界是否清晰？

- [ ] **代码质量**
  - [ ] 是否有过度设计（不需要的抽象、接口）？
  - [ ] 是否有重复代码？
  - [ ] 变量和方法命名是否清晰？
  - [ ] 是否有硬编码的魔法值？

- [ ] **异常处理**
  - [ ] 是否捕获了可能的异常？
  - [ ] 异常信息是否明确？
  - [ ] 是否记录了必要的错误日志？

- [ ] **日志**
  - [ ] 关键业务操作是否有日志？
  - [ ] 日志级别是否正确（INFO/WARN/ERROR）？
  - [ ] 日志信息是否包含必要的上下文？

- [ ] **注释**
  - [ ] 复杂业务逻辑是否有注释说明？
  - [ ] 公共 API 是否有 Javadoc？
  - [ ] 是否有无意义的废话注释？

- [ ] **测试**
  - [ ] 核心业务逻辑是否有单元测试？
  - [ ] 边界情况是否覆盖？
  - [ ] 异常场景是否测试？

---

## Python 开发规范

### 1. 代码风格

```python
# ✅ 遵循 PEP 8
# 导入顺序：标准库 -> 第三方库 -> 本地模块
import os
import sys

import numpy as np
import pandas as pd

from fafa.core import PetService
from fafa.utils import logger

# ✅ 类名：大驼峰
class PetAnalyzer:
    pass

# ✅ 函数名和变量：小写下划线
def calculate_pet_score(pet_id: str) -> float:
    max_score = 100
    return max_score

# ✅ 常量：全大写下划线
MAX_PET_COUNT = 10
DEFAULT_SCORE_THRESHOLD = 0.8
```

### 2. 类型注解

```python
# ✅ 使用类型注解提升可读性
from typing import List, Dict, Optional

def get_pet_info(pet_id: str) -> Optional[Dict[str, any]]:
    """获取宠物信息"""
    pass

def batch_process_pets(pet_ids: List[str]) -> Dict[str, float]:
    """批量处理宠物数据"""
    results: Dict[str, float] = {}
    for pet_id in pet_ids:
        results[pet_id] = calculate_score(pet_id)
    return results

# ✅ dataclass 简化数据类
from dataclasses import dataclass

@dataclass
class PetInfo:
    pet_id: str
    name: str
    age: int
    score: float = 0.0
```

### 3. 异常处理

```python
# ✅ 明确的异常类型
class PetNotFoundException(Exception):
    """宠物不存在异常"""
    pass

class InvalidPetDataException(Exception):
    """无效宠物数据异常"""
    pass

# ✅ 具体的异常捕获
def get_pet_by_id(pet_id: str) -> Pet:
    try:
        pet = pet_repository.find_by_id(pet_id)
        if pet is None:
            raise PetNotFoundException(f"宠物不存在: {pet_id}")
        return pet
    except DatabaseError as e:
        logger.error(f"数据库查询失败: pet_id={pet_id}", exc_info=True)
        raise SystemException("查询失败") from e

# ❌ 避免捕获所有异常
try:
    do_something()
except Exception:  # 太宽泛
    pass
```

### 4. 日志规范

```python
import logging

# ✅ 配置日志
logger = logging.getLogger(__name__)

def process_pet_data(pet_id: str):
    # 关键操作记录
    logger.info(f"开始处理宠物数据: pet_id={pet_id}")
    
    try:
        data = fetch_pet_data(pet_id)
        result = analyze_data(data)
        
        logger.info(f"宠物数据处理完成: pet_id={pet_id}, score={result.score}")
        return result
        
    except InvalidDataException as e:
        logger.warning(f"无效的宠物数据: pet_id={pet_id}, error={str(e)}")
        raise
        
    except Exception as e:
        logger.error(f"处理宠物数据失败: pet_id={pet_id}", exc_info=True)
        raise

# ✅ 外部服务调用记录
def call_ai_service(image_url: str):
    logger.info(f"调用 AI 识别服务: url={image_url}")
    
    response = ai_client.recognize(image_url)
    
    logger.info(f"AI 服务返回: confidence={response.confidence}")
    return response
```

### 5. 注释规范

```python
# ✅ 模块注释
"""
宠物数据分析模块

提供宠物行为分析、健康评估等功能
"""

# ✅ 函数文档字符串
def calculate_pet_health_score(
    age: int,
    weight: float,
    activity_level: str
) -> float:
    """
    计算宠物健康评分
    
    根据年龄、体重和活动水平综合评估宠物健康状况
    
    Args:
        age: 宠物年龄（月）
        weight: 宠物体重（kg）
        activity_level: 活动水平，可选值: 'low', 'medium', 'high'
    
    Returns:
        健康评分 (0-100)
    
    Raises:
        ValueError: 参数值不在有效范围内
    """
    if age < 0 or weight <= 0:
        raise ValueError("年龄和体重必须为正数")
    
    # 计算基础分数
    base_score = 100 - (age / 12) * 5
    
    # 体重调整（假设理想体重 10kg）
    weight_penalty = abs(weight - 10) * 2
    
    return max(0, base_score - weight_penalty)

# ✅ 复杂逻辑注释
def recommend_pets(user_preferences: Dict) -> List[Pet]:
    # 第一阶段：按品种过滤
    candidates = filter_by_breed(user_preferences['breeds'])
    
    # 第二阶段：按年龄和性格匹配（权重 0.7）
    scored_pets = score_by_attributes(candidates, user_preferences)
    
    # 第三阶段：按地理位置排序（优先推荐附近的宠物）
    sorted_pets = sort_by_distance(scored_pets, user_preferences['location'])
    
    return sorted_pets[:10]
```

### 6. 代码组织

```python
# ✅ 清晰的模块结构
# fafa/
# ├── domain/           # 领域模型
# │   ├── pet.py
# │   └── user.py
# ├── service/          # 业务服务
# │   ├── pet_service.py
# │   └── recommendation_service.py
# ├── repository/       # 数据访问
# │   └── pet_repository.py
# ├── api/             # API 接口
# │   └── pet_api.py
# └── utils/           # 工具函数
#     └── logger.py

# ✅ 单一职责
class PetRepository:
    """只负责数据访问"""
    
    def find_by_id(self, pet_id: str) -> Optional[Pet]:
        pass
    
    def save(self, pet: Pet) -> None:
        pass

class PetService:
    """只负责业务逻辑"""
    
    def __init__(self, repository: PetRepository):
        self.repository = repository
    
    def adopt_pet(self, pet_id: str, user_id: str) -> None:
        pet = self.repository.find_by_id(pet_id)
        # 业务逻辑
        self.repository.save(pet)
```

### 7. 性能考虑

```python
# ✅ 使用生成器处理大数据
def process_large_dataset(file_path: str):
    """逐行处理避免内存溢出"""
    with open(file_path) as f:
        for line in f:
            yield process_line(line)

# ✅ 批量操作
def batch_insert_pets(pets: List[Pet]):
    """批量插入提升性能"""
    batch_size = 1000
    for i in range(0, len(pets), batch_size):
        batch = pets[i:i + batch_size]
        pet_repository.bulk_insert(batch)
        logger.info(f"已插入 {i + len(batch)} 条记录")

# ✅ 必要时使用缓存
from functools import lru_cache

@lru_cache(maxsize=128)
def get_pet_breed_info(breed_name: str) -> BreedInfo:
    """缓存品种信息，避免重复查询"""
    return breed_repository.find_by_name(breed_name)
```

### 8. 测试规范

```python
# ✅ 单元测试
import pytest

def test_calculate_pet_health_score():
    # 正常情况
    score = calculate_pet_health_score(age=12, weight=10.0, activity_level='medium')
    assert 90 <= score <= 100
    
    # 边界情况
    score = calculate_pet_health_score(age=0, weight=5.0, activity_level='high')
    assert score > 0
    
    # 异常情况
    with pytest.raises(ValueError):
        calculate_pet_health_score(age=-1, weight=10.0, activity_level='medium')

# ✅ Mock 外部依赖
from unittest.mock import Mock, patch

def test_pet_service_with_mock():
    # Mock repository
    mock_repo = Mock(spec=PetRepository)
    mock_repo.find_by_id.return_value = Pet(id="123", name="旺财")
    
    service = PetService(mock_repo)
    pet = service.get_pet("123")
    
    assert pet.name == "旺财"
    mock_repo.find_by_id.assert_called_once_with("123")
```

---

## 通用开发流程

### 1. 需求理解
- 明确功能边界和业务规则
- 识别核心领域概念
- 确定验收标准

### 2. 设计（不过度设计）
- 识别聚合和实体
- 定义领域服务
- 简单够用即可，不提前抽象

### 3. 实现
- 先写核心业务逻辑
- 再补充边界检查和异常处理
- 添加必要日志和注释

### 4. 自测
- 运行单元测试
- 手动测试主流程和边界情况
- 检查日志输出是否完整

### 5. Review（必须）
- 对照上述清单自查
- 确认没有过度设计
- 确认日志和注释齐全

### 6. 提交
- 清理调试代码
- 提交前再次运行测试

---

## AI 开发协作提示词模板

当使用 AI 开发时，可以在提示中加入：

```
请严格遵守项目开发规范（.agent/development-guidelines.md）：
1. Java 代码遵循 DDD 设计，领域逻辑放在 domain 层
2. 不要过度设计，简单够用即可
3. 添加必要的日志（INFO 记录关键操作，ERROR 记录异常）
4. 复杂逻辑添加注释说明业务规则
5. 实现后进行代码审查，检查是否符合规范
```

---

## 工具推荐

- **Java**: 
  - CheckStyle（代码规范检查）
  - SpotBugs（Bug 检测）
  - JaCoCo（测试覆盖率）
  
- **Python**:
  - Black（代码格式化）
  - Flake8（代码检查）
  - MyPy（类型检查）
  - Pytest（测试框架）

---

*最后更新: 2026-08-18*
*文档维护: FaFa Team*
