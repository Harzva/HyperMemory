# Open Source References

Snapshot checked on 2026-05-12. Stars change over time, so use the links as the source of truth.

| Repo | Why it is relevant | What to borrow | Watchouts |
| --- | --- | --- | --- |
| [zhayujie/CowAgent](https://github.com/zhayujie/CowAgent) | Multi-platform AI assistant for WeChat, Feishu, DingTalk, WeCom, QQ, and web. | Channel adapters, skill execution, long-term memory concepts. | Large feature surface; avoid copying architecture wholesale. |
| [BytePioneer-AI/openclaw-china](https://github.com/BytePioneer-AI/openclaw-china) | China IM integration plugin for OpenClaw. | Plugin-style channel abstraction for Feishu, DingTalk, WeChat, and WeCom. | Tied to OpenClaw extension points. |
| [ConnectAI-E/feishu-openai](https://github.com/ConnectAI-E/feishu-openai) | Feishu plus OpenAI integration with voice, image, and document flows. | Feishu event flow, card messages, LLM interaction patterns. | Feishu-specific; do not use as the only Bot abstraction. |
| [ymlluo/group-robot](https://github.com/ymlluo/group-robot) | Lightweight multi-platform group robot sender for DingTalk, WeCom, and Feishu. | Unified outbound message builders. | Sender-oriented; not enough for inbound event routing. |
| [RockChinQ/LangBot](https://github.com/RockChinQ/LangBot) | Multi-platform agentic IM Bot platform. | Plugin model, channel lifecycle, model-provider abstraction. | Large platform; extract patterns, not dependencies. |
| [starryeve/webhook-chatbot](https://github.com/starryeve/webhook-chatbot) | Node.js webhook robot wrapper for WeCom and DingTalk. | Small webhook adapter structure and message send examples. | Low activity; use only as a minimal reference. |
| [openakita/openakita](https://github.com/openakita/openakita) | AI assistant framework with skills and agent architecture. | Skill organization and assistant composition ideas. | Not a direct campus QA runtime. |
| [wechaty/wechaty](https://github.com/wechaty/wechaty) | Mature conversational automation SDK. | WeChat-style session abstraction and adapter model. | Personal WeChat automation may be unsuitable for production compliance. |
| [larksuite/oapi-sdk-java](https://github.com/larksuite/oapi-sdk-java) | Official Feishu/Lark OpenAPI Java SDK. | Official client patterns for Feishu message send and event APIs. | SDK only; still need gateway, auth, and operations design. |
| [open-dingtalk/dingtalk-stream-sdk-java](https://github.com/open-dingtalk/dingtalk-stream-sdk-java) | DingTalk Stream Mode Java SDK. | Production inbound event connection option for DingTalk. | Requires DingTalk-specific stream service lifecycle. |

## Practical Reference Strategy

1. Use official SDKs for provider-specific auth and outbound APIs.
2. Keep this repository's normalized Bot contract as the internal boundary.
3. Implement provider parsing in a small edge adapter, not inside every QA service.
4. Compare open-source projects for channel lifecycle, retry, and plugin structure before adopting dependencies.
