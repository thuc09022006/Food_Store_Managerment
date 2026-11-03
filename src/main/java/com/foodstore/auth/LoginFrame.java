package com.foodstore.auth;

import com.foodstore.FoodStoreApplication;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.springframework.context.ApplicationContext;

/** Màn hình đăng nhập. Chỉ dựng giao diện và gọi {@link AuthService}; không chứa nghiệp vụ. */
public class LoginFrame extends JFrame {

    private static final Logger LOG = Logger.getLogger(LoginFrame.class.getName());

    private final AuthService authService;
    private final CurrentSession session;
    private final ApplicationContext ctx;

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("Đăng nhập");

    public LoginFrame(AuthService authService, CurrentSession session, ApplicationContext ctx) {
        super("FoodStore Management System - Đăng nhập");
        this.authService = authService;
        this.session = session;
        this.ctx = ctx;
        buildUi();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        form.add(new JLabel("Tên đăng nhập:"), c);
        c.gridx = 1;
        form.add(usernameField, c);

        c.gridx = 0; c.gridy = 1;
        form.add(new JLabel("Mật khẩu:"), c);
        c.gridx = 1;
        form.add(passwordField, c);

        errorLabel.setForeground(new Color(0xB00020));
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2;
        form.add(errorLabel, c);

        c.gridy = 3; c.anchor = GridBagConstraints.CENTER;
        form.add(loginButton, c);

        loginButton.addActionListener(e -> onLogin());
        getRootPane().setDefaultButton(loginButton); // phím Enter = đăng nhập
        setContentPane(form);
    }

    private void onLogin() {
        char[] password = passwordField.getPassword();
        try {
            Staff staff = authService.login(usernameField.getText(), new String(password));
            session.login(staff);
            // Mở cửa sổ chính TRƯỚC khi đóng cửa sổ đăng nhập để JVM không tắt giữa chừng.
            new MainFrame(staff, ctx, () -> FoodStoreApplication.showLogin(ctx)).setVisible(true);
            dispose();
        } catch (AuthenticationFailedException e) {
            errorLabel.setText(e.getMessage());
            passwordField.setText("");
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Đăng nhập lỗi hệ thống", e);
            errorLabel.setText("Không kết nối được cơ sở dữ liệu. Kiểm tra MySQL và biến môi trường DB_*");
        } finally {
            Arrays.fill(password, '\0');
        }
        pack();
    }
}
