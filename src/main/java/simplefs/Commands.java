package simplefs;

import com.google.common.collect.ImmutableMap;

public class Commands {
  public static final String CAT = "cat";
  public static final String CD = "cd";
  public static final String CP = "cp";
  public static final String FIND = "find";
  public static final String HELP = "help";
  public static final String LS = "ls";
  public static final String MKDIR = "mkdir";
  public static final String MV = "mv";
  public static final String MV_MERGE_OPTION = "--merge";
  public static final String PWD = "pwd";
  public static final String QUIT = "quit";
  public static final String RM = "rm";
  public static final String WRITE = "write";

  public static final String HELP_STRING =
    "These are the commands supported by this in-memory filesystem. Type\n" + 
    "`help <name>` to find out more about the command `name`.\n\n" +
    CAT + " <filename>:  Outputs the text contained within <filename>.\n" +
    CD + " <filepath>: Changes the current working directory to <filepath>\n" +
    CP + " [--merge] <filepath_from> <filepath_to>: Copies all files and\n" +
    "    directories from <filepath_from> to <filepath_to> \n" +
    FIND + " <filename>:  Outputs all paths that contain <filename>.\n" +
    HELP + ": Lists available commands \n" +
    LS + ":  Lists the current directory's contents.\n" +
    MKDIR + " <directory>: Creates a new directory <directory>\n" +
    MV +" [--merge] <filepath_from> <filepath_to>: Moves the files and\n" +
    "    directories at <filepath_from> to <filepath_to>.\n" +
    PWD + ": Lists the current directory's filepath.\n" +
    QUIT + ": Exits the program. All files and directories will be deleted.\n" +
    RM + " <filepath>:  Removes the directory or file located at <filepath>\n" +
    WRITE + " <filename> \"<contents>\":  Writes <contents> into file\n" +
    "    <filename>.";

  public static final String DETAILED_DESCRIPTION_CAT =
    "cat <filename>\n" +
    "Outputs the body text contained within <filename>";
  public static final String DETAILED_DESCRIPTION_CD =
    "cd <filepath>\n" +
    "Changes the current working directory to <filepath>. Supports both\n" +
    "absolute and relative filepaths, as well as special path operators `.`\n" +
    "and `..`";
  public static final String DETAILED_DESCRIPTION_CP =
    "cp [--merge] <filepath_from> <filepath_to>\n" +
    "Copies the files and directories from <filepath_from> to\n" +
    "<filepath_to>. Collisions are handled by renaming the copied file or\n" +
    "directory. If the merge flag is supplied, then all directories and\n" +
    "files are merged between <filepath_from> into <filepath_to>.\n" +
    "Recursively copies all files and directories.\n" +
    "Supports both absolute and relative filepaths, as well as special path\n" +
    "operators `.` and `..`";
  public static final String DETAILED_DESCRIPTION_FIND =
    "find <filename>\n" +
    "Outputs all paths that contain <filename>.Directories containing the\n" +
    "filename will be outputted with a trailing slash.";
  public static final String DETAILED_DESCRIPTION_HELP =
    "help [<command>]\n" +
    "Type `Help` to view all commands. Type `Help <command>` to view more\n" +
    "detailed instructions for a specific command.";
  public static final String DETAILED_DESCRIPTION_LS =
    "ls [<filepath>]\n" +
    "Outputs all files and directories within the filepath. If no filepath\n" +
    "is provided, output is for the current directory Supports both\n" +
    "absolute and relative filepaths, as well as special path operators `.`\n" +
    "and `..`";
  public static final String DETAILED_DESCRIPTION_MKDIR =
    "mkdir <filepath>\n" +
    "Creates a new directory at the given filepath. It will recursively\n" +
    "create any directories that do not yet exist. Supported filepath\n" +
    "characters are limited to [A-Z] [0-9] `.` and `_`.\n" +
    "Supports both absolute and relative filepaths, as well as special path\n" +
    "operators `.` and `..`";
  public static final String DETAILED_DESCRIPTION_MV =
    "mv [--merge] <filepath_from> <filepath_to>\n" +
    "Moves the files and directories from <filepath_from> to <filepath_to>.\n" +
    "Collisions are handled by renaming the copied file or directory. If\n" +
    "the merge flag is supplied, then all directories and files are merged\n" +
    "between <filepath_from> into <filepath_to>\n" +
    "Supports both absolute and relative filepaths, as well as special path\n" +
    "operators `.` and `..`";
  public static final String DETAILED_DESCRIPTION_PWD =
    "pwd\n" +
    "Prints the absolute filepath starting at root for the current directory";
  public static final String DETAILED_DESCRIPTION_QUIT =
    "quit\n" +
    "Exits the application. All files and directories will be deleted.";
  public static final String DETAILED_DESCRIPTION_RM =
    "rm <filepath>\n" +
    "Removes all files and directories at filepath.\n" +
    "Supports both absolute and relative filepaths, as well as special path\n" +
    "operators `.` and `..`";
  public static final String DETAILED_DESCRIPTION_WRITE = 
    "write <filename> [\"<contents>\"]\n" + 
    "Creates a new file with optional body text <contents>. A filepath may\n" +
    "be included in the filename.\n" +
    "Supported filename characters are limited to [A-Z] [0-9] `.` and `_`.\n" +
    "Body contents to write must be surrounded by quotation marks.\n" +
    "Supports both absolute and relative filepaths, as well as special path\n" +
    "operators `.` and `..`";

  public static final ImmutableMap<String, String> COMMANDS =
    ImmutableMap.<String, String>builder()
      .put(CAT, DETAILED_DESCRIPTION_CAT)
      .put(CD, DETAILED_DESCRIPTION_CD)
      .put(CP, DETAILED_DESCRIPTION_CP)
      .put(FIND, DETAILED_DESCRIPTION_FIND)
      .put(HELP, DETAILED_DESCRIPTION_HELP)
      .put(LS, DETAILED_DESCRIPTION_LS)
      .put(MKDIR, DETAILED_DESCRIPTION_MKDIR)
      .put(MV, DETAILED_DESCRIPTION_MV)
      .put(PWD,  DETAILED_DESCRIPTION_PWD)
      .put(QUIT, DETAILED_DESCRIPTION_QUIT)
      .put(RM, DETAILED_DESCRIPTION_RM)
      .put(WRITE, DETAILED_DESCRIPTION_WRITE)
      .build();
}