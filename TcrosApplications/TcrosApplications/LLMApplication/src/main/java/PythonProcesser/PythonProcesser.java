package PythonProcesser;

import java.io.*;

public class PythonProcesser {
    private PythonProcesser(){}
    public static String repairJson(String brokenJson) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("python", "F:\\Project\\Thesis\\Thesis\\Eclispe MOSAIC Application\\TcrosApplications\\TcrosApplications\\jsonRepair.py");
        pb.redirectErrorStream(true); // 合併 stderr 和 stdout

        Process process = pb.start();

        // 寫入錯誤 JSON 到 Python 腳本 stdin
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            writer.write(brokenJson);
        }

        // 讀取修復後的輸出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Repair Fail: " + output);
        }

        return output.toString();
    }

}

