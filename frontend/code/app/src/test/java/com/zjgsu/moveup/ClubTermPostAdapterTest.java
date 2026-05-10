package com.zjgsu.moveup;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class ClubTermPostAdapterTest {

    private MockWebServer mockWebServer;
    private Context context;
    private ClubTermPostAdapter adapter;
    private List<ClubTermPost> dummyPosts;

    @Before
    public void setUp() throws Exception {
        // 使用 Robolectric 自带的 Context
        context = org.robolectric.RuntimeEnvironment.application;
        // 明确指定使用 Material 组件库的内置主题，彻底消灭 Cannot resolve symbol 报错
        context.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar);

        // 1. 启动 MockWebServer 拦截网络请求
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String mockUrl = mockWebServer.url("/").toString();
        // 截取掉最后的斜杠
        ClubTermPostAdapter.BASE_URL = mockUrl.substring(0, mockUrl.length() - 1);

        // 设置网络请求的模拟响应
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.contains("/like")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"is_liked\":true,\"like_count\":11}}");
                } else if (path.contains("/comment")) {
                    return new MockResponse().setResponseCode(200)
                            .setBody("{\"code\":200,\"data\":{\"total_comments\":4,\"comments\":[{\"id\":\"c99\",\"author\":\"Robot\",\"content\":\"Test!\",\"time\":\"now\"}]}}");
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        // 2. 构造各种边界条件的测试数据
        dummyPosts = new ArrayList<>();

        // --- 构造 Post 1 ---
        List<ClubComment> comments1 = new ArrayList<>();
        // 无回复对象的普通评论
        comments1.add(new ClubComment("c1", "Bob", "Nice!", "10:00", null, null));
        // 有回复对象的楼中楼评论 (覆盖富文本分支)
        comments1.add(new ClubComment("c2", "Charlie", "Agree!", "10:05", "c1", "Bob"));
        // 使用底部的 Helper 方法快速创建对象
        dummyPosts.add(createMockPost("p1", "Alice", false, 10, 2, comments1));

        // --- 构造 Post 2 ---
        List<ClubComment> comments2 = new ArrayList<>();
        // 触发点赞变红、展开评论等分支
        dummyPosts.add(createMockPost("p2", "Dave", true, 20, 5, comments2));

        adapter = new ClubTermPostAdapter(dummyPosts, "user_me");
    }

    @After
    public void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    // ==========================================
    // 1. UI 渲染与状态分支测试 (Missed Branches: onBindViewHolder)
    // ==========================================
    @Test
    public void testOnBindViewHolder_RenderBranches() {
        ViewGroup parent = new FrameLayout(context);
        ClubTermPostAdapter.PostViewHolder holder = adapter.onCreateViewHolder(parent, 0);

        // 测试 Post 1 (未点赞，少于3条评论)
        adapter.onBindViewHolder(holder, 0);
        assertEquals(View.GONE, holder.tvViewAllComments.getVisibility());
        assertEquals(2, holder.llCommentsList.getChildCount());

        // 测试 Post 2 (已点赞，多于3条评论)
        adapter.onBindViewHolder(holder, 1);
        assertEquals(View.VISIBLE, holder.tvViewAllComments.getVisibility());

        // 点击“查看所有评论”，验证是否跳转到详情页 (覆盖 openPostDetail 分支)
        holder.tvViewAllComments.performClick();
        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull("应该触发跳转详情页的Intent", startedIntent);
        assertEquals(PostDetailActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals("p2", startedIntent.getStringExtra("POST_ID"));
    }

    // ==========================================
    // 2. 评论区输入交互分支测试 (Keyboard & Focus logic)
    // ==========================================
    @Test
    public void testCommentInteraction_FocusAndClear() {
        ViewGroup parent = new FrameLayout(context);
        ClubTermPostAdapter.PostViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        // A. 模拟点击某一条评论，触发回复模式
        View commentItemView = holder.llCommentsList.getChildAt(0); // 点击第一条 Bob 的评论
        commentItemView.performClick();
        assertEquals("c1", holder.etCommentInput.getTag());
        assertEquals("Reply to Bob...", holder.etCommentInput.getHint().toString());

        // B. 模拟点击卡片空白处，触发取消回复模式
        holder.itemView.performClick();
        assertNull("Tag 应该被清空", holder.etCommentInput.getTag());
        assertEquals("Add a comment...", holder.etCommentInput.getHint().toString());
    }

    // ==========================================
    // 3. 点赞逻辑的 网络成功与崩溃保护 分支
    // ==========================================
    @Test
    public void testToggleLike_SuccessAndException() throws Exception {
        ViewGroup parent = new FrameLayout(context);
        ClubTermPostAdapter.PostViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        // --- A. 成功分支 ---
        holder.ivLike.performClick(); // 触发 toggleLike 网络请求
        Thread.sleep(200); // 稍微等待子线程发出请求
        Robolectric.flushForegroundThreadScheduler(); // 强制执行主线程回调 (mainHandler.post)

        // 验证数据是否被成功更新
        assertEquals(true, dummyPosts.get(0).isLiked);
        assertEquals(11, dummyPosts.get(0).likeCount);

        // --- B. 异常保护分支 (覆盖 catch Exception e) ---
        String originalUrl = ClubTermPostAdapter.BASE_URL;
        ClubTermPostAdapter.BASE_URL = "http://invalid-domain.test"; // 制造断网异常

        holder.ivLike.performClick();
        Thread.sleep(200);
        Robolectric.flushForegroundThreadScheduler(); // 只要没导致崩溃，catch分支就被成功覆盖了

        ClubTermPostAdapter.BASE_URL = originalUrl; // 恢复 URL
    }

    // ==========================================
    // 4. 发送评论逻辑的 各类边界条件 分支
    // ==========================================
    @Test
    public void testSendComment_Empty_Success_Exception() throws Exception {
        ViewGroup parent = new FrameLayout(context);
        ClubTermPostAdapter.PostViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        // --- A. 内容为空时不发送分支 (if (content.isEmpty()) return;) ---
        holder.etCommentInput.setText("   "); // 只有空格
        holder.btnSendComment.performClick();
        assertEquals(2, dummyPosts.get(0).totalComments); // 数量未变，说明被拦截了

        // --- B. 成功发送分支 ---
        holder.etCommentInput.setText("This is a great post!");
        holder.etCommentInput.setTag("c1"); // 模拟这是一条回复
        holder.btnSendComment.performClick();

        Thread.sleep(200);
        Robolectric.flushForegroundThreadScheduler();

        // 验证评论数据是否被更新，且输入框被清空
        assertEquals(4, dummyPosts.get(0).totalComments);
        assertEquals("", holder.etCommentInput.getText().toString());
        assertNull("发送成功后Tag应该被清空", holder.etCommentInput.getTag());

        // --- C. 异常保护分支 (覆盖 catch Exception e) ---
        String originalUrl = ClubTermPostAdapter.BASE_URL;
        ClubTermPostAdapter.BASE_URL = "http://invalid-domain.test"; // 制造异常

        holder.etCommentInput.setText("Test Exception");
        holder.btnSendComment.performClick();
        Thread.sleep(200);
        Robolectric.flushForegroundThreadScheduler();

        ClubTermPostAdapter.BASE_URL = originalUrl;
    }

    // ==========================================
    // 5. 测试基本功能
    // ==========================================
    @Test
    public void testGetItemCount() {
        assertEquals(2, adapter.getItemCount());
    }

    // ==========================================
    // 辅助方法：快速生成满足 13 个参数的测试用 Post 对象
    // ==========================================
    private ClubTermPost createMockPost(String id, String author, boolean isLiked, int likeCount, int totalComments, List<ClubComment> comments) {
        // 这里的 0、"Mock" 等都是不影响逻辑的占位符，专门用来满足原业务代码构造函数的要求
        return new ClubTermPost(
                id, author, "Just now", "Mock Title", 0, "Mock Badge",
                "Mock Sub", "Mock Detail", 0, isLiked, likeCount, totalComments, comments
        );
    }
}