# Examples

- `java-sdk-demo`锛氱函 Java SDK 绀轰緥
- `spring-starter-demo`锛歋pring Boot Starter 绀轰緥

## 鍏堝畨瑁呮湰鍦颁緷璧?
```bash
cd ../cq-agent-parent/cq-agent
mvn -q clean install -DskipTests
```

## 杩愯 Java SDK 绀轰緥

```bash
cd ../examples/java-sdk-demo
mvn -q compile exec:java -Dexec.mainClass=demo.Main
```

## 杩愯 Starter 绀轰緥

```bash
cd ../examples/spring-starter-demo
mvn spring-boot:run
```

鍚姩鍚庡彲鍏堣闂細

- [http://localhost:18080/onboarding](http://localhost:18080/onboarding)
- [http://localhost:18080/demo/chat?q=浣犲ソ](http://localhost:18080/demo/chat?q=浣犲ソ)

