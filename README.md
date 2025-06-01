# SweetData

Minecraft 通用数值同步插件

## 简介

这个插件用于储存和读取自定义数值，方便服主或者其它插件进行读取和写入操作，并且支持全服同步。

## 命令

根命令为 `/sweetdata`，别名为 `/sdata`, `/data`，不建议给玩家任何权限。
| 命令 | 描述 | 权限 |
| --- | --- | --- |
| 全局数据命令 |  | `sweet.data.global.*` |
| `/data <global/g> get <玩家名> <键>` | 获取全局数值 | `sweet.data.global.get` |
| `/data <global/g> set <玩家名> <键> <值>` | 设置全局数值 | `sweet.data.global.set` |
| `/data <global/g> plus <玩家名> <键> <值>` | 如果全局数值是整数，增加数值(可以为负数)，如果数值不是整数或不存在，不进行任何操作 | `sweet.data.global.plus` |
| `/data <global/g> <remove/del> <玩家名> <键>` | 移除全局数值 | `sweet.data.global.del` |
| 玩家数据命令 |  | `sweet.data.player.*` |
| `/data get <玩家名> <键>` | 获取玩家的数值 | `sweet.data.player.get` |
| `/data set <玩家名> <键> <值>` | 设置玩家的数值 | `sweet.data.player.set` |
| `/data plus <玩家名> <键> <值>` | 如果数值是整数，增加玩家的数值(可以为负数)，如果数值不是整数或不存在，不进行任何操作 | `sweet.data.player.plus` |
| `/data <remove/del> <玩家名> <键>` | 移除玩家的数值 | `sweet.data.player.del` |
| 通用命令 |  |  |
| `/data` | 查看帮助命令 | `sweet.data.help` |
| `/data reload database` | 重新连接数据库，并刷新所有缓存 | `sweet.data.reload` |
| `/data reload` | 重载配置文件 | `sweet.data.reload` |

## PAPI 变量

+ `%sweetdata_<键>[;<默认值>]%` 获取玩家数值
+ `%sweetdata_$range,<最小值>,<最大值>$<键>[;<默认值>]%` 判定玩家数值是否是整数，且是否在范围内
