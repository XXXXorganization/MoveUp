// src/modules/ai/service.ts
import { getLLMClient } from '../../utils/llm';
import { SportSummaryInput, SportSummaryResponse, HistorySummaryInput } from './types';

const SYSTEM_PROMPT = `你是一位专业的运动健康教练，擅长分析跑步数据并给出鼓励性的总结和实用建议。
请用简洁、友好的中文回复，总结控制在100字以内，建议3条以内，每条建议不超过30字。
返回格式必须是合法的 JSON：{"summary": "...", "suggestions": ["...", "..."]}`;

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return m > 0 ? `${m}分${s}秒` : `${s}秒`;
}

function formatPace(secPerKm: number): string {
  const m = Math.floor(secPerKm / 60);
  const s = secPerKm % 60;
  return `${m}'${String(s).padStart(2, '0')}"`;
}

export class AIService {
  async summarizeSportRecord(input: SportSummaryInput): Promise<SportSummaryResponse> {
    const distanceKm = (input.distance / 1000).toFixed(2);
    const duration = formatDuration(input.duration);
    const pace = input.avgPace ? formatPace(input.avgPace) : '未知';

    const userMessage = `请分析以下跑步数据并给出总结和建议：
- 距离：${distanceKm} 公里
- 时长：${duration}
- 消耗卡路里：${input.calories} 千卡
- 平均配速：${pace}/公里
${input.avgHeartRate ? `- 平均心率：${input.avgHeartRate} bpm` : ''}
${input.maxHeartRate ? `- 最大心率：${input.maxHeartRate} bpm` : ''}
${input.date ? `- 运动日期：${input.date}` : ''}`;

    return this.callAndParse(userMessage);
  }

  async summarizeHistory(input: HistorySummaryInput): Promise<SportSummaryResponse> {
    const totalDistance = input.records.reduce((s, r) => s + r.distance, 0);
    const totalDuration = input.records.reduce((s, r) => s + r.duration, 0);
    const totalCalories = input.records.reduce((s, r) => s + r.calories, 0);
    const avgPaces = input.records.filter(r => r.avgPace).map(r => r.avgPace!);
    const avgPace = avgPaces.length > 0 ? avgPaces.reduce((a, b) => a + b, 0) / avgPaces.length : null;

    const userMessage = `请分析用户${input.periodLabel}的跑步汇总数据并给出总结和建议：
- 跑步次数：${input.records.length} 次
- 总距离：${(totalDistance / 1000).toFixed(2)} 公里
- 总时长：${formatDuration(totalDuration)}
- 总消耗卡路里：${totalCalories} 千卡
${avgPace ? `- 平均配速：${formatPace(Math.round(avgPace))}/公里` : ''}`;

    return this.callAndParse(userMessage);
  }

  private async callAndParse(userMessage: string): Promise<SportSummaryResponse> {
    const client = getLLMClient();
    const result = await client.chat([
      { role: 'system', content: SYSTEM_PROMPT },
      { role: 'user', content: userMessage },
    ]);

    // 提取 JSON（兼容模型在 markdown 代码块中返回的情况）
    const jsonMatch = result.content.match(/\{[\s\S]*\}/);
    if (!jsonMatch) throw new Error('AI 返回格式异常');

    const parsed = JSON.parse(jsonMatch[0]);
    return {
      summary: parsed.summary ?? '',
      suggestions: Array.isArray(parsed.suggestions) ? parsed.suggestions : [],
      generatedAt: new Date().toISOString(),
    };
  }
}
