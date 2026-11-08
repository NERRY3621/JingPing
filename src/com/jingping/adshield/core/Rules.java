package com.jingping.adshield.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** 内置规则：广告 Activity、跳过按钮、跳转目标、广告 SDK 组件。 */
public final class Rules {

    private Rules() {
    }

    /** 广告 SDK 组件前缀。只用于 Activity，不动 Service/Provider，降低风险。 */
    public static final String[] AD_PREFIXES = new String[]{
            "com.bytedance.sdk.openadsdk",
            "com.bytedance.msdk",
            "com.bytedance.pangle",
            "com.qq.e.",
            "com.kwad.",
            "com.mbridge.",
            "com.unity3d.ads",
            "com.unity3d.services.ads",
            "com.applovin.",
            "com.vungle.",
            "com.ironsource.",
            "com.chartboost.",
            "com.inmobi.",
            "com.sigmob.",
            "com.huawei.hms.ads",
            "com.baidu.mobads",
            "com.tencent.klevin",
            // B 站自营开屏广告页（不包含 SplashShellActivity 启动壳）
            "tv.danmaku.bili.splash.ad."
    };

    /** 广告跳转常见落地 App。 */
    public static final Set<String> JUMP_TARGETS = new HashSet<>(Arrays.asList(
            "com.taobao.taobao",
            "com.jingdong.app.mall",
            "com.xunmeng.pinduoduo",
            "com.taobao.idlefish",
            "com.eg.android.AlipayGphone",
            "com.ss.android.ugc.aweme",
            "com.smile.gifmaker",
            "com.android.chrome",
            "com.android.browser",
            "com.UCMobile",
            "com.quark.browser",
            "com.baidu.searchbox",
            "com.autonavi.minimap",
            "com.sankuai.meituan",
            "com.dianping.v1",
            "com.ctrip.ctrip",
            "com.trip.octopus"
    ));

    /** 默认深度净化 / 勾选的应用（仅存已安装的）。 */
    public static final String[] DEFAULT_SCOPE = new String[]{
            "tv.danmaku.bili",
            "com.zhihu.android",
            "com.sina.weibo",
            "com.xingin.xhs",
            "com.ss.android.ugc.aweme",
            "com.smile.gifmaker",
            "com.taobao.taobao",
            "com.jingdong.app.mall",
            "com.xunmeng.pinduoduo",
            "com.sankuai.meituan",
            "com.dianping.v1",
            "com.tencent.qqlive",
            "com.qiyi.video",
            "com.youku.phone",
            "com.netease.cloudmusic",
            "com.tencent.qqmusic",
            "com.baidu.searchbox",
            "com.autonavi.minimap"
    };

    /** 广告 / 开屏 Activity 名称特征（用于判断“现在是广告上下文”）。 */
    private static final Pattern AD_ACTIVITY = Pattern.compile(
            "(?i)(splash.*ad|ad.*splash|hotsplash|adactivity|advert|interstitial|rewardad|fullscreenad|splashad|ad\\.page)");

    private static final Pattern REWARD = Pattern.compile("(?i)reward");

    /** 可以安全按返回键的纯开屏广告页；不含启动壳 / 激励视频 / 广告落地页。 */
    private static final Pattern BACKABLE = Pattern.compile(
            "(?i)(hotsplash|splashad|adactivity|advertactivity|interstitial|fullscreenad)");

    private static final Set<String> SYSTEM_UI = new HashSet<>(Arrays.asList(
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "android"
    ));

    /** 桌面 / 启动器：绝不能在桌面上找“跳过”并点击。 */
    private static final Set<String> LAUNCHERS = new HashSet<>(Arrays.asList(
            "com.zte.mifavor.launcher",
            "com.android.launcher3",
            "com.android.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.miui.home",
            "com.bbk.launcher2",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.sec.android.app.launcher",
            "com.motorola.launcher3",
            "com.oneplus.launcher",
            "com.coloros.launcher",
            "com.realme.launcher"
    ));

    /** 开屏壳（SplashShellActivity 等）也按广告上下文处理，延长扫描窗口。 */
    private static final Pattern SPLASH_LIKE = Pattern.compile("(?i)splash");

    public static boolean isAdActivity(String cls) {
        return cls != null && AD_ACTIVITY.matcher(cls).find();
    }

    public static boolean isBackableAd(String cls) {
        return cls != null && BACKABLE.matcher(cls).find() && !REWARD.matcher(cls).find();
    }

    public static boolean isSystemUi(String pkg) {
        return pkg != null && SYSTEM_UI.contains(pkg);
    }

    public static boolean isLauncher(String pkg) {
        if (pkg == null) {
            return false;
        }
        if (LAUNCHERS.contains(pkg)) {
            return true;
        }
        // 兜底：厂商桌面常见命名
        String l = pkg.toLowerCase(Locale.ROOT);
        return l.contains("launcher") && (l.startsWith("com.android")
                || l.startsWith("com.zte") || l.startsWith("com.miui")
                || l.startsWith("com.google") || l.startsWith("com.huawei")
                || l.startsWith("com.oppo") || l.startsWith("com.sec")
                || l.startsWith("com.oneplus") || l.startsWith("com.coloros"));
    }

    public static boolean isSplashLike(String cls) {
        return cls != null && SPLASH_LIKE.matcher(cls).find()
                && !cls.contains("MainActivity");
    }

    public static boolean isJumpTarget(String pkg) {
        return pkg != null && JUMP_TARGETS.contains(pkg);
    }

    /**
     * 是否像开屏 / 广告的“跳过”文案。
     * 明确排除设置项、已跳过提示、片头片尾跳过等正常文案。
     */
    public static boolean isSkipText(CharSequence cs) {
        if (cs == null) {
            return false;
        }
        String s = cs.toString().trim().replace('\u00A0', ' ');
        if (s.isEmpty() || s.length() > 14) {
            return false;
        }
        if (s.contains("已跳过") || s.contains("拦截生效") || s.contains("自动跳过")
                || s.contains("跳过片头") || s.contains("跳过片尾") || s.contains("跳过设置")
                || s.contains("跳过片尾曲") || s.contains("跳过广告设置")) {
            return false;
        }
        if (s.contains("跳过")) {
            return true;
        }
        String l = s.toLowerCase(Locale.ROOT).trim();
        if (l.equals("skip") || l.equals("skip ad") || l.equals("skip ads")
                || l.equals("close ad") || l.equals("close ads")) {
            return true;
        }
        return l.matches("skip\\s*\\d{1,2}") || l.matches("\\d{1,2}\\s*skip");
    }

    /** 控件 ID 是否像跳过 / 关闭广告按钮。 */
    public static boolean isSkipId(String id) {
        if (id == null) {
            return false;
        }
        String l = id.toLowerCase(Locale.ROOT);
        int slash = l.lastIndexOf('/');
        if (slash >= 0) {
            l = l.substring(slash + 1);
        }
        if (l.contains("intro") || l.contains("setting")) {
            return false;
        }
        return l.contains("skip")
                || l.contains("ad_close") || l.contains("close_ad")
                || l.contains("adclose") || l.contains("closead");
    }

    /** 匹配广告 SDK Activity。 */
    public static boolean isAdComponent(String className) {
        if (className == null) {
            return false;
        }
        if (className.contains("SplashShellActivity") || className.contains("MainActivity")) {
            return false;
        }
        for (String p : AD_PREFIXES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    public static String shortClass(String cls) {
        if (cls == null) {
            return "";
        }
        int i = cls.lastIndexOf('.');
        return i >= 0 ? cls.substring(i + 1) : cls;
    }
}
