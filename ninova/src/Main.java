import com.sun.jdi.connect.AttachingConnector;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import javax.swing.JOptionPane;
import javax.xml.crypto.Data;

public class Main {
    private static Connection connection;

    public static void main(String[] args) {

        connection = DataBaseHelper.getConnection();

        JFrame openf = new JFrame();
        openf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        openf.setSize(1920, 1080);
        openf.setTitle("Ninova Yoklama Programı");

        JPanel openp = new JPanel();
        openp.setLayout(null);
        openp.setBackground(Color.white );
        openf.add(openp);

        JLabel isimlabel = new JLabel("İsim");
        isimlabel.setBounds(800, 150, 100, 100);
        isimlabel.setHorizontalAlignment(SwingConstants.CENTER);
        isimlabel.setForeground(Color.black);
        openp.add(isimlabel);

        JLabel sifrelabel = new JLabel("Şifre");
        sifrelabel.setBounds(800, 400, 100, 100);
        sifrelabel.setHorizontalAlignment(SwingConstants.CENTER);
        sifrelabel.setForeground(Color.black);
        openp.add(sifrelabel);

        JTextField isimtextfield = new JTextField();
        isimtextfield.setBounds(800, 250, 300, 50);
        isimtextfield.setFocusable(true); // Odaklanabilir yapmak

        isimtextfield.setHorizontalAlignment(SwingConstants.CENTER);
        openp.add(isimtextfield);

        JPasswordField sifrepasswordfield = new JPasswordField();
        sifrepasswordfield.setBounds(800, 500, 300, 50);
        sifrepasswordfield.setHorizontalAlignment(SwingConstants.CENTER);

        openp.add(sifrepasswordfield);

        JButton loginbutton = new JButton(" Giriş yap!");
        loginbutton.setBounds(800, 600, 100, 50);
        loginbutton.setHorizontalAlignment(SwingConstants.CENTER);
        openp.add(loginbutton);

        loginbutton.addActionListener(e -> {
            String isim = isimtextfield.getText();
            String sifre = String.valueOf(sifrepasswordfield.getPassword());

            if(isim.equals("ninova") && sifre.equals("ninova123")) {
                openf.dispose();
                String[] options = {"Tabloyu Düzenle", "Yoklamaya Devam Et"};
                int choice = JOptionPane.showOptionDialog(
                        null,
                        "Lütfen yapmak istediğiniz seçeneği seçiniz",
                        "Giriş Başarılı",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) {  // Tabloyu Düzenle seçeneği
                    try (Connection connection = DataBaseHelper.getConnection()) {
                        if (connection != null) {
                            Tables table = new Tables(connection);
                            table.execute();
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace(); // veya uygun bir hata yönetimi
                    }
                } else if (choice == 1) {  // Yoklamaya Devam Et seçeneği
                    Attendance.DayTable();

                }

            }
            else if(isim.equals("ninova") && !sifre.equals("ninova123")) {
                JOptionPane.showMessageDialog(null, "Şifre Hatalı", "Hata", JOptionPane.ERROR_MESSAGE);
            }
            else if(!isim.equals("ninova") && sifre.equals("ninova123")) {
                JOptionPane.showMessageDialog(null, "İsim Hatalı", "Hata", JOptionPane.ERROR_MESSAGE);
            }
            else {
                JOptionPane.showMessageDialog(null, "İsim ve Şifre Hatalı", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        });
        // ENTER tuşuna basıldığında loginbutton'ı tetiklemek için keyListener
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginbutton.doClick(); // Butona basılmış gibi çalıştır
                }
            }
        };

        // JTextField ve JPasswordField için KeyListener ekle
        isimtextfield.addKeyListener(enterKeyListener);
        sifrepasswordfield.addKeyListener(enterKeyListener);

        // JFrame'e KeyListener ekleyelim
        openf.addKeyListener(enterKeyListener);



        // JFrame'in ve bileşenlerin odaklanmasını sağla
        openf.setFocusable(true);
        openf.requestFocusInWindow();

        openf.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent evt) {
                isimtextfield.requestFocusInWindow();
            }
        });
        openf.setVisible(true);
            }
}
