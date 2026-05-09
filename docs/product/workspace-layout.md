# Workspace 鐩綍瑙勮寖

寤鸿鍦ㄦ帴鍏ラ」鐩腑绾﹀畾宸ヤ綔鐩綍锛堥粯璁?`./workspace`锛夈€?
## 寤鸿缁撴瀯

```text
workspace/
鈹溾攢鈹€ AGENT.md
鈹溾攢鈹€ INFO.md
鈹溾攢鈹€ context/
鈹溾攢鈹€ skills/
鈹溾攢鈹€ plugins/
鈹斺攢鈹€ tasks/
```

## 璇存槑

- `AGENT.md`锛氱郴缁熸彁绀鸿瘝妯℃澘
- `INFO.md`锛氳繍琛岀幆澧冧笌涓氬姟涓婁笅鏂?- `context/`锛氶暱鏈熶笂涓嬫枃涓庤蹇嗘枃浠?- `skills/`锛氭妧鑳芥枃妗ｏ紙鍙槧灏勫埌鑷畾涔夊伐鍏峰姞杞斤級
- `plugins/`锛氭彃浠?Jar 鐩綍锛堟寜閰嶇疆鑷姩鎵弿锛?- `tasks/`锛氫换鍔¤褰曚笌鐘舵€佹枃浠?
## skills 鍐呭寤鸿

鍙湪鎶€鑳芥枃浠朵腑澧炲姞鍏冩暟鎹互浼樺寲鍛戒腑璐ㄩ噺锛?
```text
priority: 10
keywords: invoice,billing,璐﹀崟,鍙戠エ
```

- `priority`锛氭暟鍊艰秺澶т紭鍏堢骇瓒婇珮
- `keywords`锛氶€楀彿鍒嗛殧鍏抽敭瀛楋紝鍛戒腑鍚庝紭鍏堥€夋嫨璇ユ妧鑳?
## 閰嶇疆椤?
Starter:

- `agent.product.workspace=./workspace`
- `agent.product.plugin.enabled=true`
- `agent.product.plugin.directory=./workspace/plugins`
- `agent.product.skills.enabled=true`
- `agent.product.skills.directory=./workspace/skills`

SDK:

- 鍙敱涓氬姟鏂硅嚜琛岀害瀹氬苟浼犲叆宸ュ叿/瀛樺偍瀹炵幇浣跨敤


