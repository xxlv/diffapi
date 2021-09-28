## Diff断言工具

提供一种轻量级的针对API进行断言工具。

### 场景

1. 对某些接口进行接口测试。
2. 对某些接口提供 *对比断言* 功能。 使用场景
新老接口替换对比。如上线了新的接口，要测试是否能覆盖到老接口。如新接口 返回{id,name} 老接口返回{id,name}
检测在新老接口返回的数据是否满足需求


### 如何使用？

在.checkpoints/下编写json 文件

- 全局配置

```
{
  "scope": "global",
  "desc": "全局配置",
  "left": {
    "apiAddress": "https://apigw-pre.domain.com/request",
    "method": "post"
  },
  "right": {
    "apiAddress": "https://apigw.domain.com/request",
    "method": "post"
  }
}

``` 
    
在全局配置 中可以定义待验证接口（left）和基准接口（right）的http请求地址

- token配置 
```
{
  "scope": "global",
  "desc": "token配置",
  "left": {
    "token": "$TOKEN"
  },
  "right": {
    "token": "$TOKEN"
  }
}
```
如上配置，可以定义http接口传递的token.(在POST中当作参数传递)


- 对比接口配置(Diff Mode)
```
{
  "desc": "对比LEFT和RIGHT接口 描述",
  "author": "作者",
  "left": {
    "id": "",
    "api": "",
    "payload": []
  },
  "right": {
    "id": "",
    "api": "",
    "payload": [
    ]
  },
  "rules": [
    "$1.success==$2.success",
    "$1.success==true"
  ]
}

```


- 单接口测试配置(Node Mode)

```
{
  "id": "newtest",
  "author": "innovation",
  "desc": "接口测试，在node下配置需要测试的节点，在payload提供参数，在rules 中定义校验规则.",
  "node": {
    "id": "",
    "api": "",
    "payload": [
    ]
  },
  "rules": [
    "$1.success==true",
    "$1.result.values()[0].size()>0"
  ]
}
```

- 接口环境对比（SAME Mode）

```
{
  "id": "newtest-same",
  "author": "innovation",
  "mode": "SAME",
  "desc": "其中left和right为需要对比的不同环境的接口，为left和right启用相同的 payload 期望完全相同的结果返回。一般是发布预发后，在预发与生产的接口进行回归用",
  "left": {
    "api": "your-api"
  },
  "right": {
    "api": "your-api"
  },
  "rules": [
    "$1.success==true",
    "$1.result.values()[0].size()>0",
    "py:result=len(_vars['$1']['result'].keys())>0",
    "py:result=True\nimport time\nprint(time.time())"

  ],
  "sharedPayloads": [
  ]
}

```

 
### 规则脚本支持


diffAPI 默认启用`groovy` 脚本。用来做断言脚本。 在json文件 *rules* 中进行定义。diffAPI不仅仅支持`groovy`，同时还支持
`SPI`模式，以及`Python`模式。
比如规则: `py:print('myRules')` 将使用 `python` 脚本来执行。

#### 规则脚本目前支持如下的语言校验
- Java SPI
- Groovy Script 
将规则本身传递到$0变量中
将left对应的JSON result 传递到 $1变量中
将right对应的JSON result 传递到 $2 变量中
可以diffGroup对象传递到$diffGroup中
如 $1 为 `{"result":true}`
如 $2 为 `{"result":false}`

如可以通过 `$1.result==$2.result` 来进行断言 

- Python Script
将规则/left/right传递到_vars 字典中。
如
```
result=true 
_vars={"$0":ruleScript,"$1":leftResult,"$2":rightResult,"$diffGroup":diffGroup}
```         
需要在脚本中通过设置result 来达到断言的目的
如 
```
result=_vars['$1']['result']==_vars['$2']['result']
```   

### 高级配置
- PayloadSupplier 
对于复杂的请求，如参数payloads 太多，可以使用PayloadSupplier 这个SPI进行生产
- RuleSPI
对于复杂的断言校验，可以通过RuleApi 这个SPI进行生成校验。

PS. 
1. 在json中配置的规则和payloads将会跟SPI生成的进行合并。
2. 在对比接口的场景下，left和right 的payloads数量必须一直
3. json中定义的rules可以是任意有效的groovy表达式/Python表达式 返回bool值（Python表达式需要将断言结果存放在 r1esult）
4. 真实请求的数据将会自动缓存在.diffcache中，优先从缓存获取。方便调试
（可能在某些场景下，对于基准数据可以缓存，而对于待验证数据每次都请求真实数据，这样效率更高）


### TODO
- 支持project 
- 支持grpc
- cache 支持db