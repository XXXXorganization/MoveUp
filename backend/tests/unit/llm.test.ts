// tests/unit/llm.test.ts
import axios from 'axios';
import { LLMClient, getLLMClient, getQwenClient, aiChat } from '../../src/utils/llm';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('LLMClient', () => {
  let client: LLMClient;

  beforeEach(() => {
    jest.clearAllMocks();
    process.env.DEEPSEEK_API_KEY = 'test-key';
    process.env.DEEPSEEK_BASE_URL = 'https://test.api.com';
    client = new LLMClient();
  });

  it('should call chat API and return response', async () => {
    const mockResponse = {
      data: {
        choices: [{ message: { content: 'Hello from AI' } }],
        usage: { prompt_tokens: 10, completion_tokens: 5 },
      },
    };
    mockedAxios.post.mockResolvedValue(mockResponse);

    const result = await client.chat([{ role: 'user', content: 'hi' }], 0.7);

    expect(mockedAxios.post).toHaveBeenCalledWith(
      'https://test.api.com/chat/completions',
      expect.objectContaining({ model: 'deepseek-chat', temperature: 0.7 }),
      expect.any(Object),
    );
    expect(result.content).toBe('Hello from AI');
    expect(result.usage.promptTokens).toBe(10);
    expect(result.usage.completionTokens).toBe(5);
  });

  it('should throw if no API key is set', () => {
    delete process.env.DEEPSEEK_API_KEY;
    process.env.DEEPSEEK_BASE_URL = undefined;
    expect(() => new LLMClient()).toThrow();
    process.env.DEEPSEEK_API_KEY = 'test-key';
  });
});

describe('getLLMClient', () => {
  it('should return singleton instance', () => {
    process.env.DEEPSEEK_API_KEY = 'k';
    const c1 = getLLMClient();
    const c2 = getLLMClient();
    expect(c1).toBe(c2);
  });
});

describe('getQwenClient', () => {
  it('should return singleton with Qwen config', () => {
    process.env.QWEN_API_KEY = 'qwen-key';
    process.env.QWEN_BASE_URL = 'https://qwen.api.com';
    process.env.QWEN_MODEL = 'Qwen/Qwen2.5-7B';
    const c1 = getQwenClient();
    const c2 = getQwenClient();
    expect(c1).toBe(c2);
  });
});

describe('aiChat', () => {
  it('should call LLM and return content', async () => {
    process.env.DEEPSEEK_API_KEY = 'k';
    const mockResponse = {
      data: {
        choices: [{ message: { content: 'AI reply' } }],
        usage: { prompt_tokens: 5, completion_tokens: 3 },
      },
    };
    mockedAxios.post.mockResolvedValue(mockResponse);

    const result = await aiChat([{ role: 'user', content: 'hello' }]);
    expect(result).toBe('AI reply');
  });
});
