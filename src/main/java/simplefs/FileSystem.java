package simplefs;

import static simplefs.Commands.*;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import java.lang.IllegalStateException;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import simplefs.exceptions.UserInputException;

/**
 * Creates a new FileSystem. Get the environment set up via {@method startUp}
 * and tear down the filesystem with {@method tearDown}.
 * 
 * Notes:
 * If there were certain commands that were very common and we didn't expect
 * a lot of updates to directory structure, or we had lots of really deeply
 * nested directories we could cache pwd or ls commands so that we're not
 * re-traversing the tree each time.
 * 
 * Pull out the commands into some class implementing a 'Command' interface.
 * That would allow this class to be a bit shorter if we need to support many
 * more command types or if commands start taking flag options. Didn't do that
 * here because we don't have that many commands, and most are relatively
 * simple in logic. If this class grew to be too long, that could also be a
 * reason to refactor.
 * 
 * Other improvements could be to keep track of the system's working memory,
 * and if we approach some threshold of RAM usage (80-90% maybe) then we start
 * limiting the operations allowed.
 */
public class FileSystem {
  private static final String EMPTY_OUTPUT = "";
  @VisibleForTesting
  static final String CURRENT_DIR_SHORTCUT = ".";
  @VisibleForTesting
  static final String PARENT_DIR_SHORTCUT = "..";
  private static final ImmutableSet<String> SPECIAL_NAMES =
    ImmutableSet.of(CURRENT_DIR_SHORTCUT, PARENT_DIR_SHORTCUT);

  // A valid name contains only numbers, letters, or the characters `_`, `.`
  private static final String VALID_NAME_REGEX = "[\\w\\.]+";
  // A rough regex to do a quick sanity check that any given string does not
  // contain any unsupported characters. Does not do any checks on the actual
  // ordering of elements.
  private static final String VALID_PATH_NAME_REGEX = "[\\w\\.\\/]+";
  private static final Splitter PATH_SPLITTER =
    Splitter.on('/').trimResults().omitEmptyStrings();
  private static final Splitter WHITESPACE_SPLITTER = 
    Splitter.on(CharMatcher.whitespace()).trimResults().omitEmptyStrings();

  private DirectoryNode root;
  private DirectoryNode currentDirectory;
  private boolean isActive = false;

  /**
   * Initializes the filesystem. Returns start up text to be displayed to the
   * user.
   */
  public String startUp() {
    isActive = true;
    root = new DirectoryNode();
    currentDirectory = root;
    return String.format("Welcome to your in-memory filesystem! To view " +
      "commands, enter `%s`", HELP);
  }

  public void teardown() {
    isActive = false;
    root = null;
    currentDirectory = null;
  }

  public boolean isActive() {
    return isActive;
  }

  public String handleCommand(String userInput) throws UserInputException {
    if (userInput == null) {
      return EMPTY_OUTPUT;
    }
    // Limit the split to three parts in case we're trying to write a file.
    List<String> commandParts =
      WHITESPACE_SPLITTER.limit(3).splitToList(userInput);
    if (commandParts.size() == 0) {
      // Empty command received, do nothing.
      return EMPTY_OUTPUT;
    }

    String command = commandParts.get(0);
    // Treat the file write command as a special case, because we don't want to
    // unnecessarily split the string if the body has a lot of text.
    if (command.equals(WRITE)) {
      return write(commandParts);
    }

    // Treat whitespace as the delimiter for all other commands.
    commandParts = WHITESPACE_SPLITTER.splitToList(userInput);
    switch(command) {
      case CAT:
        return cat(commandParts);
      case CD:
        return cd(commandParts);
      case CP:
        return cp(commandParts);
      case HELP:
        return help(commandParts);
      case LS:
        return ls(commandParts);
      case MKDIR:
        return mkdir(commandParts);
      case MV:
        return mv(commandParts);
      case QUIT:
        return quit(commandParts);
      case RM:
        return rm(commandParts);
      case PWD:
        return pwd(commandParts);
      case FIND:
        return find(commandParts);
      default:
        return String.format("Unrecognized command: %s\n"
         + "Type `help` to view supported commands.", command);
    }
  }

  private String cat(List<String> userInput) throws UserInputException {
    if (userInput.size() == 1 || userInput.size() > 2) {
      throw new UserInputException(
        "Missing or incorrect number of arguments received.");
    }

    Optional<File> file = getFile(userInput.get(1));
    if (!file.isPresent()) {
      throw new UserInputException(
        "File not found. Check if the path is valid.");
    }

    return file.get().read();
  }

  /**
   * Navigates the user into or out of the given directory.
   */
  private String cd(List<String> userInput) throws UserInputException {
    if (userInput.size() > 2) {
      throw new UserInputException("Command 'cd' expects at most 1 argument.");
    } else if (userInput.size() == 1) {
      currentDirectory = root;
      return EMPTY_OUTPUT;
    } 

    Optional<DirectoryNode> targetDirectory =
      getDirectoryNode(userInput.get(1));

    if (targetDirectory.isPresent()){
      currentDirectory = targetDirectory.get();
    } else {
      throw new UserInputException(
        String.format("No such directory %s.", userInput.get(1)));
    }

    // If we've successfully changed directories, return nothing.
    return EMPTY_OUTPUT;
  }

  private String cp(List<String> userInput) throws UserInputException {
    if (userInput.size() < 3 || userInput.size() > 4) {
      throw new UserInputException(
        "Invalid number of arguments received for 'cp'.");
    }
    String from = userInput.get(userInput.size() == 3 ? 1 : 2);
    String to = userInput.get(userInput.size() == 3 ? 2 : 3);
    if (!from.matches(VALID_PATH_NAME_REGEX)
          || !to.matches(VALID_PATH_NAME_REGEX)) {
      throw new UserInputException("Unsupported characters in path.");
    }

    boolean isMerge = false;
    if (userInput.size() == 4) {
      if (!userInput.get(1).equals(MV_MERGE_OPTION)) {
        throw new UserInputException(
          String.format("Unrecognized flag for 'mv': %s", userInput.get(1)));
      }
      isMerge = true;
    }
    return moveOrCopy(
      getFileSystemObject(from),
      getFileSystemObject(to),
      isMerge,
      /* isCopy */ true);
  }

  private String find(List<String> userInput) throws UserInputException {
    if (userInput.size() != 2) {
      throw new UserInputException(userInput.size() == 1 
          ? "Missing name of the filename to find."
          : "Too many arguments. Find accepts only 1 argument.");
    }

    String filename = userInput.get(1);
    ArrayList<String> matchedObjectPaths = new ArrayList<>();
    LinkedList<DirectoryNode> nodesToTraverse = new LinkedList<>();
    nodesToTraverse.add(currentDirectory);

    while (!nodesToTraverse.isEmpty()) {
      DirectoryNode traversalNode = nodesToTraverse.pop();
      nodesToTraverse.addAll(traversalNode.getSubdirectories().values());
      if (traversalNode.hasChildDirectory(filename)) {
        matchedObjectPaths.add(
          traversalNode.getChildDirectory(filename).getPath() + "/");
      }
      if (traversalNode.hasChildFile(filename)) {
        matchedObjectPaths.add(
          traversalNode.getPath() + "/" + filename);
      }
    }

    if (matchedObjectPaths.size() == 0) {
      return String.format("No instances of `%s` found", filename);
    }
    // Output the paths of the strings in alphabetical order
    return matchedObjectPaths.stream()
          .sorted()
          .collect(Collectors.joining("\n"));
  }

  private String help(List<String> userInput) throws UserInputException {
    if (userInput.size() > 2) {
      throw new UserInputException("Command 'help' received too many " +
        "arguments.\n" + DETAILED_DESCRIPTION_HELP);
    } else if (userInput.size() == 1) {
      return HELP_STRING;
    }

    String command = userInput.get(1);
    if (!COMMANDS.containsKey(command)){
      throw new UserInputException(String.format(
        "Unrecognized command %s\n" + DETAILED_DESCRIPTION_HELP, command));
    }

    return COMMANDS.get(command);
  }

  private String ls(List<String> userInput) throws UserInputException {
    if (userInput.size() == 1) {
      return directoryToString(
        "", Optional.of(currentDirectory), /* includeFilepath */ false);
    } else if (userInput.size() == 2) {
      String filepath = userInput.get(1);
      return directoryToString(
        filepath, getDirectoryNode(filepath), /* includeFilepath */ false);
    }

    StringBuilder directoriesBuilder = new StringBuilder();
    for (int i = 1; i < userInput.size(); i ++) {
      String filepath = userInput.get(i);
      directoriesBuilder
        .append(
          directoryToString(
            filepath, getDirectoryNode(filepath), /* includeFilepath */ true))
        .append("\n");
    }
    return directoriesBuilder.toString();
  }

  /** 
   * Notes: Current mkdir defaults to creating any parent directories that
   * haven't been defined yet. Another extension is to allow for additional
   * arguments to specify whether or not we want this recursive directory
   * creation by default or not.
   */
  private String mkdir(List<String> userInput) throws UserInputException {
    if (userInput.size() != 2) {
      throw new UserInputException(
        userInput.size() == 1 
          ? "Missing new directory name."
          : "Too many arguments. Mkdir accepts only 1 argument.");
    } else if (!userInput.get(1).matches(VALID_PATH_NAME_REGEX)) {
        throw new UserInputException(
          String.format(
            "Unsupported characters in path: %s", userInput.get(1)));
    }

    DirectoryNode traversalNode =
      startsAtRoot(userInput.get(1)) ? root : currentDirectory;
    List<String> pathParts = PATH_SPLITTER.splitToList(userInput.get(1));
    for (int i = 0; i < pathParts.size(); i++) {
      String pathPart = pathParts.get(i);
      if (SPECIAL_NAMES.contains(pathPart)) {
        traversalNode = getSpecialPathNode(pathPart, traversalNode);
      } else if (traversalNode.hasChildDirectory(pathPart)) {
        if (i == pathParts.size() -1) {
          // We've reached the end of the path, and it's an existing directory.
          throw new UserInputException("Directory already exists");
        }
        traversalNode = traversalNode.getChildDirectory(pathPart);
      } else {
        traversalNode = traversalNode.addSubdirectory(pathPart);
      }
    }
    return EMPTY_OUTPUT;
  }

  private String mv(List<String> userInput) throws UserInputException {
    if (userInput.size() < 3 || userInput.size() > 4) {
      throw new UserInputException(
        "Invalid number of arguments received for 'mv'.");
    }
    String from = userInput.get(userInput.size() == 3 ? 1 : 2);
    String to = userInput.get(userInput.size() == 3 ? 2 : 3);
    if (!from.matches(VALID_PATH_NAME_REGEX)
          || !to.matches(VALID_PATH_NAME_REGEX)) {
      throw new UserInputException("Unsupported characters in path.");
    }

    boolean isMerge = false;
    if (userInput.size() == 4) {
      if (!userInput.get(1).equals(MV_MERGE_OPTION)) {
        throw new UserInputException(
          String.format("Unrecognized flag for 'mv': %s", userInput.get(1)));
      }
      isMerge = true;
    }
    return moveOrCopy(
      getFileSystemObject(from),
      getFileSystemObject(to),
      isMerge,
      /* isCopy */ false);
  }

  private String pwd(List<String> userInput) throws UserInputException {
    if (userInput.size() > 1) {
      throw new UserInputException("Command 'pwd' does not expect arguments.");
    }
    return currentDirectory.getPath();
  }

  private String quit(List<String> userInput) {
    isActive = false;
    return "Thanks for using this in-memory filesystem!";
  }

  private String rm(List<String> userInput) throws UserInputException {
    if (userInput.size() != 2) {
      throw new UserInputException(
        userInput.size() == 1 
          ? "Missing name of the object to delete."
          : "Too many arguments. Rm accepts only 1 argument.");
    }

    // Get the object located at the given input path, if it exists.
    Optional<FileSystemObject> fsObjectOptional =
      getFileSystemObject(userInput.get(1));
    if (!fsObjectOptional.isPresent()) {
      throw new UserInputException(
        String.format("Invalid filepath: %s", userInput.get(1)));
    }

    FileSystemObject fsObject = fsObjectOptional.get();
    if (fsObject.hasFile()) {
      // If the object is a file, delete the file from it's directory.
      fsObject
        .getDirectoryOfFile()
        .get()
        .remove(fsObject.getFile().get().getFilename());
    } else if (fsObject.hasDirectory()) {
      // If the object is a directory, check that it's not the root node.
      DirectoryNode directory = fsObject.getDirectory().get();
      if (directory.isRoot()) {
        throw new UserInputException(
          "Unsupported operation: Can't delete the root directory.");
      }

      // If we're deleting a directory that contains the current node, set
      // the current directory to the highest parent that exists.
      if (currentDirectory.hasAncestor(directory)) {
        currentDirectory = directory.getParent().get();
      }
      directory.getParent().get().remove(directory.getName().get());
    }
    return EMPTY_OUTPUT;
  }

  private String write(List<String> userInput) throws UserInputException {
    if (userInput.size() == 1 || userInput.size() > 3) {
      throw new UserInputException(
        "Missing or incorrect number of arguments received.");
    }

    String filepath = userInput.get(1);
    List<String> pathParts = PATH_SPLITTER.splitToList(filepath);
    if (pathParts.size() == 0 || filepath.endsWith("/")) {
      // Directories are not valid filepaths.
      throw new UserInputException(
        String.format("Invalid filename: %s", filepath));
    }

    String filename = pathParts.get(pathParts.size() - 1);
    if (!filename.matches(VALID_NAME_REGEX)) {
      throw new UserInputException(
        String.format(
          "Unsupported characters detected. Invalid filename: %s", filename));
    } 

    Optional<DirectoryNode> writeDirectory = Optional.of(currentDirectory);
    if (pathParts.size() > 1 || startsAtRoot(filepath)) {
      writeDirectory =
        getDirectoryNode(
          filepath.substring(0, filepath.length() - filename.length()));
      if (!writeDirectory.isPresent()) {
        throw new UserInputException(
          String.format("File is in an invalid directory: %s", filepath));
      }
    }

    if (userInput.size() == 2) {
      // Write an empty file.
      writeDirectory.get().addFile(
        new File(filename), /* overwriteExisting */ true);
    } else {
      String bodyText = userInput.get(2);
      if (!bodyText.startsWith("\"") || !bodyText.endsWith("\"")) {
        throw new UserInputException(
          "Specify the file contents to write by wrapping it in quotes.");
      }
      writeDirectory.get().addFile(
        new File(filename, bodyText.substring(1, bodyText.length() - 1)),
        /* overwriteExisting */ true);
    }
    return EMPTY_OUTPUT;
  }
 

  private String moveOrCopy(
    Optional<FileSystemObject> fromOptional,
    Optional<FileSystemObject> toOptional,
    boolean isMerge,
    boolean isCopy) throws UserInputException {
    if (!fromOptional.isPresent() || !toOptional.isPresent()){ 
      // One of the paths is malformed.
      throw new UserInputException("Directory or file does not exist.");
    }

    FileSystemObject from = fromOptional.get();
    FileSystemObject to = toOptional.get();
    if (to.hasFile()) {
      // Trying to move an object to a file.
      throw new UserInputException(
        "Cannot move or copy an object into an existing file. Try " +
        "passing a directory instead");
    } else if (from.hasDirectory()
               && to.getDirectory()
                    .get()
                    .hasAncestor(from.getDirectory().get())) {
      throw new UserInputException(
        "Cannot move or copy a directory into a child directory.");
    } else if (from.hasFile() && isMerge) {
      throw new UserInputException("Merging files is unsupported.");
    }

    DirectoryNode destination = to.getDirectory().get();
    if (from.hasFile()) {
      return moveOrCopyFile(from, destination, isCopy);
    }

    // Move the directory
    DirectoryNode movedDirectory = from.getDirectory().get();
    if (isCopy) {
      movedDirectory = DirectoryNode.getDeepCopy(movedDirectory);
    } else {
      movedDirectory.getParent().get().remove(movedDirectory.getName().get());
    }

    if (isMerge) {
      destination.mergeDirectory(movedDirectory);
    } else {
      destination.addSubdirectory(movedDirectory, /* renameIfExists */ true);
      movedDirectory.setParent(destination);
    }
    return EMPTY_OUTPUT;
  }

  private String moveOrCopyFile(
    FileSystemObject file,
    DirectoryNode destination,
    boolean isCopy) throws UserInputException {
    if (destination.equals(file.getDirectoryOfFile().get())) {
      // Moving the file into the same directory it's in is a no-op.
      return EMPTY_OUTPUT;
    }
    // Move the file into a new directory.
    File fileToMove =
      isCopy 
        ? File.makeCopy(file.getFile().get())
        : file.getFile().get();
    if (!isCopy) {
      file.getDirectoryOfFile().get().remove(fileToMove.getFilename());
    }
    // Add the file to the new destination after it gets moved, in case the
    // file needs to be renamed.
    destination.addFile(
      fileToMove, /* overwriteExisting */ false, /* renameIfExists */ true);
    return EMPTY_OUTPUT;
  }

  private String directoryToString(
    String filepath,
    Optional<DirectoryNode> dirNodeOptional,
    boolean includeFilepath) {
    StringBuilder directoryString = new StringBuilder();
    if (!dirNodeOptional.isPresent()) {
      directoryString
        .append(filepath)
        .append(": No such directory.");
        return directoryString.toString();
    }

    if (includeFilepath) {
      directoryString.append(filepath).append("\n");
    }

    DirectoryNode dirNode = dirNodeOptional.get();
    if (!dirNode.getFiles().isEmpty()) {
      directoryString
        .append(
          dirNode.getFiles().keySet().stream()
            .sorted()
            .collect(Collectors.joining("  ")));
    }
    if (!dirNode.getSubdirectories().isEmpty()){
      if (!dirNode.getFiles().isEmpty()) {
        // We just printed the files above, so add a new line before
        // printing the directories.
        directoryString.append("\n");
      }
      directoryString
        .append(
          dirNode.getSubdirectories().keySet().stream()
            .sorted()
            .map(dirName -> dirName + "/")
            .collect(Collectors.joining("  ")));
    }

    return directoryString.toString();
  }

  private Optional<DirectoryNode> getDirectoryNode(String filepath) {
    Optional<FileSystemObject> fsObj = getFileSystemObject(filepath);
    if (fsObj.isPresent() && fsObj.get().hasDirectory()) {
      return fsObj.get().getDirectory();
    }
    return Optional.empty();
  }

  private Optional<File> getFile(String filepath) {
    Optional<FileSystemObject> fsObj = getFileSystemObject(filepath);
    if (fsObj.isPresent() && fsObj.get().hasFile()) {
      return fsObj.get().getFile();
    }
    return Optional.empty();
  }

  /** 
   * Returns the directory or file at the given filepath if accessible from the
   * current working directory. Expects the filepath to delimit nodes with a
   * '/'.
   */
  private Optional<FileSystemObject> getFileSystemObject(String filepath) {
    if(!filepath.matches(VALID_PATH_NAME_REGEX)) {
      return Optional.empty();
    }
    DirectoryNode traversalNode =
      startsAtRoot(filepath) ? root : currentDirectory;

    List<String> pathParts = PATH_SPLITTER.splitToList(filepath);
    for (int i = 0; i < pathParts.size(); i++) {
      String pathPart = pathParts.get(i);
      if (SPECIAL_NAMES.contains(pathPart)) {
        traversalNode = getSpecialPathNode(pathPart, traversalNode);
      } else if (traversalNode.hasChildDirectory(pathPart)) {
        // If the path part is another directory, set that to the current
        // traversal node and keep iterating.
        traversalNode = traversalNode.getChildDirectory(pathPart);
      } else if (i == pathParts.size() - 1
                  && traversalNode.hasChildFile(pathPart)){
        // If we're at the end of this path and the final element is a file,
        // return that.
        return Optional.of(
          new FileSystemObject(
            traversalNode.getChildFile(pathPart),
            traversalNode));
      } else {
        // The path part is not a child file or directory, so this filepath
        // is invalid.
        return Optional.empty();
      }
    }

    return Optional.of(new FileSystemObject(traversalNode));
  }

  private boolean startsAtRoot(String path) {
    return path.startsWith("/");
  }

  private DirectoryNode getSpecialPathNode(
    String path, DirectoryNode currentNode) {
    if (path.equals(CURRENT_DIR_SHORTCUT)
          || (path.equals(PARENT_DIR_SHORTCUT) && currentNode.isRoot())) {
      // The special character '.' signifies the current directory.
      return currentNode;
    } else if (path.equals(PARENT_DIR_SHORTCUT)) {
      // The special character '..' signifies the parent directory, unless
      // we are at the root node, in which case it signifies itself.
      return currentNode.getParent().get();
    }
    throw new IllegalStateException(
      "Called getSpecialPathNode with unrecognized path.");
  }
}