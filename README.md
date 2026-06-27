# HabitTracker

一个轻量级 Android 习惯追踪 App，灵感来自 iBetter。支持记录每日习惯打卡、查看统计数据和日历视图。

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM |
| DI | Hilt |
| 数据库 | Room + KSP |
| 导航 | Navigation Compose |
| 图表 | Vico Charts |
| 后台 | WorkManager |
| 最低 SDK | 26 |
| 目标 SDK | 35 |

## 功能

- **Today 主页** — 每日习惯打卡，周进度网格，支持最多 5 行显示
- **Calendar 日历** — 月历视图，日期打卡标记（△ 正常 / ★ 补签），滑动切换月份
- **Statistics 统计** — 完成率、连续天数、总天数统计，左滑露出编辑入口
- **习惯管理** — 新增/编辑习惯（名称、Emoji、提醒时间、结束日期、周目标）
- **背景系统** — 支持图片（缩放/偏移）和纯色背景，统一穿透到各页面

## 截图

> TODO: Add screenshots

## 构建

```bash
./gradlew assembleDebug
```

## 项目结构

```
app/src/main/java/com/habittracker/app/
├── HabitTrackerApp.kt          # Application 入口 + Hilt
├── MainActivity.kt             # 主 Activity + Scaffold + 底部导航
├── data/
│   ├── local/
│   │   ├── dao/                # Room DAO
│   │   ├── entity/             # 数据实体
│   │   └── AppDatabase.kt      # 数据库定义
│   ├── repository/
│   │   └── HabitRepository.kt   # 数据仓库
│   └── di/AppModule.kt         # Hilt DI 模块
├── ui/
│   ├── BackgroundManager.kt    # 背景管理（图片/颜色）
│   ├── navigation/
│   │   └── AppNavGraph.kt      # 导航图
│   ├── screens/
│   │   ├── home/               # Today 主页
│   │   ├── calendar/           # 日历页
│   │   ├── stats/              # 统计页
│   │   └── habits/             # 习惯编辑页
│   └── theme/
│       ├── Theme.kt            # Material3 主题
│       └── Type.kt             # 字体配置
└── util/
    └── DateUtils.kt            # 日期工具
```
