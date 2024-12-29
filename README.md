dview-progress-view
![Release](https://jitpack.io/v/dora4/dview-progress-view.svg)
--------------------------------

##### 卡名：Dora视图 ProgressView 
###### 卡片类型：效果怪兽
###### 属性：光
###### 星级：4
###### 种族：天使族
###### 攻击力/防御力：2200/2500
###### 效果：此卡不会因为对方卡的效果而破坏，并可使其无效化。此卡攻击里侧守备表示的怪兽时，若攻击力高于其守备力，则给予对方此卡原攻击力的伤害，并抽一张卡。只要此卡在场上表侧表示存在，我方回合结束阶段，回复500点生命值。

#### Gradle依赖配置

```kotlin
// 添加以下代码到项目根目录下的build.gradle.kts
allprojects {
    repositories {
        maven { setUrl("https://jitpack.io") }
    }
}
// 添加以下代码到app模块的build.gradle.kts
dependencies {
    implementation("com.github.dora4:dview-progress-view:1.0")
}
```
