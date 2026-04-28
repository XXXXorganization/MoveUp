// tests/unit/ai.service.test.ts
import { AIService } from '../../src/modules/ai/service';
import { getLLMClient } from '../../src/utils/llm';

jest.mock('../../src/utils/llm', () => ({
  getLLMClient: jest.fn(),
}));

const mockChat = jest.fn();
(getLLMClient as jest.Mock).mockReturnValue({ chat: mockChat });

const svc = () => new AIService();

function mockLLMResponse(content: string) {
  mockChat.mockResolvedValue({ content, usage: { promptTokens: 50, completionTokens: 30 } });
}

beforeEach(() => jest.clearAllMocks());

// ==================== summarizeSportRecord ====================

describe('summarizeSportRecord', () => {
  const validResponse = JSON.stringify({ summary: '今天跑得不错！', suggestions: ['注意补水', '保持配速'] });

  it('应正确格式化并调用 LLM', async () => {
    mockLLMResponse(validResponse);
    await svc().summarizeSportRecord({ distance: 5000, duration: 1800, calories: 300, avgPace: 360 });
    expect(mockChat).toHaveBeenCalledWith(
      expect.arrayContaining([
        expect.objectContaining({ role: 'system' }),
        expect.objectContaining({ role: 'user', content: expect.stringContaining('5.00 公里') }),
      ]),
    );
  });

  it('应解析 LLM 返回的 JSON', async () => {
    mockLLMResponse(validResponse);
    const result = await svc().summarizeSportRecord({ distance: 5000, duration: 1800, calories: 300, avgPace: 360 });
    expect(result.summary).toBe('今天跑得不错！');
    expect(result.suggestions).toEqual(['注意补水', '保持配速']);
    expect(result.generatedAt).toBeDefined();
  });

  it('应处理 Markdown 代码块中的 JSON', async () => {
    mockLLMResponse('```json\n{"summary": "棒棒的", "suggestions": ["休息好"]}\n```');
    const result = await svc().summarizeSportRecord({ distance: 3000, duration: 900, calories: 150, avgPace: 300 });
    expect(result.summary).toBe('棒棒的');
  });

  it('有心率数据时消息中应包含心率信息', async () => {
    mockLLMResponse(validResponse);
    await svc().summarizeSportRecord({ distance: 5000, duration: 1800, calories: 300, avgPace: 360, avgHeartRate: 155, maxHeartRate: 180 });
    const call = mockChat.mock.calls[0][0] as any[];
    const userMsg = call.find(m => m.role === 'user').content;
    expect(userMsg).toContain('155 bpm');
    expect(userMsg).toContain('180 bpm');
  });

  it('无 avgPace 时配速显示为未知', async () => {
    mockLLMResponse(validResponse);
    await svc().summarizeSportRecord({ distance: 5000, duration: 1800, calories: 300 });
    const call = mockChat.mock.calls[0][0] as any[];
    const userMsg = call.find(m => m.role === 'user').content;
    expect(userMsg).toContain('未知');
  });

  it('LLM 返回格式异常时应抛出错误', async () => {
    mockLLMResponse('这不是合法的JSON');
    await expect(svc().summarizeSportRecord({ distance: 5000, duration: 1800, calories: 300 }))
      .rejects.toThrow('AI 返回格式异常');
  });
});

// ==================== summarizeHistory ====================

describe('summarizeHistory', () => {
  const records = [
    { distance: 5000, duration: 1800, calories: 300, avgPace: 360 },
    { distance: 8000, duration: 2700, calories: 480, avgPace: 337 },
    { distance: 10000, duration: 3600, calories: 600, avgPace: 360 },
  ];
  const validResponse = JSON.stringify({ summary: '本周表现出色！', suggestions: ['继续保持'] });

  it('应聚合多条记录并调用 LLM', async () => {
    mockLLMResponse(validResponse);
    await svc().summarizeHistory({ periodLabel: '本周', records });
    const call = mockChat.mock.calls[0][0] as any[];
    const userMsg = call.find(m => m.role === 'user').content;
    expect(userMsg).toContain('3 次');
    expect(userMsg).toContain('23.00 公里'); // (5+8+10)km
  });

  it('无配速数据时消息中不应包含平均配速', async () => {
    mockLLMResponse(validResponse);
    const recordsNoPace = records.map(r => ({ ...r, avgPace: undefined }));
    await svc().summarizeHistory({ periodLabel: '本周', records: recordsNoPace });
    const call = mockChat.mock.calls[0][0] as any[];
    const userMsg = call.find(m => m.role === 'user').content;
    expect(userMsg).not.toContain('平均配速');
  });

  it('应正确解析响应', async () => {
    mockLLMResponse(validResponse);
    const result = await svc().summarizeHistory({ periodLabel: '本周', records });
    expect(result.summary).toBe('本周表现出色！');
    expect(result.suggestions).toHaveLength(1);
  });

  it('suggestions 非数组时应返回空数组', async () => {
    mockLLMResponse(JSON.stringify({ summary: '很好', suggestions: null }));
    const result = await svc().summarizeHistory({ periodLabel: '本月', records: [records[0]] });
    expect(result.suggestions).toEqual([]);
  });
});
