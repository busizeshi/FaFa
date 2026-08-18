# 阶段六：AI Agent 与对话 - 架构设计文档

## 一、设计目标

### 1.1 核心目标
实现基于 LangGraph 的智能对话系统，支持：
1. **多工具调用**：根据用户意图自动选择和调用工具（获取宠物数据、搜索照片等）
2. **上下文理解**：理解对话上下文，维护会话状态
3. **智能推荐**：根据宠物数据主动生成建议问题
4. **对话管理**：支持多会话、历史记录、会话归档

### 1.2 用户场景
- **数据查询**："豆包最近一周的体重变化如何？"
- **照片搜索**："找一下豆包夏天在公园玩的照片"
- **趋势分析**："豆包的饮食习惯有什么变化？"
- **健康建议**："根据豆包最近的表现，有什么健康建议？"
- **提醒管理**："豆包下周有什么待办事项？"

---

## 二、整体架构

### 2.1 系统架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      微信小程序前端                            │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐             │
│  │聊天界面│  │建议问题│  │历史会话│  │数据卡片│             │
│  └───┬────┘  └───┬────┘  └───┬────┘  └────────┘             │
└──────┼───────────┼───────────┼──────────────────────────────┘
       │           │           │
       │ POST /api/ai/chat    │ GET /api/ai/conversations
       │           │           │
┌──────▼───────────▼───────────▼──────────────────────────────┐
│                      Java 服务 (8080)                         │
│  ┌────────────────────────────────────────────────────┐     │
│  │           AIController (接口层)                      │     │
│  │  - chat(): 转发对话请求到 Python                     │     │
│  │  - getConversations(): 查询会话列表                 │     │
│  │  - getMessages(): 查询会话消息                       │     │
│  └─────────────────┬──────────────────────────────────┘     │
│                    │ HTTP                                    │
│                    │ POST http://python-service:8000/ai/chat │
└────────────────────┼─────────────────────────────────────────┘
                     │
┌────────────────────▼─────────────────────────────────────────┐
│                    Python 服务 (8000)                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              LangGraph Agent 核心                      │   │
│  │  ┌────────────┐      ┌────────────┐                  │   │
│  │  │  Agent节点  │ ───> │  Tools节点  │                  │   │
│  │  │ (LLM推理)  │ <─── │ (工具调用)  │                  │   │
│  │  └────────────┘      └──────┬─────┘                  │   │
│  │       │                     │                         │   │
│  │       │                     │ 调用工具                │   │
│  │  ┌────▼─────────────────────▼─────────────────┐      │   │
│  │  │            工具集 (Tools)                    │      │   │
│  │  │  • get_pet_profile        获取宠物档案      │      │   │
│  │  │  • get_weight_history     获取体重历史      │      │   │
│  │  │  • get_feeding_history    获取喂食历史      │      │   │
│  │  │  • get_water_history      获取饮水历史      │      │   │
│  │  │  • get_excretion_history  获取排便历史      │      │   │
│  │  │  • get_event_history      获取事件历史      │      │   │
│  │  │  • search_photos          搜索照片          │      │   │
│  │  │  • get_reminders          获取提醒列表      │      │   │
│  │  │  • analyze_health         分析健康状况      │      │   │
│  │  └──────────────────────────────────────────┘      │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              对话管理服务                              │   │
│  │  • ConversationRepository: 会话持久化                 │   │
│  │  • MessageRepository: 消息持久化                      │   │
│  │  • ChatService: 对话编排                              │   │
│  │  • SuggestionService: 建议问题生成                    │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              外部服务调用                              │   │
│  │  • JavaAPIClient: 调用 Java 服务 API                  │   │
│  │  • QdrantClient: 向量搜索                             │   │
│  │  • DashScopeClient: 通义千问 LLM                      │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
                     │
                     │ MySQL (会话、消息)
                     │ Qdrant (向量搜索)
                     │
┌────────────────────▼─────────────────────────────────────────┐
│                        数据层                                 │
│  • ai_conversation: 对话会话表                                │
│  • ai_message: 对话消息表                                     │
│  • pet, record_*, photo, reminder: 宠物相关数据              │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 技术栈
- **LangGraph**: Agent 工作流编排
- **LangChain**: 工具定义、LLM 调用
- **通义千问 (qwen-max)**: 主力 LLM，支持 Function Calling
- **Qdrant**: 向量搜索（照片语义搜索）
- **MySQL**: 对话历史持久化
- **FastAPI**: Python Web 框架
- **Spring Boot**: Java Web 框架

---

## 三、LangGraph Agent 设计

### 3.1 Agent 状态定义

```python
from typing import TypedDict, Annotated, List
from langgraph.graph.message import add_messages

class AgentState(TypedDict):
    """Agent 状态"""
    # 消息列表（自动追加）
    messages: Annotated[List, add_messages]
    
    # 用户和宠物信息
    user_id: int
    pet_id: int | None
    
    # 会话上下文
    conversation_id: int | None
    context_type: str  # general, data_query, photo_search, diary_write
    
    # 工具调用记录
    tools_called: List[str]
    
    # 宠物上下文数据（缓存）
    pet_context: dict | None
```

### 3.2 工作流设计

```python
from langgraph.graph import StateGraph, END
from langgraph.prebuilt import ToolNode

# 1. 创建工作流图
workflow = StateGraph(AgentState)

# 2. 添加节点
workflow.add_node("agent", call_model)        # LLM 推理节点
workflow.add_node("tools", ToolNode(tools))   # 工具调用节点

# 3. 设置入口点
workflow.set_entry_point("agent")

# 4. 添加条件边（判断是否需要调用工具）
workflow.add_conditional_edges(
    "agent",
    should_continue,  # 判断函数
    {
        "continue": "tools",  # 需要调用工具
        "end": END            # 直接结束
    }
)

# 5. 添加边（工具调用后返回 agent）
workflow.add_edge("tools", "agent")

# 6. 编译
agent = workflow.compile()
```

**流程说明**:
1. 用户消息进入 `agent` 节点
2. LLM 分析是否需要调用工具
3. 如果需要，进入 `tools` 节点执行工具
4. 工具结果返回 `agent` 节点
5. LLM 基于工具结果生成最终回答
6. 循环直到不再需要调用工具

### 3.3 核心节点实现

#### 3.3.1 Agent 节点（LLM 推理）

```python
from langchain_community.chat_models import ChatTongyi
from langchain_core.messages import SystemMessage

async def call_model(state: AgentState):
    """
    调用 LLM 进行推理
    """
    messages = state["messages"]
    
    # 构建系统提示词
    system_prompt = build_system_prompt(state)
    
    # 调用 LLM（支持 Function Calling）
    llm = ChatTongyi(
        model="qwen-max",
        dashscope_api_key=settings.DASHSCOPE_API_KEY,
    ).bind_tools(tools)
    
    response = await llm.ainvoke([system_prompt] + messages)
    
    return {"messages": [response]}


def build_system_prompt(state: AgentState) -> SystemMessage:
    """
    构建系统提示词（包含宠物上下文）
    """
    pet_context = state.get("pet_context")
    
    prompt = f"""你是 FaFa 宠物生活助手的 AI 伙伴。

当前对话上下文：
- 用户 ID: {state['user_id']}
- 宠物 ID: {state['pet_id']}
- 宠物信息: {pet_context['name'] if pet_context else '未指定'}

你的职责：
1. 理解用户意图，必要时调用工具获取数据
2. 基于工具返回的数据，提供准确、友好的回答
3. 对数据进行分析和洞察，给出实用建议
4. 保持对话自然流畅

可用工具：
- get_pet_profile: 获取宠物档案（品种、年龄、体重等）
- get_weight_history: 获取体重历史记录
- get_feeding_history: 获取喂食历史记录
- search_photos: 搜索照片（语义搜索）
- get_reminders: 获取提醒列表
- analyze_health: 分析健康状况

回答要求：
1. 简洁明了，避免冗长
2. 数据驱动，基于实际记录
3. 提供建议时要实用且可执行
4. 语气亲切、自然
"""
    
    return SystemMessage(content=prompt)
```

#### 3.3.2 条件判断函数

```python
def should_continue(state: AgentState) -> str:
    """
    判断是否继续调用工具
    """
    messages = state["messages"]
    last_message = messages[-1]
    
    # 如果 LLM 返回了 tool_calls，则继续
    if hasattr(last_message, "tool_calls") and last_message.tool_calls:
        return "continue"
    
    # 否则结束
    return "end"
```

---

## 四、工具（Tools）设计

### 4.1 工具列表

| 工具名称 | 功能描述 | 参数 | 返回值 |
|---------|---------|------|--------|
| `get_pet_profile` | 获取宠物档案 | pet_id | 宠物基本信息 |
| `get_weight_history` | 获取体重历史 | pet_id, days | 体重记录列表 |
| `get_feeding_history` | 获取喂食历史 | pet_id, days | 喂食记录列表 |
| `get_water_history` | 获取饮水历史 | pet_id, days | 饮水记录列表 |
| `get_excretion_history` | 获取排便历史 | pet_id, days | 排便记录列表 |
| `get_event_history` | 获取事件历史 | pet_id, event_type, days | 事件记录列表 |
| `search_photos` | 搜索照片 | pet_id, query, limit | 照片列表 |
| `get_reminders` | 获取提醒列表 | pet_id, status | 提醒列表 |
| `analyze_health` | 分析健康状况 | pet_id | 健康分析报告 |

### 4.2 工具实现示例

#### 4.2.1 get_pet_profile（获取宠物档案）

```python
from langchain_core.tools import tool
from app.clients.java_api_client import JavaAPIClient

java_client = JavaAPIClient()

@tool
async def get_pet_profile(pet_id: int) -> dict:
    """
    获取宠物档案信息
    
    Args:
        pet_id: 宠物 ID
    
    Returns:
        宠物档案，包含名称、品种、年龄、体重等信息
    """
    try:
        response = await java_client.get(f"/api/pets/{pet_id}")
        return {
            "success": True,
            "data": {
                "id": response["id"],
                "name": response["name"],
                "species": response["species"],
                "breed": response["breed"],
                "gender": response["gender"],
                "birthday": response["birthday"],
                "weight": response["currentWeight"],
                "avatar": response["avatar"],
            }
        }
    except Exception as e:
        return {"success": False, "error": str(e)}
```

#### 4.2.2 get_weight_history（获取体重历史）

```python
@tool
async def get_weight_history(pet_id: int, days: int = 30) -> dict:
    """
    获取宠物体重历史记录
    
    Args:
        pet_id: 宠物 ID
        days: 查询天数，默认 30 天
    
    Returns:
        体重记录列表，包含日期、体重、变化趋势
    """
    try:
        response = await java_client.get(
            f"/api/records/weight",
            params={"petId": pet_id, "days": days}
        )
        
        records = response.get("records", [])
        
        # 计算趋势
        if len(records) >= 2:
            first_weight = records[-1]["weight"]
            last_weight = records[0]["weight"]
            change = last_weight - first_weight
            trend = "上升" if change > 0 else "下降" if change < 0 else "稳定"
        else:
            change = 0
            trend = "数据不足"
        
        return {
            "success": True,
            "data": {
                "records": records,
                "summary": {
                    "total_count": len(records),
                    "first_weight": records[-1]["weight"] if records else None,
                    "last_weight": records[0]["weight"] if records else None,
                    "change": round(change, 2),
                    "trend": trend,
                }
            }
        }
    except Exception as e:
        return {"success": False, "error": str(e)}
```

#### 4.2.3 search_photos（搜索照片）

```python
from app.service.photo_service import PhotoService

photo_service = PhotoService()

@tool
async def search_photos(pet_id: int, query: str, limit: int = 5) -> dict:
    """
    使用语义搜索查找照片
    
    Args:
        pet_id: 宠物 ID
        query: 搜索关键词，例如："夏天在公园玩"、"吃饭的照片"
        limit: 返回数量，默认 5 张
    
    Returns:
        照片列表，包含 URL、描述、相似度
    """
    try:
        results = await photo_service.search(pet_id, query, limit)
        
        return {
            "success": True,
            "data": {
                "query": query,
                "count": len(results),
                "photos": [
                    {
                        "id": r["photo_id"],
                        "url": r["url"],
                        "description": r["description"],
                        "score": r["score"],
                        "taken_at": r["taken_at"],
                    }
                    for r in results
                ]
            }
        }
    except Exception as e:
        return {"success": False, "error": str(e)}
```

#### 4.2.4 analyze_health（分析健康状况）

```python
@tool
async def analyze_health(pet_id: int) -> dict:
    """
    分析宠物健康状况（基于体重、饮食、排便等数据）
    
    Args:
        pet_id: 宠物 ID
    
    Returns:
        健康分析报告
    """
    try:
        # 获取多维度数据
        weight_data = await get_weight_history(pet_id, 30)
        feeding_data = await get_feeding_history(pet_id, 7)
        excretion_data = await get_excretion_history(pet_id, 7)
        
        # 调用 LLM 进行分析
        prompt = f"""
请基于以下数据分析宠物健康状况：

体重数据（30天）：
{weight_data}

喂食数据（7天）：
{feeding_data}

排便数据（7天）：
{excretion_data}

请从以下维度分析：
1. 体重趋势是否正常
2. 饮食规律性
3. 排便情况
4. 潜在健康风险
5. 改善建议

请用简洁的语言输出分析结果。
"""
        
        analysis = await call_qwen_text(prompt)
        
        return {
            "success": True,
            "data": {
                "analysis": analysis,
                "data_sources": ["weight", "feeding", "excretion"],
            }
        }
    except Exception as e:
        return {"success": False, "error": str(e)}
```

### 4.3 工具注册

```python
from langchain_core.tools import BaseTool

# 所有工具列表
tools: List[BaseTool] = [
    get_pet_profile,
    get_weight_history,
    get_feeding_history,
    get_water_history,
    get_excretion_history,
    get_event_history,
    search_photos,
    get_reminders,
    analyze_health,
]
```

---

## 五、对话管理

### 5.1 会话生命周期

```
创建会话 → 多轮对话 → 归档会话
   │           │           │
   │           │           └─ 超过 24 小时无活动自动归档
   │           └─ 每次对话更新 last_message_at
   └─ 首次对话创建会话记录
```

### 5.2 会话状态管理

#### 5.2.1 会话创建

```python
async def create_conversation(
    user_id: int,
    pet_id: int | None,
    context_type: str = "general"
) -> int:
    """
    创建新会话
    """
    conversation = {
        "user_id": user_id,
        "pet_id": pet_id,
        "context_type": context_type,
        "last_message_at": datetime.now(),
        "message_count": 0,
        "is_archived": 0,
    }
    
    conversation_id = await conversation_repo.create(conversation)
    return conversation_id
```

#### 5.2.2 消息保存

```python
async def save_message(
    conversation_id: int,
    role: str,  # user, assistant, system
    content: str,
    content_type: str = "text",
    metadata: dict | None = None,
) -> int:
    """
    保存消息
    """
    message = {
        "conversation_id": conversation_id,
        "role": role,
        "content": content,
        "content_type": content_type,
        "metadata": metadata,
        "token_count": count_tokens(content) if role == "assistant" else None,
    }
    
    message_id = await message_repo.create(message)
    
    # 更新会话
    await conversation_repo.update(conversation_id, {
        "last_message_at": datetime.now(),
        "message_count": "message_count + 1",  # 原子递增
    })
    
    return message_id
```

#### 5.2.3 历史加载（限制长度）

```python
async def load_history(conversation_id: int, limit: int = 20) -> List[dict]:
    """
    加载会话历史（最近 N 条）
    """
    messages = await message_repo.list_by_conversation(
        conversation_id=conversation_id,
        limit=limit,
        order_by="created_at DESC"
    )
    
    # 反转顺序（时间正序）
    messages.reverse()
    
    # 转换为 LangChain 格式
    return [
        {
            "role": msg["role"],
            "content": msg["content"],
        }
        for msg in messages
    ]
```

### 5.3 上下文类型

| context_type | 说明 | 特点 |
|-------------|------|------|
| `general` | 通用对话 | 无特定上下文，自由聊天 |
| `data_query` | 数据查询 | 侧重调用工具获取数据 |
| `photo_search` | 照片搜索 | 侧重语义搜索照片 |
| `diary_write` | 日记写作 | 辅助用户撰写日记 |
| `health_analysis` | 健康分析 | 深度分析健康状况 |

---

## 六、建议问题生成

### 6.1 生成策略

基于宠物的实际数据，生成个性化的建议问题。

```python
async def generate_suggestions(pet_id: int, user_id: int) -> List[str]:
    """
    生成建议问题
    """
    # 1. 获取宠物基本信息
    pet = await get_pet_profile(pet_id)
    pet_name = pet["data"]["name"]
    
    # 2. 获取最近数据状态
    has_weight = await check_recent_data("weight", pet_id, days=7)
    has_feeding = await check_recent_data("feeding", pet_id, days=3)
    has_photos = await check_recent_data("photos", pet_id, days=30)
    has_reminders = await check_pending_reminders(pet_id)
    
    # 3. 生成建议
    suggestions = []
    
    # 数据相关
    if has_weight:
        suggestions.append(f"{pet_name}最近的体重变化如何？")
    
    if has_feeding:
        suggestions.append(f"{pet_name}这周的饮食规律吗？")
    
    # 照片相关
    if has_photos:
        suggestions.append(f"找一下{pet_name}最近开心的照片")
        suggestions.append(f"看看{pet_name}户外活动的照片")
    
    # 提醒相关
    if has_reminders:
        suggestions.append(f"{pet_name}最近有什么待办事项？")
    
    # 通用问题
    suggestions.extend([
        f"分析一下{pet_name}的健康状况",
        f"给我讲讲{pet_name}的故事",
        f"{pet_name}有什么值得关注的变化吗？",
    ])
    
    # 随机返回 3-5 个
    import random
    return random.sample(suggestions, min(5, len(suggestions)))
```

### 6.2 智能标题生成

为会话自动生成标题（首次对话后）。

```python
async def generate_conversation_title(conversation_id: int) -> str:
    """
    根据首条消息生成会话标题
    """
    messages = await load_history(conversation_id, limit=2)
    
    if not messages:
        return "新对话"
    
    first_user_message = messages[0]["content"]
    
    # 调用 LLM 生成标题
    prompt = f"""
请为以下对话生成一个简短的标题（5-10个字）：

用户消息：{first_user_message}

要求：
1. 简洁明了
2. 概括核心意图
3. 不超过10个字

直接输出标题，不要其他内容。
"""
    
    title = await call_qwen_text(prompt, model="qwen-turbo")
    return title.strip()[:50]  # 限制长度
```

---

## 七、数据流设计

### 7.1 完整对话流程

```
1. 用户发送消息
   ↓
2. 小程序调用 Java 服务
   POST /api/ai/chat
   ↓
3. Java 转发到 Python 服务
   POST http://python-service:8000/ai/chat
   ↓
4. Python ChatService 处理
   ├─ 加载/创建会话
   ├─ 加载历史消息（最近 20 条）
   ├─ 构建 Agent 状态
   ├─ 调用 LangGraph Agent
   │   ├─ Agent 节点：LLM 分析意图
   │   ├─ 判断是否需要工具
   │   ├─ Tools 节点：执行工具调用
   │   │   ├─ 调用 Java API 获取数据
   │   │   └─ 或调用 Qdrant 搜索
   │   ├─ 返回 Agent 节点
   │   └─ LLM 生成最终回答
   ├─ 保存用户消息
   ├─ 保存 AI 消息
   └─ 返回响应
   ↓
5. Java 返回给小程序
   ↓
6. 小程序展示回答
```

### 7.2 数据结构

#### 7.2.1 对话请求

```json
{
  "userId": 123,
  "petId": 456,
  "message": "豆包最近体重变化如何？",
  "conversationId": 789  // 可选，首次为 null
}
```

#### 7.2.2 对话响应

```json
{
  "conversationId": 789,
  "message": "根据最近 30 天的体重记录，豆包的体重从 5.2kg 增加到 5.5kg，增长了 0.3kg。这个增长速度是正常的...",
  "toolsCalled": ["get_weight_history"],
  "tokenCount": 156,
  "metadata": {
    "context_type": "data_query",
    "data_sources": ["weight_record"]
  }
}
```

---

## 八、性能优化

### 8.1 缓存策略

#### 8.1.1 宠物上下文缓存

```python
from functools import lru_cache
from datetime import datetime, timedelta

# 宠物档案缓存（10 分钟）
@lru_cache(maxsize=1000)
async def get_pet_context_cached(pet_id: int, cache_time: str) -> dict:
    """
    获取宠物上下文（带缓存）
    cache_time: 缓存时间戳（每 10 分钟变化一次）
    """
    return await get_pet_profile(pet_id)

# 调用时
cache_key = datetime.now().strftime("%Y%m%d%H%M")[:-1]  # 精确到 10 分钟
pet_context = await get_pet_context_cached(pet_id, cache_key)
```

#### 8.1.2 向量搜索结果缓存

```python
import hashlib
from app.core.redis_client import redis_client

async def search_photos_with_cache(pet_id: int, query: str, limit: int) -> dict:
    """
    照片搜索（带 Redis 缓存）
    """
    # 生成缓存键
    cache_key = f"photo_search:{pet_id}:{hashlib.md5(query.encode()).hexdigest()}"
    
    # 尝试从缓存获取
    cached = await redis_client.get(cache_key)
    if cached:
        return json.loads(cached)
    
    # 执行搜索
    results = await photo_service.search(pet_id, query, limit)
    
    # 缓存 1 小时
    await redis_client.setex(cache_key, 3600, json.dumps(results))
    
    return results
```

### 8.2 并发控制

```python
import asyncio

async def batch_fetch_data(pet_id: int) -> dict:
    """
    并发获取多个数据源
    """
    tasks = [
        get_weight_history(pet_id, 30),
        get_feeding_history(pet_id, 7),
        get_water_history(pet_id, 7),
        get_excretion_history(pet_id, 7),
    ]
    
    results = await asyncio.gather(*tasks, return_exceptions=True)
    
    return {
        "weight": results[0] if not isinstance(results[0], Exception) else None,
        "feeding": results[1] if not isinstance(results[1], Exception) else None,
        "water": results[2] if not isinstance(results[2], Exception) else None,
        "excretion": results[3] if not isinstance(results[3], Exception) else None,
    }
```

### 8.3 Token 优化

```python
def truncate_history(messages: List[dict], max_tokens: int = 4000) -> List[dict]:
    """
    截断历史消息以控制 Token 数量
    """
    total_tokens = 0
    truncated = []
    
    # 从最新消息开始
    for msg in reversed(messages):
        tokens = count_tokens(msg["content"])
        if total_tokens + tokens > max_tokens:
            break
        truncated.insert(0, msg)
        total_tokens += tokens
    
    return truncated
```

---

## 九、错误处理

### 9.1 工具调用失败处理

```python
@tool
async def safe_tool_call(func, *args, **kwargs) -> dict:
    """
    安全的工具调用（自动重试 + 降级）
    """
    max_retries = 3
    
    for i in range(max_retries):
        try:
            result = await func(*args, **kwargs)
            return {"success": True, "data": result}
        except Exception as e:
            if i == max_retries - 1:
                logger.error(f"工具调用失败: {func.__name__}, {e}")
                return {
                    "success": False,
                    "error": f"暂时无法获取数据，请稍后再试",
                    "detail": str(e)
                }
            await asyncio.sleep(0.5 * (i + 1))  # 指数退避
```

### 9.2 LLM 调用超时处理

```python
async def call_model_with_timeout(state: AgentState, timeout: int = 30) -> dict:
    """
    带超时的 LLM 调用
    """
    try:
        result = await asyncio.wait_for(
            call_model(state),
            timeout=timeout
        )
        return result
    except asyncio.TimeoutError:
        logger.error("LLM 调用超时")
        return {
            "messages": [{
                "role": "assistant",
                "content": "抱歉，我思考的时间有点长，请稍后再试。"
            }]
        }
```

---

## 十、监控与日志

### 10.1 关键指标

| 指标 | 说明 | 目标值 |
|-----|------|--------|
| 平均响应时间 | Agent 完整执行时间 | < 5秒 |
| 工具调用成功率 | 工具调用成功 / 总调用 | > 95% |
| 对话成功率 | 成功返回 / 总请求 | > 99% |
| Token 消耗 | 每次对话平均 Token | < 2000 |
| 并发处理能力 | QPS | > 100 |

### 10.2 日志记录

```python
from loguru import logger

async def chat_with_logging(request: ChatRequest) -> ChatResponse:
    """
    带日志的对话处理
    """
    start_time = time.time()
    
    logger.info(f"[Chat Start] user_id={request.user_id}, pet_id={request.pet_id}")
    
    try:
        response = await chat_service.chat(
            user_id=request.user_id,
            pet_id=request.pet_id,
            message=request.message,
            conversation_id=request.conversation_id,
        )
        
        elapsed = time.time() - start_time
        
        logger.info(
            f"[Chat Success] "
            f"conversation_id={response['conversation_id']}, "
            f"tools_called={response.get('tools_called', [])}, "
            f"token_count={response.get('token_count')}, "
            f"elapsed={elapsed:.2f}s"
        )
        
        return response
        
    except Exception as e:
        elapsed = time.time() - start_time
        logger.error(
            f"[Chat Failed] "
            f"user_id={request.user_id}, "
            f"error={str(e)}, "
            f"elapsed={elapsed:.2f}s",
            exc_info=True
        )
        raise
```

---

## 十一、安全与权限

### 11.1 权限校验

```python
async def check_pet_permission(user_id: int, pet_id: int) -> bool:
    """
    校验用户对宠物的访问权限
    """
    pet = await java_client.get(f"/api/pets/{pet_id}")
    return pet["userId"] == user_id


async def chat_with_permission(request: ChatRequest) -> ChatResponse:
    """
    带权限校验的对话
    """
    # 校验宠物归属
    if request.pet_id:
        has_permission = await check_pet_permission(
            request.user_id,
            request.pet_id
        )
        if not has_permission:
            raise HTTPException(status_code=403, detail="无权访问此宠物")
    
    return await chat_service.chat(...)
```

### 11.2 内容安全

```python
async def content_safety_check(content: str) -> bool:
    """
    内容安全检查（可接入阿里云内容安全）
    """
    # TODO: 接入内容安全 API
    # 检测敏感词、违规内容等
    return True
```

---

## 十二、测试策略

### 12.1 单元测试

```python
import pytest
from app.service.chat_service import ChatService

@pytest.mark.asyncio
async def test_chat_with_data_query():
    """
    测试数据查询类对话
    """
    service = ChatService()
    
    response = await service.chat(
        user_id=1,
        pet_id=1,
        message="豆包最近体重变化如何？",
    )
    
    assert response["conversation_id"] is not None
    assert "体重" in response["message"]
    assert "get_weight_history" in response["tools_called"]
```

### 12.2 集成测试

```python
@pytest.mark.asyncio
async def test_full_conversation_flow():
    """
    测试完整对话流程
    """
    # 1. 第一轮对话
    response1 = await chat_service.chat(
        user_id=1,
        pet_id=1,
        message="豆包体重多少？",
    )
    
    conversation_id = response1["conversation_id"]
    
    # 2. 第二轮对话（带上下文）
    response2 = await chat_service.chat(
        user_id=1,
        pet_id=1,
        message="那它的体重正常吗？",
        conversation_id=conversation_id,
    )
    
    # 验证上下文连贯性
    assert conversation_id == response2["conversation_id"]
    assert "正常" in response2["message"] or "不正常" in response2["message"]
```

---

## 十三、部署方案

### 13.1 环境变量配置

```bash
# Python 服务
DASHSCOPE_API_KEY=sk-xxx
AI_MODEL=qwen-max
JAVA_API_BASE_URL=http://java-service:8080
MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_DB=fafa
REDIS_HOST=redis
REDIS_PORT=6379
QDRANT_HOST=qdrant
QDRANT_PORT=6333
```

### 13.2 Docker Compose

```yaml
version: '3.8'

services:
  python-service:
    build: ./fafa-python
    ports:
      - "8000:8000"
    environment:
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
      - JAVA_API_BASE_URL=http://java-service:8080
      - MYSQL_HOST=mysql
      - REDIS_HOST=redis
      - QDRANT_HOST=qdrant
    depends_on:
      - mysql
      - redis
      - qdrant
      - java-service
    
  java-service:
    build: ./fafa-java
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/fafa
      - SPRING_REDIS_HOST=redis
      - PYTHON_API_URL=http://python-service:8000
    depends_on:
      - mysql
      - redis
```

---

## 十四、后续优化方向

### 14.1 短期优化（1-2周）
1. **流式输出**：支持 SSE 流式返回，提升用户体验
2. **多模态输入**：支持用户上传照片提问
3. **智能推荐**：根据对话内容推荐相关功能
4. **会话分类**：自动分类会话类型

### 14.2 长期优化（1-3个月）
1. **记忆系统**：长期记忆用户偏好和宠物特征
2. **主动发现**：AI 主动分析数据并生成洞察
3. **多宠物对话**：同时管理多只宠物的对话上下文
4. **语音交互**：支持语音输入输出
5. **个性化 Agent**：根据用户习惯调整 Agent 行为

---

## 十五、总结

本架构设计实现了基于 LangGraph 的智能对话系统，具备以下核心能力：

### 核心特性
✅ **多工具调用**：9 个专业工具覆盖宠物数据查询、照片搜索、健康分析
✅ **智能编排**：LangGraph 自动判断工具调用流程
✅ **上下文管理**：支持多轮对话、会话历史、上下文缓存
✅ **个性化推荐**：基于数据生成建议问题
✅ **高性能**：并发调用、缓存优化、Token 控制

### 技术亮点
1. **LangGraph 工作流**：清晰的 Agent → Tools → Agent 循环
2. **Function Calling**：通义千问原生支持工具调用
3. **数据驱动**：所有回答基于实际宠物数据
4. **可扩展性**：工具化设计，易于添加新能力

下一步将进入**实际开发阶段**，按照本设计文档实现完整的 AI Agent 系统。
