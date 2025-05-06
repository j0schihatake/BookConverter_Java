package com.j0schi.bookconverter.bookConverter;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
public class DocToPdfLibreOfficeConverter {

    private static final String LIBRE_OFFICE_PATH = "C:\\Program Files\\LibreOffice\\program\\soffice.bin"; // Используем .bin вместо .exe

    public static void main(String[] args) {

        String inputDir = "X:/test/";
        String outputDir = "X:/test/pdf/";

        try {
            convertAllDocuments(inputDir, outputDir);
        } catch (Exception e) {
            System.err.println("Ошибка при конвертации: " + e.getMessage());
        }
    }

    public static void convertAllDocuments(String inputDir, String outputDir) throws Exception {
        if (!Files.exists(Paths.get(inputDir))) {
            throw new IllegalArgumentException("Папка " + inputDir + " не существует!");
        }

        Files.createDirectories(Paths.get(outputDir));

        List<File> documents = getWordFiles(inputDir);

        if (documents.isEmpty()) {
            System.out.println("Нет файлов .doc/.docx в папке " + inputDir);
            return;
        }

        // Проверяем LibreOffice один раз перед обработкой
        if (!isLibreOfficeAvailable()) {
            throw new IllegalStateException("LibreOffice не найден! Проверьте путь: " + LIBRE_OFFICE_PATH);
        }

        for (File docFile : documents) {
            try {
                convertToPdf(docFile, outputDir);
                System.out.println("[Успех] " + docFile.getName() + " → " +
                        docFile.getName().replaceFirst("[.][^.]+$", "") + ".pdf");
            } catch (Exception e) {
                System.err.println("[Ошибка] " + docFile.getName() + ": " + e.getMessage());
            }
        }
    }

    private static List<File> getWordFiles(String dir) {
        File[] files = new File(dir).listFiles((d, name) ->
                name.toLowerCase().endsWith(".doc") || name.toLowerCase().endsWith(".docx")
        );
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }

    public static void convertToPdf(File inputFile, String outputDir) throws Exception {
        // Используем ProcessBuilder с перенаправлением вывода
        ProcessBuilder pb = new ProcessBuilder(
                LIBRE_OFFICE_PATH,
                "--headless",
                "--convert-to", "pdf:writer_pdf_Export",
                "--outdir", outputDir,
                inputFile.getAbsolutePath()
        );

        // Перенаправляем стандартные потоки
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = pb.start();

        // Таймаут 2 минуты на обработку файла
        if (!process.waitFor(2, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            throw new RuntimeException("Превышено время ожидания конвертации");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Ошибка конвертации (код " + process.exitValue() + ")");
        }
    }

    private static boolean isLibreOfficeAvailable() {
        try {
            Process process = new ProcessBuilder(LIBRE_OFFICE_PATH, "--version")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}