// src/modules/ai/types.ts

export interface SportSummaryInput {
  distance: number;       // 米
  duration: number;       // 秒
  calories: number;       // 千卡
  avgPace?: number;       // 秒/公里
  avgHeartRate?: number;  // bpm
  maxHeartRate?: number;  // bpm
  date?: string;          // 运动日期
}

export interface SportSummaryResponse {
  summary: string;        // AI 生成的自然语言总结
  suggestions: string[];  // 建议列表
  generatedAt: string;
}

export interface HistorySummaryInput {
  records: SportSummaryInput[];
  periodLabel: string;    // 如 "本周" / "本月"
}
