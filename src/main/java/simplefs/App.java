package simplefs;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import simplefs.exceptions.UserInputException;

/**
 * Runs the in-memory file system taking user commands via System.in.
 */
public class App 
{
  public static void main( String[] args ) {
    // Just use Scanner to read and buffer the user's input. Tried testing it
    // with BufferedReader as well (which has an 8KB default buffer limit), but
    // for some reason System.in is limited to a 1KB buffer regardless. 
    Scanner input= new Scanner(System.in);
    FileSystem fs = new FileSystem();
    String startUpText = fs.startUp();
    System.out.println(startUpText);
    boolean isRunning = fs.isActive();
    while(isRunning){
      System.out.print("> ");
      String userInput = input.nextLine();
      String outputString;
      try {
        outputString = fs.handleCommand(userInput);
      } catch (UserInputException e) {
        outputString = e.getMessage();
      }

      if (!Strings.isNullOrEmpty(outputString)) {
        System.out.println(outputString);
      }
      isRunning = fs.isActive();
    }
    fs.teardown();
  }
    
}
