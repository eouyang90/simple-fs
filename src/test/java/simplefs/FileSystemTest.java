package simplefs;

import static simplefs.Commands.*;
import static simplefs.FileSystem.PARENT_DIR_SHORTCUT;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.CoreMatchers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import simplefs.exceptions.UserInputException;

/**
 * Unit test for FileSystem.
 */
public class FileSystemTest {
  private static final String ROOT = "/";
  private static final String DIR_A = "directoryA";
  private static final String DIR_B = "directoryB";
  private static final String DIR_C = "directoryC";
  private static final String DIR_D = "directoryD";
  private static final String DIR_E = "directoryE";
  private static final String FILE_A = "fileA";
  private static final String FILE_A_TEXT = "A nice story blah blah blah";
  private static final String FILE_B = "fileB";
  private static final String FILE_B_TEXT =
    "Hello this  \" (*&)#(*&@#($&^ has \n some weird <<< characters \"\\";

  private FileSystem fs;

  @Before
  public void init() {
    fs = new FileSystem();
    fs.startUp();
  }

  @After
  public void teardown() {
    fs.teardown();
  }

  @Test
  public void help_outputsHelpText() throws Exception {
    // Normal help command should output help text and some of the commands.
    assertThat(
      fs.handleCommand(HELP),
      allOf(
        containsString(HELP), containsString(PWD), containsString(WRITE)));

    // Help + a command should output help text for that command and no others.
    assertThat(
      fs.handleCommand(HELP + " " + MKDIR),
      allOf(containsString(MKDIR), not(containsString(PWD))));
  }

  @Test
  public void helpInvalidInput_throwsException() throws Exception {
    // Too many arguments should throw an exception
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(HELP + " ls mv"));
    // Invalid arguments should throw an exception
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(HELP + " blah"));
  }

  @Test
  public void lsNoArgs_printsCurrentDirectoryContents() throws Exception {
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    addFile(FILE_A, FILE_A_TEXT);

    String output = fs.handleCommand(LS);
    assertThat(output,
      allOf(
        containsString(DIR_A),
        containsString(DIR_B),
        containsString(FILE_A)));
  }

  @Test
  public void lsWithFilepath_printsDirectoryContents() throws Exception {
    // A
    //   - B
    //   - File_B
    // File_A
    addDirectory(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    addChildDirectory(DIR_A, DIR_B);
    addChildFile(DIR_A, FILE_B);

    // Test root
    assertThat(
      fs.handleCommand(LS + " /"),
      allOf(containsString(DIR_A), containsString(FILE_A)));

    // Test current directory shortcut
    assertThat(
      fs.handleCommand(LS + " ."),
      allOf(containsString(DIR_A), containsString(FILE_A)));

    // Test subdirectory
    assertThat(
      fs.handleCommand(LS + " " + DIR_A),
      allOf(containsString(DIR_B), containsString(FILE_B)));

    // Test parent path shortcut
    cd(DIR_A);
    assertThat(
      fs.handleCommand(LS + " " + ".."),
      allOf(containsString(DIR_A), containsString(FILE_A)));
  }

  @Test
  public void lsMultiplePaths_printsContents() throws Exception {
    // A
    //   - B
    //     - C
    //     - File_B
    // File_A
    addDirectory(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    cd(DIR_A);
    addDirectory(DIR_B);
    addChildDirectory(DIR_B, DIR_C);
    addChildFile(DIR_B, FILE_B);
    cd(ROOT);

    // Output of 3 different directories: '/', 'a', and 'a/b'
    String output =
      fs.handleCommand(LS + " /" + " " + DIR_A + " " + DIR_A + "/" + DIR_B);
    assertThat(output, containsString(DIR_A));
    assertThat(output, containsString(FILE_A));
  }

  @Test
  public void pwd_printsPath() throws Exception {
    // A
    //   - C
    addDirectory(DIR_A);
    addChildDirectory(DIR_A, DIR_C);

    assertThat(fs.handleCommand(PWD), equalTo("/"));

    cd(DIR_A);
    cd(DIR_C);
    assertThat(
      fs.handleCommand(PWD),
      equalTo("/" + DIR_A + "/" + DIR_C));
  }

  @Test
  public void pwdInvalidArguments_throwsError() throws Exception {
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(PWD + " somearg"));
  }

  @Test
  public void mkdir_createsDirectories() throws Exception {
    // Basic mkdir for a child directory
    fs.handleCommand(MKDIR + " " + DIR_A);
    assertThat(fs.handleCommand(LS), containsString(DIR_A));

    // mkdir with a path recursively creates non-existent directories.
    fs.handleCommand(MKDIR + " " + DIR_B + "/" + DIR_C);
    assertThat(fs.handleCommand(LS + " " + DIR_B), containsString(DIR_C));

    // mkdir in parents' directory
    cd(DIR_B);
    cd(DIR_C);
    fs.handleCommand(MKDIR + " ../../" + DIR_D);
    cd(ROOT);
    assertThat(fs.handleCommand(LS), containsString(DIR_D));

    // mkdir with a path from root
    cd(DIR_B);
    cd(DIR_C);
    fs.handleCommand(MKDIR + " /" + DIR_E);
    cd(ROOT);
    assertThat(fs.handleCommand(LS), containsString(DIR_E));
  }

  @Test
  public void mkdirExistingDirectory_throwsError() throws Exception {
    fs.handleCommand(MKDIR + " " + DIR_A);
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(MKDIR + " " + DIR_A));
  }

  @Test
  public void mkdirInvalidName_throwsError() throws Exception {
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(MKDIR + " " + "(*&)"));
  }

  @Test
  public void mkdirInvalidArgs_throwsError() throws Exception {
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(MKDIR));
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(MKDIR + " " + DIR_A + " " + DIR_B));
  }

  @Test
  public void cd_navigatesToRoot() throws Exception {
    // A
    //   - B
    //     - C
    addDirectory(DIR_A);

    // cd into a child directory a
    fs.handleCommand(CD + " " + DIR_A);
    assertThat(fs.handleCommand(PWD), equalTo("/" + DIR_A));

    addDirectory(DIR_B);
    addChildDirectory(DIR_B, DIR_C);

    // cd into path b/c
    fs.handleCommand(CD + " " + DIR_B + "/" + DIR_C);
    assertThat(
      fs.handleCommand(PWD),
      equalTo("/" + DIR_A + "/" + DIR_B + "/" + DIR_C));

    // cd to a parent
    fs.handleCommand(CD + " ..");
    assertThat(
      fs.handleCommand(PWD),
      equalTo("/" + DIR_A + "/" + DIR_B));

    // cd back to root
    fs.handleCommand(CD + " /");
    assertThat(fs.handleCommand(PWD), equalTo("/"));

    // cd no args goes to root
    fs.handleCommand(CD + " " + DIR_A);
    fs.handleCommand(CD);
    assertThat(fs.handleCommand(PWD), equalTo("/"));
  }

  @Test
  public void cdInvalidPathOrArgs_throwsError() throws Exception {
    addDirectory(DIR_A);

    assertThrows(
      UserInputException.class, () -> fs.handleCommand(CD + " blah"));
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(CD + " " + DIR_A + " " + DIR_B));
  }

  @Test
  public void writeFile() throws Exception {
    // Write an empty file
    fs.handleCommand(WRITE + " " + FILE_A);
    assertThat(fs.handleCommand(CAT + " " + FILE_A), equalTo(""));

    // Write a file with text
    fs.handleCommand(WRITE + " " + FILE_B + " \"" + FILE_B_TEXT + "\"");
    assertThat(fs.handleCommand(CAT + " " + FILE_B), equalTo(FILE_B_TEXT));

    // Write with absolute filepath
    fs.handleCommand(WRITE + " /" + FILE_A + " \"" + FILE_A_TEXT + "\"");
    assertThat(fs.handleCommand(CAT + " " + FILE_A), equalTo(FILE_A_TEXT));

    // Write into child directory
    addDirectory(DIR_A);
    String filepath = DIR_A + "/" + FILE_A;
    fs.handleCommand(WRITE + " " + filepath + " \"" + FILE_A_TEXT + "\"");
    assertThat(fs.handleCommand(CAT + " " + filepath), equalTo(FILE_A_TEXT));
  }

  @Test
  public void writeFile_overwritesExisting() throws Exception  {
    // Write an empty file
    fs.handleCommand(WRITE + " " + FILE_A + " \"" + FILE_A_TEXT + "\"");
    assertThat(fs.handleCommand(CAT + " " + FILE_A), equalTo(FILE_A_TEXT));

    // Write same file with text
    fs.handleCommand(WRITE + " " + FILE_A + " \"" + FILE_B_TEXT + "\"");
    assertThat(fs.handleCommand(CAT + " " + FILE_A), equalTo(FILE_B_TEXT));
  }

  @Test
  public void writeFileInvalid_throwsError() throws Exception {
    // No args
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(WRITE));

    // Invalid name
    String filepath = "weird$Symbols)(";
    String writeCommand = WRITE + " " + filepath + " \"" + FILE_A_TEXT + "\"";
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(writeCommand));

    // A directory is not a valid file name
    addDirectory(DIR_A);
    filepath = "/" + DIR_A;
    String writeCommand2 = WRITE + " " + filepath + " \"" + FILE_A_TEXT + "\"";
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(writeCommand2));

    // Root directory is not a valid file name
    filepath = "/";
    String writeCommand3 = WRITE + " " + filepath + " \"" + FILE_A_TEXT + "\"";
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(writeCommand3));

    // Invalid body text, missing quotes
    String writeCommand4 = WRITE + " " + DIR_A + " \"" + FILE_A_TEXT;
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(writeCommand4));
  }

  @Test
  public void cat_outputsFileContents() throws Exception {
    // fileA
    // fileB
    // dirA
    //   - fileA
    addFile(FILE_A, null);
    addFile(FILE_B, FILE_B_TEXT);
    addDirectory(DIR_A);
    cd(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    cd(ROOT);

    // cat an empty file
    assertThat(fs.handleCommand(CAT + " " + FILE_A), equalTo(""));

    // cat a file with text
    assertThat(fs.handleCommand(CAT + " " + FILE_B), equalTo(FILE_B_TEXT));

    // cat with absolute filepath
    assertThat(fs.handleCommand(CAT + " /" + FILE_B), equalTo(FILE_B_TEXT));
    String filepath = "/" + DIR_A + "/" + FILE_A;
    assertThat(fs.handleCommand(CAT + " " + filepath), equalTo(FILE_A_TEXT));
  }

  @Test
  public void catInvalidFile_throwsError() throws Exception {
    addFile(FILE_A, null);
    addFile(FILE_B, FILE_B_TEXT);
    addDirectory(DIR_A);

    // cat a nonexistent file
    String invalidName = FILE_A + "b";
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(CAT + " " + invalidName));
    // cat a directory
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(CAT + " " + DIR_A));

    // cat too many args
    String invalidArgs = " " + FILE_A + " " + FILE_B;
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(CAT + invalidArgs));

    // cat not enough args
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(CAT));
  }

  @Test
  public void rm_removesContents() throws Exception {
    // dirA
    //   - fileA
    //   - dirC
    //     - dirD
    // dirB
    //   - fileB
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    cd(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    addDirectory(DIR_C);
    addChildDirectory(DIR_C, DIR_D);
    cd(ROOT);
    addChildFile(DIR_B, FILE_B);
    cd(ROOT);

    cd(DIR_B);
    // absolute path works to another branch
    fs.handleCommand(RM + " /" + DIR_A + "/" + DIR_C);
    assertThat(fs.handleCommand(LS + " " + DIR_A), not(containsString(DIR_C)));

    cd(ROOT);
    cd(DIR_A);
    fs.handleCommand(RM + " " + ".");
    assertThat(fs.handleCommand(LS), not(containsString(DIR_A)));
    assertThat(fs.handleCommand(PWD), equalTo(ROOT));

    // Rm removes child files and directories
    fs.handleCommand(RM + " /" + DIR_B + "/" + FILE_B);
    assertThat(fs.handleCommand(LS + " " + DIR_B), not(containsString(FILE_B)));
    fs.handleCommand(RM + " " + DIR_B);
    assertThat(fs.handleCommand(LS), not(containsString(DIR_B)));
  }

  @Test
  public void rmParent_removesContents_andUpdatesDirectory() throws Exception {
    // dirA
    //   - fileA
    //   - dirC
    //     - dirD
    // dirB
    //   - fileB
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    cd(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    addDirectory(DIR_C);
    addChildDirectory(DIR_C, DIR_D);
    cd(ROOT);
    addChildFile(DIR_B, FILE_B);
    cd(ROOT);

    // Rm removes child files and directories
    fs.handleCommand(RM + " /" + DIR_B + "/" + FILE_B);
    assertThat(fs.handleCommand(LS + " " + DIR_B), not(containsString(FILE_B)));
    fs.handleCommand(RM + " " + DIR_B);
    assertThat(fs.handleCommand(LS), not(containsString(DIR_B)));

    // removing the parent directory of the one you're in puts you in the
    // directory of the parent.
    cd(DIR_A);
    cd(DIR_C);
    cd(DIR_D);
    fs.handleCommand(RM + " " + "..");
    assertThat(fs.handleCommand(PWD), equalTo("/" + DIR_A));
    fs.handleCommand(RM + " " + "/" + DIR_A);
    assertThat(fs.handleCommand(PWD), equalTo(ROOT));
  }

  @Test
  public void rmInvalidArgs_throwsError() throws Exception {
    addDirectory(DIR_A);
    addDirectory(DIR_B);

    // Expects at least 1 arg
    assertThrows(UserInputException.class, () -> fs.handleCommand(RM));
    // Can't handle multiple args
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(RM + " " + DIR_A + " " + DIR_B));
  }

  @Test
  public void rmInvalidFile_throwsError() throws Exception {
    addDirectory(DIR_A);
    addDirectory(DIR_B);

    // Can't delete root.
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(RM + " " + ROOT));

    // Non-existent file or directory
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(RM + " " + DIR_B + "/" + DIR_C));
  }

  @Test
  public void find_recursivelyFindsAllFilesAndDirectories() throws Exception {
    // dirA
    //   - dirA
    //     - dirA (file)
    // dirB
    //   - dirA
    addDirectory(DIR_A);
    cd(DIR_A);
    addDirectory(DIR_A);
    addChildFile(DIR_A, DIR_A);
    cd(ROOT);
    addDirectory(DIR_B);
    addChildDirectory(DIR_B, DIR_A);
    cd(ROOT);

    // Directories are outputted with a trailing slash, files are not.
    assertThat(
      fs.handleCommand(FIND + " " + DIR_A),
      allOf(
        containsString("/" + DIR_A + "/"),
        containsString("/" + DIR_A + "/" + DIR_A + "/"),
        containsString("/" + DIR_A + "/" + DIR_A + "/" + DIR_A),
        not(containsString("/" + DIR_A + "/" + DIR_A + "/" + DIR_A + "/")),
        containsString("/" + DIR_B + "/" + DIR_A + "/")));
  }

  @Test
  public void findNoInstances_printsMessage() throws Exception {
    addDirectory(DIR_B);
    assertThat(
      fs.handleCommand(FIND + " " + DIR_A),
      containsString("No instances"));
  }

  @Test
  public void findInvalidArgs_throwsError() throws Exception {
    addDirectory(DIR_A);
    addDirectory(DIR_B);

    // Expects at least 1 arg
    assertThrows(UserInputException.class, () -> fs.handleCommand(FIND));
    // Can't handle multiple args
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(FIND + " " + DIR_A + " " + DIR_B));
  }

  @Test
  public void mv_movesFile() throws Exception {
    // dirA
    //   - fileA
    //   - dirC
    // dirB
    //   - fileB
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    cd(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    addDirectory(DIR_C);
    cd(ROOT);
    addChildFile(DIR_B, FILE_B);
    cd(ROOT);

    // mv a file into a sister directory
    // dirA
    //   - fileA
    //   - fileB
    //   - dirC
    // dirB
    cd(DIR_B);
    fs.handleCommand(MV + " " + FILE_B + " " + ROOT + DIR_A);
    cd(ROOT);
    // File no longer exists in old directory
    assertThat(fs.handleCommand(LS + " " + DIR_B), equalTo(""));
    assertThat(fs.handleCommand(LS + " " + DIR_A), containsString(FILE_B));

    // mv to current directory does nothing
    cd(DIR_A);
    fs.handleCommand(MV + " " + FILE_B + " " + ROOT + DIR_A);
    assertThat(fs.handleCommand(LS), containsString(FILE_B));

    // mv to a child directory
    // dirA
    //   - fileA
    //   - dirC
    //     - fileB
    // dirB
    fs.handleCommand(MV + " " + FILE_B + " " + DIR_C);
    assertThat(fs.handleCommand(LS + " " + DIR_C), containsString(FILE_B));
  }

  @Test
  public void mv_renamesFileIfExisting() throws Exception {
    // dirA
    //   - fileA
    //   - dirC
    //     - fileA (directory)
    // dirB
    //   - fileB
    // fileB
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    addFile(FILE_B, FILE_B_TEXT);
    cd(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    addDirectory(DIR_C);
    addChildDirectory(DIR_C, FILE_A);
    cd(ROOT);
    addChildFile(DIR_B, FILE_B);
    cd(ROOT);

    String fileBWithSuffix = FILE_B + "_1";
    // mv a file into a directory with a file of the same name
    // dirA
    //   - dirC
    //     - fileA (directory)
    // dirB
    //     - fileB
    //     - fileB_1
    fs.handleCommand(MV + " " + FILE_B + " " + DIR_B);
    assertThat(
      fs.handleCommand(LS + " " + DIR_B), containsString(fileBWithSuffix));
    assertThat(fs.handleCommand(LS), not(containsString(FILE_B)));

    addFile(fileBWithSuffix, null);
    addFile(FILE_B, null);
    // dirA
    //   - dirC
    //     - fileA (directory)
    //     - fileA_1
    // dirB
    //     - fileB
    //     - fileB_1
    //     - fileB_1_1
    //     - fileB_2
    fs.handleCommand(MV + " " + fileBWithSuffix + " " + DIR_B);
    fs.handleCommand(MV + " " + FILE_B + " " + DIR_B);
    assertThat(
      fs.handleCommand(LS + " " + DIR_B),
      containsString(fileBWithSuffix + "_1"));
    assertThat(
      fs.handleCommand(LS + " " + DIR_B),
      containsString(FILE_B + "_2"));

    // mv a file into a directory with a directory of the same name
    // dirA
    //   - dirC
    //     - fileA (directory)
    //     - fileA_1
    // dirB
    //     - fileB
    //     - fileB_1
    cd(DIR_A);
    fs.handleCommand(MV + " " + FILE_A + " " + DIR_C);
    assertThat(
      fs.handleCommand(LS + " " + DIR_C), containsString(FILE_A + "_1"));
  }

  @Test
  public void mv_addsAllDirectories() throws Exception {
    // dirA
    //   - fileA
    //   - dirC
    // dirB
    //   - fileB
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    addChildFile(DIR_A, FILE_A);
    addChildDirectory(DIR_A, DIR_C);
    addChildFile(DIR_B, FILE_B);

    // Moving a directory into another adds all child directories.
    // dirB
    //   - fileB
    //   - dirA
    //     - fileA
    //     - dirC
    fs.handleCommand(MV + " " + DIR_A + " " + DIR_B);
    assertThat(fs.handleCommand(LS), not(containsString(DIR_A)));
    assertThat(fs.handleCommand(LS + " " + DIR_B), containsString(DIR_A));
    assertThat(
      fs.handleCommand(LS + " " + DIR_B + "/" + DIR_A),
      allOf(containsString(FILE_A), containsString(DIR_C)));

    // Directory name collisions are handled by renaming the parent directory.
    addDirectory(DIR_A);
    addChildDirectory(DIR_A, DIR_C);
    // dirB
    //   - fileB
    //   - dirA
    //     - fileA
    //     - dirC
    //   - dirA_1
    //     - dirC
    fs.handleCommand(MV + " " + DIR_A + " " + DIR_B);
    assertThat(
      fs.handleCommand(LS + " " + DIR_B), containsString(DIR_A + "_1"));
    // The parent directory got renamed, but not children directories.
    assertThat(
      fs.handleCommand(LS + " " + DIR_B + "/" + DIR_A + "_1"),
      containsString(DIR_C));
  }
  
  @Test
  public void mv_withMerge_recursivelyMergesAllDirectories() throws Exception {
    // dirB
    //   dirA
    //     - fileA
    //     - dirC
    //       - fileB
    // dirC
    //   - dirA
    //     - fileA
    //     - dirC (file)
    addDirectory(DIR_B);
    addDirectory(DIR_C);
    cd(DIR_B);
    addDirectory(DIR_A);
    cd(DIR_A);
    addFile(FILE_A, FILE_A_TEXT);
    addDirectory(DIR_C);
    addChildFile(DIR_C, FILE_B);
    cd(ROOT);
    cd(DIR_C);
    addDirectory(DIR_A);
    addChildFile(DIR_A, FILE_A);
    addChildFile(DIR_A, DIR_C);
    cd(ROOT);

    // merging directories B and C, which have name collisions
    // dirC
    //   - dirA
    //     - fileA
    //     - fileA_1
    //     - dirC (file)
    //     - dirC_1
    //       - fileB
    fs.handleCommand(MV + " " + MV_MERGE_OPTION + " "+ DIR_B + " " + DIR_C);
    assertThat(fs.handleCommand(LS), not(containsString(DIR_B)));
    assertThat(fs.handleCommand(LS + " " + DIR_C), containsString(DIR_A));
    cd("/" + DIR_C + "/" + DIR_A);
    assertThat(
      fs.handleCommand(LS),
      allOf(
        containsString(FILE_A),
        containsString(FILE_A + "_1"),
        containsString(DIR_C),
        containsString(DIR_C + "_1")));

    assertThat(
      fs.handleCommand(LS + " " + DIR_C + "_1"), containsString(FILE_B));
  }

  @Test
  public void mvCurrentDirectory_updatesParent() throws Exception {
    // dirA
    //   - dirB
    //     - dirC
    // dirD
    addDirectory(DIR_A);
    addDirectory(DIR_D);
    cd(DIR_A);
    addDirectory(DIR_B);
    addChildDirectory(DIR_B, DIR_C);

    cd(DIR_B);
    cd(DIR_C);
    // Move the current directory to its parent
    // dirA
    //   - dirB
    //   - dirC
    // dirD
    fs.handleCommand(MV + " . " + "/" + DIR_A);
    assertThat(fs.handleCommand(PWD), equalTo("/" + DIR_A + "/" + DIR_C));

    cd("/" + DIR_A + "/" + DIR_C);
    fs.handleCommand(MV + " . " + "/" + DIR_D);
    assertThat(fs.handleCommand(PWD), equalTo("/" + DIR_D + "/" + DIR_C));
  }

  @Test
  public void mv_cannotMoveParentDirectoryIntoChild() throws Exception {
    // dirA
    //   - dirB
    //     - dirC
    addDirectory(DIR_A);
    cd(DIR_A);
    addDirectory(DIR_B);
    addChildDirectory(DIR_B, DIR_C);

    cd(DIR_B);
    cd(DIR_C);
    String mvParentToChild = MV + " /"+ DIR_A + " " + ".";
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(mvParentToChild));
  }

  @Test
  public void mv_invalidArgs() throws Exception {
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    addDirectory(DIR_C);
    addFile(FILE_A, FILE_A_TEXT);

    // Unrecognized flag passed in
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(MV + " --badflag " + DIR_A + " " + DIR_B));

    // Too few args
    assertThrows(
      UserInputException.class, () -> fs.handleCommand(MV +" "+ DIR_A));

    // Too many args
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(
              MV + " --merge " + DIR_A + " " + DIR_B + " " + DIR_C));

    // Moving to a file as destination
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(MV + " "+ DIR_A + " " + FILE_A));

    // Moving a file with the merge option
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(
              MV + " " + MV_MERGE_OPTION + " "+ FILE_A + " " + DIR_B));

    // Invalid filepath
    assertThrows(
      UserInputException.class,
      () -> fs.handleCommand(MV + " badfilepath " + DIR_A));
  }

  @Test
  public void cp_recursivelyAddsAllDirectories() throws Exception {
    // dirA
    //   - fileA
    //   - dirC
    // dirB
    //   - fileB
    addDirectory(DIR_A);
    addDirectory(DIR_B);
    addChildFile(DIR_A, FILE_A);
    addChildDirectory(DIR_A, DIR_C);
    addChildFile(DIR_B, FILE_B);

    // Moving a directory into another adds all child directories.
    // dirA
    //   - fileA
    //   - dirC
    // dirB
    //   - fileB
    //   - dirA
    //     - fileA
    //     - dirC
    fs.handleCommand(CP + " " + DIR_A + " " + DIR_B);
    assertThat(fs.handleCommand(LS), containsString(DIR_A));
    assertThat(fs.handleCommand(LS + " " + DIR_B), containsString(DIR_A));
    assertThat(
      fs.handleCommand(LS + " " + DIR_B + "/" + DIR_A),
      allOf(containsString(FILE_A), containsString(DIR_C)));

    // Deleting a child in the old directory should not affect the new one.
    fs.handleCommand(RM + " " + DIR_A + "/" + FILE_A);
    fs.handleCommand(RM + " " + DIR_A + "/" + DIR_C);
    assertThat(fs.handleCommand(LS + " " + DIR_A), equalTo(""));
    assertThat(
      fs.handleCommand(LS + " " + DIR_B + "/" + DIR_A),
      allOf(containsString(FILE_A), containsString(DIR_C)));
  }

  private void addDirectory(String name) throws Exception {
    fs.handleCommand(MKDIR + " " + name);
  }

  private void addFile(String name, String contents) throws Exception {
    if (contents == null) {
      fs.handleCommand(WRITE + " " + name);
    } else {
      fs.handleCommand(WRITE + " " + name + " \"" + contents + "\"");
    }
  }

  // Adds a child directory to parent. Expects parent to exist as a child to
  // the current working directory.
  private void addChildDirectory(String parent, String child) throws Exception {
    fs.handleCommand(CD + " " + parent);
    fs.handleCommand(MKDIR + " " + child);
    fs.handleCommand(CD + " ..");
  }

  // Adds a child file to parent. Expects parent to exist in the current
  // working directory.
  private void addChildFile(String parent, String child) throws Exception {
    fs.handleCommand(CD + " " + parent);
    fs.handleCommand(WRITE + " " + child);
    fs.handleCommand(CD + " ..");
  }

  private void cd(String child) throws Exception{
    fs.handleCommand(CD + " " + child);
  }

  private void cdParent() throws Exception {
    fs.handleCommand(CD + " " + PARENT_DIR_SHORTCUT);
  }
}
