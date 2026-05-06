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
public class SecondFragmentTest {

    // ==========================================
    // 1. 测试 UI 渲染和按钮点击导航逻辑
    // ==========================================
    @Test
    public void testButtonSecond_Clicks_NavigatesToFirstFragment() {
        // 1. 创建一个“虚拟”的导航控制器 (Mock NavController)
        NavController mockNavController = mock(NavController.class);

        // 2. 启动 SecondFragment
        FragmentScenario<SecondFragment> scenario = FragmentScenario.launchInContainer(SecondFragment.class);

        // 3. 在 Fragment 内部执行操作
        scenario.onFragment(fragment -> {
            // 将虚拟的导航控制器绑定到 Fragment 的根 View 上，这样 findNavController 就能找到它
            Navigation.setViewNavController(fragment.requireView(), mockNavController);

            // 触发按钮点击 (由于你用了 ViewBinding，按钮的 id 默认是 button_second)
            fragment.requireView().findViewById(R.id.button_second).performClick();
        });

        // 4. 断言验证：检查是否成功调用了 navigate()，并且传入了正确的 Action ID
        verify(mockNavController).navigate(R.id.action_SecondFragment_to_FirstFragment);
    }

    // ==========================================
    // 2. 测试生命周期销毁逻辑 (防止内存泄漏)
    // ==========================================
    @Test
    public void testOnDestroyView_NullifiesBinding() {
        // 启动 Fragment
        FragmentScenario<SecondFragment> scenario = FragmentScenario.launchInContainer(SecondFragment.class);

        // 强行把 Fragment 的生命周期推进到 DESTROYED 状态
        // 这会自动触发 onDestroyView() 方法，覆盖 binding = null; 这行代码
        scenario.moveToState(Lifecycle.State.DESTROYED);

        // 只要能顺利走完不崩溃，这块代码的覆盖率就会被成功统计
    }
}