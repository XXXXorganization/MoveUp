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
