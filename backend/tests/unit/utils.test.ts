// tests/unit/utils.test.ts
import axios from 'axios';
import { AppError } from '../../src/utils/errors';
import { LLMClient, getLLMClient } from '../../src/utils/llm';

jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('AppError', () => {
  it('应使用默认状态码 500', () => {
    const err = new AppError('服务器错误');
    expect(err.code).toBe(500);
    expect(err.message).toBe('服务器错误');
    expect(err.name).toBe('AppError');
  });

  it('应使用自定义状态码', () => {
    const err = new AppError('未找到', 404);
    expect(err.code).toBe(404);
    expect(err.message).toBe('未找到');
  });

  it('应是 Error 的实例', () => {
    const err = new AppError('测试');
    expect(err).toBeInstanceOf(Error);
  });
});

describe('LLMClient', () => {
  const ORIGINAL_API_KEY = process.env.DEEPSEEK_API_KEY;

  beforeEach(() => {
    process.env.DEEPSEEK_API_KEY = 'test-key';
    mockedAxios.post.mockReset();
  });

  afterAll(() => {
    process.env.DEEPSEEK_API_KEY = ORIGINAL_API_KEY;
  });

  it('API Key 未配置时应抛出错误', () => {
    delete process.env.DEEPSEEK_API_KEY;
    expect(() => new LLMClient()).toThrow('DEEPSEEK_API_KEY 未配置');
  });

  it('应正确调用 DeepSeek API 并返回解析结果', async () => {
    mockedAxios.post.mockResolvedValue({
      data: {
        choices: [{ message: { content: '本周跑量不错，继续加油！' } }],
        usage: { prompt_tokens: 100, completion_tokens: 50 },
      },
    });

    const client = new LLMClient();
    const messages = [{ role: 'user' as const, content: '总结我的运动数据' }];
    const result = await client.chat(messages);

    expect(result.content).toBe('本周跑量不错，继续加油！');
    expect(result.usage.promptTokens).toBe(100);
    expect(result.usage.completionTokens).toBe(50);
  });

  it('应以正确的请求体调用 API', async () => {
    mockedAxios.post.mockResolvedValue({
      data: {
        choices: [{ message: { content: '好' } }],
        usage: { prompt_tokens: 10, completion_tokens: 5 },
      },
    });

    const client = new LLMClient();
    const messages = [{ role: 'user' as const, content: '测试消息' }];
    await client.chat(messages, 0.5);

    expect(mockedAxios.post).toHaveBeenCalledWith(
      expect.stringContaining('/chat/completions'),
      expect.objectContaining({
        model: 'deepseek-chat',
        messages,
        temperature: 0.5,
        max_tokens: 512,
      }),
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer test-key' }),
        timeout: 30000,
      }),
    );
  });

  it('API 调用失败时应抛出错误', async () => {
    mockedAxios.post.mockRejectedValue(new Error('网络错误'));
    const client = new LLMClient();
    await expect(client.chat([{ role: 'user', content: '测试' }])).rejects.toThrow('网络错误');
  });
});

describe('getLLMClient', () => {
  beforeEach(() => {
    process.env.DEEPSEEK_API_KEY = 'test-key';
    // 重置模块内的单例
    jest.resetModules();
  });

  it('多次调用应返回同一实例', () => {
    const { getLLMClient: getClient } = require('../../src/utils/llm');
    const a = getClient();
    const b = getClient();
    expect(a).toBe(b);
  });
});
