package LlmModule.tools;

import PythonProcesser.PythonProcesser;

import java.util.Deque;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextClearer {

    private static final String markDownJsonBlockReg =  "```(?:json)?\\s*([\\s\\S]*?)```";
    private static final String canNotFound = "Not found";
    private static final String invalidJson = "Invalid JSON structure";
    private static final String pythonRuntimeError = "Python Runtime Error";
    private TextClearer(){}
    public static String extractJson(String text) {
        int start = text.indexOf('{');
        if (start == -1) {
            return canNotFound;
        }

        Deque<Character> stack = new LinkedList<>();
        int end = start;

        for (; end < text.length(); end++) {
            char ch = text.charAt(end);
            if (ch == '{') {
                stack.push(ch);
            } else if (ch == '}') {
                stack.pop();
                if (stack.isEmpty()) {
                    break;
                }
            }
        }

        if (stack.isEmpty()) {
            text = text.substring(start, end + 1);
            return removeAllComment(text);
        } else {
            return invalidJson;
        }
    }
    public static String extractMarkdownJsonBlock(String text){
        Pattern pattern = Pattern.compile(markDownJsonBlockReg);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return  matcher.group(1);
        } else {
            return canNotFound;
        }
    }
    public static String removeAllComment(String text){
        return text.replaceAll("//.*", "").replaceAll("#.*","");
    }

    public static String repairJsonByPython(String text){
        try {
            return PythonProcesser.repairJson(text);
        }catch (Exception e){
            return pythonRuntimeError;
        }
    }
}
