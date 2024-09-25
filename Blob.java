import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

public class Blob {
    private static boolean COMPRESSION_ENABLED = true;
    private static final String TEST_REPO_PATH = "/Users/User/Desktop/HTCS_Projects/GIT-TEST-REPO";
    private static final String TEST_FILE_PATH = TEST_REPO_PATH + "/test_file.txt";

    public static void main(String[] args) {
        try {
            // Stretch Goal #1: Test initialization
            System.out.println("Testing initialization...");
            Git git = new Git();
            git.initRepo(TEST_REPO_PATH);
            
            File gitDir = new File(TEST_REPO_PATH, "git");
            File objectsDir = new File(gitDir, "objects");
            File indexFile = new File(gitDir, "index");
            
            if (gitDir.exists() && objectsDir.exists() && indexFile.exists()) {
                System.out.println("Initialization test passed.");
            } else {
                System.out.println("Initialization test failed.");
            }

            // Stretch Goal #2: Test blob creation and verification
            System.out.println("Testing blob creation...");
            
            // Create a test file
            Files.write(Paths.get(TEST_FILE_PATH), "Test content".getBytes());
            
            Blob blob = new Blob();
            blob.createBlob(TEST_FILE_PATH, TEST_REPO_PATH);
            
            String hash = generateUniqueFileName(TEST_FILE_PATH);
            File blobFile = new File(TEST_REPO_PATH, "git/objects/" + hash);
            
            if (blobFile.exists()) {
                System.out.println("Blob creation test passed.");
            } else {
                System.out.println("Blob creation test failed.");
            }
            
            // Verify index
            indexFile = new File(TEST_REPO_PATH, "git/index");
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
            Files.write(Paths.get(TEST_FILE_PATH), repetitiveContent.getBytes());
            blob.createBlob(TEST_FILE_PATH, TEST_REPO_PATH);
            hash = generateUniqueFileName(TEST_FILE_PATH);
            blobFile = new File(TEST_REPO_PATH, "git/objects/" + hash);
            long originalSize = Files.size(Paths.get(TEST_FILE_PATH));
            long compressedSize = Files.size(blobFile.toPath());
            System.out.println("Compression enabled - Original size: " + originalSize + ", Compressed size: " + compressedSize);
            
            // Test with compression disabled
            COMPRESSION_ENABLED = false;
            blob.createBlob(TEST_FILE_PATH, TEST_REPO_PATH);
            hash = generateUniqueFileName(TEST_FILE_PATH);
            blobFile = new File(TEST_REPO_PATH, "git/objects/" + hash);
            long uncompressedSize = Files.size(blobFile.toPath());
            System.out.println("Compression disabled - Original size: " + originalSize + ", Uncompressed size: " + uncompressedSize);
            
            if (compressedSize < uncompressedSize) {
                System.out.println("Compression test passed. Compressed size is smaller than uncompressed size.");
            } else {
                System.out.println("Compression test failed. Compressed size is not smaller than uncompressed size.");
            }
            
            // Clean up
            System.out.println("Cleaning up test files...");
            deleteDirectory(gitDir);
            new File(TEST_FILE_PATH).delete();
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

    // Create a new blob and stores its content in the "objects" directory
    public void createBlob(String filePath, String repoPath) throws IOException, NoSuchAlgorithmException {
        String uniqueFileName = generateUniqueFileName(filePath);

        // Create the objects directory if it doesn't exist
        File objectsDir = new File(repoPath, "git/objects");
        if (!objectsDir.exists()) {
            if (!objectsDir.mkdirs()) {
                throw new IOException("Failed to create objects directory.");
            }
        }

        // Creates the blob
        File blobFile = new File(objectsDir, uniqueFileName);
        if (!blobFile.exists()) {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            fileContent = maybeCompress(fileContent); // Compress if needed
            Files.write(blobFile.toPath(), fileContent);
            System.out.println("Blob created: " + uniqueFileName);
        } else {
            System.out.println("Blob already exists: " + uniqueFileName);
        }

        insertIntoIndex(uniqueFileName, filePath, repoPath);
    }

    // Insert the blob's hash and original filename into the index file
    private void insertIntoIndex(String hash, String originalFilePath, String repoPath) throws IOException {
        File indexFile = new File(repoPath, "git/index");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile, true))) {
            String fileName = Paths.get(originalFilePath).getFileName().toString();
            writer.write(hash + " " + fileName);
            writer.newLine();
            System.out.println("Added to index: " + hash + " " + fileName);
        }
    }

    // Compresses data if COMPRESSION_ENABLED is true
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