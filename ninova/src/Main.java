
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    private static Connection connection;

    public static void main(String[] args) {

        try {
            connection = DataBaseHelper.getConnection(); // Veritabanı bağlantısını al

            JFrame openf = new JFrame();
            openf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            openf.setTitle("Ninova Yoklama Programı");

            JPanel openp = new JPanel(new BorderLayout());
            openp.setLayout(null);
            openp.setBackground(Color.white);
            openf.add(openp);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int screenWidth = (int) screenSize.getWidth();
            int screenHeight = (int) screenSize.getHeight();

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
            isimtextfield.setFocusable(true);
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

                if (isim.equals("ninova") && sifre.equals("ninova123")) {
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

                    if (choice == 0) {
                        try (Connection tableConnection = DataBaseHelper.getConnection()) { // Yeni bir bağlantı al
                            if (tableConnection != null) {
                                Tables table = new Tables(tableConnection);
                                table.execute();
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace(); // Hata yönetimi
                            JOptionPane.showMessageDialog(null, "Tablo düzenlenirken bir hata oluştu: " + ex.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);
                        }
                    } else if (choice == 1) {
                        Attendance.DayTable();
                    }
                } else if (isim.equals("ninova") && !sifre.equals("ninova123")) {
                    JOptionPane.showMessageDialog(null, "Şifre Hatalı", "Hata", JOptionPane.ERROR_MESSAGE);
                } else if (!isim.equals("ninova") && sifre.equals("ninova123")) {
                    JOptionPane.showMessageDialog(null, "İsim Hatalı", "Hata", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "İsim ve Şifre Hatalı", "Hata", JOptionPane.ERROR_MESSAGE);
                }
            });

            KeyAdapter enterKeyListener = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        loginbutton.doClick();
                    }
                }
            };

            isimtextfield.addKeyListener(enterKeyListener);
            sifrepasswordfield.addKeyListener(enterKeyListener);
            openf.addKeyListener(enterKeyListener);
            openf.setFocusable(true);
            openf.requestFocusInWindow();

            openf.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent evt) {
                    isimtextfield.requestFocusInWindow();
                }
            });
            openf.setExtendedState(JFrame.MAXIMIZED_BOTH);
            openf.setVisible(true);

        } catch (SQLException e) {
            System.err.println("Veritabanı bağlantısı kurulamadı: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Veritabanı bağlantısı kurulamadı: " + e.getMessage(), "Hata", JOptionPane.ERROR_MESSAGE);

            System.exit(1);
        } finally {

            try {
                if (connection != null) {
                    DataBaseHelper.closeConnection(connection);
                }
            } catch (Exception e) {
                System.err.println("Bağlantı kapatılırken hata oluştu: " + e.getMessage());

            }
        }
    }
}
