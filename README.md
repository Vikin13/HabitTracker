# HabitTracker

一个轻量级 Android 习惯追踪 App，灵感来自 iBetter。支持记录每日习惯打卡、日历回溯、统计看板。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM |
| DI | Hilt |
| 数据库 | Room + KSP |
| 导航 | Navigation Compose |
| 后台 | WorkManager |
| 最低 SDK | 26 |
| 目标 SDK | 35 |

## 功能

### Home — 今日打卡
- 每日习惯打卡，周进度网格可视化
- 设置对话框：自定义标题、背景图/纯色、颜色主题 (Auto/Light/Dark)
- 暂停习惯在暂停期间隐藏不展示、不可打卡
- ❌ 清除所有数据（设置底部，二次确认）

### History — 日历回溯
- 月历视图，左右滑动切换月份
- 打卡标记：★ 正常打卡 / △ 补签
- 禁止补签到习惯创建日期之前
- 禁止跨越未来月份，暂停期内不可操作

### Tasks — 统计看板
- 各习惯统计卡片：总天数、完成天数、连续天数、最佳连续天数
- 每周完成率百分比
- 左滑露出编辑入口（Animatable 自定义手势）
- 详情弹窗：完整统计 + 近 5 周周完成趋势

### 习惯管理
- 新增/编辑习惯：名称、Emoji（63 个自选）、提醒时间、结束日期、周目标
- Emoji 选择器：LazyVerticalGrid 8 列网格
- 暂停/恢复习惯
- 删除习惯（编辑模式，二次确认）

### 背景系统
- 支持纯色背景（8 个预设色）和自定义图片（缩放 + 偏移）
- 三页统一穿透，暗色背景自动加 scrim 遮罩
- 颜色主题自适应：Auto 模式下根据背景亮度切换暗色/亮色主题

### 导航
- 底部导航栏（纯图标模式）：Today / History / Tasks

## 项目结构

```
app/src/main/java/com/habittracker/app/
├── HabitTrackerApp.kt          # Application 入口 + Hilt
├── MainActivity.kt             # 主 Activity + Scaffold + 底部导航
├── data/
│   ├── local/
│   │   ├── dao/                # Room DAO (HabitDao + RecordDao)
│   │   ├── entity/             # 数据实体
│   │   └── HabitDatabase.kt    # 数据库定义 + Migration
│   ├── repository/
│   │   └── HabitRepository.kt  # 数据仓库
│   └── di/DatabaseModule.kt    # Hilt DI 模块
├── ui/
│   ├── BackgroundManager.kt    # 背景管理（图片/颜色/主题模式）
│   ├── navigation/
│   │   └── AppNavGraph.kt      # 导航图 (Home/Calendar/Stats/AddEditHabit)
│   ├── screens/
│   │   ├── home/               # Home 主页（打卡 + 设置）
│   │   ├── calendar/           # History 日历回溯
│   │   ├── stats/              # Tasks 统计看板
│   │   └── habits/             # 习惯编辑页
│   ├── components/
│   │   └── HabitItem.kt        # 习惯列表项组件
│   └── theme/
│       ├── Theme.kt            # Material3 主题（背景感知）
│       ├── Color.kt            # 调色板
│       └── Type.kt             # 字体配置
└── util/
    └── DateUtils.kt            # 日期工具
```

## 构建

```bash
./gradlew assembleDebug
```

## 截图

> TODO: Add screenshots
