package com.antra.evaluation.reporting_system.service;

import com.antra.evaluation.reporting_system.pojo.api.PDFRequest;
import com.antra.evaluation.reporting_system.pojo.exception.PDFGenerationException;
import com.antra.evaluation.reporting_system.pojo.report.PDFFile;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PDFGenerator {
    private static final Logger log = LoggerFactory.getLogger(PDFGenerator.class);

    public PDFFile generate(PDFRequest request) {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("desc_str", request.getDescription());
        StringBuilder data = new StringBuilder();
        for (List<String> datum : request.getData()) {
            data.append(String.join(", ", datum));
            data.append("\r\n");
        }
        parameters.put("content_str", String.join(",", request.getHeaders()) + "\r\n" + data.toString());

        List<Object> itemList = List.of("Empty");
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemList);

        final int MAX_ATTEMPTS = 5;
        int attempts = 0;
        while (attempts < MAX_ATTEMPTS) {
            attempts++;
            try {
                Resource resource = new ClassPathResource("Coffee_Landscape.jasper");

                InputStream inputStream = resource.getInputStream();

                byte[] buffer = new byte[inputStream.available()];
                inputStream.read(buffer);

                File jaspFile = new File("Coffee_Landscape_copy.jasper");
                OutputStream outStream = new FileOutputStream(jaspFile);
                outStream.write(buffer);

                //            File jaspFile = ResourceUtils.getFile("classpath:Coffee_Landscape.jasper");
                JasperPrint jprint = JasperFillManager.fillReport(jaspFile.getAbsolutePath(), parameters, dataSource);
                File temp = File.createTempFile(request.getSubmitter(), "_tmp.pdf");
                JasperExportManager.exportReportToPdfFile(jprint, temp.getAbsolutePath());
                PDFFile generatedFile = new PDFFile();
                generatedFile.setFileLocation(temp.getAbsolutePath());
                generatedFile.setFileName(temp.getName());
                generatedFile.setFileSize(temp.length());
                log.info("Generated PDF file: {}", generatedFile);
                return generatedFile;
            } catch (IOException | JRException e) {
                log.error("Error in generating PDF file", e);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted", e);
            }
        }
        throw new PDFGenerationException();
    }
}
