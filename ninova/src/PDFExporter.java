
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class PDFExporter {
    public static void exportpdf(JTable table) {
        System.out.println("PDF oluşturma başladı...");

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String formattedDate = now.format(formatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH-mm-ss");
        String formattedTime = now.format(timeFormatter);

        String userHome = System.getProperty("user.home");
        String desktopPath = userHome + File.separator + "Desktop";

        // Tarihe göre klasör oluşturuluyor
        String folderPath = desktopPath + File.separator + "Yoklama " + formattedDate;
        File folder = new File(folderPath);
        if (!folder.exists()) {
            boolean dirCreated = folder.mkdirs();
            if (!dirCreated) {
                JOptionPane.showMessageDialog(null, "Dizin oluşturulamadı: " + folder.getAbsolutePath());
                return;
            }
        }

        // PDF dosyasının tam yolu
        String fileName = "Yoklama_" + formattedTime + ".pdf";
        String filePath = folderPath + File.separator + fileName;
        System.out.println("PDF dosya yolu: " + filePath);

        File file = new File(filePath);
        if (!file.getParentFile().exists()) {
            boolean dirCreated = file.getParentFile().mkdirs();
            if (!dirCreated) {
                JOptionPane.showMessageDialog(null, "Dizin oluşturulamadı: " + file.getParentFile().getAbsolutePath());
                return;
            }
        }

        try (PDDocument document = new PDDocument()) {
            PDType0Font font = PDType0Font.load(document, new File("C:\\Windows\\Fonts\\arial.ttf"));
            PDType0Font boldFont = PDType0Font.load(document, new File("C:\\Windows\\Fonts\\arialbd.ttf"));

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.setFont(boldFont, 16);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText("Yoklama Listesi - " + formattedDate);
            contentStream.endText();

            // Başlıkları yazdır
            contentStream.setFont(boldFont, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(50, 720);
            TableModel model = table.getModel();
            for (int i = 0; i < model.getColumnCount(); i++) {
                contentStream.showText(model.getColumnName(i) + " | ");
            }
            contentStream.endText();

            contentStream.setFont(font, 12);
            int yPosition = 700;
            int pageCount = 1;

            // Öğrencilerin verilerini yazdır
            for (int i = 0; i < model.getRowCount(); i++) {
                StringBuilder rowData = new StringBuilder();
                for (int j = 0; j < model.getColumnCount(); j++) {
                    Object cellValue = model.getValueAt(i, j);
                    if (cellValue != null) {
                        String filteredValue = cellValue.toString().replace("✔", "[OK]");
                        rowData.append(filteredValue).append(" | ");
                    }
                }

                // Yeni sayfa ekleme kontrolü
                if (yPosition < 50) {
                    contentStream.close(); // Önceki içerik akışını kapatıyoruz
                    page = new PDPage(PDRectangle.A4); // Yeni sayfa ekliyoruz
                    document.addPage(page);

                    contentStream = new PDPageContentStream(document, page);  // Yeni contentStream oluştur

                    contentStream.setFont(boldFont, 12);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, 750);
                    contentStream.showText("Yoklama Listesi - Sayfa " + (++pageCount));
                    contentStream.endText();
                    yPosition = 700; // Yeni sayfada y pozisyonunu sıfırlıyoruz
                }

                contentStream.beginText();
                contentStream.newLineAtOffset(50, yPosition);
                contentStream.showText(rowData.toString().trim());
                contentStream.endText();
                yPosition -= 20; // Satırlar arası mesafe
            }

            contentStream.close(); // Son içerik akışını kapatıyoruz
            document.save(new File(filePath));
            System.out.println("PDF başarıyla kaydedildi: " + filePath);
            JOptionPane.showMessageDialog(null, "PDF Kaydedildi: " + filePath);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "PDF oluşturulurken hata oluştu!");
        }
    }
}
