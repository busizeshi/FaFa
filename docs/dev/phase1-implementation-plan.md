# Phase 1 宠物档案 - 实施计划

> 关联文档：
> - [分阶段开发计划.md](../分阶段开发计划.md) - Phase 1 概要
> - [FaFa产品功能设计-最终版.md](../FaFa产品功能设计-最终版.md) - 第二章宠物档案
> - [development-guidelines.md](../../.agent/development-guidelines.md) - DDD 设计规范

---

## 一、目标与范围

### 核心目标
用户能完整建立并管理宠物档案，这是后续所有功能（记录、素材、AI）的数据挂载点。

### 交付范围
- ✅ 宠物 CRUD（创建/更新/删除/列表/详情）
- ✅ 头像与三视图上传（走 `StorageService` 策略）
- ✅ 手动排序（多宠物顺序调整）
- ✅ 宠物数量上限：普通用户 2 只 / 会员不限（配置化）
- ✅ 三视图向量化：上传后调用 Python `/api/pets/profile-photos`

### 明确不做
- ❌ 宠物数字主页的"最近动态"与"AI 最近发现"（依赖 Phase 2/4/5 数据）
- ❌ 自动以宠物名称创建 user_tag（Phase 4 素材模块实现）
- ❌ 宠物删除时清理 Qdrant 向量（Phase 4 补充，当前只做软删）

---

## 二、数据库设计

### 2.1 已有表（V1.0 已建）

```sql
CREATE TABLE `pet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户',
  `name` VARCHAR(32) NOT NULL COMMENT '宠物名称',
  `type` VARCHAR(16) NOT NULL COMMENT '宠物类型: cat/dog/other',
  `breed` VARCHAR(64) NULL COMMENT '品种',
  `gender` VARCHAR(16) NOT NULL COMMENT '性别: male/female/unknown',
  `birth_date` DATE NULL COMMENT '出生日期（外推，用于自动计算年龄）',
  `is_neutered` TINYINT NOT NULL DEFAULT 0 COMMENT '是否绝育: 0否1是',
  `adopt_date` DATE NULL COMMENT '到家时间',
  `weight` DECIMAL(5,2) NULL COMMENT '当前体重（kg），冗余最新值',
  `avatar_url` VARCHAR(512) NULL COMMENT '头像URL',
  `front_photo_url` VARCHAR(512) NULL COMMENT '正面照URL',
  `side_photo_url` VARCHAR(512) NULL COMMENT '侧面照URL',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序字段，越小越靠前',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0否1是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_user_id` (`user_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';
```

### 2.2 本期无需新建表

宠物基础信息完全由 `pet` 表承载。三视图向量化在 Qdrant 的 `fafa_pet_profiles` 集合（Phase 0 已创建）。

---

## 三、Java 实现清单

### 3.1 领域层（domain/pet）

#### Pet 实体
```java
package com.fafa.domain.pet;

/**
 * 宠物聚合根
 * 
 * 职责：
 * - 封装宠物的核心业务规则（命名校验、年龄计算、体重更新）
 * - 维护聚合内一致性（三视图 URL 要么全空要么全有）
 */
@Getter
public class Pet {
    private PetId id;
    private UserId userId;
    private String name;            // 长度 1-32
    private PetType type;           // cat/dog/other
    private String breed;           // 可空，长度 ≤64
    private Gender gender;
    private LocalDate birthDate;    // 可空，用户提供"几年几个月"外推
    private Boolean isNeutered;
    private LocalDate adoptDate;    // 可空
    private BigDecimal weight;      // 可空，单位 kg
    private String avatarUrl;       // 头像
    private String frontPhotoUrl;   // 正面照
    private String sidePhotoUrl;    // 侧面照
    private Integer sortOrder;      // 排序，默认 0
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 领域行为
    public static Pet register(UserId userId, String name, PetType type, Gender gender, 
                                 Integer ageYears, Integer ageMonths) {
        // 校验：名称非空且长度合法
        // 外推 birthDate = now - (years * 12 + months)
        // 初始化 sortOrder = 0
    }

    public void updateProfile(String name, String breed, Gender gender, LocalDate birthDate,
                               Boolean isNeutered, LocalDate adoptDate) {
        // 更新基础信息，校验名称长度
    }

    public void updateWeight(BigDecimal newWeight) {
        // 校验体重 > 0
        this.weight = newWeight;
    }

    public void updateAvatar(String newAvatarUrl) {
        // 头像单独更新
    }

    public void updateProfilePhotos(String frontUrl, String sideUrl) {
        // 三视图必须同时提供或同时为空
        if ((frontUrl == null) != (sideUrl == null)) {
            throw new IllegalArgumentException("正面照与侧面照必须同时提供");
        }
        this.frontPhotoUrl = frontUrl;
        this.sidePhotoUrl = sideUrl;
    }

    public void adjustSortOrder(Integer newOrder) {
        this.sortOrder = newOrder;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public Integer getAgeInMonths() {
        // 计算当前年龄（月）
        if (birthDate == null) return null;
        return (int) ChronoUnit.MONTHS.between(birthDate, LocalDate.now());
    }

    public Integer getDaysWithOwner() {
        // 计算到家天数
        if (adoptDate == null) return null;
        return (int) ChronoUnit.DAYS.between(adoptDate, LocalDate.now());
    }
}
```

#### 值对象
```java
public record PetId(Long value) {
    public PetId {
        Objects.requireNonNull(value, "PetId 不能为空");
    }
}

public enum PetType {
    CAT, DOG, OTHER
}

public enum Gender {
    MALE, FEMALE, UNKNOWN
}
```

#### Repository 接口
```java
package com.fafa.domain.pet;

public interface PetRepository {
    Pet findById(PetId id);
    List<Pet> findByUserId(UserId userId);      // 按 sort_order 升序
    int countByUserId(UserId userId);           // 宠物数量统计
    void save(Pet pet);
    void delete(PetId id);                      // 软删
}
```

---

### 3.2 应用层（application/pet）

#### PetApplicationService
```java
package com.fafa.application.pet;

@Service
@RequiredArgsConstructor
public class PetApplicationService {
    
    private final PetRepository petRepository;
    private final StorageService storageService;
    private final PythonAiClient pythonAiClient;
    private final PetQuotaPolicy quotaPolicy;  // 配置化策略，见下节

    /**
     * 创建宠物
     * 
     * 流程：
     * 1. 检查数量上限（quotaPolicy）
     * 2. 领域模型 Pet.register
     * 3. 保存
     * 4. 返回 PetId
     */
    @Transactional
    public PetId createPet(CreatePetCommand cmd) {
        // 1. 检查数量
        int currentCount = petRepository.countByUserId(cmd.getUserId());
        if (!quotaPolicy.canCreatePet(cmd.getUserId(), currentCount)) {
            throw new BusinessException(ErrorCode.PET_LIMIT_EXCEEDED);
        }

        // 2. 创建
        Pet pet = Pet.register(cmd.getUserId(), cmd.getName(), cmd.getType(), 
                                cmd.getGender(), cmd.getAgeYears(), cmd.getAgeMonths());
        if (cmd.getBreed() != null) {
            pet.updateProfile(pet.getName(), cmd.getBreed(), pet.getGender(), 
                              pet.getBirthDate(), cmd.getIsNeutered(), cmd.getAdoptDate());
        }

        petRepository.save(pet);
        return pet.getId();
    }

    /**
     * 上传头像
     * 
     * 流程：
     * 1. 查询宠物
     * 2. 删除旧头像（OSS + DB）
     * 3. 上传新头像到 StorageService（bucket: fafa-avatar）
     * 4. 更新 pet.avatarUrl
     */
    @Transactional
    public void uploadAvatar(PetId petId, MultipartFile file) {
        Pet pet = petRepository.findById(petId);
        if (pet == null || pet.getIsDeleted()) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }

        // 删除旧头像
        if (pet.getAvatarUrl() != null) {
            String oldKey = extractObjectKey(pet.getAvatarUrl());
            storageService.delete(oldKey);
        }

        // 上传新头像
        String objectKey = "avatar/" + petId.value() + "/" + UUID.randomUUID() + getExtension(file);
        storageService.upload(objectKey, file.getInputStream(), file.getSize());
        String newUrl = storageService.presign(objectKey, Duration.ofDays(365));

        pet.updateAvatar(newUrl);
        petRepository.save(pet);
    }

    /**
     * 上传三视图
     * 
     * 流程：
     * 1. 查询宠物
     * 2. 删除旧三视图（OSS）
     * 3. 上传新三视图到 StorageService（bucket: fafa-media）
     * 4. 更新 pet.frontPhotoUrl / sidePhotoUrl
     * 5. **调用 Python 向量化**：pythonAiClient.uploadPetProfilePhotos(petId, frontUrl, sideUrl)
     */
    @Transactional
    public void uploadProfilePhotos(PetId petId, MultipartFile frontPhoto, MultipartFile sidePhoto) {
        Pet pet = petRepository.findById(petId);
        if (pet == null || pet.getIsDeleted()) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }

        // 删除旧三视图
        if (pet.getFrontPhotoUrl() != null) {
            storageService.delete(extractObjectKey(pet.getFrontPhotoUrl()));
            storageService.delete(extractObjectKey(pet.getSidePhotoUrl()));
        }

        // 上传新三视图
        String frontKey = "pet-profile/" + petId.value() + "/front_" + UUID.randomUUID() + getExtension(frontPhoto);
        String sideKey = "pet-profile/" + petId.value() + "/side_" + UUID.randomUUID() + getExtension(sidePhoto);

        storageService.upload(frontKey, frontPhoto.getInputStream(), frontPhoto.getSize());
        storageService.upload(sideKey, sidePhoto.getInputStream(), sidePhoto.getSize());

        String frontUrl = storageService.presign(frontKey, Duration.ofDays(365));
        String sideUrl = storageService.presign(sideKey, Duration.ofDays(365));

        pet.updateProfilePhotos(frontUrl, sideUrl);
        petRepository.save(pet);

        // 调用 Python 向量化（异步，失败不影响主流程）
        try {
            pythonAiClient.uploadPetProfilePhotos(petId.value(), frontUrl, sideUrl);
        } catch (Exception ex) {
            log.warn("三视图向量化失败: petId={}", petId.value(), ex);
            // 不抛异常，照片已保存，向量化可后续补偿
        }
    }

    /**
     * 手动排序
     * 
     * 输入：List<PetSortItem> items，每个 item 含 petId + newSortOrder
     * 流程：
     * 1. 批量查询宠物
     * 2. 逐个调用 pet.adjustSortOrder(newOrder)
     * 3. 批量保存
     */
    @Transactional
    public void reorderPets(UserId userId, List<PetSortItem> items) {
        List<PetId> petIds = items.stream().map(PetSortItem::getPetId).toList();
        List<Pet> pets = petIds.stream().map(petRepository::findById).toList();

        // 权限校验：所有宠物必须属于当前用户
        if (pets.stream().anyMatch(p -> !p.getUserId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 更新排序
        for (int i = 0; i < pets.size(); i++) {
            pets.get(i).adjustSortOrder(items.get(i).getNewSortOrder());
        }

        pets.forEach(petRepository::save);
    }

    /**
     * 删除宠物（软删）
     * 
     * 注意：Phase 4 补充 Qdrant 向量清理
     */
    @Transactional
    public void deletePet(PetId petId, UserId userId) {
        Pet pet = petRepository.findById(petId);
        if (pet == null || pet.getIsDeleted()) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
        if (!pet.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        pet.delete();
        petRepository.save(pet);
    }

    /**
     * 查询列表（按 sort_order 升序，过滤已删除）
     */
    public List<PetDTO> listPets(UserId userId) {
        List<Pet> pets = petRepository.findByUserId(userId);
        return pets.stream()
                   .filter(p -> !p.getIsDeleted())
                   .map(PetAssembler::toDTO)
                   .toList();
    }

    /**
     * 查询详情
     */
    public PetDetailDTO getPetDetail(PetId petId, UserId userId) {
        Pet pet = petRepository.findById(petId);
        if (pet == null || pet.getIsDeleted() || !pet.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PET_NOT_FOUND);
        }
        return PetAssembler::toDetailDTO(pet);
    }
}
```

#### 宠物数量上限策略（配置化）
```java
package com.fafa.application.pet;

/**
 * 宠物数量上限策略接口
 * 
 * Phase 1 实现：配置化（application.yml 读取）
 * Phase 7 替换：从数据库 member_quota_config 读取
 */
public interface PetQuotaPolicy {
    boolean canCreatePet(UserId userId, int currentCount);
    int getMaxPets(UserId userId);
}

@Service
@RequiredArgsConstructor
public class ConfigurablePetQuotaPolicy implements PetQuotaPolicy {
    
    @Value("${fafa.pet.limit.normal:2}")
    private int normalLimit;
    
    @Value("${fafa.pet.limit.vip:999}")
    private int vipLimit;
    
    private final UserRepository userRepository;  // 查询用户会员状态

    @Override
    public boolean canCreatePet(UserId userId, int currentCount) {
        int max = getMaxPets(userId);
        return currentCount < max;
    }

    @Override
    public int getMaxPets(UserId userId) {
        User user = userRepository.findById(userId);
        return (user != null && user.isVip()) ? vipLimit : normalLimit;
    }
}
```

**配置文件**：
```yaml
# application-dev.yml
fafa:
  pet:
    limit:
      normal: 2      # 普通用户上限
      vip: 999       # 会员用户上限（实际不限）
```

---

### 3.3 接口层（interfaces/controller）

#### PetController
```java
package com.fafa.interfaces.controller;

@Tag(name = "宠物档案")
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {
    
    private final PetApplicationService petApplicationService;

    @Operation(summary = "创建宠物")
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreatePetRequest request) {
        UserId userId = getCurrentUserId();  // 从 Sa-Token 获取
        CreatePetCommand cmd = PetAssembler.toCommand(userId, request);
        PetId petId = petApplicationService.createPet(cmd);
        return Result.ok(petId.value());
    }

    @Operation(summary = "更新宠物信息")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UpdatePetRequest request) {
        UserId userId = getCurrentUserId();
        UpdatePetCommand cmd = PetAssembler.toCommand(new PetId(id), userId, request);
        petApplicationService.updatePet(cmd);
        return Result.ok();
    }

    @Operation(summary = "查询宠物列表")
    @GetMapping
    public Result<List<PetDTO>> list() {
        UserId userId = getCurrentUserId();
        return Result.ok(petApplicationService.listPets(userId));
    }

    @Operation(summary = "查询宠物详情")
    @GetMapping("/{id}")
    public Result<PetDetailDTO> detail(@PathVariable Long id) {
        UserId userId = getCurrentUserId();
        return Result.ok(petApplicationService.getPetDetail(new PetId(id), userId));
    }

    @Operation(summary = "删除宠物")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        UserId userId = getCurrentUserId();
        petApplicationService.deletePet(new PetId(id), userId);
        return Result.ok();
    }

    @Operation(summary = "上传头像")
    @PostMapping("/{id}/avatar")
    public Result<Void> uploadAvatar(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        // 校验文件类型（jpg/png）和大小（≤5MB）
        petApplicationService.uploadAvatar(new PetId(id), file);
        return Result.ok();
    }

    @Operation(summary = "上传三视图（正面 + 侧面）")
    @PostMapping("/{id}/profile-photos")
    public Result<Void> uploadProfilePhotos(@PathVariable Long id, 
                                              @RequestPart("frontPhoto") MultipartFile frontPhoto,
                                              @RequestPart("sidePhoto") MultipartFile sidePhoto) {
        petApplicationService.uploadProfilePhotos(new PetId(id), frontPhoto, sidePhoto);
        return Result.ok();
    }

    @Operation(summary = "手动排序")
    @PutMapping("/sort")
    public Result<Void> reorder(@RequestBody @Valid ReorderPetsRequest request) {
        UserId userId = getCurrentUserId();
        petApplicationService.reorderPets(userId, request.getItems());
        return Result.ok();
    }
}
```

#### DTO 定义
```java
package com.fafa.interfaces.dto.pet;

// CreatePetRequest
public record CreatePetRequest(
    @NotBlank @Size(max = 32) String name,
    @NotNull PetType type,
    @NotNull Gender gender,
    @Min(0) @Max(30) Integer ageYears,      // 年龄：年
    @Min(0) @Max(11) Integer ageMonths,     // 年龄：月
    String breed,                            // 可空
    Boolean isNeutered,
    LocalDate adoptDate
) {}

// UpdatePetRequest（与 Create 类似，但字段可选）

// PetDTO
public record PetDTO(
    Long id,
    String name,
    PetType type,
    String breed,
    Gender gender,
    Integer ageInMonths,         // 计算值
    Boolean isNeutered,
    BigDecimal weight,
    String avatarUrl,
    Integer sortOrder
) {}

// PetDetailDTO（含三视图 + 到家天数）
public record PetDetailDTO(
    Long id,
    String name,
    PetType type,
    String breed,
    Gender gender,
    Integer ageInMonths,
    Boolean isNeutered,
    LocalDate adoptDate,
    Integer daysWithOwner,       // 计算值
    BigDecimal weight,
    String avatarUrl,
    String frontPhotoUrl,
    String sidePhotoUrl,
    Integer sortOrder
) {}

// ReorderPetsRequest
public record ReorderPetsRequest(
    @NotEmpty List<PetSortItem> items
) {}

public record PetSortItem(
    @NotNull Long petId,
    @NotNull Integer newSortOrder
) {}
```

---

### 3.4 基础设施层（infrastructure/persistence）

#### PetDO
```java
package com.fafa.infrastructure.persistence.pet;

@Data
@TableName("pet")
public class PetDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String type;              // cat/dog/other
    private String breed;
    private String gender;            // male/female/unknown
    private LocalDate birthDate;
    private Boolean isNeutered;
    private LocalDate adoptDate;
    private BigDecimal weight;
    private String avatarUrl;
    private String frontPhotoUrl;
    private String sidePhotoUrl;
    private Integer sortOrder;
    @TableLogic
    private Boolean isDeleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

#### PetMapper
```java
@Mapper
public interface PetMapper extends BaseMapper<PetDO> {
    
    @Select("SELECT * FROM pet WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY sort_order ASC")
    List<PetDO> findByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM pet WHERE user_id = #{userId} AND is_deleted = 0")
    int countByUserId(@Param("userId") Long userId);
}
```

#### PetRepositoryImpl
```java
@Repository
@RequiredArgsConstructor
public class PetRepositoryImpl implements PetRepository {
    
    private final PetMapper petMapper;

    @Override
    public Pet findById(PetId id) {
        PetDO petDO = petMapper.selectById(id.value());
        return petDO == null ? null : PetConverter.toDomain(petDO);
    }

    @Override
    public List<Pet> findByUserId(UserId userId) {
        List<PetDO> list = petMapper.findByUserId(userId.value());
        return list.stream().map(PetConverter::toDomain).toList();
    }

    @Override
    public int countByUserId(UserId userId) {
        return petMapper.countByUserId(userId.value());
    }

    @Override
    public void save(Pet pet) {
        PetDO petDO = PetConverter.toDO(pet);
        if (pet.getId() == null) {
            petMapper.insert(petDO);
            pet.setId(new PetId(petDO.getId()));  // 回填 ID
        } else {
            petMapper.updateById(petDO);
        }
    }

    @Override
    public void delete(PetId id) {
        petMapper.deleteById(id.value());  // MyBatis-Plus 逻辑删除
    }
}
```

#### PetConverter（DO ↔ Domain 转换）
```java
public class PetConverter {
    public static Pet toDomain(PetDO petDO) {
        // 字段映射，枚举转换（type: String -> PetType, gender: String -> Gender）
    }

    public static PetDO toDO(Pet pet) {
        // 反向映射
    }
}
```

---

### 3.5 Python 客户端扩展

```java
package com.fafa.infrastructure.ai;

/**
 * Python AI 服务客户端
 * 
 * Phase 1 新增方法：上传宠物三视图向量化
 */
@Service
@RequiredArgsConstructor
public class PythonAiClient {
    
    @Value("${fafa.python.base-url}")
    private String pythonBaseUrl;

    @Value("${fafa.internal-token}")
    private String internalToken;

    private final RestTemplate restTemplate;

    /**
     * 上传宠物三视图向量化
     * 
     * POST /api/pets/profile-photos
     * Body: { "petId": 123, "frontPhotoUrl": "...", "sidePhotoUrl": "..." }
     * Header: X-Internal-Token
     */
    public void uploadPetProfilePhotos(Long petId, String frontUrl, String sideUrl) {
        String url = pythonBaseUrl + "/api/pets/profile-photos";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "petId", petId,
            "frontPhotoUrl", frontUrl,
            "sidePhotoUrl", sideUrl
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(url, entity, Void.class);
    }
}
```

---

## 四、Python 实现清单

### 4.1 路由新增

```python
# app/api/pets.py

from fastapi import APIRouter, Depends, HTTPException
from app.schemas.pet import PetProfilePhotosRequest
from app.services.pet_service import upload_pet_profile_photos
from app.core.auth import verify_internal_token

router = APIRouter(prefix="/api/pets", tags=["pets"])

@router.post("/profile-photos")
async def upload_profile_photos(
    request: PetProfilePhotosRequest,
    _: None = Depends(verify_internal_token)
):
    """
    上传宠物三视图向量化
    
    流程：
    1. 下载 frontPhotoUrl / sidePhotoUrl
    2. qwen3-vl-embedding 嵌入（1024 维）
    3. Qdrant upsert 到 fafa_pet_profiles
       point_id = uuid5("pet-profile", f"{pet_id}")
       payload = { "pet_id": petId, "user_id": userIdFromJavaCallback }
    """
    try:
        await upload_pet_profile_photos(
            pet_id=request.petId,
            front_url=request.frontPhotoUrl,
            side_url=request.sidePhotoUrl
        )
        return {"status": "ok"}
    except Exception as ex:
        logger.error(f"三视图向量化失败: petId={request.petId}, error={ex}")
        raise HTTPException(status_code=500, detail="向量化失败")
```

### 4.2 Schema
```python
# app/schemas/pet.py

from pydantic import BaseModel

class PetProfilePhotosRequest(BaseModel):
    petId: int
    frontPhotoUrl: str
    sidePhotoUrl: str
```

### 4.3 Service
```python
# app/services/pet_service.py

import uuid
from app.core.qdrant import get_qdrant_client
from app.core.llm import get_model_client, Purpose

COLLECTION_NAME = "fafa_pet_profiles"

async def upload_pet_profile_photos(pet_id: int, front_url: str, side_url: str):
    """
    宠物三视图向量化
    
    1. 下载照片
    2. 多模态嵌入（qwen3-vl-embedding，1024 维）
    3. Qdrant upsert
    """
    # 1. 下载照片（httpx.get）
    front_bytes = await download_image(front_url)
    side_bytes = await download_image(side_url)

    # 2. 嵌入（调用百炼多模态嵌入）
    client = get_model_client(Purpose.MULTIMODAL_EMBEDDING)
    embedding = client.embed_multimodal([front_bytes, side_bytes])  # 返回 1024 维向量

    # 3. Qdrant upsert
    qdrant = get_qdrant_client()
    point_id = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"pet-profile-{pet_id}"))
    
    qdrant.upsert(
        collection_name=COLLECTION_NAME,
        points=[{
            "id": point_id,
            "vector": embedding,
            "payload": {
                "pet_id": pet_id,
                "front_photo_url": front_url,
                "side_photo_url": side_url,
                "created_at": datetime.now().isoformat()
            }
        }]
    )
    
    logger.info(f"宠物三视图向量化完成: petId={pet_id}, pointId={point_id}")
```

---

## 五、验收标准

### 5.1 功能验收
1. **创建宠物**
   - Knife4j `/api/pets` POST，返回 petId
   - 数据库 `pet` 表新增一行，`sort_order=0`
   - 普通用户创建第 3 只时报错 `PET_LIMIT_EXCEEDED`（会员不限）

2. **上传头像**
   - `/api/pets/{id}/avatar` POST，前端传 `multipart/form-data`
   - OSS 中出现新头像文件（MinIO 开发环境，OSS 生产环境）
   - 数据库 `avatar_url` 更新为预签名 URL

3. **上传三视图**
   - `/api/pets/{id}/profile-photos` POST，传 `frontPhoto` + `sidePhoto`
   - OSS 中出现两张照片
   - 数据库 `front_photo_url` / `side_photo_url` 更新
   - Qdrant `fafa_pet_profiles` 集合出现对应 point（point_id = uuid5）
   - Python 日志显示"宠物三视图向量化完成"

4. **手动排序**
   - `/api/pets/sort` PUT，传 `[{petId:1, newSortOrder:10}, {petId:2, newSortOrder:5}]`
   - 列表查询返回顺序为 petId=2, petId=1（按 `sort_order` 升序）

5. **删除宠物**
   - `/api/pets/{id}` DELETE
   - 数据库 `is_deleted=1`
   - 列表不可见

### 5.2 非功能验收
- `mvn compile` 通过
- Knife4j 文档 `http://localhost:8080/doc.html` 可访问，所有接口可调试
- 日志中所有请求含 traceId（MDC）
- Python 日志中可检索到 Java 透传的 traceId

---

## 六、开发顺序

1. **Day 1**：领域模型 + Repository 接口 + DO/Mapper/RepositoryImpl
2. **Day 2**：ApplicationService（不含文件上传）+ Controller 骨架
3. **Day 3**：文件上传（头像 / 三视图）+ Python 向量化
4. **Day 4**：手动排序 + 数量上限策略
5. **Day 5**：Knife4j 接口测试 + 验收 + 文档更新

---

## 七、注意事项

1. **三视图向量化失败不影响主流程**：照片已保存，向量可后续补偿
2. **宠物删除暂不清理 Qdrant**：Phase 4 补充向量清理接口
3. **配置化限额**：`application-dev.yml` 配置 `fafa.pet.limit.normal=2`，Phase 7 迁移到数据库策略
4. **OSS 策略切换**：开发用 MinIO，生产用 OSS，通过 `fafa.storage.type` 条件注册，业务代码同一条
5. **权限校验**：所有宠物操作必须校验 `pet.userId == 当前登录用户`

---

## 八、后续增强（Phase 4/5/6）

- Phase 4：宠物删除时清理 Qdrant 向量（`/api/pets/{petId}/profile-vectors` DELETE）
- Phase 5：宠物数字主页"最近动态"与"AI 最近发现"（聚合记录/照片/预警数据）
- Phase 7：限额策略从配置迁移到数据库 `member_quota_config` 表

---

> 本文档完成时间：2026-08-21  
> 预计开发周期：5 个工作日  
> 责任人：Full-stack AI Agent
