# SACRED TREES

![banner](common/src/main/resources/banner.png)

> A Minecraft mod that brings the Sacred Rubber Tree from [Minefactory Reloaded](https://github.com/skyboy/MineFactoryReloaded) back!  

Adds Mega, Sacred and Enchanted Sacred saplings for every vanilla trees, including fungus.  
Supports **NeoForge** and **Fabric** for **Minecraft 26.1.2**.  

---
> 一个Minecraft模组，将[我的工厂2(MineFactory Reloaded)](https://github.com/skyboy/MineFactoryReloaded)的附魔神圣橡胶树带回来！  

为每种原版树木(包括菌树)添加宏伟，神圣和附魔神圣树苗。  
支持 **NeoForge** 和 **Fabric** 双平台，基于 **Minecraft 26.1.2**。

## Build / 构建

```bash
# NeoForge
./gradlew :neoforge:build

# Fabric
./gradlew :fabric:build
```

Output JARs will be in `neoforge/build/libs/` and `fabric/build/libs/`.

## Run / 运行

```bash
# NeoForge
./gradlew :neoforge:runClient

# Fabric
./gradlew :fabric:runClient
```

## Project Structure / 项目结构

```
├── common/         # Shared code + resources (tree gen, saplings, assets)
├── neoforge/       # NeoForge platform module
├── fabric/         # Fabric platform module
├── build.gradle    # Root aggregation build
└── settings.gradle # Multi-module declaration
```

## Gallery / 图片展示

![gallery_0](gallery/0.png)
![gallery_1](gallery/1.png)
![gallery_2](gallery/2.png)
![gallery_3](gallery/3.png)

Special thanks to The original MineFactory Reloaded Team for tree generation code!