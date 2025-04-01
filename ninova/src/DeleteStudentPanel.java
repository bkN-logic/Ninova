import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeleteStudentPanel extends JPanel {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton deleteSelectedButton;

    public DeleteStudentPanel() {
        setLayout(new BorderLayout());

        // Tablo modelini oluştur
        tableModel = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0 -> Integer.class; // Sirano int olarak sıralanacak
                    case 1 -> String.class;  // Ad soyad string olarak sıralanacak
                    case 2 -> Boolean.class; // Checkbox boolean olarak sıralanacak
                    default -> Object.class;
                };
            }
        };

        tableModel.addColumn("Sirano");
        tableModel.addColumn("Ad Soyad");
        tableModel.addColumn("Sil");

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Sıralama eklemek için sorter kullan
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Öğrenci verilerini yükle
        loadStudentsData();

        // Seçili öğrencileri silme butonu
        deleteSelectedButton = new JButton("Seçili Öğrencileri Sil");
        deleteSelectedButton.addActionListener(e -> deleteSelectedStudents());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(deleteSelectedButton);

        // Arama kutusunu oluştur
        JTextField searchField = new JTextField(15);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // Kullanıcı metni değiştirdiğinde arama fonksiyonunu çağır
                searchTable(searchField.getText());
            }
        });

        bottomPanel.add(new JLabel("Arama:"));
        bottomPanel.add(searchField);


        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadStudentsData() {
        tableModel.setRowCount(0); // Mevcut verileri temizle

        try (Connection conn = DriverManager.getConnection(
                DataBaseHelper.getUrl(),
                DataBaseHelper.getUsername(),
                DataBaseHelper.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sirano, adsoyad FROM ogrencitakip")) {

            while (rs.next()) {
                int sirano = rs.getInt("sirano");
                String adsoyad = rs.getString("adsoyad");
                tableModel.addRow(new Object[]{sirano, adsoyad, false}); // Checkbox başlangıçta false
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Veritabanı hatası!", "Hata", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedStudents() {
        List<Integer> selectedStudents = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean isChecked = (Boolean) tableModel.getValueAt(i, 2);
            if (isChecked) {
                selectedStudents.add((Integer) tableModel.getValueAt(i, 0));
            }
        }

        if (selectedStudents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Silmek için en az bir öğrenci seçmelisiniz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Seçili öğrencileri silmek istediğinizden emin misiniz?", "Onay", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(
                    DataBaseHelper.getUrl(),
                    DataBaseHelper.getUsername(),
                    DataBaseHelper.getPassword())) {

                PreparedStatement stmt = conn.prepareStatement("DELETE FROM ogrencitakip WHERE sirano = ?");
                for (int sirano : selectedStudents) {
                    stmt.setInt(1, sirano);
                    stmt.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Seçili öğrenciler başarıyla silindi!", "Başarılı", JOptionPane.INFORMATION_MESSAGE);
                loadStudentsData(); // Tabloyu güncelle
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Veritabanı hatası!", "Hata", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void searchTable(String query) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        if (query.trim().isEmpty()) {
            sorter.setRowFilter(null); // Eğer arama kutusu boşsa, tüm satırları göster
        } else {
            RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + query); // Arama sorgusuna göre filtrele
            sorter.setRowFilter(rf); // Filtreyi uygula
        }
    }
}
