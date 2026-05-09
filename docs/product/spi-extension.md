# SPI 鎵╁睍璇存槑

## 1. 妯″瀷鎵╁睍

瀹炵幇鎺ュ彛锛?
- `io.github.alcq77.cqagent.spi.model.ProductModelProvider`

瑕佹眰锛?
- `providerCode()` 鍏ㄥ眬鍞竴
- `createChatLanguageModel(...)` 杩斿洖 LangChain4j `ChatLanguageModel`
- 鍙€夊疄鐜?`createStreamingChatLanguageModel(...)` 鎻愪緵娴佸紡妯″瀷

娉ㄥ唽鏂瑰紡锛?
- SDK锛歚AgentClientBuilder.modelProvider(...)`
- SDK锛氭垨閫氳繃 Java SPI锛坄META-INF/services/...ProductModelProvider`锛夎嚜鍔ㄥ彂鐜?- Starter锛氬０鏄庝负 Spring Bean 鑷姩鍙戠幇

## 2. 宸ュ叿鎵╁睍

瀹炵幇鎺ュ彛锛?
- `io.github.alcq77.cqagent.spi.tool.ProductTool`

鍏抽敭鏂规硶锛?
- `name()` 宸ュ叿鏍囪瘑
- `description()` 宸ュ叿鎻忚堪锛堢敤浜庢ā鍨嬮€夋嫨锛?- `parameterSpecs()` 鍙傛暟 schema 瀹氫箟锛坘ey=鍙傛暟鍚嶏級
- `execute(Map<String, Object> arguments)` 缁撴瀯鍖栨墽琛屽叆鍙?
鍏煎璇存槑锛?
- 鏃ф帴鍙?`execute(String)` 浠嶄繚鐣欙紝榛樿鐢?`execute(Map)` 閫傞厤璋冪敤
- `supports(String)` 涓哄吋瀹规帴鍙ｏ紝LangChain4j 涓婚摼涓嶄緷璧栬鏂规硶鍋氬伐鍏烽€夋嫨

娉ㄥ唽鏂瑰紡锛?
- SDK锛歚AgentClientBuilder.tool(...)`
- SDK锛氭垨閫氳繃 Java SPI锛坄META-INF/services/...ProductTool`锛夎嚜鍔ㄥ彂鐜?- Starter锛氬０鏄庝负 Spring Bean 鑷姩鍙戠幇

## 3. 浼氳瘽瀛樺偍鎵╁睍

瀹炵幇鎺ュ彛锛?
- `io.github.alcq77.cqagent.spi.session.ProductSessionStore`

榛樿瀹炵幇锛?
- `InMemoryProductSessionStore`

