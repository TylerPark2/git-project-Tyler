import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

public class Blob {
    private static boolean COMPRESSION_ENABLED = true;
    private static final String TEST_FILE_PATH = TEST_REPO_PATH + "/test_file.txt";
    private static final String TREE_FILE_PATH = TEST_REPO_PATH + "/testTree";
    private static final String TEST_REPO_PATH = "C:/Users/gioba/Desktop/HTCS/git-project-Tyler";

    public static void main(String[] args) {
        try {
            System.out.println("Testing initialization...");
            Git git = new Git();
            git.initRepo(repoPath);
            File gitDir = new File(repoPath, "git");
            File objectsDir = new File(gitDir, "objects");
            File indexFile = new File(gitDir, "index");
            if (gitDir.exists() && objectsDir.exists() && indexFile.exists()) {
                System.out.println("Initialization test passed.");
            } else {
                System.out.println("Initialization test failed.");
            }
            System.out.println("Testing blob creation...");
            Files.write(Paths.get(testPath), "Test content".getBytes());
            Blob blob = new Blob();
            blob.createBlob(testPath, repoPath);
            String hash = generateUniqueFileName(testPath);
            File blobFile = new File(repoPath, "git/objects/" + hash);
            
            if (blobFile.exists()) {
                System.out.println("Blob creation test passed.");
            } else {
                System.out.println("Blob creation test failed.");
            }
            
            // Verify index
            indexFile = new File(repoPath, "git/index");
            String indexContent = new String(Files.readAllBytes(indexFile.toPath()));
            if (indexContent.contains(hash + " test_file.txt")) {
                System.out.println("Index update test passed.");
            } else {
                System.out.println("Index update test failed.");
            }

            // Stretch Goal #3: Test compression
            System.out.println("Testing compression...");
            
            // Test with compression enabled
            COMPRESSION_ENABLED = true;
            String repetitiveContent = "This is a test.".repeat(1000);
            Files.write(Paths.get(testPath), repetitiveContent.getBytes());
            blob.createBlob(testPath, repoPath);
            hash = generateUniqueFileName(testPath);
            blobFile = new File(repoPath, "git/objects/" + hash);
            long originalSize = Files.size(Paths.get(testPath));
            long compressedSize = Files.size(blobFile.toPath());
            System.out.println("Compression enabled - Original size: " + originalSize + ", Compressed size: " + compressedSize);

            // Compression disabled
            COMPRESSION_ENABLED = false;
            blob.createBlob(testPath, repoPath);
            hash = generateUniqueFileName(testPath);
            blobFile = new File(repoPath, "git/objects/" + hash);
            long uncompressedSize = Files.size(blobFile.toPath());
            System.out.println("Compression disabled - Original size: " + originalSize + ", Uncompressed size: " + uncompressedSize);
            if (compressedSize < uncompressedSize) {
                System.out.println("Compression test passed. Compressed size is smaller than uncompressed size.");
            } else {
                System.out.println("Compression test failed. Compressed size is not smaller than uncompressed size.");
            }

            // Test as tree
            blob.createBlob(TREE_FILE_PATH, TEST_REPO_PATH);
            
            // Clean up
            System.out.println("Cleaning up test files...");
            deleteDirectory(gitDir);
            new File(testPath).delete();
            System.out.println("Test files cleaned up.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    //returns SHA-1 hash of tree
    public String createTree(String filePath, String repoPath) throws IOException, NoSuchAlgorithmException {

        //save children of given filePath as File[]
        File readFile = new File(filePath);
        readFile.setReadable(true);
        File[] children = readFile.listFiles();

        //create new tree object in git/objects
        File treeFile = new File(repoPath + "/git/objects/tempName");
        if(!treeFile.exists()) {
            treeFile.createNewFile();
        }
        treeFile.setWritable(true);

        //write contents in filePath to new tree object
        FileWriter treeWriter = new FileWriter(treeFile);
        for(File child: children) {
            if(child.isDirectory())
            {
                treeWriter.append("tree " + generateUniqueFileName(child.getPath()) + " " + child.getName() + "\n");
            } else {
                treeWriter.append("blob " + generateUniqueFileName(child.getPath()) + " " + child.getName() + "\n");
            }
        }
        treeWriter.close();

        //rename treeFile to hash of contents
        File renamedTree = new File(repoPath + "/git/objects/" + (generateUniqueFileName(treeFile.getAbsolutePath())));
        treeFile.renameTo(renamedTree);
        treeFile.delete();

        byte[] fileContent = Files.readAllBytes(Paths.get(renamedTree.getAbsolutePath()));
        fileContent = maybeCompress(fileContent); // Compress if needed
        Files.write(Paths.get(renamedTree.getAbsolutePath()), fileContent);
        System.out.println("Created tree: " + generateUniqueFileName(repoPath + "/git/objects/" + renamedTree.getName()));
        return generateUniqueFileName(renamedTree.getAbsolutePath());
    }

    // Insert the blob's hash and original filename into the index file
    private void insertIntoIndex(boolean isTree, String hash, String originalFilePath, String repoPath) throws IOException {
        File indexFile = new File(repoPath, "git/index");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile, true))) {
            String fileName = Paths.get(originalFilePath).getFileName().toString();
            //create index file with respective file type
            if(isTree) {
                writer.write ("tree " + hash + " " + fileName);
            } else {
                writer.write("blob " + hash + " " + fileName);
            }
            writer.newLine();
            System.out.println("Added to index: " + hash + " " + fileName);
        }
    }

    // Compresses data if COMPRESSION_ENABLED is = true
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