# 开发记录

## [1.2.0] - 2026-06-28

### UI 重构

- **底部导航栏精简** — 移除文字标签，仅保留图标，高度自动压缩为紧凑模式
- **页面重命名** — Calendar → **History**，Statistics → **Tasks**，标题居中统一 `headlineMedium`
- **自定义颜色选择器** — 替换 `FilterChip` 为 `Box` + `.clickable` 自定义 chip，文字永远居中，无勾选图标偏移
- **背景色网格对齐** — `FlowRow` → 固定 4 列的 `Row` + `SpaceEvenly`，移除 Lemon/Navy 两个预设色

### 设置对话框优化

- **紧凑布局** — "Reset to system default" 与 "Change photo" 并排显示，缩短为 "Reset"
- **清除所有数据** — 新增 "Clear all data" 按钮（红色），二次确认后删除全部 habits + records
- **颜色主题自适应** — Auto 模式根据背景图片/颜色亮度自动切换暗色/亮色主题

### 数据库变更

- **HabitDao/RecordDao** — 新增 `deleteAll()` 全表清空方法

### 修复

- 修复 `FilterChip` 选中时勾选图标占位导致文字不居中
- 修复 `OutlinedButton` 在暗色主题下文字完全消失的问题
- 修复 设置照片后 "Reset" 按钮额外占据一行压缩 color scheme 空间
- 修复 `FlowRow` 因各色块文字宽度不同导致网格无法对齐

## [1.1.0] - 2026-06-28

### Stats 统计
- **实时刷新** — 新增 RecordDao.anyRecordCount() Flow，StatsViewModel 用 combine 监听 habits + records 双表变化，日历补打卡后自动重算
- **当前周完成率** — 每条 habit 卡片右侧显示本周完成率百分比，替代历史总完成率
- **除零保护** — detail 弹窗的 weeklyTarget 和 expectedTotal 加 coerceAtLeast(1) 兜底
- **存量修复** — MIGRATION_6_7 将旧数据 weeklyTarget=0 设为 7

### Calendar 日历
- **禁止未来月份** — nextMonth() 检查不越过当前月，loadMonth() 钳制
- **禁止补打卡到创建日期前** — toggleDayHabit 检查 date < createdAt 时直接 return
- **暂停期不可操作** — 日历也受 isActiveOn 控制
- **视觉优化** — 未来/非当月日期渲染为透明占位，CircleShape 圆形选中，padding 缩至 1dp

### Home 主页
- **暂停习惯隐藏** — HomeViewModel 用 isActiveOn(todayDate) 过滤，暂停期间不展示、不可打卡

### 习惯编辑页
- **每周目标默认 7** — 不设置即视为每天完成
- **范围 1-7** — 滚轴从 0..31 改为 1..7
- **滚轴重构** — 移除不对齐的中心横条，用 derivedStateOf 计算视口中心选中项，非选中项缩小字号淡化

### 数据库变更
- **版本 6→7** — MIGRATION_6_7 更新 weeklyTarget=0→7

## [1.0.0] - 2026-06-28

### 项目初始化
- 完整 Android 项目骨架搭建（31+ 文件）
- 集成 Kotlin + Jetpack Compose + Hilt + Room + Navigation Compose + Vico Charts
- 数据模型：HabitEntity / RecordEntity + Room DAO + Repository
- Hilt DI 模块 + Material3 主题 + 底部导航

### Today 主页
- 实现标题居中、周进度网格、间距调整
- 设置齿轮和新增按钮对称布局
- 上半部分控制最多展示 5 个习惯，超出可滚动
- 下半部分 LazyColumn 显示完整习惯列表并打卡交互
- 图片预览对话框（两步流程：先预览调大小再 Apply）
- 预览支持手势缩放（0.3f–3.0f）和平移拖拽

### Calendar 日历
- 月历视图，每日打卡标记（★正常 / △补签）
- 透明 TopAppBar，背景穿透显示

### Statistics 统计
- 统计卡片：完成率、连续天数、总天数
- 详情弹窗显示完整统计信息
- 透明 TopAppBar，背景穿透
- **自定义左滑露出编辑** — 用 Animatable + detectHorizontalDragGestures 替代 SwipeToDismissBox
  - 刹车 1/5 屏宽，左右双向可滑
  - 任何操作复位（点击、滚动、拖拽）
  - 卡片先滑走 → 编辑按钮延迟出现（fadeIn + 250ms delay）
  - 编辑按钮为笔图标（Icons.Default.Edit），无底色

### 习惯编辑页
- 新增/编辑习惯表单
- Emoji 选择器：LazyVerticalGrid 8列网格，63个自律主题 Emoji，可滚动
- 标签与值字体区分（labelMedium + 低透明度 vs bodyLarge）
- TimePicker / DatePicker 弹窗内嵌 Clear 按钮
- 数字滚轴选择器（Weekly Goal）
- **删除习惯功能** — 确认对话框 + 删除按钮（仅编辑模式显示）
- 编辑返回使用 navController.popBackStack()

### 背景系统
- Unified BackgroundManager 单例管理背景状态（图片/颜色）
- 支持图片缩放（scale）和偏移（offsetX/offsetY）
- 3 页（Home/Calendar/Stats）统一穿透

### 体验优化
- **背景亮度自适应** — 添加暗色背景检测 + scrim 遮罩（0.18f~0.30f），确保所有文字在深色背景上可读
- **Calendar 手势滑动** — 日历区域支持左右滑动切换月份（阈值 120px）
- **Calendar 布局上移** — 替换 TopAppBar 为紧凑标题，减少垂直空间占用
- **编辑页布局优化** — Reminder/End date/Weekly goal 标签与值并排显示，节省空间
- **Save/Delete 同行** — Save 按钮和 Delete 按钮同一行左右分布
- **统计 Rate 修复** — 计算时考虑 endDate，只统计习惯实际活跃天数
- **统计弹窗丰富** — 新增 Missed Days、Best Streak、Weekly Trend（近5周周完成数）
- **Habit 暂停功能** — 基于 isArchived 字段，编辑页可 Pause/Resume
- **暂停项排末尾** — DAO 查询按 isArchived ASC 排序，活跃在前暂停在后
- **暂停可视化** — 主页/统计页暂停习惯半透明 + (Paused) 标签，隐藏打卡圈

### 修复记录
- 修复 SwipeToDismissBox 交互限制 → 改用自定义 Animatable 实现
- 修复 NavigationBar 强制 height 与 Scaffold innerPadding 冲突 → 移除固定高度
- 修复 edit 页面返回 Behavior → popBackStack()
- 修复 LazyVerticalGrid itemsIndexed 歧义 → 添加 grid.itemsIndexed import
- 修复 fillMaxHeight/roundToInt 等未引入的 import
- 修复 AnimatedVisibility ColumnScope 歧义 → 使用全限定名
- 修复 deleteHabit 确认弹窗缩进错乱 → 移到 Scaffold 外与其他弹窗并列
- 修复 左滑后仅部分操作可复位 → 提升状态到屏幕级别 + 3 层复位机制
