# Todo List

## Done

- ✅ **背景亮度检测** — `BackgroundSettings.scrimAlpha()` 自动检测背景亮度，IMAGE 0.18f / 暗色 0.30f 遮罩
- ✅ **Calendar 布局上移+手势滑动** — 替换 TopAppBar 为紧凑标题，添加左右滑动切换月份
- ✅ **编辑页布局优化** — Reminder/End date/Weekly goal 标签值并排；Save/Delete 同行
- ✅ **统计 Rate 修复** — `calculateStat` 考虑 endDate，只统计活跃天数
- ✅ **统计弹窗丰富** — 新增 Missed Days、Best Streak、Weekly Trend
- ✅ **Habit 暂停** — `isArchived` → `pausedAt`/`resumedAt`，编辑页 Pause/Resume，暂停项排末尾
- ✅ **暂停可视化** — 主页/统计/日历统一用 isActiveOn 过滤，暂停期间不可见/不可打卡
- ✅ **统计实时刷新** — RecordDao.anyRecordCount Flow + combine 监听 habits+records 双表变化
- ✅ **当前周完成率** — habit 卡片右侧显示本周完成率百分比
- ✅ **Calendar 禁止未来月份/补打卡保护** — 不能滑到未来月、不能补打卡到创建日期前、暂停期不可操作
- ✅ **Calendar 视觉优化** — 未来/非当月透明占位、CircleShape、padding 1dp
- ✅ **Home 暂停过滤** — isActiveOn(todayDate) 过滤，暂停习惯不在主页展示
- ✅ **每周目标默认 7** — `weeklyTarget` 默认 7，范围 1-7，0→7 迁移 MIGRATION_6_7
- ✅ **滚轴重构** — 用 `derivedStateOf` 计算视口中心选中项，移除不对齐横条
- ✅ **数据库版本 6→7** — MIGRATION_6_7 修复存量 weeklyTarget=0

## Remaining

### P2 — 功能增强

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
