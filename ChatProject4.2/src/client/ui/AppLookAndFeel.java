//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package client.ui;

import java.awt.Font;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class AppLookAndFeel {
    public AppLookAndFeel() {
    }

    public static void setLookAndFeel() {
        try {
            LookAndFeelInfo[] var0 = UIManager.getInstalledLookAndFeels();
            int var1 = var0.length;

            for(int var2 = 0; var2 < var1; ++var2) {
                LookAndFeelInfo info = var0[var2];
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    configureNimbus();
                    return;
                }
            }

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception var4) {
            System.err.println("无法设置 UI 主题，使用默认样式");
            var4.printStackTrace();
        }

    }

    private static void configureNimbus() {
        try {
            Font defaultFont = new Font("微软雅黑", 0, 12);
            UIManager.put("defaultFont", defaultFont);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
        } catch (Exception var1) {
        }

    }
}
