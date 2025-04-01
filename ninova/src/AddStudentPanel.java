import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;

public class AddStudentPanel {

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Ogrenci Ekle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setLayout(new GridLayout(0, 2));

        String[] labels = {"Kat", "Ad Soyad", "Salon", "Pazartesi Giris Sabah", "Pazartesi Cikis Sabah",
                "Pazartesi Giris Oglen", "Pazartesi Cikis Oglen", "Pazartesi Giris Aksam", "Pazartesi Cikis Aksam",
                "Sali Giris Sabah", "Sali Cikis Sabah", "Sali Giris Oglen", "Sali Cikis Oglen", "Sali Giris Aksam", "Sali Cikis Aksam",
                "Carsamba Giris Sabah", "Carsamba Cikis Sabah", "Carsamba Giris Oglen", "Carsamba Cikis Oglen", "Carsamba Giris Aksam", "Carsamba Cikis Aksam",
                "Persembe Giris Sabah", "Persembe Cikis Sabah", "Persembe Giris Oglen", "Persembe Cikis Oglen", "Persembe Giris Aksam", "Persembe Cikis Aksam",
                "Cuma Giris Sabah", "Cuma Cikis Sabah", "Cuma Giris Oglen", "Cuma Cikis Oglen", "Cuma Giris Aksam", "Cuma Cikis Aksam",
                "Cumartesi Giris Sabah", "Cumartesi Cikis Sabah", "Cumartesi Giris Oglen", "Cumartesi Cikis Oglen", "Cumartesi Giris Aksam", "Cumartesi Cikis Aksam"};

        JTextField[] textFields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            frame.add(new JLabel(labels[i]));
            textFields[i] = new JTextField();
            frame.add(textFields[i]);
        }

        JButton saveButton = new JButton("Kaydet");
        saveButton.addActionListener(e -> {
            try (Connection conn = DriverManager.getConnection(DataBaseHelper.getUrl(), DataBaseHelper.getUsername(), DataBaseHelper.getPassword())) {
                String sql = "INSERT INTO ogrencitakip (kat, adsoyad, salon, pazartesigirissabah, pazartesicikissabah, pazartesigirisoglen, pazartesicikisoglen, pazartesigirisaksam, pazartesicikisaksam, " +
                        "saligirissabah, salicikissabah, saligirisoglen, salicikisoglen, saligirisaksam, salicikisaksam, " +
                        "carsambagirissabah, carsambacikissabah, carsambagirisoglen, carsambacikisoglen, carsambagirisaksam, carsambacikisaksam, " +
                        "persembegirissabah, persembecikissabah, persembegirisoglen, persembecikisoglen, persembegirisaksam, persembecikisaksam, " +
                        "cumagirissabah, cumacikissabah, cumagirisoglen, cumacikisoglen, cumagirisaksam, cumacikisaksam, " +
                        "cumartesigirissabah, cumartesicikissabah, cumartesigirisoglen, cumartesicikisoglen, cumartesigirisaksam, cumartesicikisaksam) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (int i = 0; i < textFields.length; i++) {
                        String value = textFields[i].getText().isEmpty() ? null : textFields[i].getText();

                        // INTEGER alanlar için dönüşüm
                        if (i == 0 || i == 2) {  // kat ve salon için
                            if (value != null && !value.isEmpty()) {
                                try {
                                    stmt.setInt(i + 1, Integer.parseInt(value));  // INTEGER dönüşümü
                                } catch (NumberFormatException ex) {
                                    stmt.setNull(i + 1, java.sql.Types.INTEGER);  // Hatalı giriş varsa NULL gönder
                                }
                            } else {
                                stmt.setNull(i + 1, java.sql.Types.INTEGER);  // Null değeri
                            }
                        } else {
                            // Zaman verileri için HH:mm -> HH:mm:ss dönüşümü
                            if (value != null && value.matches("\\d{2}:\\d{2}")) {
                                value = value + ":00";  // Saniye kısmını ekle
                            }

                            // TIME türüne dönüştürme
                            if (i > 2) {  // Zaman alanları genellikle 3. indexten sonra başlar
                                if (value != null) {
                                    try {
                                        stmt.setTime(i + 1, Time.valueOf(value));  // Zaman değeri için dönüştürme
                                    } catch (IllegalArgumentException ex) {
                                        stmt.setNull(i + 1, java.sql.Types.TIME);  // Hatalı formatta veri varsa NULL gönder
                                    }
                                } else {
                                    stmt.setNull(i + 1, java.sql.Types.TIME);  // Null değeri
                                }
                            } else {
                                stmt.setString(i + 1, value);  // Diğer alanlar için String olarak ekle
                            }
                        }
                    }
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Veri Girisi Basarili!", "Basarili!", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Veri girisi sirasinda bir hata olustu!", "Error!", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        JButton clearButton = new JButton("Temizle");
        clearButton.addActionListener(e -> {
            for (JTextField textField : textFields) {
                textField.setText("");
            }
        });

        frame.add(saveButton);
        frame.add(clearButton);
        frame.setVisible(true);
    }
}
