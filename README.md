# In-memory filesystem
A basic in-memory filesystem written in Java.

## Supported commands
| Command       | Use case      
| ------------------- |:-------------:|
| `cat <filename>`| Outputs the body text contained within `<filename>`|
| `cd <filepath>`| Changes the current working directory to `<filepath>`. Supports both absolute and relative filepaths, as well as special path operators `.` and `..`|
| `cp [--merge] <filepath_from> <filepath_to>`| Copies the files and directories from `<filepath_from>` to `<filepath_to>`. Collisions are handled by renaming the copied file or directory. If the merge flag is supplied, then all directories and files are merged between `<filepath_from>` into `<filepath_to>`. Recursively copies all files and directories. Supports both absolute and relative filepaths, as well as special path operators `.` and `..`|
| `find <filename>` |Outputs all paths that contain `<filename>`. Directories containing the filename will be outputted with a trailing slash.|
| `help [<command>]`  | Type `Help` to view all commands. Type `Help <command>` to view more detailed instructions for a specific command.|
| `rm <filepath>`  | Removes the directory or file located at `<filepath>`|
| `ls [<filepath>]` | Outputs all files and directories within the filepath. If no filepath is provided, output is for the current directory Supports both absolute and relative filepaths, as well as special path operators `.` and `..`|
| `mkdir <filepath>` | Creates a new directory at the given filepath. It will recursively create any directories that do not yet exist. Supported filepath characters are limited to [A-Z] [0-9] `.` and `_` Supports both absolute and relative filepaths, as well as special path operators `.` and `..`|
| `mv [--merge] <filepath_from> <filepath_to>` | Moves the files and directories from `<filepath_from>` to `<filepath_to>`. Collisions are handled by renaming the copied file or directory. If the merge flag is supplied, then all directories and files are merged between `<filepath_from>` into `<filepath_to>` Supports both absolute and relative filepaths, as well as special path operators `.` and `..`|
| `pwd` | Prints the absolute filepath starting at root for the current directory. |
| `quit` | Exits the application. All files and directories will be deleted.|
| `rm <filepath>` |Removes all files and directories at filepath. Supports both absolute and relative filepaths, as well as special path operators `.` and `..`|
| `write <filename> [\"<contents>\"]` | Creates a new file with optional body text `<contents>`. A filepath may be included in the filename. Supported filename characters are limited to [A-Z] [0-9] `.` and `_`. Body contents to write must be surrounded by quotation marks. Supports both absolute and relative filepaths, as well as special path operators `.` and `..`|

## Usage
This program uses [Maven](https://maven.apache.org). You can download and install Maven [here](https://maven.apache.org/install.html).

In the project directory, build the program by issuing the following command
```
mvn package
```

You can then run the code by running 
```
java -cp target/uber-simple-fs-1.0-SNAPSHOT.jar simplefs.App
```

If the program has started successfully, you should see the following output
```
Welcome to your in-memory filesystem! To view commands, enter `help`
> 
```
From here, feel free to issue any of the supported commands and play around with the app.

  ### Additional Commands
  To run the test suite, issue
  ```
  mvn test
  ```
