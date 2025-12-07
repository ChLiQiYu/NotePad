# NotePad - Android笔记管理应用

## 项目整体描述

NotePad是一个基于Android平台的笔记管理应用程序，源自Android官方示例项目并进行了功能扩展。该项目旨在提供一个简单易用的个人笔记管理工具，支持笔记的创建、编辑、删除等基本操作，同时增加了分类管理和状态标记等实用功能，帮助用户更好地组织和管理个人笔记内容。

作为Android平台的经典教学案例，NotePad项目完美诠释了现代Android应用开发的最佳实践。通过这个项目，开发者可以深入理解Content Provider机制、MVC架构模式、数据库操作和用户体验设计等核心概念。

## 功能特性说明

### 基本功能

- **笔记管理**：支持笔记的增删改查（CRUD）操作
- **分类管理**：可创建、编辑、删除笔记分类，每篇笔记可归属到特定分类
- **状态标记**：支持将笔记标记为待办、进行中、已完成三种状态
- **数据持久化**：使用SQLite数据库存储所有笔记和分类信息
- **上下文菜单**：提供便捷的操作菜单，支持快速编辑和删除

### 扩展功能

- **分类筛选**：可通过底部抽屉式面板快速筛选指定分类的笔记
- **UI优化**：现代化界面设计，提升用户体验
- **数据导入导出**：支持JSON格式的笔记数据备份与恢复
- **实时文件夹**：支持将笔记以实时文件夹形式展示在桌面

## 技术实现细节

### 开发环境

- **最低支持版本**：Android API 23 (Android 6.0)
- **目标SDK版本**：API 34 (Android 14)
- **开发语言**：Java 1.8
- **构建工具**：Gradle 8.2+

### 核心技术栈

- **数据库**：SQLite本地数据库存储
- **UI框架**：Android原生组件 + AppCompat库
- **架构模式**：基于Activity的传统Android架构
- **数据访问**：Content Provider内容提供者模式
- **兼容性支持**：使用AppCompat库确保向后兼容性

### 架构特点

- **MVC架构模式**：清晰的Model-View-Controller职责分离
- **模块化设计**：各功能模块相对独立，便于维护和扩展
- **数据访问层**：封装了SQLite数据库操作，提供统一的数据访问接口
- **UI组件化**：复用布局组件，提高开发效率
- **观察者模式**：通过ContentObserver实现数据变更的自动刷新

## 页面截图展示

### 主列表页
![PixPin_2025-12-07_12-04-40.png](https://obsidian-image1-1323713274.cos.ap-guangzhou.myqcloud.com/image/202512071204821.png)
主列表页展示了所有笔记的概览信息，包括笔记标题、修改时间和状态标签。用户可以通过顶部的搜索框快速过滤笔记，也可以通过底部的分类筛选面板按分类查看笔记。

### 分类管理页
![PixPin_2025-12-07_12-04-59.png](https://obsidian-image1-1323713274.cos.ap-guangzhou.myqcloud.com/image/202512071205318.png)
分类管理页允许用户创建、编辑和删除笔记分类。每个分类会显示包含的笔记数量，方便用户了解分类的使用情况。

### 笔记编辑页
![PixPin_2025-12-07_12-05-13.png](https://obsidian-image1-1323713274.cos.ap-guangzhou.myqcloud.com/image/202512071205931.png)
笔记编辑页提供了一个带有行线的文本编辑器，让用户能够舒适地撰写笔记内容。用户可以为笔记设置分类和标记状态（待办/已完成）。

### 分类编辑页
![PixPin_2025-12-07_12-05-43.png](https://obsidian-image1-1323713274.cos.ap-guangzhou.myqcloud.com/image/202512071205498.png)
分类编辑页提供了一个简洁的界面用于创建或修改分类名称，确保分类名称的唯一性。

## 使用说明

### 编译和运行步骤

1. 使用Android Studio打开项目
2. 确保已安装Android SDK API 23及以上版本
3. 同步Gradle依赖
4. 构建项目：
   ```bash
   ./gradlew assembleDebug
   ```
   或在Windows系统上：
   ```bash
   gradlew.bat assembleDebug
   ```
5. 在模拟器或真机上运行应用

### API兼容性说明

本应用完全兼容Android API 23及以上版本设备，已在以下版本测试通过：
- Android 6.0 (API 23)
- Android 7.0 (API 24)
- Android 10 (API 29)
- Android 14 (API 34)

应用使用AppCompat库确保在不同Android版本上的一致体验。

### 权限说明

应用需要以下权限：
- `WRITE_EXTERNAL_STORAGE`：用于笔记数据的导入导出功能

## 架构设计

NotePad项目采用经典的MVC（Model-View-Controller）架构模式，实现了清晰的职责分离：

```
View Layer (视图层):
├── NotesList: 笔记列表界面
├── NoteEditor: 笔记编辑界面
├── CategoriesList: 分类管理界面
└── CategoryEditor: 分类编辑界面

Controller Layer (控制层):
├── NotesList: 列表控制器
├── NoteEditor: 编辑控制器
├── CategoriesList: 分类控制器
└── CategoryEditor: 分类编辑控制器

Model Layer (数据层):
├── NotePadProvider: 内容提供者
├── DatabaseHelper: 数据库助手
└── SQLite数据库: 数据存储
```

### 核心组件

1. **NotesList**：显示笔记列表，支持上下文菜单和选项菜单
2. **NoteEditor**：提供富文本编辑功能，支持行线绘制
3. **CategoriesList**：管理笔记分类，支持分类筛选
4. **CategoryEditor**：编辑分类信息
5. **NotePadProvider**：实现Content Provider接口，提供统一的数据访问接口
6. **DatabaseHelper**：继承SQLiteOpenHelper，管理数据库的创建和版本升级

## 项目结构

```
app/
├── src/main/java/com/example/android/notepad/
│   ├── NoteEditor.java          # 笔记编辑Activity
│   ├── NotesList.java           # 笔记列表Activity
│   ├── CategoryEditorActivity.java # 分类编辑Activity
│   ├── CategoriesListActivity.java # 分类列表Activity
│   ├── Category.java            # 分类数据模型
│   ├── CategoryDataSource.java  # 分类数据访问层
│   ├── Note.java                # 笔记数据模型
│   ├── NoteDataSource.java      # 笔记数据访问层
│   ├── NotePad.java             # 数据契约类
│   ├── NotePadProvider.java     # 内容提供者
│   ├── ExportActivity.java      # 数据导出Activity
│   ├── ImportActivity.java      # 数据导入Activity
│   ├── ExportManager.java       # 数据导出管理器
│   ├── ImportManager.java       # 数据导入管理器
│   └── JsonSerializer.java      # JSON序列化工具
└── src/main/res/
    ├── layout/                  # 布局文件
    │   ├── noteslist_item.xml   # 笔记列表项布局
    │   ├── note_editor.xml      # 笔记编辑器布局
    │   ├── categories_list_item.xml # 分类列表项布局
    │   ├── activity_categories_list.xml # 分类管理界面布局
    │   ├── activity_category_editor.xml # 分类编辑界面布局
    │   └── bottom_sheet_category_filter.xml # 分类筛选底部面板
    ├── menu/                    # 菜单资源
    └── values/                  # 字符串等资源
```

## 数据库设计

SQLite数据库包含以下表结构：

### notes表
- `_ID`: 主键标识符
- `title`: 笔记标题（TEXT类型）
- `note`: 笔记内容（TEXT类型）
- `created`: 创建时间戳（INTEGER类型）
- `modified`: 修改时间戳（INTEGER类型）
- `status`: 笔记状态（INTEGER类型，0-待办，1-已完成）
- `category_id`: 分类ID（INTEGER类型）

### categories表
- `_ID`: 主键标识符
- `name`: 分类名称（TEXT类型）
- `created_time`: 创建时间戳（INTEGER类型）
- `modified_time`: 修改时间戳（INTEGER类型）

## 开发指南

### 代码规范

1. 遵循Android官方编码规范
2. 使用驼峰命名法命名变量和方法
3. 为公共方法添加JavaDoc注释
4. 保持代码简洁，避免过长的方法

### 扩展建议

1. 添加笔记标签功能
2. 实现笔记搜索功能
3. 增加笔记分享功能
4. 添加夜间模式支持
5. 实现云同步功能

## 贡献指南

欢迎提交Issue和Pull Request来改进这个项目。在提交代码前，请确保：

1. 遵循项目的代码规范
2. 添加适当的测试用例
3. 更新相关文档
4. 确保所有测试通过

