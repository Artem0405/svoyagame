package org.example.ui.components; // или org.example.ui.utils

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

public class ImageDisplayDialog extends JDialog {

    private JLabel imageLabel;
    private JScrollPane scrollPane; // На случай очень больших картинок

    public ImageDisplayDialog(Frame owner, String title, URL imageUrl) {
        super(owner, title, false); // true делает диалог модальным
        initComponents(imageUrl);
        setupLayout();
        pack(); // Подгоняет размер диалога под содержимое
        setLocationRelativeTo(owner); // Центрировать относительно родителя

        // Закрывать диалог по клику мыши или нажатию Esc
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose(); // Закрыть диалог
            }
        });

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void initComponents(URL imageUrl) {
        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (imageUrl != null) {
            ImageIcon icon = new ImageIcon(imageUrl);
            // Опционально: Масштабирование, если картинка больше экрана
            Image image = icon.getImage();
            int imgWidth = icon.getIconWidth();
            int imgHeight = icon.getIconHeight();

            // Получаем размеры экрана
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = (int) (screenSize.width * 0.9); // Используем 90% экрана
            int screenHeight = (int) (screenSize.height * 0.9);

            // Масштабируем, если нужно, сохраняя пропорции
            if (imgWidth > screenWidth || imgHeight > screenHeight) {
                double widthRatio = (double) screenWidth / imgWidth;
                double heightRatio = (double) screenHeight / imgHeight;
                double ratio = Math.min(widthRatio, heightRatio); // Берем меньший коэфф.

                int newWidth = (int) (imgWidth * ratio);
                int newHeight = (int) (imgHeight * ratio);

                Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                imageLabel.setIcon(icon); // Используем оригинал
            }

        } else {
            imageLabel.setText("Ошибка загрузки изображения");
            imageLabel.setForeground(Color.RED);
        }

        scrollPane = new JScrollPane(imageLabel);
        scrollPane.setBorder(null); // Убираем рамку у скролла
    }

    private void setupLayout() {
        // Используем простой BorderLayout
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        // Можно добавить кнопку "Закрыть", если нужно
    }
}