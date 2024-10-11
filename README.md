# git-project-Tyler

1. Did you code stage() / how well does it work?

Yes, the stage() method has been implemented. The method creates a blob for the file and updates the index. I would say it works pretty well since it it able to handle basic error cases such as non-existent files and directories which I checked.

2. Did you code commit() / how well does it work?

Yes, the commit() method has been implemented. It creates a new commit object with the given author and message, updates the HEAD, and returns the commit hash. The method uses the existing tree structure to capture the current state of the repository.

3. Did you do checkout / how well does it work?

Yes, the checkout() method has been implemented as an extra credit feature. It restores the working directory to the state of a specific commit by clearing the current working directory and reconstructing it based on the tree object of the specified commit. The method also handles potential errors and updates the HEAD to point to the checked-out commit.

4. What bugs did you find / which of them did you fix?

During implementing, I encountered a error for the stage() method when handling non-existent files and directories. (HOW DID I DO THAT?)
I also added checks in the checkout() method that resulted 
     - Improved error handling in the `stage()` method for non-existent files and directories.
     - Added checks in the `checkout()` method to ensure the commit and tree objects exist before attempting to reconstruct the working directory.
     - Fixed potential issues with file paths by using `Path` and `Paths` classes for better cross-platform compatibility.


// Usage Examples

