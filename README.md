# git-project-Tyler

1. Did you code stage() / how well does it work?

Yes, the stage() method has been implemented. The method creates a blob for the file and updates the index. I would say it works pretty well since it it able to handle basic error cases such as non-existent files and directories which I checked. In short, after extensive testing my stage method handles file versioning well and it efficiently manages staged files across folder hierarchies.

2. Did you code commit() / how well does it work?

Yes, the commit() method has been implemented. It creates a new commit object with the given author and message, updates the HEAD, and returns the commit hash. It works pretty well I would say. The method handles tree creation, hash generation, and HEAD updates correctly.

3. Did you do checkout / how well does it work? 

No. I did not implement the checkout feature. 

4. What bugs did you find / which of them did you fix?

During implementing, I encountered a error for the stage() method when handling non-existent files and directories. (HOW DID I DO THAT?)
I also added checks in the checkout() method that resulted 


I also improved error handling in the `stage()` method for non-existent files and directories by adding checks in the `checkout()` method to ensure the commit and tree objects exist before attempting to reconstruct the working directory. With that I also fixed potential issues with file paths by using `Path` and `Paths` classes for better cross-platform compatibility.




How to use the interface
GitInterface git = new GitInterfaceImpl("/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER");

// Stage a file
git.stage("/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER/example.txt");

// Create a commit
String commitHash = git.commit("Tyler", "Initial commit");
System.out.println("New commit hash: " + commitHash);
