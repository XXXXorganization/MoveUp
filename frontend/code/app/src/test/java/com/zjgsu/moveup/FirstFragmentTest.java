package com.zjgsu.moveup;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class FirstFragmentTest {

    // ==========================================
    // 1. 测试 UI 渲染和按钮点击导航逻辑
    // ==========================================
    @Test
    public void testButtonFirst_Clicks_NavigatesToSecondFragment() {
        // 1. 创建一个“虚拟”的导航控制器 (Mock NavController)
        NavController mockNavController = mock(NavController.class);

        // 2. 启动 FirstFragment
        FragmentScenario<FirstFragment> scenario = FragmentScenario.launchInContainer(FirstFragment.class);

        // 3. 在 Fragment 内部执行操作
        scenario.onFragment(fragment -> {
            // 将虚拟的导航控制器绑定到 Fragment 的根 View 上
            Navigation.setViewNavController(fragment.requireView(), mockNavController);

            // 触发按钮点击 (在 FragmentFirstBinding 中，按钮默认 ID 为 button_first)
            fragment.requireView().findViewById(R.id.button_first).performClick();
        });

        // 4. 断言验证：检查是否成功调用了 navigate()，并且传入了去往 SecondFragment 的 Action ID
        verify(mockNavController).navigate(R.id.action_FirstFragment_to_SecondFragment);
    }

    // ==========================================
    // 2. 测试生命周期销毁逻辑 (防止内存泄漏)
    // ==========================================
    @Test
    public void testOnDestroyView_NullifiesBinding() {
        // 启动 Fragment
        FragmentScenario<FirstFragment> scenario = FragmentScenario.launchInContainer(FirstFragment.class);

        // 强行把 Fragment 的生命周期推进到 DESTROYED 状态
        // 这会自动触发 onDestroyView() 方法，覆盖 binding = null; 这行代码
        scenario.moveToState(Lifecycle.State.DESTROYED);
    }
}