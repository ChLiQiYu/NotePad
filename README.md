# NotePad - Android笔记管理应用

## 项目整体描述

NotePad是一个基于Android平台的笔记管理应用程序，源自Android官方示例项目并进行了功能扩展。该项目旨在提供一个简单易用的个人笔记管理工具，支持笔记的创建、编辑、删除等基本操作，同时增加了分类管理和状态标记等实用功能，帮助用户更好地组织和管理个人笔记内容。

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
- **兼容性支持**：使用AppCompat库确保向后兼容性

### 架构特点

- **模块化设计**：各功能模块相对独立，便于维护和扩展
- **数据访问层**：封装了SQLite数据库操作，提供统一的数据访问接口
- **UI组件化**：复用布局组件，提高开发效率

## 页面截图展示

### 主列表页
【此处插入主列表页面截图】

### 分类管理页
【此处插入分类管理页面截图】

### 笔记编辑页
【此处插入笔记编辑页面截图】

### 分类编辑页
【此处插入分类编辑页面截图】

## 使用说明

### 编译和运行步骤

1. 使用Android Studio打开项目
2. 确保已安装Android SDK API 23及以上版本
3. 同步Gradle依赖
4. 构建项目：
   ```bash
   ./gradlew assembleDebug
   ```
5. 在模拟器或真机上运行应用

### API兼容性说明

本应用完全兼容Android API 23及以上版本设备，已在以下版本测试通过：
- Android 6.0 (API 23)
- Android 7.0 (API 24)
- Android 10 (API 29)
- Android 14 (API 34)

应用使用AppCompat库确保在不同Android版本上的一致体验。

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
│   └── NotePad.java            # 数据契约类
└── src/main/res/
    ├── layout/                  # 布局文件
    ├── menu/                    # 菜单资源
    └── values/                  # 字符串等资源
```