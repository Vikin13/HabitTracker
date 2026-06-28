# Todo List

## Done

- ✅ **背景亮度检测** — `BackgroundSettings.scrimAlpha()` 自动检测背景亮度，IMAGE 0.18f / 暗色 0.30f 遮罩
- ✅ **Calendar 布局上移+手势滑动** — 替换 TopAppBar 为紧凑标题，添加左右滑动切换月份
- ✅ **编辑页布局优化** — Reminder/End date/Weekly goal 标签值并排；Save/Delete 同行
- ✅ **统计 Rate 修复** — `calculateStat` 考虑 endDate，只统计活跃天数
- ✅ **统计弹窗丰富** — 新增 Missed Days、Best Streak、Weekly Trend
- ✅ **Habit 暂停** — `isArchived` 字段 + Room v3→v4 迁移，编辑页 Pause/Resume，暂停项排末尾
- ✅ **暂停可视化** — 主页半透明 + (Paused) 标签 + 隐藏打卡圈；统计页 (Paused) 标注
- ✅ **GitHub 推送** — 项目已推送至 https://github.com/Vikin13/HabitTracker

## Remaining

### P1 — 功能完善

### 1. 自定义排序功能
- 当前 `sortOrder` 字段存在，但无 UI 调整排序
- 需要：拖拽排序或上下移动按钮
- 可能的方案：`LazyColumn` 拖拽（modifier + state）或 `moveUp`/`moveDown` 按钮

### 2. Habit 颜色标识
- `HabitColors` 调色板已定义但未使用
- 每个习惯可选颜色，在日历圆点和统计卡片体现

### 3. 统计页交互增强
- 当前只用来看统计，可增加：暂停/恢复快捷操作、批量管理
- 使其成为真正的 habit list 管理中心

### 4. Weekly Grid 效率优化
- 当前每周刷新全量数据，多习惯时 DAO 查询频繁
- 可批量查询或缓存

---

## Icebox

- 图片背景支持从相机拍摄
- 多语言支持
- 数据导出/备份
- 桌面 Widget
