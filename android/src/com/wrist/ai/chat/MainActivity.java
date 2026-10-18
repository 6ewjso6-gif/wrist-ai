package com.wrist.ai.chat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.xiaomi.xms.wearable.Wearable;
import com.xiaomi.xms.wearable.auth.AuthApi;
import com.xiaomi.xms.wearable.auth.Permission;
import com.xiaomi.xms.wearable.message.MessageApi;
import com.xiaomi.xms.wearable.node.Node;
import com.xiaomi.xms.wearable.node.NodeApi;
import com.xiaomi.xms.wearable.tasks.OnFailureListener;
import com.xiaomi.xms.wearable.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int BG     = 0xFF0D1117;
    private static final int CARD   = 0xFF161B22;
    private static final int FG     = 0xFFE6EDF3;
    private static final int SUB    = 0xFF8A94A0;
    private static final int ACCENT = 0xFF62D0FF;
    private static final int GREEN  = 0xFF238636;
    private static final int BLUE   = 0xFF1F6FEB;

    private static final int ID_SEND  = 1001;
    private static final int ID_FIND  = 1002;
    private static final int ID_P1    = 2001;
    private static final int ID_P2    = 2002;
    private static final int ID_P3    = 2003;

    private LinearLayout root;
    private TextView status;
    private TextView nodesView;

    private EditText etChatUrl, etChatKey, etChatModel;
    private EditText etAsrUrl, etAsrKey;
    private EditText etTtsUrl;
    private EditText etQ1, etQ2, etQ3, etQ4;

    private String nodeId = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(buildUi());
        askPerms();
        refreshNodes();
    }

    // ================= UI =================
    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(20);
        root.setPadding(p, p, p, p);
        sv.addView(root);

        title("腕上AI 设置器");
        sub("在这里舒服地填好配置，一键推到手表。\n手表需已安装「腕上AI」快应用，并与这台手机蓝牙连接。");

        nodesView = card("正在查找手表…");
        root.addView(nodesView);

        section("对话模型");
        etChatUrl   = field("API 地址", "https://api.deepseek.com/chat/completions", false);
        etChatKey   = field("API 密钥", "sk-...", true);
        etChatModel = field("模型名", "deepseek-chat", false);
        presetRow();

        section("语音识别（可留空）");
        etAsrUrl = field("接口地址", "https://api.openai.com/v1/audio/transcriptions", false);
        etAsrKey = field("密钥", "sk-...", true);

        section("朗读 TTS（可留空）");
        etTtsUrl = field("URL 模板", "https://dict.youdao.com/dictvoice?type=2&audio={text}", false);

        section("快捷提问");
        etQ1 = field("问题一", "今天有什么值得关注的新闻？", false);
        etQ2 = field("问题二", "用一句话解释量子纠缠", false);
        etQ3 = field("问题三", "推荐一部电影并说明理由", false);
        etQ4 = field("问题四", "帮我写一句朋友圈文案", false);

        root.addView(button(ID_SEND, "发送到手表", GREEN, Color.WHITE, 54, 24));
        root.addView(button(ID_FIND, "重新查找手表", CARD, ACCENT, 48, 10));

        status = new TextView(this);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        status.setTextColor(SUB);
        status.setPadding(0, dp(20), 0, dp(30));
        status.setLineSpacing(0, 1.3f);
        status.setText("");
        root.addView(status);

        return sv;
    }

    private void title(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        v.setTextColor(ACCENT);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, dp(10), 0, dp(6));
        root.addView(v);
    }

    private void sub(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        v.setTextColor(SUB);
        v.setLineSpacing(0, 1.4f);
        v.setPadding(0, 0, 0, dp(14));
        root.addView(v);
    }

    private TextView card(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        v.setTextColor(FG);
        v.setBackgroundColor(CARD);
        v.setPadding(dp(16), dp(14), dp(16), dp(14));
        v.setLineSpacing(0, 1.3f);
        return v;
    }

    private void section(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        v.setTextColor(ACCENT);
        v.setPadding(0, dp(22), 0, dp(8));
        root.addView(v);
    }

    private EditText field(String hint, String def, boolean secret) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(def);
        e.setHintTextColor(SUB);
        e.setTextColor(FG);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        e.setBackgroundColor(CARD);
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        e.setLayoutParams(lp);
        root.addView(e);
        return e;
    }

    private Button button(int id, String text, int bg, int fg, int h, int topMargin) {
        Button b = new Button(this);
        b.setId(id);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTextColor(fg);
        b.setBackgroundColor(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(h));
        lp.topMargin = dp(topMargin);
        b.setLayoutParams(lp);
        b.setOnClickListener(this);
        return b;
    }

    private void presetRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int[] ids = {ID_P1, ID_P2, ID_P3};
        String[] names = {"DeepSeek", "通义", "Kimi"};
        for (int i = 0; i < 3; i++) {
            Button b = new Button(this);
            b.setId(ids[i]);
            b.setText(names[i]);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            b.setBackgroundColor(BLUE);
            b.setTextColor(Color.WHITE);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
            lp.rightMargin = dp(6);
            b.setLayoutParams(lp);
            b.setOnClickListener(this);
            row.addView(b);
        }
        root.addView(row);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    // ================= 点击分发 =================
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == ID_SEND) { doSend(); return; }
        if (id == ID_FIND) { refreshNodes(); return; }
        if (id == ID_P1) {
            etChatUrl.setText("https://api.deepseek.com/chat/completions");
            etChatModel.setText("deepseek-chat");
            status.setText("已填入 DeepSeek");
        } else if (id == ID_P2) {
            etChatUrl.setText("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
            etChatModel.setText("qwen-plus");
            status.setText("已填入通义千问");
        } else if (id == ID_P3) {
            etChatUrl.setText("https://api.moonshot.cn/v1/chat/completions");
            etChatModel.setText("moonshot-v1-8k");
            status.setText("已填入 Kimi");
        }
    }

    // ================= 权限 =================
    private void askPerms() {
        if (Build.VERSION.SDK_INT < 31) return;
        String[] need = { "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN" };
        java.util.ArrayList<String> miss = new java.util.ArrayList<String>();
        for (int i = 0; i < need.length; i++) {
            if (checkSelfPermission(need[i]) != PackageManager.PERMISSION_GRANTED) miss.add(need[i]);
        }
        if (!miss.isEmpty()) {
            requestPermissions(miss.toArray(new String[0]), 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        refreshNodes();
    }

    // ===== 环境自检 =====
    private String envReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("系统: Android ").append(Build.VERSION.RELEASE)
          .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

        boolean mi = isInstalled("com.xiaomi.wearable");
        boolean mh = isInstalled("com.mi.health");
        sb.append("小米运动健康: ").append(mi ? "已安装" : "未安装");
        if (!mi && mh) sb.append("（找到 com.mi.health）");
        sb.append("\n");

        if (Build.VERSION.SDK_INT >= 31) {
            boolean c = checkSelfPermission("android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED;
            sb.append("蓝牙权限: ").append(c ? "已授予" : "未授予");
        } else {
            sb.append("蓝牙权限: 无需申请");
        }
        return sb.toString();
    }

    private boolean isInstalled(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ================= 查找手表 =================
    private void refreshNodes() {
        nodesView.setText("正在查找手表…\n\n" + envReport());
        nodeId = null;
        try {
            Wearable.getNodeApi(this).getConnectedNodes()
                    .addOnSuccessListener(new NodeListener(this))
                    .addOnFailureListener(new FailListener(this, "查找手表"));
        } catch (Throwable t) {
            nodesView.setText("SDK 调用失败：" + t + "\n\n请确认手机已安装「小米运动健康」App");
        }
    }

    void onNodes(List nodes) {
        if (nodes == null || nodes.isEmpty()) {
            nodesView.setText("未找到已连接的手表\n\n" + envReport() + "\n请依次确认：\n1. 手表与手机已蓝牙配对\n2. 手表上已装「腕上AI」并【打开着】\n3. 小米运动健康在后台且已连手表\n4. 已授予蓝牙权限");
            nodeId = null;
            return;
        }
        StringBuilder sb = new StringBuilder("已连接设备：\n");
        for (int i = 0; i < nodes.size(); i++) {
            Node n = (Node) nodes.get(i);
            sb.append("  • ").append(n.name).append("\n");
        }
        nodesView.setText(sb.toString());
        nodeId = ((Node) nodes.get(0)).id;
    }

    private static class NodeListener implements OnSuccessListener {
        private final MainActivity a;
        NodeListener(MainActivity x) { a = x; }
        public void onSuccess(Object nodes) { a.onNodes((List) nodes); }
    }

    private static class FailListener implements OnFailureListener {
        private final MainActivity a;
        private final String what;
        FailListener(MainActivity x, String w) { a = x; what = w; }
        public void onFailure(Exception e) {
            if (a.status != null) a.status.setText(what + " 失败：" + e);
            if (a.nodesView != null) a.nodesView.setText(what + " 失败：" + e
                    + "\n\n请确认手机已安装「小米运动健康」App，并授予蓝牙权限");
        }
    }

    private static class SendListener implements OnSuccessListener {
        private final MainActivity a;
        SendListener(MainActivity x) { a = x; }
        public void onSuccess(Object v) {
            a.status.setText("✅ 已发送到手表\n\n现在打开手表上的「腕上AI」，配置会自动生效");
        }
    }

    // ================= 发送 =================
    private void doSend() {
        if (nodeId == null) {
            status.setText("还没有可用的手表连接，先点「重新查找手表」");
            return;
        }
        status.setText("检查权限中…");
        try {
            AuthApi auth = Wearable.getAuthApi(this);
            auth.checkPermissions(nodeId, new Permission[]{ Permission.DEVICE_MANAGER, Permission.NOTIFY })
                .addOnSuccessListener(new PermCheckListener(this))
                .addOnFailureListener(new PermFailListener(this));
        } catch (Throwable t) {
            status.setText("检查权限出错：" + t);
        }
    }

    // 权限检查回调
    void onPermChecked(Object result) {
        boolean all = true;
        try {
            boolean[] arr = (boolean[]) result;
            for (int i = 0; i < arr.length; i++) if (!arr[i]) all = false;
        } catch (Throwable t) { all = false; }

        if (all) {
            status.setText("权限已授予，发送中…");
            doSendNow();
            return;
        }
        status.setText("请在手表上点「允许」，等待授权…");
        try {
            Wearable.getAuthApi(this)
                .requestPermission(nodeId, Permission.DEVICE_MANAGER, Permission.NOTIFY)
                .addOnSuccessListener(new PermReqListener(this))
                .addOnFailureListener(new PermFailListener(this));
        } catch (Throwable t) {
            status.setText("请求权限出错：" + t);
        }
    }

    // 授权成功回调
    void onPermGranted() {
        status.setText("已授权，发送中…");
        doSendNow();
    }

    // 权限相关失败：不放弃，仍然尝试发送一次
    void onPermFail(Object err) {
        status.setText("权限请求失败（" + err + "），仍尝试发送…");
        doSendNow();
    }

    private void doSendNow() {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "cfg");
            o.put("apiUrl", etChatUrl.getText().toString().trim());
            o.put("apiKey", etChatKey.getText().toString().trim());
            o.put("model", etChatModel.getText().toString().trim());
            o.put("asrUrl", etAsrUrl.getText().toString().trim());
            o.put("asrKey", etAsrKey.getText().toString().trim());
            o.put("ttsUrl", etTtsUrl.getText().toString().trim());

            JSONArray q = new JSONArray();
            String[] qs = {etQ1.getText().toString(), etQ2.getText().toString(),
                    etQ3.getText().toString(), etQ4.getText().toString()};
            for (int i = 0; i < qs.length; i++) {
                if (qs[i] != null && qs[i].trim().length() > 0) q.put(qs[i].trim());
            }
            o.put("quickAsks", q);

            byte[] data = o.toString().getBytes("UTF-8");
            status.setText("正在发送 " + data.length + " 字节…");

            Wearable.getMessageApi(this).sendMessage(nodeId, data)
                    .addOnSuccessListener(new SendListener(this))
                    .addOnFailureListener(new FailListener(this, "发送"));
        } catch (Throwable t) {
            status.setText("发送出错：" + t);
        }
    }

    // ===== 回调类（命名内部类 + 原始类型，绕开 d8 的 NPE）=====
    private static class PermCheckListener implements OnSuccessListener {
        private final MainActivity a;
        PermCheckListener(MainActivity x) { a = x; }
        public void onSuccess(Object r) { a.onPermChecked(r); }
    }

    private static class PermReqListener implements OnSuccessListener {
        private final MainActivity a;
        PermReqListener(MainActivity x) { a = x; }
        public void onSuccess(Object r) { a.onPermGranted(); }
    }

    private static class PermFailListener implements OnFailureListener {
        private final MainActivity a;
        PermFailListener(MainActivity x) { a = x; }
        public void onFailure(Exception e) { a.onPermFail(e.getMessage()); }
    }}
