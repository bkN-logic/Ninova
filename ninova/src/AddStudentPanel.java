
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;

public class AddStudentPanel {

    public static void createAndShowGUI() {
        JFrame frame = new JFrame("Öğrenci Ekle");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setLayout(new GridLayout(0, 2));

        String[] labels = {"Kat", "Ad Soyad", "Salon", "Pazartesi Giriş Sabah", "Pazartesi Çıkış Sabah",
                "Pazartesi Giriş Öğlen", "Pazartesi Çıkış Öğlen", "Pazartesi Giriş Akşam", "Pazartesi Çıkış Akşam",
                "Salı Giriş Sabah", "Salı Çıkış Sabah", "Salı Giriş Öğlen", "Salı Çıkış Öğlen", "Salı Giriş Akşam", "Salı Çıkış Akşam",
                "Çarşamba Giriş Sabah", "Çarşamba Çıkış Sabah", "Çarşamba Giriş Öğlen", "Çarşamba Çıkış Öğlen", "Çarşamba Giriş Akşam", "Çarşamba Çıkış Akşam",
                "Perşembe Giriş Sabah", "Perşembe Çıkış Sabah", "Perşembe Giriş Öğlen", "Perşembe Çıkış Öğlen", "Perşembe Giriş Akşam", "Perşembe Çıkış Akşam",
                "Cuma Giriş Sabah", "Cuma Çıkış Sabah", "Cuma Giriş Öğlen", "Cuma Çıkış Öğlen", "Cuma Giriş Akşam", "Cuma Çıkış Akşam",
                "Cumartesi Giriş Sabah", "Cumartesi Çıkış Sabah", "Cumartesi Giriş Öğlen", "Cumartesi Çıkış Öğlen", "Cumartesi Giriş Akşam", "Cumartesi Çıkış Akşam"};

        JTextField[] textFields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            frame.add(new JLabel(labels[i]));
            textFields[i] = new JTextField();
            frame.add(textFields[i]);
        }

        JButton saveButton = new JButton("Kaydet");
        saveButton.addActionListener(e -> {
            try (Connection conn = DataBaseHelper.getConnection()) { // Veritabanı bağlantısını DataBaseHelper'dan al
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
                    JOptionPane.showMessageDialog(null, "Veri girisi sirasinda bir hata olustu! " + ex.getMessage(), "Hata!", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace(); // Ek olarak hatayı konsola yazdır
                JOptionPane.showMessageDialog(null, "Veritabanı bağlantısı kurulamadı! " + ex.getMessage(), "Hata!", JOptionPane.ERROR_MESSAGE);
                // Uygulamanın devam etmesi uygun değilse, burada sonlandırılabilir:
                // System.exit(1);
                throw new RuntimeException(ex); // Daha üst katmana fırlat
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
