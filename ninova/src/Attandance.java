
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import java.text.Collator;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import java.sql.Time;
import java.time.LocalTime;
import java.util.List;

class Attendance {
    private static LocalTime entryStart;
    private static LocalTime exitEnd;
    private static final JCheckBox checkBox = new JCheckBox();

    public static String getCurrentDateAndTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d.MM.yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return currentDateTime.format(dateFormatter) + " " + currentDateTime.getDayOfWeek() + " " + currentDateTime.format(timeFormatter);
    }

    public static void DayTable() {
        String currentDay = LocalDateTime.now().getDayOfWeek().toString();

        try (Connection connection = DataBaseHelper.getConnection()) {
            if (connection == null) {
                JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı.");
                return;
            }

            String query = getQueryForDay(currentDay);
            System.out.println("Bugun " + currentDay);
            System.out.println("Çalıştırılan Sorgu: " + query);

            try (PreparedStatement pstmt = connection.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                String[] columnNames = {"Sıra No", "Kat", "Ad Soyad", "Salon", "Giriş Saati", "Çıkış Saati", "Durum", "Notlar"};
                DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        return (columnIndex == 6) ? Boolean.class : String.class;
                    }
                };

                while (rs.next()) {
                    Object[] row = getRowDataForDay(rs, currentDay, rs);
                    model.addRow(row);
                    System.out.println("Tabloya eklendi: " + Arrays.toString(row));
                }

                JTable table = new JTable(model);
                setupTableAppearance(table, model);
                addCheckboxListener(table, model);
                setupTableSorting(model, table);

                JFrame frame = new JFrame("Yoklama Tablosu - " + currentDay);
                frame.setSize(900, 500);
                frame.setLocationRelativeTo(null);
                frame.setLayout(new BorderLayout());

                table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                JScrollPane scrollPane = new JScrollPane(table);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

                TableColumn adSoyadColumn = table.getColumnModel().getColumn(2);
                adSoyadColumn.setPreferredWidth(200);

                TableColumn notlarColumn = table.getColumnModel().getColumn(7);
                notlarColumn.setPreferredWidth(1350);

                frame.add(scrollPane, BorderLayout.CENTER);
                frame.add(createBottomPanel(table), BorderLayout.SOUTH);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        int choice = JOptionPane.showConfirmDialog(null, "Pencereyi kapatmak istiyor musunuz?", "Kapatma Onayı", JOptionPane.YES_NO_OPTION);
                        if (choice == JOptionPane.NO_OPTION) return;

                        String[] options = {"Tabloyu Düzenle", "Yoklamaya Devam Et"};
                        int actionChoice = JOptionPane.showOptionDialog(null, "Lütfen yapmak istediğiniz seçeneği seçiniz", "Giriş Başarılı", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

                        if (actionChoice == 0) {
                            try (Connection connection = DataBaseHelper.getConnection()) {
                                if (connection != null) {
                                    Tables table = new Tables(connection);
                                    table.execute();
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        } else if (actionChoice == 1) {
                            DayTable();
                        }

                        frame.dispose();
                    }
                });

                frame.setVisible(true);
                System.out.println("Frame gösterildi!");

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Veri çekme hatası: " + e.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Veritabanı bağlantı hatası: " + e.getMessage());
        }
    }

    private static void setupTableAppearance(JTable table, DefaultTableModel model) {
        table.getColumnModel().getColumn(6).setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component cellComponent = table.getDefaultRenderer(Boolean.class) // Boolean sınıfı için default renderer'ı al
                        .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                boolean isChecked = false;
                if (value instanceof Boolean) {
                    isChecked = (Boolean) value;
                }

                String entryStartString = (String) table.getValueAt(row, 4);
                String exitEndString = (String) table.getValueAt(row, 5);

                LocalTime entryStart = parseLocalTime(entryStartString, LocalTime.MIN);
                LocalTime exitEnd = parseLocalTime(exitEndString, LocalTime.MAX);

                cellComponent.setBackground(getColorForCell(isChecked, entryStart, exitEnd));
                return cellComponent;
            }
        });
    }

    private static LocalTime parseLocalTime(String timeString, LocalTime defaultValue) {
        if (timeString != null && !timeString.equals("-")) {
            try {
                return LocalTime.parse(timeString);
            } catch (DateTimeParseException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static Color getColorForCell(boolean isChecked, LocalTime entryStart, LocalTime exitEnd) {
        LocalTime defaultEntry = LocalTime.parse("00:00");
        LocalTime endOfDayThreshold = LocalTime.parse("23:59").plusSeconds(59).plusNanos(999999999);
        LocalTime now = LocalTime.now();

        System.out.println("isChecked: " + isChecked + ", entryStart: " + entryStart + ", exitEnd: " + exitEnd + ", now: " + now);
        System.out.println("defaultEntry.equals(entryStart): " + defaultEntry.equals(entryStart));
        System.out.println("defaultExit.equals(exitEnd): " + endOfDayThreshold.equals(exitEnd));

        if (isChecked) {
            return Color.green;
        } else {
            if (entryStart != null && exitEnd != null) {
                if (entryStart.equals(defaultEntry) && (exitEnd.isAfter(endOfDayThreshold.minusSeconds(1)) || exitEnd.equals(endOfDayThreshold))) {
                    return Color.cyan;
                } else if (entryStart.isBefore(LocalTime.now()) && exitEnd.isAfter(LocalTime.now())) {
                    return Color.red;
                } else if (entryStart.isAfter(LocalTime.now()) || exitEnd.isBefore(LocalTime.now())) {
                    return Color.yellow;
                } else {
                    return Color.lightGray;
                }
            } else {
                return Color.cyan;
            }
        }
    }

    private static String getQueryForDay(String day) {
        Map<String, String[]> dayMapping = Map.of(
                "MONDAY", new String[]{"pazartesigirissabah", "pazartesigirisoglen", "pazartesigirisaksam", "pazartesicikissabah", "pazartesicikisoglen", "pazartesicikisaksam"},
                "TUESDAY", new String[]{"saligirissabah", "saligirisoglen", "saligirisaksam", "salicikissabah", "salicikisoglen", "salicikisaksam"},
                "WEDNESDAY", new String[]{"carsambagirissabah", "carsambagirisoglen", "carsambagirisaksam", "carsambacikissabah", "carsambacikisoglen", "carsambacikisaksam"},
                "THURSDAY", new String[]{"persembegirissabah", "persembegirisoglen", "persembegirisaksam", "persembecikissabah", "persembecikisoglen", "persembecikisaksam"},
                "FRIDAY", new String[]{"cumagirissabah", "cumagirisoglen", "cumagirisaksam", "cumacikissabah", "cumacikisoglen", "cumacikisaksam"},
                "SATURDAY", new String[]{"cumartesigirissabah", "cumartesigirisoglen", "cumartesigirisaksam", "cumartesicikissabah", "cumartesicikisoglen", "cumartesicikisaksam"}
        );

        String[] columns = dayMapping.getOrDefault(day, new String[]{});
        if (columns.length == 0) {
            JOptionPane.showMessageDialog(null, "Hata: Günlük zaman sütunları tanımlanmamış!");
            return "SELECT sirano, kat, adsoyad, salon, checkbox_state FROM ogrencitakip";
        }

        LocalTime now = LocalTime.now();
        String entryColumn, exitColumn;

        if (now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(12, 30))) {
            entryColumn = columns[0];
            exitColumn = columns[3];
        } else if (now.isAfter(LocalTime.of(12, 30)) && now.isBefore(LocalTime.of(18, 20))) {
            entryColumn = columns[1];
            exitColumn = columns[4];
        } else {
            entryColumn = columns[2];
            exitColumn = columns[5];
        }

        // checkbox_state'i sorguya dahil ediyoruz
        return String.format("SELECT sirano, kat, adsoyad, salon, %s, %s, checkbox_state FROM ogrencitakip", entryColumn, exitColumn);
    }

    private static Object[] getRowDataForDay(ResultSet rs, String day, ResultSet fullRs) throws SQLException {
        Object[] row = new Object[8];

        row[0] = rs.getInt("sirano");
        row[1] = rs.getString("kat");
        row[2] = rs.getString("adsoyad");
        row[3] = rs.getString("salon");

        int colIndex = 5;

        // LocalTime değişkenlerini önceden tanımla
        LocalTime entryStart = null;
        LocalTime exitEnd = null;

        if (rs.getMetaData().getColumnCount() >= colIndex + 2) { // checkbox_state 때문에 2로 변경
            String entryStartStr = formatTime(rs, colIndex);
            String exitEndStr = formatTime(rs, colIndex + 1);

            entryStart = parseLocalTime(entryStartStr, null);
            exitEnd = parseLocalTime(exitEndStr, null);

            row[4] = entryStartStr.equals("-") ? "-" : entryStartStr;
            row[5] = exitEndStr.equals("-") ? "-" : exitEndStr;
        } else {
            row[4] = "Belirtilmemiş";
            row[5] = "Belirtilmemiş";
        }

        row[6] = rs.getBoolean("checkbox_state"); // Checkbox durumunu doğrudan al
        row[7] = "";

        return row;
    }

    private static String formatTime(ResultSet rs, int columnIndex) throws SQLException {
        Time time = rs.getTime(columnIndex);
        return (time != null) ? time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-";
    }

    private static void setupTableListener(JTable table, DefaultTableModel model) {
        model.addTableModelListener(e -> {
            if (e.getColumn() == 6) {
                int row = e.getFirstRow();
                Boolean checked = (Boolean) model.getValueAt(row, 6);
                String name = (String) model.getValueAt(row, 2);

                if (checked && name.contains("!!!")) {
                    model.setValueAt(name.replace("!!!", "").trim(), row, 2);
                }
            }
        });
    }

    private static void setupTableSorting(DefaultTableModel model, JTable table) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        sorter.setComparator(0, Comparator.comparingInt(o -> Integer.parseInt(o.toString())));
        sorter.setComparator(1, Comparator.comparingInt(o -> Integer.parseInt(o.toString())));
        sorter.setComparator(3, (o1, o2) -> {
            try {
                return Integer.compare(Integer.parseInt(o1.toString()), Integer.parseInt(o2.toString()));
            } catch (NumberFormatException e) {
                return o1.toString().compareTo(o2.toString());
            }
        });

        Collator collator = Collator.getInstance(new Locale("tr", "TR"));
        collator.setStrength(Collator.PRIMARY);
        sorter.setComparator(2, (o1, o2) -> collator.compare(o1.toString(), o2.toString()));
    }

    private static Timer debounceTimer;
    private static int debouncedStudentId;
    private static boolean debouncedIsChecked;

    private static void addCheckboxListener(JTable table, DefaultTableModel model) {
        table.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == 6) {
                int row = e.getFirstRow();
                int studentId = (int) model.getValueAt(row, 0);
                boolean isChecked = (boolean) model.getValueAt(row, 6);
                updateCheckboxValueAsync(studentId, isChecked);
            }
        });
    }

    private static void updateCheckboxValueAsync(int studentId, boolean isChecked) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                updateCheckboxValue(studentId, isChecked); // Veritabanı güncelleme işlemi
                return null;
            }

            @Override
            protected void done() {
                // İsteğe bağlı: İşlem tamamlandığında UI'da geri bildirim gösterebilirsiniz
            }
        };
        worker.execute();
    }

    private static void updateCheckboxValue(int studentId, boolean isChecked) {
        String sql = "UPDATE ogrencitakip SET checkbox_state = ? WHERE sirano = ?";
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = DataBaseHelper.getConnection();
            pstmt = connection.prepareStatement(sql);
            pstmt.setInt(1, isChecked ? 1 : 0);
            pstmt.setInt(2, studentId);
            pstmt.executeUpdate();
            System.out.println("Güncellendi: " + studentId + " -> " + isChecked);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Checkbox güncelleme hatası: " + e.getMessage());
        } finally {
            DataBaseHelper.closeConnection(connection);
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static JPanel createBottomPanel(JTable table) {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportButton = new JButton("PDF Olarak Kaydet");
        exportButton.addActionListener(e -> PDFExporter.exportpdf(table));
        buttonPanel.add(exportButton);

        JButton printButton = new JButton("Yazdır");
        printButton.addActionListener(e -> printTable(table));
        buttonPanel.add(printButton);

        JLabel searchLabel = new JLabel("Ara:");
        buttonPanel.add(searchLabel);

        JTextField searchField = new JTextField(15);
        buttonPanel.add(searchField);

        JButton clearButton = new JButton("Yoklamayı Sıfırla");
        buttonPanel.add(clearButton);
        clearButton.addActionListener(e -> resetAllCheckboxesInDatabase(table));

        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                searchTable(searchField.getText(), table);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) {
                    searchField.requestFocus();
                }
            }
        });

        JLabel timeLabel = new JLabel();
        timeLabel.setFont(new Font("Tahoma", Font.PLAIN, 12));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        Timer timer = new Timer(1000, e -> SwingUtilities.invokeLater(() -> timeLabel.setText(getCurrentDateAndTime())));
        timer.start();

        JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        timePanel.add(timeLabel);

        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bottomPanel.add(buttonPanel, BorderLayout.WEST);
        bottomPanel.add(timePanel, BorderLayout.EAST);

        return bottomPanel;
    }

    private static void resetAllCheckboxesInDatabase(JTable table) {
        try (Connection connection = DataBaseHelper.getConnection()) {
            if (connection == null) {
                System.out.println("Veritabanına bağlanılamadı.");
                return;
            }
            System.out.println("Veritabanına başarıyla bağlanıldı.");
            String sql = "UPDATE ogrencitakip SET checkbox_state = 0";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int affectedRows = pstmt.executeUpdate();
                System.out.println("Yoklama sıfırlandı: " + affectedRows + " satır güncellendi.");
            }

            // 🚀 Bu satırı ekliyoruz: tablonun modelini güncelle
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0); // önce tabloyu temizle

            // Şimdi yeni verilerle doldur:
            String currentDay = LocalDateTime.now().getDayOfWeek().toString();
            String query = getQueryForDay(currentDay);
            try (PreparedStatement pstmt = connection.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    Object[] row = getRowDataForDay(rs, currentDay, rs);
                    model.addRow(row);
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Veri yenileme hatası: " + e.getMessage());
            }

        } catch (SQLException e) {
            System.out.println("Veritabanına bağlanırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void searchTable(String query, JTable table) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        if (query.trim().isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            RowFilter<TableModel, Object> rf = RowFilter.regexFilter("(?i)" + query);
            sorter.setRowFilter(rf);
        }
    }

    private static void printTable(JTable table) {
        // Kullanıcıya seçenek sun
        String[] options = {"Tümünü Yazdır", "Salona Göre Filtrele"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "Yazdırma seçeneklerinden birini seçin:",
                "Yazdırma Seçenekleri",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == JOptionPane.CLOSED_OPTION) {
            return; // Kullanıcı pencereyi kapatırsa işlem iptal edilir
        }

        String salon = null;
        if (choice == 1) { // "Salona Göre Filtrele" seçildiyse salon numarası sor
            salon = JOptionPane.showInputDialog(null, "Yazdırmak istediğiniz salon numarasını girin:");
            if (salon == null || salon.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Geçersiz salon numarası!");
                return;
            }
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        String finalSalon = salon;
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            // Sayfa düzeni ve yazı tipi ayarları
            graphics.setFont(new Font("Arial", Font.PLAIN, 10)); // Genel yazı tipi
            int yPosition = 50; // Başlangıç y pozisyonu
            int xPosition = 50; // Başlangıç x pozisyonu
            int rowHeight = 20; // Satır yüksekliği

            DefaultTableModel model = (DefaultTableModel) table.getModel();
            List<List<String>> filteredData = new ArrayList<>();

            if (finalSalon != null) { // Filtreleme yapılıyorsa
                int columnIndex = model.findColumn("Salon");

                for (int row = 0; row < model.getRowCount(); row++) {
                    String currentSalon = (String) model.getValueAt(row, columnIndex);
                    if (currentSalon.equals(finalSalon)) {
                        List<String> rowData = new ArrayList<>();
                        for (int col = 0; col < model.getColumnCount(); col++) {
                            rowData.add((String) model.getValueAt(row, col));
                        }
                        filteredData.add(rowData);
                    }
                }
            } else { // Tümünü yazdır
                for (int row = 0; row < model.getRowCount(); row++) {
                    List<String> rowData = new ArrayList<>();
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        rowData.add((String) model.getValueAt(row, col));
                    }
                    filteredData.add(rowData);
                }
            }

            // Sütun başlıklarını al ve kalın yap
            String[] columnNames = new String[model.getColumnCount()];
            for (int i = 0; i < model.getColumnCount(); i++) {
                columnNames[i] = model.getColumnName(i);
            }

            // Başlıkları yazdır
            graphics.setFont(new Font("Arial", Font.BOLD, 12)); // Başlıklar için kalın font
            for (int col = 0; col < columnNames.length; col++) {
                graphics.drawString(columnNames[col], xPosition + (col * 100), yPosition);
            }

            yPosition += rowHeight; // Başlıkların altına geçiş

            // Filtrelenmiş veriyi yazdır
            graphics.setFont(new Font("Arial", Font.PLAIN, 10)); // Veriler için normal font
            for (int row = 0; row < filteredData.size(); row++) {
                List<String> rowData = filteredData.get(row);
                for (int col = 0; col < rowData.size(); col++) {
                    // Durum sütununda checkbox'ı gizle
                    String value = rowData.get(col);
                    if (col == model.findColumn("Durum")) {
                        value = value.equals("true") ? "Var" : "Yok"; // Durum metni olarak yazdır
                    }
                    graphics.drawString(value, xPosition + (col * 100), yPosition);
                }
                yPosition += rowHeight;
            }

            // Sayfa numarası ekleyelim
            graphics.drawString("Sayfa: " + (pageIndex + 1), 300, 800);

            return Printable.PAGE_EXISTS;
        });

        try {
            job.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(null, "Yazdırma hatası: " + ex.getMessage());
        }
    }
    }
