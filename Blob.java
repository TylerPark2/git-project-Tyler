import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

public class Blob {

    private static final boolean COMPRESSION_ENABLED = false; // Toggle for compression

    public static void main(String[] args) {
        String repoPath = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER";
        String filePath = "/Users/User/Desktop/HTCS_Projects/GIT-PROJECT-TYLER/tester.txt"; // Test file

        try {
            Blob blob = new Blob();
            blob.createBlob(filePath, repoPath);
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

    // Create a new blob and store its content in the "objects" directory
    public void createBlob(String filePath, String repoPath) throws IOException, NoSuchAlgorithmException {
        String uniqueFileName = generateUniqueFileName(filePath);

        // Create the objects directory if it doesn't exist
        File objectsDir = new File(repoPath, "git/objects");
        if (!objectsDir.exists()) {
            if (!objectsDir.mkdirs()) {
                throw new IOException("Failed to create objects directory.");
            }
        }

        // Create the blob file
        File blobFile = new File(objectsDir, uniqueFileName);
        if (!blobFile.exists()) {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            fileContent = maybeCompress(fileContent); // Compress if needed
            Files.write(blobFile.toPath(), fileContent);
            System.out.println("Blob created: " + uniqueFileName);
        } else {
            System.out.println("Blob already exists: " + uniqueFileName);
        }

        // Insert the blob into the index file
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

    // Helper method to compress data using GZIP if compression is enabled
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

    // Reset functionality to clear out test files and reset the repo
    public void resetTestFiles(String repoPath) {
        File gitDir = new File(repoPath, "git");

        if (gitDir.exists()) {
            deleteDirectory(gitDir);
            System.out.println("Test files reset.");
        } else {
            System.out.println("No repository to reset.");
        }
    }

    // Recursively delete directories
    private void deleteDirectory(File dir) {
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
