# 杩佺Щ鎸囧崡锛氬钩鍙版湇鍔¤皟鐢?鈫?Jar 鍐呭祵

## 閫傜敤鍦烘櫙

- 鍘熸潵閫氳繃 HTTP 璋?`agent-platform` 缃戝叧/鏈嶅姟
- 鐜板湪甯屾湜鍦ㄤ笟鍔¤繘绋嬪唴鐩存帴璋冪敤鑳藉姏锛屽噺灏戦儴缃插鏉傚害涓庣綉缁滆烦鏁?
## 杩佺Щ姝ラ

1. 寮曞叆 `cqagent-java-sdk` 鎴?`cqagent-spring-boot-starter`
2. 灏嗗師 `model-service` 绔偣閰嶇疆杩佺Щ鍒?`AgentClientBuilder` 鎴?`agent.product.*`
3. 灏嗕笟鍔¤皟鐢ㄥ叆鍙ｆ敼涓?`AgentClient#chat` / `AgentClient#stream`
4. 灏嗚嚜瀹氫箟宸ュ叿閫昏緫杩佺Щ涓?`ProductTool`
5. 鐏板害瀵规瘮锛氬悓璇锋眰瀵规瘮 HTTP 妯″紡涓?Jar 妯″紡杈撳嚭

## 鍏煎鎬ц鏄?
- 璇锋眰/鍝嶅簲 DTO 娌跨敤 `agent-api` / `model-api`锛屽噺灏戜笂灞傛敼閫犻噺
- 璺敱绛栫暐缁х画鏀寔闈欐€佽矾鐢便€佷富澶囥€佸姞鏉冦€佸仴搴锋劅鐭?- 宸ュ叿璋冪敤浠庘€滄枃鏈В鏋愬崗璁€濊縼绉讳负 LangChain4j 鍘熺敓缁撴瀯鍖栧伐鍏峰崗璁紙鎺ㄨ崘鍚屾杩佺Щ鑷畾涔夊伐鍏峰埌 `parameterSpecs + execute(Map)`锛?
