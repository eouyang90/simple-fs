package simplefs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import simplefs.exceptions.UserInputException;

public class DirectoryNode {
  /**
   * Depending on whether or not we're optimizing for read type commands or
   * create/delete commands, we should either use a HashSet or a SortedList
   * to track the elements in the directory.
   * Assuming that there's a relatively small number of elements per directory,
   * a hashset makes more sense since the time cost of sorting the list is
   * relatively low.
   * Alternatively, since path manipulations can be common it might make more
   * sense for the directories to live in some sort of sorted tree, to make
   * searches for subdirectories faster.
  */
  private HashMap<String, File> files;
  private HashMap<String, DirectoryNode> subdirectories;

  // The name and parent are empty if this is the root directory.
  private Optional<String> name;
  private Optional<DirectoryNode> parent;

  /** Create a root directory node. */
  public DirectoryNode() {
    files = new HashMap<String, File>();
    subdirectories = new HashMap<String, DirectoryNode>();
    this.name = Optional.empty();
    this.parent = Optional.empty();
  }

  public DirectoryNode(String name, DirectoryNode parent) {
    // TODO: Do some arg validation here
    files = new HashMap<String, File>();
    subdirectories = new HashMap<String, DirectoryNode>();
    this.name = Optional.of(name);
    this.parent = Optional.of(parent);
  }

  /** Gets the path of this current directory.
   * 
   * Notes: Depending on how deep/nested the directory structure is, different
   * ways to calculate the path can be used. A few other things to test and
   * possibly benchmark include:
   *   - adding all names to a linked list and then using String.join()
   *   - Adding pointers to parent nodes to a stack, and then iterating
   *     through the nodes so that StringBuilder.append() could be used (the
   *     more optimal way of using StringBuilder)
   * 
   * Since the likelihood that we will have a user with a large # of nested
   * directories is low, I decided to optimize here for easy to read code,
   * while still avoiding less efficient direct string concatenation '+' or
   * a recursive function call.
   */
  public String getPath() {
    if (isRoot()) {
      return "/";
    }

    StringBuilder path = new StringBuilder();
    DirectoryNode curNode = this;
    while (!curNode.isRoot()) {
      path.insert(0, "/" + curNode.getName().get());
      curNode = curNode.getParent().get();
    }
    return path.toString();
  }

  // TODO: Somehow get user confirmation before actually deleting
  public boolean remove(String fileOrDirectory) {
    if (hasChildDirectory(fileOrDirectory)) {
      // Note:
      // The only reference to a directory's children should exist either in
      // this node, or the removed directory's children. The removed directory
      // as well as its children should be garbage collected since they are
      // no longer reachable.
      subdirectories.remove(fileOrDirectory);
      return true;
    } else if (hasChildFile(fileOrDirectory)) {
      files.remove(fileOrDirectory);
      return true;
    }
    return false;
  }

  // TODO: Update return false if there's not enough space to add this file.
  public boolean addFile(
    File file,
    boolean overwriteExisting) throws UserInputException{
    return addFile(file, overwriteExisting, /* renameIfExists */ false);
  }

  public boolean addFile(
    File file,
    boolean overwriteExisting,
    boolean renameIfExists) throws UserInputException {
    String filename = file.getFilename();
    if (hasChildDirectory(filename)
        || (hasChildFile(filename) && !overwriteExisting)) {
      if (!renameIfExists) {
        throw new UserInputException(
          String.format(
            "Directory or file `%s` already exists.", file.getFilename()));
      }
      filename = generateUniqueName(filename);
      file.setFilename(filename);
    }

    files.put(file.getFilename(), file);
    return true;
  }

  /**
   * Creates a new subdirectory with the given name. Returns the newly created
   * directory, or throws an error if the directory already exists.
   */
  public DirectoryNode addSubdirectory(String name) throws UserInputException {
    return addSubdirectory(name, /* renameIfExists */ false);
  }

  public DirectoryNode addSubdirectory(
    String name,
    boolean renameIfExists) throws UserInputException {
    String directoryName = name;
    if (hasChildDirectory(name) || hasChildFile(name)) {
      if (!renameIfExists) {
        throw new UserInputException(
          String.format("Directory or file `%s` already exists.", name));
      }
      directoryName = generateUniqueName(name);
    }

    DirectoryNode newDirectory = new DirectoryNode(directoryName, this);
    subdirectories.put(directoryName, newDirectory);

    return newDirectory;
  }

  public void addSubdirectory(
    DirectoryNode node,
    boolean renameIfExists) throws UserInputException {
    if (node.isRoot()) {
      throw new UserInputException("Cannot add root as a subdirectory.");
    }

    String directoryName = node.getName().get();
    if (hasChildDirectory(directoryName) || hasChildFile(directoryName)) {
      if (!renameIfExists) {
        throw new UserInputException(
          String.format(
            "Directory or file `%s` already exists.", directoryName));
      }
      directoryName = generateUniqueName(directoryName);
      node.setName(directoryName);
    }
  
    subdirectories.put(directoryName, node);
  }

  // Not using streams in this method because you can't throw checked
  // exceptions from within a lambda.
  public void mergeDirectory(DirectoryNode node) throws UserInputException {
    // Add all the files to this directory.
    for (File file : node.getFiles().values()) {
      addFile(
        file,
        /* overwriteExisting */ false,
        /* renameIfExists */ true);
    }

    // Add all the directories into this one. Merge if the child exists.
    for (DirectoryNode childDir : node.getSubdirectories().values()) {
      String childDirName = childDir.getName().get();
      if (hasChildDirectory(childDirName)) {
        // We already have a subdirectory with the given child name,
        // so merge those two directories.
        subdirectories.get(childDirName).mergeDirectory(childDir);
        return;
      } else if (hasChildFile(childDirName)) {
        // If the directory we're trying to merge already exists as a file,
        // then rename it during the merge.
        childDirName = generateUniqueName(childDirName);
        childDir.setName(childDirName);
      } 
      subdirectories.put(childDirName, childDir);
    }
  }

  private String generateUniqueName(String filename) {
    String prefix = filename + "_";
    Optional<Integer> maxVersion =
      Stream.concat(files.keySet().stream(), subdirectories.keySet().stream())
        .map((String name) -> {
          if (name.startsWith(prefix)) {
            try{
              int number =
                Integer.parseInt(
                  name.substring(prefix.length(), name.length()));
              return number;
            } catch (NumberFormatException ex){
              return null;
            }
          }
          return null;
        }).filter(Objects::nonNull)
        .max(Integer::compare);

    if (maxVersion.isPresent()) {
      return prefix + String.valueOf(maxVersion.get() + 1);
    }
    return prefix + "1";
  }

  /** Returns true if {@param directory} is an ancestor of this directory. */
  public boolean hasAncestor(DirectoryNode directory) {
    DirectoryNode traversalNode = this;
    while (!traversalNode.equals(directory)) {
      if (!traversalNode.hasParent()) {
        break;
      }
      traversalNode = traversalNode.getParent().get();
    }
    return traversalNode.equals(directory);
  }

  public boolean isRoot() {
    return !hasParent() && !name.isPresent();
  }

  public boolean hasParent() {
    return parent.isPresent();
  }

  public Optional<DirectoryNode> getParent() {
    return parent;
  }

  public void setParent(DirectoryNode newParent) {
    parent = Optional.of(newParent);
  }

  public Optional<String> getName() {
    return name;
  }

  public void setName(String newName) {
    name = Optional.of(newName);
  }

  public HashMap<String, File> getFiles() {
    return files;
  }

  public HashMap<String, DirectoryNode> getSubdirectories() {
    return subdirectories;
  }

  public boolean hasChildDirectory(String directoryName) {
    return subdirectories.containsKey(directoryName);
  }

  public DirectoryNode getChildDirectory(String directoryName) {
    return subdirectories.get(directoryName);
  }

  public boolean hasChildFile(String filename) {
    return files.containsKey(filename);
  }

  public File getChildFile(String filename) {
    return files.get(filename);
  }

  /**
   * Returns a deep copy of the given node. Sets the parent to be the same
   * parent as the passed in node.
   */
  public static DirectoryNode getDeepCopy(
    DirectoryNode node) throws UserInputException {
    DirectoryNode newNode;
    if (node.isRoot()) {
      newNode = new DirectoryNode();
    } else {
      newNode = new DirectoryNode(node.getName().get(), node.getParent().get());
    }

    for (File file : node.getFiles().values()) {
      newNode.addFile(file, /* overwriteExisting */ false);
    }

    for (DirectoryNode subdir : node.getSubdirectories().values()) {
      newNode.addSubdirectory(getDeepCopy(subdir), /* renameIfExists */ false);
    }

    return newNode;
  }
}