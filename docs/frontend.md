## frontend.md - Moveup 前端开发文档

# Moveup 前端开发文档

## 📱 项目概述
Moveup 前端是基于 **React Native** 框架开发的跨平台移动应用，支持Android平台。应用致力于为跑步爱好者提供流畅、直观的运动记录体验和积极的社交互动环境。

## 🏗️ 技术架构

### 核心技术栈
```
┌─────────────────────────────────────┐
│          UI 层 (组件库)              │
│  React Native + React Navigation     │
├─────────────────────────────────────┤
│        状态管理层                     │
│  Redux Toolkit + Redux Saga          │
├─────────────────────────────────────┤
│        数据处理层                     │
│  Axios + React Query + AsyncStorage  │
├─────────────────────────────────────┤
│        功能模块层                     │
│  地图定位 | 运动追踪 | 社交分享 | 支付  │
└─────────────────────────────────────┘
```

### 主要依赖库
| 类别 | 库名称 | 用途 |
|------|--------|------|
| **路由导航** | React Navigation 6.x | 页面路由和导航 |
| **状态管理** | Redux Toolkit + Redux Saga | 全局状态管理 |
| **网络请求** | Axios + React Query | API请求和数据缓存 |
| **UI组件库** | React Native Elements | 基础UI组件 |
| **地图定位** | React Native Maps + 高德SDK | 路线展示和定位 |
| **图表展示** | Victory Native | 运动数据可视化 |
| **本地存储** | AsyncStorage + MMKV | 数据持久化 |
| **动画效果** | React Native Reanimated | 流畅的交互动画 |
| **权限管理** | React Native Permissions | 系统权限申请 |
| **推送通知** | React Native Push Notification | 消息推送 |
| **热更新** | CodePush | 动态更新应用 |

## 📁 项目结构

```
moveup-frontend/
├── src/
│   ├── api/                 # API接口层
│   │   ├── client.js        # Axios配置
│   │   ├── auth.js          # 认证相关API
│   │   ├── runs.js          # 运动相关API
│   │   ├── social.js        # 社交相关API
│   │   └── user.js          # 用户相关API
│   │
│   ├── components/          # 可复用组件
│   │   ├── common/          # 通用组件
│   │   │   ├── Button/
│   │   │   ├── Input/
│   │   │   ├── Header/
│   │   │   └── Loading/
│   │   ├── runs/            # 运动相关组件
│   │   │   ├── RunMap/
│   │   │   ├── PaceChart/
│   │   │   └── RunSummary/
│   │   └── social/          # 社交相关组件
│   │       ├── FeedCard/
│   │       ├── CommentList/
│   │       └── UserAvatar/
│   │
│   ├── screens/             # 页面
│   │   ├── auth/            # 认证相关
│   │   │   ├── LoginScreen.js
│   │   │   ├── RegisterScreen.js
│   │   │   └── ForgotPasswordScreen.js
│   │   ├── home/            # 首页
│   │   │   ├── HomeScreen.js
│   │   │   ├── RunScreen.js        # 实时跑步页面
│   │   │   └── RunSummaryScreen.js
│   │   ├── profile/         # 个人中心
│   │   │   ├── ProfileScreen.js
│   │   │   ├── SettingsScreen.js
│   │   │   └── StatisticsScreen.js
│   │   ├── social/          # 社交
│   │   │   ├── FeedScreen.js
│   │   │   ├── FriendsScreen.js
│   │   │   └── LeaderboardScreen.js
│   │   └── challenges/      # 挑战
│   │       ├── ChallengesScreen.js
│   │       └── ChallengeDetailScreen.js
│   │
│   ├── navigation/          # 导航配置
│   │   ├── AppNavigator.js
│   │   ├── AuthNavigator.js
│   │   ├── MainTabNavigator.js
│   │   └── navigationRef.js
│   │
│   ├── store/               # Redux状态管理
│   │   ├── index.js
│   │   ├── rootReducer.js
│   │   ├── rootSaga.js
│   │   ├── slices/
│   │   │   ├── userSlice.js
│   │   │   ├── runSlice.js
│   │   │   └── socialSlice.js
│   │   └── sagas/
│   │       ├── userSaga.js
│   │       └── runSaga.js
│   │
│   ├── utils/               # 工具函数
│   │   ├── permission.js    # 权限处理
│   │   ├── location.js      # 定位工具
│   │   ├── calculator.js    # 运动计算（配速、卡路里）
│   │   ├── formatter.js     # 数据格式化
│   │   └── storage.js       # 本地存储封装
│   │
│   ├── hooks/               # 自定义Hooks
│   │   ├── useLocation.js   # 定位Hook
│   │   ├── useRunTimer.js   # 跑步计时
│   │   └── usePermission.js # 权限管理
│   │
│   ├── constants/           # 常量配置
│   │   ├── colors.js        # 颜色主题
│   │   ├── fonts.js         # 字体配置
│   │   ├── config.js        # 环境配置
│   │   └── strings.js       # 多语言文本
│   │
│   └── assets/              # 静态资源
│       ├── images/
│       ├── icons/
│       └── animations/
│
├── ios/                     # iOS原生代码
├── android/                 # Android原生代码
├── index.js                  # 入口文件
├── App.js                    # 根组件
├── package.json
└── README.md
```

## 🎨 UI/UX 设计规范

### 主题配色
```javascript
// constants/colors.js
export const colors = {
  primary: '#4A90E2',     // 主色调 - 活力蓝
  secondary: '#50E3C2',   // 辅助色 - 清新绿
  accent: '#F5A623',      // 强调色 - 活力橙
  background: '#FFFFFF',  // 背景色
  surface: '#F8F9FA',     // 表面色
  error: '#E74C3C',       // 错误提示
  success: '#27AE60',     // 成功提示
  warning: '#F39C12',     // 警告提示
  text: {
    primary: '#2C3E50',   // 主要文字
    secondary: '#7F8C8D', // 次要文字
    disabled: '#BDC3C7',  // 禁用文字
    inverse: '#FFFFFF'    // 反色文字
  },
  border: '#E0E0E0',      // 边框颜色
  gradient: {
    start: '#4A90E2',
    end: '#9013FE'
  }
};
```

### 字体系统
```javascript
// constants/fonts.js
export const typography = {
  h1: {
    fontSize: 28,
    fontWeight: 'bold',
    lineHeight: 34
  },
  h2: {
    fontSize: 24,
    fontWeight: 'bold',
    lineHeight: 30
  },
  h3: {
    fontSize: 20,
    fontWeight: '600',
    lineHeight: 26
  },
  body1: {
    fontSize: 16,
    fontWeight: 'normal',
    lineHeight: 22
  },
  body2: {
    fontSize: 14,
    fontWeight: 'normal',
    lineHeight: 20
  },
  caption: {
    fontSize: 12,
    fontWeight: 'normal',
    lineHeight: 16
  }
};
```

### 间距规范
- 基础单位：4px
- 间距：4px, 8px, 12px, 16px, 20px, 24px, 32px, 40px
- 圆角：4px, 8px, 12px, 16px, 24px (圆形)

## 📱 核心功能模块实现

### 1. 实时跑步追踪模块

```javascript
// screens/home/RunScreen.js 核心逻辑
const RunScreen = () => {
  const [runState, setRunState] = useState({
    distance: 0,
    duration: 0,
    pace: '0\'00"',
    calories: 0,
    heartRate: 0,
    route: []
  });
  
  const { startTracking, stopTracking, location } = useLocation();
  const { time, startTimer, pauseTimer, resetTimer } = useRunTimer();
  
  // 每秒更新运动数据
  useEffect(() => {
    if (location) {
      // 计算距离、配速、卡路里
      const newDistance = calculateDistance([...route, location]);
      const newPace = calculatePace(newDistance, time);
      const newCalories = calculateCalories(newDistance, userWeight);
      
      setRunState(prev => ({
        ...prev,
        distance: newDistance,
        pace: newPace,
        calories: newCalories,
        route: [...prev.route, location]
      }));
    }
  }, [location, time]);
  
  // 语音播报
  useVoiceAnnouncement({
    distance: runState.distance,
    pace: runState.pace,
    frequency: 'every_km'
  });
  
  return (
    <View style={styles.container}>
      <RunMap route={runState.route} />
      <RunStats data={runState} />
      <ControlButtons 
        onStart={startTracking}
        onPause={pauseTimer}
        onStop={handleStopRun}
      />
    </View>
  );
};
```

### 2. 数据可视化组件

```javascript
// components/runs/PaceChart.js
import { VictoryLine, VictoryChart, VictoryAxis } from 'victory-native';

const PaceChart = ({ data }) => {
  return (
    <VictoryChart>
      <VictoryLine
        data={data}
        x="kilometer"
        y="pace"
        style={{
          data: { stroke: colors.primary, strokeWidth: 3 }
        }}
        animate={{ duration: 500 }}
      />
      <VictoryAxis
        label="公里"
        style={{ tickLabels: { fontSize: 12 } }}
      />
      <VictoryAxis
        dependentAxis
        label="配速 (min/km)"
        style={{ tickLabels: { fontSize: 12 } }}
      />
    </VictoryChart>
  );
};
```

### 3. 状态管理示例 (Redux Toolkit)

```javascript
// store/slices/runSlice.js
import { createSlice } from '@reduxjs/toolkit';

const runSlice = createSlice({
  name: 'run',
  initialState: {
    currentRun: null,
    history: [],
    statistics: {
      weekly: null,
      monthly: null,
      yearly: null
    },
    loading: false,
    error: null
  },
  reducers: {
    setCurrentRun: (state, action) => {
      state.currentRun = action.payload;
    },
    addRunToHistory: (state, action) => {
      state.history.unshift(action.payload);
    },
    updateRunData: (state, action) => {
      if (state.currentRun) {
        Object.assign(state.currentRun, action.payload);
      }
    },
    setStatistics: (state, action) => {
      state.statistics = action.payload;
    }
  }
});

export const { 
  setCurrentRun, 
  addRunToHistory, 
  updateRunData,
  setStatistics 
} = runSlice.actions;
export default runSlice.reducer;
```

### 4. 网络请求封装

```javascript
// api/client.js
import axios from 'axios';
import { getToken, refreshToken } from '../utils/auth';

const client = axios.create({
  baseURL: 'https://api.moveup.com/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  }
});

// 请求拦截器
client.interceptors.request.use(
  async config => {
    const token = await getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// 响应拦截器
client.interceptors.response.use(
  response => response.data,
  async error => {
    const originalRequest = error.config;
    
    // Token过期处理
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const newToken = await refreshToken();
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return client(originalRequest);
      } catch (refreshError) {
        // 跳转到登录页
        navigationRef.navigate('Login');
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default client;
```

## 🔧 开发环境配置

### 1. 环境变量
```bash
# .env.development
API_URL=https://dev-api.moveup.com/v1
MAP_KEY=your_dev_map_key
WEBSOCKET_URL=wss://dev-api.moveup.com/ws

# .env.production
API_URL=https://api.moveup.com/v1
MAP_KEY=your_prod_map_key
WEBSOCKET_URL=wss://api.moveup.com/ws
```

### 2. 调试配置
```json
// .vscode/launch.json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Debug iOS",
      "type": "reactnative",
      "request": "launch",
      "platform": "ios",
      "sourceMaps": true
    },
    {
      "name": "Debug Android",
      "type": "reactnative",
      "request": "launch",
      "platform": "android",
      "sourceMaps": true
    }
  ]
}
```

### 3. 包管理命令
```json
// package.json 脚本
{
  "scripts": {
    "start": "react-native start",
    "android": "react-native run-android",
    "ios": "react-native run-ios",
    "lint": "eslint . --ext .js,.jsx",
    "test": "jest",
    "build:android": "cd android && ./gradlew assembleRelease",
    "build:ios": "cd ios && xcodebuild -workspace Moveup.xcworkspace -scheme Moveup -configuration Release",
    "codepush:ios": "appcenter codepush release-react -a moveup/ios -d Production",
    "codepush:android": "appcenter codepush release-react -a moveup/android -d Production"
  }
}
```

## 📊 性能优化策略

### 1. 渲染优化
- 使用 `React.memo` 缓存组件
- 使用 `useMemo` 和 `useCallback` 避免重复计算
- 长列表使用 `FlatList` 的 `getItemLayout` 和 `initialNumToRender`

### 2. 图片优化
- 使用 `react-native-fast-image` 缓存图片
- 图片压缩和 WebP 格式转换
- 按需加载和懒加载

### 3. 启动优化
- 代码分割和懒加载
- 使用 Hermes 引擎
- 减少主线程阻塞操作

## 🔒 安全规范

### 1. 数据存储
- 敏感信息使用 `react-native-keychain` 存储
- Token 存储在 Keychain/Keystore
- 本地数据加密存储

### 2. 权限管理
```javascript
// utils/permission.js
import { check, request, PERMISSIONS, RESULTS } from 'react-native-permissions';

export const requestLocationPermission = async () => {
  try {
    const result = await request(
      Platform.select({
        ios: PERMISSIONS.IOS.LOCATION_WHEN_IN_USE,
        android: PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION,
      })
    );
    
    return result === RESULTS.GRANTED;
  } catch (error) {
    console.error('Permission error:', error);
    return false;
  }
};
```

## 📱 页面路由结构

```javascript
// navigation/MainTabNavigator.js
const Tab = createBottomTabNavigator();

const MainTabNavigator = () => {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName;
          if (route.name === 'Home') {
            iconName = focused ? 'home' : 'home-outline';
          } else if (route.name === 'Run') {
            iconName = focused ? 'run' : 'run-fast';
          } else if (route.name === 'Social') {
            iconName = focused ? 'account-group' : 'account-group-outline';
          } else if (route.name === 'Profile') {
            iconName = focused ? 'account' : 'account-outline';
          }
          return <Icon name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.text.secondary,
        headerShown: false
      })}
    >
      <Tab.Screen name="Home" component={HomeScreen} />
      <Tab.Screen name="Run" component={RunScreen} />
      <Tab.Screen name="Social" component={FeedScreen} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  );
};
```

## 📦 打包发布流程


### Android 发布
1. 生成签名密钥
2. 配置 `gradle.properties`
3. 运行 `npm run build:android`
4. 上传到 Google Play Console 或国内应用商店

---

*文档版本：v1.0.0 | 最后更新：2026-3-11*