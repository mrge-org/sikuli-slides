package org.sikuli.slides.apps;

import java.util.Arrays;

public class Main {

    private static void printHelp() {
        String helpMessage =
                "Sikuli Slides commands:\n\n"+
                        "  execute   Execute slides (see 'execute -help' for options)\n"+
                        "  record    Record actions as slides\n"+
                        "  generate  Generate code from slides\n"+
                        "  gui       Launch GUI";
        System.out.println(helpMessage);
    }
    
    private static void exitWithHelp(int code) {
        printHelp();
        System.exit(code);
    }

    public static void main(String[] args) { 

        // Show help if no args or help flag is provided
        if (args.length == 0) {
            exitWithHelp(0);
        }
        if (args.length == 1) {
            String a = args[0];
            if ("help".equalsIgnoreCase(a) || "-h".equalsIgnoreCase(a) || "--help".equalsIgnoreCase(a)) {
                exitWithHelp(0);
            }
        }

        if (args.length >= 1){
            String command = args[0]; 
            String[] otherArgs;
            if (args.length >= 2){
                otherArgs = Arrays.copyOfRange(args,1,args.length);
            }else{
                otherArgs = new String[]{};
            }
            if (command.compareToIgnoreCase("execute") == 0){
                ExecuteMain.main(otherArgs);
            }else if (command.compareToIgnoreCase("gui") == 0){
                //MainUI.runGuiTool();            
            }else if (command.compareToIgnoreCase("record") == 0){
                RecorderMain.main(otherArgs);               
            }else if (command.compareToIgnoreCase("generate") == 0){
                GenerateMain.main(otherArgs);  
            }else{
                System.err.println("[" + command + "] is not a valid command");
                exitWithHelp(1);
            }     
        }
    }
}
