package simplefs;

import java.util.Optional;

public class FileSystemObject {
  private Optional<DirectoryNode> directory = Optional.empty();
  private Optional<File> file = Optional.empty();
  private Optional<DirectoryNode> directoryOfFile;
  
  public FileSystemObject(DirectoryNode directory) {
    this.directory = Optional.of(directory);
  }

  public FileSystemObject(File file, DirectoryNode directory) {
    this.file = Optional.of(file);
    this.directoryOfFile = Optional.of(directory);
  }

  public boolean hasFile() {
    return file.isPresent() && directoryOfFile.isPresent();
  }

  public boolean hasDirectory() {
    return directory.isPresent();
  }

  public Optional<File> getFile() {
    return file;
  }

  public Optional<DirectoryNode> getDirectoryOfFile() {
    return directoryOfFile;
  }

  public Optional<DirectoryNode> getDirectory() {
    return directory;
  }
}