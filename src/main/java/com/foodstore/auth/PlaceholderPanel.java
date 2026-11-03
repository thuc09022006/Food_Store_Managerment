package com.foodstore.auth;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Panel giữ chỗ cho module chưa xong. Phase 2: MainFrame thay bằng panel thật của từng module. */
public class PlaceholderPanel extends JPanel {

    public PlaceholderPanel(String title) {
        super(new BorderLayout());
        JLabel label = new JLabel("<html><center><h2>" + title + "</h2>Module này chưa được cài đặt.</center></html>",
                SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
    }
}
