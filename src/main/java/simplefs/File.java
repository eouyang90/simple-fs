package simplefs;

import java.util.HashMap;
import java.util.HashSet;
import simplefs.exceptions.UserInputException;

/**
 * TODO: Can make this an interface, and have a local, in memory file as well
 * as an actual on disk one.
 * Can also add extensions here to allow buffering and filesize checks to make
 * sure that it's not too large to write out or to save.
 */
public class File {
  private String filename;
  private String contents;

  public File(String filename) { 
    this(filename, "");
  }

  public File(String filename, String contents) {
    this.filename = filename;
    this.contents = contents;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getFilename() {
    return filename;
  }

  /** Returns the content written in the file. */
  public String read() {
    return contents;
  }

  public boolean write(String content) {
    // TODO: Add some size checks here.
    contents = content;
    return true;
  }

  public static File makeCopy(File fileToCopy) {
    return new File(fileToCopy.getFilename(), fileToCopy.read());
  }
}