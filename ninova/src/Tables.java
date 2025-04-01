import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Tables {
    DefaultTableModel tableModel = new DefaultTableModel();
    JTable table = new JTable(tableModel);

    private Connection connection;
    private final Map<String, String> columnMappings;
    private final Map<Integer, Map<String, Object>> changedData; // Değişiklikleri saklamak için map

    public Tables(Connection connection) {
        this.connection = connection;
        this.columnMappings = new HashMap<>();
        this.changedData = new HashMap<>(); // Öğrenci bazlı değişiklikleri saklanacak.


    }

    public void execute() {
        String query = "SELECT * FROM ogrencitakip";

        try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
             ResultSet resultSet = statement.executeQuery(query)) {


            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            Vector<String> columnNames = new Vector<>();

            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            tableModel.setColumnIdentifiers(columnNames);

            while (resultSet.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(resultSet.getObject(i));
                }
                tableModel.addRow(row);
            }


            table.putClientProperty("terminateEditOnFocusLost", true);

            // Tabloyu sıralanabilir hale getir
            TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
            table.setRowSorter(sorter);

// Sütun tiplerine göre sıralayıcıları ekleyelim
            for (int i = 0; i < table.getColumnCount(); i++) {
                if (i == 0 || i == 3) { // Kat ve Salon sütunları sayısal olacak
                    sorter.setComparator(i, (o1, o2) -> {
                        try {
                            return Integer.compare(Integer.parseInt(o1.toString()), Integer.parseInt(o2.toString()));
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    });
                } else if (i >= 3) { // Saat giriş/çıkış sütunları TIME olarak sıralanacak
                    sorter.setComparator(i, (o1, o2) -> {
                        try {
                            Time t1 = Time.valueOf(o1.toString());
                            Time t2 = Time.valueOf(o2.toString());
                            return t1.compareTo(t2);
                        } catch (IllegalArgumentException e) {
                            return 0;
                        }
                    });
                }
            }



            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            // Sütun genişliklerini ayarla
            int[] columnWidths = {
                    80, // sirano
                    50,  // Kat
                    230, // Ad Soyad
                    80,  // Salon
                    120, 120, 120, 120, 120, 120,  // Pazartesi
                    120, 120, 120, 120, 120, 120,  // Salı
                    120, 120, 120, 120, 120, 120,  // Çarşamba
                    120, 120, 120, 120, 120, 120,  // Perşembe
                    120, 120, 120, 120, 120, 120,  // Cuma
                    120, 120, 120, 120, 120, 120   // Cumartesi
            };

            for (int i = 0; i < columnWidths.length && i < table.getColumnCount(); i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
            }


            JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            tableModel.addTableModelListener(e -> {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int column = e.getColumn();
                    String columnName = tableModel.getColumnName(column);
                    Object newValue = tableModel.getValueAt(row, column);
                    Object sirano = tableModel.getValueAt(row, 0);

                    if (columnMappings.containsKey(columnName)) {
                        columnName = columnMappings.get(columnName);
                    }

                    // Değişiklikleri `changedData` map'ine kaydedelim
                    int studentId = Integer.parseInt(sirano.toString());
                    changedData.putIfAbsent(studentId, new HashMap<>());
                    changedData.get(studentId).put(columnName, newValue);

                    // Konsola yazdır (debug için)
                    System.out.println("Değişiklik Algılandı: " + columnName + " -> " + newValue);
                }
            });

            JFrame frame = new JFrame("Öğrenci Takip Tablosu");

            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Kapatma işlevini devre dışı bırakılıyor.

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Kullanıcı pencereyi kapatmaya çalıştığında, işlem onayı göster
                    int choice = JOptionPane.showConfirmDialog(
                            null,
                            "Pencereyi kapatmak istiyor musunuz?",
                            "Kapatma Onayı",
                            JOptionPane.YES_NO_OPTION);

                    if (choice == JOptionPane.NO_OPTION) {
                        return; // Kullanıcı pencereyi kapatmak istemezse işlem sonlanır
                    }

                    // Eğer kullanıcı "Evet" derse, iki seçenekli başka bir dialog gösterelim
                    String[] options = {"Tabloyu Düzenle", "Yoklamaya Devam Et"};
                    int actionChoice = JOptionPane.showOptionDialog(
                            null,
                            "Lütfen yapmak istediğiniz seçeneği seçiniz",
                            "Giriş Başarılı",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (actionChoice == 0) {  // "Tabloyu Düzenle" seçeneği
                        try (Connection connection = DataBaseHelper.getConnection()) {
                            if (connection != null) {
                                Tables table = new Tables(connection);
                                table.execute();
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace(); // veya uygun bir hata yönetimi
                        }
                    } else if (actionChoice == 1) {  // "Yoklamaya Devam Et" seçeneği
                        Attendance.DayTable();
                    }

                    // Pencereyi kapatma işlemi burada yapılır
                    frame.dispose();
                }
            });



            frame.setSize(800, 400);
            frame.add(scrollPane);

            JMenuBar menuBar = new JMenuBar();
            JButton addStudentButton = new JButton("\u2795 Öğrenci Ekle");
            JButton deleteStudentButton = new JButton("\uD83D\uDDD1 Öğrenci Sil");
            JButton saveButton = new JButton("\uD83D\uDCBE Kaydet");

            addStudentButton.addActionListener(e -> AddStudentPanel.createAndShowGUI());
            deleteStudentButton.addActionListener(e ->{
                    DeleteStudentPanel deleteStudentPanel = new DeleteStudentPanel();
                JFrame deleteStudentFrame = new JFrame("Öğrenci Silme Paneli");
                deleteStudentFrame.setSize(600, 400);
                deleteStudentFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                deleteStudentFrame.setLocationRelativeTo(null); // Ortada açılır
                deleteStudentFrame.add(deleteStudentPanel);
                deleteStudentFrame.setVisible(true);

        });
            saveButton.addActionListener(e -> updateAllChanges());

            menuBar.add(addStudentButton);
            menuBar.add(deleteStudentButton);
            menuBar.add(saveButton);
            frame.setJMenuBar(menuBar);

            frame.setVisible(true);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    closeConnection();
                }
            });

        } catch (SQLException e) {
            System.out.println("Sorgu çalıştırılırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Veritabanı bağlantısı kapatıldı.");
            }
        } catch (SQLException e) {
            System.out.println("Bağlantıyı kapatırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateAllChanges() {
        if (changedData.isEmpty()) {
            System.out.println("Kaydedilecek değişiklik yok.");
            return;
        }

        for (Map.Entry<Integer, Map<String, Object>> entry : changedData.entrySet()) {
            int studentId = entry.getKey();
            Map<String, Object> updates = entry.getValue();

            for (Map.Entry<String, Object> updateEntry : updates.entrySet()) {
                String columnName = updateEntry.getKey();
                Object newValue = updateEntry.getValue();
                updateDatabase(columnName, newValue, studentId);
            }
        }

        // Güncelleme tamamlandı, değişiklikleri temizle
        changedData.clear();
        System.out.println("Tüm değişiklikler başarıyla kaydedildi.");
    }

    private void updateDatabase(String columnName, Object newValue, Object sirano) {
        if (columnName == null || columnName.isEmpty()) {
            System.out.println("Geçersiz sütun adı!");
            return;
        }

        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("Bağlantı kapalı! Yeniden bağlanıyor...");
                connection = DriverManager.getConnection(DataBaseHelper.getUrl(), DataBaseHelper.getUsername(), DataBaseHelper.getPassword());
            }
        } catch (SQLException e) {
            System.out.println("Bağlantı yeniden kurulurken hata oluştu: " + e.getMessage());
            return;
        }

        String updateQuery = "UPDATE ogrencitakip SET " + columnName + " = ? WHERE sirano = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            // 🟢 Eğer giriş/çıkış saati ise TIME olarak kaydet
            if (columnName.contains("giris") || columnName.contains("cikis")) {
                try {
                    String timeValue = newValue.toString();
                    if (timeValue.length() == 5) { // HH:MM formatını HH:MM:SS formatına çevir
                        timeValue = timeValue + ":00";
                    }
                    preparedStatement.setTime(1, Time.valueOf(timeValue));
                } catch (IllegalArgumentException e) {
                    preparedStatement.setNull(1, Types.TIME); // Geçersiz saat formatı varsa NULL kaydet
                }
            }
            // 🟢 Eğer "kat" veya "salon" sütunu ise INTEGER olarak kaydet
            else if (columnName.equals("kat") || columnName.equals("salon")) {
                try {
                    if (newValue == null || newValue.toString().trim().isEmpty()) {
                        preparedStatement.setObject(1, null, Types.INTEGER); // NULL değerini INTEGER olarak ayarla
                    } else {
                        String valueStr = newValue.toString().trim();
                        if (valueStr.matches("\\d+")) { // Sadece rakamlardan oluşuyor mu?
                            int numericValue = Integer.parseInt(valueStr);
                            preparedStatement.setInt(1, numericValue);
                        } else {
                            System.out.println("Hata: " + columnName + " için geçersiz sayı formatı!");
                            preparedStatement.setObject(1, null, Types.INTEGER); // Geçersiz değer için NULL ayarla
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Hata: " + columnName + " için geçersiz sayı formatı!");
                    preparedStatement.setObject(1, null, Types.INTEGER);
                }
            }

            // 🟢 Eğer "adsoyad" gibi STRING olan bir sütunsa STRING olarak kaydet
            else if (columnName.equals("ad")) {
                preparedStatement.setString(1, newValue.toString().trim());
            }
            // 🟢 Varsayılan olarak tüm diğer sütunları STRING olarak kaydet
            else {
                preparedStatement.setString(1, newValue.toString());
            }

            preparedStatement.setInt(2, Integer.parseInt(sirano.toString())); // `sirano` ID olarak kullanılıyor

            // 🔍 Debug İçin Konsola Yazdır 🚀
            System.out.println("SQL Sorgusu: " + updateQuery);
            System.out.println("Yeni Değer: " + newValue + " | SiraNo: " + sirano);

            int updatedRows = preparedStatement.executeUpdate();
            if (updatedRows > 0) {
                System.out.println("Veritabanı güncellendi: " + columnName + " = " + newValue);
                reloadTable();
            } else {
                System.out.println("Güncelleme başarısız.");
            }

        } catch (SQLException ex) {
            System.out.println("Veritabanı güncellenirken hata oluştu: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public void reloadTable() {
        String query = "SELECT * FROM ogrencitakip ORDER BY sirano ASC"; // 📌 **Sıralamayı PK'ye göre yap**

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            tableModel.setRowCount(0); // **Tabloyu temizle**

            while (resultSet.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    row.add(resultSet.getObject(i));
                }
                tableModel.addRow(row);
            }

            tableModel.fireTableDataChanged(); // **JTable'i Güncelle**

        } catch (SQLException e) {
            System.out.println("Tablo güncellenirken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
