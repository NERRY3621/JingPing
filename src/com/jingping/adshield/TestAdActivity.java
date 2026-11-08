package com.jingping.adshield;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.jingping.adshield.core.Logs;

/** 模拟开屏广告页，用于验证无障碍跳过引擎。 */
public class TestAdActivity extends Activity {

    private Button btnSkip;
    private TextView tvResult;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int count = 5;
    private boolean done;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (done) {
                return;
            }
            count--;
            if (count > 0) {
                btnSkip.setText("跳过 " + count);
                handler.postDelayed(this, 1000);
            } else {
                done = true;
                btnSkip.setText("关闭");
                tvResult.setText(R.string.test_ad_closed);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setContentView(R.layout.activity_test_ad);

        btnSkip = findViewById(R.id.btnSkip);
        tvResult = findViewById(R.id.tvResult);
        Button btnClose = findViewById(R.id.btnClose);

        btnSkip.setText("跳过 " + count);
        btnSkip.setOnClickListener(v -> {
            if (done) {
                finish();
            } else {
                skip();
            }
        });
        btnClose.setOnClickListener(v -> finish());

        handler.postDelayed(tick, 1000);
        Logs.add("测试开屏页已打开");
    }

    private void skip() {
        done = true;
        handler.removeCallbacksAndMessages(null);
        tvResult.setText(R.string.test_ad_done);
        btnSkip.setText("已跳过");
        btnSkip.setEnabled(false);
        Logs.add("测试开屏：跳过按钮被点击");
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
