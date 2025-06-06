package com.Orakatrap.rbx.Utility;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileToolAlt {

    // Check if root is available (basic Magisk-aware check)
    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo rooted"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.destroy();
            return "rooted".equals(line);
        } catch (Exception e) {
            return false;
        }
    }

    // Run root command using Magisk-friendly syntax
    public static String runRootCommand(String command) throws IOException {
        Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
        StringBuilder output = new StringBuilder();

        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            while ((line = error.readLine()) != null) {
                output.append("ERR: ").append(line).append("\n");
            }

            process.waitFor();
        } catch (Exception e) {
            throw new IOException("Failed to execute root command", e);
        }

        return output.toString();
    }

    public static void createDirectoryWithPermissions(String path) throws IOException {
        runRootCommand("mkdir -p \"" + path + "\" && chmod -R 755 \"" + path + "\"");
    }

    public static boolean pathExists(String path) {
        try {
            String result = runRootCommand("ls \"" + path + "\"");
            return !result.contains("No such file");
        } catch (Exception e) {
            return false;
        }
    }

    public static void writeFile(String path, String content) throws IOException {
        File tempFile = File.createTempFile("temp", null);
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        runRootCommand("cat \"" + tempFile.getAbsolutePath() + "\" > \"" + path + "\"");
        runRootCommand("chmod 644 \"" + path + "\"");

        if (!tempFile.delete()) {
            throw new IOException("Failed to delete temp file: " + tempFile.getAbsolutePath());
        }
    }

    public static boolean isDirectory(String path) throws IOException {
        String result = runRootCommand("[ -d \"" + path + "\" ] && echo dir || echo file").trim();
        return result.equals("dir");
    }

    public static boolean isExists(String path) throws IOException {
        String result = runRootCommand("[ -e \"" + path + "\" ] && echo exists || echo missing").trim();
        return result.equals("exists");
    }

    public static File[] listFiles(String directoryPath) throws IOException {
        String output = runRootCommand("ls -1 \"" + directoryPath + "\"");
        List<File> fileList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !line.startsWith("ERR:")) {
                    fileList.add(new File(directoryPath, line.trim()));
                }
            }
        }

        return fileList.toArray(new File[0]);
    }

    public static String readFile(String path) throws IOException {
        return runRootCommand("cat \"" + path + "\"");
    }
}

