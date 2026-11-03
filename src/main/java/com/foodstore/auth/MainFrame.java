package com.foodstore.auth;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.springframework.context.ApplicationContext;

/**
 * Khung chính: một JFrame + CardLayout. Menu bên trái được dựng từ {@link Staff#allowedScreens()}
 * nên mỗi vai trò thấy menu khác nhau mà không có if/else theo vai trò (đa hình).
 *
 * Phase 0: mọi màn hình là {@link PlaceholderPanel}.
 * Phase 2 (M5): sửa {@link #createPanel(Screen)} để trả panel thật, ví dụ
 * {@code case MENU -> ctx.getBean(MenuPanel.class)} (panel khai báo @Component @Lazy).
 */
public class MainFrame extends JFrame {

    private final Staff staff;
    private final ApplicationContext ctx;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Set<Screen> opened = EnumSet.noneOf(Screen.class);

    public MainFrame(Staff staff, ApplicationContext ctx, Runnable onLogout) {
        super("FoodStore Management System");
        this.staff = staff;
        this.ctx = ctx;

        add(buildHeader(onLogout), BorderLayout.NORTH);
        add(buildSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // mọi vai trò đều có ít nhất một màn hình
        show(staff.allowedScreens().iterator().next());
    }

    private JPanel buildHeader(Runnable onLogout) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(new JLabel("Xin chào, " + staff.getFullName() + " (" + staff.getRole().getLabel() + ")"),
                BorderLayout.WEST);
        JButton logout = new JButton("Đăng xuất");
        logout.addActionListener(e -> {
            ctx.getBean(CurrentSession.class).logout();
            dispose();
            onLogout.run();
        });
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        for (Screen screen : staff.allowedScreens()) {
            JButton button = new JButton(screen.getLabel());
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            button.addActionListener(e -> show(screen));
            side.add(button);
            side.add(Box.createVerticalStrut(6));
        }
        side.setPreferredSize(new Dimension(180, 0));
        return side;
    }

    private void show(Screen screen) {
        if (!staff.canAccess(screen)) {
            throw new IllegalStateException(staff.getRole() + " không có quyền vào " + screen);
        }
        if (opened.add(screen)) {
            content.add(createPanel(screen), screen.name());
        }
        cards.show(content, screen.name());
    }

    /** Điểm nối Phase 2: thay PlaceholderPanel bằng panel thật của từng module. */
    protected JPanel createPanel(Screen screen) {
        return new PlaceholderPanel(screen.getLabel());
    }
}
