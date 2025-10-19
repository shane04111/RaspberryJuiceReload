package com.shane.raspberryjuicereload.gui;

import javax.swing.*;

public class Window {
    public void initialize() {
        JFrame frame = new JFrame("My First Window");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setVisible(true);
    }
}
