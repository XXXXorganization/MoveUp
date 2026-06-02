// src/utils/llm.ts
import axios from 'axios';

interface Message {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

interface LLMResponse {
  content: string;
  usage: { promptTokens: number; completionTokens: number };
}

export class LLMClient {
  private apiKey: string;
  private baseUrl: string;
  private model: string;

  constructor() {
    this.apiKey = process.env.DEEPSEEK_API_KEY || '';
    this.baseUrl = process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com';
    this.model = 'deepseek-chat';

    if (!this.apiKey) throw new Error('DEEPSEEK_API_KEY 未配置');
  }

  async chat(messages: Message[], temperature = 0.7): Promise<LLMResponse> {
    const response = await axios.post(
      `${this.baseUrl}/chat/completions`,
      { model: this.model, messages, temperature, max_tokens: 512 },
      {
        headers: {
          Authorization: `Bearer ${this.apiKey}`,
          'Content-Type': 'application/json',
        },
        timeout: 30000,
      },
    );

    const choice = response.data.choices[0];
    const usage = response.data.usage;

    return {
      content: choice.message.content,
      usage: {
        promptTokens: usage.prompt_tokens,
        completionTokens: usage.completion_tokens,
      },
    };
  }
}

// 单例，避免重复实例化
let client: LLMClient | null = null;

export function getLLMClient(): LLMClient {
  if (!client) client = new LLMClient();
  return client;
}

// Qwen 聊天客户端（通义千问 via SiliconFlow）
let qwenClient: LLMClient | null = null;

export function getQwenClient(): LLMClient {
  if (!qwenClient) {
    const apiKey = process.env.QWEN_API_KEY || '';
    const baseUrl = process.env.QWEN_BASE_URL || 'https://api.siliconflow.cn';
    const model = process.env.QWEN_MODEL || 'Qwen/Qwen2.5-7B-Instruct';
    qwenClient = new LLMClient();
    (qwenClient as any).apiKey = apiKey;
    (qwenClient as any).baseUrl = baseUrl;
    (qwenClient as any).model = model;
  }
  return qwenClient;
}

// 聊天消息类型（兼容不同格式）
export async function aiChat(messages: Message[]): Promise<string> {
  try {
    // 优先用 DeepSeek（容器内直连不需要代理）
    const client = getLLMClient();
    const response = await client.chat(messages, 0.8);
    return response.content;
  } catch (e1) {
    // DeepSeek 失败时回退到通义千问
    try {
      const qwen = getQwenClient();
      const response = await qwen.chat(messages, 0.8);
      return response.content;
    } catch (e2) {
      throw e1; // 两个都失败了
    }
  }
}
