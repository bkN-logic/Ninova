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
        Connection connection = DataBaseHelper.getConnection();
        if (connection == null) {
            JOptionPane.showMessageDialog(null, "Veritabanına bağlanılamadı.");
            return;
        }

        String query = getQueryForDay(currentDay);
        System.out.println("Bugun " + currentDay);

        System.out.println("Çalıştırılan Sorgu: " + query);
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            String[] columnNames = {"Sıra No", "Kat", "Ad Soyad", "Salon", "Giriş Saati", "Çıkış Saati", "Durum", "Notlar"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return (columnIndex == 6) ? Boolean.class : String.class;
                }
            };

            while (rs.next()) {

                Object[] row = getRowDataForDay(rs, currentDay);
                model.addRow(row);
                System.out.println("Tabloya eklendi: " + Arrays.toString(row));
            }

            JTable table = new JTable(model);
            for (int i = 0; i < model.getRowCount(); i++) {
                int studentId = (int) model.getValueAt(i, 0); // Öğrencinin ID'si veya sıra numarası
                boolean isChecked = getCheckboxValueForStudent(studentId);
                model.setValueAt(isChecked, i, 6);
            }


            table.getColumnModel().getColumn(6).setCellRenderer(new TableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    // Varsayılan component'i al
                    Component cellComponent = table.getDefaultRenderer(value != null ? value.getClass() : String.class)
                            .getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                    // Eğer value null ise, uygun bir değer ata
                    if (value == null) {
                        value = Boolean.FALSE; // Null değer için varsayılan olarak işaretli değil (false) bir değer kullan
                    }

                    Boolean isChecked = (Boolean) value;

                    // Öğrencinin giriş ve çıkış saatlerini al (4. ve 5. indexte)
                    String entryStartString = (String) table.getValueAt(row, 4); // Giriş saati (4. index)
                    String exitEndString = (String) table.getValueAt(row, 5);   // Çıkış saati (5. index)

                    LocalTime entryStart = null;
                    LocalTime exitEnd = null;

                    // Giriş saati geçerli değilse varsayılan bir değer atama
                    if (entryStartString != null && !entryStartString.equals("-")) {
                        try {
                            entryStart = LocalTime.parse(entryStartString);
                        } catch (DateTimeParseException e) {
                            // Hata durumunda işlemi atla veya uygun bir işlem yap
                            entryStart = LocalTime.MIN;  // Varsayılan değer: en erken saat (00:00)
                        }
                    }

                    // Çıkış saati geçerli değilse varsayılan bir değer atama
                    if (exitEndString != null && !exitEndString.equals("-")) {
                        try {
                            exitEnd = LocalTime.parse(exitEndString);
                        } catch (DateTimeParseException e) {
                            // Hata durumunda işlemi atla veya uygun bir işlem yap
                            exitEnd = LocalTime.MAX;  // Varsayılan değer: en geç saat (23:59)
                        }
                    }

                    // Eğer giriş saati ve çıkış saati geçerli ise işlemi yap
                    if (Boolean.TRUE.equals(isChecked)) {
                        cellComponent.setBackground(Color.green);
                    } else {
                        // Adım 2: Eğer checkbox işaretli değilse, giriş ve çıkış saatlerine göre renk belirle
                        if (entryStart != null && exitEnd != null) {
                            if (entryStart.isBefore(LocalTime.now()) && exitEnd.isAfter(LocalTime.now())) {
                                // Öğrenci giriş yapmış ve çıkış yapmamışsa, siyah
                                cellComponent.setBackground(Color.red);
                            } else if (entryStart.isAfter(LocalTime.now()) || exitEnd.isBefore(LocalTime.now())) {
                                // Giriş saati gelmemiş veya çıkış saati geçmişse, kırmızı
                                cellComponent.setBackground(Color.yellow);
                            }
                        } else {
                            // Saatler geçerli değilse (null veya geçersiz format), varsayılan renk
                            cellComponent.setBackground(Color.cyan);
                        }
                    }

                    return cellComponent;
                }
            });



            setupTableSorting(model, table);
            System.out.println("Frame oluşturuluyor...");

            JFrame frame = new JFrame("Yoklama Tablosu - " + currentDay);
            frame.setSize(900, 500);
            frame.setLocationRelativeTo(null);
            frame.setLayout(new BorderLayout());
            addCheckboxListener(table, model);


            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            TableColumn adSoyadColumn = table.getColumnModel().getColumn(2);
            adSoyadColumn.setPreferredWidth(200);

            TableColumn notlarColumn = table.getColumnModel().getColumn(7);
            notlarColumn.setPreferredWidth(1350);


            frame.add(new JScrollPane(table), BorderLayout.CENTER);
            frame.add(createBottomPanel(table), BorderLayout.SOUTH);


            // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Kapatma işlevini devre dışı bırak

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


                frame.setVisible(true);
                System.out.println("Frame gösterildi!");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Veri çekme hatası: " + e.getMessage());
        }
    }

    private static boolean getCheckboxValueForStudent(int studentId) {
        boolean isChecked = false; // Varsayılan olarak false (checkbox işaretli değil)

        // Veritabanı bağlantısını al
        try (Connection connection = DataBaseHelper.getConnection()) {
            if (connection == null) {
                System.out.println("Veritabanına bağlanılamadı.");
                return false;
            }

            // SQL sorgusu ile checkbox_state değerini al
            String sql = "SELECT checkbox_state FROM ogrencitakip WHERE sirano = ?";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                // Sorguya öğrenci ID'sini ekle
                stmt.setInt(1, studentId);

                // Sorguyu çalıştır ve sonucu al
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Eğer sonuç varsa, checkbox_state değerini al ve boolean'a çevir
                        int checkboxState = rs.getInt("checkbox_state"); // 0 veya 1 gelir
                        isChecked = (checkboxState == 1); // 1 ise true, 0 ise false
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL Hatası: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (SQLException e) {
            System.out.println("Veritabanına bağlanırken hata oluştu: " + e.getMessage());
            e.printStackTrace();
        }

        return isChecked;
    }

    private static Map<Integer, Boolean> getAllCheckboxStates() {
        Map<Integer, Boolean> checkboxMap = new HashMap<>();

        String sql = "SELECT sirano, checkbox_state FROM ogrencitakip";

        try (Connection connection = DataBaseHelper.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int studentId = rs.getInt("sirano");
                boolean isChecked = rs.getInt("checkbox_state") == 1;
                checkboxMap.put(studentId, isChecked);
            }

            System.out.println("Checkbox_state değerleri başarıyla yüklendi.");

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return checkboxMap;
    }



    private static void updateCheckboxValue(int studentId, boolean isChecked) {
        String sql = "UPDATE ogrencitakip SET checkbox_state = ? WHERE sirano = ?";

        try (Connection connection = DataBaseHelper.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, isChecked ? 1 : 0);
            stmt.setInt(2, studentId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Checkbox değeri güncellendi: Öğrenci ID " + studentId);
            } else {
                System.out.println("Güncelleme başarısız. Öğrenci ID bulunamadı.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
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

        String[] columns = dayMapping.getOrDefault(day, new String[] {});
        if (columns.length == 0) {
            JOptionPane.showMessageDialog(null, "Hata: Günlük zaman sütunları tanımlanmamış!");
            return "SELECT sirano, kat, adsoyad, salon FROM ogrencitakip";
        }


        LocalTime now = LocalTime.now();
        String entryColumn, exitColumn;

        if (now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(12, 30))) {
            entryColumn = columns[0]; // Sabah giriş
            exitColumn = columns[3];  // Sabah çıkış
        } else if (now.isAfter(LocalTime.of(12, 30)) && now.isBefore(LocalTime.of(18, 20))) {
            entryColumn = columns[1]; // Öğlen giriş
            exitColumn = columns[4];  // Öğlen çıkış
        } else {
            entryColumn = columns[2]; // Akşam giriş
            exitColumn = columns[5];  // Akşam çıkış
        }

        return String.format("SELECT sirano, kat, adsoyad, salon, %s, %s, %s, %s FROM ogrencitakip",
                entryColumn, exitColumn, entryColumn, exitColumn);

    }

    private static Object[] getRowDataForDay(ResultSet rs, String day) throws SQLException {
        Object[] row = new Object[8]; // Yeni sütun sayısına uygun dizi

        row[0] = rs.getInt("sirano");
        row[1] = rs.getString("kat");
        row[2] = rs.getString("adsoyad");
        row[3] = rs.getString("salon");

        System.out.println("salon: " + row[3]);

        int colIndex = 5; // Giriş-çıkış saatleri sütunlarının başlangıç indeksi

        // LocalTime değişkenlerini önceden tanımla
        LocalTime entryStart = null;
        LocalTime exitEnd = null;

        if (rs.getMetaData().getColumnCount() >= colIndex + 3) {
            String entryStartStr = formatTime(rs, colIndex);
            String exitEndStr = formatTime(rs, colIndex + 3);

            // Eğer "-" değilse LocalTime'a çevir
            if (!entryStartStr.equals("-")) {
                entryStart = LocalTime.parse(entryStartStr);
            }
            if (!exitEndStr.equals("-")) {
                exitEnd = LocalTime.parse(exitEndStr);
            }

            row[4] = entryStartStr.equals("-") ? "-" : entryStartStr; // Giriş saati
            row[5] = exitEndStr.equals("-") ? "-" : exitEndStr; // Çıkış saati
        } else {
            row[4] = "Belirtilmemiş";
            row[5] = "Belirtilmemiş";
        }
        
        row[7] = "";    // Notlar sütunu

        return row;
    }



    private static String formatTime(ResultSet rs, int columnIndex) throws SQLException {
        Time time = rs.getTime(columnIndex);
        return (time != null) ? time.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "-";
    }





    private static void setupTableListener(JTable table, DefaultTableModel model) {
        model.addTableModelListener(e -> {
            if (e.getColumn() == 6) { // Checkbox değiştiğinde çalıştır
                int row = e.getFirstRow();
                Boolean checked = (Boolean) model.getValueAt(row, 6);
                String name = (String) model.getValueAt(row, 2);

                if (checked && name.contains("!!!")) {
                    name = name.replace("!!!", "").trim();  // "!!!" işaretini kaldır
                    model.setValueAt(name, row, 2);  // Ad Soyad hücresini güncelle
                }
            }
        });
    }


    private static void setupTableSorting(DefaultTableModel model, JTable table) {
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Sıralama işlemi yapılacak her sütun için uygun sıralama fonksiyonu
        sorter.setComparator(0, Comparator.comparingInt(o -> Integer.parseInt(o.toString()))); // Sıra No (sayısal)
        sorter.setComparator(1, Comparator.comparingInt(o -> Integer.parseInt(o.toString()))); // Kat (sayısal)
        sorter.setComparator(3, (o1, o2) -> { // Salon (sayısal)
            try {
                // Salon verisi sayısal olabilir
                int salon1 = Integer.parseInt(o1.toString());
                int salon2 = Integer.parseInt(o2.toString());
                return Integer.compare(salon1, salon2);
            } catch (NumberFormatException e) {
                // Eğer sayısal bir değer değilse (metin), alfabetik sıralama yapılır
                return o1.toString().compareTo(o2.toString());
            }
        });
        // Diğer sütunlar (Ad Soyad vs.) String olarak sıralanabilir.
        Collator collator = Collator.getInstance(new Locale("tr", "TR"));
        collator.setStrength(Collator.PRIMARY); // Büyük/küçük harf ve aksan duyarlılığını kaldır
        sorter.setComparator(2, (o1, o2) -> collator.compare(o1.toString(), o2.toString()));
    }
    // Bu method checkbox'lar için ItemListener ekler
    private static void addCheckboxListener(JTable table, DefaultTableModel model) {
        table.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        table.getModel().addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int column = e.getColumn();

                // 6. sütun checkbox sütunuydu
                if (column == 6) {
                    int studentId = (int) model.getValueAt(row, 0); // Öğrencinin ID'sini alıyoruz
                    boolean isChecked = (boolean) model.getValueAt(row, column); // Checkbox durumunu alıyoruz
                    // Değişikliği veritabanına kaydediyoruz
                    updateCheckboxValue(studentId, isChecked);
                }
            }
        });
    }
    static DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"User ID", "Checkbox"}, 0);
    JTable table = new JTable(tableModel);

    private static JPanel createBottomPanel(JTable table) {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportButton = new JButton("PDF Olarak Kaydet");
        exportButton.addActionListener(e -> PDFExporter.exportpdf(table));
        buttonPanel.add(exportButton);

        JButton printButton = new JButton("Yazdır");
        printButton.addActionListener(e -> printTable(table));
        buttonPanel.add(printButton);

        JTextField searchField = new JTextField(15);


        buttonPanel.add(searchField);
        JButton clearButton = new JButton("Yoklamayı Sıfırla");
        buttonPanel.add(clearButton);
        clearButton.addActionListener(e -> resetAllCheckboxesInDatabase(table,tableModel));



        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                // Kullanıcı metni her değiştirdiğinde arama fonksiyonunu çağır
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

    private static void resetAllCheckboxesInDatabase(JTable table, DefaultTableModel tableModel) {
        try (Connection connection = DataBaseHelper.getConnection()) {

            if (connection == null) {
                System.out.println("Veritabanına bağlanılamadı.");
                return;
            }

            System.out.println("Veritabanına başarıyla bağlanıldı.");

            // Tüm checkbox_state değerlerini 0 olarak güncelle
            String sql = "UPDATE ogrencitakip SET checkbox_state = 0";

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int affectedRows = stmt.executeUpdate();
                System.out.println("Yoklama sıfırlandı: " + affectedRows + " satır güncellendi.");



            } catch (SQLException e) {
                System.out.println("SQL Hatası: " + e.getMessage());
                e.printStackTrace();
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
            sorter.setRowFilter(null); // Eğer arama sorgusu boşsa, tüm satırları göster
        } else {
            RowFilter<TableModel, Object> rf = RowFilter.regexFilter("(?i)" + query); // Arama sorgusuna göre filtrele
            sorter.setRowFilter(rf); // Filtreyi uygula
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
