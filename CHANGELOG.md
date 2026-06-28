# 开发记录

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
