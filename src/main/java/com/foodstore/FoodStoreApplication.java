package com.foodstore;

import com.foodstore.auth.AuthService;
import com.foodstore.auth.CurrentSession;
import com.foodstore.auth.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Điểm vào của ứng dụng: khởi tạo Spring (chỉ làm DI + cấu hình), sau đó mở Swing trên EDT.
 * Không có web server.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class FoodStoreApplication {

    public static void main(String[] args) {
        // Spring Boot mặc định headless=true; để mở được JFrame phải tắt chế độ này,
        // nếu không Swing ném HeadlessException ngay khi tạo cửa sổ.
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(FoodStoreApplication.class)
                .headless(false)
                .run(args);

        SwingUtilities.invokeLater(() -> {
            useSystemLookAndFeel();
            showLogin(ctx);
        });
    }

    /** Mở màn hình đăng nhập (dùng khi khởi động và khi đăng xuất). Phải gọi trên EDT. */
    public static void showLogin(ApplicationContext ctx) {
        new LoginFrame(ctx.getBean(AuthService.class), ctx.getBean(CurrentSession.class), ctx)
                .setVisible(true);
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // giữ look-and-feel mặc định của Swing
        }
    }
}
