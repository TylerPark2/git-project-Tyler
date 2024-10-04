import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

public class Blob {
    private static boolean COMPRESSION_ENABLED = true;

    public static String generateUniqueFileName(String filePath) throws IOException, NoSuchAlgorithmException {
        byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
        fileContent = maybeCompress(fileContent);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = digest.digest(fileContent);
        StringBuilder hashString = new StringBuilder();
        for (byte b : hashBytes) {
            hashString.append(String.format("%02x", b));
        }
        return hashString.toString();
    }

    // Create a new blob
    public void createBlob(String filePath, String repoPath) throws IOException, NoSuchAlgorithmException {
        boolean isTree;
        String uniqueFileName = "";
        if(new File(filePath).isDirectory()) {
            uniqueFileName = createTree(filePath, repoPath);
            isTree = true;
        } else {
            uniqueFileName = generateUniqueFileName(filePath);
            isTree = false;  
        }

        // Create the objects directory if it doesn't exist
        File objectsDir = new File(repoPath, "git/objects");
        if (!objectsDir.exists()) {
            if (!objectsDir.mkdirs()) {
                throw new IOException("Failed to create objects directory.");
            }
        }
        File blobFile = new File(objectsDir, uniqueFileName);
        if (!blobFile.exists()) {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            fileContent = maybeCompress(fileContent); // Compress if needed
            Files.write(blobFile.toPath(), fileContent);
            System.out.println("Blob created: " + uniqueFileName);
        } else {
            System.out.println("Blob already exists: " + uniqueFileName);
        }

        insertIntoIndex(isTree, uniqueFileName, filePath, repoPath);
    }

    // Creates the root tree by recursively processing the working directory
    public String createRootTree(String workingDir, String repoPath) throws IOException, NoSuchAlgorithmException {
        return createTree(workingDir, repoPath); // Delegate to createTree for root-level
    }

    //returns SHA-1 hash of tree
    public String createTree(String filePath, String repoPath) throws IOException, NoSuchAlgorithmException {
        // Save children of the given filePath as File[]
        File readFile = new File(filePath);
        readFile.setReadable(true);
        File[] children = readFile.listFiles();

        // Create a new tree object in git/objects
        File treeFile = new File(repoPath + "/git/objects/tempName");
        if(!treeFile.exists()) {
            treeFile.createNewFile();
        }
        treeFile.setWritable(true);

        // Write contents in filePath to new tree object
        FileWriter treeWriter = new FileWriter(treeFile);
        for(File child: children) {
            if(child.isDirectory()) {
                String childTreeHash = createTree(child.getPath(), repoPath); // Recursive for subdirectories
                treeWriter.append("tree " + childTreeHash + " " + child.getName() + "\n");
            } else {
                String blobHash = generateUniqueFileName(child.getPath());
                createBlob(child.getPath(), repoPath); // Create blobs for files
                treeWriter.append("blob " + blobHash + " " + child.getName() + "\n");
            }
        }
        treeWriter.close();

        // Rename treeFile to the hash of its contents
        String treeHash = generateUniqueFileName(treeFile.getAbsolutePath());
        File renamedTree = new File(repoPath + "/git/objects/" + treeHash);
        treeFile.renameTo(renamedTree);
        treeFile.delete();

        byte[] fileContent = Files.readAllBytes(Paths.get(renamedTree.getAbsolutePath()));
        fileContent = maybeCompress(fileContent); // Compress if needed
        Files.write(Paths.get(renamedTree.getAbsolutePath()), fileContent);

        System.out.println("Created tree: " + treeHash);
        return treeHash;
    }

    // Insert the blob's hash and original filename into the index file
    private void insertIntoIndex(boolean isTree, String hash, String originalFilePath, String repoPath) throws IOException {
        File indexFile = new File(repoPath, "git/index");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile, true))) {
            String fileName = Paths.get(originalFilePath).getFileName().toString();
            // Create index file with respective file type
            if(isTree) {
                writer.write ("tree " + hash + " " + fileName);
            } else {
                writer.write("blob " + hash + " " + fileName);
            }
            writer.newLine();
            System.out.println("Added to index: " + hash + " " + fileName);
        }
    }

    // Compresses data if COMPRESSION_ENABLED = true
    private static byte[] maybeCompress(byte[] data) throws IOException {
        if (COMPRESSION_ENABLED) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(data);
            }
            return byteArrayOutputStream.toByteArray();
        }
        return data;
    }

    // Clear out the test file and reset the repo
    public void resetTestFiles(String repoPath) {
        File gitDir = new File(repoPath, "git");
        if (gitDir.exists()) {
            deleteDirectory(gitDir);
            System.out.println("Test files reset.");
        } else {
            System.out.println("No repository to reset.");
        }
    }

    // Deletes directories recursively
    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }
}
